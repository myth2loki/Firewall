package com.protect.kid.core.filter;

public interface DomainFilter extends Filter {
	void prepare();
	int filter(String domain, int ip, int port);
}
