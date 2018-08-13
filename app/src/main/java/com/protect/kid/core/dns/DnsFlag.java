package com.protect.kid.core.dns;

public class DnsFlag {

	/**
	 * DNS报文的标志（2字节）
	 * <p/>
	 * |-------------------------------------------------|
	 * | qr | opcode  | aa | tc | rd | ra | zero | rcode |
	 * |-------------------------------------------------|
	 * 1       4       1    1    1    1     3       4
	 * <p/>
	 * qr：查询/响应的标志位，1为响应，0为查询
	 * opcode(4比特)：定义查询或响应的类型（若0则表示是标准的，1表示反向，2则是服务器状态请求）
	 * aa(1比特)：授权回答的标志位。该位在响应报文中有效，1表示名字服务器是权限服务器
	 * tc(1比特)：截断标志位。1表示响应已超过512字节并已被截断
	 * rd(1比特)：该位为1表示客户端希望得到递归回答
	 * ra(1比特)：只能在响应报文中置为1，表示可以得到递归响应
	 * zero(3比特)：为0，保留字段
	 * rcode(4比特)：返回码，表示响应的差错状态，通常为0和3，各取值含义如下：
	 * 0       无差错
	 * 1       格式差错
	 * 2       问题在域名服务器上
	 * 3       域参照问题
	 * 4       查询类型不支持
	 * 5       在管理上被禁止
	 * 6-15    保留
	 */

	private boolean qr;
	private int opCode;
	private boolean aa;
	private boolean tc;
	private boolean rd;
	private boolean ra;
	private int zero;
	private int rCode;


	//下面的实现好像与实际的格式不符，不知道是不是SmartProxy作者弄错了，先把原来的注释，按照我理解的格式来写
	public static DnsFlag parse(short value) {
		int flags = value & 0xFFFF;
		DnsFlag flag = new DnsFlag();
//		flag.qr = ((flags >> 7) & 0x01) == 1;
//		flag.opCode = (flags >> 3) & 0x0F;
//		flag.aa = ((flags >> 2) & 0x01) == 1;
//		flag.tc = ((flags >> 1) & 0x01) == 1;
//		flag.rd = (flags & 0x01) == 1;
//		flag.ra = (flags >> 15)  == 1;
//		flag.zero = (flags >> 12) & 0x07;
//		flag.rCode = ((flags >> 8) & 0xF);

		flag.qr = ((flags >> 15) & 0x01) == 1;
		flag.opCode = (flags >> 11) & 0x0F;
		flag.aa = ((flags >> 10) & 0x01) == 1;
		flag.tc = ((flags >> 9) & 0x01) == 1;
		flag.rd = ((flags >> 8) & 0x01) == 1;
		flag.ra = ((flags >> 7) & 0x01) == 1;
		flag.zero = (flags >> 4) & 0x07;
		flag.rCode = flags & 0x0F;
		return flag;
	}

	public short toShort() {
		int flags = 0;
//		flags |= (this.qr ? 1 : 0) << 7;
//		flags |= (this.opCode & 0x0F) << 3;
//		flags |= (this.aa ? 1 : 0) << 2;
//		flags |= (this.tc ? 1 : 0) << 1;
//		flags |= this.rd ? 1 : 0;
//		flags |= (this.ra ? 1 : 0) << 15;
//		flags |= (this.zero & 0x07) << 12;
//		flags |= (this.rCode & 0x0F) << 8;

		flags |= (this.qr ? 1 : 0) << 15;
		flags |= (this.opCode & 0x0F) << 11;
		flags |= (this.aa ? 1 : 0) << 10;
		flags |= (this.tc ? 1 : 0) << 9;
		flags |= (this.rd ? 1 : 0) << 8;
		flags |= (this.ra ? 1 : 0) << 7;
		flags |= (this.zero & 0x07) << 4;
		flags |= this.rCode & 0x0F;
		return (short) flags;
	}
}
