package thl.sentinel.data;

import android.os.Environment;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class Constants {

    public final static int EMPTY = 0;
    public final static int AP_OPEN_TIME = 90000; // 90s
    public final static int CHECK_FILES_SIZE_PERIOD = 3600000;     //1hr
    /*============ Modified variables ==================*/
    final static String PROJECT_NAME = "tsgh";
    public final static String VERSION = "2.5.2";
    public final static String SENTINEL_ID = "3";   // for "store" in the beacon server.
    public final static String WIFI_AP_PW = "123456789";
    final static String DEFAULT_UPLOAD_FREQUENCY = "1000";
    final static String DEFAULT_SCAN_TIME = "200";
    final static String DEFAULT_STOP_SCAN_TIME = "20";
    private final static String DEFAULT_UUID = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0";
    public final static String DEFAULT_BEACON_SETTING = Constants.SET_BEACON_INFO + DEFAULT_UUID + " 0000 0000 D0";
    /*==============================================*/

    /*============ WEB Server =======================*/
    public final static String WEB_SERVER_PASSWORD = "53101457";
    public final static int WEB_SERVER_PORT = 9527;
    public final static String URL_SAVE = "/save";
    public final static String URL_SETTING = "/setting";
    public final static String URL_LOG = "/log";
    public final static String URL_ERROR_LOG = "/errorLog";
    /*=============================================*/

    public final static String DATE_FORMAT =  "yyyy-MM-dd HH:mm:ss.SSS";
    public final static String WRONG_YEAR = "1970";
    public final static String STORE_PATH	 = Environment.getExternalStorageDirectory().toString()+ "/Sentinel/";
    public final static SimpleDateFormat SDF = new SimpleDateFormat(DATE_FORMAT, Locale.TAIWAN);
    public final static String QUERY_KEY = "960FBD45-D7FC-47B7-B293-38C4D929E58F";//6DA39016-50EB-474E-A9EE-0F5257A70FF8";

    // =========================USB Part====================================
    public final static int TIME_FOR_ALL_USB_PLUG_IN = 5000;
    public final static int VENDORID_2540 = 1105;
    public final static int PRODUCTID_2540 = 5802;
    public final static int VENDORID_CP210X = 4292;
    public final static int PRODUCTID_CP210X = 60000;
    public final static int TYPE_2540 = 1000;               //For 2540.
    public final static int TYPE_CP210X = 2000;            //For CP210X.
    final static int TYPE_UNKNOWN = 0;
    public final static String GET_FW_INFO = "get_fw_info";
    public final static String SET_BEACON_INFO = "set_beacon_info ";           // set_beacon_info UUID Major Minor Rssi
    public final static String SET_BEACON_DATA = "set_beacon_data ";          //set_beacon_data Major Minor RSSI
    public final static String SET_KEEP_SETTING_TRUE = "set_keep_setting 1 ";          //Set writing Major Minor to the ROM.
    public final static String SET_KEEP_SETTING_FALSE = "set_keep_setting 0 ";          //Set writing Major Minor to the ROM.
    public final static String STOP_SCAN = "stop_scan";
    public final static String USB_INFO_COMMAND = "busybox lsusb\n";

    public final static String SET_DNS_COMMAND_UNDER_6X = "ndc resolver setifdns eth0 ";  // After 6.x is ndc resolver setnetdns eth0.
    public final static String DEFAULT_DNS = "8.8.8.8";

    // =====================Alarm Beacon Part=================================
    public final static  int BUZZER_FREQ = 2976;
    public final static int BUZZER_ON = 50;                     //< The time of buzzer's opening time.
    public final static int BUZZER_OFF = 1700;                     //< The time of buzzer's closed time.

    public final static int NTP_TIME_OUT_2S = 2000;   //2s
    public final static int NETWORK_ERROR_RETRY_BASE = 2;
    public final static int CONNECT_TIME_OUT = 500;          // 500ms
    public final static int READ_TIME_OUT = 2000;             // 2s
    public final static int TIME_TO_LOG = 1800;                // 0.5 hr
    public final static int MAX_LOG_SIZE = 4000;
    public final static int INITIAL_VALUE_1 = 1;
    // =========================Beacon Server Part==================================
    public final static String SENTINEL_ID_CELL = "store";
    public final static String EXECUTE_COMMAND_CELL = "extra1";
    public final static String BEACON_ACTION_LENGTH_CELL = "extra2";
    public final static String SEQUENCE_CELL = "extra3";
    public final static String LAST_COMPLETED_DATE_CELL = "extra5";
    public final static int MAX_RETRY_COUNT = 10;
    public final static int MAX_COMMAND_SEQUENCE_NUMBER = 100;
    public final static int MAX_BEACON_ID = 1023;
    public final static int HEX = 16;

    // ========================Log Manager =====================================
    public final static int ONE_MB = 1000000;
    public final static String ERROR_LOG_FILE = "error_log.txt";
    public final static String LOG_FILE = "Log.txt";

    //========================Beacon Frame =====================================
    public final static int BEACON_DATA_LENGTH = 73;
    public final static int UUID_LENGTH = 36;
    public final static int BEACON_ELEMENT_LENGTH = 6;

    // ======================= Strings ==========================================
    public final static String COMMAND_NOT_FOUND = "command not found";
    public final static String GET = "GET";
    public final static String POST = "POST";
    public final static String MD5 = "MD5";
    public final static String DATAQUERY_KEY = "dataquery-key";
    public final static String WLAN0 = "wlan0";
    public final static String ETH0 = "eth0";
    public final static String CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
    public final static String DHCP = "DHCP";
    public final static String STATIC = "STATIC";
    public final static String ILLEGAL = "illegal";
    public final static String CONTENT_TYPE = "Content-Type";
}
