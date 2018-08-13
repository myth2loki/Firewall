package com.protect.kid.util;

import android.support.annotation.Nullable;

public interface Compressor {

	@Nullable
	byte[] compress(byte[] source) throws Exception;

	@Nullable
	byte[] uncompress(byte[] cipher) throws Exception;
}
