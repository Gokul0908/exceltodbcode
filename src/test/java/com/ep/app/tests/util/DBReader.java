package com.ep.app.tests.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DBReader {

	public static List<String> getTestCaseIDs(String sheetName) {

		List<String> ids = new ArrayList<>();

		String sql = """
				    SELECT case_id
				    FROM %s
				    WHERE sheet_name = ?
				      AND is_run = 'YES'
				""".formatted(DBConfig.getTableName()
);

		try (Connection con = DriverManager.getConnection(DBConfig.DB_URL, DBConfig.USER, DBConfig.PASS);
				PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setString(1, sheetName);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					ids.add(rs.getString("case_id"));
				}
			}

		} catch (Exception e) {
			throw new RuntimeException("Failed to read test case IDs from DB", e);
		}

		return ids;
	}

	public static List<String> getChainingGroupIDs(String sheetName) {

		List<String> groupIds = new ArrayList<>();

		String sql = """
				    SELECT DISTINCT case_id
				    FROM %s
				    WHERE sheet_name = ?
				      AND is_run = 'YES'
				""".formatted(DBConfig.getTableName()
);

		try (Connection con = DriverManager.getConnection(DBConfig.DB_URL, DBConfig.USER, DBConfig.PASS);
				PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setString(1, sheetName);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					groupIds.add(rs.getString("case_id"));
				}
			}

		} catch (Exception e) {
			throw new RuntimeException("Failed to read chaining group IDs from DB", e);
		}

		return groupIds;
	}
}
