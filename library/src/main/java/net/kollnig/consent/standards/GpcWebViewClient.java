package net.kollnig.consent.standards;

import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;
import java.util.Map;

/**
 * WebViewClient that injects Global Privacy Control signals into WebViews.
 *
 * This does two things:
 * 1. Adds the Sec-GPC: 1 HTTP header to all WebView requests
 * 2. Injects navigator.globalPrivacyControl = true via JavaScript
 *
 * Usage:
 *   webView.setWebViewClient(new GpcWebViewClient());
 *   // or wrap an existing client:
 *   webView.setWebViewClient(new GpcWebViewClient(existingClient));
 *
 * Make sure JavaScript is enabled on the WebView:
 *   webView.getSettings().setJavaScriptEnabled(true);
 */
public class GpcWebViewClient extends WebViewClient {

    private static final String GPC_JS_INJECTION =
            "javascript:Object.defineProperty(navigator,'globalPrivacyControl'," +
                    "{value:true,writable:false,configurable:false});";

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (GpcInterceptor.isEnabled()) {
            Map<String, String> headers = new HashMap<>();
            headers.put(GpcInterceptor.GPC_HEADER_NAME, GpcInterceptor.GPC_HEADER_VALUE);
            view.loadUrl(request.getUrl().toString(), headers);
            return true;
        }
        return super.shouldOverrideUrlLoading(view, request);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (GpcInterceptor.isEnabled()) {
            view.evaluateJavascript(
                    "Object.defineProperty(navigator,'globalPrivacyControl'," +
                            "{value:true,writable:false,configurable:false});",
                    null);
        }
    }
}
