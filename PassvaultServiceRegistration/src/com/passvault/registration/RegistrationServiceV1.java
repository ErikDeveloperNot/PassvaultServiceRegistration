package com.passvault.registration;


import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.passvault.registration.utils.SyncGatewayAdmin;
import com.passvault.registration.utils.SyncGatewayAdmin.AccountCreationStatus;
import com.passvault.registration.utils.Util;
import com.passvault.server.PassvaultSyncServer;
import com.passvault.server.PassvaultSyncServer.Codes;
import com.passvault.util.model.Gateway;
import com.passvault.util.model.RegistrationRequest;



/*
 * This will be a temp service that simply accepts username/pass and checks if the account
 * exists on sync gateway. 
 *  If not create it and send back Gateway config
 *  If so, fail
 *  
 *  There will be no password recovery, etc. Accounts maybe cleaned from sync at any time
 *  so it is not a storage solution but temp space while syncing devices.
 *  
 *  One POST method
 *  IN:
 *    RegistrationRequest
 *  OUT:
 *    Gateway
 */


//Sets the path to base URL + /register
@Path("/registerV1")
public class RegistrationServiceV1 {
	
	static Logger logger;
	static DataSource ds;
	
	static {
		logger = Logger.getLogger("RegistrationServiceV1");
		
	}

	public static enum SyncServerType {
		PASS_VAULT,
		COUCH_BASE;
	}
	
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	  @Produces(MediaType.APPLICATION_JSON)
	  public Response registerAccount(String jsonIn) {
		  logger.setLevel(Level.FINEST);
		  
		  logger.log(Level.FINEST, "Received Request\n" + jsonIn);
	  //System.out.println(">>REceived: " + jsonIn);
	
	  Holder holder = verifyInput(jsonIn);
	  
	  if (holder.error)
		  return holder.response;
	  
	  
	  // create sync gateway account
	  //AccountCreationStatus status = Util.createSyncGatewayAccount(email, password);
	  AccountCreationStatus status = SyncGatewayAdmin.createUser(holder.email, holder.password);
	 
	  switch (status) {
	  case SUCCESS:
		  logger.log(Level.INFO, "Sync Gateway account created");
		  break;
	  case ALREADY_EXISTS:
		  logger.log(Level.INFO, "An account with the same name already exists.");
		  return Response.status(500).entity("An account with the same name already exists.").build();
	  case ERROR:
	  default:
		  logger.log(Level.INFO, "Was not able to create account due to backend error.");
		  return Response.status(500).entity("Was not able to create account due to backend error.").build();
	  }
	  
	  // Get gateway config to send back
	  return getGatewayConfig(holder.email, holder.password, SyncServerType.COUCH_BASE);
	  /*
	  Gateway returnModel = Util.getGateway(holder.email, holder.password, SyncServerType.COUCH_BASE);
	  
	  // done with registration, return Gateway
	  ObjectMapper mapper = new ObjectMapper();
	  mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	  
	  try {
		  return Response.status(200).entity(mapper.writeValueAsString(returnModel)).build();
	  } catch (JsonProcessingException e) {
		logger.log(Level.CONFIG, "Error returning gateway from RegistrationServiceV1: " + e.getMessage() + "\n" +
				"Email: " + holder.email);
		logger.log(Level.CONFIG, returnModel.toString());
		e.printStackTrace();
		return Response.status(500).entity("Error returning response").build();
	  }
	  */
	}
  
	  
	  @Path("/sync-server")
	  @POST
	  @Consumes(MediaType.APPLICATION_JSON)
	  @Produces(MediaType.APPLICATION_JSON)
	  public Response registerAccountSyncServer(String jsonIn) {
		  logger.setLevel(Level.FINEST);
		  logger.log(Level.FINEST, "Received Request\n" + jsonIn);
		  
		  Holder holder = verifyInput(jsonIn);
		  
		  if (holder.error)
			  return holder.response;
		  
		// try to create the account
		  int status = PassvaultSyncServer.getInstance().createAccountUUID(holder.email, holder.password);
		  
		  switch (status) {
		  case Codes.ACCOUNT_ADDED:
			  logger.log(Level.INFO, "Sync Gateway account created");
			  break;
		  case Codes.ACCOUNT_ALREADY_EXISTS:
			  logger.log(Level.INFO, "An account with the same name already exists.");
			  return Response.status(500).entity("An account with the same name already exists.").build();
		  default:
			  logger.log(Level.INFO, "Was not able to create account due to backend error.");
			  return Response.status(500).entity("Was not able to create account due to backend error.").build();
		  }
		  
		  
		 //return Response.status(200).entity(Util.getPassvaultSyncServerURLAsJSON()).build();
		  return getGatewayConfig(holder.email, holder.password, SyncServerType.PASS_VAULT);
	  }
	  
	  
	  @GET
	  @Produces(MediaType.APPLICATION_JSON)
	  public Response getGatewayConfigCB() {
		  return getGatewayConfig("xxx@xxx.com", "xxx", SyncServerType.COUCH_BASE);
		  /*
		  // Get gateway config with dummy user/pass to send back
		  Gateway returnModel = Util.getGateway("xxx@xxx.com", "xxx", SyncServerType.COUCH_BASE);
		  
		  // done with registration, return Gateway
		  ObjectMapper mapper = new ObjectMapper();
		  mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		  
		  try {
			  return Response.status(200).entity(mapper.writeValueAsString(returnModel)).build();
		  } catch (JsonProcessingException e) {
			logger.log(Level.WARNING, "Error returning gateway from getGatewayConfig: " + e.getMessage());
			logger.log(Level.WARNING, returnModel.toString());
			e.printStackTrace();
			return Response.status(500).entity("Error returning response").build();
		  }
		  */
	  }
	  
	  
	  @Path("/sync-server")
	  @GET
	  @Produces(MediaType.APPLICATION_JSON)
	  public Response getGatewayConfigSyncServer() {
		  //return Response.status(200).entity(Util.getPassvaultSyncServerURLAsJSON()).build();
		  return getGatewayConfig("xxx@xxx.com", "xxx", SyncServerType.PASS_VAULT);
	  }
	  
	  
	  private Response getGatewayConfig(String user, String password, SyncServerType syncServerType) {
		// Get gateway config with dummy user/pass to send back
		  Gateway returnModel = Util.getGateway(user, password, syncServerType);
		  
		  // done with registration, return Gateway
		  ObjectMapper mapper = new ObjectMapper();
		  mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		  
		  try {
			  return Response.status(200).entity(mapper.writeValueAsString(returnModel)).build();
		  } catch (JsonProcessingException e) {
			logger.log(Level.WARNING, "Error returning gateway from getGatewayConfig: " + e.getMessage());
			logger.log(Level.WARNING, returnModel.toString());
			e.printStackTrace();
			return Response.status(500).entity("Error returning response").build();
		  }
	  }
	  
	  
	  
