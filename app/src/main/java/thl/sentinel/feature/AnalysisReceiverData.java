package thl.sentinel.feature;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import thl.sentinel.BLE.iBeaconReceive;
import thl.sentinel.BLE.iBeaconTransmit;
import thl.sentinel.data.BeaconInfoFormat;
import thl.sentinel.data.Constants;
import thl.sentinel.data.LocatorBeaconInfo;
import thl.sentinel.data.THL;

public class AnalysisReceiverData {

    private SimpleDateFormat SDF = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.TAIWAN);
    private iBeaconReceive beaconReceive = null;

    public AnalysisReceiverData(Context context)
    {
       beaconReceive = new iBeaconReceive(context);
    }

    /*MAC:  17 byte
    *    UUID: 36 byte
    *    Major: 4 byte
    *    Minor: 4 byte
    *    RSSI: 3 byte
    *    Reference RSSI: 3 byte*/
    public void startAnalysisReceiverDate()
    {
        String time = String.valueOf(SDF.format(new Date()));  // avoid illegal scannedTime.
        for (BeaconInfoFormat aSReceiver : THL.aReceiverRecordList) {

            new Thread(() -> {
                String[] split_line = aSReceiver.sReceiverData.split("\n");
                //Log.d("notExecuteCommand", "aSReceiver: "+ aSReceiver.scannedBeaconList.size() + " " + aSReceiver.receiverMac);
                for (String beaconInfo : split_line)
                {
                    if (beaconInfo.length() == Constants.BEACON_DATA_LENGTH)
                    {
                        BeaconInfoFormat tempBeacon = new BeaconInfoFormat();
                        String[] beaconElements = beaconInfo.split(" ");

                        if (beaconElements.length == Constants.BEACON_ELEMENT_LENGTH)
                        {
                            tempBeacon.scanned_mac = checkScannedMac(beaconElements[0]);

                            if (!tempBeacon.scanned_mac.equals(""))
                            {
                                tempBeacon.uuid = checkScannedUuid(beaconElements[1]);
                                tempBeacon.major = checkScannedMajor(beaconElements[2]);
                                tempBeacon.minor = checkScannedMinor(beaconElements[3]);
                                tempBeacon.rssi = checkScannedRssi(beaconElements[5]);
                                // Check the UUID is the same with selected UUID or not.
                                if(isUuidOk(tempBeacon.uuid))
                                {
                                    tempBeacon.scannedTime = time;
                                    aSReceiver.scannedBeaconList.add(tempBeacon);
                                    //Log.d("test2", "MAC:" + tempBeacon.scanned_mac + " UUID:" + tempBeacon.uuid + " MAJOR:" + tempBeacon.major + " Minor:" + tempBeacon.minor + " RSSI: " + tempBeacon.rssi);
                                }

                                if (!THL.aLocatorBeaconList.isEmpty())
                                    beaconReceive.checkLocatorBeaconAck(tempBeacon);   // For Locator Beacon.
                            }
                        }
                    }
                }
            }).start();

        }
    }

    public String checkScannedMac(String mac) {
        mac = mac.trim();
        if (FormatCheck.macFormatCheck(mac))
            return mac;
        else return "";
    }

    public String checkScannedUuid(String uuid) {
        uuid = uuid.trim();
        if (FormatCheck.uuidFormatCheck(uuid))
            return uuid;
        else return "";
    }

    public String checkScannedMajor(String major) {
        major = major.trim();
        if(FormatCheck.stringHexFormatCheck(major))
            return String.valueOf(Integer.parseInt(major, 16));
        else
            return "";
    }

    public String checkScannedMinor(String minor) {
        minor = minor.trim();
        if(FormatCheck.stringHexFormatCheck(minor))
            return String.valueOf(Integer.parseInt(minor, 16));
        else
            return "";
    }

    @NonNull
    @org.jetbrains.annotations.Contract(pure = true)
    public String checkScannedRssi(String rssi) {
        rssi = rssi.trim();
        if (FormatCheck.integerFormatCheck(rssi))
            return rssi;
        else return "-100";

    }

    public boolean isUuidOk(String uuid) {
        if (uuid.length() == Constants.UUID_LENGTH)
        {
            if (THL.FilterUuid != null)
                return THL.FilterUuid.isEmpty() || THL.FilterUuid.equals(uuid.trim());
            else
                return true;
        }
        else
            return false;
    }

}
