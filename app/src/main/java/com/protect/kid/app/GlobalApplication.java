package com.protect.kid.app;

import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.avos.avoscloud.AVOSCloud;
import com.timedancing.easyfirewall.BuildConfig;
import com.protect.kid.constant.ApiConstant;
import com.protect.kid.core.ProxyConfig;
import com.protect.kid.core.util.VpnServiceHelper;
import com.protect.kid.receiver.CheckJobService;

import java.util.Properties;

public class GlobalApplication extends Application {
	public static final String TAG = "GlobalApplication";
	public static final boolean DEBUG = BuildConfig.DEBUG;

	private static GlobalApplication sInstance;

	public static GlobalApplication getInstance() {
		return sInstance;
	}

	@Override
	public void onCreate() {
//		if (BuildConfig.DEBUG) {
//			StrictMode.VmPolicy vmPolicy = null;
//			if (Build.VERSION.SDK_INT >= 16) {
//				vmPolicy = new StrictMode.VmPolicy.Builder()
//						.detectActivityLeaks()
//						.detectLeakedSqlLiteObjects()
//						.detectLeakedClosableObjects()
//						.detectLeakedRegistrationObjects()
//						.penaltyLog()
//						.build();
//			} else {
//				vmPolicy = new StrictMode.VmPolicy.Builder()
//						.detectActivityLeaks()
//						.detectLeakedSqlLiteObjects()
//						.detectLeakedClosableObjects()
//						.penaltyLog()
//						.build();
//			}
//			StrictMode.setVmPolicy(vmPolicy);
//		}
		super.onCreate();
//		LeakCanary.install(this);
		sInstance = this;

		AVOSCloud.initialize(this, ApiConstant.LEANCLOUND_APP_ID, ApiConstant.LEANCLOUD_APP_KEY);

		ProxyConfig.Instance.setVpnStatusListener(new StatusListener());

		boolean should = VpnServiceHelper.shouldStartVPNService(this);
		VpnServiceHelper.changeVpnRunningStatus(this, should);

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


	static class StatusListener implements ProxyConfig.VpnStatusListener {

		Properties mProperties = new Properties();

		@Override
		public void onVpnStart(Context context) {
//			StatService.trackCustomBeginKVEvent(context, "VPN_OPEN", mProperties);
		}

		@Override
		public void onVpnEnd(Context context) {
//			StatService.trackCustomEndKVEvent(context, "VPN_OPEN", mProperties);
		}
	}
}
