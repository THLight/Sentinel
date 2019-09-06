package thl.sentinel.Internet;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import thl.sentinel.data.Constants;
import thl.sentinel.data.THL;
import thl.sentinel.feature.LogManager;



public class NetworkManager {

    private WifiManager wifiManager;
    private Context mContext = null;


    public NetworkManager(Context context) {
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mContext = context;
    }

    public void configWifiAp(boolean enable)
    {
        Method setWifiApMethod  = null;
        try {
                disableWifi();  //Wifi or AP only could choose one.
                setWifiApMethod  = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                setWifiApMethod .invoke(wifiManager, initWifiApConfig(), enable);

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LogManager.saveErrorLogToFile(String.valueOf(e));
            e.printStackTrace();
        }
    }

    private WifiConfiguration initWifiApConfig(){
        WifiConfiguration netConfig = new WifiConfiguration();
        netConfig.SSID = "THL_Receiver_" + THL.WifiMac;
        netConfig.preSharedKey = Constants.WIFI_AP_PW;
        netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

        return netConfig;
    }

    public void openWifi()
    {
        configWifiAp(false);
        wifiManager.setWifiEnabled(true);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            LogManager.saveErrorLogToFile(String.valueOf(e));
            e.printStackTrace();
        }
    }

    public void disableWifi()
    {
        wifiManager.setWifiEnabled(false);
    }

    public void openEthernet()
    {
        try {
            Runtime.getRuntime().exec("ifconfig eth0 up");
            Thread.sleep(5000);
        } catch (IOException | InterruptedException e) {
            LogManager.saveErrorLogToFile(String.valueOf(e));
            e.printStackTrace();
        }
    }

    public boolean connectWifi()
    {
        boolean rtn = false;
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + THL.WifiSsid + "\"";
        conf.preSharedKey = "\""+ THL.WifiPw +"\"";

        wifiEnterpriseSetting(conf);

        wifiManager.addNetwork(conf);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + THL.WifiSsid + "\"")) {
                wifiManager.disconnect();
                wifiIpAssignmentSetting(i);
                rtn = wifiManager.enableNetwork(i.networkId, true);
                Log.d("debug", "Connect to WIFI, " + i.SSID + rtn );
                return rtn;
            }
        }
        return rtn;
    }

    private void wifiEnterpriseSetting(WifiConfiguration conf)
    {
        //Only work under Android 7.0
        if (THL.isSecurityWpa2Enterprise)
        {
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);

            WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();
            enterpriseConfig.setIdentity(THL.WifiEnsIdentity);
            enterpriseConfig.setPassword(THL.WifiEnsPw);
            enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.PEAP);
            enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.MSCHAPV2);
            conf.enterpriseConfig = enterpriseConfig;
        }
    }

    private void wifiIpAssignmentSetting(WifiConfiguration wifiConf)
    {
        try{
            // linkProperties is hided in WifiConfiguration.
            Object linkProperties = getField(wifiConf, "linkProperties");
            if(linkProperties == null) return;

            if (THL.isIpSettingDhcp)
                setIpAssignment(Constants.DHCP, wifiConf);
            else if (THL.isIpSettingStatic)
            {
                if (THL.DnsAddress.isEmpty())
                    THL.DnsAddress = Constants.DEFAULT_DNS;
                setIpAssignment(Constants.STATIC, wifiConf); //or "DHCP" for dynamic setting
                setIpAddress(InetAddress.getByName(THL.InternetIp), subnetMaskAddressToPrefixLength(), wifiConf, linkProperties);
                setGateway(InetAddress.getByName(THL.Gateway), wifiConf, linkProperties);
                setDNS(InetAddress.getByName(THL.DnsAddress), wifiConf, linkProperties);
            }

            wifiManager.updateNetwork(wifiConf); //apply the setting
            wifiManager.saveConfiguration(); //Save it
        }catch(Exception e){
            e.printStackTrace();
            LogManager.saveErrorLogToFile(String.valueOf(e));
        }
    }

    private void setIpAssignment(String assign, WifiConfiguration wifiConf)
            throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException{
        setEnumField(wifiConf, assign);
    }

      /*  public enum IpAssignment {
            // Use statically configured IP settings. Configuration can be accessed  with linkProperties
            STATIC,
            // Use dynamically configured IP settigns
            DHCP,
            // no IP details are assigned, this is used to indicate that any existing IP settings should be retained
            UNASSIGNED
        } */
    private void setEnumField(Object obj, String value)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        Field f = obj.getClass().getField("ipAssignment");
        f.set(obj, Enum.valueOf((Class<Enum>) f.getType(), value));
    }

    private void setIpAddress(InetAddress addr, int prefixLength, WifiConfiguration wifiConf, Object linkProperties)
            throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            NoSuchMethodException, ClassNotFoundException, InstantiationException, InvocationTargetException{

        //Create a constructor of LinkAddress with IP, and prefixLength.
        Class laClass = Class.forName("android.net.LinkAddress");
        Constructor laConstructor = laClass.getConstructor(new Class[]{InetAddress.class, int.class});
        Object linkAddress = laConstructor.newInstance(addr, prefixLength);

        // mLinkAddresses is a ArrayList in class LinkProperties .
        ArrayList mLinkAddresses = (ArrayList)getDeclaredField(linkProperties, "mLinkAddresses");
        mLinkAddresses.clear();
        mLinkAddresses.add(linkAddress);
    }

    private void setGateway(InetAddress gateway, WifiConfiguration wifiConf, Object linkProperties)
            throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException, InvocationTargetException{

        Class routeInfoClass = Class.forName("android.net.RouteInfo");
        Constructor routeInfoConstructor = routeInfoClass.getConstructor(new Class[]{InetAddress.class});
        Object routeInfo = routeInfoConstructor.newInstance(gateway);

        // mRoutes is a ArrayList in class LinkProperties .
        ArrayList mRoutes = (ArrayList)getDeclaredField(linkProperties, "mRoutes");
        mRoutes.clear();
        mRoutes.add(routeInfo);
    }

    private void setDNS(InetAddress dns, WifiConfiguration wifiConf, Object linkProperties)
    {
        try {
            // mDnses is a ArrayList in class LinkProperties .
            ArrayList<InetAddress> mDnses = (ArrayList<InetAddress>)getDeclaredField(linkProperties, "mDnses");
            mDnses.clear(); //or add a new dns address , here I just want to replace DNS1
            mDnses.add(dns);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            LogManager.saveErrorLogToFile(String.valueOf(e));
        }
    }

    private Object getField(Object obj, String name)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        Field f = obj.getClass().getField(name); //Returns the value of the field represented by this Field, on the specified object.
        return f.get(obj);
    }

    private Object getDeclaredField(Object obj, String name)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);  // To access private function.
        return f.get(obj);
    }

    public void connectEthernet()
    {
        if (THL.isIpSettingStatic)
        {
            new Thread(() -> {
                try {
                    String currentIp = GetInternetInfo.getIPAddress(true);

                    if (!currentIp.equals(THL.InternetIp))
                    {
                        int netBits = subnetMaskAddressToPrefixLength();

                        String cmd = "ip addr add "+ THL.InternetIp +"/"+ netBits + " dev eth0\n";

                        Log.d("debug", cmd);
                        Process process = Runtime.getRuntime().exec("su");
                        DataOutputStream os = new DataOutputStream(process.getOutputStream());
                        os.writeBytes(cmd);

                        cmd = "ip addr del "+ currentIp + " dev eth0\n";
                        os.writeBytes(cmd);
                        Log.d("debug", cmd);

                        cmd = Constants.SET_DNS_COMMAND_UNDER_6X + THL.DnsAddress + Constants.DEFAULT_DNS + "\n";
                        os.writeBytes(cmd);

                        os.writeBytes("exit\n");
                        process.waitFor();
                    }

                } catch (IOException | InterruptedException e) {
                    LogManager.saveErrorLogToFile(String.valueOf(e));
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private int subnetMaskAddressToPrefixLength()
    {
        String[] data = THL.SubnetMask.split("\\.");
        String subnetMaskBinary = "";
        for (String d : data)
        {
            subnetMaskBinary = subnetMaskBinary + Integer.toBinaryString(Integer.parseInt(d));
        }

        subnetMaskBinary = subnetMaskBinary.replace("0","");
        return subnetMaskBinary.length();
    }

    public void getWifiDnsAndGateway()
    {
        DhcpInfo d;
        d = wifiManager.getDhcpInfo();
        THL.DnsAddress = Formatter.formatIpAddress(d.dns1);
        THL.Gateway = Formatter.formatIpAddress(d.gateway);
    }

}
