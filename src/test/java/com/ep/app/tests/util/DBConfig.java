package com.ep.app.tests.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DBConfig {

	public static final boolean CREATE_DB_IF_NOT_EXISTS = true;
	public static final boolean CREATE_TABLE_IF_NOT_EXISTS = true;
	public static final boolean USE_DATE_BASED_TABLE = false;

	public static final String DB_NAME = "apimaindb";
	public static final String TABLE_NAME = "apimaintable";
	public static final String USER = "root";
	public static final String PASS = "Database@123";

	public static final String ROOT_URL = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true"; //---> TO create New database

	public static final String DB_URL = "jdbc:mysql://localhost:3306/" + DB_NAME  //---> To create New Table
			+ "?useSSL=false&allowPublicKeyRetrieval=true";

	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd_MM_yyyy");

	public static String getTableName() {
		if (USE_DATE_BASED_TABLE) {
			return TABLE_NAME + "_" + LocalDate.now().format(FMT);
		}
		return TABLE_NAME;
	}
}
