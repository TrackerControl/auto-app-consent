package net.kollnig.consent.library;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import net.kollnig.consent.ConsentManager;
import net.kollnig.consent.R;

import java.lang.reflect.Method;

public class GoogleAdsLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "google_ads";

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
    }

    static final String TAG = "HOOKED";

    // Hook for MobileAds.initialize(Context)
    public static void replacementMethod(@NonNull Context context) {
        Log.d(TAG, "successfully hooked GAds");

        if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent(LIBRARY_IDENTIFIER)))
            return;

        try {
            HookCompat.callOriginal(
                    GoogleAdsLibrary.class, "originalMethod",
                    new Class[]{Context.class}, null, context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call original GAds initialize(Context)", e);
        }
    }

    public static void originalMethod(@NonNull Context context) {
        throw new RuntimeException("Hook not installed");
    }

    // Hook for MobileAds.initialize(Context, OnInitializationCompleteListener)
    public static void replacementMethod(@NonNull Context context, @NonNull Object listener) {
        Log.d(TAG, "successfully hooked GAds");

        if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent(LIBRARY_IDENTIFIER)))
            return;

        try {
            HookCompat.callOriginal(
                    GoogleAdsLibrary.class, "originalMethod",
                    new Class[]{Context.class, Object.class}, null, context, listener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call original GAds initialize(Context, Listener)", e);
        }
    }

    public static void originalMethod(@NonNull Context context, @NonNull Object listener) {
        throw new RuntimeException("Hook not installed");
    }

    // Hook for BaseAdView.loadAd(AdRequest)
    public static void replacementLoadAd(Object thiz, @NonNull Object adRequest) {
        Log.d(TAG, "successfully hooked GAds loadAd");

        if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent(LIBRARY_IDENTIFIER)))
            return;

        try {
            HookCompat.callOriginal(
                    GoogleAdsLibrary.class, "originalLoadAd",
                    new Class[]{Object.class, Object.class}, thiz, adRequest);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call original GAds loadAd", e);
        }
    }

    public static void originalLoadAd(Object thiz, @NonNull Object adRequest) {
        throw new RuntimeException("Hook not installed");
    }

    @Override
    public Library initialise(Context context) throws LibraryInteractionException {
        super.initialise(context);

        // Hook BaseAdView.loadAd(AdRequest)
        try {
            Class<?> baseAdViewClass = Class.forName("com.google.android.gms.ads.BaseAdView");
            Class<?> adRequestClass = Class.forName("com.google.android.gms.ads.AdRequest");

            Method methodOrig = baseAdViewClass.getMethod("loadAd", adRequestClass);
            Method methodHook = GoogleAdsLibrary.class.getMethod("replacementLoadAd", Object.class, Object.class);
            Method methodBackup = GoogleAdsLibrary.class.getMethod("originalLoadAd", Object.class, Object.class);
            HookCompat.backupAndHook(methodOrig, methodHook, methodBackup);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        // Hook MobileAds.initialize(Context) and initialize(Context, Listener)
        Class<?> baseClass = findBaseClass();
        try {
            Method methodOrig = baseClass.getMethod("initialize", Context.class);
            Method methodHook = GoogleAdsLibrary.class.getMethod("replacementMethod", Context.class);
            Method methodBackup = GoogleAdsLibrary.class.getMethod("originalMethod", Context.class);
            HookCompat.backupAndHook(methodOrig, methodHook, methodBackup);

            Class<?> listenerClass = Class.forName(
                    "com.google.android.gms.ads.initialization.OnInitializationCompleteListener");
            Method methodOrig2 = baseClass.getMethod("initialize", Context.class, listenerClass);
            Method methodHook2 = GoogleAdsLibrary.class.getMethod("replacementMethod", Context.class, Object.class);
            Method methodBackup2 = GoogleAdsLibrary.class.getMethod("originalMethod", Context.class, Object.class);
            HookCompat.backupAndHook(methodOrig2, methodHook2, methodBackup2);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException("Could not find method to hook", e);
        }

        return this;
    }

    @Override
    public void passConsentToLibrary(boolean consent) {
        if (!consent)
            getContext().deleteSharedPreferences("admob");
    }

    @Override
    String getBaseClass() {
        return "com.google.android.gms.ads.MobileAds";
    }

    @Override
    public int getConsentMessage() {
        return R.string.google_ads_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.google_ads;
    }
}
