package com.protect.kid.util;

import android.content.Context;
import android.content.pm.PackageManager;

import com.protect.kid.constant.AppDebug;

public class PhoneStateUtil {

	public static String getVersionName(Context context) {
		String result = "";

		try {
			result = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0)
					.versionName;
		} catch (PackageManager.NameNotFoundException ex) {
			if (AppDebug.IS_DEBUG) {
				ex.printStackTrace(System.err);
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
		} catch (PackageManager.NameNotFoundException ex) {
			if (AppDebug.IS_DEBUG) {
				ex.printStackTrace(System.err);
			}
		}

		return result;
	}

}
