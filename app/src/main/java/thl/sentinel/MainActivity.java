package thl.sentinel;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import thl.sentinel.BLE.BeaconConnection;
import thl.sentinel.BLE.BeaconScan;
import thl.sentinel.BLE.iBeaconReceive;
import thl.sentinel.DataBase.DbConstants;
import thl.sentinel.Internet.NetworkManager;
import thl.sentinel.Internet.InternetConnectionReceiver;
import thl.sentinel.data.BeaconInfoFormat;
import thl.sentinel.data.Constants;
import thl.sentinel.data.ManagePreference;
import thl.sentinel.DataBase.MyDBHelper;
import thl.sentinel.data.THL;
import thl.sentinel.Internet.GetInternetInfo;
import thl.sentinel.feature.AnalysisReceiverData;
import thl.sentinel.feature.LogManager;
import thl.sentinel.feature.UsbMonitor;
import thl.sentinel.feature.UsbSerialPortManager;
import thl.sentinel.server.BeaconServerConnection;
import thl.sentinel.server.GetNtpTimeAndModifySystemTime;
import thl.sentinel.server.LocateServerConnection;

public class MainActivity extends Activity implements View.OnClickListener, MyCallback{

    /*===================  Class Declaration ========================*/
    NetworkManager networkManager = null;
    ManagePreference managePreference = null;
    UsbSerialPortManager usbSerialPortManager = null;
    LocateServerConnection locateServerConnection = null;
    InternetConnectionReceiver connectionReceiver = null;
    AnalysisReceiverData analysisReceiverData = null;
    LogManager logManager = null;
    GetNtpTimeAndModifySystemTime getNtpTimeAndModifySystemTime = null;
    UsbMonitor usbMonitor = null;
    BeaconScan beaconScan = null;
    BeaconConnection beaconConnection = null;
    BeaconServerConnection beaconServerConnection = null;
    WebServer webServer = null;
    MyDBHelper myDBHelper = null;
    /*============================================================*/

    final Handler handler = new Handler();

    ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
    ScheduledFuture<?> scheduledFuture ;   // For cancel the ShowLight task.
    final int UPDATE_FREQUENCY                 = 30;
    final int ANALYSIS_RECEIVER_DATA_FREQUENCY = 100;    //100ms

    /*================= UI Declaration ======================*/
    View view_locator = null;
    View view_beacon = null;

    TextView tv_bt_mac = null;
    TextView tv_internet_mac = null;
    TextView tv_ver = null;
    TextView tv_time = null;
    TextView tv_usbStatus = null;
    TextView tV_networkStatus = null;

    EditText et_upload_time = null;
    EditText et_scan_time = null;
    EditText et_stop_scan_time = null;
    EditText et_url = null;
    EditText et_filter_uuid = null;
    EditText et_ssid = null;
    EditText et_key = null;
    EditText et_identity = null;
    EditText et_pass = null;
    EditText et_beacon_server = null;
    EditText et_ntp_server = null;
    EditText et_ip = null;
    EditText et_subnet_mask = null;
    EditText et_gateway = null;
    EditText et_dns = null;
    EditText et_project = null;

    Button btn_save = null;
    Button btn_start_receiver = null;

    RadioGroup rg_server_type = null;
    RadioGroup rg_algorithm = null;
    RadioGroup rg_security_mode = null;
    RadioGroup rg_connect_mode = null;
    RadioGroup rg_ip_setting = null;

    RadioButton rd_beaconServer = null;
    RadioButton rd_locatorServer = null;
    RadioButton rd_bothServers = null;
    RadioButton rd_none = null;
    RadioButton rd_avg = null;
    RadioButton rd_wpa2Psk = null;
    RadioButton rd_wpa2Ent = null;
    RadioButton rd_dhcp = null;
    RadioButton rd_static = null;
    RadioButton rd_wifi = null;
    RadioButton rd_ethernet = null;

