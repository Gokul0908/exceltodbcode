package swagger;

public class SwaggerConfig {

	// If empty → Excel mode
	// If present → Swagger mode
	//https://petstore.swagger.io/v2/swagger.json
	//https://petstore.swagger.io/
	//http://3.148.89.49:8000/docs#/
	public static final String SWAGGER_URL = "http://3.148.89.49:8000/openapi.json"; 

	public static boolean isSwaggerEnabled() {
		return SWAGGER_URL != null && !SWAGGER_URL.isEmpty();
	}
}
