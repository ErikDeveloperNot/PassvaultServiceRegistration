package com.passvault.server.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.passvault.server.PassvaultSyncServer;
import com.passvault.server.PassvaultSyncServer.Codes;
import com.passvault.util.model.syncserver.Account;
import com.passvault.util.model.syncserver.CheckAccount;
import com.passvault.util.model.syncserver.SyncRequestFinal;
import com.passvault.util.model.syncserver.SyncRequestInitial;
import com.passvault.util.model.syncserver.SyncResponseInitial;

//Sets the path to base URL + /register
@Path("/sync-accounts")
public class SyncServer {

	private static Logger logger;
	
	static {
		logger = Logger.getLogger("com.passvault.server.sync");
	}
	
	
	@Path("sync-initial")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response syncInitial(String jsonIn) {
		ObjectMapper mapper = new ObjectMapper();
		SyncRequestInitial request = null;
		  
		try {
			request = mapper.readValue(jsonIn, SyncRequestInitial.class);
		} catch(Exception e) {
			logger.warning("Error parsing request: " + e.getMessage());
			e.printStackTrace();
			return Response.status(500).entity("Error parsing request: " + e.getMessage()).build();
		}
		
		List<CheckAccount> accounts = new ArrayList(request.getAccounts());
		Map<String, CheckAccount> accountsMap = new HashMap<>();
		
		for (CheckAccount checkAccount : accounts) {
			accountsMap.put(checkAccount.getAccountName(), checkAccount);
		}
		
		SyncResponseInitial syncResponseInitial = PassvaultSyncServer.getInstance().syncInitial(
				request.getUser(), request.getPassword(), accountsMap);
		
		if (syncResponseInitial.getResponseCode() == PassvaultSyncServer.Codes.SUCCESS) {
		  mapper = new ObjectMapper();
		  mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		  
		  try {
			  return Response.status(200).entity(mapper.writeValueAsString(syncResponseInitial)).build();
		  } catch (JsonProcessingException e) {
			logger.log(Level.CONFIG, "Error returning sync response: " + e.getMessage());
			e.printStackTrace();
			return Response.status(500).entity("Error returning sync response").build();
		  }
		}
		
		return Response.status(500).entity("Error returning sync response").build();
	}
	
	
	@Path("sync-final")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response syncFinal(String jsonIn) {
		ObjectMapper mapper = new ObjectMapper();
		SyncRequestFinal request = null;
		  
		try {
			request = mapper.readValue(jsonIn, SyncRequestFinal.class);
		} catch(Exception e) {
			logger.warning("Error parsing request: " + e.getMessage());
			e.printStackTrace();
			return Response.status(500).entity("Error parsing request: " + e.getMessage()).build();
		}
	
		Map<String, Account> accountsMap = new HashMap<>();
		
		for (Account account : request.getAccounts()) {
			accountsMap.put(account.getAccountName(), account);
		}
		
		int result = PassvaultSyncServer.getInstance().syncFinal(request.getUser(), request.getPassword(), 
				request.getLockTime(), accountsMap);
		
		if (result == Codes.SUCCESS) {
			return Response.status(200).entity("Accounts successfully synchronized").build();
		} else {
			logger.warning("Error with syncFinal, error code=" + result + ", " + Codes.getErrorStringForCode(result));
		}
		
		return Response.status(500).entity("Error syncing accounts").build();
	}
}
