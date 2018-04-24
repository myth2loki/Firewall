package com.timedancing.easyfirewall.core.nat;

import com.timedancing.easyfirewall.core.tcpip.CommonMethods;

/**
 * 保存回话，重复利用一些资源
 * Created by zengzheying on 15/12/29.
 */
public class NatSession {

	public int remoteIP;
	public short remotePort;
	public String remoteHost;
	public int bytesSent;
	public int packetSent;
	public long lastNanoTime;
	public boolean isHttpsSession;
	public String requestUrl; //HTTP请求的url， HTTPS请求则为空
	public String method; //HTTP请求方法

	@Override
	public String toString() {
		return String.format("%s/%s:%d packet: %d", remoteHost, CommonMethods.ipIntToString(remoteIP),
				remotePort & 0xFFFF, packetSent);
	}
}
