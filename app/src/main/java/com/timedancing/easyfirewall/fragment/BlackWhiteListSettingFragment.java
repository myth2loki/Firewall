package com.timedancing.easyfirewall.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.timedancing.easyfirewall.R;
import com.timedancing.easyfirewall.activity.SettingActivity1;
import com.timedancing.easyfirewall.util.SharedPrefUtil;

public class BlackWhiteListSettingFragment extends BaseSettingFragment implements View.OnClickListener {
    private CheckBox mBlackWhiteListCb;
    private boolean isWhiteList;

    @Override
    public String getTitle() {
        return "黑白名单";
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_setting_black_white_list, null);
        view.findViewById(R.id.add_ip_domain).setOnClickListener(this);
        view.findViewById(R.id.add_content).setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mBlackWhiteListCb = new CheckBox(getContext());
        String str = SharedPrefUtil.getValue(getContext(), SettingActivity1.PREF_NAME, "isWhiteList", "false");
        isWhiteList = "true".equals(str);
        mBlackWhiteListCb.setChecked(isWhiteList);
        int paddingRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10,
                getContext().getResources().getDisplayMetrics());
        mBlackWhiteListCb.setPadding(0, 0, paddingRight, 0);
        mBlackWhiteListCb.setText(R.string.white_list);
        mBlackWhiteListCb.setTextColor(Color.WHITE);
        mBlackWhiteListCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isWhiteList = isChecked;
                SharedPrefUtil.saveValue(buttonView.getContext(), SettingActivity1.PREF_NAME,
                        "isWhiteList", isChecked + "");
            }
        });
        initData();
    }

    private void initData() {
        if (isWhiteList) {
            //加载白名单数据
        } else {
            //加载黑名单数据
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_ip_domain:
                break;
            case R.id.add_content:
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add("").setActionView(mBlackWhiteListCb).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }
}
