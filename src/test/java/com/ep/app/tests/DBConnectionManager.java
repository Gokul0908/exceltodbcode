package com.ep.app.tests;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnectionManager {

	public static Connection getConnection() {
	    try {
	        Connection con = DriverManager.getConnection(
	            "jdbc:mysql://localhost:3306/apidb",
	            "root",
	            "Database@123"
	        );
	        con.setAutoCommit(false);
	        return con;
	    } catch (Exception e) {
	        throw new RuntimeException("DB connection failed", e);
	    }
	}

}
