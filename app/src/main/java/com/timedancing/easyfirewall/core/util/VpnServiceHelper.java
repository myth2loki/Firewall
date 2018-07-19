package com.timedancing.easyfirewall.core.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.timedancing.easyfirewall.core.nat.NatSessionManager;
import com.timedancing.easyfirewall.core.service.FirewallVpnService;
import com.timedancing.easyfirewall.core.tcpip.IPHeader;
import com.timedancing.easyfirewall.core.tcpip.UDPHeader;

import java.net.DatagramSocket;
import java.net.Socket;

/**
 * Created by zengzheying on 16/1/12.
 */
public class VpnServiceHelper {

	public static final int START_VPN_SERVICE_REQUEST_CODE = 2015;
	private static FirewallVpnService sVpnService;

	public static void onVpnServiceCreated(FirewallVpnService vpnService) {
		sVpnService = vpnService;
	}

	public static void onVpnServiceDestroy() {
		sVpnService = null;
	}

	public static void sendUDPPacket(IPHeader ipHeader, UDPHeader udpHeader) {
		if (sVpnService != null) {
			sVpnService.sendUDPPacket(ipHeader, udpHeader);
		}
	}

	public static boolean protect(Socket socket) {
		if (sVpnService != null) {
			return sVpnService.protect(socket);
		}
		return false;
	}

	public static boolean protect(DatagramSocket socket) {
		if (sVpnService != null) {
			return sVpnService.protect(socket);
		}
		return false;
	}

	public static boolean vpnRunningStatus() {
		if (sVpnService != null) {
			return sVpnService.vpnRunningStatus();
		}
		return false;
	}

	public static void changeVpnRunningStatus(Context context, boolean isStart) {
		if (context == null) {
			return;
		}
		if (isStart) {
			Intent intent = FirewallVpnService.prepare(context);
			if (intent == null) {
				startVpnService(context);
			} else {
				if (context instanceof Activity) {
					((Activity) context).startActivityForResult(intent, START_VPN_SERVICE_REQUEST_CODE);
				}
			}
		} else {
			boolean stopStatus = false;
			if (sVpnService != null) {
				sVpnService.setVpnRunningStatus(stopStatus);
			}
		}
	}

	public static void restartVpnService(final Context context, final Runnable run) {
		VpnServiceHelper.changeVpnRunningStatus(context, false);
		new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
				NatSessionManager.clearAllSessions();
				VpnServiceHelper.changeVpnRunningStatus(context, true);
				if (run != null) {
					run.run();
				}
			}
		}, 1000);
	}

	public static void startVpnService(Context context) {
		if (context == null) {
			return;
		}

		context.startService(new Intent(context, FirewallVpnService.class));
	}
}
