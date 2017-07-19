package com.passvault.registration.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.validator.routines.EmailValidator;

import com.passvault.registration.Account;
import com.passvault.util.model.Gateway;


public class Util {
	
	private static String GATEWAY_URL;
	private static String DB_NAME;
	private static Logger logger = Logger.getLogger("Util");
	
	static {
		
		try {
			Context ctx = new InitialContext();
		    Context env = (Context) ctx.lookup("java:comp/env");
		    GATEWAY_URL = (String) env.lookup("sync-gateway-url");
		    DB_NAME = (String) env.lookup("sync-database-name");
		} catch(Exception e) {
			System.err.println("Error reading web.xml file");
			e.printStackTrace();
			GATEWAY_URL = "http://localhost:4984/";
			DB_NAME = "passvault_service/";
		}
	}

	
	// TODO - make real implementation check sql/file/nosql or however accounts end up being stored
	public static boolean checkIfAccountExists(String email) {
		
		if (email.equalsIgnoreCase("erik@mail.com") || email.equalsIgnoreCase("manor@mail.com"))
			return true;
		
		return false;
	}
	
	
	// verify email is a valid email address
	public static boolean verifyValidEmail(String email) {
		return EmailValidator.getInstance().isValid(email);
	}
	
	
	// verify UUID being used to finish a registration is the one that was sent
	public static boolean verifyAccountRegistrationUUID(String uuid) {
		// BAD - Really should verify UUID/email combo
		
		if (uuid.equalsIgnoreCase("99"))
			return false;
		
		// TODO - real implementation, just return true for now
		return true;
	}
	
	
	// create sync gateway account from UUID
	public static boolean createSyncGatewayAccount(String uuid) {
		// TODO
		// 1. retrieve password from 24 hour cache
		// 2. call utility to create sync gateway account
		// return true if account is created successfully
		
		if (uuid.equalsIgnoreCase("97"))
			return false;

		
		return true;
	}
	
	
	// create sync gateway account - take username/password
	public static SyncGatewayAdmin.AccountCreationStatus createSyncGatewayAccount(String username, String password) {
		
		return SyncGatewayAdmin.AccountCreationStatus.SUCCESS;
	}
	
	
	// retrieve account from 24 hr store
	public static Account getAccountFromRegistrationHold(String uuid) {
		// TODO
		return new Account(uuid, "secret", "test@mail.com");
	}
	
	
	// store account
	public static boolean storeAccount(Account account) {
		// TODO
		
		if (account.getUuid().equalsIgnoreCase("98"))
			return false;
		
		return true;
	}
	
	
	// return remote Gateway
	public static Gateway getGateway(String uuid, String password) {
		
		String[] tokens = GATEWAY_URL.split(":");
		
		if (tokens.length != 3) {
			logger.log(Level.WARNING, "sync-gateway-url from web.xml not formatted correctly: " + GATEWAY_URL);
			tokens = "http://localhost:4984".split(":");
		}
		
		Gateway toReturn = new Gateway();
		
		try {
			toReturn.setBucket(DB_NAME.replace("/", ""));
			toReturn.setPassword(password);
			toReturn.setUserName(uuid);
			toReturn.setPort(Integer.parseInt(tokens[2].replace("/", "")));
			toReturn.setProtocol(tokens[0]);
			toReturn.setServer(tokens[1].replace("/", ""));
		} catch(Exception e) {
			logger.log(Level.WARNING, "sync-gateway-url and DB_NAME from web.xml not formatted correctly,\n" + 
					GATEWAY_URL + "\n" + DB_NAME);
			e.printStackTrace();
			
			toReturn.setBucket("passvault_service");
			toReturn.setPassword(password);
			toReturn.setUserName(uuid);
			toReturn.setPort(4984);
			toReturn.setProtocol("http");
			toReturn.setServer("localhost");
		}
		
		return toReturn;
	}
}
