package com.passvault.registration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passvault.registration.utils.SyncGatewayAdmin;
import com.passvault.registration.utils.Util;
import com.passvault.registration.utils.SyncGatewayAdmin.AccountCreationStatus;
import com.passvault.registration.utils.SyncGatewayAdminException;
import com.passvault.util.model.Change;
import com.passvault.util.model.Changes;
import com.passvault.util.model.ChangesRequest;
import com.passvault.util.model.DeleteRequest;
import com.passvault.util.model.RegistrationEmail;
import com.passvault.util.model.RegistrationRequest;





//Sets the path to base URL + /register
@Path("/deleteAccount")
public class DeleteService {
	
	Logger logger = Logger.getLogger("DeleteService");
	
	/*
	 * Returns
	 *  204: request accepted and calls to sync gateway were successful
	 *  202: request accepted but errors calling sync gateway, messages will be logged and retried
	 *  400: request does not contain a valid email account
	 *  500: error trying to deserialize the request
	 *  
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)  //RegistrationEmail model
	public Response deleteAccount(String jsonIn) {
		
		logger.setLevel(Level.FINEST);
		
		ObjectMapper mapper = new ObjectMapper();
		DeleteRequest request = new DeleteRequest();
		
		try {
			  request = mapper.readValue(jsonIn, DeleteRequest.class);
		} catch(Exception e) {
			e.printStackTrace();
			return Response.status(500).entity("Unable to parse request").build();
		}
		
		String user = request.getUser();
		String password = request.getPassword();
		
		logger.log(Level.INFO, "Received delete account request for: " + user);
		
		if (user == null || !Util.verifyValidEmail(user)) {
			return Response.status(400).entity("Invalid Data").build();
		}
		
		Changes changes = null;
		
		// 1. get current doc list for user
		try {
			changes = getChangesFeed(user, password);
		} catch (Exception e) {
			logAccountForDeletion(user);
			logger.log(Level.SEVERE, " FAILED to get _changes feed for account: " + user);
			e.printStackTrace();
			return Response.status(202).entity("Account has been logged and will be deleted.").build();
		}
		
		// 2. if changes has documents delete them
		if (changes != null) 
			deleteDocs(changes.getResults());
		
		// 3. delete user
		Response resp = SyncGatewayAdmin.sendDELETE(new String[] {SyncGatewayAdmin.USER_PATH, user}, null);
		
		if (resp != null) {
			int status = resp.getStatus();
			
			if (status >= 200 && status <300) {
				logger.log(Level.INFO, "Account: " + user + ", has been deleted.");
				return Response.status(200).entity("Account has been deleted.").build();
			} else if (status == 404) { 
				logger.log(Level.INFO, "delete account: " + user + ", status=" + resp.getStatus() +
						", account not found");
				return Response.status(404).entity("Account not found.").build();
			} else {
				String result = "";
				
				if (resp.hasEntity()) 
					result = resp.readEntity(String.class);
					
				logger.log(Level.WARNING, "failed to delete account: " + user + "\n" + result);
				logAccountForDeletion(user);
				return Response.status(202).entity("Account has been logged and will be deleted.").build();
			}
		}
			
		logger.log(Level.WARNING, "failed to delete account: " + user);
		logAccountForDeletion(user);
		return Response.status(202).entity("Account has been logged and will be deleted.").build();
	}
		
		/*
		try {
			
			if (deleteUser(accountToDelete)) {
				logger.log(Level.INFO, "Account: " + accountToDelete + ", has been deleted.");
				return Response.status(200).entity("Account has been deleted.").build();
			} else {
				logger.log(Level.SEVERE, " FAILED to delete account: " + accountToDelete);
				logAccountForDeletion(accountToDelete);
				return Response.status(202).entity("Account has been logged and will be deleted.").build();
			}
			
		} catch (SyncGatewayAdminException e) {
			logger.log(Level.SEVERE, " FAILED to delete account: " + accountToDelete);
			e.printStackTrace();
			Response resp = e.getResponse();
			
			if (resp != null) {
				
				if (resp.getStatus() == 404) {
					String reason = "";
					
					if (resp.hasEntity())
						reason = resp.readEntity(String.class);
					
					logger.log(Level.INFO, "delete account: " + accountToDelete + ", status=" + resp.getStatus() +
							", " + reason);
					return Response.status(404).entity("Account not found.").build();
				}
			} 
				
			logAccountForDeletion(accountToDelete);
			return Response.status(202).entity("Account has been logged and will be deleted.").build();
		}
		*/
	
	
	
