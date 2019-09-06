package thl.sentinel.feature;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import thl.sentinel.MyCallback;
import thl.sentinel.data.BeaconInfoFormat;
import thl.sentinel.data.Constants;
import thl.sentinel.data.THL;

public class UsbSerialPortManager {

    private int u32CheckReceiverRetryCount = 0;
    private Handler mHandler;
    private final String ACTION_USB_PERMISSION = "com.thlight.Sentinal.USB_PERMISSION";

    private static final int HOST_TO_DEVICE_REQUEST_TYPE = 	0x41;

    /* Request codes */
    private static final int IFC_ENABLE =					0x00;
    /* IFC_ENABLE */
    private static final int UART_ENABLE =					0x0001;
    private static final int UART_DISABLE =					0x0000;

    private PendingIntent mPermissionIntent = null;
    private UsbDevice usbDevice = null;
    private Context context = null;
    private UsbManager mUsbManager = null;

    private UsbDeviceConnection[] mUsbConnections = null;
    private UsbEndpoint[] Endpoint_outs 	= null;
    private UsbEndpoint[] Endpoint_ins  	= null;

    private int usbDeviceNumber = 0;
    private String DeviceInfoString = "";
    private boolean usbFlag = false;
    private byte[] getSerialBuf = new byte[1000];

    private MyCallback myCallback = null;

    public UsbSerialPortManager(Context context)
    {
        Log.d("usbSerialPortManager", "usbSerialPortManager");
        this.context = context;
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mHandler = new Handler();
        myCallback = (MyCallback) context;
        /*註冊廣播器*/
        registerUsbBroadcastReceiver();
    }

    /*Register a receiver to receive USB action.*/
    private void registerUsbBroadcastReceiver()
    {
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(mUsbPermissionActionReceiver, filter);
    }

    /*Do motion depends on the USB action.*/
    private final BroadcastReceiver mUsbPermissionActionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            Log.d("debug", "usbdevice1:" + usbDevice + " " + usbDevice.getProductId());

