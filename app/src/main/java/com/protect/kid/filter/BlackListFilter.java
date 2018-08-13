package com.protect.kid.filter;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;

import com.protect.kid.R;
import com.protect.kid.app.GlobalApplication;
import com.protect.kid.constant.AppDebug;
import com.protect.kid.core.ProxyConfig;
import com.protect.kid.core.filter.DomainFilter;
import com.protect.kid.core.logger.Logger;
import com.protect.kid.core.tcpip.CommonMethods;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


public class BlackListFilter implements DomainFilter {
	private static final String TAG = "BlackListFilter";

	private Map<String, Integer> mDomainMap = new HashMap<>();
	/**
	 * 需过滤的ip地址集合
	 */
	private SparseIntArray mIpMask = new SparseIntArray();

	@Override
	public void prepare() {
		if (mDomainMap.size() != 0 || mIpMask.size() != 0) {
			return;
		}
		InputStream in = getHostInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = null;
		try {
			try {
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.startsWith("#")
							|| !TextUtils.isDigitsOnly(String.valueOf(line.charAt(0)))) {
						continue;
					}

					String[] parts = line.split(" ");
					if (parts.length == 2
							&& !"localhost".equalsIgnoreCase(parts[1])) {
						String ipStr = parts[0];
						int ip = CommonMethods.ipStringToInt(ipStr);
						mDomainMap.put(parts[1], ip);
						mIpMask.put(ip, 1);
					}
				}
			} finally {
				reader.close();
				in.close();
			}
		} catch (IOException e) {
			if (AppDebug.IS_DEBUG) {
				Log.e(TAG, "prepare: failed", e);
			}
		}
	}

	@Override
	public boolean needFilter(String ipAddress, int ip, int port) {
		if (ipAddress == null) {
			return false;
		}

		boolean isFiltered = mIpMask.get(ip, -1) == 1;
		ipAddress = ipAddress.trim();
		if (Pattern.matches("\\d+\\.\\d+\\.\\d+\\.\\d+", ipAddress)) { //判断符合ip地址格式
			int newIp = CommonMethods.ipStringToInt(ipAddress);
			isFiltered = isFiltered || (mIpMask.get(newIp, -1) == 1);
		}
		String key = ipAddress;
		if (mDomainMap.containsKey(key)) {
			isFiltered = true;
			int oldIP = mDomainMap.get(key);
			//发现域名下的新ip，保存到集合中
			if (!ProxyConfig.isFakeIP(ip) && ip != oldIP) {
				mDomainMap.put(key, ip);
				mIpMask.put(ip, 1);
			}
		}
		if (isFiltered) {
			Context context = GlobalApplication.getInstance();
			Logger.getInstance(context).insert(context.getString(R.string.stop_navigate_x, ipAddress));
		}
		return isFiltered;
	}

	private InputStream getHostInputStream() {
		InputStream in = null;
		Context context = GlobalApplication.getInstance();
		File file = BlackListHelper.getHostsFile(context);
		if (file.exists()) {
			try {
				in = new FileInputStream(file);
			} catch (IOException ex) {
				if (AppDebug.IS_DEBUG) {
					ex.printStackTrace(System.err);
				}
			}
		}
		if (in == null) {
			in = context.getResources().openRawResource(R.raw.hosts);
		}
		return in;
	}
}
