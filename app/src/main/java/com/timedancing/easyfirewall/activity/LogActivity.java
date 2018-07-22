package com.timedancing.easyfirewall.activity;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.timedancing.easyfirewall.R;
import com.timedancing.easyfirewall.constant.AppGlobal;
import com.timedancing.easyfirewall.core.logger.Logger;
import com.timedancing.easyfirewall.util.SharedPrefUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogActivity extends BaseActivity implements TextWatcher {
	private static final String IS_ASC = "is_asc";

	private Toolbar mToolbar;
	private CheckBox mOrderCb;
	private Cursor mCursor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(getString(R.string.log));
		setContentView(R.layout.activity_log);


		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setUpViews();

		String value = SharedPrefUtil.getValue(this, AppGlobal.GLOBAL_PREF_NAME, IS_ASC, "false");
		boolean isAsc = "true".equals(value);
		initData(!isAsc);
		mOrderCb.setChecked(isAsc);
	}

	@Override
	protected void onDestroy() {
		if (mCursor != null) {
			mCursor.close();
		}
		super.onDestroy();
	}

	private void setUpViews() {
		mToolbar.setNavigationIcon(R.drawable.back);
		mToolbar.setTitle(R.string.log);
		setSupportActionBar(mToolbar);
		mOrderCb = (CheckBox) findViewById(R.id.order);
		mOrderCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SharedPrefUtil.saveValue(buttonView.getContext(), AppGlobal.GLOBAL_PREF_NAME, IS_ASC, isChecked + "");
				initData(!isChecked);
			}
		});
		EditText et = (EditText) findViewById(R.id.search);
		et.addTextChangedListener(this);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	@Override
	public void afterTextChanged(Editable s) {
		if (s.length() == 0) {
			initData(!mOrderCb.isChecked());
			return;
		}
		String text = s.toString();
		if (mCursor != null && !mCursor.isClosed()) {
			mCursor.close();
		}

		mCursor = Logger.getInstance(this).find(text, !mOrderCb.isChecked());
	}

	private void initData(boolean isDesc) {
		if (mCursor != null) {
			mCursor.close();
		}
		ListView lv = (ListView) findViewById(R.id.list_view);
		lv.requestFocus();
		mCursor = Logger.getInstance(this).getAll(isDesc);
		lv.setAdapter(new LogAdapter(this, mCursor));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private static class LogAdapter extends CursorAdapter {
		private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		private int mBackgroundColorRes;

		public LogAdapter(Context context, Cursor c) {
			super(context, c, false);
			mBackgroundColorRes = context.getResources().getColor(R.color.background_color);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
			TextView timeTv = (TextView) view.findViewById(android.R.id.text1);
			timeTv.setTextColor(mBackgroundColorRes);
			TextView logTv = (TextView) view.findViewById(android.R.id.text2);
			logTv.setTextColor(mBackgroundColorRes);
			ViewHolder holder = new ViewHolder();
			holder.timeTv = timeTv;
			holder.logTv = logTv;
			view.setTag(holder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			TextView timeTv = holder.timeTv;
			TextView logTv = holder.logTv;
			timeTv.setText(sdf.format(new Date(cursor.getLong(1))));
			logTv.setText(cursor.getString(2));
		}
	}

	private static class ViewHolder {
		TextView timeTv;
		TextView logTv;
	}
}
