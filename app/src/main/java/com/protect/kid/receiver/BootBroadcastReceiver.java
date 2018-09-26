package com.protect.kid.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.constant.AppGlobal;
import com.protect.kid.core.util.VpnServiceUtil;
import com.protect.kid.util.SharedPrefUtil;

public class BootBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG = "BootBroadcastReceiver";
	private static final boolean DEBUG = BuildConfig.DEBUG;

	@Override
	public void onReceive(Context context, Intent intent) {
		String value = SharedPrefUtil.getValue(context, AppGlobal.GLOBAL_PREF_NAME,
				AppGlobal.IS_PROTECTED, "false");
		boolean isStart = "true".equals(value);
		if (DEBUG) {
			Log.d(TAG, "onReceive: should start = " + isStart);
		}
		if (isStart) {
			VpnServiceUtil.changeVpnRunningStatus(context, true);
		}
	}
}
