package com.protect.kid.core.proxy;

import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.core.ProxyConfig;
import com.protect.kid.core.filter.Filter;
import com.protect.kid.core.nat.NatSession;
import com.protect.kid.core.nat.NatSessionManager;
import com.protect.kid.core.tcpip.CommonMethods;
import com.protect.kid.core.tunel.BaseTunnel;
import com.protect.kid.core.tunel.LocalTunnel;
import com.protect.kid.core.tunel.RemoteTunnel;
import com.protect.kid.core.tunel.TunnelFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class TcpProxyServer implements Runnable {
	private static final String TAG = "TcpProxyServer";
	private static final boolean DEBUG = BuildConfig.DEBUG;

	private boolean mStopped;
	private short mPort;

	/**
	 * 用于接收转发来的tcp报文，通过重新设定目的地址和端口重定向过来
	 */
	private Selector mProxySelector;
	private ServerSocketChannel mProxyServerSocketChannel;

	/**
	 * 构造TcpProxyServer实例
	 * @param port 端口
	 * @throws IOException
	 */
	public TcpProxyServer(int port) throws IOException {
		mProxySelector = Selector.open();
		mProxyServerSocketChannel = ServerSocketChannel.open();
		mProxyServerSocketChannel.configureBlocking(false);
		mProxyServerSocketChannel.socket().bind(new InetSocketAddress(port)); //绑定端口
		mProxyServerSocketChannel.register(mProxySelector, SelectionKey.OP_ACCEPT, mProxyServerSocketChannel);
		this.mPort = (short) mProxyServerSocketChannel.socket().getLocalPort();

		if (DEBUG) {
			Log.d(TAG, String.format("TcpProxyServer: AsyncTcpServer listen on %s:%d success.",
					mProxyServerSocketChannel.socket().getInetAddress()
					.toString(), this.mPort & 0xFFFF));
		}
	}

	/**
	 * 启动TcpProxyServer线程
	 */
	public void start() {
		Thread mServerThread = new Thread(this, "TcpProxyServerThread");
		mServerThread.start();
	}

	/**
	 * 04-22 02:48:31.518 24379-24428/com.timedancing.easyfirewall D/TcpProxyServer: run: onAccepted
	 * 04-22 02:48:31.523 24379-24428/com.timedancing.easyfirewall D/TcpProxyServer: run: onConnectable
	 * 04-22 02:48:31.524 24379-24428/com.timedancing.easyfirewall D/TcpProxyServer: run: onReadable
	 */
	@Override
	public void run() {
		try {
			while (true) {
				mProxySelector.select();
				Iterator<SelectionKey> keyIterator = mProxySelector.selectedKeys().iterator();
				while (keyIterator.hasNext()) {
					SelectionKey key = keyIterator.next();
					if (key.isValid()) {
						try {
							//来自tunnel的操作
							if (key.isReadable()) { // a channel is ready for reading
//								Log.d(TAG, "run: onReadable");
								((BaseTunnel) key.attachment()).onReadable(key);
							} else if (key.isWritable()) { // a channel is ready for writing
//								Log.d(TAG, "run: onWritable");
								((BaseTunnel) key.attachment()).onWritable(key);
							} else if (key.isConnectable()) { // a connection was established with a remote server.
//								Log.d(TAG, "run: onConnectable");
								((BaseTunnel) key.attachment()).onConnectable();
							//来自ProxyServer的Accept操作
							} else if (key.isAcceptable()) { // a connection was accepted by a ServerSocketChannel.
//								Log.d(TAG, "run: onAccepted");
								onAccepted(key);
							}
						} catch (Exception e) {
							if (DEBUG) {
								Log.e(TAG, "run: TcpProxyServer iterate SelectionKey catch an exception", e);
							}
						}
					}
					keyIterator.remove();
				}

			}
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "run: TcpProxyServer error", e);
			}
		} finally {
			this.stop();
			Log.i(TAG, "run: TcpServer thread exited.");
		}
	}

	public void stop() {
		this.mStopped = true;
		if (mProxySelector != null) {
			try {
				mProxySelector.close();
				mProxySelector = null;
			} catch (Exception e) {
				if (DEBUG) {
					Log.e(TAG, "stop: ", e);
				}
			}
		}

		if (mProxyServerSocketChannel != null) {
			try {
				mProxyServerSocketChannel.close();
				mProxyServerSocketChannel = null;
			} catch (Exception e) {
				if (DEBUG) {
					Log.e(TAG, "stop: ", e);
				}
			}
		}
	}

	/**
	 * 获取目的ip地址和端口，在转发的时候已经写入source中
	 * @param localChannel
	 * @return
	 */
	private NatSession getNatSession(SocketChannel localChannel) {
		int port = localChannel.socket().getPort();
		return NatSessionManager.getSession((short) port);
	}

