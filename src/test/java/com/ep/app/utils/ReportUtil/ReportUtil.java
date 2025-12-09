package com.ep.app.utils.ReportUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportUtil {

	public static String getReportFilePath() {
		String subFolder = "APIResults";

		String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		String basePath = System.getProperty("user.dir") + "/Reports/" + subFolder + "/" + date;

		File dateFolder = new File(basePath);
		if (!dateFolder.exists()) {
			dateFolder.mkdirs();
		}

		int runNumber = 1;
		while (new File(basePath + "/Report_Run" + runNumber + ".xlsx").exists()) {
			runNumber++;
		}

		return basePath + "/Report_Run" + runNumber + ".xlsx";
	}
}