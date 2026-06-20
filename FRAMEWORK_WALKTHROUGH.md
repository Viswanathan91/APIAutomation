# Framework Walkthrough — REST Assured + Cucumber BDD

This document explains every file in the framework, how they connect, and how a test run flows from the Maven command all the way to an assertion result.

---

## Table of Contents

1. [Project Structure](#1-project-structure)
2. [File-by-File Explanation](#2-file-by-file-explanation)
3. [How Execution Starts — Step by Step](#3-how-execution-starts--step-by-step)
4. [End-to-End Flow — Places API](#4-end-to-end-flow--places-api)
5. [End-to-End Flow — Post Management API](#5-end-to-end-flow--post-management-api)
6. [The Five Patterns Demonstrated](#6-the-five-patterns-demonstrated)
7. [Environment Switching](#7-environment-switching)
8. [How to Add a New Endpoint](#8-how-to-add-a-new-endpoint)

---

## 1. Project Structure

```
APIAutomation/
│
├── pom.xml                                    ← Maven: dependencies, Surefire config, HTML report plugin
│
├── src/
│   ├── main/java/pojo/                        ← Java POJOs (request & response models)
│   │   ├── AddPlace.java                      ← Request body for POST /maps/api/place/add/json
│   │   ├── Location.java                      ← Nested lat/lng object inside AddPlace
│   │   ├── DeletePlaceRequest.java            ← Request body for DELETE /maps/api/place/delete/json
│   │   ├── CreateUserRequest.java             ← Request body for POST /posts (supports optional fields)
│   │   └── GetUserResponse.java               ← Response body for GET /posts/{id} (deserialisation)
│   │
│   └── test/
│       ├── java/
│       │   ├── cucumber/Options/
│       │   │   └── TestRunner.java            ← JUnit entry point; tells Cucumber where to look
│       │   │
│       │   ├── features/
│       │   │   ├── placeValidations.feature   ← Gherkin scenarios for the Places API
│       │   │   └── userManagement.feature     ← Gherkin scenarios for the Post Management API
│       │   │
│       │   ├── resources/
│       │   │   ├── APIResources.java          ← Enum: central registry of all endpoint paths
│       │   │   ├── TestDataBuild.java         ← Factory: builds typed request payload objects
│       │   │   └── Utils.java                 ← Base class: RequestSpec, property reader, JsonPath helper
│       │   │
│       │   └── stepDefinitions/
│       │       ├── StepDefinition.java        ← Given/When/Then methods for the Places API
│       │       ├── UserStepDefinition.java    ← Given/When/Then methods for the Post Management API
│       │       └── Hooks.java                 ← @Before hook: ensures place_id exists before @DeletePlace
│       │
│       └── resources/
│           ├── sit.properties                 ← SIT environment config (default)
│           ├── uat.properties                 ← UAT environment config
│           └── prod.properties                ← Production environment config
│
└── target/
    ├── logs/api-test.log                      ← Full request/response log (written at runtime)
    └── jsonReports/cucumber-report.json       ← Raw Cucumber results (input for HTML report)
```

---

## 2. File-by-File Explanation

### `pom.xml`

The Maven project descriptor. Three things it controls:

| Section | What it does |
|---|---|
| `<dependencies>` | Pulls in REST Assured, Cucumber, JUnit, Jackson, Groovy |
| `maven-surefire-plugin` | Discovers and runs `TestRunner.java`; passes `env` as a JVM system property; `testFailureIgnore=false` means the build fails if any test fails |
| `maven-cucumber-reporting` | Converts `target/jsonReports/*.json` into a human-readable HTML report during `mvn verify` |

---

### `TestRunner.java`

```java
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/java/features",
    glue     = {"stepDefinitions"},
    plugin   = "json:target/jsonReports/cucumber-report.json"
)
public class TestRunner {}
```

The only purpose of this class is to tell Cucumber three things:
- **`features`** — where to find `.feature` files (Gherkin scenarios)
- **`glue`** — which Java package contains the step definition methods
- **`plugin`** — write results to a JSON file so the HTML report can read it

It has no code of its own. JUnit's `@RunWith(Cucumber.class)` hands all execution control to the Cucumber runtime.

---

### `placeValidations.feature`

Gherkin scenarios for the Google Maps Places API. Uses `Scenario Outline` with an `Examples` table so the same steps run once per data row without duplicating any Gherkin.

```gherkin
@AddPlace
Scenario Outline: Verify if Place is being successfully added
  Given Add Place Payload with "<name>" "<language>" "<address>"
  When  user calls "AddPlaceAPI" with "post" http request
  Then  the API call got success with status code 200
  And   "status" in response body is "OK"
  And   verify place_id created maps to "<name>" using "getPlaceAPI"

  Examples:
    | name        | language | address          |
    | Viswa house | English  | No.5 Beach Road  |
    | Adi house   | French   | Tree Hill Lane   |
```

---

### `userManagement.feature`

Gherkin scenarios for the JSONPlaceholder Post Management API. Each scenario is tagged and demonstrates a distinct pattern (optional fields, POJO deserialisation, PUT, DELETE).

---

### `Utils.java`

The base class that all step definition classes extend. Provides three shared utilities:

**① `requestSpecification()` — Places API spec (singleton)**

Builds a `RequestSpecification` once and reuses it for every Places API call in the JVM run. Pre-configures: base URI, `Content-Type: application/json`, API key query param, and request/response logging.

**② `reqresRequestSpecification()` — Post API spec (singleton)**

Same idea but for `jsonplaceholder.typicode.com`. No API key needed for this service.

**③ `getGlobalVariables(key)` — property reader with caching**

Reads a value from the active environment's `.properties` file. The file is loaded once per environment and cached in a `Map` — it is not re-read on every call.

```java
// First call for "sit" → loads sit.properties, caches it
// Second call for "sit" → returns value from cache (no file I/O)
String baseUri = getGlobalVariables("baseURI");
```

**④ `getJsonPath(response, key)` — JsonPath extractor**

Converts the raw JSON response string into a navigable object and returns the value at the given key.

```java
getJsonPath(response, "status")       // → "OK"
getJsonPath(response, "place_id")     // → "abc123"
```

Logs are written to `target/logs/api-test.log` (created at runtime).

---

### `APIResources.java`

A Java `enum` that is the single source of truth for all API endpoint paths. Step definitions never hardcode URL paths — they always go through this enum.

```java
public enum APIResources {
    AddPlaceAPI   ("/maps/api/place/add/json"),
    getPlaceAPI   ("/maps/api/place/get/json"),
    deletePlaceAPI("/maps/api/place/delete/json"),
    CreatePostAPI ("/posts"),
    GetPostAPI    ("/posts/{id}"),   // {id} is resolved by REST Assured pathParam()
    UpdatePostAPI ("/posts/{id}"),
    DeletePostAPI ("/posts/{id}");
}
```

To add a new endpoint, you only add one line here — no other file needs to change for the path itself.

---

### `TestDataBuild.java`

A factory class that constructs request payload objects. Keeps all data-construction logic out of step definitions. Default values (phone number, coordinates, etc.) that are not scenario-driven are stored as named constants here.

```java
addPlacePayload(name, language, address)  → AddPlace POJO
deletePlacePayload(placeId)               → DeletePlaceRequest POJO
createPostPayload(title, body)            → CreateUserRequest POJO (body can be null)
```

---

### `AddPlace.java`

Request POJO for the Add Place API. Jackson serialises it to JSON when passed to `.body()`.

- Annotated with `@JsonInclude(NON_NULL)` — any field left as `null` is omitted from the JSON output
- `phoneNumber` field uses `@JsonProperty("phone_number")` so the Java field uses camelCase while the JSON key stays snake_case

---

### `Location.java`

Nested POJO inside `AddPlace`. Holds `lat` and `lng` coordinates. Jackson serialises it as a nested JSON object:

```json
"location": { "lat": -38.383494, "lng": 33.427362 }
```

---

### `DeletePlaceRequest.java`

Request POJO for the Delete Place API. Uses `@JsonProperty("place_id")` to map the Java field `placeId` to the JSON key `place_id`.

```json
{ "place_id": "abc123" }
```

Using a POJO (instead of a raw JSON string) means Jackson handles escaping and serialisation — the payload cannot be malformed.

---

### `CreateUserRequest.java`

Request POJO for creating and updating posts. The `body` field is optional — pass `null` and `@JsonInclude(NON_NULL)` ensures it is completely absent from the serialised JSON (not written as `"body": null`).

```java
new CreateUserRequest("Hello", "World")  →  {"title":"Hello","body":"World"}
new CreateUserRequest("Hello", null)     →  {"title":"Hello"}
```

---

### `GetUserResponse.java`

Response POJO for GET /posts/{id}. `response.as(GetUserResponse.class)` deserialises the full JSON body into this typed object.

The `authorId` field demonstrates `@JsonProperty`: the API returns `"userId"` but the Java code names it `authorId`. Jackson handles the mapping automatically.

```json
{ "userId": 1, "id": 1, "title": "...", "body": "..." }
         ↓ @JsonProperty("userId")
GetUserResponse.authorId = 1
```

---

### `StepDefinition.java`

Cucumber step definitions for the Places API. Extends `Utils` to inherit the shared request spec and JsonPath helper.

Key responsibilities:
- Builds the REST Assured request chain (`given().spec(...).body(...)`)
- Calls the API via `when().post()` / `when().get()` etc.
- Asserts status code and response fields
- Stores `place_id` in a `static` field so the `@DeletePlace` scenario can use it

---

### `UserStepDefinition.java`

Cucumber step definitions for the Post Management API. Extends `Utils`. Covers GET, POST, PUT, DELETE.

Unique steps it adds over `StepDefinition`:
- `optionally {string} is present in response` — checks presence via `JsonPath.from()`, never fails the test
- `response is deserialized and {string} is {string}` — deserialises once into `GetUserResponse`, then asserts typed fields

---

### `Hooks.java`

```java
@Before("@DeletePlace")
public void beforeScenario() throws IOException {
    if (StepDefinition.placeId == null) {
        m.add_place_payload_with("Shetty", "French", "Asia");
        m.user_calls_with_http_request("AddPlaceAPI", "POST");
        m.verify_place_id_created_maps_to_using_get_place_api("Shetty", "getPlaceAPI");
    }
}
```

Runs automatically before any scenario tagged `@DeletePlace`. Ensures a `place_id` exists to delete — if `@AddPlace` ran earlier in the same session, the ID is already set and the hook skips. If `@DeletePlace` runs in isolation, the hook creates a place first.

---

### `.properties` files

One file per environment. All three have the same keys; only the values differ in a real project.

```properties
baseURI=https://rahulshettyacademy.com
key=qaclick123
jsonPlaceholderBaseURI=https://jsonplaceholder.typicode.com
```

Selected at runtime by the `env` JVM property (`-Denv=uat`). Default is `sit`.

---

## 3. How Execution Starts — Step by Step

```
mvn test -Denv=sit -Dcucumber.filter.tags="@AddPlace"
     │
     ▼
[1] Maven reads pom.xml
     · resolves all dependencies from Maven Central
     · compiles src/main/java and src/test/java

     │
     ▼
[2] maven-surefire-plugin activates
     · injects env=sit into the JVM as a system property
     · scans target/test-classes for classes matching *Test* or *Runner* patterns
     · finds TestRunner.java → hands it to JUnit

     │
     ▼
[3] JUnit sees @RunWith(Cucumber.class) on TestRunner
     · delegates all execution to the Cucumber runtime

     │
     ▼
[4] Cucumber reads @CucumberOptions on TestRunner
     · scans src/test/java/features/ → finds placeValidations.feature, userManagement.feature
     · scans stepDefinitions package → loads StepDefinition, UserStepDefinition, Hooks
     · applies tag filter @AddPlace → selects only matching scenarios

     │
     ▼
[5] For each selected scenario, Cucumber:
     a. Creates new instances of all step definition classes
     b. Runs any @Before hooks that match the scenario's tags
     c. Executes each Gherkin step in order:
          · matches step text to a @Given/@When/@Then annotation
          · extracts {string} / {int} tokens and passes them as method arguments
          · calls the Java method
     d. Collects pass/fail result
     e. Runs any @After hooks

     │
     ▼
[6] Results written to target/jsonReports/cucumber-report.json

     │
     ▼
[7] mvn verify (separate command) → maven-cucumber-reporting reads the JSON
     · generates HTML report in target/
```

---

## 4. End-to-End Flow — Places API

### 4.1 `@AddPlace` scenario

```
Gherkin: "Add Place Payload with 'Viswa house' 'English' 'No.5 Beach Road'"
     │
     ▼ Cucumber matches step → calls method
StepDefinition.add_place_payload_with("Viswa house", "English", "No.5 Beach Road")
     │
     ▼ delegates to factory
TestDataBuild.addPlacePayload("Viswa house", "English", "No.5 Beach Road")
     │ populates AddPlace POJO with name, language, address + default constants
     ▼
AddPlace {
  accuracy: 50,  name: "Viswa house",  language: "English",
  address: "No.5 Beach Road",  phone_number: "(+91) 983 893 3937",
  website: "http://google.com",  types: ["shoe park","shop"],
  location: { lat: -38.383494, lng: 33.427362 }
}
     │
     ▼ REST Assured calls Jackson to serialise POJO → JSON
HTTP POST https://rahulshettyacademy.com/maps/api/place/add/json?key=qaclick123
Content-Type: application/json
Body: { "accuracy":50, "name":"Viswa house", ... }
     │
     ▼ API responds
Response 200: { "status":"OK", "scope":"APP", "place_id":"abc123" }
     │
     ▼ @Then + @And assertions
assertEquals(200,  response.getStatusCode())
assertEquals("OK",  getJsonPath(response, "status"))
assertEquals("APP", getJsonPath(response, "scope"))
     │
     ▼ "verify place_id created maps to 'Viswa house' using 'getPlaceAPI'"
placeId = getJsonPath(response, "place_id")     // → "abc123" stored in static field

HTTP GET https://rahulshettyacademy.com/maps/api/place/get/json?key=qaclick123&place_id=abc123
Response: { "name":"Viswa house", ... }
assertEquals("Viswa house", getJsonPath(response, "name"))    ✓
```

---

### 4.2 `@DeletePlace` scenario — with Hook

```
Cucumber sees @DeletePlace tag
     │
     ▼ Hooks.beforeScenario() runs BEFORE any Given/When/Then
     · StepDefinition.placeId == "abc123" (set by @AddPlace above)?
         YES → skip (place already exists)
         NO  → call add_place_payload_with(...) + user_calls_with_http_request(...)
               to create a place and populate placeId
     │
     ▼ Gherkin: "DeletePlace payload"
StepDefinition.delete_place_payload()
     │
     ▼
TestDataBuild.deletePlacePayload("abc123")
     │ returns DeletePlaceRequest("abc123")
     ▼ Jackson serialises POJO
{ "place_id": "abc123" }
     │
     ▼
HTTP POST https://rahulshettyacademy.com/maps/api/place/delete/json?key=qaclick123
Body: { "place_id": "abc123" }
     │
     ▼
Response 200: { "status":"OK" }
assertEquals(200, response.getStatusCode())
assertEquals("OK", getJsonPath(response, "status"))    ✓
```

> **Why `placeId` is `static`:** Cucumber creates a new instance of each step definition class for every scenario. A regular instance field would be `null` at the start of `@DeletePlace`. Making it `static` lets the value survive across scenarios within the same JVM run.

---

## 5. End-to-End Flow — Post Management API

### 5.1 `@CreatePost` — mandatory fields

```
Gherkin: "Create Post payload with title 'First Post' and body 'Content for first post'"
     │
     ▼
UserStepDefinition.create_post_payload_with_title_and_body("First Post", "Content for first post")
     │
     ▼
TestDataBuild.createPostPayload("First Post", "Content for first post")
     │ returns CreateUserRequest — both fields non-null, both serialised
     ▼
HTTP POST https://jsonplaceholder.typicode.com/posts
Body: { "title":"First Post", "body":"Content for first post" }
     │
     ▼
Response 201: { "title":"First Post", "body":"Content for first post", "id":101 }
assertEquals(201,                    response.getStatusCode())
assertEquals("First Post",           getJsonPath(response, "title"))
assertEquals("Content for first post", getJsonPath(response, "body"))
[Optional] "id" is present → logs "101"    (no assertion on value)
```

---

### 5.2 `@CreatePostOptional` — optional field excluded

```
Gherkin: "Create Post payload with only title 'Title Only'"
     │
     ▼
TestDataBuild.createPostPayload("Title Only", null)
     │
     ▼ Jackson serialises CreateUserRequest:
     · title = "Title Only"  → written
     · body  = null          → @JsonInclude(NON_NULL) skips this field entirely
     ▼
HTTP POST https://jsonplaceholder.typicode.com/posts
Body: { "title":"Title Only" }          ← "body" key is absent
     │
     ▼
Response 201: { "title":"Title Only", "id":101 }
assertEquals(201,          response.getStatusCode())
assertEquals("Title Only", getJsonPath(response, "title"))
[Optional] "body" is present?
     → JsonPath.from(body).get("body") returns null
     → prints "[Optional] 'body' is NOT present — skipping validation"
     → test passes
```

---

### 5.3 `@GetPost` — POJO deserialisation + `@JsonProperty`

```
Gherkin: "I request post with id 1"
     │
     ▼
res = given().spec(reqresRequestSpecification())
             .pathParam("id", 1)
     │
     ▼ "user calls 'GetPostAPI' with 'GET' http method"
APIResources.GetPostAPI.getResource() → "/posts/{id}"
REST Assured resolves {id} → "/posts/1"

HTTP GET https://jsonplaceholder.typicode.com/posts/1
     │
     ▼
Response 200: { "userId":1, "id":1, "title":"sunt aut facere…", "body":"quia et…" }
     │
     ▼ "response is deserialized and 'id' is '1'"
deserializedPost = response.as(GetUserResponse.class)
     │ Jackson maps JSON → GetUserResponse:
     │   "id"     → id     = 1
     │   "userId" → authorId = 1   (via @JsonProperty("userId"))
     │   "title"  → title  = "sunt aut facere…"
     │   "body"   → body   = "quia et…"
     ▼
assertEquals(1, deserializedPost.getId())       ✓

     │ "response is deserialized and 'authorId' is '1'"
     ▼ deserializedPost already populated — Jackson not called again
assertEquals(1, deserializedPost.getAuthorId())  ✓

     │ "optionally 'title' is present in response"
     ▼
JsonPath.from(body).get("title") → "sunt aut facere…"  (not null)
prints "[Optional] 'title' is present → sunt aut facere…"    (no assertion)
```

---

## 6. The Five Patterns Demonstrated

### Pattern 1 — POJO Serialisation (request)

**Where:** `AddPlace.java`, `TestDataBuild.addPlacePayload()`

REST Assured serialises a Java object to JSON automatically when `ContentType.JSON` is set. You write setters — no manual JSON strings.

```java
AddPlace ap = new AddPlace();
ap.setName("Viswa house");
given().spec(requestSpecification()).body(ap)
// REST Assured + Jackson → {"name":"Viswa house","accuracy":50, …}
```

---

### Pattern 2 — Optional Request Fields (`@JsonInclude`)

**Where:** `CreateUserRequest.java`

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateUserRequest {
    private String title;
    private String body;   // pass null to omit from the request
}
```

```java
createPostPayload("Hello", "World")  →  {"title":"Hello","body":"World"}
createPostPayload("Hello", null)     →  {"title":"Hello"}          // no "body" key
```

---

### Pattern 3 — JsonPath Response Validation

**Where:** `Utils.getJsonPath()`, `StepDefinition.in_response_body_is()`

Navigate the response JSON using dot-notation without deserialising into a POJO.

```java
getJsonPath(response, "status")          // → "OK"
getJsonPath(response, "place_id")        // → "abc123"
getJsonPath(response, "location.lat")    // → "-38.383494"
```

---

### Pattern 4 — Optional Response Validation

**Where:** `UserStepDefinition.optionally_is_present_in_response()`

Some fields only appear under certain conditions (timestamps, environment-only fields). The optional step checks for presence using `JsonPath`, logs the value if found, and never fails the test either way.

```java
try {
    Object value = JsonPath.from(response.asString()).get(key);
    if (value != null)
        System.out.println("[Optional] '" + key + "' is present → " + value);
    else
        System.out.println("[Optional] '" + key + "' is NOT present — skipping");
} catch (Exception e) {
    System.out.println("[Optional] '" + key + "' is NOT present — skipping");
}
```

---

### Pattern 5 — POJO Deserialisation + `@JsonProperty`

**Where:** `GetUserResponse.java`, `UserStepDefinition.response_is_deserialized_and_field_is()`

`response.as(GetUserResponse.class)` maps the full JSON body into a typed Java object. You get compile-time safety and IDE autocomplete instead of raw string keys.

```java
GetUserResponse post = response.as(GetUserResponse.class);
post.getId()        // int
post.getAuthorId()  // int — mapped from JSON key "userId" via @JsonProperty
post.getTitle()     // String
```

The deserialised object is cached so Jackson runs only once per scenario, regardless of how many `And` assertion steps follow.

---

## 7. Environment Switching

```
mvn test -Denv=uat
          │
          ▼
Surefire passes env=uat to the JVM
          │
          ▼
Utils.getGlobalVariables("baseURI")
  env = System.getProperty("env", "sit")   // → "uat"
  propsCache does not have "uat" yet
  → load uat.properties from classpath → cache it
  → return p.getProperty("baseURI")         // → value from uat.properties
          │
          ▼
RequestSpecBuilder.setBaseUri(…)    ← built once, reused for the entire run
```

In a real project, each file points to a different host:

```properties
# sit.properties
baseURI=https://sit.internal.company.com

# uat.properties
baseURI=https://uat.internal.company.com

# prod.properties
baseURI=https://api.company.com
```

---

## 8. How to Add a New Endpoint

Four steps — no other files need to change.

**Step 1 — Register the path in `APIResources.java`**
```java
GetOrderAPI("/api/orders/{orderId}"),
```

**Step 2 — Add a request/response POJO in `src/main/java/pojo/` (if needed)**
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateOrderRequest {
    private String  productId;
    private Integer quantity;   // optional
    …
}
```

**Step 3 — Add a payload builder in `TestDataBuild.java`**
```java
public CreateOrderRequest createOrderPayload(String productId, Integer quantity) {
    return new CreateOrderRequest(productId, quantity);
}
```

**Step 4 — Write a Gherkin scenario and step methods**
```gherkin
@GetOrder
Scenario: Fetch order by id
  Given I request order with id 42
  When  user calls "GetOrderAPI" with "GET" http method
  Then  the API call is successful with status code 200
  And   response field "productId" is "ABC"
```

Add matching `@Given` / `@When` / `@Then` methods in the appropriate step definition class.

If the endpoint belongs to a **new service** (different base URL), add its URI key to all three `.properties` files and add a new `requestSpecification` method in `Utils.java`.
