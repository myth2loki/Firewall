package com.protect.kid.core.proxy;

import android.support.v4.util.LongSparseArray;
import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.core.ProxyConfig;
import com.protect.kid.core.filter.Filter;
import com.protect.kid.core.tcpip.CommonMethods;
import com.protect.kid.core.tcpip.IPHeader;
import com.protect.kid.core.tcpip.UDPHeader;
import com.protect.kid.core.util.VpnServiceUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class UdpProxyServer implements Runnable {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "UdpProxyServer";
    private static final long QUERY_TIMEOUT_NS = 10 * 1000 * 1000 * 1000L;

    private boolean mStop;
    private DatagramSocket mClient;
    private final LongSparseArray<QueryState> mQueryArray = new LongSparseArray<>();

    public UdpProxyServer() throws IOException {
        mClient = new DatagramSocket();
        VpnServiceUtil.protect(mClient);
    }

    public void start() {
        new Thread(this, "UdpProxyServerThread").start();
    }

    public void stop() {
        if (!mStop) {
            mStop = true;
            mClient.close();
        }
    }

    @Override
    public void run() {
        try {
            byte[] buff = new byte[20000];
            IPHeader ipHeader = new IPHeader(buff, 0);
            ipHeader.defaultValue();
            int totalHeaderSize = IPHeader.SIZE + UDPHeader.SIZE;
            UDPHeader udpHeader = new UDPHeader(buff, IPHeader.SIZE);

            ByteBuffer udpBuffer = ByteBuffer.wrap(buff);
            udpBuffer.position(totalHeaderSize);

            //不包含头部信息
            DatagramPacket packet = new DatagramPacket(buff, 28, buff.length - totalHeaderSize);
            //不包含头部信息，减去ip和udp头部长度
            packet.setLength(buff.length - totalHeaderSize);
            while (mClient != null && !mClient.isClosed()) {
                mClient.receive(packet); //获取收到的udp数据报文
                OnUdpResponseReceived(ipHeader, udpHeader, packet);
            }
        } catch (Exception e) {
            if (DEBUG) {
                Log.e(TAG, "run: UdpProxy thread catch an exception", e);
            }
        } finally {
            if (DEBUG) {
                Log.d(TAG, "run: UdpProxy thread exited");
            }
            this.stop();
        }
    }

    /**
     * 收到APPs的DNS查询包，根据情况转发或者提供一个虚假的DNS回复数据报
     */
    public void onUdpRequestReceived(IPHeader ipHeader, UDPHeader udpHeader) {
        int destIp = ipHeader.getDestinationIP();
        int destPort = udpHeader.getDestinationPort();
        if (DEBUG) {
            Log.d(TAG, "onUdpRequestReceived: dest content = " + CommonMethods.ipIntToString(destIp) + ":" + destPort);
        }
        if (filter(destIp, destPort) != Filter.NO_FILTER) {
            if (DEBUG) {
                Log.d(TAG, "onUdpRequestReceived: be filtered, ignore, dest content = " + destIp + ", dest port = " + destPort);
            }
            return;
        }

        QueryState state = new QueryState();
        state.mClientQueryID = ipHeader.getDestinationIP() << 8 | udpHeader.getDestinationPort();; //标示
        state.mQueryNanoTime = System.nanoTime();
        state.mClientIP = ipHeader.getSourceIP(); //源ip
        state.mClientPort = udpHeader.getSourcePort(); //源端口
        state.mRemoteIP = ipHeader.getDestinationIP(); //目的ip
        state.mRemotePort = udpHeader.getDestinationPort(); //目的端口

        clearExpiredQueries();
        mQueryArray.put(state.mClientQueryID, state);

        InetSocketAddress socketAddress = new InetSocketAddress(CommonMethods.ipIntToInet4Address(destIp),
                destPort);
        if (DEBUG) {
            Log.d(TAG, "onUdpRequestReceived: udpHeader = " + udpHeader);
            Log.d(TAG, "onUdpRequestReceived: offset = " + udpHeader.mOffset);
            Log.d(TAG, "onUdpRequestReceived: length = " + udpHeader.getTotalLength());
        }
        DatagramPacket packet = new DatagramPacket(udpHeader.mData, udpHeader.mOffset + 8, udpHeader.getTotalLength() - 8);
        packet.setSocketAddress(socketAddress);
        try {
            //转发请求到外网
            mClient.send(packet);
        } catch (IOException e) {
            if (DEBUG) {
                Log.e(TAG, "onUdpRequestReceived: send packet error.", e);
            }
        }
    }

    private int filter(int ip, int port) {
        return ProxyConfig.Instance.filter(CommonMethods.ipIntToString(ip), ip, port);
    }

    private void OnUdpResponseReceived(IPHeader ipHeader, UDPHeader udpHeader, DatagramPacket packet) {
        int srcIp = ipHeader.getSourceIP();
        int srcPort = udpHeader.getSourcePort();
        if (DEBUG) {
            Log.d(TAG, "OnUdpResponseReceived: srcIp = " + CommonMethods.ipIntToString(srcIp) + ", srcPort = " + srcPort);
        }
        if (filter(srcIp, srcPort) != Filter.NO_FILTER) {
            if (DEBUG) {
                Log.d(TAG, "OnUdpResponseReceived: be filtered, ignore, src content = " + srcIp + ", src port = " + srcPort);
            }
            return;
        }

        QueryState state;
        synchronized (mQueryArray) {
            //取出缓存的DNS信息
            long id = ipHeader.getDestinationIP() << 8 | udpHeader.getDestinationPort();
            state = mQueryArray.get(id);
            if (state != null) {
                mQueryArray.remove(id);
            }
        }

        //如果是自己发出的UDP的回执，则改造
        if (state != null) {
            ipHeader.setSourceIP(state.mRemoteIP);
            ipHeader.setDestinationIP(state.mClientIP);
            ipHeader.setProtocol(IPHeader.UDP);
            ipHeader.setTotalLength(20 + 8 + packet.getLength());
            // IP头部长度 + UDP头部长度 + DNS报文长度
            udpHeader.setTotalLength(8 + packet.getLength());
            udpHeader.setSourcePort(state.mRemotePort);
            udpHeader.setDestinationPort(state.mClientPort);
        }
        //输出到请求发起者
        VpnServiceUtil.sendUDPPacket(ipHeader, udpHeader);
    }

    /**
     * 清除超时的查询
     */
    private void clearExpiredQueries() {
        long now = System.nanoTime();
        for (int i = mQueryArray.size() - 1; i >= 0; i--) {
            QueryState state = mQueryArray.valueAt(i);
            if ((now - state.mQueryNanoTime) > QUERY_TIMEOUT_NS) {
                mQueryArray.removeAt(i);
            }
        }
    }

    private static class QueryState {
        long mClientQueryID;
        long mQueryNanoTime;
        int mClientIP;
        short mClientPort;
        int mRemoteIP;
        short mRemotePort;
    }
}
