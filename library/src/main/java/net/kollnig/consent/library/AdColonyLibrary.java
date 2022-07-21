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

import lab.galaxy.yahfa.HookMain;

public class AdColonyLibrary extends Library {
    public static final String LIBRARY_IDENTIFIER = "adcolony";
    static final String TAG = "HOOKED";

    public static boolean replacementInit(Context var0, Object var1, @NonNull String var2) {
        Log.d(TAG, "successfully hooked AdColony");

        if (!Boolean.TRUE.equals(ConsentManager.getInstance().hasConsent(LIBRARY_IDENTIFIER))) {
            var1 = getAppOptions(false);
        }

        return originalInit(var0, var1, var2);
    }

    // this method will be replaced by hook
    public static boolean originalInit(Context var0, Object var1, @NonNull String var2) {
        throw new RuntimeException("Could not overwrite original AdColony method");
    }

    private @NonNull
    static Object getAppOptions(boolean consent) {
        //AdColonyAppOptions options = new AdColonyAppOptions()
        //        .setPrivacyFrameworkRequired(AdColonyAppOptions.GDPR, true)
        //        .setPrivacyConsentString(AdColonyAppOptions.GDPR, consent);

        try {
            Class optionsClass = getOptionsClass();
            Object options = optionsClass.getConstructor().newInstance();

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

    @NonNull
    private static Class<?> getOptionsClass() throws ClassNotFoundException {
        return Class.forName("com.adcolony.sdk.AdColonyAppOptions");
    }

    // AdColony.configure()
    // a(Context var0, AdColonyAppOptions var1, @NonNull String var2)
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

        // AdColony.configure()
        // a(Context var0, AdColonyAppOptions var1, @NonNull String var2)
        // a(Landroid/content/Context;Lcom/adcolony/sdk/AdColonyAppOptions;Ljava/lang/String;)Z

        Class baseClass = findBaseClass();
        String methodName = findInitMethod().getName();
        String methodSig = "(Landroid/content/Context;Lcom/adcolony/sdk/AdColonyAppOptions;Ljava/lang/String;)Z";

        try {
            Method methodOrig = (Method) HookMain.findMethodNative(baseClass, methodName, methodSig);
            Method methodHook = AdColonyLibrary.class.getMethod("replacementInit", Context.class, Object.class, String.class);
            Method methodBackup = AdColonyLibrary.class.getMethod("originalInit", Context.class, Object.class, String.class);
            HookMain.backupAndHook(methodOrig, methodHook, methodBackup);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not overwrite method");
        }

        return this;
    }

    @Override
    public void passConsentToLibrary(boolean consent) throws LibraryInteractionException {
        Class<?> baseClass = findBaseClass();
        if (baseClass != null) {
            try {
                Object options = getAppOptions(consent);

                // AdColony.setAppOptions();
                Method setAppOptions = baseClass.getMethod("setAppOptions", options.getClass());
                setAppOptions.invoke(null, options);
            } catch (NoSuchMethodException
                    | IllegalAccessException
                    | InvocationTargetException e) {
                throw new LibraryInteractionException("Could not save settings to AdColony.");
            }
        }
    }

    @NonNull
    @Override
    public String getId() {
        return LIBRARY_IDENTIFIER;
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