	private void logAccountForDeletion(String account) {
		// TODO - in case of Exception log Account ID for Manual deletion and return
		System.out.println(">>>>>>>>>>>> Implement log account for deletion <<<<<<<<<<<<<<");
	}
	
	/*
	private boolean deleteUser(String account) throws SyncGatewayAdminException {
		Response resp = SyncGatewayAdmin.sendDELETE(new String[] {SyncGatewayAdmin.USER_PATH, account}, null);
		
		if (resp != null) {
			
			if (resp.getStatus() >= 200 && resp.getStatus() <300) {
				logger.log(Level.INFO, "deleted account: " + account);
				return true;
			} else {
				String result = null;
				
				if (resp.hasEntity()) {
					result = resp.readEntity(String.class);
					logger.log(Level.WARNING, "failed to delete account: " + account + "\n" + result);
				} else {
					logger.log(Level.WARNING, "failed to delete account: " + account);
					
				}
				
				throw new SyncGatewayAdminException("Account delete failed, status: " + resp.getStatus() + 
						"\n" + result, resp);
			}
		}
		
		// resp should never be null
		logger.log(Level.WARNING, "Response from deleteUser was null, account: " + account);
		return false;
	}
	*/
	
	
	private void deleteDocs(Change[] changes) {
		
		if (changes != null) {
			// log any errors but continue
			for (Change change : changes) {
				
				if (change.getChanges().length < 1)
					continue;
				
				Map<String, String> params = new HashMap<>();
				params.put("rev", change.getChanges()[0].getRev());
				Response resp = SyncGatewayAdmin.sendDELETE(new String[] {change.getId()}, params);
				
				if (resp != null) {
					
					if (resp.getStatus() >= 200 && resp.getStatus() <300) {
						logger.log(Level.FINE, "deleted doc: " + change.getId());
					} else {
						
						if (resp.hasEntity()) {
							String result = resp.readEntity(String.class);
							logger.log(Level.WARNING, "failed to delete doc: " + change.getId() + "\n" + result);
						} else {
							logger.log(Level.WARNING, "failed to delete doc: " + change.getId());
						}
					}
				}
			}
		}
	}
	
	
	private Changes getChangesFeed(String user, String password) throws Exception {
		String channel = "channel_" + user;
		ChangesRequest request = new ChangesRequest();
		request.setActive_only(true);
		request.setFilter("sync_gateway/bychannel");
		request.setLimit(0);
		request.setSince(0);
		request.setChannels(channel);
		
		Response response = SyncGatewayAdmin.sendPOST(request, new String[] {"_changes"}, user, password);
		
		int status = response.getStatus();
		Changes toReturn = null;
		
		if (status >= 200 && status < 300) {
			
			if (response.hasEntity()) {
				String entity = response.readEntity(String.class);
				logger.log(Level.FINE, "Retrieved changes feed for " + user + "\n" + entity);
				
				ObjectMapper mapper = new ObjectMapper();
				
				try {
					toReturn = mapper.readValue(entity, Changes.class);
				} catch (IOException e) {
					
					logger.log(Level.WARNING, "Error reading json into Changes for account: " + user);
					e.printStackTrace();
					// return null and continue on with account deletion
				}
			}
		} else {
			logger.log(Level.SEVERE, "Error calling changes feed for account: " + user + ", status=" + status);
			
			if (response.hasEntity()) {
				String entity = response.readEntity(String.class);
				logger.log(Level.SEVERE, entity);
				throw new Exception(entity);
			}
			
			throw new Exception("Error calling changes feed, status=" + status);
		}

		return toReturn;
	}
}