    @Override
    public void CBshowNetworkConfigurationOnUi() {
        runOnUiThread(() -> {
            if (THL.isIpSettingDhcp) {
                GetInternetInfo.getIpAddressAndSubnetMask();
                et_ip.setText(THL.InternetIp);
                et_subnet_mask.setText(THL.SubnetMask);

                if (THL.isInternetModeWifi) {
                    networkManager.getWifiDnsAndGateway();
                    et_gateway.setText(THL.Gateway);
                    et_dns.setText(THL.DnsAddress);
                }
            }
        });
    }

    @Override
    public void CBstartBeaconServerConnection() {

        if (THL.isBeaconServer)
        {
            if (!THL.isInternetConnected)
                Toast.makeText(this, "get data : Internet is not connected.",Toast.LENGTH_LONG).show();
            else
                beaconServerConnection.startBeaconServeConnection();
        }
    }

    public void CBstartAnalysisReceiverData()
    {
        if (THL.isLocatorServer)
        {
            if (THL.aReceiverRecordList.size() == Constants.EMPTY)
                Toast.makeText(this, "startAnalysis : No Receiver.",Toast.LENGTH_LONG).show();
            else if (THL.bRepeatTaskStart)
                Toast.makeText(this, "startAnalysis : Already analysis.",Toast.LENGTH_LONG).show();
            else if (!THL.isInternetConnected)
                Toast.makeText(this, "startAnalysis : Internet is not connected.",Toast.LENGTH_LONG).show();
            else
            {
                locateServerConnection = new LocateServerConnection(this);
                analysisReceiverDataRegularTask.run();
                THL.bRepeatTaskStart = true;
                LogManager.writeLogToFile("CBstartAnalysisReceiverData start");
            }
        }
    }

    public void CBstopAnalysisAndUploadReceiverData()
    {
        if (locateServerConnection != null) {
            locateServerConnection.stopUploadDataToServerTask();
            locateServerConnection = null;
        }
        handler.removeCallbacks(analysisReceiverDataRegularTask);
        THL.bRepeatTaskStart = false;
        LogManager.writeLogToFile("CBstopAnalysisAndUploadReceiverData stop");
    }

    @Override
    public void CBrecheckUsbStatus() {
        LogManager.writeLogToFile("CBrecheckUsbStatus");
        usbSerialPortManager.stopAllReceiverScan();
        usbSerialPortManager.usbActionHandler(null, UsbManager.ACTION_USB_DEVICE_DETACHED);
    }

    public void CBshowUsbStatusOnUi()
    {
        runOnUiThread(() -> {
            int tx = 0, rx = 0;
            for (boolean b : THL.bReceiverRecord)
            {
                if (b) rx++;
                else tx++;
            }

            tv_usbStatus.setText("Beacon : " + tx + ", Receiver: " + rx);
        });

    }

    public void CBshowLocatorServerStatusOnUi(String response, boolean status)
    {
        runOnUiThread(() ->
        {
            tV_networkStatus.setText(response);
            if (status)
                view_locator.setBackgroundColor(Color.GREEN);
            else
                view_locator.setBackgroundColor(Color.RED);
        });
    }

    @Override
    public void CBshowBeaconServerStatusOnUi(String response, boolean status) {
        runOnUiThread(() ->
        {
            tV_networkStatus.setText(response);
            if (status)
                view_beacon.setBackgroundColor(Color.GREEN);
            else
                view_beacon.setBackgroundColor(Color.RED);
        });
    }

    @Override
    public void CBsetBeaconCommand(String command)
    {
        //Do command on each Beacon.
        for (int i = 0; i < THL.bReceiverRecord.size(); i++) {
            if (!THL.bReceiverRecord.get(i)) {
                usbSerialPortManager.SendCMD(command, i);
            }
        }
    }