            if (action != null && usbDevice != null) {
                if (isLegalUsbType(usbDevice)) {
                    stopAllReceiverScan();
                    usbActionHandler(intent, action);
                }
            }
        }
    };

    /*Check the USB is THL product.*/
    public boolean isLegalUsbType(UsbDevice device)
    {
        if (device.getProductId() == Constants.PRODUCTID_CP210X && device.getVendorId() == Constants.VENDORID_CP210X) {
            THL.u32UsbType = Constants.TYPE_CP210X;
            return true;
        } else if (device.getProductId() == Constants.PRODUCTID_2540 && device.getVendorId() == Constants.VENDORID_2540) {
            THL.u32UsbType = Constants.TYPE_2540;
            return true;
        }
        return false;
    }

    /*Handle USB difernet action.*/
    public void usbActionHandler(Intent intent, String action)
    {
        synchronized (this) {

            Log.d("usbSerialPortManager", "action:" + action + " flag:" + usbFlag);
            LogManager.writeLogToFile("action: " + action);
            switch (action)
            {
                case ACTION_USB_PERMISSION:
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                    {
                        //user choose YES for your previously popup window asking for grant permission for this usb device
                        Log.d("usbManager", "EXTRA_PERMISSION_GRANTED: "+usbDevice.getDeviceName());
                        myCallback.CBstopAnalysisAndUploadReceiverData(); //Avoid get serial port data error.
                        mHandler.removeCallbacks(usbInitialDelay);
                        mHandler.postDelayed(usbInitialDelay, 500);
                    }
                    else
                    {
                        Log.d("usbSerialPortManager", "Permission denied");
                        //user choose NO for your previously pop up window asking for grant permission for this usb device
                        Toast.makeText(context, String.valueOf("Permission denied for device " + usbDevice), Toast.LENGTH_LONG).show();
                        usbFlag = false;
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    Log.d("usbSerialPortManager", mUsbManager.hasPermission(usbDevice) + "");

                    if(mUsbManager.hasPermission(usbDevice))
                    {
                        if(!usbFlag) {
                            usbFlag = true;
                            mHandler.postDelayed(usbInitialDelay, Constants.TIME_FOR_ALL_USB_PLUG_IN);
                        }
                    }
                    else
                    {
                        mUsbManager.requestPermission(usbDevice, mPermissionIntent);
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    if (!usbFlag)
                    {
                        Log.d("usbSerialPortManager", "USB DEVICE DETACHED");
                        usbFlag = true;
                        myCallback.CBstopAnalysisAndUploadReceiverData(); //Avoid get serial port data error.
                        mHandler.postDelayed(usbInitialDelay, Constants.TIME_FOR_ALL_USB_PLUG_IN);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private Runnable usbInitialDelay = () -> {
        usbFlag = false;
        usbClose();
        USBInitial();
        LogManager.systemMonitorLog();  // Record receiver after any USB action and USBInitial.
    };

    /*Make sure the USB status, if is receiver, start scan.*/
    public void USBInitial()
    {
        Log.d("usbSerialPortManager", "USBInitial");
        /*Returns a HashMap containing all USB devices currently attached.*/
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

        usbDeviceNumber = deviceList.size();   //Get the device size.
        DeviceInfoString = "";
        Toast.makeText(context, usbDeviceNumber + ", USB device(s) found",Toast.LENGTH_LONG).show();
        getUsbSizeFromShell();

        usbArraysInitial();

        if (checkAllUsbTypeAndConnect(deviceList.values().iterator()))
        {
            for (int index = 0; index < getUsbDeviceNumber(); index++) {
                getSerialPortData(index);  // Clean the old data at first.
                recognizeReceiver(index);
            }
            startReceiverScan();
        }

        myCallback.CBshowUsbStatusOnUi();
        myCallback.CBstartAnalysisReceiverData();
    }

    // Get USB number by "busybox lsusb".
    private void getUsbSizeFromShell()
    {
        LogManager.writeLogToFile("USB size: " + usbDeviceNumber);
        String result = getShellCommandResult(Constants.USB_INFO_COMMAND, "\n");
        LogManager.writeLogToFile(String.valueOf(result));
    }

    private String getShellCommandResult(String command, String division)
    {
        StringBuilder myLog = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            Log.e("command",command);
            os.writeBytes(command);
            os.writeBytes("exit\n");
            process.waitFor();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null){
                myLog.append(line);
                myLog.append(division);
            }
        } catch (InterruptedException | IOException e) {
            LogManager.saveErrorLogToFile(String.valueOf(e));
            e.printStackTrace();
        }

        return String.valueOf(myLog);
    }

    private void usbArraysInitial()
    {
        THL.aReceiverRecordList.clear();
        THL.bReceiverRecord.clear();
        for(int i = 0; i < usbDeviceNumber; i++)
        {
            THL.bReceiverRecord.add(false);
        }

        mUsbConnections = new UsbDeviceConnection[usbDeviceNumber];
        Endpoint_outs    = new UsbEndpoint[usbDeviceNumber];
        Endpoint_ins     = new UsbEndpoint[usbDeviceNumber];
    }

    private boolean checkAllUsbTypeAndConnect(Iterator<UsbDevice> deviceIterator)
    {
        int index = 0;
        boolean isUsbConnected = false;

        while(deviceIterator.hasNext())
        {
            boolean rtn = false;
            usbDevice = deviceIterator.next();

            //Check the product and vendor ID at first.
            if (isLegalUsbType(usbDevice))
            {
                if(mUsbManager.hasPermission(usbDevice))
                {
                    DeviceInfoString += usbDevice.getProductId() + " , " + usbDevice.getVendorId() + "\n";
                    Log.d("usbSerialPortManager", "Interface: " + usbDevice.getInterfaceCount() + "," + DeviceInfoString + index);

                    rtn = openDeviceAndGetUsbInterface(index);
                    index++;

                    if (rtn)
                        isUsbConnected = true;
                }
                else
                {
                    Log.d("usbSerialPortManager", "openDeviceAndGetUsbInterface Permission ");
                    mUsbManager.requestPermission(usbDevice, mPermissionIntent);
                }
            }
        }
        index = 0;
        return isUsbConnected;
    }

    /*Connect to the USB devices*/
    private boolean openDeviceAndGetUsbInterface(final int index)
    {
        DeviceInfoString += usbDevice.getDeviceName();

        Log.d("usbSerialPortManager", "Index: " + index + " permission: " + mUsbManager.hasPermission(usbDevice) + " device: " + usbDevice.getDeviceName());

        if(connectUsbDevice(index)) //Connect to the device successful.
        {
            getUsbInterfaceAndEndPoints(index);
            SerialSettingInitial(index);
            return true;
        }//Open device success.
        else
        {
            //Toast.makeText(context, "UsbConnection is null",Toast.LENGTH_SHORT).show();
            Log.d("usbSerialPortManager", "UsbConnection is null");
            return false;
        }
    }

    // Open the device so it can be used to send and receive data
    // return : a UsbDeviceConnection, or null if open failed
    private boolean connectUsbDevice(int index)
    {
        mUsbConnections[index] = mUsbManager.openDevice(usbDevice);
        return mUsbConnections[index] != null;
    }

    @SuppressLint("LongLogTag")
    /*Get USB endpoints form interface.*/
    private void getUsbInterfaceAndEndPoints(int index)
    {
        Log.d("getUsbInterfaceAndEndPoints", String.valueOf(usbDevice.getInterfaceCount()) + "index: " + index);
        for (int i = 0; i < usbDevice.getInterfaceCount(); i++)
        {
            UsbInterface iface = usbDevice.getInterface(i);
            DeviceInfoString += ",\n endcount:" + iface.getEndpointCount();

            //Claims exclusive access to a UsbInterface.
            //true to disconnect kernel driver if necessary
            if (mUsbConnections[index].claimInterface(iface, true))
            {
                Log.d("getUsbInterfaceAndEndPoints", "claimInterface " + i + " SUCCESS" + index);

                // Check the number of endpoint
                //If there are two endpoints, and then continue. Otherwise stop.
                if (iface.getEndpointCount() > 1)
                    getEndPointInfo(iface, index);
                else
                    Log.d("getUsbInterfaceAndEndPoints", "Interface " + i + " only has one endpoint.");
            }//Claim Interface.
            else
                Log.d("getUsbInterfaceAndEndPoints", "claimInterface " + i + " FAIL" + index);
        }// Go through USB interface
    }

    /*Get USB output and input endpoint.*/
    private void getEndPointInfo(UsbInterface iface, int index)
    {
        for (int j = 0; j < iface.getEndpointCount(); j++)
        {
            UsbEndpoint endPoint = iface.getEndpoint(j);

            //To get the endpoints.
            if (endPoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK)
            {
                if (endPoint.getDirection() == UsbConstants.USB_DIR_OUT)
                {
                    Log.d("usbSerialPortManager", String.format("Got output endpoint %d  at %s,  %d", j, usbDevice.getDeviceName(), index));
                    Endpoint_outs[index] = endPoint;
                }
                else
                {
                    Log.d("usbSerialPortManager", String.format("Got input endpoint %d  at %s", j, usbDevice.getDeviceName()));
                    Endpoint_ins[index] = endPoint;
                }
            }
            DeviceInfoString += ",endpoint:" + endPoint.getDirection() + "\n";
        }

        Log.d("usbSerialPortManager", DeviceInfoString);
    }

    private void SerialSettingInitial(final int index)
    {
        /*Initial CP 210X*/												                    // 1, tx off
        vendorWriteSingle(IFC_ENABLE, UART_ENABLE, index);
    }

    private int vendorWriteSingle(int request, int buf, int index) {
        int write = 0;
        write = mUsbConnections[index].controlTransfer(HOST_TO_DEVICE_REQUEST_TYPE, request, buf, 0, null, 0, 300);
        //Log.d("debug", String.format("Index: %d, vendorWriteSingle: %d, request: %d, value: %d", index, write, request, buf));
        return write;
    }

    private void recognizeReceiver(int index)
    {
        String sCommand = Constants.GET_FW_INFO;
        //Send get_fw_info to check the beacon type at first, and then check the all beacon's data.
        if(SendCMD(sCommand,index))
        {
            checkGetFwInfoCmdResult(index);
        }
        else
            Log.d("debug", "Send command failed. Index: " + index);
    }

    public boolean SendCMD(String sCommand,int index)
    {
        int rtn = -1;   // Get the return value of bulkTransfer;
        Log.d("debug","index: " + index + ", command: " + sCommand + ", usbConnection length: " + mUsbConnections.length);

        //Prevent empty command for 2640.
        if (sCommand.equals("") || mUsbConnections[index] == null || Endpoint_outs[index] == null)
            return false;
        else
        {
            if(THL.u32UsbType == Constants.TYPE_CP210X)
            {
                sCommand += "\r";
            }
            else if (THL.u32UsbType == Constants.TYPE_2540)
            {
                sCommand += "\n";
            }

            byte[] bytes = null;
            bytes = sCommand.getBytes(StandardCharsets.US_ASCII);
            //Send command to USB.
            rtn =  mUsbConnections[index].bulkTransfer(Endpoint_outs[index], bytes, bytes.length, 1000);
            //Log.d("hebe", "rtn: " + rtn + " " + sCommand);
            return rtn != -1;
        }
    }

    private void checkGetFwInfoCmdResult(final int index)
    {
            String sData = "";
            for (int k = 0 ; k< 3 ; k++)
            {
                sData += getSerialPortData(index);

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    LogManager.saveErrorLogToFile(String.valueOf(e));
                    e.printStackTrace();
                }
            }

            Log.d("Debug", "MSG_RECOGNIZE_RECEIVER, i: " + index + " " + sData);

            analysisReceiverCheckData(sData, index);

            sData = "";
    }

    public int getUsbDeviceNumber()
    {
        return usbDeviceNumber;
    }

    public String getSerialPortData(int index)
    {
        // 1000 : the length of the data to send or receive
        // 100 : timeout	 : in milliseconds
        if (mUsbConnections != null)
        {
            if (mUsbConnections.length > index && Endpoint_ins[index] != null) {
                Arrays.fill(getSerialBuf, (byte) 0);
                //serialData.setLength(0);
                StringBuilder serialData = new StringBuilder();
                int re = mUsbConnections[index].bulkTransfer(Endpoint_ins[index], getSerialBuf, getSerialBuf.length, 100);

                //Log.d("debug", "re:" + re + " " + index + " size: " + serialData.length());
                if(re != -1) {
                    for (int j = 0; j < re; j++) {
                        serialData.append((char) getSerialBuf[j]);
                    }
                }

                return serialData.toString();
            }
        }

        return "";
    }

    /*For continually getting data. */
    public String getSerialPortData(int index, StringBuilder serialData, byte[] serialBuf)
    {
        // 1000 : the length of the data to send or receive
        // 100 : timeout	 : in milliseconds
        if (mUsbConnections != null)
        {
            if (mUsbConnections.length > index && Endpoint_ins[index] != null) {
                Arrays.fill(serialBuf, (byte) 0);
                serialData.setLength(0);
                int re = mUsbConnections[index].bulkTransfer(Endpoint_ins[index], serialBuf, serialBuf.length, 100);

                if(re != -1) {
                    for (int j = 0; j < re; j++) {
                        serialData.append((char) serialBuf[j]);
                    }
                }

                return serialData.toString();
            }
        }

        return "";
    }

    private void analysisReceiverCheckData(String sData, int index)
    {
        /*To filter send command fail or empty string status.*/
        if(!(sData.contains(Constants.COMMAND_NOT_FOUND) || sData.equals("")))
        {
            //看是否有 beacon_scanner 字眼, 有就是 RECEIVER.
            if(isReceiver(sData))
                recordReceiverMark(index, sData);
            else   //Beacon
            {
                Log.d("Debug", "MSG_RECOGNIZE_RECEIVER, not receiver"+ " " + index + " "+ sData);
                THL.bReceiverRecord.set(index, false);
                SendCMD(Constants.SET_KEEP_SETTING_FALSE, index);
                SendCMD(Constants.DEFAULT_BEACON_SETTING, index);  //Set beacon to transmit 0,0.

                if (THL.u32UsbType == Constants.TYPE_CP210X)
                {
                    //SendBeaconCMD(MsgIndex);
                    //Enable 2640 to monitor Banana Pi,    not ready
                    //EnableMonitor(MsgIndex);
                }
            }
        }
        else   //Command not found
        {
            Log.d("Debug", "MSG_RECOGNIZE_RECEIVER, Recognize_Receiver again");
            //recognizeReceiver(index);
        }
    }

    private boolean isReceiver(String str)
    {
        return str.contains("Scanner");//!(str.contains("command not found") || str.contains("GPIO"));
    }

    private void recordReceiverMark(int index, String sData)
    {
        String mac = getReceiverMac(sData);

        if (!mac.equals(""))
        {
            Log.d("Debug", "MSG_RECOGNIZE_RECEIVER, Is Receiver and get MAC successfully.");
            //Record the receiver location.
            BeaconInfoFormat receiver = new BeaconInfoFormat();
            receiver.index = index;
            receiver.receiverMac = mac;
            Log.d("debug", "Receiver mac: " + receiver.receiverMac);
            THL.aReceiverRecordList.add(receiver);
            THL.bReceiverRecord.set(index, true);
            u32CheckReceiverRetryCount = 0;
        }
        else
        {
            if (u32CheckReceiverRetryCount < 3) {
                Log.d("Debug", "MSG_RECOGNIZE_RECEIVER, get MAC fail and retry.");
                recognizeReceiver(index);
                u32CheckReceiverRetryCount ++;
            }
            else
            {
                Log.d("Debug", "MSG_RECOGNIZE_RECEIVER, get MAC fail.");
                u32CheckReceiverRetryCount = 0;
            }
        }
    }

    /*The string would be "Name(Beacon_Scanner) Mac(99:1F:63:A0:06:40) Ver(1.1)"*/
    private  String getReceiverMac(String sData)
    {
        //LogManager.writeLogToFile("sData: " + sData);
        String mac = "";
        String [] data = sData.split("Mac");

        if (data.length > 1) //Avoid array bundle exception.
            mac = data[1].substring(1, 18);
        if (FormatCheck.macFormatCheck(mac))
            return mac;
        else
            return "";
    }

    public void unregisterReceiver()
    {
        context.unregisterReceiver(mUsbPermissionActionReceiver);
    }

    private void startReceiverScan()
    {
        if ( !(THL.ScanTime.equals("") || THL.StopScanTime.equals("")) )
        {
            String HexTime = Integer.toHexString(Integer.parseInt(THL.ScanTime));  //milliseconds
            String StopHexTime = Integer.toHexString(Integer.parseInt(THL.StopScanTime));
            String OutputString = "";

            //FW needs the length of 4.
            while(HexTime.length() < 4)
            {
                HexTime = "0" + HexTime;
            }
            while (StopHexTime.length() < 4)
            {
                StopHexTime = "0" + StopHexTime;
            }

            // Command   start_scan 1 ScanTime StopTime.    1 for scanning iBeacon.
            OutputString = "start_scan 1 "+HexTime + " " + StopHexTime;
            Log.d("debug", "OutputString:" + OutputString);

            for (int i = 0; i < THL.bReceiverRecord.size(); i++)
            {
                if (THL.bReceiverRecord.get(i))
                    SendCMD(OutputString, i);
            }
        }
        else
        {
            //Toast.makeText(context, "Receiver Scan setting is not ready.",Toast.LENGTH_SHORT).show();
        }
    }

    public void stopAllReceiverScan()
    {
        for (int i = 0; i < getUsbDeviceNumber(); i++)
        {
            boolean j = SendCMD(Constants.STOP_SCAN, i);
            Log.d("debug", "stop:" + j);
        }

    }

    public void usbClose()
    {
        if (mUsbConnections != null)
        {
            for (UsbDeviceConnection u : mUsbConnections)
                u.close();
        }
    }
}
