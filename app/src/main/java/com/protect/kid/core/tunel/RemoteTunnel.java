package com.protect.kid.core.tunel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by zengzheying on 15/12/31.
 */
public class RemoteTunnel extends RawTunnel {
	public RemoteTunnel(SocketChannel innerChannel, Selector selector) {
		super(innerChannel, selector);
		setRemoteTunnel(true);
	}

	public RemoteTunnel(InetSocketAddress serverAddress, Selector selector) throws IOException {
		super(serverAddress, selector);
		setRemoteTunnel(true);
	}
}