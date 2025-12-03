package com.ep.app.tests;

import java.io.IOException;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.changepond.test.framework.actions.APIActions;
import com.changepond.test.framework.actions.ExcelActions;
import com.ep.app.dto.DataContext;
import com.ep.app.utils.ConfigReader;

public class ChainingTest {

	private final Common common;

	public ChainingTest() {
		DataContext dataContext = new DataContext();
		ExcelActions excelActions = new ExcelActions();
		APIActions apiActions = new APIActions();
		this.common = new Common(dataContext, excelActions, apiActions);
	}

	@Test
	@Parameters("chainCaseID")
	public void testChainingRequest(String chainCaseID) throws IOException {
		String filePath = Common.getExcelFilePath(ConfigReader.getConfigValue("excelFile"));
		common.executeChainedSteps(filePath, "chainingRequests", chainCaseID);
	}
}
