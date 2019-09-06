package thl.sentinel;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import thl.sentinel.DataBase.DbConstants;
import thl.sentinel.DataBase.MyDBHelper;
import thl.sentinel.Internet.NetworkManager;
import thl.sentinel.data.BeaconInfoFormat;
import thl.sentinel.data.Constants;
import thl.sentinel.data.THL;
import thl.sentinel.feature.LogManager;

import static thl.sentinel.data.Constants.DATE_FORMAT;

public class WebServer extends NanoHTTPD{

    private Context mContext = null;
    private MyDBHelper myDBHelper = null;
    private MyCallback myCallback = null;
    private IHTTPSession mSession = null;
    private final SimpleDateFormat SDF = new SimpleDateFormat(DATE_FORMAT, Locale.TAIWAN);
    private boolean isLogin = false;

    WebServer(int port, Context context) {
        super(port);
        myDBHelper =new MyDBHelper(context);
        myCallback = (MyCallback) context;
        mContext = context;
    }

    public WebServer(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String msg = "<html><body><h1>Sentinel Config</h1>\n";
        try {
            msg += "<h2> Ver: " + Constants.VERSION +"</h2>\n";
            msg += "<h2> " + SDF.format(new Date()) +"</h2>\n";
            Map<String, String> files = new HashMap<String, String>();
            mSession = session;
            mSession.parseBody(files);

            msg += urlHandler(mSession.getUri());
            THL.isWebServerConnected = true;
            Log.d("debug", "isWebServerConnected: " + THL.isWebServerConnected);
        } catch (ResponseException  | IOException e) {
            e.printStackTrace();
            LogManager.saveErrorLogToFile(String.valueOf(e));
        }

        return newFixedLengthResponse( msg + "</body></html>\n" );
    }

    private String urlHandler(String url)
    {
        String msg = "";
        if (isLogin)
        {
            switch (url)
            {
                case Constants.URL_SAVE:
                    saveChanges();
                    myDBHelper.saveDb(DbConstants.DEFAULT_ID, DbConstants.DEFAULT_ID);
                    msg += "<p>Save Successfully</p><br>" + readFileInRaw(R.raw.index);
                    isLogin = false;
                    break;
                case Constants.URL_SETTING:
                    msg += combineSentinelSetting();
                    break;
                case Constants.URL_LOG:
                    msg += buttonSubmit("/", "Back");
                    msg += readLogFile(Constants.LOG_FILE);
                    LogManager.systemMonitorLog();
                    isLogin = false;
                    break;
                case Constants.URL_ERROR_LOG:
                    msg += buttonSubmit("/", "Back");
                    msg += readLogFile(Constants.ERROR_LOG_FILE);
                    isLogin = false;
                    break;
                default:
                    msg += readFileInRaw(R.raw.index);
                    break;
            }
        }
        else
        {
            String PW = getSessionParms("password");
            //Password check
            if (PW != null && PW.equals(Constants.WEB_SERVER_PASSWORD))
            {
                isLogin = true;
                msg += combineSentinelSetting();
            }
            else
                msg += "<p>Wrong Password!</p><br>" + readFileInRaw(R.raw.index);
        }

        Log.d("db", "msg:" + msg);
        return msg;
    }

    private void saveChanges() {
        THL.UploadFreq = getSessionParms("Upload Frequency");
        THL.ScanTime = getSessionParms("Scan Time");
        THL.StopScanTime = getSessionParms("Stop Scan Time");
        THL.ServerUrl = getSessionParms("Server Url");
        THL.BeaconServer = getSessionParms("Beacon Server");
        THL.FilterUuid = getSessionParms("Filter UUID");
        THL.NtpServer = getSessionParms("NTP Server");
        THL.Project = getSessionParms("Project Name");

        saveRadioSetting();
        saveNetworkSetting();
    }

    /*Setting page shows :
      *  1. Server connection status.
      *  2. Receivers' MAC
      *  3. Sentinel config
      *  4. Log/Error Log button.*/
    private String combineSentinelSetting()
    {
        String msg = "";
        msg += showServerStatus();
        msg += showReceivers();
        msg += readFileInRaw(R.raw.show_config);
        /*================ Javascript =========================*/
        msg += jsToShowConfigData();
        msg += jsToShowRadioChecked();
        /*=================================================*/
        msg += buttonSubmit(Constants.URL_LOG, "Log");
        msg += buttonSubmit(Constants.URL_ERROR_LOG, "Error Log");
        return msg;
    }

    private  String getSessionParms(String parms)
    {
        return mSession.getParms().get(parms);
    }

    private void saveRadioSetting()
    {
        String radioServer = getSessionParms("server");
        String radioSecurity = getSessionParms("security");
        String radioMode = getSessionParms("mode");
        String radioIpSetting = getSessionParms("ip setting");

        getRadioServerValue(radioServer);
        getRadioSecurityValue(radioSecurity);
        getRadioModeValue(radioMode);
        getRadioIpSettingValue(radioIpSetting);
    }

