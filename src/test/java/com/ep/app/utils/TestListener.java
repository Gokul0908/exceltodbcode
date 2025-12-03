package com.ep.app.utils;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

public class TestListener implements ITestListener {

	// ✅ These are used by DashboardReport
	public static int passedCount = 0;
	public static int failedCount = 0;
	public static int skippedCount = 0;

	public static int postCount = 0;
	public static int getCount = 0;
	public static int putCount = 0;
	public static int deleteCount = 0;

	public static String executionMode = "Single Mode";

	@Override
	public void onStart(ITestContext context) {
		// Reset counters for fresh run
		passedCount = 0;
		failedCount = 0;
		skippedCount = 0;

		ExtentReportManager.initReport();
	}

	@Override
	public void onFinish(ITestContext context) {
		System.out.println("\n=== Final TestNG Results ===");
		System.out.println("Passed : " + passedCount);
		System.out.println("Failed : " + failedCount);
		System.out.println("Skipped: " + skippedCount);

		ExtentReportManager.flushReport();
	}

	@Override
	public void onTestStart(ITestResult result) {
		String testName = result.getMethod().getMethodName();
		Object[] params = result.getParameters();
		if (params.length > 0) {
			testName += " - " + params[0];
		}

		ExtentTest test = ExtentReportManager.createTest(testName);
		result.setAttribute("extentTest", test);
		test.log(Status.INFO, "Starting test: " + testName);

		String name = result.getMethod().getMethodName().toLowerCase();

		if (name.contains("post"))
			postCount++;
		if (name.contains("get"))
			getCount++;
		if (name.contains("put"))
			putCount++;
		if (name.contains("delete"))
			deleteCount++;

		if (postCount > 1)
			executionMode = "Chaining Mode";

	}

	@Override
	public void onTestSuccess(ITestResult result) {
		passedCount++; // ✅ increase pass count

		ExtentTest test = (ExtentTest) result.getAttribute("extentTest");
		if (test != null) {
			test.log(Status.PASS, "Test Passed");
		}
	}

	@Override
	public void onTestFailure(ITestResult result) {
		failedCount++; // ✅ increase fail count

		ExtentTest test = (ExtentTest) result.getAttribute("extentTest");
		if (test != null) {
			test.log(Status.FAIL, "Test Failed");
			test.log(Status.FAIL, result.getThrowable());
		}

		System.out.println("\n\n===== TEST FAILED: " + result.getName() + " =====");
		if (result.getThrowable() != null) {
			result.getThrowable().printStackTrace();
		}
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		skippedCount++; // ✅ increase skip count

		ExtentTest test = (ExtentTest) result.getAttribute("extentTest");
		if (test != null) {
			test.log(Status.SKIP, "Test Skipped");
		}

		System.out.println("\n===== TEST SKIPPED: " + result.getName() + " =====");
		if (result.getThrowable() != null) {
			result.getThrowable().printStackTrace();
		}
	}
}
