package com.ep.app.tests.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DBSchemaInitializer {

	public static void createTableIfNotExists() {

		String url = "jdbc:mysql://localhost:3306/apidb";
		String user = "root";
		String pass = "Database@123";

		String sql = """
				    CREATE TABLE IF NOT EXISTS api_data (
				        id INT AUTO_INCREMENT PRIMARY KEY,

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
				""";

		try (Connection con = DriverManager.getConnection(url, user, pass); Statement stmt = con.createStatement()) {

			stmt.execute(sql);
			System.out.println("DB table verified / created");

		} catch (Exception e) {
			throw new RuntimeException("DB schema creation failed", e);
		}
	}
}
