package com.timedancing.easyfirewall.core.proxy;

import android.util.Log;

import com.timedancing.easyfirewall.BuildConfig;
import com.timedancing.easyfirewall.core.tcpip.IPHeader;
import com.timedancing.easyfirewall.core.tcpip.UDPHeader;
import com.timedancing.easyfirewall.core.util.VpnServiceHelper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class UdpProxyServer implements Runnable {
    private static final String TAG = "UdpProxyServer";

    private boolean mStop;
    private Selector mSelector;
    private DatagramChannel mServerSocketChannel;
//    private DatagramChannel mInnerServerSocketChannel;
    private short mPort;

    public UdpProxyServer(int port) throws IOException {
        mSelector = Selector.open();
        mServerSocketChannel = DatagramChannel.open();
        mServerSocketChannel.configureBlocking(false);
        mServerSocketChannel.socket().bind(new InetSocketAddress(port));
        mServerSocketChannel.register(mSelector,
//                SelectionKey.OP_CONNECT |
                        SelectionKey.OP_READ, mServerSocketChannel);
        mPort = (short) mServerSocketChannel.socket().getLocalPort();

//        VpnServiceHelper.protect(mServerSocketChannel.socket());

    }

    public void start() {
        mStop = true;
        new Thread(this, "UdpProxyServerThread").start();
    }

    public void stop() {
        mStop = false;
    }

    @Override
    public void run() {
        while (mStop) {
            try {
                mSelector.select();
                Iterator<SelectionKey> iter = mSelector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    DatagramChannel channel = (DatagramChannel) key.channel();
                    if (key.isValid()) {
                        if (key.isReadable()) { //有udp报文
                            ByteBuffer byteBuff = ByteBuffer.allocate(20000);
                            SocketAddress sa = channel.receive(byteBuff);
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "run: sa = " + sa);
                            }
                            DatagramPacket packet = new DatagramPacket(byteBuff.array(), byteBuff.limit());
                            InetAddress senderAddress = packet.getAddress();
//                            SocketAddress hostAddress = packet.getSocketAddress();
                            //TODO 需要一个tunnel
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "run: udp header read " + new UDPHeader(byteBuff.array(), 0));
                                Log.d(TAG, "run: udp content read " + new String(packet.getData(), 0, packet.getLength()));
//                                Log.d(TAG, "run: udp packet " + packet);
                            }
                        } else if (key.isWritable()) {
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "run: udp header write ");
                            }
                        } else if (key.isConnectable()) {

                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "run: udp header write ");
                            }
                        }
                    }
                    iter.remove();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取端口
     * @return
     */
    public short getPort() {
        return mPort;
    }
}
