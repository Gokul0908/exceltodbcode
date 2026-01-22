package swagger;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SwaggerExcelWriter {

	public static String generate(String swaggerUrl) {

		String filePath = System.getProperty("user.dir") + "/src/test/resources/SwaggerAPIs.xlsx";

		File file = new File(filePath);
		file.getParentFile().mkdirs();

		try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(file)) {

			Sheet sheet = workbook.createSheet("SwaggerAPIs");

			// ===== Header =====
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("caseID");
			header.createCell(1).setCellValue("action"); // ✅ action
			header.createCell(2).setCellValue("endpoint");
			header.createCell(3).setCellValue("isRun");

			JsonObject paths = SwaggerReader.getPaths(swaggerUrl);

			int rowNum = 1;

			for (Map.Entry<String, JsonElement> pathEntry : paths.entrySet()) {

				String endpoint = pathEntry.getKey();
				JsonObject methods = pathEntry.getValue().getAsJsonObject();

				for (Map.Entry<String, JsonElement> methodEntry : methods.entrySet()) {

					String action = methodEntry.getKey().toUpperCase();

					// ✅ allow only valid HTTP methods
					if (!action.matches("GET|POST|PUT|DELETE|PATCH")) {
						continue;
					}

					Row row = sheet.createRow(rowNum);
					row.createCell(0).setCellValue("SWG_" + rowNum);
					row.createCell(1).setCellValue(action); // ✅ action column
					row.createCell(2).setCellValue(endpoint);
					row.createCell(3).setCellValue("Yes");

					rowNum++;
				}
			}

			workbook.write(fos);

			System.out.println("Swagger Excel generated at: " + file.getAbsolutePath());
			return file.getAbsolutePath();

		} catch (Exception e) {
			throw new RuntimeException("Swagger Excel generation failed", e);
		}
	}
}
