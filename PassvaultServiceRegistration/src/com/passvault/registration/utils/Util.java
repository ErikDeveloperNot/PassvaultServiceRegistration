package com.passvault.registration.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.spi.RegisterableService;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.validator.routines.EmailValidator;

import com.passvault.registration.Account;
import com.passvault.registration.RegistrationServiceV1;
import com.passvault.util.model.Gateway;


public class Util {
	
	private static String GATEWAY_URL;
	private static String DB_NAME;
	private static String PASSVAULT_SYNC_SERVER_URL;
	private static String PASSVAULT_SYNC_SERVER_PATH;
	private static Logger logger = Logger.getLogger("Util");
	
	static {
		
		try {
			Context ctx = new InitialContext();
		    Context env = (Context) ctx.lookup("java:comp/env");
		    GATEWAY_URL = (String) env.lookup("sync-gateway-url");
		    DB_NAME = (String) env.lookup("sync-database-name");
		    PASSVAULT_SYNC_SERVER_URL = (String) env.lookup("passvault-sync-server-url");
		    PASSVAULT_SYNC_SERVER_PATH = (String) env.lookup("passvault-sync-server-path");
		} catch(Exception e) {
			System.err.println("Error reading web.xml file");
			e.printStackTrace();
			GATEWAY_URL = "http://localhost:4984/";
			DB_NAME = "passvault_service/";
			PASSVAULT_SYNC_SERVER_URL = "https://ec2-13-56-39-109.us-west-1.compute.amazonaws.com:8443/";
			PASSVAULT_SYNC_SERVER_PATH = "PassvaultServiceRegistration/service/sync-aacounts/";
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
	public static Gateway getGateway(String uuid, String password, RegistrationServiceV1.SyncServerType syncServerType) {
		
		String gatewayURL;
		String dbName;
		String defaultDBName;
		String defaultPort;
		
		if (syncServerType == syncServerType.COUCH_BASE) {
			gatewayURL = GATEWAY_URL;
			dbName = DB_NAME.replace("/", "");
			defaultPort = "4984";
			defaultDBName = "passvault_service";
		} else {
			gatewayURL = PASSVAULT_SYNC_SERVER_URL;
			dbName = PASSVAULT_SYNC_SERVER_PATH;
			defaultPort = "8443";
			defaultDBName = "PassvaultServiceRegistration/service/sync-aacounts/";
		}
		
		
		String[] tokens = gatewayURL.split(":");
		
		if (tokens.length != 3) {
			logger.log(Level.WARNING, "sync-gateway-url from web.xml not formatted correctly: " + gatewayURL);
			tokens = ("http://localhost:" + defaultPort + "").split(":");
		}
		
		Gateway toReturn = new Gateway();
		
		try {
			//toReturn.setBucket(dbName.replace("/", ""));
			toReturn.setBucket(dbName);
			toReturn.setPassword(password);
			toReturn.setUserName(uuid);
			toReturn.setPort(Integer.parseInt(tokens[2].replace("/", "")));
			toReturn.setProtocol(tokens[0]);
			toReturn.setServer(tokens[1].replace("/", ""));
		} catch(Exception e) {
			logger.log(Level.WARNING, "sync-gateway-url and DB_NAME from web.xml not formatted correctly,\n" + 
					GATEWAY_URL + "\n" + DB_NAME + "\n" + PASSVAULT_SYNC_SERVER_URL + "\n" + PASSVAULT_SYNC_SERVER_PATH);
			e.printStackTrace();
			
			toReturn.setBucket(defaultDBName);
			toReturn.setPassword(password);
			toReturn.setUserName(uuid);
			toReturn.setPort(4984);
			toReturn.setProtocol("http");
			toReturn.setServer("localhost");
		}
		
		return toReturn;
	}
	
	/*
	public static String getPassvaultSyncServerURLAsJSON() {
		return "{\"url\": \"" + PASSVAULT_SYNC_SERVER_URL + "\"}";
	}
	*/
}
