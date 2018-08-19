package com.protect.kid.filter;

import android.content.Context;
import android.text.TextUtils;

import com.protect.kid.R;
import com.protect.kid.activity.SettingActivity1;
import com.protect.kid.app.GlobalApplication;
import com.protect.kid.core.blackwhite.BlackContent;
import com.protect.kid.core.blackwhite.WhiteContent;
import com.protect.kid.core.filter.HtmlFilter;
import com.protect.kid.core.logger.Logger;
import com.protect.kid.db.DAOFactory;
import com.protect.kid.db.GeneralDAO;
import com.protect.kid.util.SharedPrefUtil;

import java.util.ArrayList;
import java.util.List;

public class PushBlackContentFilter implements HtmlFilter {
    private List<String> mBlackContentList = new ArrayList<>();

    private static boolean isReload;

    public static void reload() {
        isReload = true;
    }

    @Override
    public void prepare() {
        Context context = GlobalApplication.getInstance();

        GeneralDAO<BlackContent> blackDAO = DAOFactory.getPushDAO(context, BlackContent.class);
        List<BlackContent>  tempBalckList = blackDAO.queryForAll();
        for (BlackContent content : tempBalckList) {
            mBlackContentList.add(content.content);
        }
    }

    @Override
    public boolean needFilter(String content) {
        if (isReload) {
            isReload = false;
            mBlackContentList.clear();
            prepare();
        }
        if (TextUtils.isEmpty(content)) {
            return false;
        }
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
