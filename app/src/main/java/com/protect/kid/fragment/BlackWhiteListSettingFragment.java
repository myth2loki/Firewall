package com.protect.kid.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import com.timedancing.easyfirewall.BuildConfig;
import com.timedancing.easyfirewall.R;
import com.protect.kid.activity.SettingActivity1;
import com.protect.kid.component.InputDialog;
import com.protect.kid.core.blackwhite.BlackContent;
import com.protect.kid.core.blackwhite.BlackIP;
import com.protect.kid.core.blackwhite.StringItem;
import com.protect.kid.core.blackwhite.WhiteContent;
import com.protect.kid.core.blackwhite.WhiteIP;
import com.protect.kid.core.logger.Logger;
import com.protect.kid.core.util.VpnServiceHelper;
import com.protect.kid.db.DAOFactory;
import com.protect.kid.filter.CustomContentFilter;
import com.protect.kid.filter.CustomIpFilter;
import com.protect.kid.util.GeneralDAO;
import com.protect.kid.util.SharedPrefUtil;

import java.util.List;

public class BlackWhiteListSettingFragment extends BaseSettingFragment implements View.OnClickListener,
        InputDialog.OnOKClickListener, AdapterView.OnItemClickListener {
    private static final String TAG = "bwListFrag";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private CheckBox mBlackWhiteListCb;
    private boolean isWhiteList, isIP;
    private InputDialog mInputDialog;
    private GeneralDAO<BlackIP> mBlackIPDAO;
    private GeneralDAO<BlackContent> mBlackContentDAO;
    private GeneralDAO<WhiteIP> mWhiteIPDAO;
    private GeneralDAO<WhiteContent> mWhiteContentDAO;
    private ListView mIpDomainListView;
    private ListView mContentListView;

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
        View view = inflater.inflate(R.layout.fragment_setting_black_white_list, null);
        view.findViewById(R.id.add_ip_domain).setOnClickListener(this);
        view.findViewById(R.id.add_content).setOnClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        mBlackIPDAO = DAOFactory.getDAO(getContext(), BlackIP.class);
        mBlackContentDAO = DAOFactory.getDAO(getContext(), BlackContent.class);
        mWhiteIPDAO = DAOFactory.getDAO(getContext(), WhiteIP.class);
        mWhiteContentDAO = DAOFactory.getDAO(getContext(), WhiteContent.class);

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
            public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
                buttonView.setEnabled(false);
                isWhiteList = isChecked;
                CustomIpFilter.setWhiteEnabled(isWhiteList);
                initData();
                CustomContentFilter.setWhiteEnabled(isWhiteList);
                CustomIpFilter.setWhiteEnabled(isWhiteList);
                SharedPrefUtil.saveValue(buttonView.getContext(), SettingActivity1.PREF_NAME,
                        "isWhiteList", isChecked + "");
                Logger.getInstance(buttonView.getContext())
                        .insert(isWhiteList ? getString(R.string.change_to_white_list) : getString(R.string.change_to_black_list));
                VpnServiceHelper.restartVpnService(buttonView.getContext(), new Runnable() {
                    @Override
                    public void run() {
                        buttonView.setEnabled(true);
                    }
                });
            }
        });
        initData();
    }

    private void initData() {
        if (getView() == null) {
            if (DEBUG) {
                Log.w(TAG, "initData: getView == null");
            }
            return;
        }
        mIpDomainListView = (ListView) getView().findViewById(R.id.ip_domain_list);
        mIpDomainListView.setOnItemClickListener(this);
        mContentListView = (ListView) getView().findViewById(R.id.content_list);
        mContentListView.setOnItemClickListener(this);
        if (!isWhiteList) {
            //加载白名单数据
            List<BlackIP> biList = mBlackIPDAO.queryForAll();
            BlackWhiteAdapter adapter = new BlackWhiteAdapter(biList);
            mIpDomainListView.setAdapter(adapter);

            List<BlackContent> bcList = mBlackContentDAO.queryForAll();
            adapter = new BlackWhiteAdapter(bcList);
            mContentListView.setAdapter(adapter);
        } else {
            //加载黑名单数据
            List<WhiteIP> wiList = mWhiteIPDAO.queryForAll();
            BlackWhiteAdapter adapter = new BlackWhiteAdapter(wiList);
            mIpDomainListView.setAdapter(adapter);

            List<WhiteContent> wcList = mWhiteContentDAO.queryForAll();
            adapter = new BlackWhiteAdapter(wcList);
            mContentListView.setAdapter(adapter);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        showAlertDialog(parent, position);
    }

    private void showAlertDialog(final AdapterView<?> parent, int position) {
        final StringItem item = (StringItem) parent.getItemAtPosition(position);
        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.AlertDialog)
            .setTitle("提示")
            .setMessage("确认删除吗？")
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (parent == mIpDomainListView) {
                        if (isWhiteList) {
                            mWhiteIPDAO.removeById((WhiteIP) item, item.getId());
                        } else {
                            mBlackIPDAO.removeById((BlackIP) item, item.getId());
                        }
                    } else if (parent == mContentListView) {
                        if (isWhiteList) {
                            mWhiteContentDAO.removeById((WhiteContent) item, item.getId());
                        } else {
                            mBlackContentDAO.removeById((BlackContent) item, item.getId());
                        }
                    }
                    BlackWhiteAdapter adapter = (BlackWhiteAdapter) parent.getAdapter();
                    adapter.remove(item);
                    CustomIpFilter.reload();
                    CustomContentFilter.reload();
                    adapter.notifyDataSetChanged();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_ip_domain:
                isIP = true;
                showInputDialog(R.string.IP_domian_list);
                break;
            case R.id.add_content:
                isIP = false;
                showInputDialog(R.string.content_list);
                break;
        }
    }

    @Override
    public void onClick(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        if (isIP) {
            if (isWhiteList) {
                WhiteIP ip = new WhiteIP();
                ip.ip = text;
                mWhiteIPDAO.create(ip);
                Logger.getInstance(getContext()).insert(getString(R.string.insert_x_to_white_list, ip.ip));
            } else {
                BlackIP ip = new BlackIP();
                ip.ip = text;
                mBlackIPDAO.create(ip);
                Logger.getInstance(getContext()).insert(getString(R.string.insert_x_to_black_list, ip.ip));
            }
        } else {
            if (isWhiteList) {
                WhiteContent content = new WhiteContent();
                content.content = text;
                mWhiteContentDAO.create(content);
                Logger.getInstance(getContext()).insert(getString(R.string.insert_x_to_white_list, content.content));
            } else {
                BlackContent content = new BlackContent();
                content.content = text;
                mBlackContentDAO.create(content);
                Logger.getInstance(getContext()).insert(getString(R.string.insert_x_to_black_list, content.content));
            }
        }
        initData();
        CustomIpFilter.reload();
        CustomContentFilter.reload();
    }

    private void showInputDialog(int titleRes) {
        String title = getString(titleRes);
        if (mInputDialog != null) {
            mInputDialog.dismiss();
        }
        mInputDialog = new InputDialog(getActivity(), title);
        mInputDialog.setListener(this);
        mInputDialog.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add("").setActionView(mBlackWhiteListCb).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    static class BlackWhiteAdapter extends BaseAdapter {
        private List<? extends StringItem> mItems;

        BlackWhiteAdapter(List<? extends StringItem> items) {
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mItems.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            TextView tv1 = (TextView) convertView.findViewById(android.R.id.text1);
            tv1.setTextColor(convertView.getResources().getColor(R.color.background_color));
            tv1.setText(mItems.get(position).getText());
            return convertView;
        }

        public void remove(StringItem item) {
            if (item == null) {
                return;
            }
            mItems.remove(item);
        }
    }
}