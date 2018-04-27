package com.timedancing.easyfirewall.core.proxy;

import com.timedancing.easyfirewall.core.tcpip.IPHeader;
import com.timedancing.easyfirewall.core.tcpip.UDPHeader;
import com.timedancing.easyfirewall.core.util.VpnServiceHelper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
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
                SelectionKey.OP_READ | SelectionKey.OP_WRITE, mServerSocketChannel);
        mPort = (short) mServerSocketChannel.socket().getLocalPort();


        VpnServiceHelper.protect(mServerSocketChannel.socket());

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
                            int length = channel.read(byteBuff);
                            while (length > -1) {
                                byteBuff.limit(length);

                            }
                            DatagramPacket packet = new DatagramPacket(byteBuff.array(), byteBuff.limit());
                            //TODO 需要一个tunnel
                        } else if (key.isWritable()) {

                        }
                    }
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
