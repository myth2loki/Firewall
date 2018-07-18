package com.timedancing.easyfirewall.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Administrator on 2018/7/17.
 */

public class SharedPrefUtil {

    public static void saveValue(Context context, String prefName, String key, String value) {
        SharedPreferences spf = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        spf.edit().putString(key, value).apply();
    }

    public static String getValue(Context context, String prefName, String key, String defValue) {
        SharedPreferences spf = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        return spf.getString(key, defValue);
    }
}
