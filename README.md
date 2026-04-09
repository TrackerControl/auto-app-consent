# Auto App Consent for App Developers

*Developed by Konrad Kollnig*

This library helps Android app developers implement consent and privacy compliance. It addresses the finding from [our research at Oxford](https://www.usenix.org/conference/soups2021/presentation/kollnig) that less than 4% of Android apps implement any form of consent.

The library works at three levels:

1. **Industry-standard consent signals** — Writes IAB TCF v2.2 and US Privacy (CCPA) strings to SharedPreferences, which most ad SDKs read natively
2. **Build-time bytecode transforms** — A Gradle plugin injects consent checks into SDK methods at compile time (no runtime hooking)
3. **SDK-specific configuration** — Sets manifest flags and calls SDK consent APIs via reflection

## Supported Standards

| Standard | What it does | How it works |
|---|---|---|
| **IAB TCF v2.2** | GDPR consent signal for ad SDKs | Writes `IABTCF_*` keys to SharedPreferences — SDKs read these natively |
| **IAB US Privacy** | CCPA "do not sell" signal | Writes `IABUSPrivacy_String` to SharedPreferences |
| **Global Privacy Control** | Browser/web "do not sell" signal | `Sec-GPC: 1` header for WebViews and app HTTP requests |

## Supported SDKs

| SDK | Consent mechanism |
|---|---|
| Google Firebase Analytics | Reflection (`setAnalyticsCollectionEnabled`) + manifest flags |
| Google Crashlytics | Reflection (`setCrashlyticsCollectionEnabled`) + manifest flag |
| Google Ads | Build-time transform (blocks `initialize`, `loadAd`) + manifest flag |
| Facebook SDK | Reflection (`setAutoInitEnabled`, `setAutoLogAppEventsEnabled`) + manifest flags |
| AppLovin | Reflection (`setDoNotSell`, `setHasUserConsent`) |
| Flurry | Build-time transform (blocks `build`) |
| InMobi | Build-time transform (blocks `init`) |
| AppsFlyer | Build-time transform (blocks `start`) + reflection (`stop`) |
| ironSource | Reflection (`setConsent`, `setMetaData`) |
| AdColony | Build-time transform (blocks `configure`) + reflection (`setAppOptions`) |
| Vungle | Build-time transform (blocks `init`) + reflection (`updateConsentStatus`) |
| Google Advertising ID | Build-time transform (throws `IOException` on `getAdvertisingIdInfo`) |

## Installation

### 1. Add the library dependency

Add the JitPack repo and library:

```gradle
// settings.gradle
dependencyResolutionManagement {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}

// app/build.gradle
dependencies {
    implementation 'com.github.kasnder:app-consent-android:0.8'
}
```

### 2. Apply the Gradle plugin (for build-time transforms)

```gradle
// settings.gradle
pluginManagement {
    includeBuild('plugin')  // or use published coordinates
}

// app/build.gradle
plugins {
    id 'com.android.application'
    id 'net.kollnig.consent.plugin'
}
```

The plugin automatically transforms SDK bytecode during compilation — no runtime hooking needed.

### 3. Initialize the ConsentManager

```java
ConsentManager consentManager =
    new ConsentManager.Builder(this)
        .setPrivacyPolicy(Uri.parse("https://example.com/privacy"))
        .setShowConsent(true)
        // Industry standard consent signals
        .enableTcf()                    // IAB TCF v2.2
        .setGdprApplies(true)           // Set based on user location
        .setPublisherCountryCode("DE")  // Your country (ISO 3166-1)
        .enableUsPrivacy()              // IAB US Privacy (CCPA)
        .setCcpaApplies(true)           // Set based on user location
        .enableGpc()                    // Global Privacy Control
        .build();
```

### 4. Optional: GPC for WebViews

If your app uses WebViews, wrap them with `GpcWebViewClient` to send the GPC signal to websites:

```java
webView.setWebViewClient(new GpcWebViewClient());
webView.getSettings().setJavaScriptEnabled(true);
```

This injects both the `Sec-GPC: 1` HTTP header and `navigator.globalPrivacyControl = true`.

### 5. GPC coverage across HTTP stacks

When `.enableGpc()` is called, the `Sec-GPC: 1` header is automatically injected across all major HTTP clients used by Android SDKs:

| HTTP client | Coverage mechanism | SDKs using it |
|---|---|---|
| **OkHttp3** | Build-time bytecode transform on `Request.Builder.build()` | Most modern ad SDKs |
| **OkHttp2** | Build-time bytecode transform on `Request.Builder.build()` | Older SDKs |
| **Cronet** | Build-time bytecode transform on `UrlRequest.Builder.build()` | Google SDKs (Ads, Play Services) |
| **HttpURLConnection** | `URLStreamHandlerFactory` at runtime (standard Java API) | Firebase, some older SDKs |
| **WebViews** | `GpcWebViewClient` (header + `navigator.globalPrivacyControl`) | Any in-app web content |

For your app's own HTTP requests, you can also use `GpcInterceptor.applyTo()` manually:

```java
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
GpcInterceptor.applyTo(conn);  // Adds Sec-GPC: 1 if GPC is enabled
```

## Configuration Options

| Builder method | Description |
|---|---|
| `.setPrivacyPolicy(Uri)` | **Required.** Link to your privacy policy |
| `.setShowConsent(boolean)` | Show the consent dialog (default: true) |
| `.setExcludedLibraries(String[])` | Exclude specific SDKs from consent management |
| `.setCustomLibraries(Library[])` | Add custom SDK handlers |
| `.enableTcf()` | Enable IAB TCF v2.2 signals |
| `.enableTcf(cmpId, version)` | Enable TCF with a registered CMP ID |
| `.setGdprApplies(boolean)` | Whether GDPR applies to this user |
| `.setPublisherCountryCode(String)` | Publisher's country (ISO 3166-1 alpha-2) |
| `.enableUsPrivacy()` | Enable IAB US Privacy (CCPA) string |
| `.setCcpaApplies(boolean)` | Whether CCPA applies to this user |
| `.enableGpc()` | Enable Global Privacy Control |

## How the Build-Time Transform Works

The Gradle plugin uses the Android Gradle Plugin's `AsmClassVisitorFactory` to modify SDK bytecode during compilation. For each SDK method that needs consent gating, it injects a check at the beginning:

```java
// Before (original SDK bytecode):
MobileAds.initialize(context) {
    ... sdk code ...
}

// After (transformed at build time):
MobileAds.initialize(context) {
    if (!ConsentManager.getInstance().hasConsent("google_ads")) return;
    ... sdk code ...
}
```

This approach:
- Works on **all Android versions** (no ART runtime dependency)
- Causes **no Google Play Protect flags** (no method hooking)
- Has **zero runtime overhead** for the consent check wiring
- Is **deterministic** — the transform is applied at compile time, not at runtime

### Adding a new SDK to the transform

Add one line to `ConsentTransformRules.java`:

```java
addRule("com/example/sdk/Tracker", "init",
        "(Landroid/content/Context;)V",
        "tracker_id", Action.BLOCK);
```

## Architecture

```
┌─ Build Time ──────────────────────────────────────────────────┐
│  Gradle Plugin (ConsentTransformRules → ConsentMethodVisitor) │
│  Injects consent checks into SDK bytecode                     │
└───────────────────────────────────────────────────────────────┘

┌─ Runtime ─────────────────────────────────────────────────────┐
│  ConsentManager                                               │
│  ├── Consent UI (AlertDialog with per-library opt-in)         │
│  ├── Consent storage (SharedPreferences)                      │
│  ├── TCF v2.2 signals (IABTCF_* in default SharedPreferences) │
│  ├── US Privacy string (IABUSPrivacy_String)                  │
│  ├── GPC: WebViews (GpcWebViewClient)                         │
│  ├── GPC: HttpURLConnection (GpcUrlHandler — Java API)        │
│  └── SDK-specific reflection (setEnabled, setConsent, etc.)   │
└───────────────────────────────────────────────────────────────┘
```

## Implementation Details

### Industry Standard Signals

**IAB TCF v2.2**: Writes all standard `IABTCF_*` keys to the app's default SharedPreferences. Most ad SDKs (Google Ads, AppLovin, InMobi, ironSource, etc.) read these natively before making network requests, so no interception is needed.

**IAB US Privacy (CCPA)**: Writes the `IABUSPrivacy_String` (e.g., `1YNN` for consent given, `1YYN` for opted out). Ad SDKs that support CCPA read this natively.

**Global Privacy Control**: The `Sec-GPC: 1` header is injected across all HTTP stacks: OkHttp3/OkHttp2/Cronet via build-time bytecode transforms, HttpURLConnection via `URLStreamHandlerFactory`, and WebViews via `GpcWebViewClient`. For consent signaling to SDKs that read preferences before making requests, TCF/US Privacy are also used.

### SDK-Specific Details

**Google Firebase Analytics**: Managed through `setAnalyticsCollectionEnabled`. Manifest flags disable `google_analytics_ssaid_collection_enabled` and `google_analytics_default_allow_ad_personalization_signals`.

**Google Crashlytics**: Managed through `setCrashlyticsCollectionEnabled`. Manifest flag disables `firebase_crashlytics_collection_enabled`.

**Google Ads**: Build-time transform blocks `MobileAds.initialize()` and `BaseAdView.loadAd()` without consent. Manifest flag sets `DELAY_APP_MEASUREMENT_INIT`.

**Facebook SDK**: Managed through `setAutoInitEnabled` and `setAutoLogAppEventsEnabled`. Manifest flags disable `AutoInitEnabled` and `AutoLogAppEventsEnabled`.

**Google Advertising ID**: Build-time transform makes `getAdvertisingIdInfo()` throw `IOException` without consent (one of the method's declared exceptions).

*Note: The use of US-based services in the EU may be legally problematic even with consent, due to surveillance concerns. See [noyb.eu](https://noyb.eu/en/austrian-dsb-eu-us-data-transfers-google-analytics-illegal) for details.*

## Existing Consent Solutions for Mobile

| Name | Open Source | Free | Automatic setup |
|---|---|---|---|
| [GDPRConsent](https://github.com/DavidEdwards/GDPRConsent) | Yes | Yes | No |
| [gdprsdk](https://github.com/gdprsdk/android-gdpr-library) | Yes | Yes | IAB only |
| [Google User Messaging Platform](https://developers.google.com/admob/ump/android/quick-start) | No | Yes | IAB only |
| [Usercentrics](https://usercentrics.com/in-app-sdk/) | Partial | No (€4+/mo) | IAB only |
| [OneTrust](https://www.onetrust.com/products/mobile-app-consent/) | No | No | Unknown |
| **This tool** | **Yes** | **Yes** | **Yes — automatic for 12 SDKs + TCF + CCPA + GPC** |

## Contribution

Contributions are highly welcome:
- Testing across different SDK versions
- Adding transform rules for new SDKs
- Improving the consent UI

Feel free to file an issue or pull request.

## License

This project is licensed under GPLv3.
