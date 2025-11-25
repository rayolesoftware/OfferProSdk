// android/offerpro_sdk/src/main/java/com/offerpro/sdk/AppUsageUtils.java
package com.rayole.offerpro.sdk;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

final class AppUsageUtils {
    private AppUsageUtils() {}

    /** True if the app has "Usage Access" permission enabled in Settings. */
    public static boolean hasUsageAccess(Context ctx) {
        try {
            AppOpsManager appOps = (AppOpsManager) ctx.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    ctx.getPackageName()
            );
            if (mode == AppOpsManager.MODE_DEFAULT) {
                // Note: PACKAGE_USAGE_STATS is a “special” permission; this check returns
                // PERMISSION_GRANTED only on some builds. MODE_ALLOWED is the most reliable signal.
                return ctx.checkCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS")
                        == PackageManager.PERMISSION_GRANTED;
            } else {
                return mode == AppOpsManager.MODE_ALLOWED;
            }
        } catch (Throwable t) {
//            Log.d("hasUsageAccess", "hasUsageAccess failed", t);
            return false;
        }
    }

    /** Opens the system screen where the user can grant Usage Access. */
    static void openUsageAccessSettings(Context ctx) {
        String pkg = ctx.getPackageName();

        // 1) Try ACTION_USAGE_ACCESS_SETTINGS with package: Uri (works on many OEMs)
        try {
            Intent perApp = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setData(Uri.fromParts("package", pkg, null));
            if (perApp.resolveActivity(ctx.getPackageManager()) != null) {
                ctx.startActivity(perApp);
                return;
            }
        } catch (Throwable t) {
            Log.e("openUsageAccessSettings", "Per-app usage access deep link failed", t);
        }

        // 2) Fallback: open the generic “Usage access” list
        try {
            Intent list = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (list.resolveActivity(ctx.getPackageManager()) != null) {
                ctx.startActivity(list);
                return;
            }
        } catch (Throwable t) {
            Log.e("openUsageAccessSettings", "Generic usage access list failed", t);
        }

        // 3) Last resort: App Info page — user can go to “Special app access” from here
        try {
            Intent appInfo = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", pkg, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (appInfo.resolveActivity(ctx.getPackageManager()) != null) {
                ctx.startActivity(appInfo);
            }
        } catch (Throwable t) {
            Log.e("openUsageAccessSettings", "Could not open any usage access screen", t);
        }
    }

    /**
     * Precise total foreground time between [fromMs, toMs) for a package, computed from UsageEvents.
     * Falls back to aggregated stats if events are unavailable.
     *
     * Requires the user to grant Usage Access to the **host app** (PACKAGE_USAGE_STATS/AppOps).
     */
    public static long getUsageMs(Context ctx, String pkg, long fromMs, long toMs) {
        if (TextUtils.isEmpty(pkg) || toMs <= fromMs) return 0L;

        UsageStatsManager usm = (UsageStatsManager) ctx.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return 0L;

        long total = 0L;
        long lastFgStart = -1L;

        try {
            UsageEvents events = usm.queryEvents(fromMs, toMs);
            if (events != null) {
                UsageEvents.Event ev = new UsageEvents.Event();
                while (events.hasNextEvent()) {
                    events.getNextEvent(ev);
                    if (!pkg.equals(ev.getPackageName())) continue;

                    final int type = ev.getEventType();

                    // Foreground “start” markers vary by API level
                    boolean isStart =
                            type == UsageEvents.Event.MOVE_TO_FOREGROUND
                                    || (Build.VERSION.SDK_INT >= 29 && type == UsageEvents.Event.ACTIVITY_RESUMED);

                    // Foreground “stop” markers vary by API level
                    boolean isStop =
                            type == UsageEvents.Event.MOVE_TO_BACKGROUND
                                    || (Build.VERSION.SDK_INT >= 29 && type == UsageEvents.Event.ACTIVITY_PAUSED);

                    if (isStart) {
                        // only record if we’re not already “in foreground”
                        if (lastFgStart < 0) lastFgStart = ev.getTimeStamp();
                    } else if (isStop) {
                        if (lastFgStart > 0) {
                            long end = Math.min(ev.getTimeStamp(), toMs);
                            if (end > lastFgStart) total += (end - lastFgStart);
                            lastFgStart = -1L;
                        }
                    }
                }

                // If we ended the window still in foreground, close at toMs
                if (lastFgStart > 0 && toMs > lastFgStart) {
                    total += (toMs - lastFgStart);
                }

                if (total > 0) return total;
            }
        } catch (SecurityException se) {
            // Missing usage access permission
            Log.e("validateAppUsages", "Usage access not granted", se);
        } catch (Throwable t) {
            Log.e("validateAppUsages", "queryEvents failed, will try aggregated fallback", t);
        }

        // Fallback: aggregated daily stats (coarse; may return 0 for partial windows)
        try {
            List<UsageStats> list = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, fromMs, toMs);
            if (list != null) {
                for (UsageStats s : list) {
                    if (pkg.equals(s.getPackageName())) {
                        // On modern Android this is milliseconds in foreground within the bucket(s).
                        long v = s.getTotalTimeInForeground();
                        if (v > total) total = v;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return total;
    }

    /** Convenience: parse millis from string; allows empty or non-numeric -> 0. */
    static long parseMillis(String s) {
        if (TextUtils.isEmpty(s)) return 0L;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) { return 0L; }
    }
}
