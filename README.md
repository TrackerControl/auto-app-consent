# App Consent (Alpha release, not for production)

This repository shall help *app developers* implement consent in apps correctly. This helps
compliance with the GDPR, CCPA and other legal regimes. The motivation for this project that our research at Oxford found that less than 4% of Android apps implement any form of consent: <https://www.usenix.org/conference/soups2021/presentation/kollnig>

Specifically, this tool targets the following common compliance problems:

**Common Problem 1:** Failure to to implement any consent flows. This can both involve 1) the sharing of data with third-party companies without consent (violating Articles 7 and (35)1 GDPR) and 2) non technically-necessary accessing or storing of data on smartphone (violating Article 5(3) ePrivacy Directive)

**Solution:** Automatic implementation of consent flows with this library.

**Common Problem 2:** Sharing more data than necessary (violating Article 4(1) GDPR).

**Solution:** Many third-party libraries come with configuration options to reduce data collection. This library automatically chooses some of the most common settings.

At the moment, this project automatically implements a consent flow if your app uses one of the following SDKs:

- Google Firebase Analytics
- Google Crashlytics
- Google Ads
- Facebook SDK
- AppLovin
- Flurry (disabled SDK if lack of consent altogether)
- InMobi
- AppsFlyer
- ironSource
- AdColony
- Vungle (untested)
- Google Play Services Advertising Identifier Library

The tool seeks to prevent the automatic sharing of data at the first app start, and also to share more data than necessary.

*Note that the use of Google and Facebook services in the EU is [likely illegal even with user consent](https://noyb.eu/en/austrian-dsb-eu-us-data-transfers-google-analytics-illegal), because data is sent to the US and can be used for unlawful surveillance of EU citizens. The same applies to other US-based services.*

<img src="assets/screen.png"
alt="Screenshot of the automatic consent flow."
width="50%">

## Installation

**NOTE THAT THE USE OF THIS TOOL COMES AT YOUR OWN RISK. THIS TOOL CANNOT REPLACE AND DOES NOT PROVIDE *EXPERT LEGAL ADVICE*.**

1. Add the JitPack repo:
```gradle
allprojects {
      repositories {
            ...
            maven { url 'https://jitpack.io' }
      }
}
```
2. Add the library:
```gradle
dependencies {
        implementation 'com.github.kasnder:app-consent-android:0.5'
}
```
3. Initialise the library by calling
```java
ConsentManager consentManager =
      new ConsentManager.Builder(this)
            .setPrivacyPolicy(Uri.parse("http://www.example.org/privacy"))
            .build();
```
4. If you want to, you can change the title (or message) in the consent flow by changing
   the `consent_title` (or `consent_msg`) string.
5. If you want to exclude certain libraries from the consent flow (e.g. the opt-in to the use of the
   Advertising ID), then use the `setExcludedLibraries()` method of the `ConsentManager.Builder`.
   For example, for Firebase Analytics: `.setExcludedLibraries(new String[]{"firebase_analytics"})`.
   You can see the identifiers of all currently managed libraries
   through `consentManager.getManagedLibraries()`.
6. By extending the class `net.kollnig.consent.library.Library`, you can hook connect further
   libraries. Use the `setCustomLibraries()` method of the `ConsentManager.Builder` to include them,
   e.g. `.setCustomLibraries(new Library[]{new CustomLibrary()})`.

You can check the example project in `app/` to see how the library is used.

## Details

This tool interacts with third-party libraries in three ways: 1) by setting options in
the `AndroidManifest.xml` file, 2) by calling functions of the third-party library directly (through
Reflection), and 3) by intercepting method calls to the third-party library and either adding more
privacy-preserving options or preventing the call to that function altogether.

