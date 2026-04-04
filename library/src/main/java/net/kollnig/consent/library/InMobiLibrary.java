package net.kollnig.consent.library;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import net.kollnig.consent.ConsentManager;
import net.kollnig.consent.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

public class InMobiLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "inmobi";

    public static void replacementInit(@NonNull final Context var0, @NonNull @Size(min = 32L, max = 36L) final String var1, @Nullable JSONObject var2, @Nullable final Object var3) {
        Log.d(TAG, "successfully hooked Inmobi");

        if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent(LIBRARY_IDENTIFIER))) {
            if (var2 == null)
                var2 = new JSONObject();

            try {
                var2.put("gdpr_consent_available", false);
                var2.put("gdpr", "1");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            HookCompat.callOriginal(
                    InMobiLibrary.class, "originalInit",
                    new Class[]{Context.class, String.class, JSONObject.class, Object.class},
                    null, var0, var1, var2, var3);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call original InMobi init", e);
        }
    }

    static final String TAG = "HOOKED";

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
    }

    // stub — used as key for HookCompat backup registration
    public static void originalInit(@NonNull final Context var0, @NonNull @Size(min = 32L, max = 36L) final String var1, @Nullable JSONObject var2, @Nullable final Object var3) {
        throw new RuntimeException("Hook not installed for InMobi init");
    }

    @Override
    public Library initialise(Context context) throws LibraryInteractionException {
        super.initialise(context);

        Class<?> baseClass = findBaseClass();
        try {
            Class<?> listenerClass = Class.forName("com.inmobi.sdk.SdkInitializationListener");
            Method methodOrig = baseClass.getMethod("init", Context.class, String.class, JSONObject.class, listenerClass);
            Method methodHook = InMobiLibrary.class.getMethod("replacementInit", Context.class, String.class, JSONObject.class, Object.class);
            Method methodBackup = InMobiLibrary.class.getMethod("originalInit", Context.class, String.class, JSONObject.class, Object.class);
            HookCompat.backupAndHook(methodOrig, methodHook, methodBackup);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException("Could not find method to hook", e);
        }

        return this;
    }

    @Override
    public void passConsentToLibrary(boolean consent) {
        // do nothing
    }

    @Override
    String getBaseClass() {
        return "com.inmobi.sdk.InMobiSdk";
    }

    @Override
    public int getConsentMessage() {
        return R.string.inmobi_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.inmobi;
    }
}
