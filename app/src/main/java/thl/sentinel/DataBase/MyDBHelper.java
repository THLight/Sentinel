package thl.sentinel.DataBase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import thl.sentinel.data.THL;
import thl.sentinel.feature.LogManager;

import static android.provider.BaseColumns._ID;
import static thl.sentinel.DataBase.DbConstants.BASIC_SETTING_TABLE;
import static thl.sentinel.DataBase.DbConstants.BEACON_SERVER;
import static thl.sentinel.DataBase.DbConstants.BEACON_SERVER_TYPE;
import static thl.sentinel.DataBase.DbConstants.CREATE_BASIC_SETTING_TABLE;
import static thl.sentinel.DataBase.DbConstants.CREATE_INTERNET_SETTING_TABLE;
import static thl.sentinel.DataBase.DbConstants.DEFAULT_ID;
import static thl.sentinel.DataBase.DbConstants.DNS_ADDRESS;
import static thl.sentinel.DataBase.DbConstants.FILTER_UUID;
import static thl.sentinel.DataBase.DbConstants.GATEWAY;
import static thl.sentinel.DataBase.DbConstants.INTERNET_IP;
import static thl.sentinel.DataBase.DbConstants.INTERNET_MODE_ETHERNET;
import static thl.sentinel.DataBase.DbConstants.INTERNET_MODE_WIFI;
import static thl.sentinel.DataBase.DbConstants.INTERNET_SETTING_TABLE;
import static thl.sentinel.DataBase.DbConstants.IP_SETTING_DHCP;
import static thl.sentinel.DataBase.DbConstants.IP_SETTING_STATIC;
import static thl.sentinel.DataBase.DbConstants.LOCATOR_SERVER_TYPE;
import static thl.sentinel.DataBase.DbConstants.NTP_SERVER;
import static thl.sentinel.DataBase.DbConstants.PROJECT;
import static thl.sentinel.DataBase.DbConstants.SCAN_TIME;
import static thl.sentinel.DataBase.DbConstants.SECURITY_WPA2_ENTERPRISE;
import static thl.sentinel.DataBase.DbConstants.SECURITY_WPA2_PSK;
import static thl.sentinel.DataBase.DbConstants.SERVER_URL;
import static thl.sentinel.DataBase.DbConstants.STOP_SCAN_TIME;
import static thl.sentinel.DataBase.DbConstants.SUBNET_MASK;
import static thl.sentinel.DataBase.DbConstants.UPLOAD_FREQUENCY;
import static thl.sentinel.DataBase.DbConstants.WIFI_ENTERPRISE_IDENTITY;
import static thl.sentinel.DataBase.DbConstants.WIFI_ENTERPRISE_PASSWORD;
import static thl.sentinel.DataBase.DbConstants.WIFI_PASSWORD;
import static thl.sentinel.DataBase.DbConstants.WIFI_SSID;


public class MyDBHelper extends SQLiteOpenHelper {
    private final static String DATABASE_NAME="sentinel.db";  //資料庫檔案名稱
    private final static int DATABASE_VERSION = 1;   //資料庫版本
    // 資料庫物件，固定的欄位變數
    private static SQLiteDatabase database;
    private Cursor mCursor = null;

