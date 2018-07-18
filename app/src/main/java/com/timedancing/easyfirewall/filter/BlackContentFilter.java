package com.timedancing.easyfirewall.filter;

import android.text.TextUtils;

import com.timedancing.easyfirewall.core.filter.HtmlFilter;

public class BlackContentFilter implements HtmlFilter {

    @Override
    public void prepare() {

    }

    @Override
    public boolean needFilter(String content) {
        if (TextUtils.isEmpty(content)) {
            return false;
        }
        return false;
    }
}
