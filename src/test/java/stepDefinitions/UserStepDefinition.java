package stepDefinitions;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import pojo.GetUserResponse;
import resources.APIResources;
import resources.TestDataBuild;
import resources.Utils;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Step definitions for the Post Management API (jsonplaceholder.typicode.com).
 *
 * Three patterns are shown here that do not exist in the Places API example:
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ 1. Optional request fields                                              │
 * │    CreateUserRequest uses @JsonInclude(NON_NULL). Pass null for "body"  │
 * │    and it is completely excluded from the serialised JSON body.         │
 * │    Callers never need if-statements; the annotation handles it.         │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │ 2. Optional response validation                                         │
 * │    "optionally {field} is present in response" logs the field value if  │
 * │    it is present, or logs a skip message if it is absent. The test      │
 * │    never fails either way — use this for fields the API may or may not  │
 * │    return depending on the request or server state.                     │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │ 3. POJO deserialisation + @JsonProperty                                 │
 * │    response.as(GetUserResponse.class) maps the full JSON body into a    │
 * │    typed Java object. The field "userId" in JSON is bound to "authorId" │
 * │    in Java via @JsonProperty — demonstrating key-name mapping.          │
 * │    The deserialized object is cached so .as() is called only once per   │
 * │    scenario regardless of how many "And" steps validate it.             │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * Note on step text: the @When step uses "http method" (vs "http request" in
 * StepDefinition) to avoid Cucumber's ambiguous-step error when both classes
 * are loaded at the same time.
 */
public class UserStepDefinition extends Utils {

    RequestSpecification res;
    Response response;

    // Lazy-initialised once the GET response is received
    private GetUserResponse deserializedPost;

    TestDataBuild tb = new TestDataBuild();

    // ── Given steps ──────────────────────────────────────────────────────────

    /** Both title and body are present → {"title":"...","body":"..."} */
    @Given("Create Post payload with title {string} and body {string}")
    public void create_post_payload_with_title_and_body(String title, String body) throws IOException {
        res = given().spec(reqresRequestSpecification())
                .body(tb.createPostPayload(title, body));
    }

    /**
     * Only title is passed; body is null → @JsonInclude(NON_NULL) omits it.
     * Serialised body: {"title":"..."}   — no "body" key at all.
     */
    @Given("Create Post payload with only title {string}")
    public void create_post_payload_with_only_title(String title) throws IOException {
        res = given().spec(reqresRequestSpecification())
                .body(tb.createPostPayload(title, null));
    }

    /** Sets up the path parameter; the actual URL is resolved when @When fires. */
    @Given("I request post with id {int}")
    public void i_request_post_with_id(Integer postId) throws IOException {
        res = given().spec(reqresRequestSpecification())
                .pathParam("id", postId);
    }

    /** PUT with only title — body is omitted (optional field pattern). */
    @Given("Update Post payload with title {string} for post id {int}")
    public void update_post_payload_with_title_for_post_id(String title, Integer postId) throws IOException {
        res = given().spec(reqresRequestSpecification())
                .pathParam("id", postId)
                .body(tb.createPostPayload(title, null));
    }

    // ── When step ─────────────────────────────────────────────────────────────

    @When("user calls {string} with {string} http method")
    public void user_calls_with_http_method(String resource, String method) {
        APIResources resourceAPI = APIResources.valueOf(resource);
        switch (method.toUpperCase()) {
            case "POST":   response = res.when().post(resourceAPI.getResource());    break;
            case "GET":    response = res.when().get(resourceAPI.getResource());     break;
            case "PUT":    response = res.when().put(resourceAPI.getResource());     break;
            case "DELETE": response = res.when().delete(resourceAPI.getResource()); break;
            default: throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }

    // ── Then / And steps ──────────────────────────────────────────────────────

    @Then("the API call is successful with status code {int}")
    public void the_api_call_is_successful_with_status_code(int expectedCode) {
        assertEquals(expectedCode, response.getStatusCode());
    }

    /** Mandatory field check — test fails immediately if the value does not match. */
    @And("response field {string} is {string}")
    public void response_field_is(String key, String expectedValue) {
        assertEquals(expectedValue, getJsonPath(response, key));
    }

    /**
     * Optional field check — never fails the test.
     *
     * If the field is present in the response body, its value is logged so you
     * can see it in the output without writing an assertion for it.
     * If absent, a skip message is logged instead.
     *
     * Use this for:
     *  - Timestamps (e.g. "createdAt") that only appear on certain responses
     *  - Fields that are environment-dependent (e.g. only in PROD responses)
     *  - Fields that are under active development and not yet stable
     */
    @And("optionally {string} is present in response")
    public void optionally_is_present_in_response(String key) {
        try {
            Object value = io.restassured.path.json.JsonPath.from(response.asString()).get(key);
            if (value != null) {
                System.out.println("[Optional] '" + key + "' is present → " + value);
            } else {
                System.out.println("[Optional] '" + key + "' is NOT present — skipping validation");
            }
        } catch (Exception e) {
            System.out.println("[Optional] '" + key + "' is NOT present — skipping validation");
        }
    }

    /**
     * Deserialises the full response JSON into GetUserResponse (a typed POJO)
     * and asserts a specific field by name.
     *
     * Deserialisation happens once and the result is reused across all
     * "response is deserialized and" steps in the same scenario.
     *
     * Supported field names:
     *   "id"       → maps to GetUserResponse.getId()       (int)
     *   "authorId" → maps to GetUserResponse.getAuthorId() — note: JSON key is "userId"
     *   "title"    → maps to GetUserResponse.getTitle()
     *   "body"     → maps to GetUserResponse.getBody()
     */
    @And("response is deserialized and {string} is {string}")
    public void response_is_deserialized_and_field_is(String field, String expectedValue) {
        if (deserializedPost == null) {
            deserializedPost = response.as(GetUserResponse.class);
        }
        switch (field) {
            case "id":       assertEquals(Integer.parseInt(expectedValue), deserializedPost.getId());       break;
            case "authorId": assertEquals(Integer.parseInt(expectedValue), deserializedPost.getAuthorId()); break;
            case "title":    assertEquals(expectedValue, deserializedPost.getTitle());                      break;
            case "body":     assertEquals(expectedValue, deserializedPost.getBody());                       break;
            default:         fail("Unknown field for POJO deserialization check: " + field);
        }
    }
}
