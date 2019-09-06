package thl.sentinel.FeatureTest;

import org.junit.Before;
import org.junit.Test;

import thl.sentinel.feature.AnalysisReceiverData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AnalysisReceiverDataUnitTest {
    AnalysisReceiverData mAnalysisReceiverData = null;
    private String rtn = "";
    private boolean bRtn = false;

    @Before
    public void setUp()
    {
        mAnalysisReceiverData = new AnalysisReceiverData();
    }

    @Test
    public void emptyTest()
    {
        rtn = mAnalysisReceiverData.checkScannedMac("");
        assertEquals("", rtn);
        rtn = mAnalysisReceiverData.checkScannedUuid("");
        assertEquals("", rtn);
        rtn = mAnalysisReceiverData.checkScannedMajor("");
        assertEquals("", rtn);
        rtn = mAnalysisReceiverData.checkScannedMinor("");
        assertEquals("", rtn);
        rtn = mAnalysisReceiverData.checkScannedRssi("");
        assertEquals("", rtn);
        bRtn = mAnalysisReceiverData.isUuidOk("");
        assertFalse(bRtn);

    }

    @Test
    public void scannedMacTest()
    {
        parserScannedMac("A4:34:F1:8A:22:F9", "A4:34:F1:8A:22:F9");
        parserScannedMac("A4:34:F1:8A:22:F9", "A4:34:F1:8A:22:F9 ");
        parserScannedMac("", "A4:34:F1:8A:22:F");
        parserScannedMac("", "A00:34:F1:8A:22:F9");
        parserScannedMac("", "H4:34:F1:8A:22:F9");
    }

    private void parserScannedMac(String expect, String string)
    {
        rtn = mAnalysisReceiverData.checkScannedMac(string);
        assertEquals(expect, rtn);
    }

    @Test
    public void scannedUuidTest()
    {
        parserScannedUuid("F2C56DB5-DFFB-48D2-B060-D0F5A71096E0", "F2C56DB5-DFFB-48D2-B060-D0F5A71096E0");
        parserScannedUuid("F2C56DB5-DFFB-48D2-B060-D0F5A71096E0", "F2C56DB5-DFFB-48D2-B060-D0F5A71096E0 ");
        parserScannedUuid("", "FF2C56DB5-DFFB-48D2-B060-D0F5A71096E0");
        parserScannedUuid("", "G2C56DB5-DFFB-48D2-B060-D0F5A71096E0");
        parserScannedUuid("", "F2C56DB5-DFFB-48D2B060D0F5A71096E0");
        parserScannedUuid("", "F2C56DB5-FDFFB-48D2-B060-D0F5A71096E0");
        parserScannedUuid("", "F2C56DB5-DFFB-F48D2-B060-D0F5A71096E0");
        parserScannedUuid("", "F2C56DB5-DFFB-48D2-FB060-D0F5A71096E0");
        parserScannedUuid("", "F2C56DB5-DFFB-48D2-B060-FD0F5A71096E0");
    }

    private void parserScannedUuid(String expect, String string)
    {
        rtn = mAnalysisReceiverData.checkScannedUuid(string);
        assertEquals(expect, rtn);
    }

    @Test
    public void scannedMajorTest()
    {
        parserScannedMajor("500", "01F4");
        parserScannedMajor("1312", "0520 ");
        parserScannedMajor("", "+");
        parserScannedMajor("", "-10");
        parserScannedMajor("", "GHJ");
        parserScannedMajor("", "01F4.6");
    }

    private void parserScannedMajor(String expect, String string)
    {
        rtn = mAnalysisReceiverData.checkScannedMajor(string);
        assertEquals(expect, rtn);
    }

    @Test
    public void scannedMinorTest()
    {
        parserScannedMinor("500", "01F4");
        parserScannedMinor("1312", "0520 ");
        parserScannedMinor("", "+");
        parserScannedMinor("", "-10");
        parserScannedMinor("", "GHJ");
        parserScannedMinor("", "01F4.6");
    }

    private void parserScannedMinor(String expect, String string)
    {
        rtn = mAnalysisReceiverData.checkScannedMinor(string);
        assertEquals(expect, rtn);
    }

    @Test
    public void scannedRssiTest()
    {
        parserScannedRssi("-69", "-69");
        parserScannedRssi("69", "69 ");
        parserScannedRssi("-100", "69.0");
        parserScannedRssi("-100", "+");
        parserScannedRssi("-100", "GHJ");
        parserScannedRssi("-100", "01F4");
    }

    private void parserScannedRssi(String expect, String string)
    {
        rtn = mAnalysisReceiverData.checkScannedRssi(string);
        assertEquals(expect, rtn);
    }
}
