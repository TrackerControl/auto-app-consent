package net.kollnig.consent.standards;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import lab.galaxy.yahfa.HookMain;

/**
 * Network-level GPC header injector using YAHFA hooks.
 *
 * Hooks at two levels to ensure ALL outgoing HTTP requests get the
 * Sec-GPC: 1 header, regardless of which SDK initiates them:
 *
 * 1. URL.openConnection() — catches all HttpURLConnection-based traffic
 * 2. OkHttp RealCall (if present) — catches OkHttp-based traffic from
 *    SDKs that bundle their own OkHttp
 *
 * This means every ad SDK, analytics library, and attribution tracker
 * will send the GPC signal with every HTTP request they make.
 */
public class GpcNetworkInterceptor {

    private static final String TAG = "GpcNetworkInterceptor";
    private static boolean initialized = false;

    /**
     * Install network-level hooks to inject GPC headers into all HTTP requests.
     * Safe to call multiple times — only installs hooks once.
     */
    public static synchronized void install(Context context) {
        if (initialized) return;

        hookUrlOpenConnection();
        hookOkHttpIfPresent();

        initialized = true;
        Log.d(TAG, "GPC network hooks installed");
    }

    // ---- Hook 1: URL.openConnection() ----
    // This is the lowest-level Java HTTP entry point on Android.
    // By hooking it, we intercept ALL HttpURLConnection-based traffic.

    public static URLConnection replacementOpenConnection(Object thiz) throws java.io.IOException {
        URLConnection conn = originalOpenConnection(thiz);
        if (GpcInterceptor.isEnabled() && conn instanceof HttpURLConnection) {
            ((HttpURLConnection) conn).setRequestProperty(
                    GpcInterceptor.GPC_HEADER_NAME,
                    GpcInterceptor.GPC_HEADER_VALUE);
        }
        return conn;
    }

    public static URLConnection originalOpenConnection(Object thiz) throws java.io.IOException {
        throw new RuntimeException("Hook not installed for URL.openConnection()");
    }

    private static void hookUrlOpenConnection() {
        try {
            String methodSig = "()Ljava/net/URLConnection;";
            Method methodOrig = (Method) HookMain.findMethodNative(
                    URL.class, "openConnection", methodSig);
            Method methodHook = GpcNetworkInterceptor.class.getMethod(
                    "replacementOpenConnection", Object.class);
            Method methodBackup = GpcNetworkInterceptor.class.getMethod(
                    "originalOpenConnection", Object.class);
            HookMain.backupAndHook(methodOrig, methodHook, methodBackup);
            Log.d(TAG, "Hooked URL.openConnection()");
        } catch (Exception e) {
            Log.w(TAG, "Could not hook URL.openConnection(): " + e.getMessage());
        }
    }

    // ---- Hook 2: OkHttp RealCall.getResponseWithInterceptorChain() ----
    // Many ad SDKs bundle OkHttp. We hook its internal call chain to add
    // the GPC header to the request before it's sent.
    //
    // We hook RealCall.execute() which is the synchronous entry point.
    // The async path (enqueue) also calls execute internally in most versions.

    public static Object replacementOkHttpExecute(Object thiz) throws Exception {
        if (GpcInterceptor.isEnabled()) {
            injectGpcIntoOkHttpCall(thiz);
        }
        return originalOkHttpExecute(thiz);
    }

    public static Object originalOkHttpExecute(Object thiz) throws Exception {
        throw new RuntimeException("Hook not installed for OkHttp execute()");
    }

