package com.ep.app.tests;

import java.sql.*;

public class DBUtil {

	private static final String URL = "jdbc:mysql://localhost:3306/apidb";
	private static final String USER = "root"; // change if needed
	private static final String PASS = "Database@123";

	public static void insertApiData(String sheetName, String caseId, String isRun, String baseURL, String endPoint,
			String basicAuth, String apiKey, String authType, String generatedToken, String action, String queryParam,
			String queryParamValue, String verificationParam, String verificationParamValue) {

		String sql = """
				    INSERT INTO api_data
				    (sheet_name, case_id, is_run, base_url, end_point,
				     basic_auth, api_key, auth_type, generated_token,
				     action, query_param, query_param_value,
				     verification_param, verification_param_value)
				    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
				""";

		try (Connection con = DriverManager.getConnection(URL, USER, PASS);
				PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setString(1, sheetName);
			ps.setString(2, caseId);
			ps.setString(3, isRun);
			ps.setString(4, baseURL);
			ps.setString(5, endPoint);
			ps.setString(6, basicAuth);
			ps.setString(7, apiKey);
			ps.setString(8, authType);
			ps.setString(9, generatedToken);
			ps.setString(10, action);
			ps.setString(11, queryParam);
			ps.setString(12, queryParamValue);
			ps.setString(13, verificationParam);
			ps.setString(14, verificationParamValue);

			ps.executeUpdate();

		} catch (Exception e) {
			throw new RuntimeException("DB insert failed", e);
		}
	}
}
