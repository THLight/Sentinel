package thl.sentinel.FeatureTest;

import org.junit.Test;

import thl.sentinel.feature.FormatCheck;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FormatCheckUnitTest {

    private boolean rtn = false;
    @Test
    public void emptyTest()
    {
        rtn = FormatCheck.macFormatCheck("");
        assertFalse(rtn);

        rtn = FormatCheck.uuidFormatCheck("");
        assertFalse(rtn);

        rtn = FormatCheck.integerFormatCheck("");
        assertFalse(rtn);

        rtn = FormatCheck.stringHexFormatCheck("");
        assertFalse(rtn);
    }

    @Test
    public void macCheckTest()
    {
        rtn = FormatCheck.macFormatCheck("123");
        rtn = rtn || FormatCheck.macFormatCheck("FF:FF");
        rtn = rtn || FormatCheck.macFormatCheck("FF:FF:FF:FF:FF:FF:FF");
        rtn = rtn || FormatCheck.macFormatCheck("FF:FF:FF:GE:EE:EE");
        rtn = rtn || FormatCheck.macFormatCheck("AB:CD:EF:12:34:564");
        assertFalse(rtn);

        rtn = FormatCheck.macFormatCheck("aB:CD:EF:12:34:56");;
        rtn = rtn && FormatCheck.macFormatCheck("AB:CD:EF:12:34:56");
        assertTrue(rtn);
    }

    @Test
    public void uuidCheckTest()
    {
        rtn = FormatCheck.uuidFormatCheck("");
        assertFalse(rtn);
    }

    @Test
    public void stringHexCheckTest()
    {
        rtn = FormatCheck.stringHexFormatCheck(",");
        rtn = rtn || FormatCheck.stringHexFormatCheck(":");
        rtn = rtn || FormatCheck.stringHexFormatCheck("J");
        rtn = rtn || FormatCheck.stringHexFormatCheck("12+AA");
        rtn = rtn || FormatCheck.stringHexFormatCheck("-500");
        assertFalse(rtn);
    }

    @Test
    public void integerCheckTest()
    {
        rtn = FormatCheck.integerFormatCheck("A");
        rtn = rtn || FormatCheck.integerFormatCheck(":");
        rtn = rtn || FormatCheck.integerFormatCheck("J");
        rtn = rtn || FormatCheck.integerFormatCheck("12+AA");
        rtn = rtn || FormatCheck.integerFormatCheck("1A");
        rtn = rtn || FormatCheck.integerFormatCheck("1.2");
        assertFalse(rtn);

        rtn = FormatCheck.integerFormatCheck("10");
        rtn = rtn && FormatCheck.integerFormatCheck("-500");
        assertTrue(rtn);
    }

}
