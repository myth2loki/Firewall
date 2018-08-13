package com.protect.kid.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AppCache {

	public static final String KEY_IF_SINCE_MODIFIED_SINCE = "If-Modified-Since";
	private static final String BLOCK_COUNT = "blockCount";

	public static void setBlockCount(Context context, int blockCount) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sp.edit();
		editor.putInt(BLOCK_COUNT, blockCount);
		editor.apply();
	}

	public static int getBlockCount(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getInt(BLOCK_COUNT, 1);
	}

	public static void setIfSinceModifiedSince(Context context, String ifSinceModifiedSince) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(KEY_IF_SINCE_MODIFIED_SINCE, ifSinceModifiedSince);
		editor.apply();
	}

	public static String getIfSinceModifiedSince(Context context) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getString(KEY_IF_SINCE_MODIFIED_SINCE, "Sun, 24 Jan 2016 14:17:36 GMT"); //raw中host文件版本
	}

}
