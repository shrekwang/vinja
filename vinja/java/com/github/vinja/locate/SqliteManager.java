package com.github.vinja.locate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqliteManager {

	private String dbPath;
	

	public SqliteManager(String dbPath) {
		this.dbPath = dbPath;
	}

	public Connection getConnection() {
		Connection conn = null;
		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return conn;
	}

	public void closeConnection(Connection conn) {
		if (conn == null)
			return;
		try {
			conn.close();
		} catch (Exception e) {
		}
	}
	
	public int[] batchUpdate(String sql , List<String[]> values) {
		Connection conn=null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false); 
			PreparedStatement pstmt = conn.prepareStatement(sql); 
			for (String[] valueArray : values) {
				for (int i=0; i< valueArray.length; i++) {
				    pstmt.setString(i+1, valueArray[i]);
				}
			    pstmt.addBatch(); 
			} 
			int[] updateCounts = pstmt.executeBatch(); 
			conn.commit();
			return updateCounts;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void executeUpdate(String sql) {
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			Statement stat = conn.createStatement();
			stat.executeUpdate(sql);
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.closeConnection(conn);
		}
	}
	
	 public List<String[]> query(String sql) {
	        Connection conn = null;
	        List<String[]> result = new ArrayList<String[]>();
	        try {
	            conn = getConnection();
	            Statement statement = conn.createStatement();
	            ResultSet rs = statement.executeQuery(sql);
	            int columnCount = rs.getMetaData().getColumnCount();
	            while(rs.next()) {
	                String[] values = new String[ columnCount];
	                for (int i=0; i< columnCount; i++) {
	                	values[i] = rs.getString(i+1);
	                }
	                result.add(values);
	            }
	            return result;
	        } catch (Exception e ) {
	            e.printStackTrace();
	        } finally {
	            this.closeConnection(conn);
	        }
	        return result;
	    }

}
