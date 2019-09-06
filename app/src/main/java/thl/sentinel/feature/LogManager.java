package thl.sentinel.feature;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import android.os.Handler;

import thl.sentinel.MyCallback;
import thl.sentinel.data.BeaconInfoFormat;
import thl.sentinel.data.Constants;
import thl.sentinel.data.THL;

public class LogManager implements Thread.UncaughtExceptionHandler{

    private static MyCallback myCallback = null;
    private static DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.TAIWAN);
    private Handler mHandler = new Handler();
    private final static int calledMethod = 1;

    public LogManager(Context context)
    {
        myCallback = (MyCallback) context;

        File file= new File(Constants.STORE_PATH);
        if(!file.exists())
        {
            if(!file.mkdirs())
            {
                Log.d("Log", "Create folder(" + Constants.STORE_PATH + ") failed.");
            }
        }

        Thread.setDefaultUncaughtExceptionHandler(this);
        checkAndDeleteLogFileSize.run();
    }

    private Runnable checkAndDeleteLogFileSize = new Runnable() {
        @Override
        public void run() {
            try {
                cleanLog(); //this function can change value of mInterval
            } finally {
                mHandler.postDelayed(checkAndDeleteLogFileSize, Constants.CHECK_FILES_SIZE_PERIOD);
            }
        }
    };

    public static void writeLogToFile(String msg)
    {
        File saveFile = new File(Constants.STORE_PATH, Constants.LOG_FILE);

        FileOutputStream outStream = null;
        Date curDate = new Date(System.currentTimeMillis()) ; // 獲取當前時間

        try {
            outStream = new FileOutputStream(saveFile, true);
            outStream.write(df.format(curDate).getBytes());         //print time
            outStream.write("  ".getBytes());
            outStream.write(msg.getBytes());
            outStream.write(" \n".getBytes());
            outStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        saveErrorLogToFile(getErrorMsgString(throwable) + getMethodName(calledMethod));
    }

    private String getErrorMsgString(Throwable throwable)
    {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        throwable.printStackTrace(printWriter);
        Throwable cause = throwable.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        return writer.toString();
    }

    public static boolean saveErrorLogToFile(String errorMsg)
    {
        Date curDate = new Date(System.currentTimeMillis());
        String SavePath = Constants.STORE_PATH + Constants.ERROR_LOG_FILE;

        Log.d("Log","crash:" + errorMsg);

        try {
            FileWriter fw = new FileWriter(SavePath,true);
            BufferedWriter bw = new BufferedWriter(fw); //將BufferedWeiter與FileWrite物件做連結

            bw.write(df.format(curDate)+":" + errorMsg + getMethodName(calledMethod));
            bw.newLine();
            bw.close();
            return true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get the method name for a depth in call stack. <br />
     * Utility function
     * @param depth depth in the call stack (0 means current method, 1 means call method, ...)
     * @return method name
     */
    public static String getMethodName(final int depth)
    {
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();

        if (ste[ste.length - 1 - depth].getMethodName().contains("run"))
            return getMethodName(depth + 1);
        else
            return ste[ste.length - 1 - depth].getMethodName();
    }

    private void cleanLog()
    {
        File directory = new File(Constants.STORE_PATH);
        File[] files = directory.listFiles();

        for (File file : files)
        {
            if (file.length() > Constants.ONE_MB)
                file.delete();
            else
                writeLogToFile(file.getName() + ": " +file.length());
        }
    }

    public static void systemMonitorLog()
    {
        String info = "";

        for(BeaconInfoFormat r : THL.aReceiverRecordList)
        {
            info = info + r.receiverMac + ": " + r.scannedBeaconList.size() + ", ";
        }

        writeLogToFile(info);
        Log.d("Log", "Receiver Info: " + info);

    }
}
