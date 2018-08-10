package com.timedancing.easyfirewall.core.tunel;

import com.timedancing.easyfirewall.core.nat.NatSession;
import com.timedancing.easyfirewall.core.nat.NatSessionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class TunnelFactory {

	/**
	 * 包装成本地tunnel
	 * @param channel
	 * @param selector
	 * @return
	 */
	public static Tunnel wrap(SocketChannel channel, Selector selector) {
		Tunnel tunnel = new RawTunnel(channel, selector);
//		NatSession session = NatSessionManager.getSession((short) channel.socket().getPort());
////		if (session != null) {
////			tunnel.setIsHttpsRequest(session.isHttpsSession);
////		}
		return tunnel;
	}

	/**
	 * 包装成远程tunne
	 * @param destAddress
	 * @param selector
	 * @return
	 * @throws IOException
	 */
	public static Tunnel wrap(InetSocketAddress destAddress, Selector selector) throws IOException {
		//TODO 这里只是简单创建一个RawTunnel，日后可以根据代理类型创建不同的Tunnel
		return new RemoteTunnel(destAddress, selector);
	}
}
