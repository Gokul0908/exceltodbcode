package com.ep.app.tests;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;

public class TestNGXmlGenerator {

	public static void generateTestNGXml(List<String> getCases, List<String> postCases, List<String> putCases,
			List<String> deleteCases) {

		try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/test/resources/testng-api.xml"))) {

			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<!DOCTYPE suite SYSTEM \"https://testng.org/testng-1.0.dtd\">\n");
			writer.write("<suite name=\"DynamicAPIFromDB\" parallel=\"false\">\n");

			writeTests(writer, getCases);
			writeTests(writer, postCases);
			writeTests(writer, putCases);
			writeTests(writer, deleteCases);

			writer.write("</suite>");

			System.out.println("✅ testng-api.xml generated from DB");

		} catch (Exception e) {
			throw new RuntimeException("Failed to generate testng-api.xml", e);
		}
	}

	private static void writeTests(BufferedWriter writer, List<String> caseIds) throws Exception {
		for (String caseID : caseIds) {
			writer.write("  <test name=\"API_" + caseID + "\">\n");
			writer.write("    <parameter name=\"caseID\" value=\"" + caseID + "\"/>\n");
			writer.write("    <classes>\n");
			writer.write("      <class name=\"com.ep.app.tests.APIMethods\"/>\n");
			writer.write("    </classes>\n");
			writer.write("  </test>\n");
		}
	}
}
