package com.protect.kid.core.tcpip;

/**
 * Created by zengzheying on 15/12/28.
 */
public class IPHeader {

	/**
	 * IP报文格式
	 * 0                                   　　　　       15  16　　　　　　　　　　　　　　　　　　　　　　　　   31
	 * ｜　－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜  ４　位     ｜   ４位首     ｜      ８位服务类型      ｜      　　         １６位总长度            　   ｜
	 * ｜  版本号     ｜   部长度     ｜      （ＴＯＳ）　      ｜      　 　 （ｔｏｔａｌ　ｌｅｎｇｔｈ）    　    ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜  　　　　　　　　１６位标识符                         ｜　３位    ｜　　　　１３位片偏移                 ｜
	 * ｜            （ｉｎｄｅｎｔｉｆｉｅｒ）                 ｜　标志    ｜      （ｏｆｆｓｅｔ）　　           ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜      ８位生存时间ＴＴＬ      ｜       ８位协议        ｜　　　　　　　　１６位首部校验和                  ｜
	 * ｜（ｔｉｍｅ　ｔｏ　ｌｉｖｅ）　　｜   （ｐｒｏｔｏｃｏｌ） ｜              （ｃｈｅｃｋｓｕｍ）               ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                              ３２位源ＩＰ地址（ｓｏｕｒｃｅ　ａｄｄｒｅｓｓ）                           ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                         ３２位目的ＩＰ地址（ｄｅｓｔｉｎａｔｉｏｎ　ａｄｄｒｅｓｓ）                     ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                                          ３２位选项（若有）                                        ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                                                                                                  ｜
	 * ｜                                               数据                                               ｜
	 * ｜                                                                                                  ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 **/

	public static final short IP = 0x0800;
	public static final byte ICMP = 1;
	public static final byte TCP = 6;  //6: TCP协议号
	public static final byte UDP = 17; //17: UDP协议号
	public static final byte OFFSET_PROTO = 9; //9：8位协议偏移
	public static final int OFFSET_SRC_IP = 12; //12：源ip地址偏移
	public static final int OFFSET_DEST_IP = 16; //16：目标ip地址偏移
	static final byte OFFSET_VER_IHL = 0; //0: 版本号（4bits） + 首部长度（4bits）
	static final byte OFFSET_TOS = 1; //1：服务类型偏移
	static final short OFFSET_TLEN = 2; //2：总长度偏移
	static final short OFFSET_IDENTIFICATION = 4; //4：16位标识符偏移
	static final short OFFSET_FLAGS_FO = 6; //6：标志（3bits）+ 片偏移（13bits）
	static final byte OFFSET_TTL = 8; //8：生存时间偏移
	static final short OFFSET_CRC = 10; //10：首部校验和偏移
	static final int OFFSET_OP_PAD = 20; //20：选项 + 填充
	public static final int SIZE = 20;

	public byte[] mData;
	public int mOffset;

	public IPHeader(byte[] data, int offset) {
		mData = data;
		mOffset = offset;
	}

	/**
	 * 写入默认值
	 */
	public void defaultValue() {
		setHeaderLength(20);
		setTos((byte) 0);
		setTotalLength(0);
		setIdentification(0);
		setFlagsAndOffset((short) 0);
		setTTL((byte) 64);
	}

	public int getDataLength() {
		return this.getTotalLength() - this.getHeaderLength();
	}

	public int getHeaderLength() {
		return (mData[mOffset + OFFSET_VER_IHL] & 0x0F) * 4;
	}

	public void setHeaderLength(int value) {
		// 4 << 4 表示版本为IPv4
		mData[mOffset + OFFSET_VER_IHL] = (byte) ((4 << 4) | (value / 4));
	}

	public byte getTos() {
		return mData[mOffset + OFFSET_TOS];
	}

	public void setTos(byte value) {
		mData[mOffset + OFFSET_TOS] = value;
	}

	public int getTotalLength() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_TLEN) & 0xFFFF;
	}

	public void setTotalLength(int value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_TLEN, (short) value);
	}

	public int getIdentification() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_IDENTIFICATION) & 0xFFFF;
	}

	public void setIdentification(int value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_IDENTIFICATION, (short) value);
	}

	public short getFlagsAndOffset() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_FLAGS_FO);
	}

	public void setFlagsAndOffset(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_FLAGS_FO, value);
	}

	public byte getTTL() {
		return mData[mOffset + OFFSET_TTL];
	}

	public void setTTL(byte value) {
		mData[mOffset + OFFSET_TTL] = value;
	}

	public byte getProtocol() {
		return mData[mOffset + OFFSET_PROTO];
	}

	public void setProtocol(byte value) {
		mData[mOffset + OFFSET_PROTO] = value;
	}

	public short getCrc() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_CRC);
	}

	public void setCrc(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_CRC, value);
	}

	public int getSourceIP() {
		return CommonMethods.readInt(mData, mOffset + OFFSET_SRC_IP);
	}

	public void setSourceIP(int value) {
		CommonMethods.writeInt(mData, mOffset + OFFSET_SRC_IP, value);
	}

	public int getDestinationIP() {
		return CommonMethods.readInt(mData, mOffset + OFFSET_DEST_IP);
	}

	public void setDestinationIP(int value) {
		CommonMethods.writeInt(mData, mOffset + OFFSET_DEST_IP, value);
	}

	@Override
	public String toString() {
		return String.format("%s->%s Pro=%s, HLen=%d", CommonMethods.ipIntToString(getSourceIP()),
				CommonMethods.ipIntToString(getDestinationIP()), getProtocol(), getHeaderLength());
	}
}
