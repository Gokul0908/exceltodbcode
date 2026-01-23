package com.ep.app.tests;

import java.sql.*;
import java.util.*;

public class DBReader {

	public static List<String> getTestCaseIDs(String sheetName) {

		List<String> ids = new ArrayList<>();

		try {
			Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/apidb", "root", "Database@123");

			PreparedStatement ps = con
					.prepareStatement("SELECT case_id FROM api_data WHERE sheet_name=? AND is_run='YES'");
			ps.setString(1, sheetName);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ids.add(rs.getString("case_id"));
			}
			con.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return ids;
	}

	public static List<String> getChainingGroupIDs(String sheetName) {

		List<String> groupIds = new ArrayList<>();

		try {
			Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/apidb", "root", "Database@123");

			PreparedStatement ps = con
					.prepareStatement("SELECT DISTINCT case_id FROM api_data " + "WHERE sheet_name=? AND is_run='YES'");
			ps.setString(1, sheetName);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				groupIds.add(rs.getString("case_id"));
			}

			con.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return groupIds;
	}
}
