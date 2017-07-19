package com.passvault.registration.utils;

import javax.ws.rs.core.Response;

public class SyncGatewayAdminException extends Exception {

	private Response response;
	
	public SyncGatewayAdminException(String message) {
		super(message);
	}
	
	public SyncGatewayAdminException(String message, Response response) {
		super(message);
		this.response = response;
	}
	
	public Response getResponse() {
		return response;
	}
}
