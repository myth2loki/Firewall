package com.protect.kid.core.dns;

import com.protect.kid.core.tcpip.CommonMethods;

import java.nio.ByteBuffer;

public class DnsHeader {

	/**
	 * DNS数据包头部
	 * <p/>
	 * ０　　　　　　　　　　　１５　　１６　　　　　　　　　　　　３１
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜　　－－
	 * ｜          标识          ｜           标志          ｜　　  ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜     ｜
	 * ｜         问题数         ｜        资源记录数        ｜　　１２字节
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜    　｜
	 * ｜　    授权资源记录数     ｜      额外资源记录数       ｜     ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜　　－－
	 */

	private static final short offset_ID = 0;
	private static final short offset_Flags = 2;
	private static final short offset_QuestionCount = 4;
	private static final short offset_ResourceCount = 6;
	private static final short offset_AResourceCount = 8;
	private static final short offset_EResourceCount = 10;

	public short ID;
	public DnsFlag flags;
	public short QuestionCount;
	public short ResourceCount;
	public short AResourceCount;
	public short EResourceCount;
	public byte[] mData;
	public int mOffset;

	private DnsHeader(byte[] data, int offset) {
		mData = data;
		mOffset = offset;
	}

	public static DnsHeader fromBytes(ByteBuffer buffer) {
		DnsHeader header = new DnsHeader(buffer.array(), buffer.arrayOffset() + buffer.position());
		header.ID = buffer.getShort(); // short 16bit
		header.flags = DnsFlag.parse(buffer.getShort());
		header.QuestionCount = buffer.getShort(); //问题数
		header.ResourceCount = buffer.getShort(); //资源记录数
		header.AResourceCount = buffer.getShort(); //授权资源记录数
		header.EResourceCount = buffer.getShort(); //额外资源记录数
		return header;
	}

	public void toBytes(ByteBuffer buffer) {
		buffer.putShort(this.ID);
		buffer.putShort(this.flags.toShort());
		buffer.putShort(this.QuestionCount);
		buffer.putShort(this.ResourceCount);
		buffer.putShort(this.AResourceCount);
		buffer.putShort(this.EResourceCount);
	}

	public short getID() {
		return CommonMethods.readShort(mData, mOffset + offset_ID);
	}

	public void setID(short value) {
		CommonMethods.writeShort(mData, mOffset + offset_ID, value);
	}

	public short getFlags() {
		return CommonMethods.readShort(mData, mOffset + offset_Flags);
	}

	public void setFlags(short value) {
		CommonMethods.writeShort(mData, mOffset + offset_Flags, value);
	}

	public short getQuestionCount() {
		return CommonMethods.readShort(mData, mOffset + offset_QuestionCount);
	}

	public void setQuestionCount(short value) {
		CommonMethods.writeShort(mData, mOffset + offset_QuestionCount, value);
	}

	public short getResourceCount() {
		return CommonMethods.readShort(mData, mOffset + offset_ResourceCount);
	}

	public void setResourceCount(short value) {
		CommonMethods.writeShort(mData, mOffset + offset_ResourceCount, value);
	}

	public short getAResourceCount() {
		return CommonMethods.readShort(mData, mOffset + offset_AResourceCount);
	}

	public void setAResourceCount(short value) {
		CommonMethods.writeShort(mData, mOffset + offset_AResourceCount, value);
	}

	public short getEResourceCount() {
		return CommonMethods.readShort(mData, mOffset + offset_EResourceCount);
	}

	public void setEResourceCount(short value) {
		CommonMethods.writeShort(mData, mOffset + offset_EResourceCount, value);
	}
}
