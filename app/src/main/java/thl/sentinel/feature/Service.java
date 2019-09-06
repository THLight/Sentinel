package thl.sentinel.feature;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.List;

import thl.sentinel.data.BeaconInfoFormat;
import thl.sentinel.data.THL;
import thl.sentinel.feature.LogManager;

public class Service extends android.app.Service {

    final String Package_ict	= "thl.sentinel";
    static final int MSG_CHECK_ACTIVITY = 1000;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** onCreate will be called once when the service starts. */
    @Override
    public void onCreate()
    {
        super.onCreate();
        mHandler.removeCallbacks(checkAppStatusTask);
        mHandler.postDelayed(checkAppStatusTask, 10000);
        Log.d("debug", "service onCreate");
    }

    private final Handler mHandler = new Handler();

    private Runnable checkAppStatusTask = new Runnable() {
        @Override
        public void run() {

            try {
                if(!isAppLiving(Package_ict))
                {
                    Intent intent= getPackageManager().getLaunchIntentForPackage(Package_ict);
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        Log.d("check", "start app");
                    }
                    else
                        LogManager.saveErrorLogToFile("Restart APP failed.");
                }
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(checkAppStatusTask, 30000);
            }
        }
    };

    boolean isAppLiving(String packageName) {
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        List<ActivityManager.RunningAppProcessInfo> procInfos = null;
        if (activityManager != null) {
            procInfos = activityManager.getRunningAppProcesses();
        }

        if (null != procInfos) {
            for (ActivityManager.RunningAppProcessInfo procInfo : procInfos) {
                //Log.d("check", "procInfo.processName: " + procInfo.processName + " procInfo.importance: " +procInfo.importance);

                if (procInfo.processName.equals(packageName) && ( procInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND)) {
                    return true;
                }
            }
        }

        return false;

    }
}