The third method is the most invasive and only taken when no alternatives are available. It relies
on [YAHFA](https://github.com/PAGalaxyLab/YAHFA) (Yet Another Hook Framework for ART) to hook
functions of third-party libraries. Since YAHFA is only compatible with Android 7–12, lower Android
versions are not supported by the library. This might be addressed in future versions of this
library.

The following gives more details on how this tool interacts with third-party libraries.

### Google Firebase Analytics

**Purpose:** Analytics

**How consent is implemented:** Automatic data collection upon the first app start is managed through the `setAnalyticsCollectionEnabled` setting. This prevents the collection of analytics without user consent.

**Further reduced data collection:** The tool disables the settings `google_analytics_ssaid_collection_enabled`  (to prevent the collection of the ANDROID_ID) and `google_analytics_default_allow_ad_personalization_signals` (to prevent the use of data for ads). If you need the sharing of analytics data for ads, you can add the following to your `<application>` tag in your `AndroidManifest.xml` file:

```xml
<meta-data android:name="google_analytics_default_allow_ad_personalization_signals" tools:node="remove"/>
```

**Uses hooks:** No

**Further details:** <https://firebase.google.com/docs/analytics/configure-data-collection?platform=android>

### Google Crashlytics

**Purpose:** Crash reporting

**How consent is implemented:** Automatic data collection upon the first app start is prevented through the `setCrashlyticsCollectionEnabled` setting. This prevents the collection of crash reports without user consent.

**Further reduced data collection:** None, except that the `firebase_crashlytics_collection_enabled` flag is set to `false` in the `AndroidManifest.xml` file to implement consent.

**Uses hooks:** No

**Further details:** <https://firebase.google.com/docs/crashlytics/customize-crash-reports?platform=android>

### Google Ads

**Purpose:** Ads

**How consent is implemented:** If no consent is given, calls to the `init` and `loadAd` methods are blocked. This prevents communication with the Google Ads servers without user consent. As per Google’s consent policies, the use of Google Ads is only permitted with user consent (even of non-personalised ads).

**Further reduced data collection:** None, except that the `com.google.android.gms.ads.DELAY_APP_MEASUREMENT_INIT` flag is set to `false` in the `AndroidManifest.xml` file to implement consent.

**Uses hooks:** Yes

**Further details:** <https://developers.google.com/admob/ump/android/quick-start>

### Facebook SDK

**Purpose:** Various functionality, including analytics

**How consent is implemented:** Automatic data collection upon the first app start is prevented through the `setAutoInitEnabled` and `setAutoLogAppEventsEnabled` settings. This prevents the collection of analytics without user consent.

**Further reduced data collection:** None, except that the `com.facebook.sdk.AutoInitEnabled` and `com.facebook.sdk.AutoLogAppEventsEnabled` are flags set to `false` in the `AndroidManifest.xml` file to implement consent.

**Uses hooks:** No

**Further details:** <https://developers.facebook.com/docs/app-events/gdpr-compliance/>

### AppLovin

**Purpose:** Ads

**How consent is implemented:** Automatic data collection upon the first app start is prevented through the `setDoNotSell` and `setHasUserConsent` settings.

**Further reduced data collection:** None

**Uses hooks:** No

**Further details:** <https://dash.applovin.com/documentation/mediation/android/getting-started/privacy>

### Flurry

**Purpose:** Various functionality, including analytics 

**How consent is implemented:** If no consent is given, calls to the `build` method (from the `FlurryAgent.Builder` class) are blocked. This prevents the start of the SDK without user consent.

**Further reduced data collection:** None

**Uses hooks:** Yes

**Further details:** <https://developer.yahoo.com/flurry/docs/integrateflurry/android/>

### InMobi

**Purpose:** Ads

**How consent is implemented:** If no consent is given, then `gdpr_consent_available=false` and `gdpr=1` is passed to the `init()` method of InMobi.

**Further reduced data collection:** None

**Uses hooks:** Yes

**Further details:** <https://support.inmobi.com/monetize/android-guidelines/>

### AppsFlyer

**Purpose:** Ad attribution

**How consent is implemented:** If no consent is given, calls to the `start()` method of AppsFlyer are prevented.

**Further reduced data collection:** None

**Uses hooks:** Yes

**Further details:** <https://support.appsflyer.com/hc/en-us/articles/360001422989-User-opt-in-opt-out-in-the-AppsFlyer-SDK>

### ironSource

**Purpose:** Ads

**How consent is implemented:** Depending on the consent setting, `setConsent` and the `do_not_sell` flags are set.

**Further reduced data collection:** Depending on the consent setting, the `is_deviceid_optout` flag is set.

**Uses hooks:** No

**Further details:** <https://developers.is.com/ironsource-mobile/android/regulation-advanced-settings/#step-1>

### AdColony

**Purpose:** Ads

**How consent is implemented:** Depending on the consent setting, `setPrivacyFrameworkRequired` and `setPrivacyConsentString` are called. This happens both at the time of initialising the SDK (i.e. on calling `AdColony.configure()`) and when the user might change the setting (by calling `AdColony.setAppOptions()`). Other `appOptions` should be kept intact in this process.

**Further reduced data collection:** None

**Uses hooks:** Yes

**Further details:** <https://github.com/AdColony/AdColony-Android-SDK/wiki/Privacy-Laws>

### Vungle

**Purpose:** Ads

**How consent is implemented:** Consent is passed to the Vungle library through its `updateConsentStatus` setting, either setting this to `OPTED_IN` or `OPTED_OUT`. Additionally, the current consent signal is passed once the initialisation of the Vungle library is finished.

**Further reduced data collection:** None

**Uses hooks:** Yes

**Further details:** <https://support.vungle.com/hc/en-us/articles/360047780372-Advanced-Settings>

### Google Play Services Advertising Identifier Library

**Purpose:** User identification

**How consent is implemented:** Calls to the `getAdvertisingIdInfo` method throw an `IOException` if no consent is provided. The use of the `IOException` is one of the exceptions of the method signature and should be caught by apps in any case.

**Further reduced data collection:** None

**Uses hooks:** Yes

**Further details:** <https://developers.google.com/android/reference/com/google/android/gms/ads/identifier/AdvertisingIdClient>

## Contribution

Contributions to this project are highly welcome. Help is welcome with testing, improving the stability of the existing code, keeping up with changes of the third-party libraries and contributing new adapters for third-party libraries.

Feel free to file an issue or pull request with any of your ideas!

## License

This project is licensed under GPLv3.
