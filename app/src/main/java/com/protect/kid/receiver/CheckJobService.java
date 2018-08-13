package com.protect.kid.receiver;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.constant.AppGlobal;
import com.protect.kid.core.util.VpnServiceHelper;
import com.protect.kid.util.SharedPrefUtil;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CheckJobService extends JobService {
    public static final String TAG = "CheckJobService";
    public static final boolean DEBUG = BuildConfig.DEBUG;

    @Override
    public boolean onStartJob(JobParameters params) {
        String value = SharedPrefUtil.getValue(getApplicationContext(), AppGlobal.GLOBAL_PREF_NAME,
                AppGlobal.IS_PROTECTED, "false");
        boolean isStart = "true".equals(value);
        if (DEBUG) {
            Log.d(TAG, "onReceive: should start = " + isStart);
        }
        if (isStart) {
            VpnServiceHelper.changeVpnRunningStatus(getApplicationContext(), true);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
