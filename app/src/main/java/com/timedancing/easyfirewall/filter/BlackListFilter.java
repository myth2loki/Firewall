package com.timedancing.easyfirewall.filter;

import android.content.Context;
import android.text.TextUtils;
import android.util.SparseIntArray;

import com.timedancing.easyfirewall.R;
import com.timedancing.easyfirewall.app.GlobalApplication;
import com.timedancing.easyfirewall.constant.AppDebug;
import com.timedancing.easyfirewall.core.ProxyConfig;
import com.timedancing.easyfirewall.core.filter.DomainFilter;
import com.timedancing.easyfirewall.core.tcpip.CommonMethods;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by zengzheying on 16/1/6.
 */
public class BlackListFilter implements DomainFilter {

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
		} catch (IOException ex) {
			if (AppDebug.IS_DEBUG) {
				ex.printStackTrace(System.err);
			}
		} finally {
			try {
				reader.close();
				in.close();
			} catch (IOException ex) {
				if (AppDebug.IS_DEBUG) {
					ex.printStackTrace(System.err);
				}
			}
		}
	}

	@Override
	public boolean needFilter(String ipAddress, int ip) {
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

		return isFiltered;
	}

	private InputStream getHostInputStream() {
		InputStream in = null;
		Context context = GlobalApplication.getInstance();
		File file = new File(context.getExternalCacheDir(), "host.txt");
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
