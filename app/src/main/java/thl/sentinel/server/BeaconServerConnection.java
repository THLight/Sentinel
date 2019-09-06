package thl.sentinel.server;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

import thl.sentinel.BLE.BeaconConnection;
import thl.sentinel.BLE.BeaconScan;
import thl.sentinel.BLE.iBeaconTransmit;
import thl.sentinel.MyCallback;
import thl.sentinel.data.Constants;
import thl.sentinel.data.LocatorBeaconInfo;
import thl.sentinel.data.THL;
import thl.sentinel.feature.FormatCheck;
import thl.sentinel.feature.LogManager;

public class BeaconServerConnection {

    private final String ENABLE = "1";
    private int count = 0;
    private int serverRetryCount = Constants.INITIAL_VALUE_1;
    private int beaconServerReConnectTimes = 0;
    private int commandSequence = 0;
    private int serverUploadTime;
    private final Handler handler = new Handler();
    private BeaconConnection beaconConnection = null;
    private MyCallback myCallback;
    private iBeaconTransmit beaconTransmit = null;
    private final String GET_ALL_DEVICE_INFO_API = "http://" + THL.BeaconServer + "api/get_all_device_info";
    private final String GET_DEVICE_INFO_BY_PARAMETER_API ="http://" + THL.BeaconServer + "api/get_device_info_by_parameter";
    private final String UPDATE_DEVICE_BY_MAC_API = "http://" + THL.BeaconServer + "api/update_by_mac";
    private Timer timer = new Timer();

    public BeaconServerConnection(Context context)
    {
        serverUploadTime = Integer.parseInt(THL.UploadFreq);
        beaconConnection = new BeaconConnection(context);
        myCallback = (MyCallback) context;
        beaconTransmit = new iBeaconTransmit(context);
        BeaconScan.setBluetooth(true);
    }

    public void startBeaconServeConnection()
    {
        stopBeaconServerConnection();
        //Log.d("debug", "startBeaconServeConnection, serverRetryCount: " + serverRetryCount);
        getDataFromServerRegularTask.run();
    }

    private Runnable getDataFromServerRegularTask = new Runnable() {
        @Override
        public void run() {
            try {
                new Thread(() -> {
                    //Log.d("debug", "getDataFromServerRegularTask, serverRetryCount: " + serverRetryCount);
                    getDataFromServer();
                }).start();
            } finally {
                handler.postDelayed(getDataFromServerRegularTask, serverUploadTime);  // Get data for every serverUploadTime.
            }
        }
    };

    private void getDataFromServer() {

        //Log.d("debug", "getDataFromServer, serverRetryCount: " + serverRetryCount);
        if (!THL.BeaconServer.isEmpty())
        {
            String response = "NULL";
            if (serverRetryCount >= Math.pow(Constants.NETWORK_ERROR_RETRY_BASE, beaconServerReConnectTimes))
            {
                String URL = GET_DEVICE_INFO_BY_PARAMETER_API + jointParameterForGetApi(Constants.SENTINEL_ID_CELL, Constants.SENTINEL_ID);
                response = httpServerConnection(URL, Constants.GET);
                checkServerData(jsonParser(response));
                executeServerCommands(); //Start to alarm beacon or light beacon.
                serverRetryCount = Constants.INITIAL_VALUE_1;
            }
            else
                serverRetryCount++;

            // Set icon to green in a period.
            if (count >= Constants.TIME_TO_LOG && serverRetryCount == Constants.INITIAL_VALUE_1) {
                LogManager.writeLogToFile(response);
                myCallback.CBshowBeaconServerStatusOnUi(response, true);
                count = Constants.INITIAL_VALUE_1;
            }
            else
                count ++;
        }
    }

    private String jointParameterForGetApi(String parameter, String value)
    {
        return "?parameter="+ parameter + "&value=" + value;
    }

    private String httpServerConnection(String api, String method)
    {
        String response = "";
        try {
            URL url = new URL(api);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(method);
            urlConnection.setRequestProperty(Constants.CONTENT_TYPE, "application/json");
            urlConnection.setRequestProperty(Constants.DATAQUERY_KEY, Constants.QUERY_KEY);
            urlConnection.setConnectTimeout(Constants.CONNECT_TIME_OUT);
            urlConnection.setReadTimeout(Constants.READ_TIME_OUT);

            if (method.equals(Constants.POST))
                setHttpPostData(urlConnection);

            int result = urlConnection.getResponseCode();
            if (result == HttpsURLConnection.HTTP_OK) {
                String line;
                beaconServerReConnectTimes = 0;
                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            }
            else {
                response= String.valueOf(result);
            }

            THL.isBeaconServerLive = true;
            urlConnection.disconnect();
            Log.d("debug, Beacon Server", "result: " + result + " count:" + count + " retry: " + serverRetryCount);
        } catch (Exception e) {
            e.printStackTrace();
            response = String.valueOf(e);
            networkExceptionHandle(response);
        }

        return response;
    }