    /**
     * Uses reflection to modify the request inside an OkHttp RealCall to add GPC header.
     */
    private static void injectGpcIntoOkHttpCall(Object realCall) {
        try {
            // RealCall has a field "originalRequest" or "request" depending on version
            java.lang.reflect.Field requestField = null;
            for (String fieldName : new String[]{"originalRequest", "request"}) {
                try {
                    requestField = realCall.getClass().getDeclaredField(fieldName);
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }
            if (requestField == null) return;

            requestField.setAccessible(true);
            Object request = requestField.get(realCall);
            if (request == null) return;

            // Check if header already present
            Method headerMethod = request.getClass().getMethod("header", String.class);
            Object existing = headerMethod.invoke(request, GpcInterceptor.GPC_HEADER_NAME);
            if (existing != null) return;

            // request.newBuilder().header("Sec-GPC", "1").build()
            Object builder = request.getClass().getMethod("newBuilder").invoke(request);
            builder.getClass()
                    .getMethod("header", String.class, String.class)
                    .invoke(builder, GpcInterceptor.GPC_HEADER_NAME, GpcInterceptor.GPC_HEADER_VALUE);
            Object newRequest = builder.getClass().getMethod("build").invoke(builder);

            // Write it back
            requestField.set(realCall, newRequest);
        } catch (Exception e) {
            Log.w(TAG, "Could not inject GPC into OkHttp request: " + e.getMessage());
        }
    }

    private static void hookOkHttpIfPresent() {
        try {
            Class<?> realCallClass = Class.forName("okhttp3.RealCall");
            String methodSig = "()Lokhttp3/Response;";

            Method methodOrig = (Method) HookMain.findMethodNative(
                    realCallClass, "execute", methodSig);
            Method methodHook = GpcNetworkInterceptor.class.getMethod(
                    "replacementOkHttpExecute", Object.class);
            Method methodBackup = GpcNetworkInterceptor.class.getMethod(
                    "originalOkHttpExecute", Object.class);
            HookMain.backupAndHook(methodOrig, methodHook, methodBackup);
            Log.d(TAG, "Hooked OkHttp RealCall.execute()");
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "OkHttp not present, skipping hook");
        } catch (Exception e) {
            Log.w(TAG, "Could not hook OkHttp: " + e.getMessage());
        }

        // Also try to hook the internal async path
        hookOkHttpAsyncCall();
    }

    // OkHttp async calls go through AsyncCall.execute() (Runnable on a dispatcher)
    public static void replacementAsyncExecute(Object thiz) {
        if (GpcInterceptor.isEnabled()) {
            injectGpcIntoAsyncCall(thiz);
        }
        originalAsyncExecute(thiz);
    }

    public static void originalAsyncExecute(Object thiz) {
        throw new RuntimeException("Hook not installed for OkHttp AsyncCall.execute()");
    }

    private static void injectGpcIntoAsyncCall(Object asyncCall) {
        try {
            // AsyncCall has a reference to the parent RealCall
            java.lang.reflect.Field callField = null;
            // Try common field names across OkHttp versions
            for (String fieldName : new String[]{"this$0", "call"}) {
                try {
                    callField = asyncCall.getClass().getDeclaredField(fieldName);
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }
            if (callField == null) return;

            callField.setAccessible(true);
            Object realCall = callField.get(asyncCall);
            if (realCall != null) {
                injectGpcIntoOkHttpCall(realCall);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not inject GPC into OkHttp async call: " + e.getMessage());
        }
    }

    private static void hookOkHttpAsyncCall() {
        try {
            Class<?> asyncCallClass = Class.forName("okhttp3.RealCall$AsyncCall");
            String methodSig = "()V";

            Method methodOrig = (Method) HookMain.findMethodNative(
                    asyncCallClass, "execute", methodSig);
            Method methodHook = GpcNetworkInterceptor.class.getMethod(
                    "replacementAsyncExecute", Object.class);
            Method methodBackup = GpcNetworkInterceptor.class.getMethod(
                    "originalAsyncExecute", Object.class);
            HookMain.backupAndHook(methodOrig, methodHook, methodBackup);
            Log.d(TAG, "Hooked OkHttp AsyncCall.execute()");
        } catch (ClassNotFoundException e) {
            // expected if OkHttp not present
        } catch (Exception e) {
            Log.w(TAG, "Could not hook OkHttp AsyncCall: " + e.getMessage());
        }
    }
}
