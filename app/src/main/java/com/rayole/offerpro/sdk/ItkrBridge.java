package com.rayole.offerpro.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ItkrBridge {
    private final Context appCtx;
    private final Activity activity;
    private final String encKey;

    ItkrBridge(Activity activity, String encKey) {
        this.activity = activity;
        this.appCtx = activity.getApplicationContext();
        this.encKey = encKey;
    }
    /** window.itkr.closeOfferWall("com.example.app") -> true/false */
    @JavascriptInterface
    public void closeOfferWall() throws Exception {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        activity.finish();
                        Log.d("itkrBridge", "Offer wall closed successfully");
                    } catch (Exception e) {
                        Log.e("itkrBridge", "Failed to close offer wall", e);
                    }
                }
            });
        }
    }

    /** window.itkr.validateInstall("com.example.app") -> true/false */
    @JavascriptInterface
    public String validateInstall(String packageName) throws Exception {
//        if (packageName == null || packageName.trim().isEmpty()) return false;

        Log.d("validateInstall","Here it's called "+packageName);

        Log.d("validateInstall","Here it's called "+packageName);
        Map<String, Object> payload = new HashMap<>();
        payload.put("package_name", packageName);

        boolean isInstalled = isInstalled(appCtx,packageName);
        if(isInstalled){
            payload.put("validated", true);
            String enc = Encryptor.encryptData(payload, encKey); //dynamic key

            Log.d("validateInstall","Here it's called true");
            return  enc;

        }else{
            payload.put("validated", false);
            String enc = Encryptor.encryptData(payload, encKey); //dynamic key

            Log.d("validateInstall","Here it's called false");
            return enc;
        }
    }
    // --- Usage Access helpers exposed to JS ---

    /** window.itkr.hasUsageAccess() -> true/false */
    @JavascriptInterface
    public boolean hasUsageAccess() {
        return AppUsageUtils.hasUsageAccess(appCtx);
    }

    /** window.itkr.openUsageAccessSettings() -> void (opens system screen) */
    @JavascriptInterface
    public void openUsageAccessSettings() {
        AppUsageUtils.openUsageAccessSettings(appCtx);
    }

    /**
     * window.itkr.validateAppUsage(pkg, startMs, endMs) -> encrypted JSON
     * JSON payload: { package_name, usage_time_ms, validated }
     * - validated = usage_time_ms > 0
     */
    @JavascriptInterface
    public String validateAppUsage(String packageName, String startTime, String endTime) throws Exception {
        Log.d("validateAppUsages", "package=" + packageName + " start=" + startTime + " end=" + endTime);

        long fromMs = AppUsageUtils.parseMillis(startTime);
        long toMs   = AppUsageUtils.parseMillis(endTime);
        if (fromMs <= 0L || toMs <= fromMs) {
            // if caller passes bad window, do a small default: last 24h
            long now = System.currentTimeMillis();
            toMs = now;
            fromMs = now - 24L * 60L * 60L * 1000L;
        }

//        long used = AppUsageUtils.getUsageMs(appCtx, packageName, fromMs, toMs);
        long used = AppUsageUtils.getUsageMs(appCtx, "com.rayolesoftware.tapnearn", System.currentTimeMillis() - 24L*60*60*1000L, System.currentTimeMillis());

        Log.d("validateAppUsages", "usage_time_ms=" + used + " validated=" + (used > 0));

        Map<String, Object> payload = new HashMap<>();
        payload.put("package_name", packageName);
        payload.put("usage_time_ms", used);
        payload.put("validated", used > 0);

        return Encryptor.encryptData(payload, encKey);
    }

    /** Fast path: direct check via PackageManager (works when visibility is declared). */
    public boolean isInstalledDirect(Context ctx, String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) return false;
        PackageManager pm = ctx.getPackageManager();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0));
            } else {
                // noinspection deprecation
                pm.getPackageInfo(packageName, 0);
            }
            return true;
        } catch (PackageManager.NameNotFoundException notFound) {
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Fallback: list launchable packages (needs the <queries> MAIN/LAUNCHER intent). */
    public Set<String> getLaunchablePackages(Context ctx) {
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = ctx.getPackageManager().queryIntentActivities(main, 0);
        Log.d("packages" ,infos.toString());
        Set<String> pkgs = new HashSet<>();
        for (ResolveInfo ri : infos) {
            if (ri.activityInfo != null && ri.activityInfo.packageName != null) {
                pkgs.add(ri.activityInfo.packageName);
            }
        }
        return pkgs;
    }

    /** Robust check: try direct, then fallback to launchable list. */
    public boolean isInstalled(Context ctx, String packageName) {
//        if (isInstalledDirect(ctx, packageName)) return true;
        return getLaunchablePackages(ctx).contains(packageName);
    }

    /** window.itkr.hostPackage() -> "com.your.host.app" */
//    @JavascriptInterface
//    public String hostPackage() {
//        return host.getPackageName();
//    }
}
