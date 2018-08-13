package com.protect.kid.filter;

import android.content.Context;
import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.R;
import com.protect.kid.activity.SettingActivity1;
import com.protect.kid.app.GlobalApplication;
import com.protect.kid.core.blackwhite.BlackIP;
import com.protect.kid.core.blackwhite.WhiteIP;
import com.protect.kid.core.filter.DomainFilter;
import com.protect.kid.core.logger.Logger;
import com.protect.kid.db.DAOFactory;
import com.protect.kid.util.GeneralDAO;
import com.protect.kid.util.SharedPrefUtil;

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
