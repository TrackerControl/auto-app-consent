package net.kollnig.consent.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.adcolony.sdk.AdColony;
import com.appsflyer.AppsFlyerLib;
import com.flurry.android.FlurryAgent;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.inmobi.sdk.InMobiSdk;
import com.ironsource.mediationsdk.IronSource;
import com.vungle.warren.InitCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;

import net.kollnig.consent.ConsentManager;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConsentManager consentManager =
                new ConsentManager.Builder(this)
                        .setShowConsent(true)
                        .setPrivacyPolicy(Uri.parse("http://www.example.org/privacy"))
                        //.setExcludedLibraries(new String[]{"firebase_analytics"})
                        .build();

        //consentManager.clearConsent();
        //consentManager.askConsent();

        Log.d(TAG, "Detected and managed libraries: "
                + String.join(", ", consentManager.getManagedLibraries()));

        // Initialise Firebase
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        AppsFlyerLib.getInstance().start(this);

        InMobiSdk.init(this, "ACCOUNT_ID_ABCDEFGHIJKLMNOPQRSTUVW");

        new FlurryAgent.Builder().build(this, "ABCDEFGH");

        // Log some event
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "id");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "name");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        MobileAds.initialize(this);
        AdRequest adRequest = new AdRequest.Builder().build();
        AdView adview = new AdView(this);
        adview.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
        adview.setAdSize(AdSize.BANNER);
        adview.loadAd(adRequest);

        //Log.d(TAG, "Has consent: " + consentManager.hasConsent(FIREBASE_ANALYTICS_LIBRARY));

        //AppsFlyerLib.getInstance();

        IronSource.setMetaData("do_not_sell", "true");
        IronSource.setMetaData("is_deviceid_optout", "true");
        IronSource.setConsent(false);

        Vungle.init("abcedf", this, new InitCallback() {
            @Override
            public void onSuccess() {
                // do nothing
            }

            @Override
            public void onError(VungleException exception) {
                // do nothing
            }

            @Override
            public void onAutoCacheAdAvailable(String placementId) {
                // do nothing
            }
        });

        IronSource.init(this, "abcdefghhijkl");

        // a(Context var0, AdColonyAppOptions var1, @NonNull String var2)
        AdColony.configure(this, "abcdefhashsh");
        // AdColony.setAppOptions();
        // AdColonyAppOptions

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);

        //AdvertisingIdClient.getAdvertisingIdInfo();

        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                AdvertisingIdClient.Info idInfo = null;
                try {
                    idInfo = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String advertId = null;
                try{
                    advertId = idInfo.getId();
                }catch (NullPointerException e){
                    e.printStackTrace();
                }

                return advertId;
            }

            @Override
            protected void onPostExecute(String advertId) {
                Toast.makeText(getApplicationContext(), advertId, Toast.LENGTH_SHORT).show();
            }

        };
        task.execute();
    }

    protected void onResume() {
        super.onResume();
        IronSource.onResume(this);
    }

    protected void onPause() {
        super.onPause();
        IronSource.onPause(this);
    }
}