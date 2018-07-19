package com.timedancing.easyfirewall.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.timedancing.easyfirewall.R;
import com.timedancing.easyfirewall.cache.AppCache;
import com.timedancing.easyfirewall.core.util.VpnServiceHelper;
import com.timedancing.easyfirewall.filter.BlackListHelper;

import de.greenrobot.event.EventBus;

public class MainActivity1 extends BaseActivity implements View.OnClickListener {
	private static final String TAG = "MainActivity";

	private CheckBox mProtectCheckbox;
	private TextView mSubTitleProtect;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(getString(R.string.home_page));
		setContentView(R.layout.activity_main_1);
		mSubTitleProtect = (TextView) findViewById(R.id.sub_title_protect);
		mProtectCheckbox = (CheckBox) findViewById(R.id.checkbox);
		mProtectCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mSubTitleProtect.setText(isChecked ? R.string.protect_feature_sub_title_started
						: R.string.protect_feature_sub_title_stopped);
				VpnServiceHelper.changeVpnRunningStatus(buttonView.getContext(), isChecked);
			}
		});
		initData();
	}

	private void initData() {
		AppCache.syncBlockCountWithLeanCloud(this);

		boolean isRunning = VpnServiceHelper.vpnRunningStatus();
		mProtectCheckbox.setChecked(isRunning);
		mSubTitleProtect.setText(isRunning ? R.string.protect_feature_sub_title_started
				: R.string.protect_feature_sub_title_stopped);
		if (!isRunning) {
			BlackListHelper.update(this);
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
				openActivity(SettingActivity1.class);
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
				mProtectCheckbox.setChecked(true);
			} else {
				mProtectCheckbox.setChecked(false);
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
