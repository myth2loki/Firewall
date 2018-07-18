package com.timedancing.easyfirewall.core.proxy;

import android.util.SparseArray;

import com.timedancing.easyfirewall.constant.AppDebug;
import com.timedancing.easyfirewall.core.ProxyConfig;
import com.timedancing.easyfirewall.core.dns.DnsPacket;
import com.timedancing.easyfirewall.core.dns.Question;
import com.timedancing.easyfirewall.core.dns.Resource;
import com.timedancing.easyfirewall.core.dns.ResourcePointer;
import com.timedancing.easyfirewall.core.tcpip.CommonMethods;
import com.timedancing.easyfirewall.core.tcpip.IPHeader;
import com.timedancing.easyfirewall.core.tcpip.UDPHeader;
import com.timedancing.easyfirewall.core.util.VpnServiceHelper;
import com.timedancing.easyfirewall.util.DebugLog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zengzheying on 15/12/29.
 * DNS代理
 */
public class DnsProxy implements Runnable {

	private static final ConcurrentHashMap<Integer, String> IPDomainMaps = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, Integer> DomainIPMaps = new ConcurrentHashMap<>();
	private static final long QUERY_TIMEOUT_NS = 10 * 1000 * 1000 * 1000L;
	/**
	 * 保存dns查询状态
	 */
	private final SparseArray<QueryState> mQueryArray;
	private boolean mStopped;
	private DatagramSocket mClient;
	private short mQueryID;

	public DnsProxy() throws IOException {
		mQueryArray = new SparseArray<>();
		mClient = new DatagramSocket(0);
	}

	/**
	 * 根据ip查询域名
	 *
	 * @param ip ip地址
	 * @return 域名
	 */
	public static String reverseLookup(int ip) {
		return IPDomainMaps.get(ip);
	}


	/**
	 * 启动线程
	 */
	public void start() {
		new Thread(this, "DnsProxyThread").start();
	}

	/**
	 * 停止线程
	 */
	public void stop() {
		mStopped = true;
		if (mClient != null) {
			mClient.close();
			mClient = null;
		}
	}


	@Override
	public void run() {
		try {
			byte[] buff = new byte[20000];
			IPHeader ipHeader = new IPHeader(buff, 0);
			ipHeader.defaultValue();
			int ipHeaderLength = 20;
			UDPHeader udpHeader = new UDPHeader(buff, ipHeaderLength);

			int udpHeaderLength = 8;
			ByteBuffer dnsBuffer = ByteBuffer.wrap(buff);
			dnsBuffer.position(ipHeaderLength + udpHeaderLength);
			dnsBuffer = dnsBuffer.slice(); //去除ip和udp头部

			//不包含头部信息
			DatagramPacket packet = new DatagramPacket(buff, 28, buff.length - (ipHeaderLength +
					udpHeaderLength));

			//不包含头部信息，减去ip和udp头部长度
			packet.setLength(buff.length - (ipHeaderLength + udpHeaderLength));
			while (mClient != null && !mClient.isClosed()) {
				mClient.receive(packet); //获取说道的udp数据报文

				dnsBuffer.clear();
				dnsBuffer.limit(packet.getLength()); //设置dnsBuffer的长度
				try {
					DnsPacket dnsPacket = DnsPacket.fromBytes(dnsBuffer);
					if (dnsPacket != null) {
						OnDnsResponseReceived(ipHeader, udpHeader, dnsPacket);
					}
				} catch (Exception ex) {
					if (AppDebug.IS_DEBUG) {
						ex.printStackTrace(System.err);
					}

					DebugLog.e("Parse dns error: %s\n", ex);
				}
			}
		} catch (Exception e) {
			if (AppDebug.IS_DEBUG) {
				e.printStackTrace(System.err);
			}
			DebugLog.e("DnsProxy Thread catch an exception %s\n", e);
		} finally {
			DebugLog.i("DnsProxy Thread Exited.\n");
			this.stop();
		}
	}

	/**
	 * 从DNS响应报文中获取第一个IP地址
	 *
	 * @param dnsPacket DNS报文
	 * @return 第一个IP地址， 没有则返回0
	 */
	private int getFirstIP(DnsPacket dnsPacket) {
		for (int i = 0; i < dnsPacket.Header.ResourceCount; i++) {
			Resource resource = dnsPacket.Resources[i];
			if (resource.Type == 1) {
				int ip = CommonMethods.readInt(resource.Data, 0);
				return ip;
			}
		}
		return 0;
	}

