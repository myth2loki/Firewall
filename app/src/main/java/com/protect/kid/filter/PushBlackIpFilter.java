package com.protect.kid.filter;

import android.content.Context;
import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.R;
import com.protect.kid.app.GlobalApplication;
import com.protect.kid.core.blackwhite.BlackIP;
import com.protect.kid.core.filter.DomainFilter;
import com.protect.kid.core.logger.Logger;
import com.protect.kid.db.DAOFactory;
import com.protect.kid.db.GeneralDAO;

import java.util.ArrayList;
import java.util.List;

public class PushBlackIpFilter implements DomainFilter {
    private static final String TAG = "PushIpFilter";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private List<String> mBlackList = new ArrayList<>();
    private static boolean isReload;

    public static void reload() {
        isReload = true;
    }

    @Override
    public void prepare() {
        Context context = GlobalApplication.getInstance();

        GeneralDAO<BlackIP> blackIpDAO = DAOFactory.getPushDAO(context, BlackIP.class);
        List<BlackIP> tempBlackList = blackIpDAO.queryForAll();
        for (BlackIP ip : tempBlackList) {
            mBlackList.add(ip.ip);
        }

        if (DEBUG) {
            Log.d(TAG, "prepare: mBlackList = " + mBlackList);
        }
    }

    @Override
    public boolean needFilter(String ipAddress, int ip, int port) {
        if (isReload) {
            isReload = false;
            mBlackList.clear();
            prepare();
        }

        if (ipAddress == null) {
            return false;
        }
        if (port > -1) {
            ipAddress = ipAddress + ":" + port;
        }
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
