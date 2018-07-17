package com.timedancing.easyfirewall.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.timedancing.easyfirewall.R;
import com.timedancing.easyfirewall.cache.AppCache;
import com.timedancing.easyfirewall.core.util.VpnServiceHelper;
import com.timedancing.easyfirewall.network.HostHelper;
import com.timedancing.easyfirewall.util.DebugLog;

import de.greenrobot.event.EventBus;

public class MainActivity1 extends BaseActivity implements View.OnClickListener {
	private static final String TAG = "MainActivity";

	private LinearLayout mMainRoot;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(getString(R.string.home_page));
		setContentView(R.layout.activity_main_1);
		mMainRoot = (LinearLayout) findViewById(R.id.main_root);


		AppCache.syncBlockCountWithLeanCloud(this);

		if (!VpnServiceHelper.vpnRunningStatus()) {
			HostHelper.updateHost(this);
		}
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.protect_bar:
				CheckBox cb = (CheckBox) findViewById(R.id.checkbox);
				cb.setChecked(!cb.isChecked());
				break;
			case R.id.log_bar:
				break;
			case R.id.setting_bar:
				openActivity(SettingActivity.class);
				break;
			case R.id.about_bar:
				openActivity(AboutActivity.class);
				break;
		}
	}

	private void openActivity(Class<?> clazz) {
		Intent itt = new Intent(this, clazz);
		startActivity(itt);
	}

	@Override
	protected void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VpnServiceHelper.START_VPN_SERVICE_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				VpnServiceHelper.startVpnService(this);
			} else {
//				changeButtonStatus(false);
				DebugLog.e("canceled");
			}
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStart() {
		super.onStart();
//		EventBus.getDefault().registerSticky(this);
	}
}
