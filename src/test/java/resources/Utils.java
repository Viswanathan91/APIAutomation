package resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
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

    // Properties cache — avoids reloading the file on every getGlobalVariables() call
    private static final Map<String, Properties> propsCache = new HashMap<>();

    private static final String LOG_PATH = "target" + File.separator + "logs" + File.separator + "api-test.log";

    public RequestSpecification requestSpecification() throws IOException {
        if (placesReqSpec == null) {
            PrintStream log = buildLogStream(false);
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

    public RequestSpecification reqresRequestSpecification() throws IOException {
        if (reqresReqSpec == null) {
            PrintStream log = buildLogStream(true); // append so both specs share the file
            reqresReqSpec = new RequestSpecBuilder()
                    .setContentType(ContentType.JSON)
                    .setBaseUri(getGlobalVariables("jsonPlaceholderBaseURI"))
                    .addFilter(RequestLoggingFilter.logRequestTo(log))
                    .addFilter(ResponseLoggingFilter.logResponseTo(log))
                    .build();
        }
        return reqresReqSpec;
    }

    public static String getGlobalVariables(String key) throws IOException {
        String env = System.getProperty("env", "sit");
        if (!propsCache.containsKey(env)) {
            Properties p = new Properties();
            InputStream is = Utils.class.getClassLoader().getResourceAsStream(env + ".properties");
            if (is == null) {
                throw new RuntimeException(
                    "Properties file not found for environment: " + env
                    + ". Expected on classpath: " + env + ".properties");
            }
            p.load(is);
            propsCache.put(env, p);
        }
        return propsCache.get(env).getProperty(key);
    }

    public String getJsonPath(Response response, String key) {
        JsonPath js = new JsonPath(response.asString());
        return js.get(key).toString();
    }

    private static PrintStream buildLogStream(boolean append) throws IOException {
        File logDir = new File("target" + File.separator + "logs");
        logDir.mkdirs();
        return new PrintStream(new FileOutputStream(LOG_PATH, append));
    }
}
