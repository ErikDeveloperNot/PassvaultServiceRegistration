package com.passvault.server.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.passvault.util.model.syncserver.Account;
import com.passvault.util.model.syncserver.User;

public class PostgresStore implements DataStore {
	
	private DataSource dataSource;
	
	// Queries
	private static final String GET_USERS_SQL = "SELECT * FROM users";
	private static final String CREATE_USER_SQL = "INSERT INTO users VALUES (?, ?, ?, ?)";
	private static final String DELETE_USER_SQL = "DELETE from users WHERE account_uuid = ?";
	private static final String UPDATE_USER_LAST_SYNC = "UPDATE Users SET account_last_sync = ? WHERE account_uuid = ?";
	private static final String GET_ACCOUNTS_FOR_USER_SQL = "SELECT * FROM accounts where account_uuid = ?";
	private static final String CREATE_ACCOUNT = "INSERT INTO Accounts VALUES (?, ?, ?, ?, ? ,?, ?, ?)";
	private static final String UPDATE_ACCOUNT = "UPDATE Accounts SET user_name = ?, password = ?, old_password = ?, " +
												"url = ?, update_time = ?, deleted = ? WHERE account_UUID = ? " + 
												"AND account_name = ?";
												
	
	private static Logger logger;
	
	static {
		logger = Logger.getLogger("com.passvault.server.data");
	}
	
	public PostgresStore() {
		
	}

	@Override
	public List<User> getUsers() throws Exception {
		
		if (getDataSource() == null) {
			logger.warning("Unable to connect to the datastore");
			throw new Exception("Unable to connect to the datastore");
		}

		List<User> accountsList = new ArrayList<>();
		Connection conn = null;
		Statement statement = null;
		ResultSet rs = null;
		
		try {
			conn = dataSource.getConnection();
			statement = conn.createStatement();
			rs = statement.executeQuery(GET_USERS_SQL);
			
			while (rs.next()) {
				User toAdd = new User(rs.getString(Tables.USERS_ACCOUNT_UUID), 
									  rs.getString(Tables.USERS_ACCOUNT_PASSWORD), 
									  rs.getLong(Tables.USERS_ACCOUNT_LAST_SYNC), 
									  rs.getFloat(Tables.USERS_ACCOUNT_FORMAT));
				logger.fine("Added account: " + toAdd);
				accountsList.add(toAdd);
			}
			
		} catch (SQLException e) {
			logger.warning("Error getting Acounts: " + e.getMessage());
			e.printStackTrace();
			throw e;
		} finally {
			try {
				rs.close();
				statement.close();
				conn.close();
			} catch (SQLException e1) {}
		}
		
		return accountsList;
	}
	
	
	
