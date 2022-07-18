package net.kollnig.consent.app;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.facebook.FacebookSdk;
import com.google.firebase.analytics.FirebaseAnalytics;

import net.kollnig.consent.ConsentManager;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConsentManager consentManager = ConsentManager.getInstance(this, true, Uri.parse("http://www.example.org/privacy"));

        // Initialise Firebase
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Log some event
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "id");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "name");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        //Log.d(TAG, "Has consent: " + consentManager.hasConsent(FIREBASE_ANALYTICS_LIBRARY));
    }
}