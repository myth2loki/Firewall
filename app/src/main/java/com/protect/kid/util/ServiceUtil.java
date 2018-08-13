package com.protect.kid.util;


import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.util.Log;

import com.protect.kid.BuildConfig;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

public class ServiceUtil {
    public static final String TAG = "ServiceUtil";
    public static final boolean DEBUG = BuildConfig.DEBUG;

    public static boolean isServiceAvailable(Context context, Class<?> serviceClass) {
        ComponentName componentName = new ComponentName(context, serviceClass);
        try {
            ServiceInfo info = context.getPackageManager().getServiceInfo(componentName, 0);
            return info != null;
        } catch (PackageManager.NameNotFoundException e) {
            if (DEBUG) {
                Log.w(TAG, "isServiceAvailable: service not found", e);
            }
        }
        return false;
    }

    public static boolean isVpnConnected() {
        try {
            Enumeration<NetworkInterface> niList = NetworkInterface.getNetworkInterfaces();
            if(niList != null) {
                for (NetworkInterface intf : Collections.list(niList)) {
                    if(!intf.isUp() || intf.getInterfaceAddresses().size() == 0) {
                        continue;
                    }
                    if ("tun0".equals(intf.getName()) || "ppp0".equals(intf.getName())){
                        return true;
                    }
                }
            }
        } catch (Throwable e) {
            if (DEBUG) {
                Log.w(TAG, "isVpnConnected: error", e);
            }
        }
        return false;
    }

    public static boolean isWarning(Context context) {
        return false;
    }
}
