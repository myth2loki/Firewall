package com.protect.kid.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.protect.kid.BuildConfig;

class ProtectCheckReceiver extends BroadcastReceiver {
    private static final String TAG = "ProtectCheckReceiver";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    @Override
    public void onReceive(Context context, Intent intent) {
        Helper.checkOrProtect(context);
    }
}
