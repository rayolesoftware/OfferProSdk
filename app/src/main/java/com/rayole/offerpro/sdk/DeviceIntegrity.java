package com.rayole.offerpro.sdk;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.provider.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class DeviceIntegrity {
    private DeviceIntegrity() {}

    /** Returns true if any VPN transport is active. */
    // DeviceIntegrity.java
    static boolean isVpnActive(Context ctx) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (android.net.Network net : cm.getAllNetworks()) {
                    NetworkCapabilities nc = cm.getNetworkCapabilities(net);
                    if (nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        return true;
                    }
                }
                return false;
            } else {
                @SuppressWarnings("deprecation")
                android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
                @SuppressWarnings("deprecation")
                boolean vpn = ni != null && ni.getType() == ConnectivityManager.TYPE_VPN;
                return vpn;
            }
        } catch (SecurityException se) {
            // ACCESS_NETWORK_STATE missing or restricted → treat as not on VPN
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    static boolean isDeveloperOptionsEnabled(Context ctx) {
        try {
            return Settings.Global.getInt(ctx.getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1;
        } catch (Throwable ignore) { return false; }
    }

    static boolean isAdbEnabled(Context ctx) {
        try {
            return Settings.Global.getInt(ctx.getContentResolver(),
                    Settings.Global.ADB_ENABLED, 0) == 1;
        } catch (Throwable ignore) { return false; }
    }

    static boolean isEmulator() {
        String f = safeLower(Build.FINGERPRINT);
        String m = safeLower(Build.MODEL);
        String b = safeLower(Build.BRAND);
        String d = safeLower(Build.DEVICE);
        String p = safeLower(Build.PRODUCT);
        String man = safeLower(Build.MANUFACTURER);
        String h = safeLower(Build.HARDWARE);

        String[] suspects = {"generic","sdk_gphone","emulator","ranchu","goldfish","aosp","vbox"};
        for (String s : suspects) {
            if (f.contains(s) || m.contains(s) || b.contains(s) || d.contains(s) || p.contains(s) || h.contains(s) || man.contains(s)) {
                return true;
            }
        }
        return ("google".equals(b) && (p.contains("sdk") || m.contains("sdk")));
    }

    static boolean isRooted() {
        boolean testKeys = Build.TAGS != null && Build.TAGS.contains("test-keys");

        String[] suPaths = {
                "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
                "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su"
        };
        boolean suExists = false;
        for (String path : suPaths) {
            if (new File(path).exists()) { suExists = true; break; }
        }

        boolean whichSu = false;
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            whichSu = (proc.waitFor() == 0);
        } catch (Throwable ignore) {}

        return testKeys || suExists || whichSu;
    }

    private static String safeLower(String s) { return s == null ? "" : s.toLowerCase(); }

    /** Build the blocked reasons list (strings you want to surface). */
    static List<String> getBlockedReasons(Context ctx) {
        List<String> reasons = new ArrayList<>();
        if (isVpnActive(ctx)) reasons.add("vpn");
        if (isEmulator()) reasons.add("emulator");
        if (isRooted()) reasons.add("root");
        if (isAdbEnabled(ctx)) reasons.add("adb");
        if (isDeveloperOptionsEnabled(ctx)) reasons.add("dev");
        return reasons;
    }
}
