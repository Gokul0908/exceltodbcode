package com.ep.app.tests.util;

public class DBConfig {

	public static final String DB_NAME = "apidb";
	public static final String TABLE_NAME = "api_data";

	public static final String USER = "root";
	public static final String PASS = "Database@123";

	public static final String ROOT_URL = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true";

	public static final String DB_URL = "jdbc:mysql://localhost:3306/" + DB_NAME
			+ "?useSSL=false&allowPublicKeyRetrieval=true";
}
