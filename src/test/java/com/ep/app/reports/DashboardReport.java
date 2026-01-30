package com.ep.app.reports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.xml.XmlSuite;

public class DashboardReport implements IReporter {

	// Example response time list (you already have this)
	List<Integer> responseTimes = Arrays.asList(120, 98, 200, 350, 180);

	// Convert into JS-valid array string ➝ [120,98,200,350,180]
	String responseTimesJs = responseTimes.toString();

	// ---------- Holder for latest Excel summary ----------
	private static class ExcelSummary {
		int passed;
		int failed;
		int skipped;

		int postCount;
		int getCount;
		int putCount;
		int deleteCount;

		String tableRowsHtml; // <tr><td>caseID</td><td>action</td><td>STATUS</td></tr>
		String statusCodeLabelsJs; // e.g. ['200','201','404']
		String statusCodeCountsJs; // e.g. [5,2,1]
		String sourceFile;

	}

	// ---------- Holder for 1 run (used in trend chart) ----------
	private static class RunSummary {
		String label; // e.g. 2025-12-03-Report_Run5
		int passed;
		int failed;
		int skipped;

		RunSummary(String label, int passed, int failed, int skipped) {
			this.label = label;
			this.passed = passed;
			this.failed = failed;
			this.skipped = skipped;
		}
	}

	// ======================================================
	// TestNG entry point
	// ======================================================
	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {

		// 1) Duration from TestNG (start → end of all suites)
		long minStart = Long.MAX_VALUE;
		long maxEnd = 0L;

		for (ISuite suite : suites) {
			for (ISuiteResult suiteResult : suite.getResults().values()) {
				ITestContext ctx = suiteResult.getTestContext();
				if (ctx.getStartDate() != null) {
					minStart = Math.min(minStart, ctx.getStartDate().getTime());
				}
				if (ctx.getEndDate() != null) {
					maxEnd = Math.max(maxEnd, ctx.getEndDate().getTime());
				}
			}
		}
		long durationSec = (maxEnd > minStart && minStart != Long.MAX_VALUE) ? (maxEnd - minStart) / 1000 : 0;

		// 2) Build summary from latest Excel report
		ExcelSummary excelSummary = loadExcelSummaryFromLatestReport();
		if (excelSummary == null) {
			System.out.println("⚠ Could not build Excel summary. Dashboard will not be generated.");
			return;
		}

		// 3) Build JS arrays for last 5 runs
		String[] trendJs = buildTrendArraysJs();
		String runLabelsJs = trendJs[0];
		String passTrendJs = trendJs[1];
		String failTrendJs = trendJs[2];
		String skipTrendJs = trendJs[3];

		// 4) Generate HTML dashboard
		generateHtml(excelSummary.passed, excelSummary.failed, excelSummary.skipped, durationSec,
				excelSummary.tableRowsHtml, excelSummary.postCount, excelSummary.getCount, excelSummary.putCount,
				excelSummary.deleteCount, excelSummary.statusCodeLabelsJs, excelSummary.statusCodeCountsJs, runLabelsJs,
				passTrendJs, failTrendJs, skipTrendJs);
	}

