package com.protect.kid.core.tunel;

import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.core.ProxyConfig;
import com.protect.kid.core.filter.Filter;
import com.protect.kid.core.http.HttpResponse;
import com.protect.kid.core.util.VpnServiceHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public abstract class BaseTunnel {
	private static final boolean DEBUG = BuildConfig.DEBUG;
	private static final String TAG = "Tunnel";

	/**
	 * 用于保存数据，因为是单线程所以可以共用一个缓存
	 */
	private final static ByteBuffer GL_BUFFER = ByteBuffer.allocate(2000000);
	public static long sSessionCount;
	private boolean isRemoteTunnel = false;
	private SocketChannel mInnerChannel; //自己的Channel，受保护的，用于真正向外发送和接收数据
	private ByteBuffer mSendRemainBuffer; //发送数据缓存
	private Selector mSelector;
	private HttpResponse mHttpResponse; //http报文
	private boolean isHttpsRequest = false;
	/**
	 * 与外网的通信两个Tunnel负责，一个负责Apps与TCP代理服务器的通信，一个负责TCP代理服务器
	 * 与外网服务器的通信，Apps与外网服务器的数据交换靠这两个Tunnel来进行
	 */
	private BaseTunnel mBrotherTunnel;
	private boolean mDisposed;

	/**
	 * 创建tunnel
	 * @param innerChannel 内部channel
	 * @param selector
	 */
	BaseTunnel(SocketChannel innerChannel, Selector selector) {
		mInnerChannel = innerChannel;
		mSelector = selector;
		incSessionCount();
	}

	/**
	 * 创建tunnel
	 * @param selector
	 * @throws IOException
	 */
	BaseTunnel(Selector selector) throws IOException {
		SocketChannel innerChannel = SocketChannel.open(); //开启SocketChannel
		innerChannel.configureBlocking(false); //设置为非阻塞模式
		this.mInnerChannel = innerChannel; //此channel为真正向外请求的socket
		this.mSelector = selector;
		incSessionCount();
	}

	/**
	 * 增加session count
	 */
	private synchronized static void incSessionCount() {
		++sSessionCount;
	}

	void setRemoteTunnel(boolean enabled) {
		isRemoteTunnel = enabled;
	}

	/**
	 * 方法调用次序：
	 * connect() -> onConnectable() -> onConnected()[子类实现]
	 * beginReceived() ->  onReadable() -> afterReceived()[子类实现]
	 */

	protected abstract void onConnected(ByteBuffer buffer) throws Exception;

	protected abstract boolean isTunnelEstablished();

	protected abstract void beforeSend(ByteBuffer buffer) throws Exception;

	protected abstract void afterReceived(ByteBuffer buffer) throws Exception;

	protected abstract void onDispose();

	private void setBrotherTunnel(BaseTunnel brotherTunnel) {
		this.mBrotherTunnel = brotherTunnel;
	}

	/**
	 * 配对tunnel
	 * @param brotherTunnel
	 */
	public void pair(BaseTunnel brotherTunnel) {
		setBrotherTunnel(brotherTunnel);
		brotherTunnel.setBrotherTunnel(this);
	}

	public void protect() throws IOException {
		if (!VpnServiceHelper.protect(mInnerChannel.socket())) {
			throw new IOException("VPN protect socket failed.");
		}
	}

	protected SocketChannel getInnerChannel() {
		return mInnerChannel;
	}

	public void onConnectable() {
		try {
//			if (mInnerChannel.finishConnect()) {
//				onConnected(GL_BUFFER); //通知子类TCP已连接，子类可以根据协议实现握手等
//				DebugLog.i("Connected to %s", mServerEP);
//			} else {
//				DebugLog.e("Connect to %s failed.", mServerEP);
//				this.dispose();
//			}
			while (!mInnerChannel.finishConnect()) {
				Thread.sleep(500); //如果没有连接成功，则休眠500毫秒
			}
			onConnected(GL_BUFFER); //通知双方注册OP_READ
		} catch (Exception e) {
			if (DEBUG) {
                Log.e(TAG, "onConnectable: Connect to %s failed.", e);
			}
			this.dispose();
		}
	}

	private void beginReceived() throws Exception {
		if (mInnerChannel.isBlocking()) {
			mInnerChannel.configureBlocking(false);
		}
		mInnerChannel.register(mSelector, SelectionKey.OP_READ, this); //注册读事件
	}

	public void onReadable(SelectionKey key) {
		try {
			ByteBuffer buffer = GL_BUFFER;
			buffer.clear();
			int bytesRead = mInnerChannel.read(buffer);
			if (bytesRead > 0) {
				buffer.flip();
				afterReceived(buffer); //先让子类处理，例如解密数据
//				if (BuildConfig.DEBUG) {
//					Log.d(TAG, "onReadable: received tcp " + "remote = " + isRemoteTunnel + ",   " + new IPHeader(buffer.array(), buffer.limit()));
//				}
				if (DEBUG) {
					Log.d(TAG, "onReadable: isRemoteTunnel = " + isRemoteTunnel);
					Log.d(TAG, "onReadable: isHttpsRequest = " + isHttpsRequest);
				}
				if (isRemoteTunnel && !isHttpsRequest) { //外网发过来的数据，需要进行内容过滤
					if (mHttpResponse == null) {
						if (DEBUG) {
							Log.d(TAG, "onReadable: buffer limit = " + buffer.limit() + ", position = " + buffer.position());
//							Log.d(TAG, "onReadable: raw data = " + new String(buffer.array()));
						}
						//TODO 这段目测是内存消耗大户，得想办法降低内存
						if (buffer.limit() - buffer.position() > 5) {
							ByteBuffer httpBuffer = ByteBuffer.wrap(buffer.array(), buffer.position(),
									buffer.limit() - buffer.position());
							int oldPosition = httpBuffer.position();
							byte[] firstFiveBytes = new byte[5];
							httpBuffer.get(firstFiveBytes);
							httpBuffer.position(oldPosition);
							String firstFiveString = new String(firstFiveBytes);
							if (DEBUG) {
								Log.d(TAG, "onReadable: first five bytes = " + firstFiveString);
							}
							if ("HTTP/".equals(firstFiveString)) { //HTTP报文回复
								mHttpResponse = new HttpResponse(isHttpsRequest);
								mHttpResponse.write(httpBuffer);
							}
						}
					} else {
						mHttpResponse.write(buffer);
					}
					if (DEBUG) {
						Log.d(TAG, "onReadable: mHttpResponse = " + mHttpResponse);
					}
					if (mHttpResponse != null) {
						if (DEBUG) {
							Log.d(TAG, "onReadable: isShouldAbandon = " + mHttpResponse.isShouldAbandon() + ", isCompleted = " + mHttpResponse.isCompleted());
						}
						ByteBuffer httpBuffer = null;
						if (mHttpResponse.isShouldAbandon()) { //不过滤
							httpBuffer = mHttpResponse.getBuffer();
						} else if (mHttpResponse.isCompleted()) {  //已经完整地接收了HTTP报文
							String body = mHttpResponse.getBody();
							int result = ProxyConfig.Instance.filterContent(body);
							if (DEBUG) {
								Log.d(TAG, "onReadable: result = " + result);
							}
							switch (result) {
								case Filter.NO_FILTER:
									httpBuffer = mHttpResponse.getBuffer();
									break;
								case Filter.FILTER_LIST:
								case Filter.FILTER_TIME:
									httpBuffer = ProxyConfig.Instance.getBlockingInfo(result);
									break;
								default:
									if (DEBUG) {
										Log.w(TAG, "onReadable: unknwon result = " + result);
									}
									break;
							}
						}
						if (httpBuffer != null) {
							sendToBrother(key, httpBuffer);
							mHttpResponse = null; //节约内存
						}
					} else {
						//回复的报文不是http ~T T~
						sendToBrother(key, buffer);
					}
				} else {
					sendToBrother(key, buffer); //直接转发
				}

			} else if (bytesRead < 0) {
				if (mHttpResponse != null) {
					ByteBuffer httpBuffer = mHttpResponse.getBuffer();
					sendToBrother(key, httpBuffer);
					mHttpResponse = null;
				}
				this.dispose();
			}
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "onReadable: failed", e);
			}
			this.dispose();
		}
	}

	public void onWritable(SelectionKey key) {
		try {
			this.beforeSend(mSendRemainBuffer); //发送之前，先让子类处理，例如做加密等
			if (this.write(mSendRemainBuffer, false)) { //如果剩余数据已经发送完毕
				key.cancel();
				if (isTunnelEstablished()) {
					mBrotherTunnel.beginReceived(); //这边数据发送完毕，通知兄弟可以收数据了
				} else {
					this.beginReceived(); //开始接受代理服务器的响应数据
				}
			}
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "onWritable: failed", e);
			}
			this.dispose();
		}
	}

	/**
	 *
	 * @param key
	 * @param buffer
	 * @throws Exception
	 */
	private void sendToBrother(SelectionKey key, ByteBuffer buffer) throws Exception {
		if (isTunnelEstablished() && buffer.hasRemaining()) { //将读到的数据，转发给兄弟
			mBrotherTunnel.beforeSend(buffer); //发送之前，先让子类处理，例如做加密等。
			if (!mBrotherTunnel.write(buffer, true)) {
				key.cancel(); //写入失败，就取消读取事件
				if (DEBUG) {
					Log.w(TAG, "sendToBrother: cannot write data to " + mBrotherTunnel.mInnerChannel.socket().getInetAddress());
				}
			}
		}
	}

	private boolean write(ByteBuffer buffer, boolean copyRemainData) throws Exception {
		int byteSent;
		while (buffer.hasRemaining()) {
			byteSent = mInnerChannel.write(buffer);
			if (byteSent == 0) {
				break; //不能再发送了，终止循环
			}
		}

		if (buffer.hasRemaining()) { //数据没有发送完毕
			if (copyRemainData) { //拷贝剩余数据，然后侦听写入事件，待可写入时写入
				//拷贝剩余数据
				if (mSendRemainBuffer == null) {
					mSendRemainBuffer = ByteBuffer.allocate(buffer.capacity());
				}
				mSendRemainBuffer.clear();
				mSendRemainBuffer.put(buffer);
				mSendRemainBuffer.flip();
				mInnerChannel.register(mSelector, SelectionKey.OP_WRITE, this); //注册写事件
			}
			return false;
		} else { //发送完毕了
			return true;
		}
	}

	void onTunnelEstablished() throws Exception {
		this.beginReceived(); //开始接收数据
		mBrotherTunnel.beginReceived(); //兄弟也开始接收数据吧
	}

	public void dispose() {
		disposeInternal(true);
	}

	private void disposeInternal(boolean disposeBrother) {
		if (!mDisposed) {
			try {
				mInnerChannel.close();
			} catch (Exception e) {
				if (DEBUG) {
					Log.e(TAG, "disposeInternal: failed", e);
				}
			}

			if (mBrotherTunnel != null && disposeBrother) {
				mBrotherTunnel.disposeInternal(false); //把兄弟的资源也释放了
			}

			mInnerChannel = null;
			mSendRemainBuffer = null;
			mSelector = null;
			mBrotherTunnel = null;
			mHttpResponse = null;
			mDisposed = true;
			sSessionCount--;

			onDispose();
		}
	}

	public void setIsHttpsRequest(boolean isHttpsRequest) {
		this.isHttpsRequest = isHttpsRequest;
	}

	public boolean isHttpsRequest() {
		return isHttpsRequest;
	}

	public void sendBlockInformation(int result) throws IOException {
		ByteBuffer buffer = ProxyConfig.Instance.getBlockingInfo(result);
		mInnerChannel.write(buffer);
	}
}
