package com.timedancing.easyfirewall.filter;

import android.content.Context;
import android.util.Log;

import com.timedancing.easyfirewall.BuildConfig;
import com.timedancing.easyfirewall.app.GlobalApplication;
import com.timedancing.easyfirewall.constant.AppGlobal;
import com.timedancing.easyfirewall.core.filter.DomainFilter;
import com.timedancing.easyfirewall.fragment.TimeSettingFragment;
import com.timedancing.easyfirewall.util.SharedPrefUtil;

import static com.timedancing.easyfirewall.fragment.TimeSettingFragment.DURATION;
import static com.timedancing.easyfirewall.fragment.TimeSettingFragment.TYPE;

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
    public boolean needFilter(String domain, int ip) {
        if (mStartTime < 0) {
            return false;
        }
        if (isReload) {
            isReload = false;
            prepare();
        }
        long duration = System.currentTimeMillis() - mStartTime;
        if (mEnabled) {
            boolean result = duration <= mDuration;
            if (DEBUG) {
                Log.d(TAG, "needFilter: result = " + result);
            }
            return result;
        }
        return false;
    }
}
