package com.timedancing.easyfirewall.core.filter;


public interface HtmlFilter {

    void prepare();

    boolean needFilter(String content);
}
