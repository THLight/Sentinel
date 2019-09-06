package thl.sentinel.DataBase;

import android.provider.BaseColumns;

public interface DbConstants extends BaseColumns {

    String DEFAULT_ID = "0";

    /*============== Internet Table =====================*/
    public static final String INTERNET_SETTING_TABLE = "InternetSetting";
    /*================== Radio Group Setting ============*/
    public static final String INTERNET_MODE_WIFI = "InternetModeWifi";
    public static final String INTERNET_MODE_ETHERNET = "InternetModeEthernet";
    public static final String IP_SETTING_DHCP = "IpSettingDhcp";
    public static final String IP_SETTING_STATIC = "IpSettingStatic";
    public static final String SECURITY_WPA2_PSK = "SecurityWpa2Psk";
    public static final String SECURITY_WPA2_ENTERPRISE = "SecurityWpa2Enterprise";
    /*=================== Network Data ===================*/
    public static final String WIFI_SSID = "WifiSsid";
    public static final String WIFI_PASSWORD = "WifiPw";
    public static final String WIFI_ENTERPRISE_IDENTITY = "WifiEnsIdentity";
    public static final String WIFI_ENTERPRISE_PASSWORD = "WifiEnsPw";
    public static final String INTERNET_IP = "InternetIp";
    public static final String SUBNET_MASK = "SubnetMask";
    public static final String GATEWAY = "Gateway";
    public static final String DNS_ADDRESS = "DnsAddress";

    /* =================== Basic Table =======================*/
    public static final String BASIC_SETTING_TABLE = "BasicSetting";
    /*==================== Radio Group Setting ================*/
    public static final String BEACON_SERVER_TYPE = "BeaconServerType";
    public static final String LOCATOR_SERVER_TYPE = "LocatorServerType";
    /*==================== Basic Data =======================*/
    public static final String UPLOAD_FREQUENCY = "UploadFreq";
    public static final String SCAN_TIME = "ScanTime";
    public static final String STOP_SCAN_TIME = "StopScanTime";
    public static final String SERVER_URL = "ServerUrl";
    public static final String FILTER_UUID = "FilterUuid";
    public static final String BEACON_SERVER = "BeaconServer";
    public static final String NTP_SERVER = "NtpServer";
    public static final String PROJECT = "Project";

    public static final String ALGORITHM_NONE = "AlgorithmNone";
    public static final String ALGORITHM_AVG = "AlgorithmAvg";

    public static final String CREATE_INTERNET_SETTING_TABLE =
            "CREATE TABLE " + INTERNET_SETTING_TABLE + " (" +
                    _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    INTERNET_MODE_WIFI + " BOOLEAN, " +
                    INTERNET_MODE_ETHERNET + " BOOLEAN, " +
                    IP_SETTING_DHCP + " BOOLEAN, " +
                    IP_SETTING_STATIC + " BOOLEAN, " +
                    SECURITY_WPA2_PSK + " BOOLEAN, " +
                    SECURITY_WPA2_ENTERPRISE + " BOOLEAN, " +
                    WIFI_SSID + " VARCHAR(255), " +
                    WIFI_PASSWORD + " VARCHAR(255), " +
                    WIFI_ENTERPRISE_IDENTITY + " VARCHAR(255), " +
                    WIFI_ENTERPRISE_PASSWORD + " VARCHAR(255), " +
                    INTERNET_IP + " VARCHAR(255), " +
                    SUBNET_MASK + " VARCHAR(255), " +
                    GATEWAY + " VARCHAR(255), " +
                    DNS_ADDRESS + " VARCHAR(255))";

    public static final String CREATE_BASIC_SETTING_TABLE =
            "CREATE TABLE " + BASIC_SETTING_TABLE + " (" +
                    _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    BEACON_SERVER_TYPE + " BOOLEAN, " +
                    LOCATOR_SERVER_TYPE + " BOOLEAN, " +
                    UPLOAD_FREQUENCY + " VARCHAR(255), " +
                    SCAN_TIME + " VARCHAR(255), " +
                    STOP_SCAN_TIME + " VARCHAR(255), " +
                    SERVER_URL + " VARCHAR(255), " +
                    FILTER_UUID + " VARCHAR(255), " +
                    BEACON_SERVER + " VARCHAR(255), " +
                    NTP_SERVER + " VARCHAR(255), " +
                    PROJECT + " VARCHAR(255))";
}
