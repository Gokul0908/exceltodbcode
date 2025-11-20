package com.ep.app.tests;  // New Code

import java.util.List;

import org.testng.TestNG;
import com.ep.app.utils.ConfigReader;

public class RunEnabledAPITests {

	public static void main(String[] args) {

		String fileName = ConfigReader.getConfigValue("excelFile");
		String excelPath = Common.getExcelFilePath(fileName);

		// 1. Get caseIDs for individual API operations
		List<String> getCases = Common.getAllTestCaseIDs(excelPath, "apiGET", "caseID", "isRun");
		List<String> postCases = Common.getAllTestCaseIDs(excelPath, "apiPOST", "caseID", "isRun");
		List<String> putCases = Common.getAllTestCaseIDs(excelPath, "apiPUT", "caseID", "isRun");
		List<String> deleteCases = Common.getAllTestCaseIDs(excelPath, "apiDELETE", "caseID", "isRun");

		// 2. Generate XML and run for api tests
		Common.generateTestNGXmlFile(getCases, postCases, putCases, deleteCases);
		runTestNGSuite("src/test/resources/testng-api.xml");

		// 3. Get chain case IDs (grouped)
		List<String> chainingGroupIDs = Common.getUniqueChainingGroupIDs(excelPath, "chainingRequests", "caseID",
				"isRun");

		// 4. Generate XML and run for chaining tests
		Common.generateChainingXML(chainingGroupIDs, "src/test/resources/testng-chaining.xml");
		runTestNGSuite("src/test/resources/testng-chaining.xml");
	}

	private static void runTestNGSuite(String xmlFilePath) {
		TestNG testng = new TestNG();
		testng.setTestSuites(List.of(xmlFilePath));
		testng.run();
	}
}