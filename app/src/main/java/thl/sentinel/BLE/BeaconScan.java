package thl.sentinel.BLE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.THLight.USBeacon.App.Lib.iBeaconData;

import java.util.Objects;
import thl.sentinel.data.BeaconInfoFormat;

/**
 * Created by 顏培峻 on 2017/12/7.
 */

public class BeaconScan implements BluetoothAdapter.LeScanCallback{

    static final int SCAN_PERIOD = 60000;                   //60s restart BT.
    String sMacAddress = "";
    boolean bPauseRecordRssi = false;

    // Initializes Bluetooth adapter.
    private BluetoothManager bluetoothManager;
    static private BluetoothAdapter mBluetoothAdapter;

    HandlerThread BTht=new HandlerThread("BT");
    Handler mHandler;
    Context mContext;

    public BeaconScan(Context context)
    {
        mContext = context;
        setBluetooth(true);
        bluetoothManager = (BluetoothManager)
                context.getSystemService(Context.BLUETOOTH_SERVICE);

        BTht.start();
        mHandler=new Handler(BTht.getLooper());
        mHandler.post(RestartBT);

        //Set a filter to only receive bluetooth state changed events.
        //IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        //context.registerReceiver(mReceiver, filter);
    }

    static public boolean setBluetooth(boolean enable) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = mBluetoothAdapter.isEnabled();
        if (enable && !isEnabled) {
            return mBluetoothAdapter.enable();
        }
        else if(!enable && isEnabled) {
            return mBluetoothAdapter.disable();
        }
        // No need to change bluetooth state
        return true;
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            Log.d("debug", "action:" + action);

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (bluetoothState) {
                    case BluetoothAdapter.STATE_ON:
                        break;
                }
            }
        }
    };

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

        iBeaconData iB = iBeaconData.generateiBeacon(scanRecord);

        if(iB == null)
        {
             //Log.d("UIMain","There is no beacon!");
        }
        else
        {
            String strName = iB.major+
                    "." +
                    iB.minor;

            if ( rssi>-80)// && iB.major == THLAPP.TARGET_MAJOR)
            {

                sMacAddress = device.getAddress();
                Log.d("Debug, Main", "Name:" + strName + " , Mac Address: " + device.getAddress());
                //Log.d("Debug, Main", "RSSI:" + rssi);

            }
        }

    }

    private Runnable RestartBT=new Runnable(){
        //工作內容寫進run(),繁雜的工作可以給另一個特約工人做
        public void run(){
            try{
                Log.d("Debug, BeaconScan", "Stop BLE scan.");
                mBluetoothAdapter.stopLeScan(BeaconScan.this);
                Thread.sleep(1000);
                scanLeDevice(true);

            } catch(Exception e){
                e.printStackTrace();
            }
        }
    };

    // 掃描藍芽裝置, every 60 seconds restart BLE.
    public void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(() -> mHandler.post(RestartBT), SCAN_PERIOD);

            Log.d("Debug, BeaconScan", "Start BLE scan.");
            mBluetoothAdapter.startLeScan(this);

        } else {
            mHandler.removeCallbacksAndMessages(null);     // Remove all mhandler message(include postdelay)
            BTht.quitSafely();
            mBluetoothAdapter.stopLeScan(this);
            Log.d("Debug, BeaconScan", "Stop BLE scan. scanLeDevice is false");

        }
    }

    public void unregisterReceiver()
    {
        mContext.unregisterReceiver(mReceiver);
    }

}
