package com.protect.kid.event;

public class HostUpdateEvent {

	private Status mStatus;

	public HostUpdateEvent(Status status) {
		mStatus = status;
	}

	public Status getStatus() {
		return mStatus;
	}

	public enum Status {
		Updating,
		UpdateFinished,
	}
}
