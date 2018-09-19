package com.protect.kid.core.nat;

import com.protect.kid.core.tcpip.CommonMethods;

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
