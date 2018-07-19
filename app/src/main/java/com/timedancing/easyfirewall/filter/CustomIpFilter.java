package com.timedancing.easyfirewall.filter;

import android.content.Context;
import android.util.Log;

import com.timedancing.easyfirewall.BuildConfig;
import com.timedancing.easyfirewall.activity.SettingActivity1;
import com.timedancing.easyfirewall.app.GlobalApplication;
import com.timedancing.easyfirewall.core.blackwhite.BlackIP;
import com.timedancing.easyfirewall.core.blackwhite.WhiteIP;
import com.timedancing.easyfirewall.core.filter.DomainFilter;
import com.timedancing.easyfirewall.db.DAOFactory;
import com.timedancing.easyfirewall.util.GeneralDAO;
import com.timedancing.easyfirewall.util.SharedPrefUtil;

import java.util.ArrayList;
import java.util.List;

public class CustomIpFilter implements DomainFilter {
    private static final String TAG = "CustomIpFilter";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private List<String> mBlackList = new ArrayList<>();
    private List<String> mWhiteList = new ArrayList<>();
    private static boolean isWhite;

    public static void setWhiteEnabled(boolean enabled) {
        isWhite = enabled;
    }

    @Override
    public void prepare() {
        Context context = GlobalApplication.getInstance();

        String str = SharedPrefUtil.getValue(context, SettingActivity1.PREF_NAME, "isWhiteList", "false");
        isWhite = "true".equals(str);

        GeneralDAO<BlackIP> blackIpDAO = DAOFactory.getDAO(context, BlackIP.class);
        List<BlackIP> tempBlackList = blackIpDAO.queryForAll();
        for (BlackIP ip : tempBlackList) {
            mBlackList.add(ip.ip);
        }

        GeneralDAO<WhiteIP> whiteIpDAO = DAOFactory.getDAO(context, WhiteIP.class);
        List<WhiteIP> tempWhiteList = whiteIpDAO.queryForAll();
        for (WhiteIP ip : tempWhiteList) {
            mWhiteList.add(ip.ip);
        }

        if (DEBUG) {
            Log.d(TAG, "prepare: mBlackList = " + mBlackList);
            Log.d(TAG, "prepare: mWhiteList = " + mWhiteList);
        }
    }

    @Override
    public boolean needFilter(String ipAddress, int ip) {
        if (ipAddress == null) {
            return false;
        }
        if (isWhite) {
            for (String white : mWhiteList) {
                if (white.contains(ipAddress)) {
                    return false;
                }
            }
            return true;
        } else {
            for (String black : mBlackList) {
                if (black.contains(ipAddress)) {
                    return true;
                }
            }
            return false;
        }
    }
}
