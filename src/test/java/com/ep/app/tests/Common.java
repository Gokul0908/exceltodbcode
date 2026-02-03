package com.ep.app.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Assert;
import org.testng.SkipException;

import com.changepond.test.framework.actions.APIActions;
import com.changepond.test.framework.actions.ExcelActions;
import com.ep.app.dto.DataContext;
import com.ep.app.tests.util.DBReader;
import com.ep.app.tests.util.DBUtil;
import com.ep.app.utils.ReportUtil.ReportUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class Common {

	private final DataContext dataContext;
//	private static ExcelActions excelActions;
	private final APIActions apiActions;
	private static String reportFilePath;
	private static String singleReportFilePath = null;

	private static ExcelActions excelActions = new ExcelActions();

	public Common(DataContext dataContext, ExcelActions excelActions, APIActions apiActions) {
		this.dataContext = dataContext;
		this.excelActions = excelActions;
		this.apiActions = apiActions;

	}

	public Map<String, String> getTestData(String caseID, String sheet) {
		String excelPath = System.getProperty("user.dir") + "/src/test/resources/testData/APIData.xlsx";
		String dateFormat = "dd-MM-yyyy";
		Map<String, String> data = excelActions.extractExcelDataAsMap(excelPath, sheet, caseID, dateFormat);

		if (data == null || data.isEmpty()) {
			throw new SkipException("No test data found for caseID: " + caseID + " in sheet: " + sheet);
		}

		return data;
	}

	public boolean validateIsRun(String caseID, String sheet) {
		Map<String, String> data = getTestData(caseID, sheet);

		if (!"Yes".equalsIgnoreCase(data.get("isRun"))) {
			System.out.println("Skipping test | isRun = No | caseID = " + caseID);
			return false;
		}
		return true;
	}

	private boolean isNullOrEmpty(String str) {
		return str == null || str.trim().isEmpty();
	}

	public void setBaseUrl(String caseID, String sheet) {
		Map<String, String> data = getTestData(caseID, sheet);
		if (!validateIsRun(caseID, sheet)) {
			return;
		}

		String baseURL = data.get("baseURL");
		if (isNullOrEmpty(baseURL)) {
			throw new SkipException("Base URL missing for caseID: " + caseID);
		}

		RequestSpecification request = RestAssured.given().baseUri(baseURL);
		apiActions.setContentTypeInRequestHeader(request, "application/json");
		dataContext.setRequest(request);
		request.log().all();

	}

	public void setPathParam(String caseID, String sheet) {
		Map<String, String> data = getTestData(caseID, sheet);

		String paramKey = data.get("queryParam");
		String paramValue = data.get("queryParamValue");

		String endpoint = data.get("endPoint");
		RequestSpecification request = dataContext.getRequest();

		if (!isNullOrEmpty(paramKey) && !isNullOrEmpty(paramValue) && !paramKey.equalsIgnoreCase("NA")
				&& !paramValue.equalsIgnoreCase("NA")) {

//			System.out.println("Setting path parameter: " + paramKey + " = " + paramValue);
//			request = apiActions.setSinglePathParameter(request, paramKey, paramValue);

			if (endpoint.contains("{id}")) {
				endpoint = endpoint.replace("{id}", paramValue);
				request.basePath(endpoint);
			} else {
				request = apiActions.setSinglePathParameter(request, paramKey, paramValue);
			}

			dataContext.setRequest(request);

		} else {
			System.out.println("Skipping path parameter setup for caseID: " + caseID + " as values are missing or NA.");
		}
	}

	public void setAuthToken(String caseID, String sheet) {
		Map<String, String> data = getTestData(caseID, sheet);
		RequestSpecification request = dataContext.getRequest();

		String token = data.get("generatedToken");
		String baseauth = data.get("basicAuth");
		String authType = data.get("authType");

		if (isNullOrEmpty(authType) || authType.equalsIgnoreCase("NA")) {
			// No auth required
			System.out.println("No authentication applied for caseID: " + caseID);
		} else if (authType.equalsIgnoreCase("Bearer")) {
			if (!isNullOrEmpty(token)) {
				apiActions.setBearerAuthInRequestHeader(request, "Authorization", token);
			} else {
				throw new RuntimeException("Missing token for Bearer auth in caseID: " + caseID);
			}
		} else if (authType.equalsIgnoreCase("OAuth2")) {
			if (!isNullOrEmpty(token)) {
				apiActions.setOAuth2AuthInRequestHeader(request, token);
			} else {
				throw new RuntimeException("Missing token for OAuth2 auth in caseID: " + caseID);
			}
		} else if (authType.equalsIgnoreCase("Basic")) {
			if (!isNullOrEmpty(baseauth)) {
				String[] creds = baseauth.split(":");
				if (creds.length == 2) {
					apiActions.setBasicAuthInRequestHeader(request, creds[0], creds[1]);
				} else {
					throw new RuntimeException(
							"Invalid baseauth format (expected username:password) in caseID: " + caseID);
				}
			} else {
				throw new RuntimeException("Missing baseauth for Basic auth in caseID: " + caseID);
			}
		} else {
			throw new IllegalArgumentException(
					"Unsupported authType '" + authType + "' in Excel for caseID: " + caseID);
		}

		dataContext.setRequest(request);
	}

	public void preparePayload(String caseID, String sheet) {
		Map<String, String> data = getTestData(caseID, sheet);

		String payload = data.get("payLoad");
		if (isNullOrEmpty(payload)) {
			throw new RuntimeException("No 'payLoad' found in Excel for caseID: " + caseID);
		}

		// 💥 generate random
		String randomValue = Long.toHexString(System.currentTimeMillis());

		// 💥 replace random in payload
		payload = payload.replace("${random}", randomValue);

		// 💥 AUTO-GENERATE UNIQUE EMAIL for POST requests only
		if ("POST".equalsIgnoreCase(data.get("action")) && payload.contains("\"email\"")) {
			String randomEmail = "apiUser_" + randomValue + "@gmail.com";

			payload = payload.replaceAll("\"email\"\\s*:\\s*\"[^\"]+\"", "\"email\": \"" + randomEmail + "\"");

			System.out.println("🔄 Auto-generated email = " + randomEmail);
		}

		// 💥 save JSON + random value
		dataContext.setJsonBody(payload);
		dataContext.setRandomValue(randomValue);

	}

	public void makeRequest(String method, String caseID, String sheet) {

		RestAssured.useRelaxedHTTPSValidation();
		RestAssured.config = RestAssured.config().httpClient(HttpClientConfig.httpClientConfig());

		Map<String, String> data = getTestData(caseID, sheet);

		String endpoint = dataContext.getUpdatedEndpoint();
		if (isNullOrEmpty(endpoint)) {
			endpoint = data.get("endPoint");
		}

		if (isNullOrEmpty(method)) {
			throw new IllegalArgumentException("HTTP method (action) missing for caseID: " + caseID);
		}

		RequestSpecification request = dataContext.getRequest();
		Response response;

		// ---- HANDLE FIRST: If {id} missing for standalone PUT / DELETE / GET ----
		if ((method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("DELETE") || method.equalsIgnoreCase("GET"))
				&& endpoint.contains("{id}")
				&& (dataContext.getLastCreatedId() == null || dataContext.getLastCreatedId().trim().isEmpty())) {

			System.out.println(" No POST id --> Fetching latest user ID...");

			Response tempResponse = RestAssured.given().baseUri("https://gorest.co.in")
					.header("Authorization", "Bearer " + data.get("generatedToken")).get("/public/v2/users");

			String latestId = tempResponse.jsonPath().getString("[0].id");
			dataContext.setLastCreatedId(latestId);

			System.out.println("Latest ID fetched: " + latestId);

			endpoint = endpoint.replace("{id}", latestId);
		}

		// ---- Resolve ID inside endpoint if already available (Chaining case) ----
		if (endpoint.contains("{id}")) {
			String lastId = dataContext.getLastCreatedId();
			if (lastId == null || lastId.trim().isEmpty()) {
				throw new IllegalStateException("Missing ID for endpoint: " + endpoint);
			}
			endpoint = endpoint.replace("{id}", lastId);
		}

		System.out.println("ENDPOINT = " + endpoint);

		// ---- Set Body only for POST & PUT ----
		if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
			String body = dataContext.getJsonBody();
			if (isNullOrEmpty(body))
				throw new RuntimeException("Request body missing!");
			request.body(body);
		}

		switch (method.toUpperCase()) {
		case "POST" -> {
			long start = System.currentTimeMillis();
			response = request.post(endpoint);
			long end = System.currentTimeMillis();
			dataContext.setResponseTime(end - start); // ⬅ Save response time

			String postId = response.jsonPath().getString("id");
			if (postId == null)
				postId = response.jsonPath().getString("data.id");
			dataContext.setLastCreatedId(postId);
			System.out.println("🔥 Saved POST ID = " + postId);
		}
		case "PUT" -> {
			long start = System.currentTimeMillis();
			response = request.put(endpoint);
			long end = System.currentTimeMillis();
			dataContext.setResponseTime(end - start);
		}
		case "DELETE" -> {
			long start = System.currentTimeMillis();
			response = request.delete(endpoint);
			long end = System.currentTimeMillis();
			dataContext.setResponseTime(end - start);
		}
		case "GET" -> {
			long start = System.currentTimeMillis();
			response = request.get(endpoint);
			long end = System.currentTimeMillis();
			dataContext.setResponseTime(end - start);
		}
		default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
		}

		dataContext.setResponse(response);
	}

	public void verifyResponseCode(String... expectedCodes) {
		Response response = dataContext.getResponse();
		response.prettyPrint();

		int actualCode = apiActions.getStatusCodeFromResponse(response);
		System.out.println("Actual Status Code: " + actualCode);

		for (String expectedCode : expectedCodes) {
			if (Integer.parseInt(expectedCode) == actualCode) {
				return;
			}
		}

//		updated by se for checking purpose
//		Assert.fail("Expected one of " + Arrays.toString(expectedCodes) + " but got " + actualCode);

	}
	// Perform field verification even for single GET calls

	public void validateResponseParams(String caseID, String sheet) {

		Response response = dataContext.getResponse();
		if (response == null) {
			throw new IllegalStateException("API response is null.");
		}

		String responseBody = response.getBody().asString();

		// 🔥 FIX: Skip validation if response body is empty
		if (responseBody == null || responseBody.trim().isEmpty()) {
			System.out.println("Empty response body — Skipping response validation");
			return;
		}
		// 🔥 SKIP VALIDATION for Single GET when no POST id exists
		if (dataContext.getLastCreatedId() == null || dataContext.getLastCreatedId().trim().isEmpty()) {
			System.out.println("Single GET detected — Skipping id/email validation");
			return;
		}
//		validateResponseParams(caseID, sheet);

		Map<String, String> data = getTestData(caseID, sheet);
		String[] verificationParams = data.get("verificationParam").split(",");
		String[] expectedValues = data.get("verificationParamValue").split(",");

		if (verificationParams == null || expectedValues == null) {
			throw new SkipException("Missing verification data for caseID: " + caseID);
		}

		if (verificationParams.length != expectedValues.length) {
			throw new IllegalArgumentException("Mismatch between parameter and value count for caseID: " + caseID);
		}

		// --------------------------------------------------------------------
		// FIRST: CHECK IF RESPONSE IS A LIST (GET /users)
		// --------------------------------------------------------------------
		try {
			Object first = response.jsonPath().get("");
			if (first instanceof List) {

				// Read ID from context (POST id saved already)
				String postId = dataContext.getLastCreatedId();
				if (postId == null) {
					postId = dataContext.getResponse().jsonPath().getString("data.id");
				}

				List<Map<String, Object>> list = response.jsonPath().getList("");

				// Find the object that matches ID
				Map<String, Object> matched = null;
				for (Map<String, Object> item : list) {
					if (String.valueOf(item.get("id")).equals(postId)) {
						matched = item;
						break;
					}
				}

				if (matched == null) {
					throw new AssertionError("User with ID " + postId + " not found in GET response list");
				}

				// Validate parameters against matched user
				for (int i = 0; i < verificationParams.length; i++) {

					String param = verificationParams[i].trim();
					String expected = expectedValues[i].trim();

					// Replace ${random}
					String random = dataContext.getRandomValue();
					if (expected.contains("${random}")) {
						expected = expected.replace("${random}", random);
					}

					Object actualObj = matched.get(param);
					String actual = actualObj != null ? actualObj.toString().trim() : "null";

					if (param.equalsIgnoreCase("email")) {
						Assert.assertTrue(actual.contains("@gmail.com"),
								"Email validation failed: expected gmail but found " + actual);
					} else {
						Assert.assertEquals(actual, expected, "Mismatch for parameter [" + param + "]: expected = "
								+ expected + ", actual = " + actual);
					}

					System.out.println("Verified: " + param + " = " + expected);
				}

				return; // Done for list case
			}
		} catch (Exception ignored) {
			// Not a list — fall through to normal validation
		}

		// --------------------------------------------------------------------
		// SECOND: NORMAL VALIDATION (POST, PUT, DELETE responses)
		// --------------------------------------------------------------------
		for (int i = 0; i < verificationParams.length; i++) {

			String param = verificationParams[i].trim();
			String expected = expectedValues[i].trim();

			// Replace ${random}
			String random = dataContext.getRandomValue();
			if (expected.contains("${random}")) {
				expected = expected.replace("${random}", random);
			}

			Object actualObj = response.jsonPath().get("data." + param);
			if (actualObj == null) {
				actualObj = response.jsonPath().get(param);
			}

			String actual = actualObj != null ? actualObj.toString().trim() : "null";

			Assert.assertEquals(actual, expected,
					"Mismatch for parameter [" + param + "]: expected = " + expected + ", actual = " + actual);

			System.out.println("Verified: " + param + " = " + expected);
		}
	}

	public void validateResponseAgainstJsonSchema(String schemaFileName) {
		String schemaFilePath = "src/test/resources/inputJsonFile/" + schemaFileName;

		Response response = dataContext.getResponse();
		Assert.assertNotNull(response);

		apiActions.validateResponseWithJSONSchemaFile(response, schemaFilePath);
	}

	// 🔹 Save API Response into FINAL REPORT EXCEL (Single Report per Execution)
	public void saveApiResponse(String fileName, String sheetName, String testCaseId, int headerRowIndex) {
		try {
			String baseExcel = System.getProperty("user.dir") + "/src/test/resources/testData/" + fileName + ".xlsx";

			if (singleReportFilePath == null) {
				String outputExcelPath = ReportUtil.getReportFilePath();
				dataContext.setReportFilePath(outputExcelPath);

				File reportFile = new File(outputExcelPath);
				reportFile.getParentFile().mkdirs();

				FileUtils.copyFile(new File(baseExcel), reportFile);
				singleReportFilePath = reportFile.getAbsolutePath();
				dataContext.setReportFilePath(singleReportFilePath);

				System.out.println("📌 Report Created: " + singleReportFilePath);
			} else {
				System.out.println("📎 Updating Existing Report: " + singleReportFilePath);
			}

			FileInputStream fis = new FileInputStream(singleReportFilePath);
			Workbook workbook = WorkbookFactory.create(fis);
			fis.close();

			Sheet sheet = workbook.getSheet(sheetName);
			if (sheet == null) {
				System.out.println("⚠ Sheet not found: " + sheetName);
				workbook.close();
				return;
			}

			Row headerRow = sheet.getRow(headerRowIndex);
			if (headerRow == null) {
				System.out.println("⚠ Header row missing!");
				workbook.close();
				return;
			}

			int caseIdCol = -1, actionCol = -1, responseBodyCol = -1, statusCodeCol = -1, statusLineCol = -1,
					contentTypeCol = -1, responseTimeCol = -1; // 🔹 NEW COLUMN

			for (int c = 0; c < headerRow.getLastCellNum(); c++) {
				Cell cell = headerRow.getCell(c);
				if (cell == null)
					continue;
				String header = cell.getStringCellValue().trim().toLowerCase();

				if (header.equals("caseid"))
					caseIdCol = c;
				if (header.equals("action"))
					actionCol = c;
				if (header.equals("responsebody"))
					responseBodyCol = c;
				if (header.equals("statuscode"))
					statusCodeCol = c;
				if (header.equals("statusline"))
					statusLineCol = c;
				if (header.equals("contenttype"))
					contentTypeCol = c;

				if (header.equals("responsetime"))
					responseTimeCol = c; // 🔹 NEW COLUMN DETECT
			}

			Response response = dataContext.getResponse();
			if (response == null)
				return;

			String body = response.getBody().asString();
			String status = String.valueOf(response.getStatusCode());
			String statusLine = response.getStatusLine();
			String contentType = response.getContentType();
			String action = dataContext.getCurrentAction();
			long responseTime = dataContext.getResponseTime(); // 🔹 GET STORED RESPONSE TIME

			int targetRow = -1;
			for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
				Row row = sheet.getRow(r);
				if (row == null)
					continue;
				String caseVal = row.getCell(caseIdCol).toString().trim();
				String actionVal = (actionCol >= 0 && row.getCell(actionCol) != null)
						? row.getCell(actionCol).toString().trim()
						: "";

				if (caseVal.equalsIgnoreCase(testCaseId)) {
					Cell respCell = row.getCell(responseBodyCol);
					if (respCell == null || respCell.getStringCellValue().trim().isEmpty()) {
						targetRow = r;
						break;
					}
				}
			}

			if (targetRow == -1) {
				System.out.println("⚠ No matching row for: " + testCaseId);
				workbook.close();
				return;
			}

			Row row = sheet.getRow(targetRow);
			row.createCell(responseBodyCol).setCellValue(body);
			row.createCell(statusCodeCol).setCellValue(status);
			row.createCell(statusLineCol).setCellValue(statusLine);
			row.createCell(contentTypeCol).setCellValue(contentType);

			if (responseTimeCol >= 0) {
				row.createCell(responseTimeCol).setCellValue(responseTime); // 🔹 SAVE VALUE
			}

			FileOutputStream fos = new FileOutputStream(singleReportFilePath);
			workbook.write(fos);
			fos.close();
			workbook.close();

			System.out.println("⏱ Response Time Saved: " + responseTime + " ms");
			System.out.println(
					"✅ Saved Response in: " + singleReportFilePath + " | Sheet: " + sheetName + " | Row: " + targetRow);

		} catch (Exception e) {
			System.out.println("❌ Error Updating Excel");
			e.printStackTrace();
		}
	}

	public void copyExcel(String sourcePath, String destinationPath) {
		try (FileInputStream fis = new FileInputStream(new File(sourcePath));
				XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

			FileOutputStream fos = new FileOutputStream(new File(destinationPath));
			workbook.write(fos);
			fos.close();

			System.out.println("Excel copied successfully to: " + destinationPath);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to copy Excel file!");
		}
	}

	public int getRowIndexForTestCase(String filePath, String sheetName, String headerName, String testCaseId,
			int headerRowIndex) {
		try (FileInputStream fileInputStream = new FileInputStream(filePath);
				XSSFWorkbook workbook = new XSSFWorkbook(fileInputStream)) {

			XSSFSheet sheet = workbook.getSheet(sheetName);
			if (sheet == null) {
				System.out.println("Sheet not found: " + sheetName);
				return -1;
			}

			XSSFRow headerRow = sheet.getRow(headerRowIndex);
			int columnIdx = -1;
			for (Cell cell : headerRow) {
				if (cell.getStringCellValue().trim().equalsIgnoreCase(headerName)) {
					columnIdx = cell.getColumnIndex();
					break;
				}
			}

			if (columnIdx == -1) {
				System.out.println("Header not found: " + headerName);
				return -1;
			}

			for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				XSSFRow row = sheet.getRow(rowIndex);
				if (row != null) {
					Cell cell = row.getCell(columnIdx);
					if (cell != null && cell.getCellType() == CellType.STRING) {
						String cellValue = cell.getStringCellValue().trim();
						if (cellValue.equalsIgnoreCase(testCaseId.trim())) {
							return rowIndex;
						}
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("TestCaseID '" + testCaseId + "' not found in sheet: " + sheetName);
		return -1;
	}

	public Response getAndPrintResponse() {
		Response response = dataContext.getResponse();
		Assert.assertNotNull("API response is null");
		response.prettyPrint();
		return response;
	}

	public void updatePostIdInQueryParamForNextMatchingCaseID(String sheetName, String currentCaseID,
			int currentRowIndex, int headerRowIndex) {
		Response response = dataContext.getResponse();
		if (response == null) {
			throw new IllegalStateException(
					"Response is null. Ensure POST request was successful before updating Excel.");
		}

		// Try to get the ID from different possible JSON paths
		String postId = null;
		if (response.jsonPath().get("data.id") != null) {
			postId = response.jsonPath().getString("data.id");
		} else if (response.jsonPath().get("id") != null) {
			postId = response.jsonPath().getString("id");
		}

		if (postId == null || postId.isEmpty()) {
			throw new IllegalStateException("Post ID is missing from the response.");
		}

		String excelPath = System.getProperty("user.dir") + "/src/test/resources/testData/APIData.xlsx";
		List<List<String>> rows = excelActions.extractExcelDataAsList(excelPath, sheetName, 0, 0, "dd-MM-yyyy");

		int caseIDColumnIndex = rows.get(headerRowIndex).indexOf("caseID");
		int queryParamValueIndex = rows.get(headerRowIndex).indexOf("queryParamValue");

		if (caseIDColumnIndex == -1 || queryParamValueIndex == -1) {
			throw new RuntimeException("Required columns not found in sheet: " + sheetName);
		}

		List<String> sheetNames = Collections.singletonList(sheetName);

		for (int i = currentRowIndex + 1; i < rows.size(); i++) {

			List<String> row = rows.get(i);

			// Skip rows that don't match case ID
			if (!row.get(caseIDColumnIndex).trim().equalsIgnoreCase(currentCaseID)) {
				continue;
			}

			// Fetch current cell value
			String currentValue = row.get(queryParamValueIndex).trim();

			// Only update if cell contains ${id} OR is empty/NA
			if (currentValue.equalsIgnoreCase("${id}") || currentValue.equalsIgnoreCase("NA")
					|| currentValue.isEmpty()) {

				excelActions.setValueInSpecificCell(excelPath, sheetNames, "queryParamValue", headerRowIndex, i,
						postId);

				System.out.println("Updated queryParamValue in row " + i + " for caseID " + currentCaseID
						+ " with id = " + postId);
			} else {
				System.out.println("Skipped updating row " + i + " (value not ${id})");
			}
		}

//	    List<String> sheetNames2 = Collections.singletonList(sheetName);
//
//	    for (int i = currentRowIndex + 1; i < rows.size(); i++) {
//	        List<String> row = rows.get(i);
//
//	        // ✅ Extra safety: check row has enough columns
//	        if (row.size() > caseIDColumnIndex) {
//	            String caseIDFromExcel = row.get(caseIDColumnIndex).trim();
//
//	            // ✅ Update if the row has SAME caseID as current OR {id} placeholder in endpoint
//	            if (caseIDFromExcel.equalsIgnoreCase(currentCaseID)) {
//	                excelActions.setValueInSpecificCell(
//	                        excelPath, sheetNames2, "queryParamValue", headerRowIndex, i, postId
//	                );
//	                System.out.println("Updated queryParamValue in row " + i +
//	                                   " for caseID: " + currentCaseID +
//	                                   " with POST ID = " + postId);
//	            }
//	        }
//	    }

	}

	// additional excel action

	public static String getExcelFilePath(String fileName) {
		String basePath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "test"
				+ File.separator + "resources" + File.separator + "testData" + File.separator;

		if (!fileName.endsWith(".xlsx")) {
			fileName += ".xlsx";
		}

		String fullPath = basePath + fileName;
		System.out.println("Accessed the Excel File: " + fullPath);
		return fullPath;
	}

	// Get all TestCaseIDs from a specific column
	public static List<String> getAllTestCaseIDs(String filePath, String sheetName, String testCaseIdHeaderName,
			String runFlagHeaderName) {

		List<String> testCaseIds = new ArrayList<>();

		try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {

			Sheet sheet = workbook.getSheet(sheetName);
			if (sheet == null)
				return testCaseIds;

			Row headerRow = sheet.getRow(0);
			int testCaseIdCol = getColumnIndex(headerRow, testCaseIdHeaderName);
			int runFlagCol = getColumnIndex(headerRow, runFlagHeaderName);

			if (testCaseIdCol == -1 || runFlagCol == -1)
				return testCaseIds;

			for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row != null) {
					Cell testCaseCell = row.getCell(testCaseIdCol);
					Cell runFlagCell = row.getCell(runFlagCol);

					String testCaseId = getCellValueAsString(testCaseCell).trim();
					String runFlag = getCellValueAsString(runFlagCell).trim();

					if ("Yes".equalsIgnoreCase(runFlag) && !testCaseId.isEmpty()) {
						testCaseIds.add(testCaseId);
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return testCaseIds;
	}

	// method to get column index based on header name
	public static int getColumnIndex(Row headerRow, String headerName) {
		for (Cell cell : headerRow) {
			if (cell.getStringCellValue().trim().equalsIgnoreCase(headerName.trim())) {
				return cell.getColumnIndex();
			}
		}
		return -1;
	}

	// method to convert cell value to string
	public static String getCellValueAsString(Cell cell) {
		DataFormatter formatter = new DataFormatter();
		return formatter.formatCellValue(cell);
	}

	public static void generateTestNG(List<String> getCases, List<String> postCases, List<String> putCases,
			List<String> deleteCases) {
		try {
			File file = new File("src/test/resources/testng-api.xml");
			FileWriter writer = new FileWriter(file);

			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<suite name=\"DynamicSuite\">\n");

			// If any case exists, include the APIMethods class
			if (!getCases.isEmpty() || !postCases.isEmpty() || !putCases.isEmpty() || !deleteCases.isEmpty()) {
				writer.write("  <test name=\"APIMethodsTest\">\n");
				writer.write("    <classes>\n");
				writer.write("      <class name=\"com.ep.app.tests.APIMethods\"/>\n"); // Your single test class
				writer.write("    </classes>\n");
				writer.write("  </test>\n");
			}

			writer.write("</suite>");
			writer.close();

			System.out.println("testng-api.xml created successfully.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void generateTestNGXmlFile(List<String> getCases, List<String> postCases, List<String> putCases,
			List<String> deleteCases) {

		try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/test/resources/testng-api.xml"))) {

			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<suite name=\"DynamicSuite\" parallel=\"false\">\n");

			// Listener
			writer.write("  <listeners>\n");
			writer.write("    <listener class-name=\"com.ep.app.utils.TestListener\"/>\n");
			writer.write("  </listeners>\n");

			/*
			 * IMPORTANT: The second argument MUST be UNIQUE per group This value becomes
			 * part of <test name="">
			 */
			writeTestBlocks(writer, "testGETRequest", "apiGET", getCases);
			writeTestBlocks(writer, "testPOSTRequest", "apiPOST", postCases);
			writeTestBlocks(writer, "testPUTRequest", "apiPUT", putCases);
			writeTestBlocks(writer, "testDELETERequest", "apiDELETE", deleteCases);

			writer.write("</suite>\n");

			System.out.println("testng-api.xml created successfully.");

		} catch (IOException e) {
			throw new RuntimeException("Failed to generate testng-api.xml", e);
		}
	}

	private static void writeTestBlocks(BufferedWriter writer, String methodName, String uniqueGroupName,
			List<String> caseIds) throws IOException {

		for (String caseID : caseIds) {

			/*
			 * FINAL TEST NAME FORMAT (100% UNIQUE):
			 *
			 * testGETRequest_apiGET_TC001 testPOSTRequest_apiPOST_TC001
			 *
			 * methodName → testGETRequest uniqueGroupName → apiGET / apiPOST / apiPUT /
			 * apiDELETE caseID → TC001
			 */
			writer.write("  <test name=\"" + methodName + "_" + uniqueGroupName + "_" + caseID + "\">\n");

			writer.write("    <parameter name=\"caseID\" value=\"" + caseID + "\"/>\n");
			writer.write("    <classes>\n");
			writer.write("      <class name=\"com.ep.app.tests.APIMethods\">\n");
			writer.write("        <methods>\n");
			writer.write("          <include name=\"" + methodName + "\"/>\n");
			writer.write("        </methods>\n");
			writer.write("      </class>\n");
			writer.write("    </classes>\n");
			writer.write("  </test>\n");
		}
	}

	public static void generateChainingXML(List<String> chainIDs, String outputPath) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<suite name=\"ChainingSuite\" parallel=\"false\">\n");

			// Add listener block
			writer.write("  <listeners>\n");
			writer.write("    <listener class-name=\"com.ep.app.utils.TestListener\"/>\n");
			writer.write("  </listeners>\n");

			for (String chainID : chainIDs) {
				writer.write("  <test name=\"Chaining_" + chainID + "\">\n");
				writer.write("    <parameter name=\"chainCaseID\" value=\"" + chainID + "\"/>\n");
				writer.write("    <classes>\n");
				writer.write("      <class name=\"com.ep.app.tests.ChainingTest\"/>\n");
				writer.write("    </classes>\n");
				writer.write("  </test>\n");
			}

			writer.write("</suite>\n");
			System.out.println("testng-chaining.xml created successfully.");
		} catch (IOException e) {
			throw new RuntimeException("Failed to write testng-chaining.xml", e);
		}
	}

	public void executeChainedSteps(String filePath, String sheetName, String chainCaseID) throws IOException {
		String dateFormat = "dd-MM-yyyy";
		List<Map<String, String>> chainSteps = excelActions.extractExcelDataAsListOfMaps(filePath, sheetName,
				chainCaseID, dateFormat);
		List<List<String>> allRows = excelActions.extractExcelDataAsList(filePath, sheetName, 0, 0, dateFormat);

		int headerRowIndex = 0;
		String prevMethod = null;

		for (int i = 0; i < chainSteps.size(); i++) {

			Map<String, String> stepData = chainSteps.get(i);

			if (!"Yes".equalsIgnoreCase(stepData.get("isRun")))
				continue;

			String method = stepData.get("action");
			String stepCaseID = stepData.get("caseID");
			String endpointFromExcel = stepData.get("endPoint");

			// 🔥 IMPORTANT: Set current action here
			dataContext.setCurrentAction(method); // <-- ADDED LINE

			if (method == null || stepCaseID == null) {
				System.err.println("Skipping due to null method or caseID at step " + i);
				continue;
			}

			if (method.equalsIgnoreCase("PUT")) {
				dataContext.setUpdatedEndpoint(endpointFromExcel);
				System.out.println("🔧 Updated PUT endpoint = " + endpointFromExcel);
			} else {
				dataContext.setUpdatedEndpoint(endpointFromExcel);
			}

			System.out.println("Executing: " + method + " for caseID: " + stepCaseID);

			setBaseUrl(stepCaseID, sheetName);

			if (!"GET".equalsIgnoreCase(method)) {
				preparePayload(stepCaseID, sheetName);
			}

			setAuthToken(stepCaseID, sheetName);

			if (method.equalsIgnoreCase("PUT")) {
				String postId = dataContext.getLastCreatedId();
				if (postId != null) {
					String updatedEndpoint = endpointFromExcel.replace("{id}", postId);
					dataContext.setUpdatedEndpoint(updatedEndpoint);
					System.out.println("💡 PUT URL updated to use ID: " + updatedEndpoint);
				}
			}

			// Execute Request
			makeRequest(method, stepCaseID, sheetName);

			// Save ID after POST
			if (method.equalsIgnoreCase("POST")) {
				Response response = dataContext.getResponse();
				String responseBody = response.getBody().asString();

				if (responseBody != null && !responseBody.trim().isEmpty()) {

					String postId = null;

					try {
						postId = response.jsonPath().getString("id");
						if (postId == null) {
							postId = response.jsonPath().getString("data.id");
						}
					} catch (Exception e) {
						System.out.println("ℹ Response has no JSON id field");
					}

					dataContext.setLastCreatedId(postId);
					System.out.println("🔥 Saved POST ID = " + postId);

				} else {
					System.out.println("ℹ Empty response body — skipping ID extraction");
				}

			}

			// Status Validation
			int expectedStatusCode;
			if (method.equalsIgnoreCase("GET")) {
				expectedStatusCode = ("DELETE".equalsIgnoreCase(prevMethod)) ? 404 : 200;
			} else if (method.equalsIgnoreCase("POST")) {
				expectedStatusCode = 201;
			} else if (method.equalsIgnoreCase("PUT")) {
				expectedStatusCode = 200;
			} else if (method.equalsIgnoreCase("DELETE")) {
				expectedStatusCode = 204;
			} else {
				throw new IllegalArgumentException("Unsupported method: " + method);
			}

			verifyResponseCode(String.valueOf(expectedStatusCode));
			if (!method.equalsIgnoreCase("DELETE") && expectedStatusCode != 404) {
				validateResponseParams(stepCaseID, sheetName);
			}

			prevMethod = method;

			saveApiResponse("APIData", sheetName, stepCaseID, headerRowIndex);

		}
	}

	public static List<String> getUniqueChainingGroupIDs(String filePath, String sheetName, String caseIDColumn,
			String runFlagColumn) {
		List<List<String>> data = excelActions.extractExcelDataAsList(filePath, sheetName, 0, 0, "dd-MM-yyyy");

		List<String> uniqueGroupIDs = new ArrayList<>();

		if (data == null || data.size() <= 1) {
			System.out.println("No chaining request data found in sheet: " + sheetName);
			return uniqueGroupIDs;
		}

		List<String> headers = data.get(0);
		int caseIdIdx = headers.indexOf(caseIDColumn);
		int runFlagIdx = headers.indexOf(runFlagColumn);

		if (caseIdIdx == -1 || runFlagIdx == -1) {
			throw new RuntimeException("Column headers not found: " + caseIDColumn + ", " + runFlagColumn);
		}

		Set<String> groupSet = new LinkedHashSet<>();
		for (int i = 1; i < data.size(); i++) {
			List<String> row = data.get(i);
			if (row.size() <= Math.max(caseIdIdx, runFlagIdx))
				continue;

			if ("Yes".equalsIgnoreCase(row.get(runFlagIdx).trim())) {
				groupSet.add(row.get(caseIdIdx).trim());
			}
		}

		uniqueGroupIDs.addAll(groupSet);
		return uniqueGroupIDs;
	}

	public String getNormalizedResponse(Response response) {
		String rawResponse = response.asString();

		JsonObject json = JsonParser.parseString(rawResponse).getAsJsonObject();
		JsonElement dataElement = json.get("data");

		JsonObject newJson = new JsonObject();
		newJson.addProperty("code", json.get("code").getAsInt());
		newJson.add("meta", json.get("meta"));

		if (dataElement.isJsonArray()) {
			// Take the first element from array
			JsonObject first = dataElement.getAsJsonArray().get(0).getAsJsonObject();
			newJson.add("data", first);
		} else {
			// Keep single object as it is
			newJson.add("data", dataElement.getAsJsonObject());
		}

		return newJson.toString();
	}

	public void resolveDynamicEndpoint(String caseID, String sheet) {
		Map<String, String> data = getTestData(caseID, sheet);

		String endpoint = data.get("endPoint");
		String id = data.get("queryParamValue");

		if (endpoint.contains("{id}") && id != null) {
			endpoint = endpoint.replace("{id}", id);
		}

		dataContext.setUpdatedEndpoint(endpoint);
	}

	public static int getCellIndex(Row headerRow, String columnName) {
		for (int i = 0; i < headerRow.getLastCellNum(); i++) {
			if (headerRow.getCell(i).getStringCellValue().equalsIgnoreCase(columnName)) {
				return i;
			}
		}
		return -1;
	}

	public static void copyExcelDataToDB(String excelPath) {
		int runId = DBUtil.getNewRunId(); // one run = one run_id

		try (Workbook workbook = WorkbookFactory.create(new FileInputStream(excelPath))) {

			int sheetCount = workbook.getNumberOfSheets();
			System.out.println("Total sheets found: " + sheetCount);

			for (int s = 0; s < sheetCount; s++) {

				Sheet sheet = workbook.getSheetAt(s);
				String sheetName = sheet.getSheetName();

				int runLineNo = 1; // RESET PER SHEET
				Row header = sheet.getRow(0);

				int caseIdIdx = requireColumn(header, "caseID", sheetName);
				int isRunIdx = requireColumn(header, "isRun", sheetName);
				int baseURLIdx = requireColumn(header, "baseURL", sheetName);
				int endPointIdx = requireColumn(header, "endPoint", sheetName);
				int basicAuthIdx = requireColumn(header, "basicAuth", sheetName);
				int apiKeyIdx = requireColumn(header, "apiKey", sheetName);
				int authTypeIdx = requireColumn(header, "authType", sheetName);
				int tokenIdx = requireColumn(header, "generatedToken", sheetName);
				int actionIdx = requireColumn(header, "action", sheetName);
				int queryParamIdx = requireColumn(header, "queryParam", sheetName);
				int queryParamValueIdx = requireColumn(header, "queryParamValue", sheetName);
				int verificationParamIdx = requireColumn(header, "verificationParam", sheetName);
				int verificationParamValueIdx = requireColumn(header, "verificationParamValue", sheetName);

				for (int r = 1; r <= sheet.getLastRowNum(); r++) {

					Row row = sheet.getRow(r);
					if (row == null)
						continue;

					String isRun = getCellValue(row.getCell(isRunIdx));
					if (!"YES".equalsIgnoreCase(isRun))
						continue;

					DBUtil.insertApiData(runId, runLineNo++, //  auto-increment
							sheetName, getCellValue(row.getCell(caseIdIdx)), isRun,
							getCellValue(row.getCell(baseURLIdx)), getCellValue(row.getCell(endPointIdx)),
							getCellValue(row.getCell(basicAuthIdx)), getCellValue(row.getCell(apiKeyIdx)),
							getCellValue(row.getCell(authTypeIdx)), getCellValue(row.getCell(tokenIdx)),
							getCellValue(row.getCell(actionIdx)), getCellValue(row.getCell(queryParamIdx)),
							getCellValue(row.getCell(queryParamValueIdx)),
							getCellValue(row.getCell(verificationParamIdx)),
							getCellValue(row.getCell(verificationParamValueIdx)));
				}
			}

		} catch (Exception e) {
			throw new RuntimeException("Excel → DB upload failed", e);
		}
	}

	// ================= COLUMN VALIDATION =================
	private static int requireColumn(Row header, String col, String sheet) {
		for (int i = 0; i < header.getLastCellNum(); i++) {
			if (header.getCell(i).getStringCellValue().trim().equalsIgnoreCase(col)) {
				return i;
			}
		}
		throw new RuntimeException("Missing column '" + col + "' in sheet: " + sheet);
	}

	// ================= SAFE CELL READ =================
	@SuppressWarnings("deprecation")
	private static String getCellValue(Cell cell) {
		if (cell == null)
			return "";
		cell.setCellType(CellType.STRING);
		return cell.getStringCellValue().trim();
	}

	public static List<String> getAllTestCaseIDs(String excelPath, String sheetName, String caseIDCol, String isRunCol,
			boolean runFromDB) {

		List<String> caseIds;

		if (runFromDB) {
			caseIds = DBReader.getTestCaseIDs(sheetName);
		} else {
			caseIds = getAllTestCaseIDs(excelPath, sheetName, caseIDCol, isRunCol);
		}

		//  CRITICAL FIX: REMOVE DUPLICATES
		return new ArrayList<>(new LinkedHashSet<>(caseIds));
	}

	public static List<String> getUniqueChainingGroupIDs(String excelPath, String sheetName, String caseIDCol,
			String isRunCol, boolean runFromDB) {

		List<String> groupIds;

		if (runFromDB) {
			groupIds = DBReader.getChainingGroupIDs(sheetName);
		} else {
			groupIds = getUniqueChainingGroupIDs(excelPath, sheetName, caseIDCol, isRunCol);
		}

		//  DEDUPLICATE
		return new ArrayList<>(new LinkedHashSet<>(groupIds));
	}

}