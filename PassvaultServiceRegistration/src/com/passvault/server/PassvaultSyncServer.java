package com.passvault.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.util.encoders.Hex;

import com.passvault.server.data.DataStore;
import com.passvault.server.data.PostgresStore;
import com.passvault.util.model.syncserver.Account;
import com.passvault.util.model.syncserver.CheckAccount;
import com.passvault.util.model.syncserver.SyncResponseInitial;
import com.passvault.util.model.syncserver.User;

/*
 * The sync server will work in a very basic way. Since Syncing will be done in 2 calls
 * both being REST the account will be locked during this. If the second call does not come
 * in within 30 seconds the account will be released. All Database changes will not happen until
 * until the second call is received.
 * 
 * This class will be a single instance created by the servlet context listener. What do any synchronized
 * for this, but assume no other class other then the context listener will call the initialize method.
 * 
 */
public class PassvaultSyncServer {
	

	private static PassvaultSyncServer server;
	private static Logger logger;
	
	private static long MAX_LOCK_TIME = 30_000L;   // 30 seconds
	
	private Map<String, User> accountMap;
	private DataStore dataStore;
	private boolean initialized;
	
	private PassvaultSyncServer() {
		dataStore = new PostgresStore();
		accountMap = new ConcurrentHashMap<>();
		
		try {
			loadAccountsMap(accountMap);
			initialized = true;
		} catch (Exception e) {
			initialized = false;
			logger.severe("Error trying to load accounts Map: " + e.getMessage());
			logger.info("Starting thread to keep trying to load accounts");
			e.printStackTrace();
			
			new Thread(new Runnable() {
				int attempt = 1;
				
				@Override
				public void run() {
					while (true) {
						try {
							Thread.sleep(30_000L);
							loadAccountsMap(accountMap);
							initialized = true;
							return;
						} catch(Exception e1) {
							logger.warning("Failed to load Accounts Map on " + attempt++ + " attempt");
							e1.printStackTrace();
						}
					}
				}
			}, "Accounts-Load-Backgroud-Thread").start();
		}
	}
	
	
	protected static void initialize() {
		logger = Logger.getLogger("com.passvault.server");
		logger.info("Initializing PassvaultSyncServer");
		server = new PassvaultSyncServer();
	}
	
	
	public static PassvaultSyncServer getInstance() {
		return server;
	}
	
	
	public int createAccountUUID(String accountUUID, String password) {
		int toReturn;
		
		User toAdd = new User(accountUUID, hashPassword(password), 0L, 1.00F);
		
		synchronized (accountMap) {
			
			if (accountMap.get(accountUUID) == null) {
				//add account
				accountMap.put(accountUUID, toAdd);
				toReturn = Codes.SUCCESS;
			} else {
				//account already exists
				toReturn = Codes.ACCOUNT_ALREADY_EXISTS;
			}
		}
			
		if (toReturn == Codes.SUCCESS) {
			if (!dataStore.addUser(toAdd)) {
				
				synchronized (accountMap) {
					accountMap.remove(accountUUID);
				}
				
				toReturn = Codes.ERROR;
			} else {
				toReturn = Codes.ACCOUNT_ADDED;
			}
		}

		return toReturn;
	}
	
	
	public int deleteAccountUUID(String accountUUID, String password) {
		int toReturn = Codes.ERROR;
		long lock = lockAccountUUIDBlocking(accountUUID, password);
		
		if (lock > Codes.LOCK_SUCCESS || lock == Codes.ACCOUNT_DOES_NOT_EXIST) {
			// go ahead and attempt to remove from the DB either way in case an account is dangling
			if (dataStore.deleteUser(accountUUID)) {
				
				synchronized (accountMap) {
					accountMap.remove(accountUUID);
				}
				
				toReturn = Codes.SUCCESS;
			} else {
				toReturn = Codes.ERROR;
			}
		}
		
		return toReturn;
	}
	
	
	public SyncResponseInitial syncInitial(String accountUUID, String accountPassword, Map<String, CheckAccount> accounts) { //List<CheckAccount> accounts) {
		SyncResponseInitial toReturn = new SyncResponseInitial();
		long lock = lockAccountUUIDBlocking(accountUUID, accountPassword);
		
		if (lock < Codes.LOCK_SUCCESS) {
			logger.info("Failed to lock account for: " + accountUUID + ", error code=" + lock + ", " + 
						Codes.getErrorStringForCode((int)lock));
			toReturn.setResponseCode((int)lock);
			return toReturn;
		} else {
			toReturn.setResponseCode(Codes.SUCCESS);
			toReturn.setLockTime(lock);
		}
		
		logger.fine("Retrieving accounts for user: " + accountUUID);
		List<Account> currentAccounts = dataStore.getAccountsForUser(accountUUID);
		
		if (currentAccounts == null) {
			logger.warning("Error getting accounts for: " + accountUUID);
			toReturn.setResponseCode(Codes.ERROR);
			unlockAccount(accountUUID);
			return toReturn;
		}
		
		List<String> sendAccountsToServerList = new ArrayList<>();
		List<Account> accountsToSendBackToClient = new ArrayList<>();
		
		// this will check client version to server version and update both send back lists as needed
		for (Account account : currentAccounts) {
			//if (accounts.contains(account)) {
			if (accounts.containsKey(account.getAccountName())) {
				// both server and client have account, check update times
				long checkAccountUpdateTime = accounts.get(account.getAccountName()).getUpdateTime();
						
				if (checkAccountUpdateTime > account.getUpdateTime()) {
					// have client send its version since it is newer
					logger.finest("Adding account: " + account.getAccountName() + " to list to send to server");
					sendAccountsToServerList.add(account.getAccountName());
				} else if (checkAccountUpdateTime < account.getUpdateTime()) {
					// send server version back to client since it is newer
					logger.finest("Adding account: " + account.getAccountName() + " to list to send to client");
					accountsToSendBackToClient.add(account);
				} else {
					// do nothing since they are both up to date
					logger.finest("Aaccount: " + account.getAccountName() + " is up to date");
				}
				
				// remove the account from the list the client sent to loop through later
				accounts.remove(account.getAccountName());
			} else {
				// client does not have account so add it
				logger.fine("Adding account: " + account.getAccountName() + ", to send back to client");
				accountsToSendBackToClient.add(account);
			}
		}
		
		// this will request the client to send any new account the server does not have
		Iterator<String> it = accounts.keySet().iterator();
		while (it.hasNext()) {
			String accountName = it.next();
			logger.fine("Adding account: " + accountName + ", to send back to client");
			sendAccountsToServerList.add(accountName);
		}
		
		toReturn.setAccountsToSendBackToClient(accountsToSendBackToClient);
		toReturn.setSendAccountsToServerList(sendAccountsToServerList);
		toReturn.setResponseCode(Codes.SUCCESS);
		
		return toReturn;
	}
	
	
	public int syncFinal(String accountUUID, String accountPassword, long lockTime, Map<String, Account> accountsFromClient) {
		int toReturn = Codes.SUCCESS;
		int lock = checkLockForAccountUUID(accountUUID, accountPassword, lockTime);
		
		if (lock != Codes.SUCCESS) {
			logger.info("Failed to verify lock account for: " + accountUUID + ", error code=" + lock);
			toReturn = lock;
			return toReturn;
		}
		
		logger.fine("Retrieving accounts for user: " + accountUUID);
		List<Account> currentAccounts = dataStore.getAccountsForUser(accountUUID);
		
		if (currentAccounts == null) {
			logger.warning("Error getting accounts for: " + accountUUID);
			toReturn = Codes.ERROR;
			unlockAccount(accountUUID);
			return toReturn;
		}
		
		List<Account> accountsToUpdate = new ArrayList<>();
		List<Account> accountsToCreate = new ArrayList<>();
		
		if (accountsFromClient != null && !accountsFromClient.isEmpty()) {
			for (Account account : currentAccounts) {
				if (accountsFromClient.containsKey(account.getAccountName())) {
					accountsToUpdate.add(accountsFromClient.remove(account.getAccountName()));
				} 
			}
			
			Iterator<String> it = accountsFromClient.keySet().iterator();
			while (it.hasNext()) {
				accountsToCreate.add(accountsFromClient.get(it.next()));
			}
		}
		
		if (!accountsToCreate.isEmpty()) {
			if (!dataStore.createAccounts(accountUUID, accountsToCreate)) {
				toReturn = Codes.ERROR;
			} else {
				toReturn = Codes.SUCCESS;
			}
		}
		
		if (toReturn == Codes.SUCCESS && !accountsToUpdate.isEmpty()) {
			if (!dataStore.updateAccounts(accountUUID, accountsToUpdate)) {
				toReturn = Codes.ERROR;
			} else {
				toReturn = Codes.SUCCESS;
			}
		}
		
		if (toReturn == Codes.SUCCESS) {
			dataStore.updateUserLastSync(accountUUID, System.currentTimeMillis());
			logger.info("Successfully synced accounts for user: " + accountUUID);
		}
		
		unlockAccount(accountUUID);
		return toReturn;
	}
	
	
	private void loadAccountsMap(Map<String, User> map) throws Exception {
		List<User> accountsList = dataStore.getUsers();
		
		for (User account : accountsList) {
			map.put(account.getAccountUUID(), account);
		}
	}
	
	
	/*
	 * dont believe I will need to match lock time so just unlock, may need to revisit
	 */
	private void unlockAccount(String accountName) {
		synchronized (accountMap) {
			User account = accountMap.get(accountName);
			
			if (account != null) {
				account.setLocked(false);
				account.setLockedTime(0L);
			}
		}
	}
	
