package com.protect.kid.core.builder;

import java.nio.ByteBuffer;


public interface BlockingInfoBuilder {

	ByteBuffer getBlockingInformation();
	boolean match(int result);
}
