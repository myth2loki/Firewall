package com.protect.kid.core.tunel;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class LocalTunnel extends RawTunnel {

	LocalTunnel(SocketChannel innerChannel, Selector selector) {
		super(innerChannel, selector);
		setRemoteTunnel(false);
	}
}
