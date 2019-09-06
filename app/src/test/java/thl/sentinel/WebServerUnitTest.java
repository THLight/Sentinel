package thl.sentinel;

import android.app.Activity;

import org.junit.Before;
import org.junit.Test;

import thl.sentinel.data.Constants;


public class WebServerUnitTest extends Activity {

    private WebServer mWebServer = null;
    @Before
    public void setUp()
    {
        mWebServer = new WebServer(Constants.WEB_SERVER_PORT, this);
    }

    @Test
    public void Test()
    {

    }
}
