package com.passvault.server.data;

/*
 * Nothing but a bunch of constants for table column names
 */
public class Tables {
	// Accounts Table
	public static final String ACCOUNTS_ACCOUNT_NAME = "account_name";
	public static final String ACCOUNTS_ACCOUNT_UUID = "account_uuid";
	public static final String ACCOUNTS_USER_NAME = "user_name";
	public static final String ACCOUNTS_PASSWORD = "password";
	public static final String ACCOUNTS_OLD_PASSWORD = "old_password";
	public static final String ACCOUNTS_URL = "url";
	public static final String ACCOUNTS_UPDATE_TIME = "update_time";
	public static final String ACCOUNTS_DELETED = "deleted";
	public static final int ACCOUNTS_ACCOUNT_NAME_POS = 1;
	public static final int ACCOUNTS_ACCOUNT_UUID_POS = 2;
	public static final int ACCOUNTS_USER_NAME_POS = 3;
	public static final int ACCOUNTS_PASSWORD_POS = 4;
	public static final int ACCOUNTS_OLD_PASSWORD_POS = 5;
	public static final int ACCOUNTS_URL_POS = 6;
	public static final int ACCOUNTS_UPDATE_TIME_POS = 7;
	public static final int ACCOUNTS_DELETED_POS = 8;
	
	// Users Table
	public static final String USERS_ACCOUNT_UUID = "account_uuid";
	public static final String USERS_ACCOUNT_PASSWORD = "account_password";
	public static final String USERS_ACCOUNT_LAST_SYNC = "account_last_sync";
	public static final String USERS_ACCOUNT_FORMAT = "account_format";
	public static final int USERS_ACCOUNT_UUID_POS = 1;
	public static final int USERS_ACCOUNT_PASSWORD_POS = 2;
	public static final int USERS_ACCOUNT_LAST_SYNC_POS = 3;
	public static final int USERS_ACCOUNT_FORMAT_POS = 4;
	
}
