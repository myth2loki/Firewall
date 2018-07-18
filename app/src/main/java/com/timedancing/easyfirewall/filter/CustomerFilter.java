package com.timedancing.easyfirewall.filter;

import com.timedancing.easyfirewall.core.filter.DomainFilter;

import java.util.ArrayList;
import java.util.List;

public class CustomerFilter implements DomainFilter {
    private List<String> mBlackList = new ArrayList<>();
    private List<String> mWhiteList = new ArrayList<>();
    private static boolean isWhite;

    public static void setWhiteEnabled(boolean enabled) {
        isWhite = enabled;
    }

    @Override
    public void prepare() {
        mBlackList.add("baidu.com");
        mWhiteList.add("baidu.com");
    }

    @Override
    public boolean needFilter(String ipAddress, int ip) {
        if (ipAddress == null) {
            return false;
        }
        if (isWhite) {
            boolean ret = mWhiteList.contains(ipAddress);
            if (!ret) {
                for (String white : mWhiteList) {
                    if (white.contains(ipAddress) || ipAddress.contains(white)) {
                        return true;
                    }
                }
            }
            return ret;
        } else {
            boolean ret = mBlackList.contains(ipAddress);
            if (!ret) {
                for (String black : mBlackList) {
                    if (black.contains(ipAddress) || ipAddress.contains(black)) {
                        return true;
                    }
                }
            }
            return ret;
        }
    }
}
