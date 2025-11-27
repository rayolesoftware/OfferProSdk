// android/offerpro_sdk/src/main/java/com/offerpro/sdk/OfferProSdk.java
package com.rayole.offerpro.sdk;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Debug;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;


public final class OfferProSdk {
    private static volatile OfferProSdk INSTANCE;
    private WeakReference<Context> appCtxRef;
    private SdkConfig config;
    private String sdkVersion = "1.0.0";

    private OfferProSdk() {}
    public static OfferProSdk getInstance() {
        if (INSTANCE == null) {
            synchronized (OfferProSdk.class) {
                if (INSTANCE == null) INSTANCE = new OfferProSdk();
            }
        }
        return INSTANCE;
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
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("link", finalUrl);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(activity, finalUrl, Toast.LENGTH_LONG).show();
            activity.startActivity(i);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build encrypted URL: " + e.getMessage(), e);
        }
    }
}
