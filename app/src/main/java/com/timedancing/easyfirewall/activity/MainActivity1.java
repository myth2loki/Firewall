package com.timedancing.easyfirewall.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
		createView();
		createView();
		createView();

		AppCache.syncBlockCountWithLeanCloud(this);

		if (!VpnServiceHelper.vpnRunningStatus()) {
			HostHelper.updateHost(this);
		}
	}

	private void createView() {
		RelativeLayout rl = (RelativeLayout) getLayoutInflater().inflate(R.layout.activity_main_item, mMainRoot, false);
		TextView title = (TextView) rl.findViewById(R.id.title);
		title.setText(R.string.protect_feature_title);
		TextView subTitle = (TextView) rl.findViewById(R.id.sub_title);
		subTitle.setText(R.string.protect_feature_sub_title);
		mMainRoot.addView(rl);
	}

	private View genItem(int titleRes, int subTitleRes, int iconRes) {
		RelativeLayout rl = (RelativeLayout) getLayoutInflater().inflate(R.layout.activity_main_item, null);
		TextView title1 = (TextView) rl.findViewById(R.id.title);
		title1.setText(titleRes);
		TextView title2 = (TextView) rl.findViewById(R.id.sub_title);
		title2.setText(subTitleRes);
		return rl;
	}

	@Override
	public void onClick(View v) {

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
