package com.protect.kid.filter;

import android.content.Context;
import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.app.GlobalApplication;
import com.protect.kid.constant.AppGlobal;
import com.protect.kid.core.filter.DomainFilter;
import com.protect.kid.fragment.TimeSettingFragment;
import com.protect.kid.util.SharedPrefUtil;

import static com.protect.kid.fragment.TimeSettingFragment.DURATION;
import static com.protect.kid.fragment.TimeSettingFragment.TYPE;

public class TimeDurationFilter implements DomainFilter {
    private static final String TAG = "TimeDurationFilter";
    private static final boolean DEBUG = BuildConfig.DEBUG;
    public static final String PROTECT_START_TIME = "protect_start_time";

    private int mType;
    private long mStartTime;
    private int mDuration;
    private boolean mEnabled;

    private static boolean isReload;

    public static void reload() {
        isReload = true;
    }

    @Override
    public void prepare() {
        Context context = GlobalApplication.getInstance();
        mType = SharedPrefUtil.getInt(context, AppGlobal.GLOBAL_PREF_NAME, TYPE, 0);
        mEnabled = (mType & TimeSettingFragment.TIME_DURATION) != TimeSettingFragment.TIME_DURATION;
        mDuration = SharedPrefUtil.getInt(context, AppGlobal.GLOBAL_PREF_NAME, DURATION, -1);
        mStartTime = SharedPrefUtil.getLong(context, AppGlobal.GLOBAL_PREF_NAME, PROTECT_START_TIME, -1);
    }

    @Override
    public int filter(String domain, int ip, int port) {
        if (mStartTime < 0) {
            return NO_FILTER;
        }
        if (isReload) {
            isReload = false;
            prepare();
        }
        int result = NO_FILTER;
        long duration = System.currentTimeMillis() - mStartTime;
        if (mEnabled) {
            if (duration > mDuration) {
                result = FILTER_TIME;
            }
            if (DEBUG) {
                Log.d(TAG, "filter: result = " + result);
            }
        }
        return result;
    }
}
