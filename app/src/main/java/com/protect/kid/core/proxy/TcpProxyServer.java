package com.protect.kid.core.proxy;

import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.constant.AppDebug;
import com.protect.kid.core.ProxyConfig;
import com.protect.kid.core.nat.NatSession;
import com.protect.kid.core.nat.NatSessionManager;
import com.protect.kid.core.tcpip.CommonMethods;
import com.protect.kid.core.tunel.Tunnel;
import com.protect.kid.core.tunel.TunnelFactory;
import com.protect.kid.util.DebugLog;

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

		DebugLog.i("AsyncTcpServer listen on %s:%d success.\n", mProxyServerSocketChannel.socket().getInetAddress()
				.toString(), this.mPort & 0xFFFF);
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
							if (key.isReadable()) {
//								Log.d(TAG, "run: onReadable");
								((Tunnel) key.attachment()).onReadable(key);
							} else if (key.isWritable()) {
//								Log.d(TAG, "run: onWritable");
								((Tunnel) key.attachment()).onWritable(key);
							} else if (key.isConnectable()) {
//								Log.d(TAG, "run: onConnectable");
								((Tunnel) key.attachment()).onConnectable();
							//来自tunnel的操作
							} else if (key.isAcceptable()) {
//								Log.d(TAG, "run: onAccepted");
								onAccepted(key);
							}
						} catch (Exception ex) {
							if (AppDebug.IS_DEBUG) {
								ex.printStackTrace(System.err);
							}

							DebugLog.e("TcpProxyServer iterate SelectionKey catch an exception: %s", ex);
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
			} catch (Exception ex) {
				if (AppDebug.IS_DEBUG) {
					ex.printStackTrace(System.err);
				}
				DebugLog.e("TcpProxyServer mProxySelector.close() catch an exception: %s", ex);
			}
		}

		if (mProxyServerSocketChannel != null) {
			try {
				mProxyServerSocketChannel.close();
				mProxyServerSocketChannel = null;
			} catch (Exception ex) {
				if (AppDebug.IS_DEBUG) {
					ex.printStackTrace(System.err);
				}

				DebugLog.e("TcpProxyServer mProxyServerSocketChannel.close() catch an exception: %s", ex);
			}
		}
	}

	/**
	 * 获取目的ip地址和端口，在转发的时候已经写入source中
	 * @param localChannel
	 * @return
	 */
	private InetSocketAddress getCachedDestAddress(SocketChannel localChannel) {
		int portKey = localChannel.socket().getPort();
		NatSession session = NatSessionManager.getSession((short)portKey);
		if (session != null) {
			if (DEBUG) {
				Log.d(TAG, "getCachedDestAddress: session = " + session);
			}
			if (ProxyConfig.Instance.filter(session.remoteHost, session.remoteIP, session.remotePort)) {
				//TODO 完成跟具体的拦截策略？？？
				if (DEBUG) {
					Log.d(TAG, String.format("getCachedDestAddress: %d/%d:[BLOCK] %s=>%s:%d\n", NatSessionManager.getSessionCount(), Tunnel.SessionCount,
							session.remoteHost,
							CommonMethods.ipIntToString(session.remoteIP), session.remotePort & 0xFFFF));
				}
				return null;
			} else {
				return new InetSocketAddress(localChannel.socket().getInetAddress(), session.remotePort & 0xFFFF);
			}
		}
		return null;
	}

	/**
	 * 连接成功，绑定
	 * @param key
	 */
	private void onAccepted(SelectionKey key) {
		Tunnel localTunnel = null;
		try {
			//获取local server的通道
//			SocketChannel localChannel = mProxyServerSocketChannel.accept();
			SocketChannel localChannel = ((ServerSocketChannel) key.attachment()).accept();
			//tcp代理服务器有连接进来，localChannel代表应用与代理服务器的连接
			localTunnel = TunnelFactory.wrap(localChannel, mProxySelector); //TODO 为何要调用wrap方法？ 因为需要将连接方和受vpn保护的socket配对

			//有连接连进来，获取到目的地址。其实就是连接方地址，因为在转发的时候已经将目的地址和端口写入到源地址和端口上
			// dstIp = localChannel.socket().getInetAddress dstPort = localChannel.socket().getPort()
			InetSocketAddress destAddress = getCachedDestAddress(localChannel);
			if (destAddress != null) {
				//创建远程tunnel，受vpn protect
				Tunnel remoteTunnel = TunnelFactory.wrap(destAddress, mProxySelector);
				//关联兄弟
				remoteTunnel.setIsHttpsRequest(localTunnel.isHttpsRequest());
				remoteTunnel.pair(localTunnel);
				remoteTunnel.protect();
				remoteTunnel.connect(); //开始连接
			} else {
				localTunnel.sendBlockInformation();
				if (DEBUG) {
					short portKey = (short) localChannel.socket().getPort();
					Log.d(TAG, String.format("onAccepted: Error: socket(%s:%d) have no session.", localChannel.socket().getInetAddress()
							.toString(), portKey));
				}
				localTunnel.dispose();
			}
		} catch (Exception ex) {
			if (DEBUG) {
				Log.e(TAG, "onAccepted: TcpProxyServer onAccepted catch an exception: %s", ex);
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
