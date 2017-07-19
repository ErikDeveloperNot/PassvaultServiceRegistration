package com.passvault.registration.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.passvault.util.model.CreateSyncAccountRequest;
import com.passvault.util.model.SyncError;

public class SyncGatewayAdmin {

	public static enum AccountCreationStatus {
		SUCCESS, ERROR, ALREADY_EXISTS 
	}
	
	// hard code for now, should come from web.xml
	private static String ADMIN_URL;
	private static String DB_NAME;
	public static String USER_PATH = "_user/";
	public static String CHANNEL_PREFIX = "channel_";
	private static boolean DISABLED = false;
	
	private static Logger logger = Logger.getLogger("SyncGatewayAdmin");

	static {
		
		try {
			Context ctx = new InitialContext();
		    Context env = (Context) ctx.lookup("java:comp/env");
		    ADMIN_URL = (String) env.lookup("admin-url");
		    DB_NAME = (String) env.lookup("sync-database-name");
		} catch(Exception e) {
			System.err.println("Error reading web.xml file for 'admin-url' property");
			e.printStackTrace();
			ADMIN_URL = "http://node1.user1.com:4985/";
			DB_NAME = "passvault_service/";
		}
	}
	
	/*
	 *  Not needed for version1
	 */
	public static AccountCreationStatus checkIfUserExists(String user) {
		
		
		return AccountCreationStatus.SUCCESS;
	}
	
	
	/*
	 *  create Sync Gateway Account using POST, (PUT would update if account already exists, DON'T want)
	 *    Success - returns 201 and no Content
	 *    Failure - returns 409 if account already exists with "error" mapping to returned HTTP code
	 *              and "reason" the specific reason
	 *              Any other error will most likely return a flavor of 500 with "error" and "reason"
	 */
	public static AccountCreationStatus createUser(String user, String password) {
		logger.log(Level.INFO, "Sending create user to sync gateway for user: " + user);
		
		CreateSyncAccountRequest sendModel = new CreateSyncAccountRequest();
		sendModel.setName(user);
		sendModel.setPassword(password);
		sendModel.setAdmin_channels(new String[] {CHANNEL_PREFIX + user});
		sendModel.setDisabled(DISABLED);
		
		Response response = sendPOST(sendModel, new String[] {USER_PATH});
		int status = response.getStatus();
		AccountCreationStatus toReturn;
		
		if (status >= 200 && status < 300) {
			logger.log(Level.INFO, "Sync Account: " + user + " created, status=" + status);
			toReturn = AccountCreationStatus.SUCCESS;
		} else if (status == 409) {
			logger.log(Level.INFO, "Sync Account: " + user + " already exists, status=" + status);
			toReturn =  AccountCreationStatus.ALREADY_EXISTS;
		} else {
			
			if (response.hasEntity()) {
				String responseString = response.readEntity(String.class);
		    		ObjectMapper mapper = new ObjectMapper();
		    		SyncError returnModel = new SyncError();
		    		
		    		try {
					returnModel = mapper.readValue(responseString, returnModel.getClass());
					logger.log(Level.WARNING, "Error creating sync account for " + user + "\nerror: " + 
							returnModel.getError() + "\nreason: " + returnModel.getReason());
				} catch (IOException e) {
					logger.log(Level.WARNING, "Error serializing response:\n" + responseString);
					e.printStackTrace();
					toReturn = AccountCreationStatus.ERROR;
				}
			} else {
				logger.log(Level.WARNING, "Error creating sync account for " + user + ", status=" + status);
			}
			
			toReturn = AccountCreationStatus.ERROR;
		}
		
		response.close();
		
		return toReturn;
	}
	
	
	
	public static Response sendPOST(Object sendModel, String[] paths) {
		//ClientConfig config = new ClientConfig();
        //Client client = ClientBuilder.newClient(config);
        Client client = getClient();
        WebTarget target = client.target(ADMIN_URL + DB_NAME);
        
        // paths[] need to make sure path objects are in the correct order
        for (String path : paths) {
			target = target.path(path);
		}
        
        //System.out.println(target.getUri().getPath());
        ObjectMapper sendMapper = new ObjectMapper();
        Invocation.Builder builder = target.request();
        Response response = null;
        
        try {
			response = builder.post(Entity.json(sendMapper.writeValueAsString(sendModel)));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return response;
		}
        
        return response;
	}
	
	
	public static Response sendDELETE(String[] paths, Map<String, String> params) {
		//ClientConfig config = new ClientConfig();
        //Client client = ClientBuilder.newClient(config);
        Client client = getClient();
        WebTarget target = client.target(ADMIN_URL + DB_NAME);
        
        // paths[] need to make sure path objects are in the correct order
        for (String path : paths) 
			target = target.path(path);
        
        if (params != null) 
        		for (String key : params.keySet()) 
					target = target.queryParam(key, params.get(key));

        ObjectMapper sendMapper = new ObjectMapper();
        Invocation.Builder builder = target.request();
        Response response = builder.delete();
        
        return response;
	}


	private static Client getClient() {
		Client client = null;
		
		if (ADMIN_URL.split(":")[0].equalsIgnoreCase("https")) {
			SslConfigurator sslConfig = SslConfigurator.newInstance()
			        .trustStoreFile("/opt/ssl/keystores/passvault_store.jks")
			        .trustStorePassword("passvault");
			SSLContext sslContext = sslConfig.createSSLContext();
			client = ClientBuilder.newBuilder().sslContext(sslContext).build();
		} else {
			ClientConfig config = new ClientConfig();
	        client = ClientBuilder.newClient(config);
		}

		return client;
	}
	
	/*
	public static void main(String args[]) {
		Map<String, String> params = new HashMap<>();
		params.put("rev", "1-557cb751cfc3437aa80aa1721a0c65bf");
		Response resp = sendDELETE(new String[] {"user_003@mail.comtest_001"}, params);
		
		if (resp != null) {
			System.out.println(resp.getStatus());
			System.out.println(resp.readEntity(String.class));
		}
		
		Response resp2 = sendDELETE(new String[] {USER_PATH, "user_005@mail.com"}, null);
		
		if (resp2 != null) {
			System.out.println(resp2.getStatus());
			System.out.println(resp2.readEntity(String.class));
		}
	
	}
	*/
}
