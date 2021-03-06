package com.protect.kid.app;

import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.protect.kid.core.util.VpnServiceUtil;
import com.protect.kid.receiver.CheckJobService;
import com.protect.kid.BuildConfig;

import cn.jpush.android.api.JPushInterface;

public class GlobalApplication extends Application {
	public static final String TAG = "GlobalApplication";
	public static final boolean DEBUG = BuildConfig.DEBUG;

	private static GlobalApplication sInstance;

	public static GlobalApplication getInstance() {
		return sInstance;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		initJpush();
		sInstance = this;

		boolean should = VpnServiceUtil.shouldStartVPNService(this);
		VpnServiceUtil.changeVpnRunningStatus(this, should);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
			ComponentName componentName = new ComponentName(this, CheckJobService.class);
			JobInfo.Builder builder = new JobInfo.Builder(9876, componentName)
					.setPeriodic(10 * 1000)
					.setPersisted(true)
					.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
					.setRequiresCharging(false)
					.setRequiresDeviceIdle(false);
			if (jobScheduler != null) {
				jobScheduler.schedule(builder.build());
			} else {
				if (DEBUG) {
					Log.w(TAG, "onCreate: no job scheduler found");
				}
			}
		}
	}

	private void initJpush() {
		// 设置开启日志,发布时请关闭日志
		JPushInterface.setDebugMode(DEBUG);
		// 初始化 JPush
		JPushInterface.init(this);
	}
}
