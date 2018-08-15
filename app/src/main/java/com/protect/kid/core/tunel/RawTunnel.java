package com.protect.kid.core.tunel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class RawTunnel extends BaseTunnel {

	RawTunnel(SocketChannel innerChannel, Selector selector) {
		super(innerChannel, selector);
	}

	RawTunnel(Selector selector) throws IOException {
		super(selector);
	}

	@Override
	protected void onConnected(ByteBuffer buffer) throws Exception {
		onTunnelEstablished();
	}

	@Override
	protected boolean isTunnelEstablished() {
		return true;
	}

	@Override
	protected void beforeSend(ByteBuffer buffer) throws Exception {

	}

	@Override
	protected void afterReceived(ByteBuffer buffer) throws Exception {

	}

	@Override
	protected void onDispose() {

	}
}
