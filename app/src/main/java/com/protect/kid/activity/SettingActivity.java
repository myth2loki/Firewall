package com.protect.kid.activity;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.protect.kid.BuildConfig;
import com.protect.kid.R;
import com.protect.kid.fragment.BaseSettingFragment;
import com.protect.kid.fragment.BlackWhiteListSettingFragment;
import com.protect.kid.fragment.TimeSettingFragment;
import com.protect.kid.fragment.UnlockFragment;
import com.protect.kid.receiver.NoUninstallReceiver;

import java.util.ArrayList;
import java.util.List;

public class SettingActivity extends BaseActivity {
	private static final String TAG = "SettingActivity";
	private static final boolean DEBUG = BuildConfig.DEBUG;

	public static final String PREF_NAME = "settings.dat";

	private static final int DEVICE_MANAGER_REQUEST_CODE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(getString(R.string.settings_page));
		setContentView(R.layout.activity_setting);

		initViews();
	}

	private void initViews() {
		Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
		toolbar.setNavigationIcon(R.drawable.back);
		toolbar.setTitle(R.string.setting);
		setSupportActionBar(toolbar);

		TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
		ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);

		FragmentStatePagerAdapter adapter = new SettingFragmentAdapter(getSupportFragmentManager());
		viewPager.setAdapter(adapter);
		tabLayout.setupWithViewPager(viewPager);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		ComponentName componentName = new ComponentName(this, NoUninstallReceiver.class);
		DevicePolicyManager manager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		if (manager != null) {
			menu.add(0, 1, 999, R.string.prevent_uninstall)
					.setCheckable(true).setChecked(manager.isAdminActive(componentName));
		}
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		} else if (item.getItemId() == 1) {
			item.setChecked(!item.isChecked());
			requestNoInstall();
			Toast.makeText(this, "admin： " + item.isChecked(), Toast.LENGTH_SHORT).show();
		}
		return super.onOptionsItemSelected(item);
	}

	private void requestNoInstall() {
		ComponentName componentName = new ComponentName(this, NoUninstallReceiver.class);
		DevicePolicyManager manager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		if (manager == null) {
			Log.d(TAG, "requestNoInstall: DevicePolicyManager is null.");
			return;
		}
		if (!manager.isAdminActive(componentName)) {
			Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this,
					NoUninstallReceiver.class));
			intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string
					.admin_request_description));

			startActivityForResult(intent, DEVICE_MANAGER_REQUEST_CODE);
		} else {
			manager.removeActiveAdmin(componentName);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == DEVICE_MANAGER_REQUEST_CODE) {
			Toast.makeText(this, "防止卸载：" + (resultCode == RESULT_OK), Toast.LENGTH_SHORT).show();
		}
	}

	private class SettingFragmentAdapter extends FragmentStatePagerAdapter {
		private List<BaseSettingFragment> mFragList = new ArrayList<>();

		public SettingFragmentAdapter(FragmentManager fm) {
			super(fm);
			mFragList.add(new BlackWhiteListSettingFragment());
			mFragList.add(new TimeSettingFragment());
			mFragList.add(new UnlockFragment(true));
		}

		@Override
		public Fragment getItem(int position) {
			return mFragList.get(position);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return mFragList.get(position).getTitle();
		}

		@Override
		public int getCount() {
			return mFragList.size();
		}
	}
}