	@Override
	public boolean addUser(User user) {
		boolean toReturn;
		logger.info("Attempting to add account: " + user.getAccountUUID());
		
		try (
				Connection conn = dataSource.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(CREATE_USER_SQL);
			){
			
			pstmt.setString(Tables.USERS_ACCOUNT_UUID_POS, user.getAccountUUID());
			pstmt.setString(Tables.USERS_ACCOUNT_PASSWORD_POS, user.getAccountPassword());
			pstmt.setLong(Tables.USERS_ACCOUNT_LAST_SYNC_POS, user.getLastSync());
			pstmt.setFloat(Tables.USERS_ACCOUNT_FORMAT_POS, user.getFormat());
			pstmt.execute();
			
			logger.info("Account: " + user.getAccountUUID() + ", added");
			toReturn = true;
		} catch (SQLException e) {
			logger.warning("Error adding Acount: " + user.getAccountUUID() + ", error" + e.getMessage());
			e.printStackTrace();
			toReturn = false;
		}
		
		return toReturn;
	}

	
	@Override
	public boolean deleteUser(String userName) {
		boolean toReturn;
		logger.info("Attempting to delete account: " + userName);
		
		try ( 
				Connection conn = dataSource.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(DELETE_USER_SQL);
			){
			
			pstmt.setString(1, userName);
			pstmt.execute();
			
			logger.info("Account: " + userName + ", deleted");
			toReturn = true;
		} catch (SQLException e) {
			logger.warning("Error deleting Acount: " + userName + ", error" + e.getMessage());
			e.printStackTrace();
			toReturn = false;
		}
		
		return toReturn;
	}

	
	@Override
	/*
	 * returning null signals a backend error, if there are no accounts then an empty List will be returned
	 */
	public List<Account> getAccountsForUser(String accountUUID) {
		List<Account> accountsList = new ArrayList<>();
		logger.info("Retrieving accounts for user: " + accountUUID);
	
		try (
				Connection conn = dataSource.getConnection();
				PreparedStatement pst = conn.prepareStatement(GET_ACCOUNTS_FOR_USER_SQL);
			){
			
			pst.setString(1, accountUUID);
			
			try (
					ResultSet rs = pst.executeQuery();
				) {
			
				while (rs.next()) {
					Account toAdd = new Account(rs.getString(Tables.ACCOUNTS_ACCOUNT_NAME), 
												rs.getString(Tables.ACCOUNTS_USER_NAME), 
												rs.getString(Tables.ACCOUNTS_PASSWORD), 
												rs.getString(Tables.ACCOUNTS_OLD_PASSWORD), 
												rs.getString(Tables.ACCOUNTS_URL), 
												rs.getLong(Tables.ACCOUNTS_UPDATE_TIME), 
												rs.getBoolean(Tables.ACCOUNTS_DELETED));
					
					logger.fine("Adding account: " + toAdd.getAccountName());
					accountsList.add(toAdd);
				}
			} catch (SQLException e) {
				throw e;
			}
			
		} catch (SQLException e) {
			logger.warning("Error getting Acounts for user: " + accountUUID + ", error: " + e.getMessage());
			e.printStackTrace();
			accountsList = null;
		}
		
		return accountsList;
	}

	
	@Override
	public boolean createAccounts(String accountUUID, List<Account> accountsToCreate) {
		boolean toReturn = true;
		logger.info("Attempting to create " + accountsToCreate.size() + " accounts for user: " + accountUUID);
		
		try (
				Connection conn = dataSource.getConnection();
				PreparedStatement pst = conn.prepareStatement(CREATE_ACCOUNT);
			) {
			
			for (Account account : accountsToCreate) {
				pst.setString(Tables.ACCOUNTS_ACCOUNT_NAME_POS, account.getAccountName());
				pst.setString(Tables.ACCOUNTS_ACCOUNT_UUID_POS, accountUUID);
				pst.setString(Tables.ACCOUNTS_USER_NAME_POS, account.getUserName());
				pst.setString(Tables.ACCOUNTS_PASSWORD_POS, account.getPassword());
				pst.setString(Tables.ACCOUNTS_OLD_PASSWORD_POS, account.getOldPassword());
				pst.setString(Tables.ACCOUNTS_URL_POS, account.getUrl());
				pst.setLong(Tables.ACCOUNTS_UPDATE_TIME_POS, account.getUpdateTime());
				pst.setBoolean(Tables.ACCOUNTS_DELETED_POS, account.isDeleted());
				logger.fine("Adding account " + account.getAccountName() + " to batch");
				pst.addBatch();
			}
			
			logger.info("Executing batch");
			pst.executeBatch();
		} catch (SQLException e) {
			logger.warning("Error creating Acounts for user: " + accountUUID + ", error: " + e.getMessage());
			e.printStackTrace();
			toReturn = false;
		}
		
		return toReturn;
	}

	
	@Override
	public boolean updateAccounts(String accountUUID, List<Account> accountsToUpdate) {
		boolean toReturn = true;
		logger.info("Attempting to update " + accountsToUpdate.size() + " accounts for user: " + accountUUID);
		
		try (
				Connection conn = dataSource.getConnection();
				PreparedStatement pst = conn.prepareStatement(UPDATE_ACCOUNT);
			) {
			
			for (Account account : accountsToUpdate) {
				pst.setString(1, account.getUserName());
				pst.setString(2, account.getPassword());
				pst.setString(3, account.getOldPassword());
				pst.setString(4, account.getUrl());
				pst.setLong(5, account.getUpdateTime());
				pst.setBoolean(6, account.isDeleted());
				pst.setString(7, accountUUID);
				pst.setString(8, account.getAccountName());
				logger.fine("Adding account " + account.getAccountName() + " to batch");
				pst.addBatch();
			}
			
			logger.info("Executing batch");
			pst.executeBatch();
		} catch (SQLException e) {
			logger.warning("Error updating Acounts for user: " + accountUUID + ", error: " + e.getMessage());
			e.printStackTrace();
			toReturn = false;
		}
		
		return toReturn;
	}

	
	@Override
	public boolean updateUserLastSync(String accountUUID, long time) {
		boolean toReturn = true;
		logger.fine("Attempting to update last sync for user " + accountUUID);
		
		try (
				Connection conn = dataSource.getConnection();
				PreparedStatement pst = conn.prepareStatement(UPDATE_USER_LAST_SYNC);
			) {
			
			pst.setLong(1, time);
			pst.setString(2, accountUUID);
			pst.execute();
			
		} catch (SQLException e) {
			logger.warning("Error updating last sync for user: " + accountUUID + ", error: " + e.getMessage());
			e.printStackTrace();
			toReturn = false;
		}
		
		return toReturn;
	}

	private DataSource getDataSource() {
		
		try {
			if (dataSource == null) {
				InitialContext cxt = new InitialContext();
				dataSource = (DataSource) cxt.lookup( "java:/comp/env/jdbc/postgres");
				return dataSource;
			} else {
				return dataSource;
			}
		} catch(Exception e) {
			logger.severe("Unable to create datasource to database: " + e.getMessage());
			e.printStackTrace();
		}
		
		return null;
	}

}
