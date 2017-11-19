package com.passvault.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class PassvaultSyncServerServletContextListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent ctx) {
		System.out.println("Servlet context destroyed");

	}

	@Override
	public void contextInitialized(ServletContextEvent ctx) {
		System.out.println("Servlet context creating instance of PassvaultSyncServer");
		// initialize server
		PassvaultSyncServer.initialize();
		// start db cleaner
		new AccountCleaner();
		
		//int result = -1;
		
		/*
		do {
			
			result = PassvaultSyncServer.getInstance().createAccountUUID("test@mail.com", 
					"password");
			
			System.out.println("account created=" + result);
			
			result = PassvaultSyncServer.getInstance().deleteAccountUUID("test@mail9.com", 
					"password");
			System.out.println("account deleted=" + result);
			
			
			
			Account account_name_4 = new Account("account_name_4", "user", "password", "password", "www.yahoo.com", System.currentTimeMillis(), false);
			Account NewAccount = new Account("NewAccount3", "user", "password", "password", "www.yahoo.com", System.currentTimeMillis(), false);
			
			Map<String, CheckAccount> checkAccounts = new HashMap<>();
			checkAccounts.put(NewAccount.getAccountName(), new CheckAccount(NewAccount.getAccountName(), NewAccount.getUpdateTime()));
			checkAccounts.put("account_name_4", new CheckAccount("account_name_4", account_name_4.getUpdateTime()));
			checkAccounts.put("account_name_5", new CheckAccount("account_name_5", 151029257006L));
			checkAccounts.put("account_name_6", new CheckAccount("account_name_6", 1510292570062L));
			SyncResponseInitial resp = PassvaultSyncServer.getInstance().syncInitial("Account_10@mail.com", "password", checkAccounts);
			System.out.println("code=" + resp.getResponseCode() + ", lock=" + resp.getLockTime() + ", sendback=" + resp.getAccountsToSendBackToClient() + ", clientSendToServer=" + resp.getSendAccountsToServerList());
			
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					System.out.println("Initial Response=" + PassvaultSyncServer.getInstance().syncInitial("Account_10@mail.com", "password", new HashMap<String, CheckAccount>()));
					
				}
			}).start();
			//System.out.println("Initial Response=" + PassvaultSyncServer.getInstance().syncInitial("Account_10@mail.com", "password", new HashMap<String, CheckAccount>()));
			
			try {
				Thread.sleep(8_000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (resp.getResponseCode() == 0) {
				Map<String, Account> accountToSendMap = new HashMap<>();
				accountToSendMap.put("account_name_4", account_name_4);
				accountToSendMap.put(NewAccount.getAccountName(), NewAccount);
				System.out.println(">>>>> SyncFinal resp = " + PassvaultSyncServer.getInstance().syncFinal("Account_10@mail.com", "password", resp.getLockTime(), accountToSendMap));
			}
			
			
			
			
			
			System.out.println("Initial Response=" + PassvaultSyncServer.getInstance().syncInitial("Account_99@mail.com", "password", new HashMap<String, CheckAccount>()));
			
			
			result = PassvaultSyncServer.getInstance().deleteAccountUUID("Account_3@mail.com", "password");
			System.out.println("account deleted=" + result);
			
			
			
			
		} while (result > 80); //!= PassvaultSyncServer.Codes.SUCCESS);
		*/
		
		
	}

}
