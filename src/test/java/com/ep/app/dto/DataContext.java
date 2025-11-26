package com.ep.app.dto;

import java.util.List;

import com.changepond.test.framework.actions.APIActions;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class DataContext {

	private static RequestSpecification request;
	private Response response;
	private static String jsonBody;
	private List<Response> responsesData;
	private String excelPath;
	private String testCaseId;
	private String randomValue;
	private String lastCreatedId;
	private String updatedEndpoint;

	// *******************************************************************************************
	// Below fields are declared to randomly generate values and reuse those values
	// during run.
	// Also, to store data at run-time and retrieve and re-use for
	// validation/assertions.
	// *******************************************************************************************

	// *******************************************************************************************
	// Constructor - initialize page elements
	// instantiate api actions to use the reusable methods to perform actions on
	// AUT.
	// *******************************************************************************************

	public void setRequest(RequestSpecification request) {
		this.request = request;
	}

	public static RequestSpecification getRequest() {
		return request;
	}

	public void setResponse(Response response) {
		this.response = response;
	}

	public Response getResponse() {
		return response;
	}

	public void setJsonBody(String jsonBody) {
		this.jsonBody = jsonBody;
	}

	public static String getJsonBody() {
		return jsonBody;
	}

	public void setResponsesData(List<Response> responsesData) {
		this.responsesData = responsesData;
	}

	public List<Response> getResponsesData() {
		return responsesData;
	}

	public String getTestCaseId() {
		return testCaseId;
	}

	public void setTestCaseId(String testCaseId) {
		this.testCaseId = testCaseId;
	}

	// excell path holder

	public String getExcelPath() {
		return excelPath;
	}

	public void setExcelPath(String excelPath) {
		this.excelPath = excelPath;
	}

	public String getPayload() {

		return null;
	}

	public String getSavedResponseValue(String string) {
		// TODO Auto-generated method stub
		return null;
	}

	public void saveResponseValue(String string, String userId) {
		// TODO Auto-generated method stub

	}

	// Updated by SE

	public String getRandomValue() {
		return randomValue;
	}

	public void setRandomValue(String randomValue) {
		this.randomValue = randomValue;
	}

	public void setLastCreatedId(String id) {
		this.lastCreatedId = id;
	}

	public String getLastCreatedId() {
		return lastCreatedId;
	}

	public void setUpdatedEndpoint(String endpointFromExcel) {
		this.updatedEndpoint = endpointFromExcel;

	}

	public String getUpdatedEndpoint() {
		// TODO Auto-generated method stub
		return updatedEndpoint;
	}

	//
}
