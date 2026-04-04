package net.kollnig.consent.library;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import top.canyie.pine.Pine;
import top.canyie.pine.callback.MethodHook;

/**
 * Hooking compatibility layer wrapping Pine.
 *
 * Pine supports Android 5.0-14+ and provides a Java API for ART method hooking.
 * Unlike LSPlant (pure native C++, no Java API) or YAHFA (Android 7-12 only,
 * abandoned), Pine offers both broad version support and a usable Java interface.
 *
 * This class provides a YAHFA-compatible API so existing Library classes
 * need minimal changes: just replace HookMain calls with HookCompat calls.
 */
public class HookCompat {

    private static final String TAG = "HookCompat";
    private static boolean initialized = false;

    // Maps stub key -> hooked original method
    private static final ConcurrentHashMap<String, Method> hookedMethods = new ConcurrentHashMap<>();

    /**
     * Initialize Pine. Called automatically before first hook.
     */
    public static synchronized void init() {
        if (initialized) return;
        Pine.ensureInitialized();
        Pine.disableJitInline();
        initialized = true;
    }

    /**
     * Hook a method: intercept calls to target, redirect to hook, allow
     * calling the original via callOriginal().
     *
     * The hook method should check consent and then call
     * HookCompat.callOriginal() to invoke the original if consent is granted.
     *
     * @param target     the method to hook
     * @param hook       the static replacement method
     * @param backupStub the stub method (used as key for callOriginal dispatch)
     */
    public static void backupAndHook(Method target, Method hook, Method backupStub) {
        init();
        try {
            String key = stubKey(backupStub);
            hookedMethods.put(key, target);

            // Hook the target method. Pine intercepts the call and lets us
            // call the original via Pine.invokeOriginalMethod().
            // The replacement logic is in the Library classes' static hook methods
            // which are wired up by the caller.
            Pine.hook(target, new MethodHook() {
                @Override
                public void beforeCall(Pine.CallFrame callFrame) throws Throwable {
                    // Invoke the static hook method with the same arguments
                    Object[] args = callFrame.getArgs();

                    // Build argument list: for instance methods, Pine provides
                    // thisObject separately; our hook methods expect it as first arg
                    Object[] hookArgs;
                    if (java.lang.reflect.Modifier.isStatic(target.getModifiers())) {
                        hookArgs = args;
                    } else {
                        hookArgs = new Object[args.length + 1];
                        hookArgs[0] = callFrame.thisObject;
                        System.arraycopy(args, 0, hookArgs, 1, args.length);
                    }

                    Object result = hook.invoke(null, hookArgs);
                    callFrame.setResult(result);
                }
            });

            Log.d(TAG, "Hooked: " + target.getDeclaringClass().getName()
                    + "." + target.getName());
        } catch (Exception e) {
            Log.e(TAG, "Hook failed for " + target.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Call the original (pre-hook) method.
     */
    public static Object callOriginal(Method backupStub, Object thiz, Object... args)
            throws InvocationTargetException, IllegalAccessException {
        String key = stubKey(backupStub);
        Method target = hookedMethods.get(key);
        if (target == null) {
            throw new RuntimeException("No hook registered for: " + key);
        }
        return Pine.invokeOriginalMethod(target, thiz, args);
    }

    /**
     * Convenience: call original by class + method name + param types.
     */
    public static Object callOriginal(Class<?> libraryClass, String stubMethodName,
                                      Class<?>[] stubParamTypes,
                                      Object thiz, Object... args)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method stub = libraryClass.getMethod(stubMethodName, stubParamTypes);
        return callOriginal(stub, thiz, args);
    }

    private static String stubKey(Method m) {
        StringBuilder sb = new StringBuilder();
        sb.append(m.getDeclaringClass().getName()).append('#').append(m.getName());
        for (Class<?> p : m.getParameterTypes()) {
            sb.append('#').append(p.getName());
        }
        return sb.toString();
    }
}
