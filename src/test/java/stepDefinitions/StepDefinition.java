package stepDefinitions;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import resources.APIResources;
import resources.TestDataBuild;
import resources.Utils;

public class StepDefinition extends Utils {

    RequestSpecification res;
    Response response;

    // Static so the value survives across the AddPlace → GetPlace → DeletePlace scenario chain.
    // For parallel execution, replace with PicoContainer-injected ScenarioContext.
    static String placeId;

    TestDataBuild tb = new TestDataBuild();

    @Given("Add Place Payload with {string} {string} {string}")
    public void add_place_payload_with(String name, String language, String address) throws IOException {
        res = given().spec(requestSpecification())
                     .body(tb.addPlacePayload(name, language, address));
    }

    @When("user calls {string} with {string} http request")
    public void user_calls_with_http_request(String resource, String method) {
        APIResources resourceAPI = APIResources.valueOf(resource);
        switch (method.toUpperCase()) {
            case "POST":   response = res.when().post(resourceAPI.getResource());    break;
            case "GET":    response = res.when().get(resourceAPI.getResource());     break;
            case "PUT":    response = res.when().put(resourceAPI.getResource());     break;
            case "DELETE": response = res.when().delete(resourceAPI.getResource()); break;
            default: throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }

    @Then("the API call got success with status code {int}")
    public void the_api_call_got_success_with_status_code(int expectedCode) {
        assertEquals("Unexpected HTTP status code", expectedCode, response.getStatusCode());
    }

    @And("{string} in response body is {string}")
    public void in_response_body_is(String key, String value) {
        assertEquals("Response field '" + key + "' mismatch", value, getJsonPath(response, key));
    }

    @And("verify place_id created maps to {string} using {string}")
    public void verify_place_id_created_maps_to_using_get_place_api(String expectedName, String resource) throws IOException {
        placeId = getJsonPath(response, "place_id");

        res = given().spec(requestSpecification()).queryParam("place_id", placeId);
        user_calls_with_http_request(resource, "GET");

        String actualName = getJsonPath(response, "name");
        assertEquals("Place name returned by GetPlace API does not match what was added", expectedName, actualName);
    }

    @Given("DeletePlace payload")
    public void delete_place_payload() throws IOException {
        res = given().spec(requestSpecification()).body(tb.deletePlacePayload(placeId));
    }
}
