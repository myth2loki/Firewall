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

	public int Size;

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
		packet.Size = buffer.limit();
		packet.header = DnsHeader.fromBytes(buffer);

		if (packet.header.QuestionCount > 2 || packet.header.ResourceCount > 50
				|| packet.header.AResourceCount > 50
				|| packet.header.EResourceCount > 50) {
			return null;
		}

		//申请记录问题和资源数的数组空间
		packet.questions = new Question[packet.header.QuestionCount];
		packet.resources = new Resource[packet.header.ResourceCount];
		packet.aResources = new Resource[packet.header.AResourceCount];
		packet.eResources = new Resource[packet.header.EResourceCount];

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
				int pointer = buffer.get() & 0xFF; //低8位
				pointer |= (len & 0x3F) << 8;

				ByteBuffer newBuffer = ByteBuffer.wrap(buffer.array(), dnsHeaderOffset + pointer, dnsHeaderOffset +
						buffer.limit());
//				ByteBuffer newBuffer = ByteBuffer.wrap(buffer.array(), dnsHeaderOffset + pointer, buffer.limit() -
//						(dnsHeaderOffset + pointer));
				sb.append(readDomain(newBuffer, dnsHeaderOffset));
				return sb.toString();
			} else {
				while (len > 0 && buffer.hasRemaining()) {
					sb.append((char) (buffer.get() & 0xFF));
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
		header.QuestionCount = 0;
		header.ResourceCount = 0;
		header.AResourceCount = 0;
		header.EResourceCount = 0;

		if (questions != null) {
			header.QuestionCount = (short) questions.length;
		}
		if (resources != null) {
			header.ResourceCount = (short) resources.length;
		}
		if (aResources != null) {
			header.AResourceCount = (short) aResources.length;
		}
		if (eResources != null) {
			header.EResourceCount = (short) eResources.length;
		}

		this.header.toBytes(buffer);

		for (int i = 0; i < header.QuestionCount; i++) {
			this.questions[i].toBytes(buffer);
		}

		for (int i = 0; i < header.ResourceCount; i++) {
			this.resources[i].toBytes(buffer);
		}

		for (int i = 0; i < header.AResourceCount; i++) {
			this.aResources[i].toBytes(buffer);
		}

		for (int i = 0; i < header.EResourceCount; i++) {
			this.eResources[i].toBytes(buffer);
		}
	}
}
