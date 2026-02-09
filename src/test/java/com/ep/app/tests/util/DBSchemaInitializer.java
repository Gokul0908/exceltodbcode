package com.ep.app.tests.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DBSchemaInitializer {

	public static void initSchemaIfNeeded() {

		if (DBConfig.CREATE_DB_IF_NOT_EXISTS) {
			createDatabaseIfNeeded();
		}

		if (DBConfig.CREATE_TABLE_IF_NOT_EXISTS) {
			createTableIfNotExists();
		}
	}

	public static void createDatabaseIfNeeded() {
		String sql = "CREATE DATABASE IF NOT EXISTS " + DBConfig.DB_NAME;

		try (Connection con = DriverManager.getConnection(DBConfig.ROOT_URL, DBConfig.USER, DBConfig.PASS);
				Statement st = con.createStatement()) {

			st.execute(sql);
			System.out.println("--->DB verified / Created: " + DBConfig.DB_NAME);

		} catch (Exception e) {
			throw new RuntimeException("--->DB creation failed", e);
		}
	}

	public static void createTableIfNotExists() {

		String table = DBConfig.getTableName();

		String sql = """
				    CREATE TABLE IF NOT EXISTS %s (
				        id INT AUTO_INCREMENT PRIMARY KEY,
				        run_id INT,
				        run_line_no INT,
				        sheet_name VARCHAR(100),
				        case_id VARCHAR(100),
				        is_run VARCHAR(10),
				        base_url VARCHAR(500),
				        end_point VARCHAR(500),
				        basic_auth VARCHAR(500),
				        api_key VARCHAR(500),
				        auth_type VARCHAR(100),
				        generated_token TEXT,
				        action VARCHAR(50),
				        query_param VARCHAR(200),
				        query_param_value VARCHAR(500),
				        verification_param VARCHAR(200),
				        verification_param_value VARCHAR(500),
				        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
				    )
				""".formatted(table);

		try (Connection con = DriverManager.getConnection(DBConfig.DB_URL, DBConfig.USER, DBConfig.PASS);
				Statement st = con.createStatement()) {

			st.execute(sql);
			System.out.println("--->Table ready: " + table);

		} catch (Exception e) {
			throw new RuntimeException("--->Table creation failed", e);
		}
	}
}
