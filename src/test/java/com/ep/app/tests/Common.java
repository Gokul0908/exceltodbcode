package com.ep.app.tests; //New Code

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.ep.app.utils.ExtentReportManager;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.response.Response;
import com.google.gson.*;
import static io.restassured.RestAssured.given;

public class Common {

	private final DataContext dataContext;
	private static ExcelActions excelActions;

	static {
		if (excelActions == null) {
			excelActions = new ExcelActions();
		}
	}

	private final APIActions apiActions;

	public Common(DataContext dataContext, ExcelActions excelActions, APIActions apiActions) {
		this.dataContext = dataContext;
		Common.excelActions = excelActions;
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

		RequestSpecification request = dataContext.getRequest();

		if (!isNullOrEmpty(paramKey) && !isNullOrEmpty(paramValue) && !paramKey.equalsIgnoreCase("NA")
				&& !paramValue.equalsIgnoreCase("NA")) {

			System.out.println("Setting path parameter: " + paramKey + " = " + paramValue);
			request = apiActions.setSinglePathParameter(request, paramKey, paramValue);
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
					throw new RuntimeException("Invalid baseauth format in caseID: " + caseID);
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

	// 🔥🔥 PAYLOAD FIX — Auto-generate UNIQUE email
	public void preparePayload(String caseID, String sheet) {
		Map<String, String> data = getTestData(caseID, sheet);

		String payload = data.get("payLoad");

		if (payload.contains("\"email\"")) {
			String uniqueEmail = "api_" + System.currentTimeMillis() + "@gmail.com";
			payload = payload.replaceAll("\"email\"\\s*:\\s*\"[^\"]+\"", "\"email\": \"" + uniqueEmail + "\"");
			System.out.println("Generated Email: " + uniqueEmail);
		}

		if (isNullOrEmpty(payload)) {
			throw new RuntimeException("No 'payLoad' found in Excel for caseID: " + caseID);
		}

		dataContext.setJsonBody(payload);
	}

	public void makeRequest(String method, String caseID, String sheet) {

		Map<String, String> data = getTestData(caseID, sheet);

		String endpoint = data.get("endPoint");
		String Request = data.get("action");

		if (isNullOrEmpty(endpoint)) {
			throw new IllegalArgumentException("Endpoint is missing for caseID: " + caseID);
		}
		if (isNullOrEmpty(method)) {
			throw new IllegalArgumentException("HTTP method is missing for caseID: " + caseID);
		}

		RequestSpecification request = dataContext.getRequest();

		if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
			String body = dataContext.getJsonBody();
			if (isNullOrEmpty(body)) {
				throw new RuntimeException("Request body is missing for caseID: " + caseID);
			}
			request.body(body);
		}

		Response response;
		switch (method.toUpperCase()) {
		case "POST":
			response = request.post(endpoint);
			break;
		case "PUT":
			response = request.put(endpoint);
			break;
		case "DELETE":
			response = request.delete(endpoint);
			break;
		case "GET":
			response = request.get(endpoint);
			break;
		default:
			throw new IllegalArgumentException("Unsupported HTTP method: " + method);
		}

		dataContext.setResponse(response);

		ITestResult result = Reporter.getCurrentTestResult();
		ExtentTest test = (ExtentTest) result.getAttribute("extentTest");

		test.log(Status.INFO, "Response Body:");
		test.log(Status.INFO, MarkupHelper.createCodeBlock(response.asString(), CodeLanguage.JSON));
		test.log(Status.INFO, "StatusCode: " + response.getStatusCode());
		test.log(Status.INFO, "StatusLine: " + response.getStatusLine());
		test.log(Status.INFO, "ContentType: " + response.getContentType());
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

		Map<String, String> data = getTestData(caseID, sheet);
		String[] verificationParams = data.get("verificationParam").split(",");
		String[] expectedValues = data.get("verificationParamValue").split(",");

		if (verificationParams == null || expectedValues == null) {
			throw new SkipException("Missing verification data for caseID: " + caseID);
		}

		if (verificationParams.length != expectedValues.length) {
			throw new IllegalArgumentException("Mismatch between parameter and value count for caseID: " + caseID);
		}

		for (int i = 0; i < verificationParams.length; i++) {
			String param = verificationParams[i].trim();
			String expected = expectedValues[i].trim();

			Object actualObj = response.jsonPath().get("data." + param);
			if (actualObj == null) {
				actualObj = response.jsonPath().get(param);
			}
			String actual = actualObj != null ? actualObj.toString().trim() : "null";

			Assert.assertEquals(actual, expected,
					String.format("Mismatch for [%s]: expected=%s, actual=%s", param, expected, actual));

			System.out.println("Verified: " + param + " = " + expected);
		}
	}

	public void validateResponseAgainstJsonSchema(String schemaFileName) {
		String schemaFilePath = "src/test/resources/inputJsonFile/" + schemaFileName;

		Response response = dataContext.getResponse();
		Assert.assertNotNull(response);

		apiActions.validateResponseWithJSONSchemaFile(response, schemaFilePath);
	}

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

		String postId = null;
		if (response.jsonPath().get("data.id") != null) {
			postId = response.jsonPath().getString("data.id");
		} else if (response.jsonPath().get("id") != null) {
			postId = response.jsonPath().getString("id");
		}

		if (postId == null || postId.isEmpty()) {
			throw new IllegalStateException("Post ID missing from response.");
		}

		String excelPath = System.getProperty("user.dir") + "/src/test/resources/testData/APIData.xlsx";
		List<List<String>> rows = excelActions.extractExcelDataAsList(excelPath, sheetName, 0, 0, "dd-MM-yyyy");

		int caseIDColumnIndex = rows.get(headerRowIndex).indexOf("caseID");

		List<String> sheetNames = Collections.singletonList(sheetName);

		for (int i = currentRowIndex + 1; i < rows.size(); i++) {
			List<String> row = rows.get(i);
			if (row.size() > caseIDColumnIndex && row.get(caseIDColumnIndex).trim().equalsIgnoreCase(currentCaseID)) {
				excelActions.setValueInSpecificCell(excelPath, sheetNames, "queryParamValue", headerRowIndex, i,
						postId);
				System.out.println("Updated queryParamValue in row " + i + " for caseID: " + currentCaseID);
			}
		}
	}

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

