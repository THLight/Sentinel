package thl.sentinel.data;

import android.app.Application;
import android.os.Environment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class THL {
    public static  int buzzerCount = 10;                 //< The times of Buzzer on + Buzzer off

    public static String WifiMac = "";
    public static String EthernetMac = "";
    public static String SentinelMac = "";
    public static boolean isInternetConnected = false;
    public static boolean isWebServerConnected = false;

    public static String UploadFreq = Constants.DEFAULT_UPLOAD_FREQUENCY;
    public static String ScanTime = Constants.DEFAULT_SCAN_TIME;
    public static String StopScanTime = Constants.DEFAULT_STOP_SCAN_TIME;
    public static String ServerUrl = "";
    public static String FilterUuid = "";
    public static String WifiSsid = "";
    public static String WifiPw = "";
    public static String WifiEnsIdentity = "";
    public static String WifiEnsPw = "";

    public static String BeaconServer = "";
    public static String NtpServer = "";
    public static String InternetIp = "";
    public static String SubnetMask = "";
    public static String Gateway = "";
    public static String DnsAddress = "";
    public static String Project = Constants.PROJECT_NAME;      //Default project is "terry"

    public static boolean isBeaconServer = false;
    public static boolean isLocatorServer = true;
    public static boolean isAlgorithmNone = true;
    public static boolean isAlgorithmAvg = false;
    public static boolean isSecurityWpa2Psk = true;
    public static boolean isSecurityWpa2Enterprise = false;
    public static boolean isInternetModeWifi = true;
    public static boolean isInternetModeEthernet = false;
    public static boolean isIpSettingDhcp = true;
    public static boolean isIpSettingStatic = false;

    public static int u32UsbType = 0;
    public static boolean bRepeatTaskStart = false;
    public static boolean isLocatorServerLive = false;
    public static boolean isBeaconServerLive = false;

    public static ArrayList<BeaconInfoFormat> aReceiverRecordList = new ArrayList<>();
    public static ArrayList<Boolean> bReceiverRecord = new ArrayList<>();
    public static ArrayList<String> aAlarmBeaconList = new ArrayList<>();  // Decide which index can  start scan.
    public static ArrayList<LocatorBeaconInfo> aLocatorBeaconList = new ArrayList<>();
    public static Map<String, String> httpPostData = new HashMap<>();

    public static void settingInitialize()
    {
        switch (THL.Project.toLowerCase())
        {
            case "terry":
                BeaconServer = "13.113.225.180/tbw/";
                ServerUrl = "http://192.168.1.42/Senti_Server_V1/api/scanBeacon";
                NtpServer = "192.168.1.42";
                WifiSsid = "THLight DLink 2.4G";
                WifiPw = "53101457";
                THL.isBeaconServer = false;
                THL.isLocatorServer = true;
                break;
            case "tsgh":
                ServerUrl = "http://10.224.11.100/Senti_Server_V1/api/scanBeacon";
                NtpServer = "10.224.11.100";
                THL.isBeaconServer = false;
                THL.isLocatorServer = true;
                THL.isInternetModeWifi = false;
                THL.isInternetModeEthernet = true;
                break;
            case "nchu":
                BeaconServer = "140.120.49.68/nchu/";
                WifiSsid = "NCHU";
                THL.isBeaconServer = true;
                THL.isLocatorServer = false;
                THL.isInternetModeWifi = true;
                THL.isInternetModeEthernet = false;
                break;
            case "fenc":
                ServerUrl = "http://10.16.191.225/Senti_Server_V1/api/scanBeacon";
                NtpServer = "10.16.191.225";
                THL.isBeaconServer = false;
                THL.isLocatorServer = true;
                THL.isInternetModeWifi = false;
                THL.isInternetModeEthernet = true;
                break;
            case "bmw":
                ServerUrl = "http://10.11.12.58/Senti_Server_V1/api/scanBeacon";
                NtpServer = "10.11.12.5";
                THL.isBeaconServer = false;
                THL.isLocatorServer = true;
                THL.isInternetModeWifi = false;
                THL.isInternetModeEthernet = true;
                break;
            case "poc":
                ServerUrl = "http://192.168.100.10/Senti_Server_V1/api/scanBeacon";
                NtpServer = "192.168.100.10";
                THL.isBeaconServer = false;
                THL.isLocatorServer = true;
                THL.isInternetModeWifi = false;
                THL.isInternetModeEthernet = true;
                WifiSsid = "Sentinel_POC";
                WifiPw = "thlight5310";
            case "tbw":

                break;
            default:
                break;
        }
    }
}

