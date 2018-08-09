package com.timedancing.easyfirewall.filter;

import android.content.Context;
import android.util.Log;

import com.timedancing.easyfirewall.BuildConfig;
import com.timedancing.easyfirewall.R;
import com.timedancing.easyfirewall.activity.SettingActivity1;
import com.timedancing.easyfirewall.app.GlobalApplication;
import com.timedancing.easyfirewall.core.blackwhite.BlackIP;
import com.timedancing.easyfirewall.core.blackwhite.WhiteIP;
import com.timedancing.easyfirewall.core.filter.DomainFilter;
import com.timedancing.easyfirewall.core.logger.Logger;
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
    private static boolean isReload;

    public static void setWhiteEnabled(boolean enabled) {
        isWhite = enabled;
    }

    public static void reload() {
        isReload = true;
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
    public boolean needFilter(String ipAddress, int ip, int port) {
        if (isReload) {
            isReload = false;
            mBlackList.clear();
            mWhiteList.clear();
            prepare();
        }
        if (ipAddress == null) {
            return false;
        }
        if (port > -1) {
            ipAddress = ipAddress + ":" + port;
        }
        if (isWhite) {
            Context context = GlobalApplication.getInstance();
            Logger logger = Logger.getInstance(context);
            for (String white : mWhiteList) {
                if (ipAddress.contains(white)) {
                    logger.insert(context.getString(R.string.allow_to_navigate_x, white));
                    return false;
                }
            }
            logger.insert(context.getString(R.string.stop_navigate_x, ipAddress));
            return true;
        } else {
            for (String black : mBlackList) {
                if (ipAddress.contains(black)) {
                    Context context = GlobalApplication.getInstance();
                    Logger logger = Logger.getInstance(context);
                    logger.insert(context.getString(R.string.stop_navigate_x, black));
                    return true;
                }
            }
            return false;
        }
    }
}
