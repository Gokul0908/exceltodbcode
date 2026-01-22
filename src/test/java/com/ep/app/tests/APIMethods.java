package com.ep.app.tests;

import java.io.IOException;
import java.util.Map;

import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.Test;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.changepond.test.framework.actions.APIActions;
import com.changepond.test.framework.actions.ExcelActions;
import com.ep.app.dto.DataContext;

import swagger.SwaggerRunner;

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

	// =========================================================
	// ✅ SINGLE ENTRY POINT – NO @Parameters
	// =========================================================
	@Test
	public void runApi() throws IOException {

		// 🔥 READ caseID DIRECTLY FROM TESTNG XML
		String caseID = Reporter.getCurrentTestResult().getTestContext().getCurrentXmlTest().getParameter("caseID");

		if (caseID == null) {
			throw new RuntimeException("caseID not found in TestNG XML");
		}

		String sheet;

		if (SwaggerRunner.isSwaggerMode()) {
			sheet = "SwaggerAPIs";
		} else {
			// Auto-detect sheet from action (OLD FLOW SAFE)
			Map<String, String> tempData = common.getTestData(caseID, "apiGET"); // read once to know action

			String action = tempData.get("action").toUpperCase();

			sheet = switch (action) {
			case "GET" -> "apiGET";
			case "POST" -> "apiPOST";
			case "PUT" -> "apiPUT";
			case "DELETE" -> "apiDELETE";
			default -> throw new RuntimeException("Invalid action: " + action);
			};
		}

		Map<String, String> data = common.getTestData(caseID, sheet);
		String action = data.get("action");

		if (action == null || action.isBlank()) {
			throw new RuntimeException("Action missing for caseID: " + caseID);
		}

		action = action.toUpperCase();

		getTestLogger().log(Status.INFO, "Executing " + action + " request for caseID: " + caseID);

		System.out.println("Executing " + action + " with caseID: " + caseID);

		switch (action) {

		case "GET":
			common.setBaseUrl(caseID, sheet);
			common.setPathParam(caseID, sheet);
			common.setAuthToken(caseID, sheet);
			common.makeRequest("GET", caseID, sheet);
			common.verifyResponseCode("200");
			break;

		case "POST":
			common.setBaseUrl(caseID, sheet);
			common.preparePayload(caseID, sheet);
			common.setAuthToken(caseID, sheet);
			common.makeRequest("POST", caseID, sheet);
			common.verifyResponseCode("200", "201");
			break;

		case "PUT":
			common.setBaseUrl(caseID, sheet);
			common.resolveDynamicEndpoint(caseID, sheet);
			common.preparePayload(caseID, sheet);
			common.setAuthToken(caseID, sheet);
			common.makeRequest("PUT", caseID, sheet);
			common.verifyResponseCode("200");
			break;

		case "DELETE":
			common.setBaseUrl(caseID, sheet);
			common.setPathParam(caseID, sheet);
			common.setAuthToken(caseID, sheet);
			common.makeRequest("DELETE", caseID, sheet);
			common.verifyResponseCode("200", "204");
			break;

		default:
			throw new RuntimeException("Unsupported action: " + action);
		}
	}
}
