package swagger;

public class SwaggerRunner { 

	private static boolean swaggerMode = false;

	// ✅ HARD-CODED SWAGGER URL  http://3.148.89.49:8000/openapi.json
	private static final String SWAGGER_URL = "";

	public static String prepareSwaggerExcelIfNeeded() {

		// ✅ Directly use the URL
		System.out.println("Swagger URL = " + SWAGGER_URL);

		if (SWAGGER_URL == null || SWAGGER_URL.trim().isEmpty()) {
			swaggerMode = false;
			return null;
		}

		swaggerMode = true;
		return SwaggerExcelWriter.generate(SWAGGER_URL);
	}

	public static boolean isSwaggerMode() {
		return swaggerMode;
	}
}
