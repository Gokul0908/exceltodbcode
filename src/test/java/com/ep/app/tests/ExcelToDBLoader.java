package com.ep.app.tests;

import java.sql.Connection;

public class ExcelToDBLoader {

	public static void loadExcelDataIntoDB(String excelPath, Connection con) {
		try {
			con.createStatement().execute("TRUNCATE TABLE api");
			ExcelReaderUtil.readAndInsert(excelPath, con);
			con.commit();
			con.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
