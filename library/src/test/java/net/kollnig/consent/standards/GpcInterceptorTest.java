package net.kollnig.consent.standards;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class GpcInterceptorTest {

    @Before
    public void setUp() {
        GpcInterceptor.setEnabled(false);
    }

    @After
    public void tearDown() {
        GpcInterceptor.setEnabled(false);
    }

    @Test
    public void disabledByDefault() {
        assertFalse(GpcInterceptor.isEnabled());
    }

    @Test
    public void enableAndDisable() {
        GpcInterceptor.setEnabled(true);
        assertTrue(GpcInterceptor.isEnabled());

        GpcInterceptor.setEnabled(false);
        assertFalse(GpcInterceptor.isEnabled());
    }

    @Test
    public void headerConstants() {
        assertEquals("Sec-GPC", GpcInterceptor.GPC_HEADER_NAME);
        assertEquals("1", GpcInterceptor.GPC_HEADER_VALUE);
    }

    @Test
    public void applyTo_addsHeaderWhenEnabled() throws Exception {
        GpcInterceptor.setEnabled(true);

        URL url = new URL("http://example.com");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        GpcInterceptor.applyTo(conn);

        assertEquals("1", conn.getRequestProperty("Sec-GPC"));
        conn.disconnect();
    }

    @Test
    public void applyTo_doesNotAddHeaderWhenDisabled() throws Exception {
        GpcInterceptor.setEnabled(false);

        URL url = new URL("http://example.com");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        GpcInterceptor.applyTo(conn);

        assertNull(conn.getRequestProperty("Sec-GPC"));
        conn.disconnect();
    }
}
