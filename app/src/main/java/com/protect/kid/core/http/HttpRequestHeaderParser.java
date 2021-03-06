package com.protect.kid.core.http;

import android.text.TextUtils;
import android.util.Log;

import com.protect.kid.BuildConfig;
import com.protect.kid.core.nat.NatSession;
import com.protect.kid.core.tcpip.CommonMethods;

import java.util.Locale;

public class HttpRequestHeaderParser {
	private static final String TAG = "HttpRequestHeaderParser";
	private static final boolean DEBUG = BuildConfig.DEBUG;

	public static void parseHttpRequestHeader(NatSession session, byte[] buffer, int offset, int count) {
		try {
			switch (buffer[offset]) {
				case 'G': //GET
				case 'H': //HEAD
				case 'P': //POST, PUT
				case 'D': //DELETE
				case 'O': //OPTIONS
				case 'T': //TRACE
				case 'C': //CONNECT
					getHttpHostAndRequestUrl(session, buffer, offset, count);
					break;
				case 0x16: //SSL
					session.remoteHost = getSNI(session, buffer, offset, count);
					if (DEBUG) {
						Log.d(TAG, "parseHttpRequestHeader: sni host = " + session.remoteHost);
					}
					break;
			}
		} catch (Exception ex) {
			if (DEBUG) {
				Log.e(TAG, "parseHttpRequestHeader: error: parseHost", ex);
			}
		}
	}

	/**
	 * 解析http host, 请求url和method
	 * @param session
	 * @param buffer
	 * @param offset
	 * @param count
	 */
	private static void getHttpHostAndRequestUrl(NatSession session, byte[] buffer, int offset, int count) {
		session.isHttpsSession = false;
		String headerString = new String(buffer, offset, count);
		String[] headerLines = headerString.split("\\r\\n");
		String host = getHttpHost(headerLines);
		if (DEBUG) {
			Log.d(TAG, "getHttpHostAndRequestUrl: host = " + host);
		}
		if (!TextUtils.isEmpty(host)) {
			session.remoteHost = host;
		}
		paresRequestLine(session, headerLines[0]);
	}

	/**
	 * 解析http host
	 * @param headerLines
	 * @return
	 */
	private static String getHttpHost(String[] headerLines) {
		String requestLine = headerLines[0];
		if (requestLine.startsWith("GET") || requestLine.startsWith("POST") || requestLine.startsWith("HEAD")
				|| requestLine.startsWith("OPTIONS")) {
			for (int i = 1; i < headerLines.length; i++) {
				String[] nameValueStrings = headerLines[i].split(":");
				if (nameValueStrings.length == 2) {
					String name = nameValueStrings[0].toLowerCase(Locale.ENGLISH).trim();
					String value = nameValueStrings[1].trim();
					if ("host".equals(name)) {
						return value;
					}
				}
			}
		}
		return null;
	}

	/**
	 * 解析method，请求url
	 * @param session
	 * @param requestLine
	 */
	private static void paresRequestLine(NatSession session, String requestLine) {
		String[] parts = requestLine.trim().split(" ");
		if (parts.length == 3) {
			session.method = parts[0];
			String url = parts[1];
			if (url.startsWith("/")) {
				session.requestUrl = session.remoteHost + url;
			} else {
				session.requestUrl = url;
			}
		}
	}

	//offset为tcp包的开始位置 https://blog.csdn.net/makenothing/article/details/53292335
	//https://blog.csdn.net/hpp205/article/details/48995235
	private static String getSNI(NatSession session, byte[] buffer, int offset, int count) {
		int limit = offset + count;
		/**
		 0x14	20	ChangeCipherSpec
		 0x15	21	Alert
		 0x16	22	Handshake
		 0x17	23	Application
		 0x18	24	Heartbeat
		 */
		if (count > 43 && buffer[offset] == 0x16) { //TLS Client Hello
			offset += 43; //Skip 43 byte header

			//read sessionID
			if (offset + 1 > limit) {
				return null;
			}
			int sessionIDLength = buffer[offset++] & 0xFF;
			offset += sessionIDLength;

			//read cipher suites
			if (offset + 2 > limit) {
				return null;
			}

			//读取16个bit
			int cipherSuitesLength = CommonMethods.readShort(buffer, offset) & 0xFFFF;
			offset += 2;
			offset += cipherSuitesLength;

			//read Compression method.
			if (offset + 1 > limit) {
				return null;
			}
			int compressionMethodLength = buffer[offset++] & 0xFF;
			offset += compressionMethodLength;
			if (offset == limit) {
				if (DEBUG) {
					Log.w(TAG, "getSNI: no SNI found in TLS Client Hello packet");
				}
				return null;
			}

			//read Extensions
			if (offset + 2 > limit) {
				return null;
			}
			int extensionsLength = CommonMethods.readShort(buffer, offset) & 0xFFFF;
			offset += 2;

			if (offset + extensionsLength > limit) {
				if (DEBUG) {
					Log.w(TAG, "getSNI: TLS Client Hello packet is incomplete.");
				}
				return null;
			}

			while (offset + 4 <= limit) {
				int type0 = buffer[offset++] & 0xFF;
				int type1 = buffer[offset++] & 0xFF;
				int length = CommonMethods.readShort(buffer, offset) & 0xFFFF;
				offset += 2;

				if (type0 == 0x00 && type1 == 0x00 && length > 5) { //have SNI
					offset += 5;
					length -= 5;
					if (offset + length > limit) {
						return null;
					}
					String serverName = new String(buffer, offset, length);
					if (DEBUG) {
						Log.d(TAG, "getSNI: " + serverName);
					}
					session.isHttpsSession = true;
					return serverName;
				} else {
					offset += length;
				}
			}
			if (DEBUG) {
				Log.e(TAG, "getSNI: TLS Client Hello packet has no Host info.");
			}
			return null;
		} else {
			if (DEBUG) {
				Log.e(TAG, "getSNI: Incorrect TLS Client Hello packet.");
			}
			return null;
		}
	}

}
