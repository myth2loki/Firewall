package com.protect.kid.core.tunel;

import com.protect.kid.core.nat.NatSession;
import com.protect.kid.core.nat.NatSessionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class TunnelFactory {

	/**
	 * 包装成本地tunnel
	 * @param localChannel
	 * @param selector
	 * @return
	 */
	public static LocalTunnel createLocalTunnel(SocketChannel localChannel, Selector selector) {
		LocalTunnel tunnel = new LocalTunnel(localChannel, selector);
		//获取是否是https
		NatSession session = NatSessionManager.getSession((short) localChannel.socket().getPort());
		if (session != null) {
			tunnel.setIsHttpsRequest(session.isHttpsSession);
		}
		return tunnel;
	}

	/**
	 * 包装成远程tunne
	 * @param destAddress
	 * @param selector
	 * @return
	 * @throws IOException
	 */
	public static RemoteTunnel createRemoteTunnel(InetSocketAddress destAddress, Selector selector) throws IOException {
		return new RemoteTunnel(destAddress, selector);
	}
}
