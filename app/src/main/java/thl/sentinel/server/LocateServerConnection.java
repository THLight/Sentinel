package thl.sentinel.server;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

import thl.sentinel.MyCallback;
import thl.sentinel.data.BeaconInfoFormat;
import thl.sentinel.data.Constants;
import thl.sentinel.data.THL;
import thl.sentinel.feature.LogManager;

public class LocateServerConnection {
    private int serverUploadTime;
    private int count = Constants.INITIAL_VALUE_1;
    private int serverRetryCount = Constants.INITIAL_VALUE_1;
    private int locatorServerReConnectTimes = 0;
    private ArrayList<BeaconInfoFormat> copy;
    private final Handler handler = new Handler();
    private MyCallback myCallback;

    public LocateServerConnection(Context context)
    {
        serverUploadTime = Integer.parseInt(THL.UploadFreq);
        uploadDataToServerRegularTask.run();
        myCallback = (MyCallback) context;
    }

    private Runnable uploadDataToServerRegularTask = new Runnable() {
        @Override
        public void run() {
            try {
                new Thread(() -> {
                    uploadDataToServer();
                }).start();
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                handler.postDelayed(uploadDataToServerRegularTask, serverUploadTime);  // Upload data for every seconds.
            }
        }
    };


    private void uploadDataToServer() {
        try {
            String response = "no response";

            if (!THL.ServerUrl.equals("")) {

                if (count == Constants.TIME_TO_LOG) {
                    count = Constants.INITIAL_VALUE_1;
                    LogManager.systemMonitorLog();
                }

                String Json = concatenateDataToJsonFormat();
                String V = "v=" + calculateSecureDataV(Json);  //security key
                Json = "data=" + Json;
                //largeLog(Json);
                //Log.d("debug", V + " " + Json.length());

                if (serverRetryCount == Math.pow(Constants.NETWORK_ERROR_RETRY_BASE, locatorServerReConnectTimes)) {
                    // Transfer two data by "&"
                    response = httpPostConnection(Json + "&" + V);
                    serverRetryCount = Constants.INITIAL_VALUE_1;
                    Log.d("debug", "Response:" + response);

                    if (response.contains(Constants.ILLEGAL)) {
                        largeLog(Json);
                        Log.d("debug", V + " " + Json.length());
                    }
                }
                else
                    serverRetryCount++;

                if (count == Constants.INITIAL_VALUE_1) {
                    LogManager.writeLogToFile("Response: " + response);
                    if (serverRetryCount == Constants.INITIAL_VALUE_1)
                        myCallback.CBshowLocatorServerStatusOnUi(response, true);
                }
                count ++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogManager.saveErrorLogToFile(String.valueOf(e));
        }
    }

    private String httpPostConnection(String uploadData)
    {
        String response = "";
        try {
                URL url = new URL(THL.ServerUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod(Constants.POST);
                urlConnection.setRequestProperty(Constants.CONTENT_TYPE, "application/x-www-form-urlencoded");
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.setConnectTimeout(Constants.CONNECT_TIME_OUT);
                urlConnection.setChunkedStreamingMode(0);
                urlConnection.setReadTimeout(Constants.READ_TIME_OUT);

                OutputStream out = urlConnection.getOutputStream();
                byte[] outputBytes = uploadData.getBytes(StandardCharsets.UTF_8);
                out.write(outputBytes);
                out.close();

                int result = urlConnection.getResponseCode();
                if (result == HttpsURLConnection.HTTP_OK) {
                    String line;
                    locatorServerReConnectTimes = 0;
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response += line;
                    }
                }
                else {
                 response= String.valueOf(result);
                }

                THL.isLocatorServerLive = true;
                urlConnection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            response = String.valueOf(e);
            networkExceptionHandle(response);
        }
        return response;
    }

    private void networkExceptionHandle(String error)
    {
        if (locatorServerReConnectTimes < Constants.MAX_RETRY_COUNT) // the maximum is 10
            locatorServerReConnectTimes ++;
        LogManager.saveErrorLogToFile(String.valueOf(error) + "retry: " + locatorServerReConnectTimes + " httpPostConnection()");
        myCallback.CBshowLocatorServerStatusOnUi(String.valueOf(error) + " retry: " + locatorServerReConnectTimes, false);
        THL.isLocatorServerLive = false;
    }

    /*Json format
    * {"project":"Kuozui_Dock",  "intervals":"1000",  "version":"1.5.1",  "mac":"94:A1:A2:CA:71:54",
         "bundle":{“distance”:12535}, # extra Information, may not exist
         "receivers":[{  "mac":"94:A1:A2:CA:71:54",  # Receiver MAC
                               "scanned":[...]  # Scanned beacon list by receiver
                          }],
                             "scanned":[  # Scanned beacon list by senti
                            {"mac":"FF:FF:FF:FF:FF:FF",  # Beacon MAC
                             "rssi":"0", # Beacon RSSI
                             "uuid":"...", # iBeacon UUID
                             "major":"0", # iBeacon Major
                             "minor":"0", # iBeacon Minor
                             "scannedTime":"2017-03-29 11:48:09.904", # time be scanned
                            }],
         "sentTime":"2017-03-29 11:48:09.906", # The time senti called API
        }
        */
    private String concatenateDataToJsonFormat() {
        JSONObject basicInfo = new JSONObject();
        JSONArray allReceiverScanData = new JSONArray();

        try {
            basicInfo.put("project", THL.Project);
            basicInfo.put("intervals", THL.UploadFreq);
            basicInfo.put("version", Constants.VERSION);
            basicInfo.put("mac", THL.SentinelMac);

            for (BeaconInfoFormat aSReceiver : THL.aReceiverRecordList)
            {
                JSONArray eachReceivedDataArray = new JSONArray();
                JSONObject eachReceiverScanData = new JSONObject();
                eachReceiverScanData.put("mac", aSReceiver.receiverMac);
                //ArrayList<BeaconInfoFormat> copy = new ArrayList<>(aSReceiver.scannedBeaconList);
                copy = (ArrayList<BeaconInfoFormat>) aSReceiver.scannedBeaconList.clone();

                aSReceiver.scannedBeaconList.clear();
                //Log.d("notExecuteCommand", "size: " + aSReceiver.scannedBeaconList.size() + " " + copy.size());

                for (BeaconInfoFormat b : copy)//aSReceiver.scannedBeaconList)
                {
                    JSONObject receivedData = new JSONObject();
                    if (b != null) {
                        receivedData.put("mac", b.scanned_mac);
                        receivedData.put("rssi", b.rssi);
                        receivedData.put("uuid", b.uuid);
                        receivedData.put("major", b.major);
                        receivedData.put("minor", b.minor);
                        receivedData.put("scannedTime", b.scannedTime);

                        eachReceivedDataArray.put(receivedData);
                    }
                    else {
                        Log.d("debug", "b is null.");
                        LogManager.saveErrorLogToFile("b is null, copy size:  " + copy.size());
                    }
                    //Log.d("notExecuteCommand", eachReceivedDataArray.toString());
                }
                eachReceiverScanData.put("scanned", eachReceivedDataArray);
                allReceiverScanData.put(eachReceiverScanData);
                copy.clear();
                //Log.d("notExecuteCommand", eachReceiverScanData.toString());
            }

            basicInfo.put("receivers", allReceiverScanData);
            basicInfo.put("sentTime",Constants.SDF.format(new Date()));

        } catch (JSONException e) {
            LogManager.saveErrorLogToFile(String.valueOf(e));
            Log.d("error", "JSONException: " + String.valueOf(e));
            e.printStackTrace();
        }catch (Exception e) {
            LogManager.saveErrorLogToFile(String.valueOf(e) + " Json function");
            //System.exit(0);
        }

        return basicInfo.toString();
    }

    private String calculateSecureDataV(String json)
    {
        String result = "";
        result = md5("Sentinel_" + THL.Project.toLowerCase() + "_" + json + "_THLight");
        return result;
    }

    private String md5(final String s) {

        if (s.equals(""))
            return "";

        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance(Constants.MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            LogManager.saveErrorLogToFile(String.valueOf(e));
            e.printStackTrace();
        }
        return "";
    }

    private void largeLog(String content) {
        if (content.length() > Constants.MAX_LOG_SIZE) {
            Log.d("debug", "Json: " + content.substring(0, Constants.MAX_LOG_SIZE));
            largeLog( content.substring(Constants.MAX_LOG_SIZE));
        } else {
            Log.d("debug", content);
        }
    }

    public void stopUploadDataToServerTask()
    {
        handler.removeCallbacks(uploadDataToServerRegularTask);
        serverRetryCount = Constants.INITIAL_VALUE_1;
        count = Constants.INITIAL_VALUE_1;
        locatorServerReConnectTimes = 0;
        THL.isLocatorServerLive = false;
    }
}
