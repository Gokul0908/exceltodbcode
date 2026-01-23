package com.ep.app.tests;

import java.util.List;
import java.util.Scanner;
import org.testng.TestNG;
import org.testng.annotations.Test;
import com.ep.app.tests.util.DBSchemaInitializer;
import com.ep.app.utils.ConfigReader;

public class RunEnabledAPITests {

	@Test
	public static void runEnabledTests() {

		Scanner sc = new Scanner(System.in);
		System.out.println("Choose execution mode:");
		System.out.println("1 - Run directly from Excel");
		System.out.println("2 - Excel -> DB -> Run");
		int option = sc.nextInt();

		String fileName = ConfigReader.getConfigValue("excelFile");
		String excelPath = Common.getExcelFilePath(fileName);

		boolean runFromDB = false;

		if (option == 2) {
			DBSchemaInitializer.createTableIfNotExists();
			Common.copyExcelDataToDB(excelPath);
			runFromDB = true; // 🔥 REQUIRED
		}

		// 1. Get caseIDs for individual API operations
		List<String> getCases = Common.getAllTestCaseIDs(excelPath, "apiGET", "caseID", "isRun", runFromDB);

		List<String> postCases = Common.getAllTestCaseIDs(excelPath, "apiPOST", "caseID", "isRun", runFromDB);

		List<String> putCases = Common.getAllTestCaseIDs(excelPath, "apiPUT", "caseID", "isRun", runFromDB);

		List<String> deleteCases = Common.getAllTestCaseIDs(excelPath, "apiDELETE", "caseID", "isRun", runFromDB);

		// 2. Generate XML and run for api tests
		Common.generateTestNGXmlFile(getCases, postCases, putCases, deleteCases);
		runTestNGSuite("src/test/resources/testng-api.xml");

		// 3. Chaining (Excel or DB)
		List<String> chainingGroupIDs = Common.getUniqueChainingGroupIDs(excelPath, "chainingRequests", "caseID",
				"isRun", runFromDB);

		if (chainingGroupIDs != null && !chainingGroupIDs.isEmpty()) {
			Common.generateChainingXML(chainingGroupIDs, "src/test/resources/testng-chaining.xml");
			runTestNGSuite("src/test/resources/testng-chaining.xml");
		} else {
			System.out.println(" ---> No chaining test cases enabled. Skipping chaining suite <-----");
		}
	}

	private static void runTestNGSuite(String xmlFilePath) {
		TestNG testng = new TestNG();
		testng.setTestSuites(List.of(xmlFilePath));
		testng.run();
	}
}
