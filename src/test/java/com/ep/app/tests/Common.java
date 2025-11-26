package com.ep.app.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.SkipException;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.changepond.test.framework.actions.APIActions;
import com.changepond.test.framework.actions.ExcelActions;
import com.ep.app.dto.DataContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class Common {

	private final DataContext dataContext;
//	private static ExcelActions excelActions;
	private final APIActions apiActions;

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

	public void validateIsRun(String caseID, String sheet) {
		Map<String, String> data = getTestData(caseID, sheet);
		if (!"Yes".equalsIgnoreCase(data.get("isRun"))) {
			System.out.println("Skipping test: isRun is not 'Yes' for caseID: " + caseID);
			throw new SkipException("Skipping this scenario");
		}
	}

	private boolean isNullOrEmpty(String str) {
		return str == null || str.trim().isEmpty();
	}

	public void setBaseUrl(String caseID, String sheet) {
		Map<String, String> data = getTestData(caseID, sheet);
		validateIsRun(caseID, sheet);

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
		String baseauth = data.get("baseauth");
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

	        System.out.println("⚠ No POST id → Fetching latest user ID...");

	        Response tempResponse = RestAssured.given()
	                .baseUri("https://gorest.co.in")
	                .header("Authorization", "Bearer " + data.get("generatedToken"))
	                .get("/public/v2/users");

	        String latestId = tempResponse.jsonPath().getString("[0].id");
	        dataContext.setLastCreatedId(latestId);

	        System.out.println("✔ Latest ID fetched: " + latestId);

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
	        if (isNullOrEmpty(body)) throw new RuntimeException("Request body missing!");
	        request.body(body);
	    }

	    switch (method.toUpperCase()) {
	        case "POST" -> {
	            response = request.post(endpoint);
	            String postId = response.jsonPath().getString("id");
	            if (postId == null) postId = response.jsonPath().getString("data.id");
	            dataContext.setLastCreatedId(postId);
	            System.out.println("🔥 Saved POST ID = " + postId);
	        }
	        case "PUT" -> response = request.put(endpoint);
	        case "DELETE" -> response = request.delete(endpoint);
	        case "GET" -> response = request.get(endpoint);
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
		Assert.fail("Expected one of " + Arrays.toString(expectedCodes) + " but got " + actualCode);

	}

	public void validateResponseParams(String caseID, String sheet) {

		Response response = dataContext.getResponse();
		if (response == null) {
			throw new IllegalStateException("API response is null.");
		}

		// 🔥 SKIP VALIDATION for Single GET when no POST id exists
		if (dataContext.getLastCreatedId() == null || dataContext.getLastCreatedId().trim().isEmpty()) {
			System.out.println("⚠ Single GET detected — Skipping id/email validation");
			return;
		}

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

//save api response
	public void saveApiResponse(String fileName, String sheetName, String testCaseId, int headerRowIndex) {
		String excelPath = getExcelFilePath(fileName);
		List<String> sheetNames = List.of(sheetName);

		Response response = dataContext.getResponse();
		Assert.assertNotNull(response, "Response is null for testCaseId: " + testCaseId);

		String responseBody = response.getBody().asString();
		int statusCode = response.getStatusCode();
		String statusLine = response.getStatusLine();
		String contentType = response.getContentType();

		int rowIndex = getRowIndexForTestCase(excelPath, sheetName, "caseID", testCaseId, headerRowIndex);
		if (rowIndex == -1) {
			System.out.println("TestCaseID not found in Excel: " + testCaseId);
			return;
		}

		excelActions.setValueInSpecificCell(excelPath, sheetNames, "responseBody", headerRowIndex, rowIndex,
				responseBody);
		excelActions.setValueInSpecificCell(excelPath, sheetNames, "statusCode", headerRowIndex, rowIndex,
				String.valueOf(statusCode));
		excelActions.setValueInSpecificCell(excelPath, sheetNames, "statusLine", headerRowIndex, rowIndex, statusLine);
		excelActions.setValueInSpecificCell(excelPath, sheetNames, "contentType", headerRowIndex, rowIndex,
				contentType);

		System.out.println("Saved response for " + testCaseId);
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

// Add listener block
			writer.write("  <listeners>\n");
			writer.write("    <listener class-name=\"com.ep.app.utils.TestListener\"/>\n");
			writer.write("  </listeners>\n");

// Generate <test> blocks per case
			writeTestBlocks(writer, "testGETRequest", getCases);
			writeTestBlocks(writer, "testPOSTRequest", postCases);
			writeTestBlocks(writer, "testPUTRequest", putCases);
			writeTestBlocks(writer, "testDELETERequest", deleteCases);

			writer.write("</suite>\n");

			System.out.println("testng-api.xml created successfully.");
		} catch (IOException e) {
			System.err.println("Failed to write testng-generated.xml: " + e.getMessage());
		}
	}

	private static void writeTestBlocks(BufferedWriter writer, String methodName, List<String> caseIds)
			throws IOException {
		for (String caseID : caseIds) {
			writer.write("  <test name=\"" + methodName + "_" + caseID + "\">\n");
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

	public void executeChainedSteps(String filePath, String sheetName, String chainCaseID) {
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

			if (method == null || stepCaseID == null) {
				System.err.println("Skipping due to null method or caseID at step " + i);
				continue;
			}

			// ⭐⭐ FIX: If method = PUT, append ID to the endpoint ⭐⭐
			if (method.equalsIgnoreCase("PUT")) {
				dataContext.setUpdatedEndpoint(endpointFromExcel); // put {id} as is
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

			// ⭐ Execute Request
			makeRequest(method, stepCaseID, sheetName);

			// ⭐ Save ID after POST
			if (method.equalsIgnoreCase("POST")) {
				Response response = dataContext.getResponse();
				String postId = response.jsonPath().getString("id");
				if (postId == null) {
					postId = response.jsonPath().getString("data.id");
				}

				dataContext.setLastCreatedId(postId);
				System.out.println("🔥 Saved lastCreatedId = " + postId);
			}

			// ⭐ Verify status codes // Need to remove Updated by SE
//			switch (method.toUpperCase()) {
//			case "GET" -> verifyResponseCode("200");
//			case "POST" -> {
//				verifyResponseCode("200", "201");
//				updatePostIdInQueryParamForNextMatchingCaseID(sheetName, stepCaseID, i + 1, headerRowIndex);
//			}
//			case "PUT" -> verifyResponseCode("200");
//			case "DELETE" -> verifyResponseCode("204");
//			default -> throw new IllegalArgumentException("Unsupported method: " + method);
//			}

			// Skip validation for DELETE since response body is empty

			// determine expected status code
			int expectedStatusCode;
			if (method.equalsIgnoreCase("GET")) {

				// GET after DELETE → expect 404
				if ("DELETE".equalsIgnoreCase(prevMethod)) {
					expectedStatusCode = 404;
				} else {
					expectedStatusCode = 200; // normal GET
				}

			} else if (method.equalsIgnoreCase("POST")) {
				expectedStatusCode = 201;

			} else if (method.equalsIgnoreCase("PUT")) {
				expectedStatusCode = 200;

			} else if (method.equalsIgnoreCase("DELETE")) {
				expectedStatusCode = 204;

			} else {
				throw new IllegalArgumentException("Unsupported method: " + method);
			}

			// verify status code
			verifyResponseCode(String.valueOf(expectedStatusCode));

			// validate only when response has JSON body
			if (!method.equalsIgnoreCase("DELETE") && expectedStatusCode != 404) {
				validateResponseParams(stepCaseID, sheetName);
			}

			// update previous method
			prevMethod = method;

			saveApiResponse("APIData.xlsx", sheetName, stepCaseID, 0);
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

}
