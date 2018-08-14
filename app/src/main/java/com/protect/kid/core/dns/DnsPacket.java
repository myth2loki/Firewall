package com.protect.kid.core.dns;

import java.nio.ByteBuffer;

public class DnsPacket {

	/**
	 * DNS数据报格式
	 * <p/>
	 * 说明一下：并不是所有DNS报文都有以上各个部分的。图中标示的“12字节”为DNS首部，这部分肯定都会有
	 * 首部下面的是正文部分，其中查询问题部分也都会有。
	 * 除此之外，回答、授权和额外信息部分是只出现在DNS应答报文中的，而这三部分又都采用资源记录（Recource Record）的相同格式
	 * ０　　　　　　　　　　　１５　　１６　　　　　　　　　　　　３１
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜　　－－
	 * ｜          标识          ｜           标志           ｜　　  ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜     ｜
	 * ｜         问题数         ｜        资源记录数         ｜　　１２字节
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜    　｜
	 * ｜　    授权资源记录数     ｜      额外资源记录数        ｜     ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜　　－－
	 * ｜　　　　　　　　      查询问题                        ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                      回答                         ｜
	 * ｜　             （资源记录数可变）                    ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                      授权                         ｜
	 * ｜               （资源记录数可变）                    ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 * ｜                  　额外信息                        ｜
	 * ｜               （资源记录数可变）                    ｜
	 * ｜－－－－－－－－－－－－－－－－－－－－－－－－－－－－－｜
	 */

	public DnsHeader header;
	public Question[] questions;
	public Resource[] resources;
	public Resource[] aResources;
	public Resource[] eResources;

	public int size;

	private DnsPacket() {
		//NO OP
	}

	/**
	 * 构造DnsPacket实例
	 * @param buffer 用于构造的数据
	 * @return DnsPacket实例 或者 null如果buffer大小 < 12 or > 512
	 */
	public static DnsPacket fromBytes(ByteBuffer buffer) {
		if (buffer.limit() < 12) {
			return null;
		}
		if (buffer.limit() > 512) {
			return null;
		}

		DnsPacket packet = new DnsPacket();
		packet.size = buffer.limit();
		packet.header = DnsHeader.fromBytes(buffer);

		if (packet.header.questionCount > 2 || packet.header.resourceCount > 50
				|| packet.header.aResourceCount > 50
				|| packet.header.eResourceCount > 50) {
			return null;
		}

		//申请记录问题和资源数的数组空间
		packet.questions = new Question[packet.header.questionCount];
		packet.resources = new Resource[packet.header.resourceCount];
		packet.aResources = new Resource[packet.header.aResourceCount];
		packet.eResources = new Resource[packet.header.eResourceCount];

		for (int i = 0; i < packet.questions.length; i++) {
			packet.questions[i] = Question.fromBytes(buffer);
		}

		for (int i = 0; i < packet.resources.length; i++) {
			packet.resources[i] = Resource.fromBytes(buffer);
		}

		for (int i = 0; i < packet.aResources.length; i++) {
			packet.aResources[i] = Resource.fromBytes(buffer);
		}

		for (int i = 0; i < packet.eResources.length; i++) {
			packet.eResources[i] = Resource.fromBytes(buffer);
		}

		return packet;
	}

	public static String readDomain(ByteBuffer buffer, int dnsHeaderOffset) {
		StringBuilder sb = new StringBuilder();
		int len = 0;
		while (buffer.hasRemaining() && (len = (buffer.get() & 0xFF)) > 0) {
			if ((len & 0xC0) == 0xC0) { //pointer 高2位为11表示是指针。如：1100 0000
				// 指针的取值是前一字节的后6位加后一字节的8位共14的值
				int domainOffset = buffer.get() & 0xFF; //低8位
				domainOffset |= (len & 0x3F) << 8;

				int totalOffset = dnsHeaderOffset + domainOffset;
				ByteBuffer newBuffer = ByteBuffer.wrap(buffer.array(), totalOffset, buffer.limit() - totalOffset);
//				ByteBuffer newBuffer = ByteBuffer.wrap(buffer.array(), dnsHeaderOffset + pointer, buffer.limit() -
//						(dnsHeaderOffset + pointer));
//				sb.append(readDomain(newBuffer, dnsHeaderOffset));
				sb.append(readDomain(newBuffer, 0));
				return sb.toString();
			} else {
				while (len > 0 && buffer.hasRemaining()) {
					sb.append((char) (buffer.get() & 0xFF)); //一个字节一个字节的读取
					len--;
				}
				sb.append(".");
			}
		}

		if (len == 0 && sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1); //去掉末尾的点（.）
		}
		return sb.toString();
	}

	public static void writeDomain(String domain, ByteBuffer buffer) {
		if (domain == null || "".equals(domain.trim())) {
			buffer.put((byte) 0);
			return;
		}

		String[] arr = domain.split("\\.");
		for (String item : arr) {
			if (arr.length > 1) {
				buffer.put((byte) item.length());
			}

			for (int i = 0; i < item.length(); i++) {
				buffer.put((byte) item.codePointAt(i));
			}
		}
	}

	public void toBytes(ByteBuffer buffer) {
		header.questionCount = 0;
		header.resourceCount = 0;
		header.aResourceCount = 0;
		header.eResourceCount = 0;

		if (questions != null) {
			header.questionCount = (short) questions.length;
		}
		if (resources != null) {
			header.resourceCount = (short) resources.length;
		}
		if (aResources != null) {
			header.aResourceCount = (short) aResources.length;
		}
		if (eResources != null) {
			header.eResourceCount = (short) eResources.length;
		}

		this.header.toBytes(buffer);

		for (int i = 0; i < header.questionCount; i++) {
			this.questions[i].toBytes(buffer);
		}

		for (int i = 0; i < header.resourceCount; i++) {
			this.resources[i].toBytes(buffer);
		}

		for (int i = 0; i < header.aResourceCount; i++) {
			this.aResources[i].toBytes(buffer);
		}

		for (int i = 0; i < header.eResourceCount; i++) {
			this.eResources[i].toBytes(buffer);
		}
	}
}