//	private InetSocketAddress getCachedDestAddress(SocketChannel localChannel) {
//		int portKey = localChannel.socket().getPort();
//		NatSession session = NatSessionManager.getSession((short)portKey);
//		if (session != null) {
//			if (DEBUG) {
//				Log.d(TAG, "getCachedDestAddress: session = " + session);
//			}
//			if (ProxyConfig.Instance.filter(session.remoteHost, session.remoteIP, session.remotePort)) {
//				//完成具体的拦截
//				if (DEBUG) {
//					Log.d(TAG, String.format("getCachedDestAddress: %d/%d:[BLOCK] %s=>%s:%d\n", NatSessionManager.getSessionCount(), BaseTunnel.sSessionCount,
//							session.remoteHost,
//							CommonMethods.ipIntToString(session.remoteIP), session.remotePort & 0xFFFF));
//				}
//				return null;
//			} else {
//				return new InetSocketAddress(localChannel.socket().getInetAddress(), session.remotePort & 0xFFFF);
//			}
//		}
//		return null;
//	}

	/**
	 * 连接成功，绑定
	 * @param key
	 */
	private void onAccepted(SelectionKey key) {
		LocalTunnel localTunnel = null;
		try {
			//获取local server的通道
			SocketChannel localChannel = ((ServerSocketChannel) key.attachment()).accept();
			//tcp代理服务器有连接进来，localChannel代表应用与代理服务器的连接
			localTunnel = TunnelFactory.createLocalTunnel(localChannel, mProxySelector); //将连接方和受vpn保护的socket配对

			//有连接连进来，获取到目的地址。其实就是连接方地址，因为在转发的时候已经将目的地址和端口写入到源地址和端口上
			InetSocketAddress destAddress;
			NatSession session = getNatSession(localChannel);
			if (session != null) {
				if (DEBUG) {
					Log.d(TAG, "getCachedDestAddress: session = " + session);
				}
				int result = ProxyConfig.Instance.filter(session.remoteHost, session.remoteIP, session.remotePort);
				if (result != Filter.NO_FILTER) {
					//完成具体的拦截
					if (DEBUG) {
						Log.d(TAG, String.format("getCachedDestAddress: %d/%d:[BLOCK] %s=>%s:%d\n", NatSessionManager.getSessionCount(), BaseTunnel.sSessionCount,
								session.remoteHost,
								CommonMethods.ipIntToString(session.remoteIP), session.remotePort & 0xFFFF));
					}
					localTunnel.sendBlockInformation(result);
					if (DEBUG) {
						short portKey = (short) localChannel.socket().getPort();
						Log.d(TAG, String.format("onAccepted: Error: socket(%s:%d) have no session.", localChannel.socket().getInetAddress()
								.toString(), portKey));
					}
					localTunnel.dispose();
				} else {
					destAddress = new InetSocketAddress(localChannel.socket().getInetAddress(), session.remotePort & 0xFFFF);
					//创建远程tunnel，受vpn protect
					RemoteTunnel remoteTunnel = TunnelFactory.createRemoteTunnel(destAddress, mProxySelector);
					//关联兄弟
					remoteTunnel.setIsHttpsRequest(localTunnel.isHttpsRequest());
					remoteTunnel.pair(localTunnel);
					remoteTunnel.protect();
					remoteTunnel.connect(); //开始连接
				}
			} else {
				if (DEBUG) {
					Log.d(TAG, String.format("getCachedDestAddress: %d/%d:[BLOCK] %s=>%s:%d\n", NatSessionManager.getSessionCount(), BaseTunnel.sSessionCount,
							session.remoteHost,
							CommonMethods.ipIntToString(session.remoteIP), session.remotePort & 0xFFFF));
				}
				localTunnel.sendBlockInformation(Filter.FILTER_LIST);
				if (DEBUG) {
					short portKey = (short) localChannel.socket().getPort();
					Log.d(TAG, String.format("onAccepted: Error: socket(%s:%d) have no session.", localChannel.socket().getInetAddress()
							.toString(), portKey));
				}
				localTunnel.dispose();
			}
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "onAccepted: TcpProxyServer onAccepted catch an exception", e);
			}
			if (localTunnel != null) {
				localTunnel.dispose();
			}
		}
	}

	public boolean isStopped() {
		return mStopped;
	}

	public short getPort() {
		return mPort;
	}
}
