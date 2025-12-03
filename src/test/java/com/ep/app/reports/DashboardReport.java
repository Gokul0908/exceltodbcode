package com.ep.app.reports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

import com.ep.app.utils.TestListener;

public class DashboardReport implements IReporter {

	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {

		int passed = TestListener.passedCount;
		int failed = TestListener.failedCount;
		int skipped = TestListener.skippedCount;

		int postCount = TestListener.postCount;
		int getCount = TestListener.getCount;
		int putCount = TestListener.putCount;
		int deleteCount = TestListener.deleteCount;

		StringBuilder tableData = new StringBuilder();

		for (ISuite suite : suites) {
			for (ISuiteResult suiteResult : suite.getResults().values()) {
				ITestContext context = suiteResult.getTestContext();
				addRows(context.getPassedTests().getAllResults(), "Passed", "green", tableData);
				addRows(context.getFailedTests().getAllResults(), "Failed", "red", tableData);
				addRows(context.getSkippedTests().getAllResults(), "Skipped", "orange", tableData);
			}
		}

		generateHtml(passed, failed, skipped, deleteCount, tableData.toString(), postCount, getCount, putCount,
				deleteCount);
	}

	private void addRows(Set<ITestResult> results, String status, String color, StringBuilder sb) {
		for (ITestResult result : results) {
			sb.append("<tr style='font-weight:bold;color:").append(color).append(";'>").append("<td>")
					.append(result.getName()).append("</td>").append("<td>").append(status).append("</td>")
					.append("</tr>");
		}
	}

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

