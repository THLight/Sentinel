package thl.sentinel.feature;

import android.content.Context;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import thl.sentinel.MainActivity;
import thl.sentinel.MyCallback;
import thl.sentinel.data.BeaconInfoFormat;
import thl.sentinel.data.THL;

public class UsbMonitor {

    private MyCallback myCallback = null;
    private ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> scheduledFuture ;   // For cancel the ShowLight task.
    private final int MONITOR_PERIOD = 180000;         // 30mins

    public UsbMonitor(Context context)
    {
        myCallback = (MyCallback) context;
        startUsbMonitor();
    }

    private void startUsbMonitor()
    {
        scheduledFuture = exec.scheduleAtFixedRate(() -> {
            for (BeaconInfoFormat aSReceiver : THL.aReceiverRecordList)
            {
                if (aSReceiver.scannedBeaconList.size() == 0 && !aSReceiver.isUsbReadable) {
                    LogManager.systemMonitorLog();
                    LogManager.saveErrorLogToFile("Mac: " + aSReceiver.receiverMac + " is not available.");
                    myCallback.CBrecheckUsbStatus();
                    //System.exit(0);   // If any usb is not available, usbClose APP.(For debug)
                }
            }
        }, 1000, MONITOR_PERIOD, TimeUnit.SECONDS);
    }

    public void stopUsbMonitor()
    {
        scheduledFuture.cancel(false);
    }

}
