package com.protect.kid.core.proxy;

import android.util.Log;

import com.timedancing.easyfirewall.BuildConfig;
import com.protect.kid.constant.AppDebug;
import com.protect.kid.core.tcpip.CommonMethods;
import com.protect.kid.core.tcpip.IPHeader;
import com.protect.kid.core.tcpip.UDPHeader;
import com.protect.kid.core.util.VpnServiceHelper;
import com.protect.kid.util.DebugLog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class UdpProxyServer implements Runnable {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String TAG = "UdpProxyServer";

    private boolean mStop;
    private DatagramSocket mClient;

    public UdpProxyServer() throws IOException {
        mClient = new DatagramSocket(0);
        VpnServiceHelper.protect(mClient);
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
            int ipHeaderLength = 20;
            UDPHeader udpHeader = new UDPHeader(buff, ipHeaderLength);

            int udpHeaderLenght = 8;
            ByteBuffer udpBuffer = ByteBuffer.wrap(buff);
            udpBuffer.position(ipHeaderLength + udpHeaderLenght);
            udpBuffer = udpBuffer.slice(); //去除ip和udp头部

            //不包含头部信息
            DatagramPacket packet = new DatagramPacket(buff, 28, buff.length - (ipHeaderLength +
                    udpHeaderLenght));
            //不包含头部信息，减去ip和udp头部长度
            packet.setLength(buff.length - (ipHeaderLength + udpHeaderLenght));
            while (mClient != null && !mClient.isClosed()) {
                mClient.receive(packet); //获取说道的udp数据报文

//                udpBuffer.clear();
//                udpBuffer.limit(packet.getLength()); //设置dnsBuffer的长度
                OnUdpResponseReceived(ipHeader, udpHeader);
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
     * 收到APPs的DNS查询包，根据情况转发或者提供一个虚假的DNS回复数据报
     */
    public void onUdpRequestReceived(IPHeader ipHeader, UDPHeader udpHeader) {
        int destIp = ipHeader.getDestinationIP();
        int destPort = udpHeader.getDestinationPort();
        if (DEBUG) {
            Log.d(TAG, "onUdpRequestReceived: dest content = " + CommonMethods.ipIntToString(destIp) + ":" + destPort);
        }
        if (filter(destIp, destPort)) {
            if (DEBUG) {
                Log.d(TAG, "onUdpRequestReceived: be filtered, ignore, dest content = " + destIp + ", dest port = " + destPort);
            }
            return;
        }
        InetSocketAddress socketAddress = new InetSocketAddress(CommonMethods.ipIntToInet4Address(destIp),
                destPort);
        if (DEBUG) {
            Log.d(TAG, "onUdpRequestReceived: udpHeader = " + udpHeader);
            Log.d(TAG, "onUdpRequestReceived: offset = " + udpHeader.mOffset);
            Log.d(TAG, "onUdpRequestReceived: length = " + udpHeader.mData.length);
            Log.d(TAG, "onUdpRequestReceived: total length = " + udpHeader.getTotalLength());
        }
        DatagramPacket packet = new DatagramPacket(udpHeader.mData, udpHeader.mOffset + 8, udpHeader.getTotalLength() - 8);
        packet.setSocketAddress(socketAddress);
        try {
            mClient.send(packet);
        } catch (IOException e) {
            if (DEBUG) {
                Log.e(TAG, "onUdpRequestReceived: send packet error.", e);
            }
        }
    }

    private boolean filter(int ip, int port) {

        return false;
    }

    public void OnUdpResponseReceived(IPHeader ipHeader, UDPHeader udpHeader) {
        int srcIp = ipHeader.getSourceIP();
        int srcPort = udpHeader.getSourcePort();
        if (DEBUG) {
            Log.d(TAG, "OnUdpResponseReceived: srcIp = " + CommonMethods.ipIntToString(srcIp) + ", srcPort = " + srcPort);
        }
        if (filter(srcIp, srcPort)) {
            if (DEBUG) {
                Log.d(TAG, "OnUdpResponseReceived: be filtered, ignore, src content = " + srcIp + ", src port = " + srcPort);
            }
            return;
        }
        //输出到请求发起者
        VpnServiceHelper.sendUDPPacket(ipHeader, udpHeader);
    }
}
