package com.protect.kid.db.config;

import android.content.Context;

import com.timedancing.easyfirewall.R;
import com.protect.kid.db.bean.Domain;
import com.protect.kid.db.bean.IP;
import com.protect.kid.db.dao.DomainDao;
import com.protect.kid.db.dao.IPDao;
import com.protect.kid.constant.AppDebug;
import com.protect.kid.core.tcpip.CommonMethods;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class BlackListConfig {

	public static void configBlackList(Context context) {
		DomainDao domainDao = DomainDao.get(context);
		IPDao ipDao = IPDao.get(context);
		if (domainDao.hasData() || ipDao.hasData()) {
			InputStream in = context.getResources().openRawResource(R.raw.hosts);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.startsWith("#")) {
						continue;
					}

					String[] parts = line.split(" ");
					if (parts.length == 2) {
						String ipStr = parts[0];
						int ip = CommonMethods.ipStringToInt(ipStr);
						Domain domain = new Domain(parts[1], ip);
						IP ipModel = new IP(ip);
						domainDao.add(domain);
						ipDao.add(ipModel);
					}
				}
			} catch (IOException ex) {
				if (AppDebug.IS_DEBUG) {
					ex.printStackTrace(System.err);
				}
			} finally {
				try {
					reader.close();
					in.close();
				} catch (IOException ex) {
					if (AppDebug.IS_DEBUG) {
						ex.printStackTrace(System.err);
					}
				}
			}
		}
	}

}
