package com.rayole.offerpro.sdk;

import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

final class WebViewUtils {
    private WebViewUtils() {}

    static void applySecureDefaults(WebView webView) {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setSupportMultipleWindows(true); // target=_blank
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccess(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) s.setSafeBrowsingEnabled(true);

        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);


        try { WebView.setWebContentsDebuggingEnabled(true); } catch (Throwable ignored) {}
    }
}