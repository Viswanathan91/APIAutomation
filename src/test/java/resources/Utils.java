package resources;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class Utils {

    private static RequestSpecification placesReqSpec;
    private static RequestSpecification reqresReqSpec;

    /**
     * Singleton RequestSpecification for the Places API.
     * Base URI and API key are read from the active environment properties file.
     */
    public RequestSpecification requestSpecification() throws IOException {
        if (placesReqSpec == null) {
            PrintStream log = new PrintStream(new FileOutputStream("logging.txt"));
            placesReqSpec = new RequestSpecBuilder()
                    .setContentType(ContentType.JSON)
                    .setBaseUri(getGlobalVariables("baseURI"))
                    .addQueryParam("key", getGlobalVariables("key"))
                    .addFilter(RequestLoggingFilter.logRequestTo(log))
                    .addFilter(ResponseLoggingFilter.logResponseTo(log))
                    .build();
        }
        return placesReqSpec;
    }

    /**
     * Singleton RequestSpecification for the jsonplaceholder.typicode.com Post API.
     * Uses a separate base URI key so both APIs can coexist in the same run.
     * Logs are appended to the same logging.txt file.
     */
    public RequestSpecification reqresRequestSpecification() throws IOException {
        if (reqresReqSpec == null) {
            PrintStream log = new PrintStream(new FileOutputStream("logging.txt", true)); // append
            reqresReqSpec = new RequestSpecBuilder()
                    .setContentType(ContentType.JSON)
                    .setBaseUri(getGlobalVariables("jsonPlaceholderBaseURI"))
                    .addFilter(RequestLoggingFilter.logRequestTo(log))
                    .addFilter(ResponseLoggingFilter.logResponseTo(log))
                    .build();
        }
        return reqresReqSpec;
    }

    /**
     * Reads a property from the active environment file.
     * The environment is selected by the JVM system property "env" (default: sit).
     * Run with: mvn test -Denv=uat
     */
    public static String getGlobalVariables(String key) throws IOException {
        String env = System.getProperty("env", "sit");
        Properties p = new Properties();
        InputStream is = Utils.class.getClassLoader().getResourceAsStream(env + ".properties");
        if (is == null) {
            throw new RuntimeException("Properties file not found for environment: " + env
                    + ". Expected: src/test/resources/" + env + ".properties");
        }
        p.load(is);
        return p.getProperty(key);
    }

    public String getJsonPath(Response response, String key) {
        String res = response.asString();
        JsonPath js = new JsonPath(res);
        return js.get(key).toString();
    }
}