    private void getRadioServerValue(String value)
    {
        if (value != null) {
            switch (value)
            {
                case "beacon":
                    THL.isBeaconServer = true;
                    THL.isLocatorServer = false;
                    break;
                case "locator":
                    THL.isBeaconServer = false;
                    THL.isLocatorServer = true;
                    break;
                case "both":
                    THL.isBeaconServer = true;
                    THL.isLocatorServer = true;
                    break;
            }
        }
    }

    private void getRadioSecurityValue(String value)
    {
        if (value != null) {
            switch (value)
            {
                case "PSK":
                    THL.isSecurityWpa2Psk = true;
                    THL.isSecurityWpa2Enterprise = false;
                    break;
                case "Enterprise":
                    THL.isSecurityWpa2Psk = false;
                    THL.isSecurityWpa2Enterprise = true;
                    break;
            }
        }
    }

    private void getRadioModeValue(String value)
    {
        if (value != null) {
            switch (value)
            {
                case "WIFI":
                    THL.isInternetModeWifi = true;
                    THL.isInternetModeEthernet = false;
                    break;
                case "Ethernet":
                    THL.isInternetModeWifi = false;
                    THL.isInternetModeEthernet = true;
                    break;
            }
        }
    }

    private void getRadioIpSettingValue(String value)
    {
        if (value != null) {
            switch (value)
            {
                case "DHCP":
                    THL.isIpSettingDhcp = true;
                    THL.isIpSettingStatic = false;
                    break;
                case "Static":
                    THL.isIpSettingDhcp = false;
                    THL.isIpSettingStatic = true;
                    break;
            }
        }
    }

    private void saveNetworkSetting()
    {
        if (THL.isIpSettingStatic)
        {
            THL.InternetIp = getSessionParms("IP");
            THL.SubnetMask = getSessionParms("Subnet Mask");
            THL.Gateway = getSessionParms("Gateway");
            THL.DnsAddress = getSessionParms("DNS");
        }

        if (THL.isInternetModeWifi)
        {
            THL.WifiSsid = getSessionParms("Wifi SSID");
            THL.WifiPw = getSessionParms("Wifi Password");

            if (THL.isSecurityWpa2Enterprise)
            {
                THL.WifiEnsIdentity = getSessionParms("Wifi Ens Identity");
                THL.WifiEnsPw = getSessionParms("Wifi Ens Password");
            }
        }
    }

    private String showServerStatus()
    {
        String msg = "";
        msg += readFileInRaw(R.raw.box);
        msg += "<p><b>Locator &emsp; &emsp; &emsp; Beacon</b></p>\n";

        if (THL.isLocatorServerLive)
            msg += "<div class='box green'></div>\n";
        else
            msg += "<div class='box red'></div>\n";

        if (THL.isBeaconServerLive)
            msg += "<div class='box green'></div>\n";
        else
            msg += "<div class='box red'></div>\n";

        msg += "<div style=\"clear:both\"></div>";
        return msg;
    }

    private String showReceivers()
    {
        String msg = "";

        for (BeaconInfoFormat receiver :THL.aReceiverRecordList)
        {
            msg += "<p><b>Receiver MAC:</b> " + receiver.receiverMac + "</p>\n";
        }
        return msg;
    }

    private String jsToShowConfigData()
    {
        String msg = "<script> \n";
        msg += "document.getElementById('id').insertAdjacentHTML( 'beforeend', '" + Constants.SENTINEL_ID + "');\n";
        msg += "document.getElementById('MAC').insertAdjacentHTML( 'beforeend', '" + THL.EthernetMac + "/" + THL.WifiMac + "');\n";

        msg += getElementsByName("Upload Frequency", THL.UploadFreq)
            + getElementsByName("Scan Time", THL.ScanTime)
            + getElementsByName("Stop Scan Time", THL.StopScanTime)
            + getElementsByName("Server Url", THL.ServerUrl)
            + getElementsByName("Beacon Server", THL.BeaconServer)
            + getElementsByName("Filter UUID", THL.FilterUuid)
            + getElementsByName("Wifi SSID", THL.WifiSsid)
            + getElementsByName("Wifi Password", THL.WifiPw)
            + getElementsByName("NTP Server", THL.NtpServer)
            + getElementsByName("IP", THL.InternetIp)
            + getElementsByName("Subnet Mask", THL.SubnetMask)
            + getElementsByName("Gateway", THL.Gateway)
            + getElementsByName("DNS", THL.DnsAddress)
            + getElementsByName("Project Name", THL.Project);
        msg += "</script>\n";

        return msg;
    }

    private String getElementsByName(String name, String value)
    {
        return "document.getElementsByName('" + name + "')[0].value='" + value +"';\n";
    }

