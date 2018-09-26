package com.protect.kid.core.dns;

import com.protect.kid.core.tcpip.CommonMethods;

import java.nio.ByteBuffer;

public class DnsHeader {

	/**
	 * DNS数据包头部
	 * <p/>
	 * 0　　　　　　　　　　　15 16　　　　　　　　　　　　       ３１
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜　　－－
	 * ｜          标识          ｜           标志          ｜　　  ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜     ｜
	 * ｜         问题数         ｜        资源记录数        ｜　　12字节
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜    　｜
	 * ｜　    授权资源记录数     ｜      额外资源记录数       ｜     ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜　　－－
	 */

	private static final short OFFSET_ID = 0;
	private static final short OFFSET_FLAGS = 2;
	private static final short OFFSET_QUESTION_COUNT = 4;
	private static final short OFFSET_RESOURCE_COUNT = 6;
	private static final short OFFSET_A_RESOURCE_COUNT = 8;
	private static final short OFFSET_E_RESOURCE_COUNT = 10;

	public short ID;
	public DnsFlag flags;
	public short questionCount;
	public short resourceCount;
	public short aResourceCount;
	public short eResourceCount;
	private byte[] mData;
	private int mOffset;

	private DnsHeader(byte[] data, int offset) {
		mData = data;
		mOffset = offset;
	}

	public static DnsHeader fromBytes(ByteBuffer buffer) {
		DnsHeader header = new DnsHeader(buffer.array(), buffer.arrayOffset() + buffer.position());
		header.ID = buffer.getShort(); // short 16bit
		header.flags = DnsFlag.parse(buffer.getShort());
		header.questionCount = buffer.getShort(); //问题数
		header.resourceCount = buffer.getShort(); //资源记录数
		header.aResourceCount = buffer.getShort(); //授权资源记录数
		header.eResourceCount = buffer.getShort(); //额外资源记录数
		return header;
	}

	public void toBytes(ByteBuffer buffer) {
		buffer.putShort(this.ID);
		buffer.putShort(this.flags.toShort());
		buffer.putShort(this.questionCount);
		buffer.putShort(this.resourceCount);
		buffer.putShort(this.aResourceCount);
		buffer.putShort(this.eResourceCount);
	}

	public short getID() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_ID);
	}

	public void setID(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_ID, value);
	}

	public short getFlags() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_FLAGS);
	}

	public void setFlags(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_FLAGS, value);
	}

	public short getQuestionCount() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_QUESTION_COUNT);
	}

	public void setQuestionCount(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_QUESTION_COUNT, value);
	}

	public short getResourceCount() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_RESOURCE_COUNT);
	}

	public void setResourceCount(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_RESOURCE_COUNT, value);
	}

	public short getAResourceCount() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_A_RESOURCE_COUNT);
	}

	public void setAResourceCount(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_A_RESOURCE_COUNT, value);
	}

	public short getEResourceCount() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_E_RESOURCE_COUNT);
	}

	public void setEResourceCount(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_E_RESOURCE_COUNT, value);
	}
}
