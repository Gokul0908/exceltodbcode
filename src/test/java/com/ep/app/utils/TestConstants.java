package com.ep.app.utils;

public class TestConstants {

	// *******************************************************************************************
	// Declare and initialize the constants for Run Config properties.
	// *******************************************************************************************

	public static final String LOCAL_RUN = "localRun";
	public static final String LOCAL_RUN_OPTION_YES = "Yes";
	public static final String LOCAL_RUN_OPTION_NO = "No";

	public static final String RUN_ENVIRONMENT = "runEnv";
	public static final String LOCAL_ENVIRONMENT = "local";
	public static final String DEV_ENVIRONMENT = "dev";
	public static final String TEST_ENVIRONMENT = "test";
	public static final String UAT_ENVIRONMENT = "uat";
	public static final String BASE_ENVIRONMENT = "base";

	public static final String LOCAL_AUT_URL = "local_autURL";
	public static final String DEV_AUT_URL = "dev_autURL";
	public static final String TEST_AUT_URL = "test_autURL";
	public static final String UAT_AUT_URL = "uat_autURL";
	public static final String BASE_URL = "baseURL";

	// *******************************************************************************************
	// Declare and initialize the constants related to web elements - enablement.
	// *******************************************************************************************
	public static final String ELEMENT_ENABLED = "enabled";
	public static final String ELEMENT_DISABLED = "disabled";

	// *******************************************************************************************
	// Declare and initialize the constants related to date picker - month and year
	// dropdowns.
	// *******************************************************************************************
	public static final String DATE_PICKER_YEAR = "year";
	public static final String DATE_PICKER_MONTH = "month";

	// *******************************************************************************************
	// Declare and initialize the constants related to element selection from a
	// LIST.
	// *******************************************************************************************
	public static final String ELEMENT_SELECTION = "NO";

	// *******************************************************************************************
	// Declare and initialize the constant related to attribute name for retrieving
	// attribute value.
	// *******************************************************************************************
	public static final String TEXT_FIELD_VALUE_ATTRIBUTE = "value";

	// *******************************************************************************************
	// Declare and initialize the constants for Date formats.
	// *******************************************************************************************
	public static final String DATE_FORMAT_MONTH = "MMM";

	// *******************************************************************************************
	// Declare and initialize the constants for Azure Key Vault keywords.
	// *******************************************************************************************
	public static final String CLIENT_ID = "clientId";
	public static final String CLIENT_SECRET = "clientSecret";
	public static final String TENANT_ID = "tenantId";
	public static final String VAULT_URL = "https://" + System.getenv("kvName") + ".vault.azure.net";
	public static final String GET_SECRET = "get secret";
	public static final String CLEAR_SECRET = "clear secret";
	public static final int SECRET_COUNT = 9;

	public static final String USER_ROLE = "userRole";

	// *******************************************************************************************
	// Declare and initialize the constants for screenshot file format.
	// *******************************************************************************************
	public static final String SCREENSHOT_FORMAT = "image/png";

	public static final String DOWNLOAD_DIRECTORY = "";

}