package com.protect.kid.filter;

import android.content.Context;
import android.text.TextUtils;

import com.protect.kid.R;
import com.protect.kid.app.GlobalApplication;
import com.protect.kid.core.builder.BlockingInfoBuilder;
import com.protect.kid.core.builder.DefaultBlockingInfoBuilder;
import com.protect.kid.core.filter.Filter;
import com.protect.kid.core.http.HttpResponse;
import com.protect.kid.db.AppCache;
import com.protect.kid.util.AssetsUtil;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class HtmlBlockingTimeInfoBuilder implements BlockingInfoBuilder {

	private static final String PLACEHOLDER_TITLE = "{title}";
	private static final String PLACEHOLDER_APP_NAME = "{AppName}";
	private static final String PLACEHOLDER_BLOCK_COUNT = "{BlockCount}";

	private static String mHtmlContent;

	@Override
	public boolean match(int result) {
		return result == Filter.FILTER_TIME;
	}

	@Override
	public ByteBuffer getBlockingInformation() {
		ByteBuffer byteBuffer = null;
		Context context = GlobalApplication.getInstance();
		if (mHtmlContent == null) {
			mHtmlContent = AssetsUtil.readAssetsTextFile(context, "html/block_by_time.html");
		}
		if (!TextUtils.isEmpty(mHtmlContent)) {
			int count = AppCache.getBlockCount(GlobalApplication.getInstance());
			String result = mHtmlContent.replace(PLACEHOLDER_TITLE, context.getString(R.string.block_title));
			result = result.replace(PLACEHOLDER_APP_NAME, context.getString(R.string.app_name));
			result = result.replace(PLACEHOLDER_BLOCK_COUNT, Integer.toString(count));

			HttpResponse response = new HttpResponse(true);
			HashMap<String, String> header = new HashMap<>();
			header.put("Content-Type", "text/html; charset=utf-8");
			header.put("Connection", "close");
			header.put("Content-Length", Integer.toString(result.getBytes().length));
			response.setHeaders(header);
			response.setBody(result);
			response.setStateLine("HTTP/1.1 200 NO_FILTER");
			byteBuffer = response.getBuffer();
		} else {
			byteBuffer = DefaultBlockingInfoBuilder.get().getBlockingInformation();
		}
		return byteBuffer;
	}
}
