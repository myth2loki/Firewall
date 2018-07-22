package com.timedancing.easyfirewall.filter;

import android.content.Context;
import android.util.Log;

import com.timedancing.easyfirewall.BuildConfig;
import com.timedancing.easyfirewall.app.GlobalApplication;
import com.timedancing.easyfirewall.constant.AppGlobal;
import com.timedancing.easyfirewall.core.filter.DomainFilter;
import com.timedancing.easyfirewall.fragment.TimeSettingFragment;
import com.timedancing.easyfirewall.util.SharedPrefUtil;

import java.util.Date;

import static com.timedancing.easyfirewall.fragment.TimeSettingFragment.END_TIME;
import static com.timedancing.easyfirewall.fragment.TimeSettingFragment.START_TIME;
import static com.timedancing.easyfirewall.fragment.TimeSettingFragment.TIME_RANGE;
import static com.timedancing.easyfirewall.fragment.TimeSettingFragment.TYPE;

public class TimeRangeFilter implements DomainFilter {
    private static final String TAG = "TimeRangeFilter";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private int mType;
    private Date mStartDate;
    private Date mEndDate;

    private static boolean isReload;

    public static void reload() {
        isReload = true;
    }

    @Override
    public void prepare() {
        Context context = GlobalApplication.getInstance();
        mType = SharedPrefUtil.getInt(context, AppGlobal.GLOBAL_PREF_NAME, TYPE, 0);
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
    public boolean needFilter(String domain, int ip) {
        if (isReload) {
            isReload = false;
            prepare();
        }
        if ((mType & TimeSettingFragment.TIME_RANGE) != TIME_RANGE) {
            return false;
        }
        if (mStartDate == null || mEndDate == null) {
            return false;
        }
        long curTime = System.currentTimeMillis();
        boolean result = curTime >= mStartDate.getTime() && curTime <= mEndDate.getTime();
        if (DEBUG) {
            Log.d(TAG, "needFilter: result = " + result);
        }
        return result;
    }
}
