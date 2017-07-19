package com.passvault.util.model;

public class CreateSyncAccountRequest {

	private String name;
	private String password;
	private String[] admin_channels;
	private boolean disabled;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String[] getAdmin_channels() {
		return admin_channels;
	}
	public void setAdmin_channels(String[] admin_channels) {
		this.admin_channels = admin_channels;
	}
	public boolean isDisabled() {
		return disabled;
	}
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

}