	  /*
	   * return null if everything is good, otherwise return a response to send back to the client
	   */
	  private Holder verifyInput(String jsonIn) {
		  Holder holder = new Holder();
		  ObjectMapper mapper = new ObjectMapper();
		  RegistrationRequest request = null;
		  
		  try {
			  request = mapper.readValue(jsonIn, RegistrationRequest.class);
		  } catch(Exception e) {
			  e.printStackTrace();
			  //return Response.status(500).entity("Invalid Data").build();
			  holder.response = Response.status(500).entity("Invalid Data").build();
			  holder.error = true;
		  }
		  
		  holder.email = request.getEmail();
		  holder.password = request.getPassword();
		  String version = request.getVersion();
		  logger.log(Level.INFO, "Received request for\nEmail: " + holder.email + "\nVersion: " + version);
		  

		  // verify version is v1.x
		  if (version == null || !version.startsWith("v1.")) {
			  logger.log(Level.FINEST, "Client is not using the correct version");
			  //return Response.status(500).entity("Client is not using the correct version.").build();
			  holder.response = Response.status(500).entity("Client is not using the correct version.").build();
			  holder.error = true;
		  }
		  
		  // verify needed data was sent
		  if (holder.email == null || holder.email.equalsIgnoreCase("")) {
			  logger.log(Level.FINEST, "Email not supplied");
			  //return Response.status(500).entity("Email not supplied").build();
			  holder.response = Response.status(500).entity("Email not supplied").build();
			  holder.error = true;
		  }
		  
		  if (holder.password == null || holder.password.equalsIgnoreCase("")) {
			  logger.log(Level.FINEST, "Password not supplied");
			  //return Response.status(500).entity("Password not supplied").build();
			  holder.response = Response.status(500).entity("Password not supplied").build();
			  holder.error = true;
		  }
		  
		  // verify email is valid - Not really needed for V1 but keep it in
		  if (!Util.verifyValidEmail(holder.email)) {
			  logger.log(Level.FINEST, "Invalid Email supplied");
			  //return Response.status(500).entity("Invalid Email supplied").build();
			  holder.response = Response.status(500).entity("Invalid Email supplied").build();
			  holder.error = true;
		  }
		  
		  // TODO verify password, **doesn't seem to be needed
		  
		  return holder;
	  }
	  
	  
	  private class Holder {
		  String email;
		  String password;
		  boolean error;
		  Response response;
	  }
}
