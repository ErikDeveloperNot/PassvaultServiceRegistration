package com.passvault.registration;

public class Account {
	
	private String uuid;
	private String password;
	private String email;
	
	
	public Account() {
		super();
	}
	
	public Account(String uuid, String password, String email) {
		super();
		this.uuid = uuid;
		this.password = password;
		this.email = email;
	}
	
	
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	
}
