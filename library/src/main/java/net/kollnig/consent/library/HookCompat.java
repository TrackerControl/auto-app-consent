package net.kollnig.consent.library;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.lsposed.lsplant.LSPlant;

/**
 * Hooking compatibility layer wrapping LSPlant.
 *
 * LSPlant supports Android 5.0 through 16+ and is actively maintained
 * by the LSPosed team. It handles JIT compilation, method inlining,
 * and OEM ART modifications — all things that broke YAHFA.
 *
 * This class provides a YAHFA-compatible API so existing Library classes
 * need minimal changes: just replace HookMain calls with HookCompat calls.
 */
public class HookCompat {

    private static final String TAG = "HookCompat";
    private static boolean initialized = false;

    // Maps "ClassName#methodName#paramCount" -> LSPlant backup Method
    private static final ConcurrentHashMap<String, Method> backups = new ConcurrentHashMap<>();

    /**
     * Initialize LSPlant. Called automatically before first hook.
     */
    public static synchronized void init() {
        if (initialized) return;
        LSPlant.init(HookCompat.class.getClassLoader());
        initialized = true;
    }

    /**
     * Hook a method, replacing it with hookMethod.
     * The original can later be called via callOriginal().
     *
     * This replaces YAHFA's backupAndHook() pattern.
     *
     * @param target     the method to hook (found via reflection or findMethodNative)
     * @param hook       the static replacement method
     * @param backupStub the stub method (previously used by YAHFA; now used as key only)
     */
    public static void backupAndHook(Method target, Method hook, Method backupStub) {
        init();
        Method backup = LSPlant.hookMethod(target, hook);
        if (backup == null) {
            Log.e(TAG, "Failed to hook: " + target.getDeclaringClass().getName()
                    + "." + target.getName());
            return;
        }
        backup.setAccessible(true);
        String key = stubKey(backupStub);
        backups.put(key, backup);
        Log.d(TAG, "Hooked: " + target.getDeclaringClass().getName()
                + "." + target.getName());
    }

    /**
     * Call the original (pre-hook) method.
     * Use this from replacement methods instead of calling the old "originalXxx()" stubs.
     *
     * @param backupStub the same stub method passed to backupAndHook()
     * @param thiz       instance (null for static methods)
     * @param args       method arguments
     * @return the return value
     */
    public static Object callOriginal(Method backupStub, Object thiz, Object... args)
            throws InvocationTargetException, IllegalAccessException {
        String key = stubKey(backupStub);
        Method backup = backups.get(key);
        if (backup == null) {
            throw new RuntimeException("No backup registered for: " + key
                    + ". Was backupAndHook() called?");
        }
        return backup.invoke(thiz, args);
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
