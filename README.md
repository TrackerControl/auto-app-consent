# App Consent

This repository shall help app developers implement consent in apps correctly.

At the moment, this project only checks if consent was correctly implemented for Google Firebase Analytics. Correctly here means that the consent actually works and that no data is sent to Google Analytics without consent.

*Note that the use of Google Analytics in the EU is [likely illegal even with user consent](https://noyb.eu/en/austrian-dsb-eu-us-data-transfers-google-analytics-illegal), because data is sent to the US and can be used for unlawful surveillance of EU citizens.*

## Motivation / Background

The ultimate aim is to help app developers configure SDKs in more privacy-preserving ways, and automatically asks users for consent when required.

The background of this that our research at Oxford found that less than 4% of Android apps implement any form of consent: <https://www.usenix.org/conference/soups2021/presentation/kollnig>

## Installation

1. Add the `library` to your own project as a dependency (you can use the example project `app` as inspiration).
2. Initialise the library by calling `ConsentManager consentManager = ConsentManager.getInstance(this)` at the top of your app's `onCreate()` method.
3. Pass over user consent to the library with `consentManager.saveConsent(true|false)` -- **but only once that user has actually given consent**.

If the app crashes, then you called `FirebaseAnalytics.getInstance(this)` without consent, which you should not do.
