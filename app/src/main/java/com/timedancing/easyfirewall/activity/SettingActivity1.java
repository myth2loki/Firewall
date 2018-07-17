package com.timedancing.easyfirewall.activity;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.timedancing.easyfirewall.R;
import com.timedancing.easyfirewall.fragment.BaseSettingFragment;
import com.timedancing.easyfirewall.fragment.BlackWhiteListSettingFragment;
import com.timedancing.easyfirewall.fragment.PasswordSettingFragment;
import com.timedancing.easyfirewall.fragment.TimeSettingFragment;

import java.util.ArrayList;
import java.util.List;

public class SettingActivity1 extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(getString(R.string.settings_page));
		setContentView(R.layout.activity_setting1);

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
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private class SettingFragmentAdapter extends FragmentStatePagerAdapter {
		private List<BaseSettingFragment> mFragList = new ArrayList<>();

		public SettingFragmentAdapter(FragmentManager fm) {
			super(fm);
			mFragList.add(new BlackWhiteListSettingFragment());
			mFragList.add(new TimeSettingFragment());
			mFragList.add(new PasswordSettingFragment());
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
