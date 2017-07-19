package com.passvault.util.model;

public class ChangesRequest {
	
	private int limit;
	private boolean active_only;
	private String filter;
	private String channels;
	private int since;
	
	public int getLimit() {
		return limit;
	}
	public void setLimit(int limit) {
		this.limit = limit;
	}
	public boolean isActive_only() {
		return active_only;
	}
	public void setActive_only(boolean active_only) {
		this.active_only = active_only;
	}
	public String getFilter() {
		return filter;
	}
	public void setFilter(String filter) {
		this.filter = filter;
	}
	public String getChannels() {
		return channels;
	}
	public void setChannels(String channels) {
		this.channels = channels;
	}
	public int getSince() {
		return since;
	}
	public void setSince(int since) {
		this.since = since;
	}

}
