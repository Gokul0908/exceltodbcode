package swagger;

import java.io.InputStreamReader;
import java.net.URL;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SwaggerReader {

	public static JsonObject getPaths(String swaggerUrl) {
		try {
			@SuppressWarnings("deprecation")
			URL url = new URL(swaggerUrl);
			JsonObject root = JsonParser.parseReader(new InputStreamReader(url.openStream())).getAsJsonObject();

			return root.getAsJsonObject("paths");

		} catch (Exception e) {
			throw new RuntimeException("Failed to read Swagger JSON", e);
		}
	}
}
