package net.kollnig.consent.plugin;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for ConsentTransformRules — verifies that rules correctly match
 * SDK classes and methods.
 */
public class ConsentTransformRulesTest {

    @Test
    public void hasRulesForGoogleAds() {
        assertTrue(ConsentTransformRules.hasRulesForClass(
                "com/google/android/gms/ads/MobileAds"));
    }

    @Test
    public void hasRulesForBaseAdView() {
        assertTrue(ConsentTransformRules.hasRulesForClass(
                "com/google/android/gms/ads/BaseAdView"));
    }

    @Test
    public void hasRulesForAdvertisingIdClient() {
        assertTrue(ConsentTransformRules.hasRulesForClass(
                "com/google/android/gms/ads/identifier/AdvertisingIdClient"));
    }

    @Test
    public void hasRulesForAppsFlyer() {
        assertTrue(ConsentTransformRules.hasRulesForClass(
                "com/appsflyer/AppsFlyerLib"));
    }

    @Test
    public void hasRulesForFlurry() {
        assertTrue(ConsentTransformRules.hasRulesForClass(
                "com/flurry/android/FlurryAgent$Builder"));
    }

    @Test
    public void hasRulesForInMobi() {
        assertTrue(ConsentTransformRules.hasRulesForClass(
                "com/inmobi/sdk/InMobiSdk"));
    }

    @Test
    public void hasRulesForAdColony() {
        assertTrue(ConsentTransformRules.hasRulesForClass(
                "com/adcolony/sdk/AdColony"));
    }

    @Test
    public void hasRulesForVungle() {
        assertTrue(ConsentTransformRules.hasRulesForClass(
                "com/vungle/warren/Vungle"));
    }

    @Test
    public void noRulesForUnknownClass() {
        assertFalse(ConsentTransformRules.hasRulesForClass(
                "com/example/UnknownSdk"));
    }

    @Test
    public void noRulesForConsentManagerItself() {
        assertFalse(ConsentTransformRules.hasRulesForClass(
                "net/kollnig/consent/ConsentManager"));
    }

    @Test
    public void findRuleForGoogleAdsInitialize() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "com/google/android/gms/ads/MobileAds",
                "initialize",
                "(Landroid/content/Context;)V");
        assertNotNull(rule);
        assertEquals("google_ads", rule.libraryId);
        assertEquals(ConsentTransformRules.Action.BLOCK, rule.action);
    }

    @Test
    public void findRuleForGoogleAdsInitializeWithListener() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "com/google/android/gms/ads/MobileAds",
                "initialize",
                "(Landroid/content/Context;Lcom/google/android/gms/ads/initialization/OnInitializationCompleteListener;)V");
        assertNotNull(rule);
        assertEquals("google_ads", rule.libraryId);
    }

    @Test
    public void findRuleForLoadAd() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "com/google/android/gms/ads/BaseAdView",
                "loadAd",
                "(Lcom/google/android/gms/ads/AdRequest;)V");
        assertNotNull(rule);
        assertEquals("google_ads", rule.libraryId);
        assertEquals(ConsentTransformRules.Action.BLOCK, rule.action);
    }

    @Test
    public void findRuleForAdvertisingId_throwsException() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "com/google/android/gms/ads/identifier/AdvertisingIdClient",
                "getAdvertisingIdInfo",
                "(Landroid/content/Context;)Lcom/google/android/gms/ads/identifier/AdvertisingIdClient$Info;");
        assertNotNull(rule);
        assertEquals("google_ads_identifier", rule.libraryId);
        assertEquals(ConsentTransformRules.Action.THROW_IO_EXCEPTION, rule.action);
    }

    @Test
    public void noRuleForWrongMethodName() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "com/google/android/gms/ads/MobileAds",
                "nonexistent",
                "(Landroid/content/Context;)V");
        assertNull(rule);
    }

    @Test
    public void noRuleForWrongDescriptor() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "com/google/android/gms/ads/MobileAds",
                "initialize",
                "(Ljava/lang/String;)V");
        assertNull(rule);
    }

    @Test
    public void googleAdsHasMultipleRules() {
        List<ConsentTransformRules.Rule> rules = ConsentTransformRules.getRulesForClass(
                "com/google/android/gms/ads/MobileAds");
        assertEquals(2, rules.size()); // initialize(Context) and initialize(Context, Listener)
    }

    @Test
    public void hasRulesForOkHttpRequestBuilder() {
        assertTrue(ConsentTransformRules.hasRulesForClass(
                "okhttp3/Request$Builder"));
    }

    @Test
    public void findRuleForOkHttpBuild_gpcAction() {
        ConsentTransformRules.Rule rule = ConsentTransformRules.findRule(
                "okhttp3/Request$Builder",
                "build",
                "()Lokhttp3/Request;");
        assertNotNull(rule);
        assertEquals(ConsentTransformRules.Action.INJECT_GPC_HEADER, rule.action);
    }

    @Test
    public void allRulesHaveNonEmptyLibraryId() {
        String[] classes = {
                "com/google/android/gms/ads/MobileAds",
                "com/google/android/gms/ads/BaseAdView",
                "com/google/android/gms/ads/identifier/AdvertisingIdClient",
                "com/appsflyer/AppsFlyerLib",
                "com/flurry/android/FlurryAgent$Builder",
                "com/inmobi/sdk/InMobiSdk",
                "com/adcolony/sdk/AdColony",
                "com/vungle/warren/Vungle",
                "okhttp3/Request$Builder"
        };

        for (String cls : classes) {
            List<ConsentTransformRules.Rule> rules = ConsentTransformRules.getRulesForClass(cls);
            assertFalse("No rules for " + cls, rules.isEmpty());
            for (ConsentTransformRules.Rule rule : rules) {
                assertNotNull("Null libraryId for " + cls, rule.libraryId);
                assertFalse("Empty libraryId for " + cls, rule.libraryId.isEmpty());
            }
        }
    }
}
