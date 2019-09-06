package thl.sentinel.BLE;

import android.content.Context;
import android.util.Log;

import thl.sentinel.MyCallback;
import thl.sentinel.data.BeaconInfoFormat;
import thl.sentinel.data.Constants;
import thl.sentinel.data.LocatorBeaconInfo;
import thl.sentinel.data.THL;
import thl.sentinel.server.BeaconServerConnection;

public class iBeaconReceive {

    private MyCallback myCallback = null;
    private BeaconServerConnection beaconServerConnection = null;

    public iBeaconReceive(Context context)
    {
        myCallback = (MyCallback) context;
        beaconServerConnection = new BeaconServerConnection(context);
    }

    public void checkLocatorBeaconAck(BeaconInfoFormat tempBeacon)
    {

        if(isAckUuid(tempBeacon.uuid))
        {
            String src = getBeaconSourceFromAck(tempBeacon.major, tempBeacon.minor);
            String seq = getCommandSeqFromAck(tempBeacon.major);
            Log.d("debug", "src:" + src);
            //Server command executed by other Sentinel, remove this ID from list.
            if(!isAckDst(tempBeacon.major, tempBeacon.minor))
                removeBeacon(src);

            myCallback.CBupdateCmdResultAndReConnectToServer(src, 0, seq);
        }
        //Log.d("debug", "Major: " + tempBeacon.major + ", Minor: " + tempBeacon.minor);
/*
        if (isAckDst(tempBeacon.major, tempBeacon.minor, tempBeacon.uuid))
        {
            String src = getBeaconSourceFromAck(tempBeacon.major, tempBeacon.minor);
            String seq = getCommandSeqFromAck(tempBeacon.major);
            Log.d("debug", "src:" + src);
            myCallback.CBupdateCmdResultAndReConnectToServer(src, 0, seq);
        }*/
    }

    private boolean isAckDst(String major, String minor)
    {
        String majorBinary = iBeaconTransmit.decimalToBinary(Integer.parseInt(major), Constants.HEX);
        String minorBinary = iBeaconTransmit.decimalToBinary(Integer.parseInt(minor), Constants.HEX);
        String dst = majorBinary.substring(8, 10) + minorBinary.substring(8, 16);

        return Constants.SENTINEL_ID.equals(String.valueOf(Integer.parseInt(dst, 2)));
    }

    private String getBeaconSourceFromAck(String major, String minor)
    {
        String majorBinary = iBeaconTransmit.decimalToBinary(Integer.parseInt(major), Constants.HEX);
        String minorBinary = iBeaconTransmit.decimalToBinary(Integer.parseInt(minor), Constants.HEX);
        String src = majorBinary.substring(10, 12) + minorBinary.substring(0, 8);

        return String.valueOf(Integer.parseInt(src, 2));
    }

    private String getCommandSeqFromAck(String major)
    {
        String majorBinary = iBeaconTransmit.decimalToBinary(Integer.parseInt(major), Constants.HEX);
        String seq = majorBinary.substring(2, 8);

        return String.valueOf(Integer.parseInt(seq, 2));
    }

    private boolean isAckUuid(String uuid)
    {
        //The last byte of ACK UUID is 6.
        if (!uuid.substring(uuid.length() - 1).equals("6"))
            return false;
        else
        {
            //The last byte of CMD UUID is 5.
            uuid = uuid.substring(0,  uuid.length() - 1) + "5";

            for (LocatorBeaconInfo beacon:THL.aLocatorBeaconList)
            {
                if (beacon.uuid.equals(uuid))
                    return true;
            }

            return false;
        }
    }

    public static boolean removeBeacon(String dst){
        for(LocatorBeaconInfo beacon:THL.aLocatorBeaconList){
            if(beacon.dst.equals(dst)){
                THL.aLocatorBeaconList.remove(beacon);
                return true;
            }
        }
        return false;
    }

}