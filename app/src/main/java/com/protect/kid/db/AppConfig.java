package com.protect.kid.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.protect.kid.util.PhoneStateUtil;

public class AppConfig {

	private static final String LOCK_PASSWORD = "lockPassword";
	private static final String SHOW_GUIDE_PAGE = "shouldShowGuidePage";

	public static String getLockPassword(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getString(LOCK_PASSWORD, "");
	}

	public static void setLockPassword(Context context, String lockPassword) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(LOCK_PASSWORD, lockPassword);
		editor.apply();
	}

	public static boolean isShouldShowGuidePage(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		String key = String.format("%s_%s", SHOW_GUIDE_PAGE, PhoneStateUtil.getVersionName(context));
		return sp.getBoolean(key, true);
	}

	public static void setShouldShowGuidePage(Context context, boolean isShouldShowGuidePage) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sp.edit();
		String key = String.format("%s_%s", SHOW_GUIDE_PAGE, PhoneStateUtil.getVersionName(context));
		editor.putBoolean(key, isShouldShowGuidePage);
		editor.apply();
	}
}
