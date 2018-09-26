package com.protect.kid.receiver;

import android.content.Context;
import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.constant.AppGlobal;
import com.protect.kid.core.util.VpnServiceUtil;
import com.protect.kid.util.SharedPrefUtil;

class Helper {
    private static final String TAG = "receiver.Helper";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    public static void checkOrProtect(Context context) {
        String value = SharedPrefUtil.getValue(context, AppGlobal.GLOBAL_PREF_NAME,
                AppGlobal.PREF_IS_PROTECTED, "false");
        int retryCount = SharedPrefUtil.getInt(context, AppGlobal.GLOBAL_PREF_NAME,
                AppGlobal.PREF_RETRY_COUNT, 0);
        boolean isStart = "true".equals(value);
        if (DEBUG) {
            Log.d(TAG, "onReceive: should start = " + isStart);
        }
        if (isStart) {
            if (retryCount == 5) {
                // TODO 记录日志
            }
            VpnServiceUtil.changeVpnRunningStatus(context, true);
        }
    }
}
