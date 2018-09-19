package com.protect.kid.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.protect.kid.R;
import com.protect.kid.activity.MainActivity;
import com.protect.kid.core.logger.Logger;
import com.protect.kid.db.AppConfig;
import com.protect.kid.view.NumberKeyboard;

public class UnlockFragment extends BaseSettingFragment {
    private static final int MAXIMUM_PASSWORD = 6;
    private NumberKeyboard mNumberKeyboard;
    private EditText mEditText;
    private TextView mTvHint;

    private boolean isForceSetPassword;
    private boolean isNeedSetPassword;
    private String mFirstPassword;
    private TextView[] mPwdArr = new TextView[6];

    public UnlockFragment() {
    }

    public UnlockFragment(boolean forceSetPassword) {
        isForceSetPassword = forceSetPassword;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lock, container, false);

        mEditText = (EditText) view.findViewById(R.id.et_pwd);
        mTvHint = (TextView) view.findViewById(R.id.tv_hint);
        mNumberKeyboard = (NumberKeyboard) view.findViewById(R.id.numberKeyboard);

        setUpViews(view);

        AppConfig.setShouldShowGuidePage(getContext(), false);
        return view;
    }

    private void setUpViews(View view) {
        LinearLayout pwdCaptionLL = (LinearLayout) view.findViewById(R.id.pwd_caption_ll);
        int width = 120;
        for (int i = 0; i< 6; i++) {
            TextView tv = new TextView(getContext());
            mPwdArr[i] = tv;
            tv.setBackgroundResource(R.drawable.rect_round_white);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, width);
            lp.leftMargin = i > 0 ? 40 : 0;
            tv.setGravity(Gravity.CENTER);
            pwdCaptionLL.addView(tv, lp);
        }

        isNeedSetPassword = TextUtils.isEmpty(AppConfig.getLockPassword(getContext()));
        if (isForceSetPassword) {
            isNeedSetPassword = true;
        }
//        if (isNeedSetPassword) {
//            mTvHint.setText(R.string.set_password);
//        } else {
            mTvHint.setText(R.string.input_password);
//        }

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
            if (!isNeedSetPassword && s.toString().equals(AppConfig.getLockPassword(getContext()))) {
                navigateToMainActivity();
            } else {
                if (isNeedSetPassword) {
                    if (TextUtils.isEmpty(mFirstPassword)) {
                        mFirstPassword = s.toString();
                        mTvHint.setText(R.string.confirm_password);
                    } else {
                        if (mFirstPassword.equals(s.toString())){
                            AppConfig.setLockPassword(getContext(), mFirstPassword);
                            if (isForceSetPassword) {
                                for (int i = 0; i < s.length(); i++) {
                                    mPwdArr[i].setText("");
                                }
                                Toast.makeText(getContext(), R.string.succeeded_to_set_parent_password, Toast.LENGTH_SHORT).show();
                            } else {
                                navigateToMainActivity();
                            }
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
        Logger.getInstance(getContext()).insert(getString(R.string.parent_password_login));
        Intent intent = new Intent(getContext(), MainActivity.class);
        startActivity(intent);
        getActivity().finish();
    }


    @Override
    public String getTitle() {
        return "家长密码";
    }
}
