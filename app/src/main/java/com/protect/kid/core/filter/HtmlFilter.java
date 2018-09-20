package com.protect.kid.core.filter;


public interface HtmlFilter extends Filter {
    void prepare();
    int filter(String content);
}
