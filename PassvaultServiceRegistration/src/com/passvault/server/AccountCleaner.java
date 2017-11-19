package com.passvault.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.sql.DataSource;

public class AccountCleaner {

	private DataSource dataSource;
	
	
	private static Logger logger;
	
	static {
		logger = Logger.getLogger("com.passvault.server"); 
	}
	
	public AccountCleaner() {
		
		try {
			InitialContext cxt = new InitialContext();
			dataSource = (DataSource) cxt.lookup( "java:/comp/env/jdbc/postgres");
		} catch(Exception e) {
			logger.severe("Unable to create datasource to database: " + e.getMessage());
			e.printStackTrace();
		}
		
		logger.info("Starting DB Cleaner Timer");
		Timer timer = new Timer("DB Cleaner Timer Thread", true);
		CleanerTask task = new CleanerTask(dataSource);
		timer.schedule(task, Calendar.getInstance().getTime(), 86_400_000L);
		//timer.schedule(task, Calendar.getInstance().getTime(), 60_000L);
	}
	
	
	
	private class CleanerTask extends TimerTask {

		private static final String DELETE_SQL = "DELETE FROM Accounts where update_time < ? AND deleted = 't'";
		private static final long PURGE_AFTER_SECONDS_SQL = 86_400_000 * 30L;  // 30 days
		private DataSource dataSource;
		
		public CleanerTask(DataSource dataSource) {
			this.dataSource = dataSource;
		}
		
		@Override
		public void run() {
			logger.info("Cleaner Task Starting");
			long removeAccountsOlderThan = System.currentTimeMillis() - PURGE_AFTER_SECONDS_SQL;
			
			if (dataSource != null) {
				try (
					Connection conn = dataSource.getConnection();
					PreparedStatement pst = conn.prepareStatement(DELETE_SQL);
					) {
					
					pst.setLong(1, removeAccountsOlderThan);
					
					if (!pst.execute())
						logger.info(pst.getUpdateCount() + " Accounts purged");
					else
						logger.info("0 Accounts purged");
					
				} catch(SQLException e) {
					logger.warning("Error running cleaner: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		
	}
}
