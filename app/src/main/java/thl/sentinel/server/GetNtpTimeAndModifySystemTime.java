package thl.sentinel.server;

import android.util.Log;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

import thl.sentinel.data.Constants;
import thl.sentinel.data.THL;
import thl.sentinel.feature.LogManager;

public class GetNtpTimeAndModifySystemTime {

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmmss");

    public void  getNtpServerTime()  {

        if (!THL.NtpServer.isEmpty())
        {
            new Thread(() -> {
                Log.d("debug", "Get NTP Server Time");
                long returnTime = 0;
                NTPUDPClient timeClient = new NTPUDPClient();
                timeClient.setDefaultTimeout(Constants.NTP_TIME_OUT_2S);
                InetAddress inetAddress = null;
                try {
                    inetAddress = InetAddress.getByName(THL.NtpServer);
                    TimeInfo timeInfo = timeClient.getTime(inetAddress);
                    //long returnTime = timeInfo.getReturnTime();   //local device time
                    returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();   //server time
                    Date time = new Date(returnTime);
                    Log.d("getCurrentNetworkTime", "Time from " + THL.NtpServer + ": " + time);
                    String date = formatter.format(time);
                    Log.d("debug", date);
                    changeSystemTime(date);
                } catch (IOException e) {
                    LogManager.saveErrorLogToFile(String.valueOf(e));
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void changeSystemTime(String date)
    {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            String command = "date -s "+ date +"\n";
            Log.e("command",command);
            os.writeBytes(command);
            os.writeBytes("exit\n");
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            LogManager.saveErrorLogToFile(String.valueOf(e));
            e.printStackTrace();
        }
    }
}
