package com.protect.kid.core.dns;

import com.protect.kid.core.tcpip.CommonMethods;

public class ResourcePointer {

	private static final short OFFSET_DOMAIN = 0;
	private static final short OFFSET_TYPE = 2;
	private static final short OFFSET_CLASS = 4;
	private static final int OFFSET_TTL = 6;
	private static final int OFFSET_DATA_LENGTH = 10;
	private static final int OFFSET_IP = 12;

	private byte[] mData;
	private int mOffset;

	public ResourcePointer(byte[] data, int offset) {
		mData = data;
		mOffset = offset;
	}

	public void setDomain(short value) {
		CommonMethods.writeInt(mData, mOffset + OFFSET_DOMAIN, value);
	}

	public short getType() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_TYPE);
	}

	public void setType(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_TYPE, value);
	}

	public short getClass(short value) {
		return CommonMethods.readShort(mData, mOffset + OFFSET_CLASS);
	}

	public void setClass(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_CLASS, value);
	}

	public int getTTL() {
		return CommonMethods.readInt(mData, mOffset + OFFSET_TTL);
	}

	public void setTTL(int value) {
		CommonMethods.writeInt(mData, mOffset + OFFSET_TTL, value);
	}

	public short getDataLength() {
		return CommonMethods.readShort(mData, mOffset + OFFSET_DATA_LENGTH);
	}

	public void setDataLength(short value) {
		CommonMethods.writeShort(mData, mOffset + OFFSET_DATA_LENGTH, value);
	}

	public int getIP() {
		return CommonMethods.readInt(mData, mOffset + OFFSET_IP);
	}

	public void setIP(int value) {
		CommonMethods.writeInt(mData, mOffset + OFFSET_IP, value);
	}
}
