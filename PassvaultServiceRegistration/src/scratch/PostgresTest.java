package scratch;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class PostgresTest {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		String url = "jdbc:postgresql://192.168.56.102/passvault";
		Properties props = new Properties();
		props.setProperty("user","passvault-user");
		props.setProperty("password","passvault-secret");
		//props.setProperty("loglevel", );
		Connection conn = DriverManager.getConnection(url, props);
		
		//Statement st1 = conn.createStatement();
		//ResultSet rs1 = st1.executeQuery("INSERT into users values('Admin', 'password', 0000000, 01.00)");
		

		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("SELECT account_uuid FROM users limit 1");
		while (rs.next())
		{
			System.out.println(rs.getString(1));
			//System.out.println(rs.getString(1) +":" + rs.getString(2) + ":" + rs.getLong(3) + ":" + rs.getFloat(4));
		} 
		rs.close();
		st.close(); 
		
		// add some test data
		createTestData(conn);
		
		conn.close();
	}
	
	
	static void createTestData(Connection conn) {
		// create users
		for (int i=1; i<=10; i++) {
			try {
				PreparedStatement st = conn.prepareStatement("INSERT INTO users values (?, ?, ?, ?)");
				st.setString(1, "Account_" + i + "@mail.com");
				st.setString(2, "e9a75486736a550af4fea861e2378305c4a555a05094dee1dca2f68afea49cc3a50e8de6ea131ea521311f4d6fb054a146e8282f8e35ff2e6368c1a62e909716");
				st.setLong(3, 0L);
				st.setFloat(4, 1.00F);
				st.execute();
				
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		// create accounts
		for (int i=1; i<=10; i++) {
			for (int j=1; j<=i; j++) {
				try {
					PreparedStatement st = conn.prepareStatement("INSERT INTO accounts values (?, ?, ?, ?, ?, ?, ?, ?)");
					st.setString(1, "account_name_" + j);
					st.setString(2, "Account_" + i + "@mail.com");
					st.setString(3, "user");
					st.setString(4, "e9a75486736a550af4fea861e2378305c4a555a05094dee1dca2f68afea49cc3a50e8de6ea131ea521311f4d6fb054a146e8282f8e35ff2e6368c1a62e909716");
					st.setString(5, "e9a75486736a550af4fea861e2378305c4a555a05094dee1dca2f68afea49cc3a50e8de6ea131ea521311f4d6fb054a146e8282f8e35ff2e6368c1a62e909716");
					st.setString(6, "www.yahoo.com");
					st.setLong(7, System.currentTimeMillis());
					st.setBoolean(8, false);
					st.execute();

					st.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	
}
