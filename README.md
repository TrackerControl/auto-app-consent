# App Consent

This repository shall help app developers implement consent in apps correctly.

At the moment, this project only checks if consent was correctly implemented for Google Firebase Analytics.

## Installation

1. Add the `library` to your own. project as a dependency (you can use the example project `app` as inspiration).
2. Initialise the library by calling `ConsentManager consentManager = ConsentManager.getInstance(this)` at the top of your app's `onCreate()` method.
3. Pass over user consent to the library with `consentManager.saveConsent(true|false)` -- **but only once that user has actually given consent**.

If the app crashes, then you called `FirebaseAnalytics.getInstance(this)` without consent, which you should not do.
