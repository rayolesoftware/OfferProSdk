package com.rayole.offerpro.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebChromeClient.FileChooserParams;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import java.util.List;

public class MainActivity extends Activity {

    public static final String EXTRA_START_URL = "start_url";
    private static final String TAG = "SDKWebView";
    private static final String BASE_URL_DEFAULT = "https://sdk.offerpro.io";

    private WebView webView;
    private String initialUrl;         // first page, used ONLY on cold start
    private boolean firstLoadDone = false;
    private boolean pendingFilePicker  = false; // ← add this
    private String lastLoadedUrl = null;        // track what’s on screen
    // Used to skip reloads when returning from external intents (Play Store, file chooser, etc.)
    private boolean pendingExternalNav = false;

    // for file chooser
    private ValueCallback<Uri[]> filePathCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);


        WebView.setWebContentsDebuggingEnabled(true);

        // Build the very first URL once; we won't keep reloading it on resume.
        String passed = getIntent() != null ? getIntent().getStringExtra(EXTRA_START_URL) : null;
        initialUrl = (passed == null || passed.isEmpty()) ? BASE_URL_DEFAULT : passed;

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(webView);

        cm.setAcceptThirdPartyCookies(webView, true);

        WebViewUtils.applySecureDefaults(webView);


        // JS interface (if you need it)
        String encKey = OfferProSdk.getInstance().getConfig().encKey;
        webView.addJavascriptInterface(new ItkrBridge(this, encKey), "itkr");

        webView.setWebViewClient(new BrowserClient());
        webView.setWebChromeClient(new BrowserChrome());

        // Downloads
        webView.setDownloadListener(new DownloadListener() {
            @Override public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                // Let the system handle it (browser / DownloadManager / Play Store / etc.)
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                catch (ActivityNotFoundException ignore) {}
            }
        });

        if (savedInstanceState != null) {
            // Restore the exact state (history, current page, form data, etc.)
            webView.restoreState(savedInstanceState);
            firstLoadDone = true;
        } else {
            loadWithIntegrityIfChanged();
            firstLoadDone = true;
        }
    }


    /** Don’t reload onResume – this preserves the page when returning from Play Store. */
    @Override protected void onResume() {
        super.onResume();
        // If we just returned from an external intent, DO NOT reload.
        if (pendingExternalNav) {
            pendingExternalNav = false;
            return;
        }
//        loadWithIntegrityIfChanged();
    }



    /** Only load if the URL we SHOULD be on (based on integrity) differs from what we last loaded. */
    private void loadWithIntegrityIfChanged() {
            List<String> reasons = DeviceIntegrity.getBlockedReasons(this);
            String desired = buildBlockedUrl(BASE_URL_DEFAULT, reasons); // first reason only
            if (!TextUtils.equals(desired, initialUrl)) {
                webView.loadUrl(desired);
                // lastLoadedUrl will be updated in onPageFinished
            }
//            webView.loadUrl(initialUrl);
    }

        /** Compose base + (?|&)blocked=<firstReason> (or no param if none). */
    private static String buildBlockedUrl(String base, List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) return base;
        String first = reasons.get(0);
        String sep = base.contains("?") ? "&" : "?";
        return base + sep + "blocked=" + Uri.encode(first);
    }


    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 10001) {
            if (filePathCallback == null) return;

            Uri[] uris = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    final int n = data.getClipData().getItemCount();
                    uris = new Uri[n];
                    for (int i = 0; i < n; i++) {
                        uris[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    uris = new Uri[]{ data.getData() };
                }
            }

            // Always complete the callback — this prevents reloads on many OEMs
            filePathCallback.onReceiveValue(uris);
            filePathCallback = null;

            // We just returned from external UI; ensure no reload on resume paths
            // (onResume will run after this; the pendingExternalNav flag is still true,
            // and onResume will skip any loadUrl)
            return;
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            try { ((ViewGroup) webView.getParent()).removeView(webView); } catch (Throwable ignore) {}
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    // ---------------- Clients ----------------

    private class BrowserClient extends WebViewClient {
        @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
//            Log.d(TAG, "onPageStarted " + url);
        }

        @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return handleUrl(request.getUrl());
            }
            return false;
        }

        @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleUrl(Uri.parse(url));
        }

        private boolean handleUrl(Uri uri) {
            String scheme = uri.getScheme();
            String host = uri.getHost();

            // Let WebView handle normal http/https
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                // If it’s a Play Store web page, you might prefer to open the Play Store app instead
                if (host != null && (host.contains("play.google.com") || host.contains("apps.apple.com"))) {
                    openExternal(uri);
                    return true;
                }
                return false; // load inside WebView
            }

            // Everything else: intent://, market://, tel:, mailto:, geo:, etc.
            openExternal(uri);
            return true;
        }

        private void openExternal(Uri uri) {
            try {
                pendingExternalNav = true; // ← ADD THIS
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "No handler for " + uri, e);
            }
        }
    }

    private class BrowserChrome extends WebChromeClient {
        // Support window.open / target=_blank
        @Override public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
            // Create a temporary WebView to get the URL, then decide where to open it.
            WebView tmp = new WebView(view.getContext());
            tmp.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(WebView v, String url) {
                    // Open new windows externally like a browser would
                    try {
                        pendingExternalNav = true; // ← ADD THIS
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (ActivityNotFoundException ignore) {}
                    return true;
                }
            });
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(tmp);
            resultMsg.sendToTarget();
            return true;
        }

        // HTML5 geolocation prompt → auto-grant or show your own UI
        @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            // you can gate this behind a user setting/permission if you want
            callback.invoke(origin, true, false);
        }

        // Camera/mic (WebRTC)
        @Override public void onPermissionRequest(PermissionRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                request.grant(request.getResources()); // or filter these
            }
        }

        // File chooser for <input type="file">
        @Override public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            // Close any previous pending callback to avoid OEM reload quirks
            if (MainActivity.this.filePathCallback != null) {
                MainActivity.this.filePathCallback.onReceiveValue(null);
            }
            MainActivity.this.filePathCallback = filePathCallback;

            Intent chooserIntent;
            try {
                chooserIntent = fileChooserParams.createIntent();
            } catch (Exception e) {
                // Complete callback even on failure
                MainActivity.this.filePathCallback.onReceiveValue(null);
                MainActivity.this.filePathCallback = null;
                return false;
            }

            try {
                // IMPORTANT: Mark that we’re leaving the app to an external picker
                pendingExternalNav = true;
                startActivityForResult(chooserIntent, 10001);
            } catch (ActivityNotFoundException e) {
                // Gracefully fail, do NOT reload page
                MainActivity.this.filePathCallback.onReceiveValue(null);
                MainActivity.this.filePathCallback = null;
                pendingExternalNav = false; // nothing actually opened
                return false;
            }
            return true;
        }
    }
}