    private String organizeSentinelData(String msg)
    {
        msg += "<p><b>Ethernet/WIFI MAC:</b> " + THL.EthernetMac + "/" +THL.WifiMac+ "</p>\n";
        msg += "<form action= 'save' method='post'> <b>Upload Frequency:</b><br> <input type='text' name='Upload Frequency' value='" + THL.UploadFreq + "'><br><br>\n";
        msg += "<b>Scan Frequency:</b><br> <input type='text' name='Scan Time' value='" + THL.ScanTime
                + "'> <input type='text' name='Stop Scan Time' value='" + THL.StopScanTime +"'><br><br>\n";
        msg += "<b>Server Type</b> <br>" +
                "<input type='radio' name='server' id='beacon' value='beacon'>Beacon " +
                "<input type='radio' name='server' id='locator' value='locator'>Locator " +
                "<input type='radio' name='server' id='both' value='both'>Both<br><br>\n";

        msg += "<b>URL:</b><br> <input type='text' name='Server Url' value='" + THL.ServerUrl + "' size= '50'><br><br>\n";
        msg += "<b>Beacon Server:</b><br> <input type='text' name='Beacon Server' value='" + THL.BeaconServer + "'><br><br>\n";
        msg += "<b>Filter:</b><br> <input type='text' name='Filter UUID' value='" + THL.FilterUuid + "'><br><br>\n";

        msg += "<b>Security</b><br> " +
                "<input type='radio' name='security' id='PSK' value='PSK'>WPA2 PSK " +
                "<input type='radio' name='security' id='Enterprise' value='Enterprise'>WPA2 Enterprise<br><br>\n";

        if (THL.isInternetModeWifi)
        {
            msg += "<b>Wifi:</b><br> <input type='text' name='Wifi SSID' value='" + THL.WifiSsid
                    + "'> <input type='text' name='Wifi Password' value='" + THL.WifiPw +"'><br><br>\n";

            if (THL.isSecurityWpa2Enterprise)
                msg += " <input type='text' name='Wifi Ens Identity' value='" + THL.WifiEnsIdentity
                        + "'> <input type='text' name='Wifi Ens Password' value='" + THL.WifiEnsPw +"'><br><br>\n";
        }

        msg += "<b>Mode</b><br> " +
                "<input type='radio' name='mode' id='WIFI' value='WIFI'>WIFI " +
                "<input type='radio' name='mode' id='Ethernet' value='Ethernet'>Ethernet<br><br>\n";

        msg += "<b>NTP Server:</b><br> <input type='text' name='NTP Server' value='" + THL.NtpServer + "'><br><br>\n";

        msg += "<b>IP Setting</b><br> " +
                "<input type='radio' name='ip setting' id='DHCP' value='DHCP'>DHCP " +
                "<input type='radio' name='ip setting' id='Static' value='Static'>Static<br>\n";

        msg += "<b>IP:</b><br> <input type='text' name='IP' value='" + THL.InternetIp + "'><br><br>\n";
        msg += "<b>Subnet Mask:</b><br> <input type='text' name='Subnet Mask' value='" + THL.SubnetMask + "'><br><br>\n";
        msg += "<b>Gateway:</b><br> <input type='text' name='Gateway' value='" + THL.Gateway + "'><br><br>\n";
        msg += "<b>DNS:</b><br> <input type='text' name='DNS' value='" + THL.DnsAddress + "'><br><br>\n";

        msg += "<b>Project Name:</b><br> <input type='text' name='Project Name' value='" + THL.Project + "'><br><br>\n";

        msg += "<button>SAVE CHANGES</button></form>\n";

        return msg;
    }

    private String jsToShowRadioChecked()
    {
        String msg ="<script>\n";

        if (THL.isBeaconServer && THL.isLocatorServer)
            msg += radioCheckByGetElementById("both", true);
        else
            msg += radioCheckByGetElementById("beacon", THL.isBeaconServer)
                    + radioCheckByGetElementById("locator", THL.isLocatorServer);

        msg +=  radioCheckByGetElementById("PSK", THL.isSecurityWpa2Psk)
                + radioCheckByGetElementById("Enterprise", THL.isSecurityWpa2Enterprise);

        msg += radioCheckByGetElementById("WIFI", THL.isInternetModeWifi)
                + radioCheckByGetElementById("Ethernet", THL.isInternetModeEthernet);

        msg += radioCheckByGetElementById("DHCP", THL.isIpSettingDhcp)
                + radioCheckByGetElementById("Static", THL.isIpSettingStatic);

        msg += "</script>\n";

        return msg;
    }

    private String radioCheckByGetElementById(String name, Boolean value)
    {
        return "document.getElementById('" + name + "').checked=" + value +";\n";
    }

    private String buttonSubmit(String page, String text)
    {
        return "<button onclick='location.href=\"" + page + "\"'>" + text +"</button>&emsp;\n";
    }

    private String readLogFile(String name)
    {
        File file = new File(Constants.STORE_PATH, name);
        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append("<br>");
            }
            br.close();
        }
        catch (IOException e) {
            LogManager.saveErrorLogToFile(String.valueOf(e));
        }

        return "<p>" + text + "</p>";
    }

    private String readFileInRaw(int name)
    {
        InputStream inputStream = mContext.getResources().openRawResource(name);

        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append("\n");
            }
            br.close();
        }
        catch (IOException e) {
            LogManager.saveErrorLogToFile(String.valueOf(e));
        }
        return String.valueOf(text);
    }
}