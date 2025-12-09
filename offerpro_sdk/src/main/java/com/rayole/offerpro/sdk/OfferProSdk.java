// android/offerpro_sdk/src/main/java/com/offerpro/sdk/OfferProSdk.java
package com.rayole.offerpro.sdk;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public final class OfferProSdk {
    private static volatile OfferProSdk INSTANCE;
    private WeakReference<Context> appCtxRef;
    private SdkConfig config;
    private String sdkVersion = "1.0.0";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private OfferProSdk() {}

    public static OfferProSdk getInstance() {
        if (INSTANCE == null) {
            synchronized (OfferProSdk.class) {
                if (INSTANCE == null) INSTANCE = new OfferProSdk();
            }
        }
        return INSTANCE;
    }

    public interface MegaOfferCallback {
        void onResult(MegaOffer offer);
    }

    public synchronized void initialize(Context applicationContext, SdkConfig config) {
        if (applicationContext == null) throw new IllegalArgumentException("applicationContext null");
        if (config == null) throw new IllegalArgumentException("config null");
        this.appCtxRef = new WeakReference<>(applicationContext.getApplicationContext());
        this.config = config;
    }

    public boolean isInitialized() {
        return appCtxRef != null && appCtxRef.get() != null && config != null;
    }

    public SdkConfig getConfig() {
        if (!isInitialized()) throw new IllegalStateException("OfferProSdk not initialized");
        return config;
    }

    /** Open the WebView Activity with ?enc=<...>&app_id=<...> */
    public void openWall(Activity activity) {
        if (!isInitialized()) throw new IllegalStateException("OfferProSdk not initialized");

        try {
            // Build payload (everything except appId/startUrl)
            Map<String, Object> payload = new HashMap<>();
            payload.put("device_id", config.deviceId == null ? "" : config.deviceId); //remove
            payload.put("advertising_id", config.advertisingId == null ? "" : config.advertisingId);
            payload.put("user_email", config.userEmail == null ? "" : config.userEmail);
            payload.put("user_id", config.userId == null ? "" : config.userId);
            payload.put("app_id", config.appId);
            payload.put("user_country", config.userCountry == null ? "" : config.userCountry);
            payload.put("sdk_version", sdkVersion);

//            Log.d("UserData", payload.toString());
            String enc = Encryptor.encryptData(payload, config.encKey);

//            String base = config.startUrl != null ? config.startUrl : "https://wall.offerpro.io";
            String base = "https://sdk.offerpro.io";
            String sep = "?";
            String finalUrl = base
                    + sep + "enc=" + Uri.encode(enc)
                    + "&app_id=" + config.appId;

//            Log.d("final url",finalUrl);
            Intent i = new Intent(activity, MainActivity.class);
            i.putExtra(MainActivity.EXTRA_START_URL, finalUrl);
//            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
//            ClipData clip = ClipData.newPlainText("link", finalUrl);
//            clipboard.setPrimaryClip(clip);
//            Toast.makeText(activity, finalUrl, Toast.LENGTH_LONG).show();
            activity.startActivity(i);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build encrypted URL: " + e.getMessage(), e);
        }
    }

    private static String readStream(@NonNull InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }


    // ✅ NEW: fetch MegaOffer from your backend
    public void fetchMegaOffer(final MegaOfferCallback callback) {
        if (!isInitialized()) throw new IllegalStateException("OfferProSdk not initialized");

        // Run network on background thread (pseudo-code)
        new Thread(() -> {
        HttpURLConnection conn = null;
            try {
                // Build payload (everything except appId/startUrl)
                Map<String, Object> payload = new HashMap<>();
                payload.put("device_id", config.deviceId == null ? "" : config.deviceId); //remove
                payload.put("advertising_id", config.advertisingId == null ? "" : config.advertisingId);
                payload.put("user_email", config.userEmail == null ? "" : config.userEmail);
                payload.put("user_id", config.userId == null ? "" : config.userId);
                payload.put("app_id", config.appId);
                payload.put("user_country", config.userCountry == null ? "" : config.userCountry);
                payload.put("sdk_version", sdkVersion);

            Log.d("fetchMegaOffer", payload.toString());

            String enc = Encryptor.encryptData(payload, config.encKey);

                String urlStr =
                        "https://server.offerpro.io/api/tasks/list_mega_games/"
                                + "?ordering=-cpc&no_pagination=false&page=" + 1;

                Log.d("fetchMegaOffer", urlStr);

                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject bodyJson = new JSONObject();
                bodyJson.put("enc", enc);
                bodyJson.put("app_id", config.appId);

                Log.d("fetchMegaOffer", bodyJson.toString());

                String bodyStr = bodyJson.toString();
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, StandardCharsets.UTF_8)
                );
                writer.write(bodyStr);
                writer.flush();
                writer.close();
                os.close();

                int code = conn.getResponseCode();
                Log.d("fetchMegaOffer", String.valueOf(code));

                if (code == HttpURLConnection.HTTP_OK) {
                    String responseStr = readStream(conn.getInputStream());

                    // Server returns a JSON array (same as your Dart code)
                    JSONArray arr = new JSONArray(responseStr);
                    if (arr.length() == 0) {
                        postResult(callback, null);
                        return;
                    }

                    JSONObject first = arr.getJSONObject(0);
                    MegaOffer mega = MegaOffer.fromJson(first);
                    postResult(callback, mega);
                } else {
                    Log.d("error", String.valueOf(code));
                    // non-200 -> treat as no mega offer
                    postResult(callback, null);
                }
            } catch (Exception e)  {
                Log.d("error", e.toString());
                postResult(callback, null);
            }finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private void postResult(final MegaOfferCallback callback,
                            final MegaOffer mega) {
        mainHandler.post(() -> callback.onResult(mega));
    }


    /** Open the MegaWallWebView Activity with ?enc=<...>&app_id=<...> */
    public void openMegaWall(Activity activity, String url) {
        if (!isInitialized()) throw new IllegalStateException("OfferProSdk not initialized");

        try {
            if (url.trim().isEmpty()) return;

            Intent intent = new Intent(activity, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_START_URL, url);
            activity.startActivity(intent);

//            Log.d("final url",finalUrl);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build encrypted URL: " + e.getMessage(), e);
        }
    }
}