	private void generateHtml(int passed, int failed, int skipped, long duration, String tableRows, int post, int get,
			int put, int del) {
		try {
			File file = new File("Reports/API_Dashboard.html");
			file.getParentFile().mkdirs();

// copy logo file to Reports/logo/company_logo.jpg (same as before)
			copyLogo();

			String date = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date());
			String env = System.getProperty("env", "QA");

			String html = "<html><head>" + "<meta charset='UTF-8'>"
					+ "<title>API Automation Execution Dashboard</title>"
					+ "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>" + "<style>"
					// ====== PAGE BASE ======
					+ "body{margin:0;font-family:Arial,Helvetica,sans-serif;"
					+ "background:#e6f2ff;position:relative;color:#003366;}"
					+ ".page{position:relative;z-index:10;padding:20px 0 40px 0;text-align:center;}"
					// ====== WATERMARK LOGO (BIG, VERY LIGHT) ======
					+ ".watermark{position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);"
					+ "opacity:0.18;z-index:0;pointer-events:none;animation: rotateLogo 80s linear infinite; /* Slow smooth rotation */  filter: blur(1px) grayscale(30%);}"
					+ " .watermark img { width: 980px;  /* Make watermark big */ max-width: 98vw; opacity: inherit;};"

					/* Keyframe animation */
					+ "@keyframes rotateLogo {from { transform: translate(-50%, -50%) rotate(0deg);}to {transform: translate(-50%, -50%) rotate(360deg);}}; "

					/* 🔽 CHANGE THIS to make background logo bigger/smaller */
					+ ".watermark img{width:650px;}"
					// ====== HEADER ROW ======
					+ ".header-row{display:flex;align-items:center;justify-content:center;"
					+ "gap:25px;margin-bottom:10px;}" + ".logo{position:absolute;top:15px;left:25px;}"
					/* 🔽 CHANGE THIS to control TOP-LEFT logo size */
					+ ".logo img{height:90px;}" + ".title{font-size:28px;font-weight:bold;color:#002b5c;}"
					+ ".subtitle{font-size:13px;color:#00509e;margin-top:4px;}"
					// ====== STATS CARDS ======
					+ ".stats{display:flex;justify-content:center;gap:18px;margin:22px auto 8px auto;}"
					+ ".card{min-width:140px;padding:12px 22px;border-radius:24px;font-size:16px;"
					+ "font-weight:bold;color:#fff;box-shadow:0 4px 10px rgba(0,0,0,0.15);"
					+ "background:#999;transition:0.3s;}"
					+ ".card:hover{transform:scale(1.06);box-shadow:0 6px 16px rgba(0,0,0,0.25);}"
					+ ".p{background:#28a745;}.f{background:#dc3545;}.s{background:#ffc107;color:#333;}"
					// ====== API CALL COUNTS ======
					+ ".callCounts{text-align:center;margin:10px auto 10px auto;font-size:14px;font-weight:bold;}"
					+ ".callCounts span{margin:0 8px;padding:4px 12px;border-radius:12px;"
					+ "background:#d9e6ff;border:1px solid #b3ccff;}"
					// ====== CHART ======
					+ ".chart-container{width:320px;margin:18px auto 0 auto;}"
					// ====== TABLE ======
					+ "table{width:80%;margin:30px auto 0 auto;border-collapse:collapse;background:#fff;"
					+ "box-shadow:0 4px 16px rgba(0,0,0,0.12);}"
					+ "th,td{border:1px solid #cdd4e0;padding:9px 6px;font-size:14px;text-align:center;}"
					+ "th{background:#003366;color:#fff;}" + "tr:nth-child(even){background:#f4f7ff;}"
					+ ".footer{margin-top:25px;font-size:11px;color:#0066aa;}" + "</style></head><body>"

					// ====== BIG WATERMARK LOGO IN BACKGROUND ======
					+ "<div class='watermark'><img src='logo/company_logo.jpg' alt='logo'></div>"

					+ "<div class='page'>"

					// ====== HEADER WITH LEFT LOGO + CENTER TITLE ======
					+ "<div class='logo'><img src='logo/company_logo.jpg' alt='Logo'></div>"
					+ "<div class='header-row'>" + "  <div class='title'>API Automation Execution Dashboard</div>"
					+ "</div>" + "<div class='subtitle'>Environment: " + env + " | Execution Mode: Single"
					+ " | Executed At: " + date + " | Duration: " + duration + " sec</div>"

					// ====== PASS / FAIL / SKIP CARDS ======
					+ "<div class='stats'>" + "<div class='card p'>Passed: " + passed + "</div>"
					+ "<div class='card f'>Failed: " + failed + "</div>" + "<div class='card s'>Skipped: " + skipped
					+ "</div>" + "</div>"

					// ====== API METHOD COUNTS ======
					+ "<div class='callCounts'>" + "<span>POST: " + post + "</span>" + "<span>GET: " + get + "</span>"
					+ "<span>PUT: " + put + "</span>" + "<span>DELETE: " + del + "</span>" + "</div>"

					// ====== DONUT CHART ======
					+ "<div class='chart-container'><canvas id='pieChart'></canvas></div>"

					// ====== TABLE ======
					+ "<table><tr><th>Test Case</th><th>Status</th></tr>" + tableRows + "</table>"

					+ "<div class='footer'>Powered by Changepond Technologies &nbsp;|&nbsp; API QA Automation</div>"

					+ "</div>" // end .page

					+ "<script>" + "new Chart(document.getElementById('pieChart'),{" + "  type:'doughnut'," + "  data:{"
					+ "    labels:['Passed','Failed','Skipped']," + "    datasets:[{" + "      data:[" + passed + ","
					+ failed + "," + skipped + "]," + "      backgroundColor:['#28a745','#dc3545','#ffc107'],"
					+ "      borderWidth:1" + "    }]" + "  }," + "  options:{" + "    cutout:'60%',"
					+ "    plugins:{legend:{position:'bottom',labels:{color:'#003366',font:{size:12}}}},"
					+ "    responsive:true" + "  }" + "});" + "</script>"

					+ "</body></html>";

			try (FileWriter fw = new FileWriter(file)) {
				fw.write(html);
			}

			System.out.println("✅ Dashboard Generated Successfully!");

		} catch (

		Exception e) {
			System.out.println("Dashboard Error: " + e.getMessage());
		}
	}

}
