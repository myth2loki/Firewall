package com.timedancing.easyfirewall.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefUtil {

    public static void remove(Context context, String prefName, String key) {
        SharedPreferences spf = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        spf.edit().remove(key).apply();
    }

    public static void saveValue(Context context, String prefName, String key, String value) {
        SharedPreferences spf = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        spf.edit().putString(key, value).apply();
    }

    public static String getValue(Context context, String prefName, String key, String defValue) {
        SharedPreferences spf = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        return spf.getString(key, defValue);
    }

    public static void saveLong(Context context, String prefName, String key, long value) {
        SharedPreferences spf = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        spf.edit().putLong(key, value).apply();
    }

    public static long getLong(Context context, String prefName, String key, long defValue) {
        SharedPreferences spf = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        return spf.getLong(key, defValue);
    }

    public static void saveInt(Context context, String prefName, String key, int value) {
        SharedPreferences spf = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        spf.edit().putInt(key, value).apply();
    }

    public static int getInt(Context context, String prefName, String key, int defValue) {
        SharedPreferences spf = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        return spf.getInt(key, defValue);
    }
}
