package com.protect.kid.core.tunel;

import android.util.Log;

import com.protect.kid.BuildConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class RemoteTunnel extends RawTunnel {
	private static final boolean DEBUG = BuildConfig.DEBUG;
	private static final String TAG = "RemoteTunnel";

	private Selector mSelector;
	private SocketAddress mRemoteAddress;

	RemoteTunnel(InetSocketAddress remoteAddress, Selector selector) throws IOException {
		super(selector);
		mSelector = selector;
		mRemoteAddress = remoteAddress;
		setRemoteTunnel(true);
	}

	/**
	 * 连接到真实服务器
	 * @throws IOException
	 */
	public void connect() throws Exception {
		SocketChannel innerChannel = getInnerChannel();
		innerChannel.register(mSelector, SelectionKey.OP_CONNECT, this); //注册连接事件
		innerChannel.connect(mRemoteAddress);
		if (DEBUG) {
			Log.d(TAG, String.format("connect: to %s", mRemoteAddress));
		}
	}
}
