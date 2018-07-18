package com.timedancing.easyfirewall.filter;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.timedancing.easyfirewall.BuildConfig;
import com.timedancing.easyfirewall.cache.AppCache;
import com.timedancing.easyfirewall.event.HostUpdateEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class BlackListHelper {
	private static final boolean DEBUG = BuildConfig.DEBUG;
	private static final String TAG = "BlackListHelper";
	private static final String HOST_URL = "http://dn-mwsl-hosts.qbox.me/hosts.txt";
	private static final String HOSTS_FILE_NAME = "host.txt";

	public static void update(final Context context) {
		if (DEBUG) {
			Log.d(TAG, "update: start to update host");
		}
		OkHttpClient httpClient = new OkHttpClient.Builder()
				.connectTimeout(10000, TimeUnit.MILLISECONDS)
				.readTimeout(20000, TimeUnit.MILLISECONDS)
				.build();
		Request req = new Request.Builder()
				.url(HOST_URL)
				.header(AppCache.KEY_IF_SINCE_MODIFIED_SINCE, AppCache.getIfSinceModifiedSince(context))
				.build();
		EventBus.getDefault().post(new HostUpdateEvent(HostUpdateEvent.Status.Updating));
		httpClient.newCall(req).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				if (DEBUG) {
					Log.w(TAG, "onFailure: update hosts failed", e);
				}
				EventBus.getDefault().post(new HostUpdateEvent(HostUpdateEvent.Status.UpdateFinished));
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (response.isSuccessful()) {
					String lastModified = response.header("Last-Modified");
					if (!TextUtils.isEmpty(lastModified)) {
						AppCache.setIfSinceModifiedSince(context, lastModified);
					}
					ResponseBody body = response.body();
					if (body == null) {
						if (DEBUG) {
							Log.w(TAG, "onResponse: update hosts failed, reason: no body found");
						}
					} else {
						writeHostFile(context, body.string());
						if (DEBUG) {
							Log.d(TAG, "onResponse: succeeded to update host");
						}
					}

				} else {
					if (DEBUG) {
						Log.w(TAG, "onResponse: response is invalid, code = " + response.code());
					}
				}
				EventBus.getDefault().post(new HostUpdateEvent(HostUpdateEvent.Status.UpdateFinished));
			}
		});
	}

	private static void writeHostFile(Context context, String content) {
		File file = null;
		OutputStream outputStream = null;
		BufferedWriter writer = null;
		try {
			try {
				file = getHostsFile(context);
				if (file.exists()) {
					file.delete();
				}
				outputStream = new FileOutputStream(file);
				writer = new BufferedWriter(new OutputStreamWriter(outputStream));
				writer.write(content, 0, content.length());
			} finally {
				if (writer != null) {
					writer.close();
				}

				if (outputStream != null) {
					outputStream.close();
				}
			}
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "writeHostFile: error", e);
			}
		}
	}

	public static File getHostsFile(Context context) {
		File file = new File(context.getFilesDir(), HOSTS_FILE_NAME);
		if (DEBUG) {
			Log.d(TAG, "getHostsFile: host file = " + file.getAbsolutePath());
		}
		return file;
	}

}
