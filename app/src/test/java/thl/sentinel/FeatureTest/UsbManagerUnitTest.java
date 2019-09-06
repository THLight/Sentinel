package thl.sentinel.FeatureTest;

import android.app.Activity;
import android.hardware.usb.UsbDevice;

import org.junit.Before;
import org.junit.Test;

import thl.sentinel.feature.UsbSerialPortManager;

import static org.junit.Assert.assertFalse;

public class UsbManagerUnitTest extends Activity {

    UsbSerialPortManager mUsbSerialPortManager = null;
    boolean bRtrn = false;

    @Before
    public void setUp()
    {
        mUsbSerialPortManager = new UsbSerialPortManager(this);
    }

    @Test
    public void isLegalUsbTypeTest()
    {
        bRtrn = mUsbSerialPortManager.isLegalUsbType(null);
        assertFalse(bRtrn);
    }

}
