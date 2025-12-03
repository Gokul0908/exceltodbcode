package com.ep.app.tests;

import java.util.List;

import org.testng.TestNG;
import org.testng.annotations.Test;

import com.ep.app.utils.ConfigReader;

public class RunEnabledAPITests {

	public static void main(String[] args) {
		runEnabledTests1();
	}

	@Test
	public static void runEnabledTests1() {

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

		// Run chaining tests only if chaining cases exist
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

	@Test
	public void runEnabledTests() {
		main(null); // Call your existing code
	}

}