    // 建構子，在一般的應用都不需要修改
    public MyDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_INTERNET_SETTING_TABLE);
        sqLiteDatabase.execSQL(CREATE_BASIC_SETTING_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        final String DROP_TABLE = "drop table if exists " + BASIC_SETTING_TABLE;
        sqLiteDatabase.execSQL(DROP_TABLE);
        sqLiteDatabase.execSQL("drop table if exists " + INTERNET_SETTING_TABLE);
        onCreate(sqLiteDatabase);
    }

    public void add(String tableName){
        SQLiteDatabase db = getWritableDatabase();  //透過dbHelper取得讀取資料庫的SQLiteDatabase物件，可用在新增、修改與刪除
        ContentValues values = new ContentValues();  //建立 ContentValues 物件並呼叫 put(key,value) 儲存欲新增的資料，key 為欄位名稱  value 為對應值。
        values.put(UPLOAD_FREQUENCY, THL.UploadFreq);
        values.put(SCAN_TIME, THL.ScanTime);
        values.put(STOP_SCAN_TIME, THL.StopScanTime);
        db.insert(tableName,null,values);
    }

    public void initializeDataBase()
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues internetValue = saveInternetSettingValue(DEFAULT_ID);
        ContentValues basicValue = saveBasicSettingValue(DEFAULT_ID);
        db.insertWithOnConflict(INTERNET_SETTING_TABLE,null, internetValue, SQLiteDatabase.CONFLICT_IGNORE);
        db.insertWithOnConflict(BASIC_SETTING_TABLE,null, basicValue, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private ContentValues saveInternetSettingValue(String id)
    {
        ContentValues values = new ContentValues();
        values.put(_ID, id);
        values.put(INTERNET_MODE_WIFI, THL.isInternetModeWifi);
        values.put(INTERNET_MODE_ETHERNET, THL.isInternetModeEthernet);
        values.put(IP_SETTING_DHCP, THL.isIpSettingDhcp);
        values.put(IP_SETTING_STATIC, THL.isIpSettingStatic);
        values.put(SECURITY_WPA2_PSK, THL.isSecurityWpa2Psk);
        values.put(SECURITY_WPA2_ENTERPRISE, THL.isSecurityWpa2Enterprise);
        values.put(WIFI_SSID, THL.WifiSsid);
        values.put(WIFI_PASSWORD, THL.WifiPw);
        values.put(WIFI_ENTERPRISE_IDENTITY, THL.WifiEnsIdentity);
        values.put(WIFI_ENTERPRISE_PASSWORD, THL.WifiEnsPw);

        if (THL.isIpSettingStatic)
        {
            values.put(INTERNET_IP, THL.InternetIp);
            values.put(SUBNET_MASK, THL.SubnetMask);
            values.put(GATEWAY, THL.Gateway);
            values.put(DNS_ADDRESS, THL.DnsAddress);
        }
        else
        {
            values.put(INTERNET_IP, "");
            values.put(SUBNET_MASK, "");
            values.put(GATEWAY, "");
            values.put(DNS_ADDRESS, "");
        }

        return values;
    }

    private ContentValues saveBasicSettingValue(String id)
    {
        ContentValues values = new ContentValues();
        values.put(_ID, id);
        values.put(BEACON_SERVER_TYPE, THL.isBeaconServer);
        values.put(LOCATOR_SERVER_TYPE, THL.isLocatorServer);
        values.put(UPLOAD_FREQUENCY, THL.UploadFreq);
        values.put(SCAN_TIME, THL.ScanTime);
        values.put(STOP_SCAN_TIME, THL.StopScanTime);
        values.put(SERVER_URL, THL.ServerUrl);
        values.put(FILTER_UUID, THL.FilterUuid);
        values.put(BEACON_SERVER, THL.BeaconServer);
        values.put(NTP_SERVER, THL.NtpServer);
        values.put(PROJECT, THL.Project);
        return values;
    }

    private Cursor getCursor(String tableName){
        SQLiteDatabase db = getReadableDatabase();  //透過dbHelper取得讀取資料庫的SQLiteDatabase物件，可用在查詢
        String sql = " Select * from " + tableName;
        return db.rawQuery(sql,null);
    }

    public void showDatabaseData(String tableName){
        StringBuilder resultData = new StringBuilder("Result:\n");
        mCursor = getCursor(tableName);  //取得查詢物件Cursor;

        if (tableName.equals(INTERNET_SETTING_TABLE))
            resultData.append(parserDbInternetSetting(mCursor));
        else if (tableName.equals(BASIC_SETTING_TABLE))
            resultData.append(parserDbBasicSetting(mCursor));

        Log.d("db", String.valueOf(resultData));
    }

    private String parserDbInternetSetting(Cursor cursor)
    {
        StringBuilder resultData = new StringBuilder();

        while (cursor.moveToNext()){
            Log.d("db", "count: " + cursor.getColumnCount());
            int id = cursor.getInt(cursor.getColumnIndex(_ID));

            resultData.append(id).append(": ");

            for (int i = 1; i < getDbColumnIndex( WIFI_SSID); i++)
                resultData.append(getDbBoolean(cursor, i)).append(": ");
            for (int i = getDbColumnIndex( WIFI_SSID); i < cursor.getColumnCount(); i++)
                resultData.append(getDbString(cursor, i)).append(": ");

            resultData.append("\n");
        }

        return String.valueOf(resultData);
    }

    private String parserDbBasicSetting(Cursor cursor)
    {
        StringBuilder resultData = new StringBuilder();

        if (cursor != null)
        {
            while (cursor.moveToNext()){
                int id = cursor.getInt(cursor.getColumnIndex(_ID));
                resultData.append(id).append(": ");
                resultData.append(getDbBoolean(cursor, 1)).append(": ");
                resultData.append(getDbBoolean(cursor, 2)).append(": ");
                for (int i = 3; i < cursor.getColumnCount(); i++)
                    resultData.append(getDbString(cursor, i)).append(": ");

                resultData.append("\n");
            }
        }

        return String.valueOf(resultData);
    }

    public void loadDB(String internetId, String basicId)
    {
        //============ Internet Setting ============
        mCursor = getCursor(INTERNET_SETTING_TABLE);  //取得查詢物件Cursor
        loadInternetSetting(Integer.parseInt(internetId));
        //============ Basic Setting ==============
        mCursor = getCursor(BASIC_SETTING_TABLE);
        loadBasicSetting(Integer.parseInt(basicId));
    }

    private void loadInternetSetting( int id)
    {
        if (mCursor != null)
        {
            while (mCursor.moveToNext()) {
                if (id != mCursor.getInt(getDbColumnIndex( _ID)))
                    continue;

                THL.isInternetModeWifi     = getDbBoolean(mCursor, getDbColumnIndex( INTERNET_MODE_WIFI));
                THL.isInternetModeEthernet  = getDbBoolean(mCursor, getDbColumnIndex( INTERNET_MODE_ETHERNET));
                THL.isIpSettingDhcp         = getDbBoolean(mCursor, getDbColumnIndex( IP_SETTING_DHCP));
                THL.isIpSettingStatic        = getDbBoolean(mCursor, getDbColumnIndex( IP_SETTING_STATIC));
                THL.isSecurityWpa2Psk      = getDbBoolean(mCursor, getDbColumnIndex( SECURITY_WPA2_PSK));
                THL.isSecurityWpa2Enterprise = getDbBoolean(mCursor, getDbColumnIndex( SECURITY_WPA2_ENTERPRISE));
                THL.WifiSsid              = getDbString(mCursor, getDbColumnIndex( WIFI_SSID));
                THL.WifiPw               = getDbString(mCursor, getDbColumnIndex( WIFI_PASSWORD));
                THL.WifiEnsIdentity        = getDbString(mCursor, getDbColumnIndex( WIFI_ENTERPRISE_IDENTITY));
                THL.WifiEnsPw            = getDbString(mCursor, getDbColumnIndex( WIFI_ENTERPRISE_PASSWORD));
                THL.InternetIp             = getDbString(mCursor, getDbColumnIndex( INTERNET_IP));
                THL.SubnetMask           = getDbString(mCursor, getDbColumnIndex( SUBNET_MASK));
                THL.Gateway              = getDbString(mCursor, getDbColumnIndex( GATEWAY));
                THL.DnsAddress           = getDbString(mCursor, getDbColumnIndex( DNS_ADDRESS));
            }
        }
        else
            LogManager.writeLogToFile("mCursor is null, loadInternetSetting");

    }

    private void loadBasicSetting(int id)
    {
        if (mCursor != null)
        {
            while (mCursor.moveToNext()){
                if (id != mCursor.getInt(mCursor.getColumnIndex(_ID)))
                    continue;

                THL.isBeaconServer = getDbBoolean(mCursor, getDbColumnIndex(BEACON_SERVER_TYPE));
                THL.isLocatorServer = getDbBoolean(mCursor, getDbColumnIndex(LOCATOR_SERVER_TYPE));
                THL.UploadFreq    = getDbString(mCursor, getDbColumnIndex(UPLOAD_FREQUENCY));
                THL.ScanTime     = getDbString(mCursor, getDbColumnIndex(SCAN_TIME));
                THL.StopScanTime  = getDbString(mCursor, getDbColumnIndex(STOP_SCAN_TIME));
                THL.ServerUrl      = getDbString(mCursor, getDbColumnIndex(SERVER_URL));
                THL.FilterUuid      = getDbString(mCursor, getDbColumnIndex(FILTER_UUID));
                THL.BeaconServer   = getDbString(mCursor, getDbColumnIndex(BEACON_SERVER));
                THL.NtpServer      = getDbString(mCursor, getDbColumnIndex(NTP_SERVER));
                THL.Project        = getDbString(mCursor, getDbColumnIndex(PROJECT));
            }
        }
        else
            LogManager.writeLogToFile("mCursor is null, loadBasicSetting");
    }

    private boolean getDbBoolean(Cursor cursor, int columnIndex)
    {
        return cursor.getInt(columnIndex) > 0;
    }

    private String getDbString(Cursor cursor, int columnIndex)
    {
        return cursor.getString(columnIndex);
    }

    private int getDbColumnIndex(String name)
    {
        if (mCursor != null)
            return mCursor.getColumnIndex(name);
        else
        {
            LogManager.writeLogToFile("mCursor is null, getDbColumnIndex");
            return 0;
        }
    }

    public void saveDb(String internetId, String basicId)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues internetValue = saveInternetSettingValue(internetId);
        ContentValues basicValue = saveBasicSettingValue(basicId);
        db.insertWithOnConflict(INTERNET_SETTING_TABLE,null, internetValue, SQLiteDatabase.CONFLICT_REPLACE);
        db.insertWithOnConflict(BASIC_SETTING_TABLE,null, basicValue, SQLiteDatabase.CONFLICT_REPLACE);
    }

}
