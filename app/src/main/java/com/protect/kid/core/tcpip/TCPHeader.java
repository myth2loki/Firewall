package com.protect.kid.core.tcpip;

/**
 * Created by zengzheying on 15/12/28.
 */
public class TCPHeader {


	/**
	 * ＴＣＰ报头格式
	 * ０                                                      １５ １６
	 * ３１
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜               源端口号（ｓｏｕｒｃｅ　ｐｏｒｔ）           　｜       　目的端口号（ｄｅｓｔｉｎａｔｉｏｎ　ｐｏｒｔ）     ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜　　　　　　　　　　　　　　　　　　　　　　　　顺序号（ｓｅｑｕｅｎｃｅ　ｎｕｍｂｅｒ）　　　　　　　　　　　　　　　　　　　　　｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜　　　　　　　　　　　　　　　　　　　　　确认号（ａｃｋｎｏｗｌｅｄｇｅｍｅｎｔ　ｎｕｍｂｅｒ）　　　　　　　　　　　　　　　　　｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜　ＴＣＰ报头　　｜　　保　　            ｜Ｕ｜Ａ｜Ｐ｜Ｒ｜Ｓ｜Ｆ｜                                                     ｜
	 * ｜　　　长度　　　｜　　留　　            ｜Ｒ｜Ｃ｜Ｓ｜Ｓ｜Ｙ｜Ｉ｜　　　　　　窗口大小（ｗｉｎｄｏｗ　ｓｉｚｅ）              ｜
	 * ｜　　（４位）   ｜　（６位）             ｜Ｇ｜Ｋ｜Ｈ｜Ｔ｜Ｎ｜Ｎ｜                                                     ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜              校验和（ｃｈｅｃｋｓｕｍ）                     ｜           紧急指针（ｕｒｇｅｎｔ　ｐｏｉｎｔｅｒ）       ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                                                选项＋填充（０或多个３２位字）                                    　｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                                                   数据（０或多个字节）                                            |
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 **/

	private  static final int FIN = 1;
	private  static final int SYN = 2;
	private  static final int RST = 4;
	private  static final int PSH = 8;
	private  static final int ACK = 16;
	private  static final int URG = 32;

	private static final short OFFSET_SRC_PORT = 0; // 16位源端口
	private static final short OFFSET_DEST_PORT = 2; // 16位目的端口
	private static final int OFFSET_SEQ = 4; //32位序列号
	private static final int OFFSET_ACK = 8; //32位确认号
	private static final byte OFFSET_LENRES = 12; //4位首部长度 + 4位保留位
	private static final byte OFFSET_FLAG = 13; //2位保留字 + 6位标志位
	private static final short OFFSET_WIN = 14; //16位窗口大小
	private static final short OFFSET_CRC = 16; //16位校验和
	private static final short OFFSET_URP = 18; //16位紧急偏移量

	public byte[] mData;
	public int mOffset;

	public TCPHeader(byte[] data, int offset) {
		mData = data;
		mOffset = offset;
	}

	public int getHeaderLength() {
		int lenres = mData[mOffset + OFFSET_LENRES] & 0xFF;
		return (lenres >> 4) * 4;
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

	public byte getFlag() {
		return mData[mOffset + OFFSET_FLAG];
	}

	public short getCrc() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_CRC);
	}

	public void setCrc(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_CRC, value);
	}

	public int getSeqID() {
		return CommonMethods.readInt(mData, mOffset + OFFSET_SEQ);
	}

	public int getAckID() {
		return CommonMethods.readInt(mData, mOffset + OFFSET_ACK);
	}

	@Override
	public String toString() {
		return String.format("%s%s%s%s%s%s %d->%d %s:%s",
				(getFlag() & SYN) == SYN ? "SYN" : "",
				(getFlag() & ACK) == ACK ? "ACK" : "",
				(getFlag() & PSH) == PSH ? "PSH" : "",
				(getFlag() & RST) == RST ? "RST" : "",
				(getFlag() & FIN) == FIN ? "FIN" : "",
				(getFlag() & URG) == URG ? "URG" : "",
				getSourcePort() & 0xFFFF,
				getDestinationPort() & 0xFFFF,
				getSeqID(),
				getAckID());
	}
}
