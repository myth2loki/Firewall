package com.timedancing.easyfirewall.filter;

import com.timedancing.easyfirewall.core.filter.HtmlFilter;

/**
 * Created by Administrator on 2018/7/19.
 */

public class BlackHtmlFilter implements HtmlFilter {
    @Override
    public void prepare() {

    }

    @Override
    public boolean needFilter(String content) {
//        return content.contains("传递价值资讯");
        return false;
    }
}
