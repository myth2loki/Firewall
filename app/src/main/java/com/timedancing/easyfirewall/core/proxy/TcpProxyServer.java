package com.timedancing.easyfirewall.core.proxy;

import com.timedancing.easyfirewall.constant.AppDebug;
import com.timedancing.easyfirewall.core.ProxyConfig;
import com.timedancing.easyfirewall.core.nat.NatSession;
import com.timedancing.easyfirewall.core.nat.NatSessionManager;
import com.timedancing.easyfirewall.core.tcpip.CommonMethods;
import com.timedancing.easyfirewall.core.tunel.Tunnel;
import com.timedancing.easyfirewall.core.tunel.TunnelFactory;
import com.timedancing.easyfirewall.util.DebugLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by zengzheying on 15/12/30.
 */
public class TcpProxyServer implements Runnable {
	private static final String TAG = "TcpProxyServer";

	public boolean Stopped;
	public short Port;

	private Selector mSelector;
	private ServerSocketChannel mServerSocketChannel;

	public TcpProxyServer(int port) throws IOException {
		mSelector = Selector.open();
		mServerSocketChannel = ServerSocketChannel.open();
		mServerSocketChannel.configureBlocking(false);
		mServerSocketChannel.socket().bind(new InetSocketAddress(port));
		mServerSocketChannel.register(mSelector, SelectionKey.OP_ACCEPT, mServerSocketChannel);
		this.Port = (short) mServerSocketChannel.socket().getLocalPort();

		DebugLog.i("AsyncTcpServer listen on %s:%d success.\n", mServerSocketChannel.socket().getInetAddress()
				.toString(), this.Port & 0xFFFF);
	}

	/**
	 * 启动TcpProxyServer线程
	 */
	public void start() {
		Thread mServerThread = new Thread(this, "TcpProxyServerThread");
		mServerThread.start();
	}

	public void stop() {
		this.Stopped = true;
		if (mSelector != null) {
			try {
				mSelector.close();
				mSelector = null;
			} catch (Exception ex) {
				if (AppDebug.IS_DEBUG) {
					ex.printStackTrace(System.err);
				}
				DebugLog.e("TcpProxyServer mSelector.close() catch an exception: %s", ex);
			}
		}

		if (mServerSocketChannel != null) {
			try {
				mServerSocketChannel.close();
				mServerSocketChannel = null;
			} catch (Exception ex) {
				if (AppDebug.IS_DEBUG) {
					ex.printStackTrace(System.err);
				}

				DebugLog.e("TcpProxyServer mServerSocketChannel.close() catch an exception: %s", ex);
			}
		}
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
				mSelector.select();
				Iterator<SelectionKey> keyIterator = mSelector.selectedKeys().iterator();
				while (keyIterator.hasNext()) {
					SelectionKey key = keyIterator.next();
					if (key.isValid()) {
						try {
							if (key.isReadable()) {
//								Log.d(TAG, "run: onReadable");
								((Tunnel) key.attachment()).onReadable(key);
							} else if (key.isWritable()) {
//								Log.d(TAG, "run: onWritable");
								((Tunnel) key.attachment()).onWritable(key);
							} else if (key.isConnectable()) {
//								Log.d(TAG, "run: onConnectable");
								((Tunnel) key.attachment()).onConnectable();
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
			if (AppDebug.IS_DEBUG) {
				e.printStackTrace(System.err);
			}

			DebugLog.e("TcpProxyServer catch an exception: %s", e);
		} finally {
			this.stop();
			DebugLog.i("TcpServer thread exited.");
		}
	}

	private InetSocketAddress getDestAddress(SocketChannel localChannel) {
		short portKey = (short) localChannel.socket().getPort();
		NatSession session = NatSessionManager.getSession(portKey);
		if (session != null) {
			if (ProxyConfig.Instance.filter(session.RemoteHost, session.RemoteIP)) {
				//TODO 完成跟具体的拦截策略？？？
				DebugLog.i("%d/%d:[BLOCK] %s=>%s:%d\n", NatSessionManager.getSessionCount(), Tunnel.SessionCount,
						session.RemoteHost,
						CommonMethods.ipIntToString(session.RemoteIP), session.RemotePort & 0xFFFF);

				return null;
			} else {
				return new InetSocketAddress(localChannel.socket().getInetAddress(), session.RemotePort & 0xFFFF);
			}
		}
		return null;
	}

	/**
	 * 连接成功
	 * @param key
	 */
	private void onAccepted(SelectionKey key) {
		Tunnel localTunnel = null;
		try {
			//获取local server的通道
//			SocketChannel localChannel = mServerSocketChannel.accept();
			SocketChannel localChannel = ((ServerSocketChannel) key.attachment()).accept();
			localTunnel = TunnelFactory.wrap(localChannel, mSelector);

			InetSocketAddress destAddress = getDestAddress(localChannel);
			if (destAddress != null) {
				Tunnel remoteTunnel = TunnelFactory.createTunnelByConfig(destAddress, mSelector);
				//关联兄弟
				remoteTunnel.setIsHttpsRequest(localTunnel.isHttpsRequest());
				remoteTunnel.setBrotherTunnel(localTunnel);
				localTunnel.setBrotherTunnel(remoteTunnel);
				remoteTunnel.connect(destAddress); //开始连接
			} else {
				short portKey = (short) localChannel.socket().getPort();
				NatSession session = NatSessionManager.getSession(portKey);
				if (session != null) {
					DebugLog.i("Have block a request to %s=>%s:%d", session.RemoteHost, CommonMethods.ipIntToString
									(session.RemoteIP),
							session.RemotePort & 0xFFFF);
					localTunnel.sendBlockInformation();
				} else {
					DebugLog.i("Error: socket(%s:%d) have no session.", localChannel.socket().getInetAddress()
							.toString(), portKey);
				}

				localTunnel.dispose();
			}
		} catch (Exception ex) {
			if (AppDebug.IS_DEBUG) {
				ex.printStackTrace(System.err);
			}

			DebugLog.e("TcpProxyServer onAccepted catch an exception: %s", ex);

			if (localTunnel != null) {
				localTunnel.dispose();
			}
		}
	}
}
