package com.protect.kid.util;

import android.support.annotation.Nullable;
import android.util.Log;

import com.timedancing.easyfirewall.BuildConfig;

/**
 * Created by zengzheying on 15/12/31.
 */
public class CompressorFactory {
	private static final String TAG = "CompressorFactory";
	private static final boolean DEBUG = BuildConfig.DEBUG;
	public static final String METHOD_GZIP = "gzip";

	@Nullable
	public static Compressor getCompressor(String method) {
		if (DEBUG) {
			Log.d(TAG, "getCompressor: method = " + method);
		}
		Compressor compressor = null;
		if (method != null) {
			if (method.trim().contains(METHOD_GZIP)) {
				compressor = new GZipCompressor();
			}
		}

		return compressor;
	}

}
