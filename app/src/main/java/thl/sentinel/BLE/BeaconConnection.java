package thl.sentinel.BLE;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.THLight.USBeacon.Writer.Lib.BLEManager;

import thl.sentinel.MyCallback;
import thl.sentinel.data.Constants;
import thl.sentinel.data.THL;
import thl.sentinel.feature.LogManager;

/**
 * Created by 顏培峻 on 2017/12/8.
 */

public class BeaconConnection {

    private String mBeaconMac = "";
    private BLEManager bleManager = null;
    private boolean bRepeat = false;
    private Context context = null;
    private final int SUCCESS = 0;
    private final int FAIL = -1;

    int count = 0;

    //HandlerThread connectht=new HandlerThread("connectBeacon");
    //Handler mHandler;
    private MyCallback myCallback;

    public BeaconConnection(final Context context)
    {
        this.context = context;
        bleManager = new BLEManager(context);

        myCallback = (MyCallback) context;

        // Callback function from library.
        bleManager.setOnBLEListener(new BLEManager.onBLEListener()
        {
            public void onErrorAck(String s) {
                Log.d("BeaconConnection", "error: " + s);
                bleManager.disconnectBLE();
                myCallback.CBupdateAlarmCmdResultAndReConnectToServer(mBeaconMac, FAIL);
                if (!THL.aAlarmBeaconList.isEmpty())
                    startBleConnection();
                showConnectionResultToast("連線失敗, 請重新連線");
            }

            @Override
            public void onActionFinished() {
                Log.d("BeaconConnection", "連線成功");

                if (bRepeat)
                {
                    Log.d("debug", "count:" + count);
                    count++;
                    //mHandler.post(ReconnectBeacon);
                }
                else
                {
                    //mHandler.removeCallbacksAndMessages(null);
                    //connectht.quitSafely();
                    myCallback.CBupdateAlarmCmdResultAndReConnectToServer(mBeaconMac, SUCCESS);
                    if (!THL.aAlarmBeaconList.isEmpty())
                        startBleConnection();
                    showConnectionResultToast("連線成功");
                }

            }
        });
    }

    private void showConnectionResultToast(String msg)
    {
        //Create a new thread to run the loop.
        new Thread(() -> {
            // TODO Auto-generated method stub
            //create loop for toast.
            if (Looper.myLooper() == null)
            {
                Looper.prepare();
            }

            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            Looper.loop();
        }).start();
    }

    public void startBleConnection()
    {
        connectBLE(THL.aAlarmBeaconList.get(0), false);
    }

    public void connectBLE(String mac, boolean repeat ) {

        Log.d("debug", "Connect to " + mac);
        mBeaconMac = mac;
        bRepeat = repeat;
        bleManager.disconnectBLE();

        if (context != null)
        {
            bleManager.connectBLE(mac,
                    Constants.BUZZER_FREQ, Constants.BUZZER_ON, Constants.BUZZER_OFF,
                    THL.buzzerCount);
        }
        else
        {
            Toast.makeText(context, "Beacon 不存在或距離太遠", Toast.LENGTH_SHORT).show();
        }
    }

    public void disconnectBLE()
    {
        //mHandler.removeCallbacksAndMessages(null);
        //connectht.quitSafely();
        bleManager.finish();
        bleManager = null;
    }

    //Reconnect after 5 secs.
    private Runnable ReconnectBeacon= () -> {
        try{
            Log.d("Debug, hebe", "reconnect~.");
            Thread.sleep((Constants.BUZZER_ON+Constants.BUZZER_OFF)*THL.buzzerCount + 5000);
            //connectBLE(mbeacon, bRepeat);

        } catch(Exception e){
            LogManager.saveErrorLogToFile(String.valueOf(e));
            e.printStackTrace();
        }
    };
}
