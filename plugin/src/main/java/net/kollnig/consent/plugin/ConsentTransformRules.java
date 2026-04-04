package net.kollnig.consent.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines which SDK methods should have consent checks injected.
 *
 * Each rule specifies:
 * - The class and method to transform
 * - The consent library ID to check
 * - The action to take when consent is denied (BLOCK or MODIFY_ARGS)
 *
 * These rules replace the runtime hooks that were previously in:
 * - GoogleAdsLibrary (YAHFA hooks on initialize, loadAd)
 * - AdvertisingIdLibrary (YAHFA hook on getAdvertisingIdInfo)
 * - AppsFlyerLibrary (YAHFA hook on start)
 * - FlurryLibrary (YAHFA hook on build)
 * - InMobiLibrary (YAHFA hook on init)
 * - AdColonyLibrary (YAHFA hook on configure)
 * - VungleLibrary (YAHFA hook on configure)
 */
public class ConsentTransformRules {

    public enum Action {
        /** Return early (void) or return default value if no consent */
        BLOCK,
        /** Throw IOException if no consent (for AdvertisingIdClient) */
        THROW_IO_EXCEPTION
    }

    public static class Rule {
        public final String className;     // internal name: com/example/Foo
        public final String methodName;
        public final String methodDesc;    // JNI descriptor
        public final String libraryId;     // consent library ID
        public final Action action;

        public Rule(String className, String methodName, String methodDesc,
                    String libraryId, Action action) {
            this.className = className;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.libraryId = libraryId;
            this.action = action;
        }
    }

    // Map from internal class name -> list of rules for that class
    private static final Map<String, List<Rule>> RULES = new HashMap<>();

    static {
        // Google Ads — block initialize() and loadAd() without consent
        addRule("com/google/android/gms/ads/MobileAds", "initialize",
                "(Landroid/content/Context;)V",
                "google_ads", Action.BLOCK);
        addRule("com/google/android/gms/ads/MobileAds", "initialize",
                "(Landroid/content/Context;Lcom/google/android/gms/ads/initialization/OnInitializationCompleteListener;)V",
                "google_ads", Action.BLOCK);
        addRule("com/google/android/gms/ads/BaseAdView", "loadAd",
                "(Lcom/google/android/gms/ads/AdRequest;)V",
                "google_ads", Action.BLOCK);

        // Advertising ID — throw IOException without consent
        addRule("com/google/android/gms/ads/identifier/AdvertisingIdClient",
                "getAdvertisingIdInfo",
                "(Landroid/content/Context;)Lcom/google/android/gms/ads/identifier/AdvertisingIdClient$Info;",
                "google_ads_identifier", Action.THROW_IO_EXCEPTION);

        // AppsFlyer — block start() without consent
        addRule("com/appsflyer/AppsFlyerLib", "start",
                "(Landroid/content/Context;Ljava/lang/String;Lcom/appsflyer/attribution/AppsFlyerRequestListener;)V",
                "appsflyer", Action.BLOCK);

        // Flurry — block build() without consent
        addRule("com/flurry/android/FlurryAgent$Builder", "build",
                "(Landroid/content/Context;Ljava/lang/String;)V",
                "flurry", Action.BLOCK);

        // InMobi — block init() without consent
        addRule("com/inmobi/sdk/InMobiSdk", "init",
                "(Landroid/content/Context;Ljava/lang/String;Lorg/json/JSONObject;Lcom/inmobi/sdk/SdkInitializationListener;)V",
                "inmobi", Action.BLOCK);

        // AdColony — block configure() without consent
        // Note: AdColony obfuscates method names, so we match by signature
        // The actual method name may vary; the visitor checks signature too
        addRule("com/adcolony/sdk/AdColony", "configure",
                "(Landroid/content/Context;Lcom/adcolony/sdk/AdColonyAppOptions;Ljava/lang/String;)Z",
                "adcolony", Action.BLOCK);

        // Vungle — block init() without consent
        addRule("com/vungle/warren/Vungle", "init",
                "(Ljava/lang/String;Landroid/content/Context;Lcom/vungle/warren/InitCallback;)V",
                "vungle", Action.BLOCK);
    }

    private static void addRule(String className, String methodName, String methodDesc,
                                String libraryId, Action action) {
        RULES.computeIfAbsent(className, k -> new ArrayList<>())
                .add(new Rule(className, methodName, methodDesc, libraryId, action));
    }

    /**
     * Check if we have any transform rules for this class.
     */
    public static boolean hasRulesForClass(String internalClassName) {
        return RULES.containsKey(internalClassName);
    }

    /**
     * Get all rules for a class.
     */
    public static List<Rule> getRulesForClass(String internalClassName) {
        return RULES.getOrDefault(internalClassName, new ArrayList<>());
    }

    /**
     * Find a matching rule for a specific method.
     */
    public static Rule findRule(String internalClassName, String methodName, String methodDesc) {
        List<Rule> rules = RULES.get(internalClassName);
        if (rules == null) return null;

        for (Rule rule : rules) {
            if (rule.methodName.equals(methodName) && rule.methodDesc.equals(methodDesc)) {
                return rule;
            }
        }
        return null;
    }
}
