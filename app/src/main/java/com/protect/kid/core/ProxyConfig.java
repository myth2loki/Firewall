package com.protect.kid.core;


import android.content.Context;
import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.core.builder.BlockingInfoBuilder;
import com.protect.kid.core.builder.DefaultBlockingInfoBuilder;
import com.protect.kid.core.filter.DomainFilter;
import com.protect.kid.core.filter.Filter;
import com.protect.kid.core.filter.HtmlFilter;
import com.protect.kid.core.tcpip.CommonMethods;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ProxyConfig {
	private static final String TAG = "ProxyConfig";
	private static final boolean DEBUG = BuildConfig.DEBUG;

	public static final ProxyConfig Instance = new ProxyConfig();

	public final static int FAKE_NETWORK_MASK = CommonMethods.ipStringToInt("255.255.0.0");
	public final static int FAKE_NETWORK_IP = CommonMethods.ipStringToInt("10.231.0.0");

	private ArrayList<IPAddress> mIpList;
	private ArrayList<IPAddress> mDnsList;
	private ArrayList<IPAddress> mRouteList;
	private int mDnsTtl;

	private String mSessionName;
	private int mMtu;
	private List<DomainFilter> mDomainFilterList = new ArrayList<>();
	private List<HtmlFilter> mHtmlFilterList = new ArrayList<>();
	private List<BlockingInfoBuilder> mBlockingInfoBuilderList = new ArrayList<>();
	private VpnStatusListener mVpnStatusListener;


	public ProxyConfig() {
		mIpList = new ArrayList<>();
		mDnsList = new ArrayList<>();
		mRouteList = new ArrayList<>();
	}

	public static boolean isFakeIP(int ip) {
		return (ip & ProxyConfig.FAKE_NETWORK_MASK) == ProxyConfig.FAKE_NETWORK_IP;
	}

	public void addDomainFilter(DomainFilter filter) {
		mDomainFilterList.add(filter);
	}

	public void addHtmlFilter(HtmlFilter filter) {
		mHtmlFilterList.add(filter);
	}

	public void prepare() throws IllegalStateException {
		for (DomainFilter filter : mDomainFilterList) {
			filter.prepare();
		}

		for (HtmlFilter filter : mHtmlFilterList) {
			filter.prepare();
		}
	}

	public IPAddress getDefaultLocalIP() {
		if (mIpList.size() > 0) {
			return mIpList.get(0);
		} else {
			return new IPAddress("10.8.0.2", 32);
		}
	}

	public ArrayList<IPAddress> getDnsList() {
		return mDnsList;
	}

	public ArrayList<IPAddress> getRouteList() {
		return mRouteList;
	}

	public int getDnsTTL() {
		if (mDnsTtl < 30) {
			mDnsTtl = 30;
		}
		return mDnsTtl;
	}

	public String getSessionName() {
		if (mSessionName == null) {
			mSessionName = "Kid Protector";
		}
		return mSessionName;
	}

	public int getMTU() {
		if (mMtu > 1400 && mMtu <= 20000) {
			return mMtu;
		} else {
			return 20000;
		}
	}

	/**
	 * 过滤地址
	 * @param host 主机地址
	 * @param ip ip地址
	 * @param port 端口号
	 * @return true代表被过滤，否则false
	 */
	public int filter(String host, int ip, int port) {
		int result = Filter.NO_FILTER;
		for (DomainFilter filter : mDomainFilterList) {
			result = filter.filter(host, ip, port);
			if (result != Filter.NO_FILTER) {
				break;
			}
		}
		if (isFakeIP(ip)) {
			result = Filter.FILTER_LIST;
		}
		if (DEBUG) {
			Log.d(TAG, String.format("filter: host %s content %s %s",
					host, CommonMethods.ipIntToString(ip), result));
		}
		return result;
	}

	public int filterContent(String content) {
		int result = Filter.NO_FILTER;
		for (HtmlFilter filter : mHtmlFilterList) {
			result = filter.filter(content);
			if (result != Filter.NO_FILTER) {
				break;
			}
		}
		if (DEBUG) {
			Log.d(TAG, String.format("filterContent: content %s %s", content, result));
		}
		return result;
	}

	public void addBlockingInfoBuilder(BlockingInfoBuilder blockingInfoBuilder) {
		mBlockingInfoBuilderList.add(blockingInfoBuilder);
	}

	public ByteBuffer getBlockingInfo(int result) {
		for (BlockingInfoBuilder builder : mBlockingInfoBuilderList) {
			if (DEBUG) {
				Log.d(TAG, "getBlockingInfo: match " + builder.match(result) + " with " + builder);
			}
			if (builder.match(result)) {
				return builder.getBlockingInformation();
			}
		}
		return DefaultBlockingInfoBuilder.get().getBlockingInformation();
	}

	public void setVpnStatusListener(VpnStatusListener vpnStatusListener) {
		mVpnStatusListener = vpnStatusListener;
	}

	public void onVpnStart(Context context) {
		if (mVpnStatusListener != null) {
			mVpnStatusListener.onVpnStart(context);
		}
	}


	public void onVpnEnd(Context context) {
		if (mVpnStatusListener != null) {
			mVpnStatusListener.onVpnEnd(context);
		}
	}

	public interface VpnStatusListener {
		void onVpnStart(Context context);

		void onVpnEnd(Context context);
	}

	public static class IPAddress {
		public final String Address;
		public final int PrefixLength;

		public IPAddress(String address, int prefixLength) {
			Address = address;
			PrefixLength = prefixLength;
		}

		public IPAddress(String ipAddressString) {
			String[] arrStrings = ipAddressString.split("/");
			String address = arrStrings[0];
			int prefixLength = 32;
			if (arrStrings.length > 1) {
				prefixLength = Integer.parseInt(arrStrings[1]);
			}

			this.Address = address;
			this.PrefixLength = prefixLength;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof IPAddress)) {
				return false;
			} else {
				return this.toString().equals(o.toString());
			}
		}

		@Override
		public String toString() {
			return String.format("%s/%d", Address, PrefixLength);
		}
	}
}