	public static int getColumnIndex(Row headerRow, String headerName) {
		for (Cell cell : headerRow) {
			if (cell.getStringCellValue().trim().equalsIgnoreCase(headerName.trim())) {
				return cell.getColumnIndex();
			}
		}
		return -1;
	}

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

			if (!getCases.isEmpty() || !postCases.isEmpty() || !putCases.isEmpty() || !deleteCases.isEmpty()) {
				writer.write("  <test name=\"APIMethodsTest\">\n");
				writer.write("    <classes>\n");
				writer.write("      <class name=\"com.ep.app.tests.APIMethods\"/>\n");
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

			writer.write("  <listeners>\n");
			writer.write("    <listener class-name=\"com.ep.app.utils.TestListener\"/>\n");
			writer.write("  </listeners>\n");

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

		for (int i = 0; i < chainSteps.size(); i++) {
			Map<String, String> stepData = chainSteps.get(i);

			if (!"Yes".equalsIgnoreCase(stepData.get("isRun")))
				continue;

			String method = stepData.get("action");
			String stepCaseID = stepData.get("caseID");

			if (method == null || stepCaseID == null) {
				System.err.println("Skipping due to null method or caseID at step " + i);
				continue;
			}

			System.out.println("Executing: " + method + " for caseID: " + stepCaseID);

			setBaseUrl(stepCaseID, sheetName);
			if (!"GET".equalsIgnoreCase(method)) {
				preparePayload(stepCaseID, sheetName);
			}

			setAuthToken(stepCaseID, sheetName);
			makeRequest(method, stepCaseID, sheetName);

			switch (method.toUpperCase()) {
			case "GET" -> verifyResponseCode("200");
			case "POST" -> {
				verifyResponseCode("200", "201");
				updatePostIdInQueryParamForNextMatchingCaseID(sheetName, stepCaseID, i + 1, headerRowIndex);
			}
			case "PUT" -> verifyResponseCode("200");
			case "DELETE" -> verifyResponseCode("204");
			default -> throw new IllegalArgumentException("Unsupported method: " + method);
			}

			validateResponseParams(stepCaseID, sheetName);
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
			JsonObject first = dataElement.getAsJsonArray().get(0).getAsJsonObject();
			newJson.add("data", first);
		} else {
			newJson.add("data", dataElement.getAsJsonObject());
		}

		return newJson.toString();
	}

	public void setPathOrQueryParam(String caseID, String sheet) {

		Map<String, String> data = getTestData(caseID, sheet);
		RequestSpecification req = dataContext.getRequest();

		String key = data.get("queryParam");
		String value = data.get("queryParamValue");

		if (isNullOrEmpty(key) || isNullOrEmpty(value)) {
			return;
		}

		String endpoint = data.get("endPoint");

		// If endpoint contains {...} → USE PATH PARAM
		if (endpoint != null && endpoint.contains("{")) {
			req = apiActions.setSinglePathParameter(req, key, value);
		}
		// Else use query param
		else {
			req = apiActions.setSingleQueryParameter(req, key, value);
		}

		dataContext.setRequest(req);
	}

}
