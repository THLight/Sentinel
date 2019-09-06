package thl.sentinel;

import org.junit.Test;

import thl.sentinel.feature.FormatCheck;

import static org.junit.Assert.*;

/**
 * Example local unit notExecuteCommand, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    public void test()
    {
        boolean rtn = FormatCheck.integerFormatCheck("");
        assertEquals(false, rtn);
    }
}