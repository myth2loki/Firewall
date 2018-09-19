package com.protect.kid.core.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.activity.MainActivity;
import com.protect.kid.core.ProxyConfig;
import com.protect.kid.core.dns.DnsPacket;
import com.protect.kid.core.http.HttpRequestHeaderParser;
import com.protect.kid.core.nat.NatSession;
import com.protect.kid.core.nat.NatSessionManager;
import com.protect.kid.core.proxy.DnsProxy;
import com.protect.kid.core.proxy.TcpProxyServer;
import com.protect.kid.core.proxy.UdpProxyServer;
import com.protect.kid.core.tcpip.CommonMethods;
import com.protect.kid.core.tcpip.IPHeader;
import com.protect.kid.core.tcpip.TCPHeader;
import com.protect.kid.core.tcpip.UDPHeader;
import com.protect.kid.core.util.VpnServiceHelper;
import com.protect.kid.event.VPNEvent;
import com.protect.kid.filter.BlackListFilter;
import com.protect.kid.filter.CustomContentFilter;
import com.protect.kid.filter.CustomIpFilter;
import com.protect.kid.filter.HtmlBlockingInfoBuilder;
import com.protect.kid.filter.PushBlackContentFilter;
import com.protect.kid.filter.PushBlackIpFilter;
import com.protect.kid.filter.TimeDurationFilter;
import com.protect.kid.filter.TimeRangeFilter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import de.greenrobot.event.EventBus;

public class IpProtectVpnService extends VpnService implements Runnable {
	private static final String TAG = "FirewallVpnService";
	private static final boolean DEBUG = BuildConfig.DEBUG;

	private static int ID;
	private static int LOCAL_IP;
	private boolean IsRunning = false;
	private Thread mVPNThread;
	private ParcelFileDescriptor mVPNInterface;
	private TcpProxyServer mTcpProxyServer;
	private UdpProxyServer mUdpProxyServer;
	private DnsProxy mDnsProxy;
	private FileOutputStream mVPNOutputStream;

	private long mSentBytes;
	private long mReceivedBytes;

	public IpProtectVpnService() {
		ID++;
		VpnServiceHelper.onVpnServiceCreated(this);
	}

	//启动Vpn工作线程
	@Override
	public void onCreate() {
		mVPNThread = new Thread(this, "VPNServiceThread");
		mVPNThread.start();
		setVpnRunningStatus(true);
		notifyStatus(new VPNEvent(VPNEvent.Status.STARTING));
		super.onCreate();
	}

