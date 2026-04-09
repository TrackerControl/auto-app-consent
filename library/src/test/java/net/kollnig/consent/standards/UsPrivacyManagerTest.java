package net.kollnig.consent.standards;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class UsPrivacyManagerTest {

    private Context context;
    private UsPrivacyManager usPrivacy;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        usPrivacy = new UsPrivacyManager(context);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().apply();
    }

    @Test
    public void writeConsentSignal_ccpaApplies_consentGiven() {
        usPrivacy.writeConsentSignal(true, true);

        String privacy = prefs.getString("IABUSPrivacy_String", null);
        assertNotNull(privacy);
        assertEquals("1YNN", privacy);
    }

    @Test
    public void writeConsentSignal_ccpaApplies_consentDenied() {
        usPrivacy.writeConsentSignal(true, false);

        String privacy = prefs.getString("IABUSPrivacy_String", null);
        assertEquals("1YYN", privacy);
    }

    @Test
    public void writeConsentSignal_ccpaDoesNotApply() {
        usPrivacy.writeConsentSignal(false, false);

        String privacy = prefs.getString("IABUSPrivacy_String", null);
        assertEquals("1---", privacy);
    }

    @Test
    public void clearConsentSignal_removesKey() {
        usPrivacy.writeConsentSignal(true, true);
        assertNotNull(prefs.getString("IABUSPrivacy_String", null));

        usPrivacy.clearConsentSignal();
        assertNull(prefs.getString("IABUSPrivacy_String", null));
    }

    @Test
    public void getPrivacyString_nullInitially() {
        assertNull(usPrivacy.getPrivacyString());
    }

    @Test
    public void getPrivacyString_returnsWrittenValue() {
        usPrivacy.writeConsentSignal(true, false);
        assertEquals("1YYN", usPrivacy.getPrivacyString());
    }

    @Test
    public void hasOptedOutOfSale_trueWhenDenied() {
        usPrivacy.writeConsentSignal(true, false);
        assertTrue(usPrivacy.hasOptedOutOfSale());
    }

    @Test
    public void hasOptedOutOfSale_falseWhenConsented() {
        usPrivacy.writeConsentSignal(true, true);
        assertFalse(usPrivacy.hasOptedOutOfSale());
    }

    @Test
    public void hasOptedOutOfSale_falseWhenNotSet() {
        assertFalse(usPrivacy.hasOptedOutOfSale());
    }

    @Test
    public void hasOptedOutOfSale_falseWhenNotApplicable() {
        usPrivacy.writeConsentSignal(false, false);
        assertFalse(usPrivacy.hasOptedOutOfSale());
    }

    @Test
    public void privacyStringFormat_startsWithVersion1() {
        usPrivacy.writeConsentSignal(true, true);
        String privacy = usPrivacy.getPrivacyString();
        assertTrue(privacy.startsWith("1"));
    }

    @Test
    public void privacyStringFormat_exactlyFourChars() {
        usPrivacy.writeConsentSignal(true, true);
        assertEquals(4, usPrivacy.getPrivacyString().length());

        usPrivacy.writeConsentSignal(false, false);
        assertEquals(4, usPrivacy.getPrivacyString().length());
    }
}
