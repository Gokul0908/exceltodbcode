package com.ep.app.tests;

import java.sql.Connection;
import java.util.List;

import org.testng.TestNG;
import org.testng.annotations.Test;

import swagger.SwaggerRunner;

public class RunEnabledAPITests {

	@Test
	public void runEnabledTests() {

		// 1️⃣ Detect Swagger mode
		String swaggerExcel = SwaggerRunner.prepareSwaggerExcelIfNeeded();
		boolean isSwaggerMode = swaggerExcel != null;

		// 2️⃣ Decide Excel path
		String excelPath;
		if (isSwaggerMode) {
			excelPath = swaggerExcel;
			System.out.println("Running in SWAGGER mode");
		} else {
			excelPath = "src/test/resources/testData/APIData.xlsx";
			System.out.println("Running in EXCEL mode");
		}

		System.setProperty("EXCEL_PATH", excelPath);

		// 3️⃣ Run tests
		if (isSwaggerMode) {

			// ✅ Swagger → ONLY SwaggerAPIs sheet
			List<String> cases = Common.getAllTestCaseIDs(excelPath, "SwaggerAPIs", "caseID", "isRun");

			Common.generateSwaggerTestNGXml(cases);
			runSuite("src/test/resources/testng-swagger.xml");

		} else {

			String dataSource = "Excel";
			System.out.println("📌 DATA SOURCE = " + dataSource);

			if ("DB".equalsIgnoreCase(dataSource)) {

				// ===== DB MODE =====
				System.setProperty("DB_MODE", "false");

				System.out.println("📊 Running DB → Code");

				List<String> getCases = Common.getAllTestCaseIDsFromDB("GET");
				List<String> postCases = Common.getAllTestCaseIDsFromDB("POST");
				List<String> putCases = Common.getAllTestCaseIDsFromDB("PUT");
				List<String> deleteCases = Common.getAllTestCaseIDsFromDB("DELETE");

				TestNGXmlGenerator.generateTestNGXml(getCases, postCases, putCases, deleteCases);

			} else {

//				// ===== EXCEL MODE (OLD FLOW – UNTOUCHED) =====
				System.setProperty("DB_MODE", "false");

				System.out.println("📘 Running EXCEL → Code");

				List<String> getCases = Common.getAllTestCaseIDs(excelPath, "apiGET", "caseID", "isRun");
				List<String> postCases = Common.getAllTestCaseIDs(excelPath, "apiPOST", "caseID", "isRun");
				List<String> putCases = Common.getAllTestCaseIDs(excelPath, "apiPUT", "caseID", "isRun");
				List<String> deleteCases = Common.getAllTestCaseIDs(excelPath, "apiDELETE", "caseID", "isRun");

				Common.generateTestNGXmlFile(getCases, postCases, putCases, deleteCases);
			}

			runSuite("src/test/resources/testng-api.xml");
		}

	}

	private void runSuite(String xmlPath) {
		TestNG testng = new TestNG();
		testng.setTestSuites(List.of(xmlPath));
		testng.run();
	}
}
