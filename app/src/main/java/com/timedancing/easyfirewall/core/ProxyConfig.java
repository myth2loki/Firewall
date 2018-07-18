package com.timedancing.easyfirewall.core;


import android.content.Context;

import com.timedancing.easyfirewall.core.builder.BlockingInfoBuilder;
import com.timedancing.easyfirewall.core.builder.DefaultBlockingInfoBuilder;
import com.timedancing.easyfirewall.core.filter.DomainFilter;
import com.timedancing.easyfirewall.core.filter.HtmlFilter;
import com.timedancing.easyfirewall.core.tcpip.CommonMethods;
import com.timedancing.easyfirewall.util.DebugLog;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ProxyConfig {

	public static final ProxyConfig Instance = new ProxyConfig();

	public final static int FAKE_NETWORK_MASK = CommonMethods.ipStringToInt("255.255.0.0");
	public final static int FAKE_NETWORK_IP = CommonMethods.ipStringToInt("10.231.0.0");

	private ArrayList<IPAddress> mIpList;
	private ArrayList<IPAddress> mDnsList;
	private ArrayList<IPAddress> mRouteList;
	private int mDnsTtl;

//	HashMap<String, Boolean> mDomainMap;
	private String mSessionName;
	private int mMtu;
	private List<DomainFilter> mDomainFilterList = new ArrayList<>();
	private List<HtmlFilter> mHtmlFilterList = new ArrayList<>();
	private BlockingInfoBuilder mBlockingInfoBuilder;
	private VpnStatusListener mVpnStatusListener;


	public ProxyConfig() {
		mIpList = new ArrayList<>();
		mDnsList = new ArrayList<>();
		mRouteList = new ArrayList<>();

//		mDomainMap = new HashMap<>();
//		mDomainFilter = new BlackListFilter();
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
			mSessionName = "Easy Firewall";
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
	 * @return true代表被过滤，否则false
	 */
	public boolean filter(String host, int ip) {
		boolean isFiltered = false;
		for (DomainFilter filter : mDomainFilterList) {
			isFiltered = isFiltered || filter.needFilter(host, ip);
			if (isFiltered) {
				break;
			}
		}
		DebugLog.iWithTag("Debug",
				String.format("host %s content %s %s", host, CommonMethods.ipIntToString(ip), isFiltered));
		return isFiltered || isFakeIP(ip);
	}

	public boolean filterContent(String content) {
		boolean isFiltered = false;
		for (HtmlFilter filter : mHtmlFilterList) {
			isFiltered = isFiltered || filter.needFilter(content);
			if (isFiltered) {
				break;
			}
		}
		return isFiltered;
	}

	public void setBlockingInfoBuilder(BlockingInfoBuilder blockingInfoBuilder) {
		mBlockingInfoBuilder = blockingInfoBuilder;
	}

	public ByteBuffer getBlockingInfo() {
		if (mBlockingInfoBuilder != null) {
			return mBlockingInfoBuilder.getBlockingInformation();
		} else {
			return DefaultBlockingInfoBuilder.get().getBlockingInformation();
		}
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
