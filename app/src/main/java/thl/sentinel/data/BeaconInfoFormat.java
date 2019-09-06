package thl.sentinel.data;

import java.util.ArrayList;

public class BeaconInfoFormat  {

    String time = "";
    public String scanned_mac = "";
    public String uuid = "";
    public String major = "";
    public String minor = "";
    public String rssi = "";

    public int index = -1;
    public String sReceiverData = "";
    public String receiverMac = "";
    public String scannedTime = "";
    public int usbType = Constants.TYPE_UNKNOWN;

    public ArrayList<BeaconInfoFormat> scannedBeaconList = new ArrayList<>();

    /* For get serial port data*/
    public  StringBuilder serialData = new StringBuilder();
    public byte[] serialBuff = new byte[2000];
    public boolean isUsbReadable = false;
}
