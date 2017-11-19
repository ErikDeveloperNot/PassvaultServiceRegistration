package com.passvault.registration;


import java.util.UUID;

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
import com.passvault.util.model.RegistrationRequest;
import com.passvault.util.model.RegistrationUUID;


/*
 * Part one of a two part registration
 * get email/password, do checks
 * send back UUID for client to finish registration
 */

//Sets the path to base URL + /register
@Path("/register")
public class RegistrationService {


/*
	  // This method is called if TEXT_PLAIN is request
	  @GET
	  @Produces(MediaType.TEXT_PLAIN)
	  public String sayPlainTextHello() {
	    return "Hello Jersey";
	  }

	  // This method is called if XML is request
	  @GET
	  @Produces(MediaType.TEXT_XML)
	  public String sayXMLHello() {
	    return "<?xml version=\"1.0\"?>" + "<hello> Hello Jersey" + "</hello>";
	  }

	  // This method is called if HTML is request
	  @GET
	  @Produces(MediaType.TEXT_HTML)
	  public String sayHtmlHello() {
	    return "<html> " + "<title>" + "Hello Jersey" + "</title>"
	        + "<body><h1>" + "Hello Jersey" + "</body></h1>" + "</html> ";
	  }
	  
	  
	  @POST
	  @Consumes(MediaType.APPLICATION_JSON)
	  @Produces(MediaType.APPLICATION_JSON)
	  //public String sendRespondJson(String s) {
	  public Response sendRespondJson(String s) {
		  
		  System.out.println(">>REceived: " + s);
		  ObjectMapper mapper = new ObjectMapper();
		  try {
			  System.out.println(mapper.readValue(s, Todo.class));
		  } catch(Exception e) {
			  e.printStackTrace();
			  return Response.status(500).entity("Invalid Data").build();
		  }

	      
	      Todo todo = new Todo();
		  todo.setSummary("This is the summary");
		  todo.setDescription("This is the description");
		  mapper = new ObjectMapper();
		  mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		  try {
			//return(mapper.writeValueAsString(todo));
			  return Response.status(200).entity(mapper.writeValueAsString(todo)).build();
		  } catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//return null;
			return Response.status(500).entity("Error writing response").build();
		  }
		  
		  //return null;
	  }
*/
	
	
	  @POST
	  @Consumes(MediaType.APPLICATION_JSON)
	  @Produces(MediaType.APPLICATION_JSON)
	  public Response registerAccount(String jsonIn) {
		  
		  System.out.println(">>REceived: " + jsonIn);
		  ObjectMapper mapper = new ObjectMapper();
		  RegistrationRequest request;
		  
		  try {
			  request = mapper.readValue(jsonIn, RegistrationRequest.class);
		  } catch(Exception e) {
			  e.printStackTrace();
			  return Response.status(500).entity("Invalid Data").build();
		  }
		  
		  String email = request.getEmail();
		  String password = request.getPassword();

		  // verify needed data was sent
		  if (email == null || email.equalsIgnoreCase("")) {
			  return Response.status(500).entity("Email not supplied").build();
		  }
		  
		  if (password == null || password.equalsIgnoreCase("")) {
			  return Response.status(500).entity("Password not supplied").build();
		  }
		  
		  // verify email is valid
		  if (!Util.verifyValidEmail(email)) {
			  return Response.status(500).entity("Invalid Email supplied").build();
		  }
		  
		  // TODO verify password, not sure of constraints by sync gateway
		  
		  // verify account does not already exist
		  if (Util.checkIfAccountExists(email)) {
			  return Response.status(500).entity("Account already exists. If you need to get your identifier " +
					  "use option to resend Identifier. If you need to reset your password use option to reset " +
					  "password.").build();
		  }
		  
		  // generate a UUID for this account which will be used as the username for sync gateway.
		  String uuid = UUID.randomUUID().toString();
		  
		  /*
		   * decide if I want to check if somehow the UUID already exists, should never happen
		   * especially since this will never be used :)
		   */
		  
		  // TODO
		  /*
		   * make a call to store the email/pass with generated UUID for some time, 24hr.
		   * if registration does not finish by then remove them and the email will be open again
		   */
		  
		  // TODO
		  /*
		   * send email
		   */
	      
		  
		  // done with first part of registration, return UUID
		  RegistrationUUID returnModel = new RegistrationUUID();
		  returnModel.setUuid(uuid);
		  mapper = new ObjectMapper();
		  mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		  
		  try {
			//return(mapper.writeValueAsString(todo));
			  return Response.status(200).entity(mapper.writeValueAsString(returnModel)).build();
		  } catch (JsonProcessingException e) {
			System.err.println("Error return UUID from RegistrationService: " + e.getMessage() + "\n" +
					"Email: " + email + ", UUID: " + uuid);
			e.printStackTrace();
			return Response.status(500).entity("Error returning response").build();
		  }
	  }
	  
	  
	  /*
	  @GET
	  @Produces(MediaType.APPLICATION_JSON)
	  public String sayJsonHello() {
		  Todo todo = new Todo();
		  todo.setSummary("This is the summary");
		  todo.setDescription("This is the description");
		  ObjectMapper mapper = new ObjectMapper();
		  mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		  try {
			return(mapper.writeValueAsString(todo));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		  //return "{ \"hello\": \"Hello Jersey\" }";
	  }
	  */	
	  

}