	/*
	 * calls lock, but will keep retrying until it becomes unlocked
	 */
	private long lockAccountUUIDBlocking(String accountUUID, String password) {
		long toReturn = lockAccountUUID(accountUUID, password);
		
		while (toReturn == Codes.ACCOUNT_ALREADY_LOCKED) {
			logger.info("Waiting for account: " + accountUUID + ", to become unlocked");
			
			try {
				Thread.sleep(1_000L);
			} catch (InterruptedException e) { e.printStackTrace(); }
			
			toReturn = lockAccountUUID(accountUUID, password);
		}
		
		return toReturn;
	}
	
	
	/*
	 * lock/autheticate an account. If the account is locked checkes the locked time
	 * and does an auto release if account was locked longer the max lock time.
	 */
	private long lockAccountUUID(String accountUUID, String password) {
		long toReturn = Codes.ERROR;
		
		if (!initialized)
			return Codes.SERVER_NOT_INITIALIZED;
		
		synchronized (accountMap) {
			User account = accountMap.get(accountUUID);
			
			if (account == null) {
				toReturn = Codes.ACCOUNT_DOES_NOT_EXIST;
			} else {
				if (account.isLocked()) {
					if (System.currentTimeMillis() - account.getLockedTime() > MAX_LOCK_TIME) {
						// account will be unlocked
						account.setLocked(false);
						/*
						 *  don't reset lock time, if xaction is still happening and no other request comes in
						 *  for the account then let it complete, no harm.
						 */
					} else {
						toReturn = Codes.ACCOUNT_ALREADY_LOCKED;
					}
				} 
				
				if (toReturn != Codes.ACCOUNT_ALREADY_LOCKED) {
					// verify password
				    if (verifyPassword(account.getAccountPassword(), password)) {
						account.setLocked(true);
						account.setLockedTime(System.currentTimeMillis());
						//toReturn = Codes.SUCCESS;
						toReturn = account.getLockedTime();
				    } else {
				    		toReturn = Codes.INVALID_PASSWORD;
				    }
				}
			}
		}
		
		return toReturn;
	}
	
	
	/*
	 * used to verify that an account is still locked and that the lock time matches lockTime
	 * still verify username/password. 
	 * if account locktime is past max but the last lock time still matches this lock time
	 * then no other transactions for this account came is so safe to continue
	 * Adds 30 seconds to lock time
	 */
	private int checkLockForAccountUUID(String accountUUID, String password, long lockTime) {
		int toReturn = Codes.ERROR;
		
		synchronized (accountMap) {
			User account = accountMap.get(accountUUID);
			
			if (account == null) {
				toReturn = Codes.ACCOUNT_DOES_NOT_EXIST;
			} else {
				if (account.getLockedTime() == lockTime) {
					// verify password
				    if (verifyPassword(account.getAccountPassword(), password)) {
						account.setLocked(true);
						account.setLockedTime(System.currentTimeMillis());
						toReturn = Codes.SUCCESS;
				    } else {
				    		toReturn = Codes.INVALID_PASSWORD;
				    }
				} else {
					logger.info("Account: " + accountUUID + ", possible slow final response, lock times don't match");
					toReturn = Codes.ERROR;
				}
			}
		}
		
		return toReturn;
	}
	
	
	private boolean verifyPassword(String accountPassword, String passwordToCheck) {
		boolean toReturn;
		
		String passwordHEX = hashPassword(passwordToCheck);
	    
	    if (passwordHEX.equals(accountPassword)) {
			toReturn = true;
	    } else {
	    		toReturn = false;
	    }
	    
	    return toReturn;
	}
	
	
	private String hashPassword(String password) {
		SHA3.DigestSHA3 digestSHA3 = new SHA3.Digest512();
	    byte[] digest = digestSHA3.digest(password.getBytes());
	    return (Hex.toHexString(digest));
	}
	
	
	
