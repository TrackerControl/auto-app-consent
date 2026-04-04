package net.kollnig.consent.standards;

/**
 * OkHttp Interceptor that adds the Global Privacy Control (Sec-GPC: 1) header
 * to all outgoing HTTP requests when GPC is enabled.
 *
 * Usage with OkHttp:
 *   OkHttpClient client = new OkHttpClient.Builder()
 *       .addInterceptor(new GpcOkHttpInterceptor())
 *       .build();
 *
 * The interceptor checks GpcInterceptor.isEnabled() for each request,
 * so it respects runtime changes to the GPC setting.
 *
 * Note: This class uses reflection to avoid a hard dependency on OkHttp.
 * If OkHttp is not in the classpath, this class should not be instantiated.
 */
public class GpcOkHttpInterceptor {

    /**
     * Creates an OkHttp Interceptor instance via reflection.
     * Returns null if OkHttp is not available.
     *
     * The returned object implements okhttp3.Interceptor and can be passed
     * directly to OkHttpClient.Builder.addInterceptor().
     */
    public static Object createInterceptor() {
        try {
            // Check if OkHttp is available
            Class.forName("okhttp3.Interceptor");
            return java.lang.reflect.Proxy.newProxyInstance(
                    GpcOkHttpInterceptor.class.getClassLoader(),
                    new Class[]{Class.forName("okhttp3.Interceptor")},
                    (proxy, method, args) -> {
                        if ("intercept".equals(method.getName()) && args.length == 1) {
                            return interceptChain(args[0]);
                        }
                        return method.invoke(proxy, args);
                    }
            );
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Intercepts an OkHttp chain, adding the GPC header if enabled.
     * Uses reflection to avoid compile-time dependency on OkHttp.
     */
    private static Object interceptChain(Object chain) throws Exception {
        // chain.request()
        Object request = chain.getClass().getMethod("request").invoke(chain);

        if (GpcInterceptor.isEnabled()) {
            // request.newBuilder()
            Object builder = request.getClass().getMethod("newBuilder").invoke(request);

            // builder.header("Sec-GPC", "1")
            builder.getClass()
                    .getMethod("header", String.class, String.class)
                    .invoke(builder, GpcInterceptor.GPC_HEADER_NAME, GpcInterceptor.GPC_HEADER_VALUE);

            // builder.build()
            request = builder.getClass().getMethod("build").invoke(builder);
        }

        // chain.proceed(request)
        return chain.getClass()
                .getMethod("proceed", Class.forName("okhttp3.Request"))
                .invoke(chain, request);
    }
}
