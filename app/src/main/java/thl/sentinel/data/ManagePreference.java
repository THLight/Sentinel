package thl.sentinel.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ManagePreference {

    private SharedPreferences sp = null;

    public ManagePreference(Context context)
    {
        sp= PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void loadSettings()
    {
        /* */
        THL.UploadFreq       = sp.getString( "UploadFreq", THL.UploadFreq);  //Set default value.
        THL.ScanTime         = sp.getString( "ScanTime", THL.ScanTime);
        THL.StopScanTime     = sp.getString( "StopScanTime", THL.StopScanTime);
        THL.ServerUrl         = sp.getString("ServerUrl", THL.ServerUrl);
        THL.FilterUuid        = sp.getString("FilterUuid", "");
        THL.WifiSsid  		 = sp.getString("WifiSsid", THL.WifiSsid);
        THL.WifiPw  	     = sp.getString("WifiPw", THL.WifiPw);
        THL.WifiEnsIdentity  	 = sp.getString("WifiEnsIdentity", "");
        THL.WifiEnsPw  	     = sp.getString("WifiEnsPw", "");

        THL.BeaconServer        = sp.getString("BeaconServer", THL.BeaconServer);
        THL.NtpServer        = sp.getString("NtpServer", THL.NtpServer);
        THL.InternetIp         = sp.getString("InternetIp", "");
        THL.SubnetMask       = sp.getString("SubnetMask", "");
        THL.Gateway          = sp.getString("Gateway", "");
        THL.DnsAddress       = sp.getString("DnsAddress", "");
        THL.Project          =sp.getString("Project", THL.Project);

        THL.isBeaconServer = sp.getBoolean("BeaconServerType", THL.isBeaconServer);
        THL.isLocatorServer = sp.getBoolean("LocatorServerType", THL.isLocatorServer);
        THL.isAlgorithmNone = sp.getBoolean("AlgorithmNone", true);
        THL.isAlgorithmAvg = sp.getBoolean("AlgorithmAvg", false);
        THL.isSecurityWpa2Psk = sp.getBoolean("SecurityWpa2Psk", true);
        THL.isSecurityWpa2Enterprise = sp.getBoolean("SecurityWpa2Enterprise", false);
        THL.isInternetModeWifi = sp.getBoolean("InternetModeWifi", THL.isInternetModeWifi);
        THL.isInternetModeEthernet = sp.getBoolean("InternetModeEthernet", THL.isInternetModeEthernet);
        THL.isIpSettingDhcp = sp.getBoolean("IpSettingDhcp", true);
        THL.isIpSettingStatic = sp.getBoolean("IpSettingStatic", false);
    }

    public void saveSettings()
    {
        SharedPreferences.Editor edit	= sp.edit();

        edit.putString("UploadFreq", THL.UploadFreq);
        edit.putString("ScanTime", THL.ScanTime);
        edit.putString("StopScanTime", THL.StopScanTime);
        edit.putString("ServerUrl", THL.ServerUrl);
        edit.putString("FilterUuid", THL.FilterUuid);
        edit.putString("WifiSsid", THL.WifiSsid);
        edit.putString("WifiPw", THL.WifiPw);
        edit.putString("WifiEnsIdentity", THL.WifiEnsIdentity);
        edit.putString("WifiEnsPw", THL.WifiEnsPw);

        edit.putString("BeaconServer", THL.BeaconServer);
        edit.putString("NtpServer", THL.NtpServer);
        edit.putString("InternetIp", THL.InternetIp);
        edit.putString("SubnetMask", THL.SubnetMask);
        edit.putString("Gateway", THL.Gateway);
        edit.putString("DnsAddress", THL.DnsAddress);
        edit.putString("Project", THL.Project);

        edit.putBoolean("BeaconServerType", THL.isBeaconServer);
        edit.putBoolean("LocatorServerType", THL.isLocatorServer);
        edit.putBoolean("AlgorithmNone", THL.isAlgorithmNone);
        edit.putBoolean("AlgorithmAvg", THL.isAlgorithmAvg);
        edit.putBoolean("SecurityWpa2Psk", THL.isSecurityWpa2Psk);
        edit.putBoolean("SecurityWpa2Enterprise", THL.isSecurityWpa2Enterprise);
        edit.putBoolean("InternetModeWifi", THL.isInternetModeWifi);
        edit.putBoolean("InternetModeEthernet", THL.isInternetModeEthernet);
        edit.putBoolean("IpSettingDhcp", THL.isIpSettingDhcp);
        edit.putBoolean("IpSettingStatic", THL.isIpSettingStatic);

        edit.apply();
    }
}