	public static class Codes {
		public static final int SUCCESS = 0;
		public static final int ERROR = 1;
		public static final int ACCOUNT_ALREADY_EXISTS = 2;
		public static final int ACCOUNT_DOES_NOT_EXIST = 3;
		public static final int ACCOUNT_ALREADY_LOCKED = 4;
		public static final int INVALID_PASSWORD = 5;
		public static final int BACK_END_TIMEOUT = 6;
		public static final int SERVER_NOT_INITIALIZED = 7;
		public static final int ACCOUNT_ADDED = 8;
		public static final int LOCK_SUCCESS = 100;
		
		public static String getErrorStringForCode(int code) {
			switch (code) {
			case SUCCESS:
				return "The operation completed successfully";
			case ERROR:
				return "There was an error in the operation";
			case ACCOUNT_ALREADY_EXISTS:
				return "Account name already exists";
			case ACCOUNT_DOES_NOT_EXIST:
				return "Account name does not exist";
			case ACCOUNT_ALREADY_LOCKED:
				return "The account is locked by another request";
			case INVALID_PASSWORD:
				return "Invalid password";
			case BACK_END_TIMEOUT:
				return "The operation timed out";
			case SERVER_NOT_INITIALIZED:
				return "The Sync Server is not ready to accept requests";
			case ACCOUNT_ADDED:
				return "The account was added";
			case LOCK_SUCCESS:
				return "The account has been locked";
			default:
				return "Unkown result code";
			}
		}
	}
	
	
}