	/**
	 * 构造dns response信息
	 * @param rawPacket dns数据
	 * @param dnsPacket dns packet
	 * @param newIP 返回的ip地址
	 */
	private void tamperDnsResponse(byte[] rawPacket, DnsPacket dnsPacket, int newIP) {
		Question question = dnsPacket.Questions[0]; //DNS的一个问题

		dnsPacket.Header.setResourceCount((short) 1); //有ip返回
		dnsPacket.Header.setAResourceCount((short) 0); //无信息
		dnsPacket.Header.setEResourceCount((short) 0); //无信息

		// 这里会有个疑问，在DNS报文中，只有头部是固定的，其他部分不一定，这个方法在DNS查询、回复中都有用到，
		// 理论上应该出现数组控件不足的情况吧（查询的DNS包只有头部部分）
		// 那么怎么这里的处理不用按情况分别增加数组空间呢？

		// 其实在DNS查询的时候，这里的rawPacket时LocalVpnService的m_Packet数组的空间
		// 在DNS回复的时候，这里的rawPacket其实是本类run方法的RECEIVE_BUFFER数组的空间
		// 两者的空间都足够大，所以不用增加数组空间
		//TODO 详细解释 http://www.cnblogs.com/cobbliu/archive/2013/04/02/2996333.html
		ResourcePointer resourcePointer = new ResourcePointer(rawPacket, question.Offset() + question.Length());
		/**
		 域名字段（不定长或2字节）：记录中资源数据对应的名字，它的格式和查询名字段格式相同。当报文中域名重复出现时，
		 就需要使用2字节的偏移指针来替换。例如，在资源记录中，域名通常是查询问题部分的域名的重复，
		 就需要用指针指向查询问题部分的域名。关于指针怎么用，TCP/IP详解里面有，即2字节的指针，最签名的两个高位是11，
		 用于识别指针。其他14位从报文开始处计数（从0开始），指出该报文中的相应字节数。注意，DNS报文的第一个字节是字节0，
		 第二个报文是字节1。一般响应报文中，资源部分的域名都是指针C00C(1100000000001100)，刚好指向请求部分的域名。

		 请求的域名。需要注意的是，此处的域名有两种类型的标示防范，一是上面提到的元信息标示方法；二是指针法。
		 指针法中请求的域名由一个16位的地址标示，该地址指向请求部分中的域名，它的地址是请求部分中域名距离消息开头的偏移量
		 */
		resourcePointer.setDomain((short) 0xC00C); //指针类型，指向问题区的域名
		/**
		 类型TYPE 2个字节表示资源记录的类型，指出RDATA数据的含义
		 */
		resourcePointer.setType(question.Type);
		/**
		 类CLASS 2个字节表示RDATA的类
		 */
		resourcePointer.setClass(question.Class);
		/**
		 生存时间TTL 4字节无符号整数表示资源记录可以缓存的时间。0代表只能被传输，但是不能被缓存。
		 */
		resourcePointer.setTTL(ProxyConfig.Instance.getDnsTTL());
		/**
		 资源数据长度（2字节）：表示资源数据的长度（以字节为单位，如果资源数据为IP则为0004）
		 */
		resourcePointer.setDataLength((short) 4);
		resourcePointer.setIP(newIP);

		// DNS报头长度 + 问题长度 + 资源记录长度（域名指针[2字节] + 类型[2字节] +
		// 类[2字节] + TTL[4字节] + 资源数据长度[2字节] + content[4字节] = 16字节）
		dnsPacket.Size = 12 + question.Length() + 16;
	}

	/**
	 * 获取或创建一个指定域名的虚假IP地址
	 *
	 * @param domainString 指定域名
	 * @return 虚假IP地址
	 */
	private int getOrCreateFakeIP(String domainString) {
		Integer fakeIP = DomainIPMaps.get(domainString);
		if (fakeIP == null) {
			int hashIP = domainString.hashCode();
			do {
				fakeIP = ProxyConfig.FAKE_NETWORK_IP | (hashIP & 0x0000FFFF);
				hashIP++;
			} while (IPDomainMaps.containsKey(fakeIP));

			DomainIPMaps.put(domainString, fakeIP);
			IPDomainMaps.put(fakeIP, domainString);
		}
		return fakeIP;
	}

