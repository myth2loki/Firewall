package com.protect.kid.core.filter;


public interface HtmlFilter {

    void prepare();

    boolean needFilter(String content);
}
