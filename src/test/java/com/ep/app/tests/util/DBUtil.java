package com.ep.app.tests.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBUtil {

	public static int getNewRunId() {

		String sql = "SELECT COALESCE(MAX(run_id), 0) + 1 FROM " + DBConfig.getTableName();

		try (Connection con = DriverManager.getConnection(DBConfig.DB_URL, DBConfig.USER, DBConfig.PASS);
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate new run_id", e);
		}
		return 1;
	}

	public static void insertApiData(int runId, int runLineNo, String sheetName, String caseId, String isRun,
			String baseURL, String endPoint, String basicAuth, String apiKey, String authType, String generatedToken,
			String action, String queryParam, String queryParamValue, String verificationParam,
			String verificationParamValue) {

		String sql = """
				    INSERT INTO %s (
				        run_id,
				        run_line_no,
				        sheet_name,
				        case_id,
				        is_run,
				        base_url,
				        end_point,
				        basic_auth,
				        api_key,
				        auth_type,
				        generated_token,
				        action,
				        query_param,
				        query_param_value,
				        verification_param,
				        verification_param_value
				    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
				""".formatted(DBConfig.getTableName());

		try (Connection con = DriverManager.getConnection(DBConfig.DB_URL, DBConfig.USER, DBConfig.PASS);
				PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, runId);
			ps.setInt(2, runLineNo);
			ps.setString(3, sheetName);
			ps.setString(4, caseId);
			ps.setString(5, isRun);
			ps.setString(6, baseURL);
			ps.setString(7, endPoint);
			ps.setString(8, basicAuth);
			ps.setString(9, apiKey);
			ps.setString(10, authType);
			ps.setString(11, generatedToken);
			ps.setString(12, action);
			ps.setString(13, queryParam);
			ps.setString(14, queryParamValue);
			ps.setString(15, verificationParam);
			ps.setString(16, verificationParamValue);

			ps.executeUpdate();

		} catch (Exception e) {
			throw new RuntimeException("DB insert failed", e);
		}
	}
}
