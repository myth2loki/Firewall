package com.protect.kid.core.tcpip;

public class UDPHeader {
	/**
	 * UDP数据报格式
	 * 头部长度：8字节
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜  １６位源端口号         ｜   １６位目的端口号        ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜  １６位ＵＤＰ长度       ｜   １６位ＵＤＰ检验和       ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                  数据（如果有）                    ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 **/

	private static final short OFFSET_SRC_PORT = 0; // 源端口
	private static final short OFFSET_DEST_PORT = 2; //目的端口
	private static final short OFFSET_TLEN = 4; //数据报长度
	private static final short OFFSET_CRC = 6; //校验和
    public static final int SIZE = 8;

    public byte[] mData;
	public int mOffset;

	public UDPHeader(byte[] data, int offset) {
		mData = data;
		mOffset = offset;
	}

	public short getSourcePort() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_SRC_PORT);
	}

	public void setSourcePort(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_SRC_PORT, value);
	}

	public short getDestinationPort() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_DEST_PORT);
	}

	public void setDestinationPort(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_DEST_PORT, value);
	}

	public int getTotalLength() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_TLEN) & 0xFFFF;
	}

	public void setTotalLength(int value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_TLEN, (short) value);
	}

	public short getCrc() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_CRC);
	}

	public void setCrc(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_CRC, value);
	}

	@Override
	public String toString() {
		return String.format("%d->%d", getSourcePort() & 0xFFFF, getDestinationPort() & 0xFFFF);
	}
}
