package com.protect.kid.core.nat;

import android.util.SparseArray;

import com.protect.kid.core.ProxyConfig;
import com.protect.kid.core.proxy.DnsProxy;
import com.protect.kid.core.tcpip.CommonMethods;

public class NatSessionManager {

	private static final int MAX_SESSION_COUNT = 64; //会话保存的最大个数
	private static final long SESSION_TIME_OUT_NS = 60 * 1000 * 1000 * 1000L; //会话保存时间
	private static final SparseArray<NatSession> sSessions = new SparseArray<>();

	/**
	 * 通过本地端口获取会话信息
	 *
	 * @param portKey 本地端口
	 * @return 会话信息
	 */
	public static NatSession getSession(int portKey) {
		return sSessions.get(portKey);
	}

	/**
	 * 获取会话个数
	 *
	 * @return 会话个数
	 */
	public static int getSessionCount() {
		return sSessions.size();
	}

	/**
	 * 清除过期的会话
	 */
	private static void clearExpiredSessions() {
		long now = System.nanoTime();
		for (int i = sSessions.size() - 1; i >= 0; i--) {
			NatSession session = sSessions.valueAt(i);
			if (now - session.lastNanoTime > SESSION_TIME_OUT_NS) {
				sSessions.removeAt(i);
			}
		}
	}

	public static void clearAllSessions() {
		sSessions.clear();
	}

	/**
	 * 创建会话
	 *
	 * @param portKey    源端口
	 * @param remoteIP   远程ip
	 * @param remotePort 远程端口
	 * @return NatSession对象
	 */
	public static NatSession createSession(int portKey, int remoteIP, short remotePort) {
		if (sSessions.size() > MAX_SESSION_COUNT) {
			clearExpiredSessions(); //清除过期的会话
		}

		NatSession session = new NatSession();
		session.lastNanoTime = System.nanoTime();
		session.remoteIP = remoteIP;
		session.remotePort = remotePort;

		if (ProxyConfig.isFakeIP(remoteIP)) {
			session.remoteHost = DnsProxy.reverseLookup(remoteIP);
		}

		if (session.remoteHost == null) {
			session.remoteHost = CommonMethods.ipIntToString(remoteIP);
		}

		sSessions.put(portKey, session);
		return session;
	}

}
