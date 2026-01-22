package com.ep.app.tests;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class DBDataProvider {

	public static Map<String, String> getTestDataFromDB(String caseID) {

		Map<String, String> data = new HashMap<>();

		String sql = "SELECT * FROM api WHERE caseID = ?";

		try (Connection con = DBConnectionManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setString(1, caseID);
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				data.put("caseID", rs.getString("caseID"));
				data.put("isRun", rs.getString("isRun"));
				data.put("baseURL", rs.getString("baseURL"));
				data.put("endPoint", rs.getString("endPoint"));
				data.put("basicAuth", rs.getString("basicAuth"));
				data.put("apiKey", rs.getString("apiKey"));
				data.put("authType", rs.getString("authType"));
				data.put("generatedToken", rs.getString("generatedToken"));
				data.put("action", rs.getString("action"));
				data.put("queryParam", rs.getString("queryParam"));
				data.put("queryParamValue", rs.getString("queryParamValue"));
				data.put("payLoad", rs.getString("payLoad"));
				data.put("verificationParam", rs.getString("verificationParam"));
				data.put("verificationParamValue", rs.getString("verificationParamValue"));
			}

		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch test data from DB for caseID: " + caseID, e);
		}

		return data;
	}
}