    private void setHttpPostData(HttpURLConnection urlConnection)
    {
        try {
            JSONObject uploadData = new JSONObject(THL.httpPostData);
            OutputStream out = urlConnection.getOutputStream();
            byte[] outputBytes = uploadData.toString().getBytes(StandardCharsets.UTF_8);
            out.write(outputBytes);
            out.close();
            THL.httpPostData.clear();
        } catch (Exception e) {
            e.printStackTrace();
            LogManager.saveErrorLogToFile(String.valueOf(e));
        }
    }

    private void networkExceptionHandle(String error)
    {
        if (beaconServerReConnectTimes < Constants.MAX_RETRY_COUNT) // the maximum is 10
            beaconServerReConnectTimes ++;
        LogManager.saveErrorLogToFile(String.valueOf(error));
        Log.d("debug, Beacon Server", String.valueOf(error) + " " + beaconServerReConnectTimes);
        myCallback.CBshowBeaconServerStatusOnUi(error, false);
        THL.isBeaconServerLive = false;
    }

    private String jsonParser(String data)
    {
        if (!FormatCheck.jsonFormatCheck(data))
            return "";

        try
        {
            JSONObject jObjectServerResponse  = new JSONObject(data);
            String code   = jObjectServerResponse.getString("code");
            String result = jObjectServerResponse.getString("result");

            if(code.equals(String.valueOf(HttpURLConnection.HTTP_OK)))
                return  result;
            else
                LogManager.saveErrorLogToFile("Beacon Server response Code: " + code);

        }
        catch (JSONException e)
        {
            e.printStackTrace();
            LogManager.saveErrorLogToFile(String.valueOf(e));
        }

        return "";
    }

    private void checkServerData (String allElement )
    {
        if (allElement.equals(""))
            return;
        try
        {
            JSONArray jArrayAllElement = new JSONArray(allElement);
            int templeSeq = commandSequence;
            for(int i = 0 ; i < jArrayAllElement.length() ; i++)
            {
                JSONObject jObjectEachElement = jArrayAllElement.getJSONObject(i);

                if (jObjectEachElement.getString(Constants.EXECUTE_COMMAND_CELL).equals(ENABLE))
                    addBeaconList(jObjectEachElement, templeSeq);

                //Log.d("DEBUG", "mac: " +beaconData.scanned_mac + " major: " + beaconData.major );
            }
        } catch (JSONException e) {
        e.printStackTrace();
        LogManager.saveErrorLogToFile(String.valueOf(e));
        }
    }

    private void addBeaconList (JSONObject jObjectEachElement, int templeSeq)
    {
        try {
            String MAC = jObjectEachElement.getString("mac_addr");
            String seq = jObjectEachElement.getString(Constants.SEQUENCE_CELL);
            if (isCommandAvailable(seq, templeSeq)) {

                LocatorBeaconInfo locatorBeaconInfo = new LocatorBeaconInfo();
                String count = jObjectEachElement.getString(Constants.BEACON_ACTION_LENGTH_CELL);

                if (FormatCheck.integerFormatCheck(count)) {
                    THL.buzzerCount = Integer.parseInt(count);
                    locatorBeaconInfo.time = count;
                }

                if (FormatCheck.macFormatCheck(MAC))
                    THL.aAlarmBeaconList.add(MAC);
                else if (FormatCheck.integerFormatCheck(MAC))  // Beacon ID in MAC cell.
                {
                    //Collect data from Server.
                    locatorBeaconInfo.uuid = jObjectEachElement.getString("uuid");
                    locatorBeaconInfo.dst = MAC;
                    locatorBeaconInfo.color = jObjectEachElement.getString("extra4");
                    locatorBeaconInfo.seq = jObjectEachElement.getString(Constants.SEQUENCE_CELL);

                    /*================================== Modify ============================================================*/
                  //  if (locatorBeaconInfoCheck(locatorBeaconInfo))
                   //     THL.aLocatorBeaconList.add(locatorBeaconInfo);
                }
            }
          //  else // Seq is not available.
           //     updateBeaconDataInServer(MAC, "-2",templeSeq);
        } catch (JSONException e) {
            e.printStackTrace();
            LogManager.saveErrorLogToFile(String.valueOf(e));
        }
    }