	/**
	 * 对收到的DNS答复进行修改，以达到DNS污染的目的
	 *
	 * @param rawPacket ip包的数据部分
	 * @param dnsPacket DNS数据包
	 * @return true: 修改了数据 false: 未修改数据
	 */
	private boolean dnsPollution(byte[] rawPacket, DnsPacket dnsPacket) {
		if (dnsPacket.Header.ResourceCount > 0) {
			Question question = dnsPacket.Questions[0];
			/**
			 名字	数值 	描述
			 A		（1） 	期望获得查询名的IP地址。
			 NS 	（2） 	一个授权的域名服务器。
			 CNAME 	（5） 	规范名称。
			 PTR 	（12） 	指针记录。
			 HINFO 	（13） 	主机信息。
			 MX 	（15） 	邮件交换记录。
			 AXFR 	（252） 	对区域转换的请求。
			 ANY 	（255） 	对所有记录的请求。
			 */
			if (question.Type == 1) { //希望获取域名的ip
				int realIP = getFirstIP(dnsPacket);
				//过滤
				if (ProxyConfig.Instance.filter(question.Domain, realIP)) {
					int fakeIP = getOrCreateFakeIP(question.Domain);
					//使用fakeIp
					tamperDnsResponse(rawPacket, dnsPacket, fakeIP);

					DebugLog.i("FakeDns: %s=>%s(%s)\n", question.Domain, CommonMethods.ipIntToString(realIP),
							CommonMethods.ipIntToString(fakeIP));
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 收到Dns查询回复，对指定域名进行污染后，转发给发起请求的客户端
	 */
	private void OnDnsResponseReceived(IPHeader ipHeader, UDPHeader udpHeader, DnsPacket dnsPacket) {
		QueryState state = null;
		synchronized (mQueryArray) {
			state = mQueryArray.get(dnsPacket.Header.ID);
			if (state != null) {
				mQueryArray.remove(dnsPacket.Header.ID);
			}
		}

		if (state != null) {
			DebugLog.i("Received DNS result form Remote DNS Server");
			if (dnsPacket.Header.QuestionCount > 0 && dnsPacket.Header.ResourceCount > 0) {
				DebugLog.i("Real IP: %s ==> %s", dnsPacket.Questions[0].Domain, CommonMethods.ipIntToString(getFirstIP
						(dnsPacket)));
			}
			//DNS污染，如果在过滤清单里会填充虚假ip
			dnsPollution(udpHeader.mData, dnsPacket);

			//伪造应答packet
			dnsPacket.Header.setID(state.mClientQueryID);
			ipHeader.setSourceIP(state.mRemoteIP);
			ipHeader.setDestinationIP(state.mClientIP);
			ipHeader.setProtocol(IPHeader.UDP);
			// IP头部长度 + UDP头部长度 + DNS报文长度
			udpHeader.setTotalLength(20 + 8 + dnsPacket.Size);
			udpHeader.setSourcePort(state.mRemotePort);
			udpHeader.setDestinationPort(state.mClientPort);
			udpHeader.setTotalLength(8 + dnsPacket.Size);

			//输出到请求发起者
			VpnServiceHelper.sendUDPPacket(ipHeader, udpHeader);
		} else {
			throw new IllegalStateException("can not get state from mQueryArray");
		}
	}

	/**
	 * 从缓冲中获取指定的域名的IP
	 *
	 * @param domain 指定域名
	 * @return 域名的IP地址
	 */
	private int getIPFromCache(String domain) {
		Integer ip = DomainIPMaps.get(domain);
		if (ip == null) {
			return 0;
		} else {
			return ip;
		}
	}

	/**
	 * 对符合过滤条件的域名（如是海外域名或者是gfw上的拦截域名），则直接构建一个提供虚假IP的DNS回复包
	 *
	 * @param ipHeader  ip报文
	 * @param udpHeader udp报文
	 * @param dnsPacket dns报文
	 * @return 构建了一个虚假的DNS回复包给查询客户端则返回true，否则false
	 */
	private boolean interceptDns(IPHeader ipHeader, UDPHeader udpHeader, DnsPacket dnsPacket) {
		Question question = dnsPacket.Questions[0];

		DebugLog.i("DNS query %s", question.Domain);

		if (question.Type == 1) {
			if (ProxyConfig.Instance.filter(question.Domain, getIPFromCache(question.Domain))) {
				int fakeIP = getOrCreateFakeIP(question.Domain);
				tamperDnsResponse(ipHeader.mData, dnsPacket, fakeIP);

				DebugLog.i("interceptDns FakeDns: %s=>%s\n", question.Domain, CommonMethods.ipIntToString(fakeIP));

				int sourceIP = ipHeader.getSourceIP();
				short sourcePort = udpHeader.getSourcePort();
				ipHeader.setSourceIP(ipHeader.getDestinationIP());
				ipHeader.setDestinationIP(sourceIP);
				//IP数据包数据长度 = ip数据报报头长度 + udp报头长度 + DNS报文长度
				ipHeader.setTotalLength(20 + 8 + dnsPacket.Size);
				udpHeader.setSourcePort(udpHeader.getDestinationPort());
				udpHeader.setDestinationPort(sourcePort);
				udpHeader.setTotalLength(8 + dnsPacket.Size);
				VpnServiceHelper.sendUDPPacket(ipHeader, udpHeader);
				return true;
			}
		}
		return false;
	}

	/**
	 * 清楚超时的查询
	 */
	private void clearExpiredQueries() {
		long now = System.nanoTime();
		for (int i = mQueryArray.size() - 1; i >= 0; i--) {
//			QueryState state = mQueryArray.valueAt(i);
			if ((now - System.nanoTime()) > QUERY_TIMEOUT_NS) {
				mQueryArray.removeAt(i);
			}
		}
	}

	/**
	 * 收到APPs的DNS查询包，根据情况转发或者提供一个虚假的DNS回复数据报
	 */
	public void onDnsRequestReceived(IPHeader ipHeader, UDPHeader udpHeader, DnsPacket dnsPacket) {
		if (!interceptDns(ipHeader, udpHeader, dnsPacket)) {
			//转发DNS
			QueryState state = new QueryState();
			state.mClientQueryID = dnsPacket.Header.ID; //标示
			state.mQueryNanoTime = System.nanoTime();
			state.mClientIP = ipHeader.getSourceIP(); //源ip
			state.mClientPort = udpHeader.getSourcePort(); //源端口
			state.mRemoteIP = ipHeader.getDestinationIP(); //目的ip
			state.mRemotePort = udpHeader.getDestinationPort(); //目的端口

			//转换QueryID //TODO 溢出
			mQueryID++;
			dnsPacket.Header.setID(mQueryID);

			synchronized (mQueryArray) {
				clearExpiredQueries(); //清空过期的查询，减少内存消耗
				mQueryArray.put(mQueryID, state);  //保存关联数据
			}

			//应该是DNS服务器的地址和端口
			InetSocketAddress remoteAddress = new InetSocketAddress(CommonMethods.ipIntToInet4Address(state.mRemoteIP),
					state.mRemotePort);
			//只需要把DNS数据报发送过去，不含UDP头部
			DatagramPacket packet = new DatagramPacket(udpHeader.mData, udpHeader.mOffset + 8, dnsPacket.Size);
			packet.setSocketAddress(remoteAddress);

			try {
				//TODO 保护socket不被vpn拦截
				if (VpnServiceHelper.protect(mClient)) {
					//使用DatagramSocket发送DatagramPacket，读取也是用该DatagramSocket
					mClient.send(packet);
					DebugLog.i("Send an DNS Request Package to Remote DNS Server(%s)\n", CommonMethods.ipIntToString
							(state.mRemoteIP));
				} else {
					DebugLog.e("VpnService protect udp socket failed.");
				}
			} catch (IOException e) {
				if (AppDebug.IS_DEBUG) {
					e.printStackTrace(System.err);
				}
				DebugLog.e("Send Dns Request Package catch an exception %s\n", e);
			}
		}
	}

	public boolean isStopped() {
		return mStopped;
	}

	private static class QueryState {
		public short mClientQueryID;
		public long mQueryNanoTime;
		public int mClientIP;
		public short mClientPort;
		public int mRemoteIP;
		public short mRemotePort;
	}
}