	// ======================================================
	// Excel summary for latest report (for table + status codes + counts)
	// ======================================================
	private ExcelSummary loadExcelSummaryFromLatestReport() {
		try {
			File root = new File("Reports" + File.separator + "APIResults");
			if (!root.exists() || !root.isDirectory()) {
				System.out.println("⚠ APIResults folder not found: " + root.getAbsolutePath());
				return null;
			}

			// --- latest date folder ---
			File latestDir = null;
			File[] dateDirs = root.listFiles(File::isDirectory);
			if (dateDirs == null || dateDirs.length == 0) {
				System.out.println("⚠ No date folders under: " + root.getAbsolutePath());
				return null;
			}
			for (File d : dateDirs) {
				if (latestDir == null || d.lastModified() > latestDir.lastModified()) {
					latestDir = d;
				}
			}

			// --- latest Report_Run*.xlsx inside that folder ---
			File latestReport = null;
			File[] reportFiles = latestDir
					.listFiles((dir, name) -> name.startsWith("Report_Run") && name.endsWith(".xlsx"));
			if (reportFiles == null || reportFiles.length == 0) {
				System.out.println("⚠ No Report_Run*.xlsx under: " + latestDir.getAbsolutePath());
				return null;
			}
			for (File f : reportFiles) {
				if (latestReport == null || f.lastModified() > latestReport.lastModified()) {
					latestReport = f;
				}
			}

			ExcelSummary summary = new ExcelSummary();
			summary.sourceFile = latestReport.getAbsolutePath();

			StringBuilder tableRows = new StringBuilder();
			Map<Integer, Integer> statusCodeCounts = new HashMap<>();

			try (FileInputStream fis = new FileInputStream(latestReport);
					Workbook workbook = WorkbookFactory.create(fis)) {

				// Loop all sheets – any sheet with isRun + caseID + action + statusCode will be
				// used
				for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
					Sheet sheet = workbook.getSheetAt(s);
					if (sheet == null)
						continue;

					String sheetName = sheet.getSheetName();
					Row headerRow = sheet.getRow(0);
					if (headerRow == null)
						continue;

					int colCaseId = -1;
					int colAction = -1;
					int colStatusCode = -1;
					int colIsRun = -1;

					for (int c = 0; c < headerRow.getLastCellNum(); c++) {
						Cell cell = headerRow.getCell(c);
						String header = getStringCell(cell);
						if (header.equalsIgnoreCase("caseID")) {
							colCaseId = c;
						} else if (header.equalsIgnoreCase("action")) {
							colAction = c;
						} else if (header.equalsIgnoreCase("statusCode")) {
							colStatusCode = c;
						} else if (header.equalsIgnoreCase("isRun")) {
							colIsRun = c;
						}
					}

					if (colCaseId == -1 || colIsRun == -1) {
						// Not a test sheet
						continue;
					}

					for (int r = 1; r <= sheet.getLastRowNum(); r++) {
						Row row = sheet.getRow(r);
						if (row == null)
							continue;

						String isRunVal = getStringCell(row.getCell(colIsRun));
						if (!"yes".equalsIgnoreCase(isRunVal)) {
							continue; // only executed rows
						}

						String caseId = getStringCell(row.getCell(colCaseId));
						if (caseId.isEmpty())
							continue;

						String action = (colAction >= 0) ? getStringCell(row.getCell(colAction)) : "";

						int statusCode = 0;
						if (colStatusCode >= 0) {
							String scStr = getStringCell(row.getCell(colStatusCode));
							if (!scStr.isEmpty()) {
								try {
									statusCode = (int) Double.parseDouble(scStr.trim());
								} catch (Exception ignored) {
									statusCode = 0;
								}
							}
						}

						// ----- method counts -----
						if ("POST".equalsIgnoreCase(action))
							summary.postCount++;
						else if ("GET".equalsIgnoreCase(action))
							summary.getCount++;
						else if ("PUT".equalsIgnoreCase(action))
							summary.putCount++;
						else if ("DELETE".equalsIgnoreCase(action))
							summary.deleteCount++;

						// ----- PASS / FAIL / SKIP (by status code) -----
						String statusText;
						String color;

						if (statusCode == 0) {
							summary.skipped++;
							statusText = "SKIPPED";
							color = "orange";
						} else if (statusCode == 200 || statusCode == 201 || statusCode == 204) {
							summary.passed++;
							statusText = "PASS";
							color = "green";
						} else {
							summary.failed++;
							statusText = "FAIL";
							color = "red";
						}

						if (statusCode > 0) {
							statusCodeCounts.merge(statusCode, 1, Integer::sum);
						}

						// ----- HTML table row: caseID | action | status -----
						String displayCase = caseId;
						if (sheetName != null && !sheetName.trim().isEmpty()) {
							displayCase = caseId + " (" + sheetName + ")";
						}
						tableRows.append("<tr>").append("<td>").append(displayCase).append("</td>").append("<td>")
								.append(action).append("</td>").append("<td style='font-weight:bold;color:")
								.append(color).append(";'>").append(statusText).append("</td>").append("</tr>");
					}
				}
			}

			summary.tableRowsHtml = tableRows.toString();

			// Build JS arrays for status code chart
			if (statusCodeCounts.isEmpty()) {
				summary.statusCodeLabelsJs = "['200']";
				summary.statusCodeCountsJs = "[0]";
			} else {
				StringBuilder labels = new StringBuilder("[");
				StringBuilder counts = new StringBuilder("[");

				List<Integer> codes = new ArrayList<>(statusCodeCounts.keySet());
				Collections.sort(codes);

				boolean first = true;
				for (Integer code : codes) {
					if (!first) {
						labels.append(",");
						counts.append(",");
					}
					labels.append("'").append(code).append("'");
					counts.append(statusCodeCounts.get(code));
					first = false;
				}
				labels.append("]");
				counts.append("]");

				summary.statusCodeLabelsJs = labels.toString();
				summary.statusCodeCountsJs = counts.toString();
			}

			System.out.println("✅ Excel summary built from: " + summary.sourceFile + " | Pass=" + summary.passed
					+ ", Fail=" + summary.failed + ", Skip=" + summary.skipped);

			return summary;

		} catch (Exception e) {
			System.out.println("❌ Error reading Excel summary for dashboard: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	// ======================================================
	// Helpers for last 5 runs trend
	// ======================================================
	private String[] buildTrendArraysJs() {
		List<RunSummary> runs = loadLastRunSummaries(5);

		if (runs.isEmpty()) {
			return new String[] { "[]", "[]", "[]", "[]" };
		}

		StringBuilder labels = new StringBuilder();
		StringBuilder pass = new StringBuilder();
		StringBuilder fail = new StringBuilder();
		StringBuilder skip = new StringBuilder();

		for (int i = 0; i < runs.size(); i++) {
			RunSummary r = runs.get(i);
			if (i > 0) {
				labels.append(",");
				pass.append(",");
				fail.append(",");
				skip.append(",");
			}
			labels.append("'").append(r.label.replace("'", "\\'")).append("'");
			pass.append(r.passed);
			fail.append(r.failed);
			skip.append(r.skipped);
		}

		return new String[] { "[" + labels + "]", "[" + pass + "]", "[" + fail + "]", "[" + skip + "]" };
	}

	private List<RunSummary> loadLastRunSummaries(int maxRuns) {
		List<RunSummary> result = new ArrayList<>();

		File root = new File(
				System.getProperty("user.dir") + File.separator + "Reports" + File.separator + "APIResults");

		if (!root.exists() || !root.isDirectory()) {
			System.out.println("⚠ No APIResults folder found at: " + root.getAbsolutePath());
			return result;
		}

		File[] dateDirs = root.listFiles(File::isDirectory);
		if (dateDirs == null || dateDirs.length == 0)
			return result;

		// latest date first
		Arrays.sort(dateDirs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

		outer: for (File dateDir : dateDirs) {
			File[] reportFiles = dateDir
					.listFiles((dir, name) -> name.startsWith("Report_Run") && name.endsWith(".xlsx"));
			if (reportFiles == null || reportFiles.length == 0)
				continue;

			// latest run first
			Arrays.sort(reportFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

			for (File report : reportFiles) {
				RunSummary rs = summarizeReportFile(report);
				if (rs != null) {
					result.add(rs);
				}
				if (result.size() >= maxRuns)
					break outer;
			}
		}

		// reverse so chart shows oldest → latest
		Collections.reverse(result);
		return result;
	}

	private RunSummary summarizeReportFile(File reportFile) {
		try (FileInputStream fis = new FileInputStream(reportFile); Workbook workbook = WorkbookFactory.create(fis)) {

			int passed = 0, failed = 0, skipped = 0;

			for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
				Sheet sheet = workbook.getSheetAt(s);
				if (sheet == null)
					continue;

				Row headerRow = sheet.getRow(0);
				if (headerRow == null)
					continue;

				int colIsRun = -1;
				int colStatusCode = -1;

				for (int c = 0; c < headerRow.getLastCellNum(); c++) {
					Cell cell = headerRow.getCell(c);
					String header = getStringCell(cell);
					if (header.equalsIgnoreCase("isRun"))
						colIsRun = c;
					if (header.equalsIgnoreCase("statusCode"))
						colStatusCode = c;
				}

				if (colStatusCode == -1)
					continue; // Skip sheets without statusCode

				for (int r = 1; r <= sheet.getLastRowNum(); r++) {
					Row row = sheet.getRow(r);
					if (row == null)
						continue;

					if (colIsRun != -1) {
						String runVal = getStringCell(row.getCell(colIsRun));
						if (!"yes".equalsIgnoreCase(runVal))
							continue;
					}

					String scStr = getStringCell(row.getCell(colStatusCode));
					if (scStr == null || scStr.trim().isEmpty()) {
						skipped++;
						continue;
					}

					try {
						int code = (int) Double.parseDouble(scStr.trim());
						if (code >= 200 && code < 300)
							passed++;
						else
							failed++;
					} catch (Exception e) {
						skipped++;
					}
				}
			}

			String label = reportFile.getParentFile().getName() + "-" + reportFile.getName().replace(".xlsx", "");

			return new RunSummary(label, passed, failed, skipped);

		} catch (Exception e) {
			System.out.println("⚠ Error summarizing report: " + reportFile.getAbsolutePath());
			return null;
		}
	}

	// ======================================================
	// Cell → String helper
	// ======================================================
	private String getStringCell(Cell cell) {
		if (cell == null)
			return "";
		try {
			switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue().trim();
			case NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					return cell.getDateCellValue().toString();
				} else {
					double d = cell.getNumericCellValue();
					if (d == (long) d) {
						return String.valueOf((long) d);
					} else {
						return String.valueOf(d);
					}
				}
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			case FORMULA:
				try {
					return cell.getStringCellValue().trim();
				} catch (Exception e) {
					return String.valueOf(cell.getNumericCellValue());
				}
			default:
				return "";
			}
		} catch (Exception e) {
			return "";
		}
	}

	// ======================================================
	// Logo copy
	// ======================================================
	private void copyLogo() {
		try {
			File src = new File("src/test/resources/logo/company_logo.jpg");
			if (!src.exists()) {
				System.out.println("❌ Logo Not Found: " + src.getPath());
				return;
			}

			File destDir = new File("Reports/logo");
			destDir.mkdirs();
			File dest = new File(destDir, "company_logo.jpg");

			try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dest)) {
				byte[] buffer = new byte[1024];
				int len;
				while ((len = in.read(buffer)) > 0) {
					out.write(buffer, 0, len);
				}
			}

			System.out.println("✔ Logo Copied Successfully!");

		} catch (Exception e) {
			System.out.println("❌ Logo Copy Error: " + e.getMessage());
		}
	}

	// ======================================================
	// HTML generation (your final HTML + dynamic data)
	// ======================================================
	private void generateHtml(int passed, int failed, int skipped, long durationSec, String tableRows, int post,
			int get, int put, int del, String statusCodeLabelsJs, String statusCodeCountsJs, String runLabelsJs,
			String passTrendJs, String failTrendJs, String skipTrendJs) {
		try {
			File file = new File("Reports/API_Dashboard.html");
			file.getParentFile().mkdirs();
			copyLogo();

			String date = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date());
			String env = System.getProperty("env", "QA");

			String durationText = (durationSec > 0) ? String.valueOf(durationSec) : "--";

			String html = "<!DOCTYPE html>" + "<html lang='en'>" + "<head>" + "<meta charset='UTF-8'>"
					+ "<title>API Automation Execution Dashboard</title>"
					+ "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>"

					+ "<style>"
					+ "body{margin:0;font-family:Arial,Helvetica,sans-serif;background:#e6f2ff;position:relative;color:#003366;}" // 🌈
																																	// Change
																																	// Background
																																	// Color
																																	// here
					+ ".page{position:relative;z-index:10;padding:20px 0 40px 0;text-align:center;}"

					+ ".watermark {\r\n" + "    position: fixed;\r\n" + "    top: 50%;\r\n" + "    left: 0;\r\n"
					+ "    transform: translateY(-50%);\r\n" + "    opacity: 0.10;\r\n" + "    z-index: 0;\r\n"
					+ "    pointer-events: none;\r\n" + "    animation: slideWatermark 5s linear infinite;\r\n"
					+ "}\r\n" + "\r\n" + ".watermark img {\r\n" + "    width: 650px;\r\n" + "    height: auto;\r\n"
					+ "}\r\n" + "\r\n" + "@keyframes slideWatermark {\r\n"
					+ "    0%   { transform: translateX(-50%) translateY(-50%); }\r\n"
					+ "    50%  { transform: translateX(50%) translateY(-50%); }\r\n"
					+ "    100% { transform: translateX(-50%) translateY(-50%); }\r\n" + "}\r\n" + "" + "" // Animation
																											// Speed
																											// change
																											// here

					+ ".header-row{display:flex;align-items:center;justify-content:center;gap:25px;margin-bottom:10px;}"
					+ ".logo{position:absolute;top:15px;left:25px;}" + ".logo img{height:150px;width:auto;}" // 🔰 //
																												// Change
																												// Company
																												// Logo
																												// size
																												// here

					+ ".title{font-size:28px;font-weight:bold;color:#002b5c;}" // 🎯 Title Font Color / Size
					+ ".subtitle{font-size:13px;color:#00509e;margin-top:4px;}" // ✏ Subtitle Color

					// 📌 Stats cards (Passed / Failed / Skipped)
					+ ".stats{display:flex;justify-content:center;gap:18px;margin:22px auto 8px auto;}"
					+ ".card{min-width:140px;padding:12px 22px;border-radius:24px;font-size:16px;font-weight:bold;color:#fff;box-shadow:0 4px 10px rgba(0,0,0,0.15);background:#999;transition:0.3s;}"
					+ ".card:hover{transform:scale(1.06);box-shadow:0 6px 16px rgba(0,0,0,0.25);}"

					// 🟢 Passed / 🔴 Failed / 🟡 Skipped Colors
					+ ".p{background:#28a745;}" // Change Passed color
					+ ".f{background:#dc3545;}" // Change Failed color
					+ ".s{background:#ffc107;color:#333;}" // Change Skipped color

					+ ".callCounts{text-align:center;margin:10px auto;font-size:14px;font-weight:bold;}"
					+ ".callCounts span{margin:0 8px;padding:4px 12px;border-radius:12px;background:#d9e6ff;border:1px solid #b3ccff;}"

					+ ".chart-container{margin:10px auto;}" // 📊 Change chart spacing

					+ "table{width:80%;margin:30px auto 0 auto;border-collapse:collapse;background:#fff;box-shadow:0 4px 16px rgba(0,0,0,0.12);}"
					// 📍 Table border & row design
					+ "th,td{border:1px solid #cdd4e0;padding:9px 6px;font-size:14px;text-align:center;}"
					+ "th{background:#003366;color:#fff;}" // 🏁 Table Header Color
					+ "tr:nth-child(even){background:#f4f7ff;}" // 🔹 Alternate Row Color

					+ ".footer{margin-top:25px;font-size:14px;font-weight:bold;color:#004a99;letter-spacing:0.6px;}" // 📌
																														// Footer
																														// Text
																														// Color

					+ ".summary-cards{display:flex;justify-content:center;gap:18px;margin:18px auto;}"
					+ ".summary-card{min-width:160px;padding:12px 20px;border-radius:18px;font-size:15px;font-weight:bold;background:#ffffff;box-shadow:0 4px 10px rgba(0,0,0,0.15);border-left:8px solid;}"
					+ ".summary-card.avg{border-color:#0d6efd;color:#004085;}" // Avg Response Card Style
					+ ".summary-card.fast{border-color:#28a745;color:#155724;}" // Fastest Card Style
					+ ".summary-card.slow{border-color:#dc3545;color:#721c24;}" // Slowest Card Style

					+ "</style>"

					+ "</head><body>"

					// 🖼️ Watermark Image Source
					+ "<div class='watermark'><img src='logo/Watermark.png' alt='logo'></div>"

					+ "<div class='page'>"
					// 🖼️ Company Logo Source
					+ "<div class='logo'><img src='logo/company_logo.jpg' alt='Logo'></div>"
					+ "<div class='header-row'><div class='title'>API Automation Execution Dashboard</div></div>"

					// 🕒 Dynamic execution details
					+ "<div class='subtitle'>Environment: " + env + " | Execution Mode: Suite | Executed At: " + date
					+ " | Duration: " + durationText + " Sec</div>"

					// ================= STATS =================
					+ "<div class='stats'>" + "<div class='card p'>Passed: " + passed + "</div>"
					+ "<div class='card f'>Failed: " + failed + "</div>" + "<div class='card s'>Skipped: " + skipped
					+ "</div>" + "</div>"

					// ================= API Count Row =================
					+ "<div class='callCounts'>" + "<span>POST: " + post + "</span>" + "<span>GET: " + get + "</span>"
					+ "<span>PUT: " + put + "</span>" + "<span>DELETE: " + del + "</span>" + "</div>"

					// ================= Summary Cards =================
					+ "<div class='summary-cards'>"
					+ "<div class='summary-card avg'>Avg Response Time: <span id='avgTime'>--</span> ms</div>"
					+ "<div class='summary-card fast'>Fastest API: <span id='fastTime'>--</span> ms</div>"
					+ "<div class='summary-card slow'>Slowest API: <span id='slowTime'>--</span> ms</div>" + "</div>"

					// ================= Charts =================
					// Layout Alignment --> Edit gap here if needed
					+ "<div style='display:flex;justify-content:center;align-items:flex-start;gap:60px;margin-top:35px;'>"
					+ "<div class='chart-container' style='width:350px;'><canvas id='pieChart'></canvas></div>" // Doughnut
																												// chart
																												// width
					+ "<div class='chart-container' style='width:600px;height:400px;'><canvas id='trendChart'></canvas></div>" // Bar
					// chart
					// width
					+ "</div>"

					// Shared Legend Style
					+ "<div style='text-align:center;margin-top:12px;font-weight:bold;'>"
					+ "<span style='color:#28a745;margin:0 15px;'>■ Passed</span>"
					+ "<span style='color:#dc3545;margin:0 15px;'>■ Failed</span>"
					+ "<span style='color:#ffc107;margin:0 15px;'>■ Skipped</span>" + "</div>"

					// ================= TABLE =================
					+ "<table id='resultTable'>" + "<tr><th>Test Case ID</th><th>Action</th><th>Status</th></tr>"
					+ tableRows + "</table>"

					// ================ FOOTER =================
					+ "<div class='footer'>Powered by Changepond Technologies | API QA Automation</div>" + "</div>"

					// ================= JAVASCRIPT =================
					+ "<script>" + "var runLabels=" + runLabelsJs + ";" + "var passTrend=" + passTrendJs + ";"
					+ "var failTrend=" + failTrendJs + ";" + "var skipTrend=" + skipTrendJs + ";"

					// 📊 Trend Chart
					+ "new Chart(document.getElementById('trendChart'),{\r\n" + "    type:'bar',\r\n" + "    data:{\r\n"
					+ "        labels: ['Run 1', 'Run 2', 'Run 3', 'Run 4', 'Run 5'],\r\n" + "        datasets:[\r\n"
					+ "            {label:'Passed',data:passTrend,backgroundColor:'#28a745'},\r\n"
					+ "            {label:'Failed',data:failTrend,backgroundColor:'#dc3545'},\r\n"
					+ "            {label:'Skipped',data:skipTrend,backgroundColor:'#ffc107'}\r\n" + "        ]\r\n"
					+ "    },\r\n" + "    options:{\r\n" + "        responsive:true,\r\n"
					+ "        maintainAspectRatio:false,\r\n" + "        animation:{\r\n"
					+ "            duration:3456,\r\n" + "            easing:'easeOutBounce'\r\n" + "        },\r\n"
					+ "        plugins:{legend:{display:false}},\r\n" + "        scales:{\r\n" + "            y:{\r\n"
					+ "                beginAtZero:true,\r\n" + "                ticks:{\r\n"
					+ "                    stepSize:1,\r\n" + "                    precision:0\r\n"
					+ "                },\r\n"
					+ "                suggestedMax:Math.max(...passTrend, ...failTrend, ...skipTrend)+1,\r\n"
					+ "                title:{display:true,text:'Test Case Count'}\r\n" + "            },\r\n"
					+ "            x:{title:{display:true,text:'Last 5 Runs ----->'}}\r\n" + "        }\r\n"
					+ "    }\r\n" + "});\r\n" + "" + "" + ""
					// 🥯 Doughnut Chart
					+ "new Chart(document.getElementById('pieChart'), {" + "  type: 'doughnut'," + "  data: {"
					+ "    labels: ['Passed','Failed','Skipped']," + "    datasets: [{" + "      data: [" + passed + ","
					+ failed + "," + skipped + "]," + "      backgroundColor: ['#28a745', '#dc3545', '#ffc107'],"
					+ "      borderWidth: 1" + "    }]" + "  }," + "  options: {" + "    cutout: '60%',"
					+ "    responsive: true," + "    animation: {" + "      animateRotate: true,"
					+ "      animateScale: true," + "      duration: 2000," + "      easing: 'easeOutElastic',"
					+ "      delay: 300" + "    }," + "    plugins: {" + "      legend: { display: false }" + "    }"
					+ "  }" + "});"

					// 📌 Response Time Summary Update
					+ "var rt=" + responseTimesJs + ";"
					+ "document.getElementById('avgTime').innerText=Math.round(rt.reduce((a,b)=>a+b,0)/rt.length);"
					+ "document.getElementById('fastTime').innerText=Math.min(...rt);"
					+ "document.getElementById('slowTime').innerText=Math.max(...rt);"

					+ "</script></body></html>";

			try (FileWriter fw = new FileWriter(file)) {
				fw.write(html);
			}

			System.out.println("✅ Dashboard Generated Successfully at: " + file.getAbsolutePath());

		} catch (Exception e) {
			System.out.println("Dashboard Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}