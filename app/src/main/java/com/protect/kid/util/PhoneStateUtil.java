package com.protect.kid.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.protect.kid.BuildConfig;

public class PhoneStateUtil {
	private static final String TAG = "PhoneStateUtil";
	private static final boolean DEBUG = BuildConfig.DEBUG;

	public static String getVersionName(Context context) {
		String result = "";

		try {
			result = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0)
					.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			if (DEBUG) {
				Log.e(TAG, "getVersionName: failed", e);
			}
		}
		return result;
	}

	public static int getVersionCode(Context context) {
		int result = 0;

		try {
			result = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0)
					.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			if (DEBUG) {
				Log.e(TAG, "getVersionCode: failed", e);
			}
		}

		return result;
	}

}
