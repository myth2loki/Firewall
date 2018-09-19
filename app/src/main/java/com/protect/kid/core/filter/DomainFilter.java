package com.protect.kid.core.filter;

public interface DomainFilter {
	void prepare();
	boolean needFilter(String domain, int ip, int port);
}
