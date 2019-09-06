package thl.sentinel.BLE;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import thl.sentinel.MyCallback;
import thl.sentinel.data.Constants;
import thl.sentinel.data.LocatorBeaconInfo;
import thl.sentinel.data.THL;

/**
 * Created by 顏培峻 on 2018/2/26.
 */

public class iBeaconTransmit {

    private MyCallback myCallback;
    private String sMajorHex = "";
    private String sMinorHex = "";
    private int u32CmdNum = 0;
    private int u32DesNum = 5;
    int u32SrcNum = 1;
    private final int MAX_CMD_SEQUENCE_NUMBER = 63;    //The largest command sequence number is 63. 6bits


    public iBeaconTransmit(Context context)
    {
        myCallback = (MyCallback) context;
    }

    /*BLE 傳送*/
    public void BLEAdvertisement()
    {
        String sColorBinary = "";
        String sTimeBinary = "";
        for (LocatorBeaconInfo beaconList : THL.aLocatorBeaconList) {
            sColorBinary = decimalToBinary(Integer.parseInt(beaconList.color), 3);
            sTimeBinary = decimalToBinary(Integer.parseInt(beaconList.time), 3);
            u32CmdNum = Integer.parseInt(beaconList.seq);
            calculateTxInfo(sColorBinary, sTimeBinary, beaconList.dst);
            beaconList.txMajorHex = sMajorHex;
            beaconList.txMinorHex = sMinorHex;
            //beaconList.command = Constants.SET_BEACON_DATA + sMajorHex + " " + sMinorHex + " D0";
            beaconList.command = Constants.SET_BEACON_INFO + beaconList.uuid + " " + sMajorHex + " " + sMinorHex + " D0";
        }

        new Thread(this::startToSendCommand).start();
    }

    static public String decimalToBinary(int number, int bits)
    {
        String hex = "";
        String format = "%" + bits + "s";
        hex = String.format(format, Integer.toBinaryString(number)).replace(' ', '0'); ;
        return hex;
    }

    //TO calculate the number of major, minor according to the color of light.
    /* SRC 1 send to DST 512 red light 81s, seq 19
       E382
    *   11     100011   10 00 001    0
    *   live     seq        d  s   color  live
    *   0100
    *   00000001 00000000
    *      src            dst
    * */
    private void calculateTxInfo(String sColorBinary, String sTimeBinary, String sDst)
    {
        String major1 = "";
        String major2 = "";
        String minor1 = "";   //src
        String minor2 = "";
        u32DesNum = Integer.parseInt(sDst);   //Destination is taken from user input.
        u32SrcNum = Integer.parseInt(Constants.SENTINEL_ID);   //Destination is taken from user input.

        /*Reset the CMD number if it reaches the maximum number.*/
        if (u32CmdNum == MAX_CMD_SEQUENCE_NUMBER)
            u32CmdNum = Constants.INITIAL_VALUE_1;
        else
            u32CmdNum ++;

        String sCmdBinary = String.format("%6s", Integer.toBinaryString(u32CmdNum)).replace(' ', '0');   //補成六位數, binary string
        String sDesBinary = String.format("%10s", Integer.toBinaryString(u32DesNum)).replace(' ', '0');
        String sSrcBinary = String.format("%10s", Integer.toBinaryString(u32SrcNum)).replace(' ', '0');

        major1 = sTimeBinary.substring(1) + sCmdBinary;    // "10" for life time 2
        major1 = Integer.toHexString(Integer.parseInt(major1, 2)); //Change to 16 進位

        major2 = sDesBinary.substring(0,2) + sSrcBinary.substring(0,2) + sColorBinary + sTimeBinary.substring(0, 1);         //"00" for source,  "0" for the first bit of life time
        major2 = String.format("%2s", Integer.toHexString(Integer.parseInt(major2, 2))).replace(' ', '0'); //補成二位數, 16 進位

        sMajorHex = major1 + major2;

        minor1 = sSrcBinary.substring(2);
        minor2 = sDesBinary.substring(2);
        minor1 = Integer.toHexString(Integer.parseInt(minor1, 2));
        minor2 = Integer.toHexString(Integer.parseInt(minor2, 2));

        minor1 = String.format("%2s", minor1).replace(' ', '0');  //補成二位數
        minor2 = String.format("%2s", minor2).replace(' ', '0');

        sMinorHex = minor1 + minor2;

        Log.d("debug", "iBeaconTransmit, sMajorHex: " + sMajorHex + " MinorHex: " + sMinorHex);
    }

    private void startToSendCommand()
    {
        /*Run 5 cycles.*/
        for (int i = 0 ; i < 5 ; i++)
        {
            ArrayList<LocatorBeaconInfo> copy = new ArrayList<>(THL.aLocatorBeaconList);
            if (!copy.isEmpty())
            {
                for (LocatorBeaconInfo beaconList : copy)
                {
                    try {
                        myCallback.CBsetBeaconCommand(beaconList.command);
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
}
