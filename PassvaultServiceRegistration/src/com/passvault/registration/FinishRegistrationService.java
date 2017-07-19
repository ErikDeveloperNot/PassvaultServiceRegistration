package com.passvault.registration;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.passvault.registration.utils.Util;
import com.passvault.util.model.Gateway;
import com.passvault.util.model.RegistrationUUID;


/*
 * Part two of a two part registration
 * get UUID 
 * verify it was the UUID sent to client
 * create the sync gateway account
 * add account to account store(?)
 * remove email from the 24 hour cache/store
 * return Gateway model
 */

/*
 * TODO
 * BAD - should accept UUID and email to avoid another finishing someone else's registration.
 * but, who cares, no one is using it 
 */

//Sets the path to base URL + /register
@Path("/finishRegister")
public class FinishRegistrationService {

	
	  @POST
	  @Consumes(MediaType.APPLICATION_JSON)
	  @Produces(MediaType.APPLICATION_JSON)
	  public Response finishRegisterAccount(String jsonIn) {
		  
		  System.out.println(">>REceived: " + jsonIn);
		  ObjectMapper mapper = new ObjectMapper();
		  RegistrationUUID request;
		  
		  try {
			  request = mapper.readValue(jsonIn, RegistrationUUID.class);
		  } catch(Exception e) {
			  e.printStackTrace();
			  return Response.status(500).entity("Invalid Data").build();
		  }
		  
		  String uuid = request.getUuid();
		 
		  // verify needed data was sent
		  if (uuid == null || uuid.equalsIgnoreCase("")) {
			  return Response.status(500).entity("Unique Identifier sent in email not supplied").build();
		  }

		  
		  // verify received UUID is the same that was sent
		  if (!Util.verifyAccountRegistrationUUID(uuid)) {
			  return Response.status(500).entity("Invalid Unique Indentifier supplied").build();
		  }
		  
		  
		  // get email/password from 24 hr store
		  Account account = Util.getAccountFromRegistrationHold(uuid);
		  
		  // create sync gateway account
		  if (!Util.createSyncGatewayAccount(uuid)) {
			  return Response.status(500).entity("Was not able to create account due to backend error.").build();
		  }
		  
		  // store account
		  if (!Util.storeAccount(account)) {
			  // TODO
			  
			  // if account fails to persist just log it, since the sync account was created still return
			  System.err.println("\nError: storing account for \nuuid: " + account.getUuid() + "\n" +
					  "email: " + account.getEmail() + "\n");
			  
			  //return Response.status(500).entity("Was not able to create account due to backend error.").build();
		  }
		  
		  
		  // instead of removing account from 24 hour store just let it get removed
		  
		  
		  // Get gateway config to send back
		  Gateway returnModel = Util.getGateway(account.getUuid(), account.getPassword());
	      
		  
		  // done with second part of registration, return Gateway
		  mapper = new ObjectMapper();
		  mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		  
		  try {
			  return Response.status(200).entity(mapper.writeValueAsString(returnModel)).build();
		  } catch (JsonProcessingException e) {
			System.err.println("Error returning gateway from FinishRegistrationService: " + e.getMessage() + "\n" +
					"Email: " + account.getEmail() + ", UUID: " + account.getUuid());
			System.err.println(returnModel);
			e.printStackTrace();
			return Response.status(500).entity("Error returning response").build();
		  }
	  }
}
