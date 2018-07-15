package com.timedancing.easyfirewall.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.stat.StatConfig;
import com.tencent.stat.StatService;
import com.timedancing.easyfirewall.R;
import com.timedancing.easyfirewall.cache.AppConfig;
import com.timedancing.easyfirewall.constant.AppDebug;
import com.timedancing.easyfirewall.view.NumberKeyboard;

public class UnlockActivity extends AppCompatActivity {
	private static final int MAXIMUM_PASSWORD = 6;
	private NumberKeyboard mNumberKeyboard;
	private EditText mEditText;
	private TextView mTvHint;

	private boolean isNeedSetPassword;
	private String mFirstPassword;
	private TextView[] mPwdArr = new TextView[6];

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.parent_login);
		setContentView(R.layout.activity_lock);

		mEditText = (EditText) findViewById(R.id.et_pwd);
		mTvHint = (TextView) findViewById(R.id.tv_hint);
		mNumberKeyboard = (NumberKeyboard) findViewById(R.id.numberKeyboard);

		setUpMta();
		setUpViews();

		AppConfig.setShouldShowGuidePage(this, false);
	}

	private void setUpViews() {
		LinearLayout pwdCaptionLL = (LinearLayout) findViewById(R.id.pwd_caption_ll);
		int width = 120;
		for (int i = 0; i< 6; i++) {
			TextView tv = new TextView(this);
			mPwdArr[i] = tv;
			tv.setBackgroundResource(R.drawable.rect_round);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, width);
			lp.leftMargin = i > 0 ? 40 : 0;
			tv.setGravity(Gravity.CENTER);
			pwdCaptionLL.addView(tv, lp);
		}

		isNeedSetPassword = TextUtils.isEmpty(AppConfig.getLockPassword(this));
		if (isNeedSetPassword) {
			mTvHint.setText(R.string.set_password);
		} else {
			mTvHint.setText(R.string.input_password);
		}

		mNumberKeyboard.setKeyboardInputListener(new NumberKeyboard.OnKeyboardInputListener() {
			@Override
			public void onKeyboardInput(String number) {
				if ("x".equals(number) && mEditText.length() > 0) {
					mEditText.setText(mEditText.getText().subSequence(0, mEditText.length() - 1));
					return;
				} else if ("▶".equals(number)) {

				}
				mEditText.append(number);
			}
		});

		mEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				for (TextView tv : mPwdArr) {
					tv.setText("");
				}
				for (int i = 0; i < s.length(); i++) {
					mPwdArr[i].setText("*");
				}
				validate(s);
			}
		});

	}

	private void validate(final Editable s) {
		if (s.length() == MAXIMUM_PASSWORD) {
			if (!isNeedSetPassword && s.toString().equals(AppConfig.getLockPassword(this))) {
				navigateToMainActivity();
			} else {
				if (isNeedSetPassword) {
					if (TextUtils.isEmpty(mFirstPassword)) {
						mFirstPassword = s.toString();
						mTvHint.setText(R.string.confirm_password);
					} else {
						if (mFirstPassword.equals(s.toString())){
							AppConfig.setLockPassword(this, mFirstPassword);
							navigateToMainActivity();
							return;
						} else {
							mTvHint.setText(R.string.set_password_again);
							mFirstPassword = null;
						}
					}
				} else {
					mTvHint.setText(R.string.wrong_password);
				}
				mEditText.postDelayed(new Runnable() {
					@Override
					public void run() {
						mEditText.setText("");
					}
				}, 100);
			}
		}
	}

	private void navigateToMainActivity() {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
		finish();
	}

	private void setUpMta() {
		StatConfig.setDebugEnable(AppDebug.IS_DEBUG);

		StatService.registerActivityLifecycleCallbacks(getApplication());
	}
}
