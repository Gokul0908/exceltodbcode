package com.ep.app.tests;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class ExcelReaderUtil {

    public static void readAndInsert(String excelPath, Connection con) throws Exception {

        String sql = """
            INSERT INTO api
            (caseID, isRun, baseURL, endPoint, basicAuth, apiKey, authType,
             generatedToken, action, queryParam, queryParamValue,
             verificationParam, verificationParamValue)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        PreparedStatement ps = con.prepareStatement(sql);

        Workbook wb = WorkbookFactory.create(new File(excelPath));
        Sheet sheet = wb.getSheetAt(0);

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r == null) continue;

            ps.setString(1,  r.getCell(0).getStringCellValue()); // caseID
            ps.setString(2,  r.getCell(1).getStringCellValue()); // isRun
            ps.setString(3,  r.getCell(2).getStringCellValue()); // baseURL
            ps.setString(4,  r.getCell(3).getStringCellValue()); // endPoint
            ps.setString(5,  r.getCell(4).getStringCellValue()); // basicAuth
            ps.setString(6,  r.getCell(5).getStringCellValue()); // apiKey
            ps.setString(7,  r.getCell(6).getStringCellValue()); // authType
            ps.setString(8,  r.getCell(7).getStringCellValue()); // generatedToken
            ps.setString(9,  r.getCell(8).getStringCellValue()); // action
            ps.setString(10, r.getCell(9).getStringCellValue()); // queryParam
            ps.setString(11, r.getCell(10).getStringCellValue()); // queryParamValue
            ps.setString(12, r.getCell(11).getStringCellValue()); // verificationParam
            ps.setString(13, r.getCell(12).getStringCellValue()); // verificationParamValue

            ps.addBatch();
        }

        ps.executeBatch();
        wb.close();
    }
}
