package com.timedancing.easyfirewall.filter;

import android.content.Context;
import android.text.TextUtils;

import com.timedancing.easyfirewall.R;
import com.timedancing.easyfirewall.activity.SettingActivity1;
import com.timedancing.easyfirewall.app.GlobalApplication;
import com.timedancing.easyfirewall.core.blackwhite.BlackContent;
import com.timedancing.easyfirewall.core.blackwhite.WhiteContent;
import com.timedancing.easyfirewall.core.filter.HtmlFilter;
import com.timedancing.easyfirewall.core.logger.Logger;
import com.timedancing.easyfirewall.db.DAOFactory;
import com.timedancing.easyfirewall.util.GeneralDAO;
import com.timedancing.easyfirewall.util.SharedPrefUtil;

import java.util.ArrayList;
import java.util.List;

public class CustomContentFilter implements HtmlFilter {
    private List<String> mBlackContentList = new ArrayList<>();
    private List<String> mWhiteContentList = new ArrayList<>();

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

        GeneralDAO<BlackContent> blackDAO = DAOFactory.getDAO(context, BlackContent.class);
        List<BlackContent>  tempBalckList = blackDAO.queryForAll();
        for (BlackContent content : tempBalckList) {
            mBlackContentList.add(content.content);
        }

        GeneralDAO<WhiteContent> whiteDAO = DAOFactory.getDAO(context, WhiteContent.class);
        List<WhiteContent>  tempWhiteList = whiteDAO.queryForAll();
        for (WhiteContent content : tempWhiteList) {
            mWhiteContentList.add(content.content);
        }
    }

    @Override
    public boolean needFilter(String content) {
        if (isReload) {
            isReload = false;
            prepare();
        }
        if (TextUtils.isEmpty(content)) {
            return false;
        }
        if (isWhite) {
            for (String white : mWhiteContentList) {
                if (content.contains(white)) {
                    return false;
                }
            }
            return true;
        } else {
            for (String black : mBlackContentList) {
                if (content.contains(black)) {
                    Context context = GlobalApplication.getInstance();
                    Logger logger = Logger.getInstance(context);
                    logger.insert(context
                            .getString(R.string.stop_navigate_content_with_x, black, ""));
                    return true;
                }
            }
            return false;
        }
    }
}
