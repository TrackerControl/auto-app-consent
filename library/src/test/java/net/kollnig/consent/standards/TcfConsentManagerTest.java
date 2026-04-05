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
public class TcfConsentManagerTest {

    private Context context;
    private TcfConsentManager tcf;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        tcf = new TcfConsentManager(context, 42, 1, "DE");
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().apply();
    }

    @Test
    public void writeConsentSignals_gdprApplies_consentGiven() {
        tcf.writeConsentSignals(true, true);

        assertEquals(1, prefs.getInt("IABTCF_gdprApplies", -1));
        assertEquals(42, prefs.getInt("IABTCF_CmpSdkID", -1));
        assertEquals(1, prefs.getInt("IABTCF_CmpSdkVersion", -1));
        assertEquals(4, prefs.getInt("IABTCF_PolicyVersion", -1));
        assertEquals("DE", prefs.getString("IABTCF_PublisherCC", null));

        // All purposes consented
        String purposeConsents = prefs.getString("IABTCF_PurposeConsents", "");
        assertEquals(TcfConsentManager.PURPOSE_COUNT, purposeConsents.length());
        assertEquals("11111111111", purposeConsents);
    }

    @Test
    public void writeConsentSignals_gdprApplies_consentDenied() {
        tcf.writeConsentSignals(true, false);

        assertEquals(1, prefs.getInt("IABTCF_gdprApplies", -1));

        String purposeConsents = prefs.getString("IABTCF_PurposeConsents", "");
        assertEquals("00000000000", purposeConsents);

        String specialFeatures = prefs.getString("IABTCF_SpecialFeaturesOptIns", "");
        assertEquals("00", specialFeatures);
    }

    @Test
    public void writeConsentSignals_gdprDoesNotApply() {
        tcf.writeConsentSignals(false, false);

        assertEquals(0, prefs.getInt("IABTCF_gdprApplies", -1));
    }

    @Test
    public void writeConsentSignals_perPurpose() {
        boolean[] purposes = new boolean[TcfConsentManager.PURPOSE_COUNT];
        purposes[0] = true;  // Purpose 1: Store/access info
        purposes[1] = false; // Purpose 2: Basic ads
        purposes[6] = true;  // Purpose 7: Measure ad performance

        boolean[] specialFeatures = {false, true}; // Scan device characteristics

        tcf.writeConsentSignals(true, purposes, specialFeatures);

        String purposeConsents = prefs.getString("IABTCF_PurposeConsents", "");
        assertEquals('1', purposeConsents.charAt(0));
        assertEquals('0', purposeConsents.charAt(1));
        assertEquals('1', purposeConsents.charAt(6));

        String specialFeaturesStr = prefs.getString("IABTCF_SpecialFeaturesOptIns", "");
        assertEquals("01", specialFeaturesStr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void writeConsentSignals_perPurpose_wrongPurposeCount() {
        boolean[] purposes = new boolean[3]; // Wrong count
        boolean[] specialFeatures = new boolean[TcfConsentManager.SPECIAL_FEATURE_COUNT];
        tcf.writeConsentSignals(true, purposes, specialFeatures);
    }

    @Test(expected = IllegalArgumentException.class)
    public void writeConsentSignals_perPurpose_wrongSpecialFeatureCount() {
        boolean[] purposes = new boolean[TcfConsentManager.PURPOSE_COUNT];
        boolean[] specialFeatures = new boolean[5]; // Wrong count
        tcf.writeConsentSignals(true, purposes, specialFeatures);
    }

    @Test
    public void clearConsentSignals_removesAllKeys() {
        tcf.writeConsentSignals(true, true);
        assertTrue(prefs.contains("IABTCF_PurposeConsents"));

        tcf.clearConsentSignals();
        assertFalse(prefs.contains("IABTCF_PurposeConsents"));
        assertFalse(prefs.contains("IABTCF_gdprApplies"));
        assertFalse(prefs.contains("IABTCF_CmpSdkID"));
    }

    @Test
    public void hasConsentSignals_falseInitially() {
        assertFalse(tcf.hasConsentSignals());
    }

    @Test
    public void hasConsentSignals_trueAfterWrite() {
        tcf.writeConsentSignals(true, true);
        assertTrue(tcf.hasConsentSignals());
    }

    @Test
    public void getPurposeConsents_returnsCorrectValues() {
        tcf.writeConsentSignals(true, true);
        boolean[] consents = tcf.getPurposeConsents();
        assertEquals(TcfConsentManager.PURPOSE_COUNT, consents.length);
        for (boolean c : consents) {
            assertTrue(c);
        }
    }

    @Test
    public void getPurposeConsents_allFalseWhenDenied() {
        tcf.writeConsentSignals(true, false);
        boolean[] consents = tcf.getPurposeConsents();
        for (boolean c : consents) {
            assertFalse(c);
        }
    }

    @Test
    public void storedInDefaultSharedPreferences() {
        // TCF spec requires keys in default SharedPreferences
        tcf.writeConsentSignals(true, true);

        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        assertTrue(defaultPrefs.contains("IABTCF_PurposeConsents"));
    }

    @Test
    public void writeConsentSignals_writesTcString() {
        tcf.writeConsentSignals(true, true);

        String tcString = prefs.getString("IABTCF_TCString", null);
        assertNotNull("TC String should be written", tcString);
        assertFalse("TC String should not be empty", tcString.isEmpty());
    }

    @Test
    public void writeConsentSignals_tcStringDiffersForConsentAndDeny() {
        tcf.writeConsentSignals(true, true);
        String consentTc = prefs.getString("IABTCF_TCString", null);

        tcf.writeConsentSignals(true, false);
        String denyTc = prefs.getString("IABTCF_TCString", null);

        assertNotEquals("TC String should differ for consent vs deny", consentTc, denyTc);
    }

    @Test
    public void writeConsentSignals_perPurpose_writesTcString() {
        boolean[] purposes = new boolean[TcfConsentManager.PURPOSE_COUNT];
        purposes[0] = true;
        boolean[] specialFeatures = {false, false};

        tcf.writeConsentSignals(true, purposes, specialFeatures);

        String tcString = prefs.getString("IABTCF_TCString", null);
        assertNotNull("TC String should be written for per-purpose consent", tcString);
    }

    @Test
    public void clearConsentSignals_removesTcString() {
        tcf.writeConsentSignals(true, true);
        assertNotNull(prefs.getString("IABTCF_TCString", null));

        tcf.clearConsentSignals();
        assertNull(prefs.getString("IABTCF_TCString", null));
    }
}