    @Override
    public void CBupdateAlarmCmdResultAndReConnectToServer(String mac, int result) {
        Log.d("debug", "CBupdateAlarmCmdResultAndReConnectToServer, size: " + THL.aAlarmBeaconList.size());
        THL.aAlarmBeaconList.remove(mac);
        beaconServerConnection.updateBeaconDataInServer(mac, String.valueOf(result), 0);
        if (THL.aAlarmBeaconList.isEmpty()) {
            try {
                Thread.sleep(1000);
                beaconServerConnection.startBeaconServeConnection();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void CBupdateCmdResultAndReConnectToServer(String id, int result, String seq) {
        Log.d("debug", "updateCmdResultAndReConnectToServer, size: " + THL.aLocatorBeaconList.size() + "id: " + id + "res:" + result);

        // Make sure id is in locator beacon list and the CMD does not execute.
        if(iBeaconReceive.removeBeacon(id) && beaconServerConnection.notExecuteCommand(id))
            beaconServerConnection.updateBeaconDataInServer(id, String.valueOf(result), Integer.parseInt(seq));
        if (THL.aLocatorBeaconList.isEmpty()) {
            try {
                Thread.sleep(1000);
                CBsetBeaconCommand(Constants.DEFAULT_BEACON_SETTING);
                beaconServerConnection.startBeaconServeConnection();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void CBsafeClose()
    {
        networkManager.configWifiAp(false);
        networkManager.disableWifi();
        usbSerialPortManager.unregisterReceiver();
        scheduledFuture.cancel(false);
        connectionReceiver.unregisterReceiver();
        handler.removeCallbacksAndMessages(null);
        CBstopAnalysisAndUploadReceiverData();
        if (usbMonitor != null)
            usbMonitor.stopUsbMonitor();
        beaconServerConnection.stopBeaconServerConnection();

        usbSerialPortManager.stopAllReceiverScan();
        usbSerialPortManager.usbClose();
        webServer.stop();
        closeDatabase();
        //beaconScan.scanLeDevice(false);

    }

    /* Continuously to analysis scanned data.*/
    private Runnable analysisReceiverDataRegularTask = new Runnable() {
        @Override
        public void run() {

            try {
                for (BeaconInfoFormat receiver : THL.aReceiverRecordList)
                {
                    //Create new thread for each receiver, and get serial port data triple to get data as more as possible.
                    new Thread(() -> {
                        receiver.sReceiverData = usbSerialPortManager.getSerialPortData(receiver.index, receiver.serialData, receiver.serialBuff);
                        receiver.sReceiverData = receiver.sReceiverData + usbSerialPortManager.getSerialPortData(receiver.index, receiver.serialData, receiver.serialBuff);
                        receiver.isUsbReadable = !receiver.sReceiverData.isEmpty();
                        //receiver.sReceiverData = receiver.sReceiverData + usbSerialPortManager.getSerialPortData(receiver.index);
                    }).start();
                    //LogManager.writeLogToFile(receiver.sReceiverData);
                }
                analysisReceiverData.startAnalysisReceiverDate();
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                handler.postDelayed(analysisReceiverDataRegularTask, ANALYSIS_RECEIVER_DATA_FREQUENCY);
            }
        }
    };

    /*Get current time and update on UI.*/
    class updateDateOnUi implements Runnable {

        public void run() {
            runOnUiThread(() -> {
                Date currentTime = Calendar.getInstance().getTime();
                //DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.TAIWAN);
                String dateString = Constants.SDF.format(currentTime);

                if (dateString.contains(Constants.WRONG_YEAR) && THL.isInternetConnected)  //re-get NTP server time.
                    getNtpTimeAndModifySystemTime.getNtpServerTime();
                tv_time.setText(dateString);
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_main);

        classesDeclare();
        THL.settingInitialize();
        openDatabase();
        UiDeclare();
        GetAndShowBasicInfoOnUi();  //Load setting
        /*===============Beacon server connection initialize.=====================*/
        beaconServerConnection = new BeaconServerConnection(this);
        if (THL.isLocatorServer)
            usbMonitor = new UsbMonitor(this);

        networkManager.configWifiAp(true);
        handler.postDelayed(this::internetConnection, Constants.AP_OPEN_TIME);

        scheduledFuture = exec.scheduleAtFixedRate(new updateDateOnUi(), UPDATE_FREQUENCY, UPDATE_FREQUENCY, TimeUnit.SECONDS);

        //startService(new Intent(getBaseContext(), Service.class));

        handler.postDelayed(() -> {
            try {
                usbSerialPortManager.USBInitial();
                webServer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 5000);

        Log.d("debug", "on Create");
    }

    private void classesDeclare()
    {
        networkManager = new NetworkManager(this);
        managePreference = new ManagePreference(this);
        usbSerialPortManager = new UsbSerialPortManager(this);
        connectionReceiver = new InternetConnectionReceiver(this);
        analysisReceiverData = new AnalysisReceiverData(this);
        logManager = new LogManager(this);
        getNtpTimeAndModifySystemTime = new GetNtpTimeAndModifySystemTime();
        //beaconScan = new BeaconScan(this);
        beaconConnection = new BeaconConnection(this);
        webServer = new WebServer( Constants.WEB_SERVER_PORT, this);
    }

    private void openDatabase(){
        myDBHelper = new MyDBHelper(this);   //取得DBHelper物件
        myDBHelper.initializeDataBase();
        myDBHelper.loadDB(DbConstants.DEFAULT_ID, DbConstants.DEFAULT_ID);
    }

    private void closeDatabase(){
        if (myDBHelper != null)
            myDBHelper.close();
    }

    private void UiDeclare()
    {
        textViewDeclare();
        editTextViewDeclare();
        buttonDeclare();
        radioGroupDeclare();
        view_locator = findViewById(R.id.view_locator);
        view_beacon = findViewById(R.id.view_beacon);
    }

    void textViewDeclare()
    {
        tv_bt_mac = findViewById(R.id.tv_bt_mac);
        tv_internet_mac = findViewById(R.id.tv_internet_mac);
        tv_ver = findViewById(R.id.tv_ver);
        tv_time = findViewById(R.id.tv_time);
        tv_usbStatus = findViewById(R.id.tv_usbStatus);
        tV_networkStatus = findViewById(R.id.tV_networkStatus);
    }

    void editTextViewDeclare()
    {
        et_upload_time = findViewById(R.id.et_upload_time);
        et_scan_time = findViewById(R.id.et_scan_time);
        et_stop_scan_time = findViewById(R.id.et_stop_scan_time);
        et_url = findViewById(R.id.et_url);
        et_filter_uuid = findViewById(R.id.et_filter_uuid);
        et_ssid = findViewById(R.id.et_ssid);
        et_key = findViewById(R.id.et_key);
        et_identity = findViewById(R.id.et_identity);
        et_pass = findViewById(R.id.et_pass);
        et_beacon_server = findViewById(R.id.et_beacon_server);
        et_ntp_server = findViewById(R.id.et_ntp_server);
        et_ip = findViewById(R.id.et_ip);
        et_subnet_mask = findViewById(R.id.et_subnet_mask);
        et_gateway = findViewById(R.id.et_gateway);
        et_dns = findViewById(R.id.et_dns);
        et_project = findViewById(R.id.et_project);
    }

    void buttonDeclare()
    {
        btn_save = findViewById(R.id.btn_save);
        btn_save.setOnClickListener(this);
        btn_start_receiver = findViewById(R.id.btn_start_receiver);
        btn_start_receiver.setOnClickListener(this);
    }

    void radioGroupDeclare()
    {
        rg_server_type = findViewById(R.id.rg_server_type);
        rg_algorithm = findViewById(R.id.rg_algorithm);
        rg_security_mode = findViewById(R.id.rg_security_mode);
        rg_connect_mode = findViewById(R.id.rg_connect_mode);
        rg_ip_setting = findViewById(R.id.rg_ip_setting);

        rg_security_mode.setOnCheckedChangeListener(mOnCheckedChangeListener);
        rg_connect_mode.setOnCheckedChangeListener(mOnCheckedChangeListener);
        rg_ip_setting.setOnCheckedChangeListener(mOnCheckedChangeListener);
        rg_server_type.setOnCheckedChangeListener(mOnCheckedChangeListener);
        rg_server_type.setFocusableInTouchMode(true);    //Set default cursor position.
        rg_server_type.requestFocus();

        rd_beaconServer = findViewById(R.id.rd_beaconServer);
        rd_locatorServer = findViewById(R.id.rd_locatorServer);
        rd_bothServers = findViewById(R.id.rd_bothServers);
        rd_none = findViewById(R.id.rd_none);
        rd_avg = findViewById(R.id.rd_avg);
        rd_wpa2Psk = findViewById(R.id.rd_wpa2Psk);
        rd_wpa2Ent = findViewById(R.id.rd_wpa2Ent);
        rd_dhcp = findViewById(R.id.rd_dhcp);
        rd_static = findViewById(R.id.rd_static);
        rd_wifi = findViewById(R.id.rd_wifi);
        rd_ethernet = findViewById(R.id.rd_ethernet);
    }

    /* Show Sentinel Setting on UI*/
    private void GetAndShowBasicInfoOnUi()
    {
        getAndShowMacAddress();
        //managePreference.loadSettings();
        ShowRadioSetting();
        showUiValueByRadioChecked();

        tv_ver.append(Constants.VERSION);
        tv_ver.append(" id : " + Constants.SENTINEL_ID);

        et_upload_time.setText(THL.UploadFreq);
        et_scan_time.setText(THL.ScanTime);
        et_stop_scan_time.setText(THL.StopScanTime);
        et_url.setText(THL.ServerUrl);
        et_filter_uuid.setText(THL.FilterUuid);
        et_ssid.setText(THL.WifiSsid);
        et_key.setText(THL.WifiPw);
        et_identity.setText(THL.WifiEnsIdentity);
        et_pass.setText(THL.WifiEnsPw);
        et_beacon_server.setText(THL.BeaconServer);
        et_ntp_server.setText(THL.NtpServer);
        et_project.setText(THL.Project);
    }

    void getAndShowMacAddress()
    {
        String BTMac = android.provider.Settings.Secure.getString(this.getContentResolver(), "bluetooth_address");
        tv_bt_mac.setText(BTMac);

        networkManager.openWifi();
        THL.EthernetMac = GetInternetInfo.getMACAddress(Constants.ETH0);
        THL.WifiMac = GetInternetInfo.getMACAddress(Constants.WLAN0);
        tv_internet_mac.setText(THL.EthernetMac);
        tv_internet_mac.append("/" + THL.WifiMac);
    }

    private void ShowRadioSetting()
    {
        if (THL.isBeaconServer && THL.isLocatorServer)
            rd_bothServers.setChecked(true);
        else
        {
            rd_beaconServer.setChecked(THL.isBeaconServer);
            rd_locatorServer.setChecked(THL.isLocatorServer);
        }
        rd_none.setChecked(THL.isAlgorithmNone);
        rd_avg.setChecked(THL.isAlgorithmAvg);
        rd_wpa2Psk.setChecked(THL.isSecurityWpa2Psk);
        rd_wpa2Ent.setChecked(THL.isSecurityWpa2Enterprise);
        rd_wifi.setChecked(THL.isInternetModeWifi);
        rd_ethernet.setChecked(THL.isInternetModeEthernet);
        rd_dhcp.setChecked(THL.isIpSettingDhcp);
        rd_static.setChecked(THL.isIpSettingStatic);
    }

    /*Show UI layout depends on the radio checked.*/
    private void showUiValueByRadioChecked()
    {
        if (THL.isSecurityWpa2Psk)
        {
            et_key.setVisibility(View.VISIBLE);
            et_identity.setVisibility(View.GONE);
            et_pass.setVisibility(View.GONE);
        }
        else if (THL.isSecurityWpa2Enterprise)
        {
            et_key.setVisibility(View.GONE);
            et_identity.setVisibility(View.VISIBLE);
            et_pass.setVisibility(View.VISIBLE);
        }

        if(THL.isInternetModeWifi)
        {
            rg_security_mode.setVisibility(View.VISIBLE);
            et_ssid.setVisibility(View.VISIBLE);
            et_key.setVisibility(View.VISIBLE);
        }
        else if (THL.isInternetModeEthernet)
        {
            rg_security_mode.setVisibility(View.GONE);
            et_ssid.setVisibility(View.GONE);
            et_key.setVisibility(View.GONE);
            et_identity.setVisibility(View.GONE);
            et_pass.setVisibility(View.GONE);
        }

        et_ip.setEnabled(THL.isIpSettingStatic);
        et_subnet_mask.setEnabled(THL.isIpSettingStatic);
        et_gateway.setEnabled(THL.isIpSettingStatic);
        et_dns.setEnabled(THL.isIpSettingStatic);
        //CBshowNetworkConfigurationOnUi();

        if (THL.isIpSettingStatic)
        {
            et_ip.setText(THL.InternetIp);
            et_subnet_mask.setText(THL.SubnetMask);
            et_gateway.setText(THL.Gateway);
            et_dns.setText(THL.DnsAddress);
        }
    }

    @Override
    public void onDestroy() {
        Log.d("debug", "OnDestroy");
        super.onDestroy();

        CBsafeClose();
    }

    @Override
    public void onClick(View view) {
        int u32ButtonId = view.getId();

        switch(u32ButtonId)
        {
            case R.id.btn_save:
                if(isSaveChangesAvailable()) {
                    saveSentiSetting();
                    Toast.makeText(this, "Save Successfully",Toast.LENGTH_SHORT).show();
                }
                else
                    Toast.makeText(this, "Need to fill the red place",Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_start_receiver:
                myDBHelper.showDatabaseData(DbConstants.INTERNET_SETTING_TABLE);
                myDBHelper.showDatabaseData(DbConstants.BASIC_SETTING_TABLE);
                beaconConnection.connectBLE("A4:34:F1:8A:25:0A",false);
                //beaconScan = new BeaconScan(this);
                new Thread(() -> {

                    //beaconServerConnection.httpServerConnection(beaconServerConnection.UPDATE_DEVICE_BY_MAC_API, "POST");
                }).start();

                break;
            default:
                break;
        }
    }

    /*Check editText is not empty before saving.*/
    boolean isSaveChangesAvailable()
    {
        boolean rtn = true;
        rtn = editTextValueCheck(et_upload_time);
        rtn = editTextValueCheck(et_scan_time) && rtn;
        rtn = editTextValueCheck(et_stop_scan_time) && rtn;
        rtn = editTextValueCheck(et_url) && rtn;
        rtn = editTextValueCheck(et_ntp_server) && rtn;
        rtn = wifiSettingCheck() && rtn;
        rtn = staticIpSettingCheck() && rtn;

        return rtn;
    }

    boolean editTextValueCheck(EditText editText)
    {
        String check = "";
        check = editText.getText().toString().trim();
        if (check.matches("")) {
            editText.setBackgroundResource(R.drawable.backwithborder);
            return false;
        }
        else
        {
            editText.setBackground(null);
            return true;
        }
    }

    boolean wifiSettingCheck()
    {
        boolean rtn = true;
        if (THL.isInternetModeWifi)
        {
            rtn = editTextValueCheck(et_ssid);

            if (THL.isSecurityWpa2Enterprise)
            {
                rtn = editTextValueCheck(et_identity) && rtn;
                rtn = editTextValueCheck(et_pass) && rtn;
            }
        }
        return rtn;
    }

    boolean staticIpSettingCheck()
    {
        boolean rtn = true;
        if(THL.isIpSettingStatic)
        {
            rtn = editTextValueCheck(et_ip);
            rtn = editTextValueCheck(et_subnet_mask) && rtn;
        }

        return rtn;
    }

    /*Save Sentinel setting and cancel the red mark.*/
    public void saveSentiSetting()
    {
        getSentinelSetting();
        managePreference.saveSettings();
        myDBHelper.saveDb(DbConstants.DEFAULT_ID, DbConstants.DEFAULT_ID);
        //Log.d("notExecuteCommand", THL.isBeaconServer +" " + THL.isLocatorServer);
        setEditTextBackgroundNull();
    }

    /*Get setting from edit text*/
    void getSentinelSetting()
    {
        THL.UploadFreq = et_upload_time.getText().toString().trim();
        THL.ScanTime = et_scan_time.getText().toString().trim();
        THL.StopScanTime = et_stop_scan_time.getText().toString().trim();
        THL.ServerUrl = et_url.getText().toString().trim();
        THL.FilterUuid = et_filter_uuid.getText().toString().trim();
        THL.WifiSsid = et_ssid.getText().toString().trim();
        THL.WifiPw = et_key.getText().toString().trim();
        THL.WifiEnsIdentity = et_identity.getText().toString().trim();
        THL.WifiEnsPw = et_pass.getText().toString().trim();
        THL.Project = et_project.getText().toString().trim();

        THL.BeaconServer = et_beacon_server.getText().toString().trim();
        THL.NtpServer = et_ntp_server.getText().toString().trim();
        if(THL.isIpSettingStatic) {
            THL.InternetIp = et_ip.getText().toString().trim();
            THL.SubnetMask = et_subnet_mask.getText().toString().trim();
            THL.Gateway = et_gateway.getText().toString().trim();
            THL.DnsAddress = et_dns.getText().toString().trim();
        }
    }

    /*Make the background of edit text transparent.*/
    void setEditTextBackgroundNull()
    {
         et_upload_time.setBackground(null);
         et_scan_time.setBackground(null);
         et_stop_scan_time.setBackground(null);
         et_url.setBackground(null);
         et_ssid.setBackground(null);
         et_identity.setBackground(null);
         et_pass.setBackground(null);
         et_ntp_server.setBackground(null);
         et_ip.setBackground(null);
         et_subnet_mask.setBackground(null);
         et_gateway.setBackground(null);
    }

    /*Start to connect the network after AP mode finished.*/
    private void internetConnection()
    {
        new Thread(() -> {
            Log.d("debug", "WebServerConnected: " + THL.isWebServerConnected);
            if (!THL.isWebServerConnected)
            {
                networkManager.configWifiAp(false);
                Log.d("debug", "Turn off ap");

                /*Need to check Internet mode, security, ip setting*/
                if (THL.isInternetModeWifi)
                    wifiConnection();
                else if (THL.isInternetModeEthernet)
                    ethernetConnection();
            }
            else
            {
                Log.d("debug", "Continually AP");
                THL.isWebServerConnected = false;
                handler.postDelayed(this::internetConnection, Constants.AP_OPEN_TIME);
            }

        }).start();
    }

    public void wifiConnection()
    {
        networkManager.openWifi();

        if (THL.isIpSettingDhcp || (THL.isIpSettingStatic && !THL.InternetIp.isEmpty())) {
            boolean rtn = networkManager.connectWifi();
            Log.d("debug", "IpSetting " + rtn);
        }
        else
            Log.d("debug", "It's Static, need to set up the IP address.");

    }

    /*================Not working now ========================*/
    public void ethernetConnection()
    {
        networkManager.connectEthernet();
    }

    private RadioGroup.OnCheckedChangeListener mOnCheckedChangeListener = (buttonView, checkedId) -> {
        switch (checkedId) {
            case R.id.rd_bothServers:
                THL.isBeaconServer = true;
                THL.isLocatorServer = true;
                break;
            case R.id.rd_beaconServer:
            case R.id.rd_locatorServer:
                THL.isBeaconServer = rd_beaconServer.isChecked();
                THL.isLocatorServer = rd_locatorServer.isChecked();
                break;
            case R.id.rd_dhcp:
            case R.id.rd_static:
                THL.isIpSettingDhcp = rd_dhcp.isChecked();
                THL.isIpSettingStatic = rd_static.isChecked();
                CBshowNetworkConfigurationOnUi();
                break;
            case R.id.rd_wpa2Psk:
            case R.id.rd_wpa2Ent:
                THL.isSecurityWpa2Psk = rd_wpa2Psk.isChecked();
                THL.isSecurityWpa2Enterprise = rd_wpa2Ent.isChecked();
                break;
            case R.id.rd_wifi:
            case R.id.rd_ethernet:
                THL.isInternetModeWifi = rd_wifi.isChecked();
                THL.isInternetModeEthernet = rd_ethernet.isChecked();
                break;
            default:
                break;
        }

        showUiValueByRadioChecked();
    };
}
