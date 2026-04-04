package net.kollnig.consent.library;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import net.kollnig.consent.ConsentManager;
import net.kollnig.consent.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

public class AdColonyLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "adcolony";

    public static boolean replacementInit(Context var0, Object var1, @NonNull String var2) {
        Log.d(TAG, "successfully hooked AdColony");

        if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent(LIBRARY_IDENTIFIER))) {
            var1 = getAppOptions(var1, false);
        }

        try {
            return (boolean) HookCompat.callOriginal(
                    AdColonyLibrary.class, "originalInit",
                    new Class[]{Context.class, Object.class, String.class},
                    null, var0, var1, var2);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call original AdColony configure", e);
            return false;
        }
    }

    static final String TAG = "HOOKED";

    private @NonNull
    static Object getAppOptions(Object options, boolean consent) {
        try {
            if (options == null) {
                Class<?> baseClass = Class.forName("com.adcolony.sdk.AdColony");
                Method getAppOptions = baseClass.getMethod("getAppOptions");
                options = getAppOptions.invoke(null);
            }

            Class<?> optionsClass = getOptionsClass();
            if (options == null) {
                options = optionsClass.getConstructor().newInstance();
            }

            String GDPR = (String) optionsClass.getDeclaredField("GDPR").get(null);

            Method setPrivacyFrameworkRequired = optionsClass.getMethod("setPrivacyFrameworkRequired", String.class, boolean.class);
            setPrivacyFrameworkRequired.invoke(options, GDPR, true);

            String consentString = consent ? "1" : "0";
            Method setPrivacyConsentString = optionsClass.getMethod("setPrivacyConsentString", String.class, String.class);
            setPrivacyConsentString.invoke(options, GDPR, consentString);

            return options;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchFieldException e) {
            throw new RuntimeException("Failed to interact with AdColony SDK.");
        }
    }

    // stub — used as key for HookCompat backup registration
    public static boolean originalInit(Context var0, Object var1, @NonNull String var2) {
        throw new RuntimeException("Hook not installed for AdColony configure");
    }

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
    }

    @NonNull
    private static Class<?> getOptionsClass() throws ClassNotFoundException {
        return Class.forName("com.adcolony.sdk.AdColonyAppOptions");
    }

    private static Method findInitMethod() throws LibraryInteractionException {
        List<Method> matchingMethods = new LinkedList<>();
        try {
            Class<?> baseClass = Class.forName("com.adcolony.sdk.AdColony");

            for (Method method : baseClass.getDeclaredMethods()) {
                Modifier.isStatic(method.getModifiers());

                if (!method.getReturnType().equals(boolean.class))
                    continue;

                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 3)
                    continue;

                if (!parameterTypes[0].equals(Context.class)
                        || !parameterTypes[1].equals(getOptionsClass())
                        || !parameterTypes[2].equals(String.class))
                    continue;

                matchingMethods.add(method);
            }
        } catch (ClassNotFoundException e) {
            throw new LibraryInteractionException("Could not find target class.");
        }

        if (matchingMethods.size() > 1)
            throw new LibraryInteractionException("Could not determine target method.");

        return matchingMethods.get(0);
    }

    @Override
    public Library initialise(Context context) throws LibraryInteractionException {
        super.initialise(context);

        Method methodOrig = findInitMethod();
        try {
            Method methodHook = AdColonyLibrary.class.getMethod("replacementInit", Context.class, Object.class, String.class);
            Method methodBackup = AdColonyLibrary.class.getMethod("originalInit", Context.class, Object.class, String.class);
            HookCompat.backupAndHook(methodOrig, methodHook, methodBackup);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find method to hook", e);
        }

        return this;
    }

    @Override
    public void passConsentToLibrary(boolean consent) throws LibraryInteractionException {
        Class<?> baseClass = findBaseClass();
        try {
            Object options = getAppOptions(null, consent);

            Method setAppOptions = baseClass.getMethod("setAppOptions", options.getClass());
            setAppOptions.invoke(null, options);
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            throw new LibraryInteractionException("Could not save settings to AdColony.");
        }
    }

    @Override
    String getBaseClass() {
        return "com.adcolony.sdk.AdColony";
    }

    @Override
    public int getConsentMessage() {
        return R.string.adcolony_consent_msg;
    }

    @Override
    public int getName() {
        return R.string.adcolony;
    }
}