	@Override
	public void run() {
		try {
			if (DEBUG) {
				Log.d(TAG, "run: VPNService(" + ID + ") work thread is Running...");
			}

			waitUntilPrepared();

			//设置黑名单
			ProxyConfig.Instance.addDomainFilter(new BlackListFilter());
			ProxyConfig.Instance.addDomainFilter(new CustomIpFilter());
			ProxyConfig.Instance.addHtmlFilter(new CustomContentFilter());
			//设置推送黑名单
			ProxyConfig.Instance.addDomainFilter(new PushBlackIpFilter());
			ProxyConfig.Instance.addHtmlFilter(new PushBlackContentFilter());
			//设置时间规则
			ProxyConfig.Instance.addDomainFilter(new TimeRangeFilter());
			ProxyConfig.Instance.addDomainFilter(new TimeDurationFilter());

			//设置网页内容过滤
			ProxyConfig.Instance.prepare();
			ProxyConfig.Instance.setBlockingInfoBuilder(new HtmlBlockingInfoBuilder());

			//启动TCP代理服务
			mTcpProxyServer = new TcpProxyServer(0);
			mTcpProxyServer.start();
			Log.i(TAG, "run: TcpProxy started");

			//启动udp代理服务
			mUdpProxyServer = new UdpProxyServer();
			mUdpProxyServer.start();
			Log.i(TAG, "run: UdpProxy started");

			//启动dns代理服务
			mDnsProxy = new DnsProxy();
			mDnsProxy.start();
			Log.i(TAG, "run: DnsProxy started");

			//回调vpn启动
			ProxyConfig.Instance.onVpnStart(this);

			runVPN();

			//回调vpn停止
			ProxyConfig.Instance.onVpnEnd(this);

		} catch (InterruptedException e) {
			if (DEBUG) {
				Log.e(TAG, "run: VpnService run catch an exception", e);
			}
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "run: VpnService run catch an exception", e);
			}
		} finally {
			if (DEBUG) {
				Log.d(TAG, "run: VpnService terminated");
			}
			dispose();
		}
	}

	//建立VPN，同时监听出口流量
	private void runVPN() throws Exception {
		this.mVPNInterface = establishVPN();
		this.mVPNOutputStream = new FileOutputStream(mVPNInterface.getFileDescriptor());
		FileInputStream in = new FileInputStream(mVPNInterface.getFileDescriptor());
		int size = 0;
		byte[] mPacket = new byte[20000];
		while (size != -1 && IsRunning) {
			//读取到来自vpn的数据，也就是来自拦截的对外请求的数据报文
			while ((size = in.read(mPacket)) > 0 && IsRunning) {
				if (mDnsProxy.isStopped() || mTcpProxyServer.isStopped()) {
					in.close();
					throw new Exception("LocalServer stopped.");
				}
				onIPPacketReceived(mPacket, size);
			}
			//非阻塞模式，休眠已节约电量
			Thread.sleep(100);
		}
		in.close();
		disconnectVPN();
	}

	//只设置IsRunning = true;
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	//停止Vpn工作线程
	@Override
	public void onDestroy() {
		if (DEBUG) {
			Log.d(TAG, "onDestroy: VPNService(" + ID + ") destroyed.");
		}
		if (mVPNThread != null) {
			mVPNThread.interrupt();
		}
		VpnServiceHelper.onVpnServiceDestroy();
		super.onDestroy();
	}

	//发送UDP数据报到应用
	public void sendUDPPacket(IPHeader ipHeader, UDPHeader udpHeader) {
		try {
			CommonMethods.ComputeUDPChecksum(ipHeader, udpHeader);
			this.mVPNOutputStream.write(ipHeader.mData, ipHeader.mOffset, ipHeader.getTotalLength());
		} catch (IOException e) {
			if (DEBUG) {
				Log.e(TAG, "sendUDPPacket: VpnService send UDP packet catch an exception", e);
			}
		}
	}

	/**
	 * 收到数据
	 * @param buff 数据
	 * @param size IP报文大小
	 * @throws IOException
	 */
	private void onIPPacketReceived(byte[] buff, int size) throws IOException {
		IPHeader ipHeader = new IPHeader(buff, 0);
		switch (ipHeader.getProtocol()) {
			case IPHeader.TCP:
				TCPHeader tcpHeader = new TCPHeader(buff, 20);
				if (tcpHeader.getSourcePort() == mTcpProxyServer.getPort()) { //tcp proxy发来的报文

					//从session中取出缓存的请求信息，比如：目的ip、端口等
					NatSession session = NatSessionManager.getSession(tcpHeader.getDestinationPort());
					if (session != null) {
						ipHeader.setSourceIP(ipHeader.getDestinationIP());
						tcpHeader.setSourcePort(session.remotePort);
						ipHeader.setDestinationIP(LOCAL_IP);

						CommonMethods.ComputeTCPChecksum(ipHeader, tcpHeader);
						mVPNOutputStream.write(ipHeader.mData, ipHeader.mOffset, size); //写到真实的应用
						mReceivedBytes += size;
					} else {
						if (DEBUG) {
							Log.i(TAG, String.format("onIPPacketReceived NoSession: %s %s\n", ipHeader.toString(), tcpHeader.toString()));
						}
					}

				} else {

					//添加端口映射
					int portKey = tcpHeader.getSourcePort();
					//获取缓存信息
					NatSession session = NatSessionManager.getSession(portKey);
					if (DEBUG) {
						Log.d(TAG, "onIPPacketReceived: session = " + session);
					}
					if (session == null || session.remoteIP != ipHeader.getDestinationIP() || session.remotePort
							!= tcpHeader.getDestinationPort()) {
						session = NatSessionManager.createSession(portKey, ipHeader.getDestinationIP(), tcpHeader
								.getDestinationPort());
					}

					session.lastNanoTime = System.nanoTime();
					session.packetSent++; //注意顺序

					int tcpDataSize = ipHeader.getDataLength() - tcpHeader.getHeaderLength();
					if (session.packetSent == 2 && tcpDataSize == 0) {
						return; //丢弃tcp握手的第二个ACK报文。因为客户端发数据的时候也会带上ACK，这样可以在服务器Accept之前分析出HOST信息。
					}

					//分析数据，找到host
					if (session.bytesSent == 0 && tcpDataSize > 10) {
						int dataOffset = tcpHeader.mOffset + tcpHeader.getHeaderLength();
						//解析http请求头，将信息存储到session中
						HttpRequestHeaderParser.parseHttpRequestHeader(session, tcpHeader.mData, dataOffset,
								tcpDataSize);
					}

					//转发给本地TCP代理服务器
					ipHeader.setSourceIP(ipHeader.getDestinationIP());
					ipHeader.setDestinationIP(LOCAL_IP); //目的地址
					tcpHeader.setDestinationPort(mTcpProxyServer.getPort()); //目的端口

					CommonMethods.ComputeTCPChecksum(ipHeader, tcpHeader);
					mVPNOutputStream.write(ipHeader.mData, ipHeader.mOffset, size);
					session.bytesSent += tcpDataSize; //注意顺序
					mSentBytes += size;
				}
				break;
			case IPHeader.UDP:
				UDPHeader udpHeader = new UDPHeader(buff, 0);
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "onIPPacketReceived: just udp packet = " + udpHeader);
				}
				udpHeader.mOffset = ipHeader.getHeaderLength();
				if (ipHeader.getSourceIP() == LOCAL_IP && udpHeader.getDestinationPort() == 53) {
					ByteBuffer mDNSBuffer = ((ByteBuffer) ByteBuffer.wrap(buff).position(28)).slice();
					mDNSBuffer.limit(udpHeader.getTotalLength() - 8);
					DnsPacket dnsPacket = DnsPacket.fromBytes(mDNSBuffer);
					if (dnsPacket != null && dnsPacket.header.questionCount > 0) {
						if (DEBUG) {
							Log.d(TAG, "onIPPacketReceived: query dns " + dnsPacket.questions[0].domain);
						}
						mDnsProxy.onDnsRequestReceived(ipHeader, udpHeader, dnsPacket);
					}
				} else {
					// 其他的udp包需要转发，创建UdpProxyServer用于转发
					if (BuildConfig.DEBUG) {
						Log.d(TAG, "onIPPacketReceived: old content header = " + ipHeader + " | " + udpHeader.getSourcePort() + "->" + udpHeader.getDestinationPort());
					}
					mUdpProxyServer.onUdpRequestReceived(ipHeader, udpHeader);
				}
				break;
		}

	}

	private void waitUntilPrepared() {
		while (prepare(this) != null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				if (DEBUG) {
					Log.e(TAG, "waitUntilPrepared: ", e);
				}
			}
		}
	}

	/**
	 * 建立VPN服务
	 * @return
	 * @throws Exception
	 */
	private ParcelFileDescriptor establishVPN() throws Exception {
		Builder builder = new Builder();
		builder.setMtu(ProxyConfig.Instance.getMTU());

		ProxyConfig.IPAddress ipAddress = ProxyConfig.Instance.getDefaultLocalIP();
		LOCAL_IP = CommonMethods.ipStringToInt(ipAddress.Address);
		builder.addAddress(ipAddress.Address, ipAddress.PrefixLength);

		for (ProxyConfig.IPAddress dns : ProxyConfig.Instance.getDnsList()) {
			builder.addDnsServer(dns.Address);
		}

		if (ProxyConfig.Instance.getRouteList().size() > 0) {
			for (ProxyConfig.IPAddress routeAddress : ProxyConfig.Instance.getRouteList()) {
				builder.addRoute(routeAddress.Address, routeAddress.PrefixLength);
			}
			builder.addRoute(CommonMethods.ipIntToInet4Address(ProxyConfig.FAKE_NETWORK_IP), 16);
		} else {
			builder.addRoute("0.0.0.0", 0);
		}

		Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
		Method method = SystemProperties.getMethod("get", new Class[]{String.class});
		ArrayList<String> servers = new ArrayList<>();
		for (String name : new String[]{"net.dns1", "net.dns2", "net.dns3", "net.dns4",}) {
			String value = (String) method.invoke(null, name);
			if (value != null && !"".equals(value) && !servers.contains(value)) {
				servers.add(value);
				builder.addRoute(value, 32); //添加路由，使得DNS查询流量也走该VPN接口
			}
		}

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
		builder.setConfigureIntent(pendingIntent);

		builder.setSession(ProxyConfig.Instance.getSessionName());
		ParcelFileDescriptor pfdDescriptor = builder.establish();
		notifyStatus(new VPNEvent(VPNEvent.Status.ESTABLISHED));
		return pfdDescriptor;
	}

	public void disconnectVPN() {
		try {
			if (mVPNInterface != null) {
				mVPNInterface.close();
				mVPNInterface = null;
			}
		} catch (Exception e) {
			//ignore
		}
		notifyStatus(new VPNEvent(VPNEvent.Status.UNESTABLISHED));
		this.mVPNOutputStream = null;
	}

	private synchronized void dispose() {
		//断开VPN
		disconnectVPN();

		//停止TCP代理服务
		if (mTcpProxyServer != null) {
			mTcpProxyServer.stop();
			mTcpProxyServer = null;
		}

		if (mDnsProxy != null) {
			mDnsProxy.stop();
			mDnsProxy = null;
		}

		stopSelf();
		setVpnRunningStatus(false);
	}

	private void notifyStatus(VPNEvent event) {
		EventBus.getDefault().post(event);
	}

	public boolean vpnRunningStatus() {
		return IsRunning;
	}

	public void setVpnRunningStatus(boolean isRunning) {
		IsRunning = isRunning;
	}
}
