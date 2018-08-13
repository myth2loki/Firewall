package com.protect.kid.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.MenuItem;
import android.widget.TextView;

import com.timedancing.easyfirewall.R;
import com.protect.kid.util.PhoneStateUtil;

public class AboutActivity extends BaseActivity {

	private TextView mTVVersion;
	private Toolbar mToolbar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(getString(R.string.about_page));
		setContentView(R.layout.activity_about);

		mTVVersion = (TextView) findViewById(R.id.tv_version);
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		TextView about = (TextView) findViewById(R.id.tv_about);
		about.setText(Html.fromHtml(getString(R.string.about_extra_info)));
		setUpViews();
	}


	private void setUpViews() {
		mTVVersion.setText(getString(R.string.about_version, PhoneStateUtil.getVersionName(this)));
		mToolbar.setNavigationIcon(R.drawable.back);
		mToolbar.setTitle(R.string.setting_about);
		setSupportActionBar(mToolbar);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
