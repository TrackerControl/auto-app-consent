# App Consent

This repository shall help app developers implement consent in apps correctly. This helps
compliance with the GDPR, CCPA and other legal regimes.
The motivation for this project that our research at Oxford found that less than 4% of Android apps implement any form of consent: <https://www.usenix.org/conference/soups2021/presentation/kollnig>

Specifically, this tool targets the following compliance problems:

- Failure to to implement any consent flows
- Sharing of data with third-party companies without consent (violating Articles 7 and (35)1 GDPR)
- Non technically-necessary accessing or storing of data on smartphone (violating Article 5(3) ePrivacy Directive)
- Sharing more data than necessary (violating Article 4(1) GDPR)

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

*Note that the use of Google and Facebook services in the EU
is [likely illegal even with user consent](https://noyb.eu/en/austrian-dsb-eu-us-data-transfers-google-analytics-illegal)
, because data is sent to the US and can be used for unlawful surveillance of EU citizens. The same
applies to other US-based services.*

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

You can check the example project in `app/` to see how the library is used.

The project also disables the sharing of analytics data with Google Ads, which is enabled by default.
If you need this functionality, you can add the following to your `<application>` tag in your `AndroidManifest.xml` file:

```xml
<meta-data android:name="google_analytics_default_allow_ad_personalization_signals" tools:node="remove"/>
```
