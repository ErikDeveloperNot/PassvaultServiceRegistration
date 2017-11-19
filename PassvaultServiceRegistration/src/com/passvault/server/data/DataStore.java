package com.passvault.server.data;

import java.util.List;

import com.passvault.util.model.syncserver.Account;
import com.passvault.util.model.syncserver.User;

public interface DataStore {

	public List<User> getUsers() throws Exception;
	public boolean addUser(User user);
	public boolean deleteUser(String user);
	public boolean updateUserLastSync(String accountUUID, long time);
	
	public List<Account> getAccountsForUser(String accountUUID);
	//public boolean deleteAccountsForUser(User user);
	
	public boolean createAccounts(String accountUUID, List<Account> accountsToCreate);
	public boolean updateAccounts(String accountUUID, List<Account> accountsToUpdate);
}