    /*Check the command is new.*/
    private boolean isCommandAvailable(String seq, int templeSeq)
    {
        if (FormatCheck.integerFormatCheck(seq)) {
            if (Integer.parseInt(seq) != templeSeq) {
                commandSequence = Integer.parseInt(seq);
                return true;
            }
        }
        return false;
    }

    private boolean locatorBeaconInfoCheck (LocatorBeaconInfo locatorBeaconInfo)
    {
        if (Integer.parseInt(locatorBeaconInfo.dst) > Constants.MAX_BEACON_ID)
            return false;
        else
        return FormatCheck.integerFormatCheck(locatorBeaconInfo.color);
    }

    private void executeServerCommands() {
        if(!THL.aAlarmBeaconList.isEmpty())
        {
            Log.d("debug, Beacon Server", "alarm size:" + THL.aAlarmBeaconList.size() + " " + commandSequence);
            stopBeaconServerConnection();
            beaconConnection.startBleConnection();
        }
        else if(!THL.aLocatorBeaconList.isEmpty())
        {
            Log.d("debug, Beacon Server", "locator size:" + THL.aLocatorBeaconList.size() + " " + commandSequence);
            stopBeaconServerConnection();
            beaconTransmit.BLEAdvertisement();
            timeOutToCancelCommand();
        }
    }

    private void timeOutToCancelCommand()
    {
        cancelAndResetTimer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d("debug", "timeOutToCancelCommand, " + THL.aLocatorBeaconList.size());
                LogManager.writeLogToFile("timeOutToCancelCommand, " + THL.aLocatorBeaconList.size());
                ArrayList<LocatorBeaconInfo> copy = new ArrayList<>(THL.aLocatorBeaconList);
                if (!copy.isEmpty())
                {
                    for (LocatorBeaconInfo locatorBeaconInfo : copy)
                        myCallback.CBupdateCmdResultAndReConnectToServer(locatorBeaconInfo.dst, -1, locatorBeaconInfo.seq);
                }
            }
        }, 45000);
    }

    private void cancelAndResetTimer()
    {
        timer.cancel();
        timer.purge();
        timer = null;
        timer = new Timer();
    }

    public void updateBeaconDataInServer(String mac, String result, int seq)
    {
        if (seq == 0) //For CBupdateAlarmCmdResultAndReConnectToServer pass seq 0.
            seq = commandSequence;
        THL.httpPostData.put("mac_addr", mac);
        THL.httpPostData.put(Constants.EXECUTE_COMMAND_CELL, result);
        THL.httpPostData.put(Constants.LAST_COMPLETED_DATE_CELL, "date");
        THL.httpPostData.put("extra7", THL.InternetIp + "/" + Constants.SENTINEL_ID);  // update sentinel info.
        Log.d("debug", "updateBeaconDataInServer, ID: " + mac + " result: " + result + " " +seq );
        LogManager.writeLogToFile("ID: " + mac + " result: " + result + " " +seq);

        int finalSeq = seq;
        new Thread(() -> {
            String response = httpServerConnection(UPDATE_DEVICE_BY_MAC_API, Constants.POST);
            myCallback.CBshowBeaconServerStatusOnUi(response  + " mac: " + mac + " Seq: " + finalSeq, Integer.parseInt(result) > -1); //Fail:-1
        }).start();
    }

    public boolean notExecuteCommand(String id)
    {
        String URL = GET_DEVICE_INFO_BY_PARAMETER_API + jointParameterForGetApi("mac_addr", id);
        String response = httpServerConnection(URL, Constants.GET);
        String data = jsonParser(response);

        if (!data.isEmpty())
        {
            try
            {
                JSONArray jArrayAllElement = new JSONArray(data);
                JSONObject jObjectEachElement = jArrayAllElement.getJSONObject(0);

                if (jObjectEachElement.getString(Constants.EXECUTE_COMMAND_CELL).equals(ENABLE))
                    return true;
            } catch (JSONException e) {
                e.printStackTrace();
                LogManager.saveErrorLogToFile(String.valueOf(e));
            }
        }
        return false;
    }

    public void stopBeaconServerConnection()
    {
        handler.removeCallbacks(getDataFromServerRegularTask);
        serverRetryCount = Constants.INITIAL_VALUE_1;
        count = Constants.INITIAL_VALUE_1;
        beaconServerReConnectTimes = 0;
        THL.isBeaconServerLive = false;
    }

}
