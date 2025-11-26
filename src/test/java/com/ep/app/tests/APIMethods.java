package com.ep.app.tests;

import java.util.Map;

import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.changepond.test.framework.actions.APIActions;
import com.changepond.test.framework.actions.ExcelActions;
import com.ep.app.dto.DataContext;

public class APIMethods {
	private final Common common;

	public APIMethods() {
		DataContext dataContext = new DataContext();
		ExcelActions excelActions = new ExcelActions();
		APIActions apiActions = new APIActions();
		this.common = new Common(dataContext, excelActions, apiActions);
	}

	private ExtentTest getTestLogger() {
		ITestResult result = Reporter.getCurrentTestResult();
		return (ExtentTest) result.getAttribute("extentTest");
	}

	@Test(priority = 1)
	@Parameters("caseID")
	public void testGETRequest(String caseID) {
		String sheet = "apiGET";
		Map<String, String> data = common.getTestData(caseID, sheet);
		String method = data.get("action");
		getTestLogger().log(Status.INFO, "Preparing GET request for: " + caseID);
		System.out.println("Executing GET with caseID: " + caseID);
		common.setBaseUrl(caseID, sheet);
		common.setPathParam(caseID, sheet);
		common.setAuthToken(caseID, sheet);
		common.makeRequest(method, caseID, sheet);
		common.verifyResponseCode("200");
		common.validateResponseParams(caseID, sheet);
		common.saveApiResponse("APIData.xlsx", sheet, caseID, 0);
	}

	@Test(priority = 2)
	@Parameters("caseID")
	public void testPOSTRequest(String caseID) {
		String sheet = "apiPOST";
		Map<String, String> data = common.getTestData(caseID, sheet);
		String method = data.get("action");
		getTestLogger().log(Status.INFO, "Preparing POST request for: " + caseID);
		System.out.println("Executing POST with caseID: " + caseID);
		common.setBaseUrl(caseID, sheet);
		common.preparePayload(caseID, sheet);
		common.setAuthToken(caseID, sheet);
		common.makeRequest(method, caseID, sheet);
		common.verifyResponseCode("200", "201");
		common.validateResponseParams(caseID, sheet);
		common.saveApiResponse("APIData.xlsx", sheet, caseID, 0);
	}

	@Test(priority = 3)
	@Parameters("caseID")
	public void testPUTRequest(String caseID) {
		String sheet = "apiPUT";
		Map<String, String> data = common.getTestData(caseID, sheet);
		String method = data.get("action");
		getTestLogger().log(Status.INFO, "Preparing PUT request for: " + caseID);
		System.out.println("Executing PUT with caseID: " + caseID);
		common.setBaseUrl(caseID, sheet);
		common.resolveDynamicEndpoint(caseID, sheet);
		common.setAuthToken(caseID, sheet);
		common.preparePayload(caseID, sheet);
		common.makeRequest(method, caseID, sheet);
		common.verifyResponseCode("200");
		common.validateResponseParams(caseID, sheet);
		common.saveApiResponse("APIData.xlsx", sheet, caseID, 0);
	}

	@Test(priority = 4)
	@Parameters("caseID")
	public void testDELETERequest(String caseID) {
		String sheet = "apiDELETE";
		Map<String, String> data = common.getTestData(caseID, sheet);
		String method = data.get("action");
		// should be "DELETE" getTestLogger().log(Status.INFO, "Preparing DELETE request
		// for: " + caseID);
		System.out.println("Executing DELETE with caseID: " + caseID);
		common.setBaseUrl(caseID, sheet);
		common.setPathParam(caseID, sheet);
		common.setAuthToken(caseID, sheet);
		common.makeRequest(method, caseID, sheet);
		common.verifyResponseCode("204");
		common.saveApiResponse("APIData.xlsx", sheet, caseID, 0);
	}
}