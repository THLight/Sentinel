package thl.sentinel.Internet;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import thl.sentinel.MyCallback;
import thl.sentinel.data.Constants;
import thl.sentinel.data.THL;
import thl.sentinel.feature.LogManager;
import thl.sentinel.server.GetNtpTimeAndModifySystemTime;

public class InternetConnectionReceiver {

    int wifiScanTimes = 0;
    private Context context = null;
    private ConnectivityManager connectivityManager;
    private GetNtpTimeAndModifySystemTime getNtpTimeAndModifySystemTime = null;
    private MyCallback myCallback = null;
    private NetworkManager networkManager = null;

    public InternetConnectionReceiver(Context context)
    {
        getNtpTimeAndModifySystemTime = new GetNtpTimeAndModifySystemTime();
        myCallback = (MyCallback) context;
        this.context = context;
        registerInternetBroadcastReceiver();
        connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Service.CONNECTIVITY_SERVICE);
        networkManager = new NetworkManager(context);
    }

    private void registerInternetBroadcastReceiver()
    {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        //intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(Constants.CONNECTIVITY_CHANGE);
        context.registerReceiver(mInternetActionReceiver, intentFilter);
    }

    private final BroadcastReceiver mInternetActionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("debug", "action:" + action);
            LogManager.writeLogToFile("Internet action:" + action);

            if (action != null) {
                switch(action)
                {
                    case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                        if(wifiScanTimes <= 20)
                            wifiScanTimes++;
                        else
                        {
                            try {
                                wifiScanTimes = 0;
                                networkManager.disableWifi();
                                Thread.sleep(100);
                                networkManager.openWifi();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        break;
                    case WifiManager.WIFI_STATE_CHANGED_ACTION:
                        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                        LogManager.writeLogToFile("wifistate:" + wifiState);
                        switch (wifiState) {
                            case WifiManager.WIFI_STATE_ENABLED:
                                //wifi已经打开..
                                break;
                        }
                        break;
                    case Constants.CONNECTIVITY_CHANGE:
                        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                        connectivityManager.getActiveNetworkInfo();
                        Log.d("debug", "Network state: " + networkInfo.isConnected() + " type: " + networkInfo.getType());
                        doNetworkChangeAction(networkInfo);
                        break;
                }
            }
        }
    };

    private void doNetworkChangeAction(NetworkInfo networkInfo)
    {
        myCallback.CBshowNetworkConfigurationOnUi();

        if (networkInfo.isConnected())
        {
            getNtpTimeAndModifySystemTime.getNtpServerTime();
            THL.isInternetConnected = true;
            wifiScanTimes = 0;

            switch (networkInfo.getType())
            {
                case ConnectivityManager.TYPE_ETHERNET:
                    if(THL.isInternetModeEthernet)
                    {
                        //internetInitialAction(THL.EthernetMac);
                        internetInitialAction(THL.WifiMac);         //For TSGH, Far East
                    }
                    break;
                case ConnectivityManager.TYPE_WIFI:
                    if(THL.isInternetModeWifi)
                        internetInitialAction(THL.WifiMac);
                    break;
                default:
                    break;
            }
        }
        else
        {
            THL.isInternetConnected = false;
            myCallback.CBstopAnalysisAndUploadReceiverData();

        }
    }

    private void internetInitialAction(String mac)
    {
        THL.SentinelMac = mac;

        if (THL.isLocatorServer)
            myCallback.CBstartAnalysisReceiverData();
        if (THL.isBeaconServer)
            myCallback.CBstartBeaconServerConnection();
    }

    public void unregisterReceiver()
    {
        context.unregisterReceiver(mInternetActionReceiver);
    }
}
