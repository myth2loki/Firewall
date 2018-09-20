package com.protect.kid.filter;

import android.content.Context;
import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.app.GlobalApplication;
import com.protect.kid.constant.AppGlobal;
import com.protect.kid.core.filter.DomainFilter;
import com.protect.kid.fragment.TimeSettingFragment;
import com.protect.kid.util.SharedPrefUtil;

import java.util.Date;

import static com.protect.kid.fragment.TimeSettingFragment.END_TIME;
import static com.protect.kid.fragment.TimeSettingFragment.START_TIME;
import static com.protect.kid.fragment.TimeSettingFragment.TYPE;

public class TimeRangeFilter implements DomainFilter {
    private static final String TAG = "TimeRangeFilter";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private int mType;
    private Date mStartDate;
    private Date mEndDate;
    private boolean mEnabled;

    private static boolean isReload;

    public static void reload() {
        isReload = true;
    }

    @Override
    public void prepare() {
        Context context = GlobalApplication.getInstance();
        mType = SharedPrefUtil.getInt(context, AppGlobal.GLOBAL_PREF_NAME, TYPE, 0);
        mEnabled = (mType & TimeSettingFragment.TIME_RANGE) != TimeSettingFragment.TIME_RANGE;
        long startTime = SharedPrefUtil.getLong(context, AppGlobal.GLOBAL_PREF_NAME, START_TIME, -1);
        long endTime = SharedPrefUtil.getLong(context, AppGlobal.GLOBAL_PREF_NAME, END_TIME, -1);
        if (startTime > -1) {
            mStartDate = new Date(startTime);
        }
        if (endTime > -1) {
            mEndDate = new Date(endTime);
        }
    }

    @Override
    public int filter(String domain, int ip, int port) {
        if (isReload) {
            isReload = false;
            prepare();
        }
        if (!mEnabled) {
            return NO_FILTER;
        }
        if (mStartDate == null || mEndDate == null) {
            return NO_FILTER;
        }
        long curTime = System.currentTimeMillis();
        int result = NO_FILTER;
        if (curTime < mStartDate.getTime() || curTime > mEndDate.getTime()) {
            result = FILTER_TIME;
        }
        if (DEBUG) {
            Log.d(TAG, "filter: result = " + result + "， domain = " + domain + ", ip = " + ip);
        }
        return result;
    }
}
