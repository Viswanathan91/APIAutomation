# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **REST API test automation framework** using BDD (Behavior-Driven Development) with Cucumber and REST Assured, built on Maven. It tests a Google Maps-like Places API hosted at `rahulshettyacademy.com` — an interview prep learning project.

## Build & Test Commands

Run all Maven commands from the `APIAutomation/` subdirectory (that's where `pom.xml` lives).

```bash
# Run all tests (defaults to sit environment)
mvn test

# Run against a specific environment
mvn test -Denv=uat
mvn test -Denv=prod

# Filter by Cucumber tag (combinable with -Denv)
mvn test -Dcucumber.filter.tags="@AddPlace"
mvn test -Dcucumber.filter.tags="@DeletePlace"
mvn test -Dcucumber.filter.tags="@AddPlace" -Denv=uat

# Generate HTML report after tests
mvn verify
```

**Default environment:** `sit` (set in `pom.xml` `<properties>`)  
**Test reports:** HTML in `target/` from `target/jsonReports/cucumber-report.json`  
**Request/response logs:** Written to `logging.txt` in project root

## Architecture

### Framework Stack
- **REST Assured 6.0.0** — HTTP client DSL for API requests/assertions
- **Cucumber 7.x** — BDD layer; feature files in Gherkin, step definitions in Java
- **JUnit** — Test runner integration
- **Jackson Databind** — POJO ↔ JSON serialization for request payloads

### Layer Responsibilities

| Layer | Location | Responsibility |
|---|---|---|
| Feature files | `src/test/java/features/` | Gherkin scenarios; tagged `@AddPlace`, `@DeletePlace` |
| Step Definitions | `src/test/java/stepDefinitions/StepDefinition.java` | Maps Gherkin steps to REST Assured calls |
| Hooks | `src/test/java/stepDefinitions/Hooks.java` | `@Before` for `@DeletePlace`: creates a place if `place_id` is null |
| API Resources | `src/test/java/resources/APIResources.java` | Enum centralizing all endpoint paths |
| Test Data | `src/test/java/resources/TestDataBuild.java` | Builds POJO/String request payloads |
| Utilities | `src/test/java/resources/Utils.java` | Singleton `RequestSpecification`, JSON path extraction, property file reading |
| POJOs | `src/main/java/pojo/` | `AddPlace` and `Location` — Jackson-serialized request bodies |

### Configuration
Environment-specific properties files live in `src/test/resources/` and are loaded from the classpath. The active file is selected by the `env` JVM property (default: `sit`).

| File | Environment |
|---|---|
| `sit.properties` | System Integration Testing |
| `uat.properties` | User Acceptance Testing |
| `prod.properties` | Production |

Each file exposes three keys:
- `baseURI` — host for the Places API (rahulshettyacademy.com)
- `key` — API key sent as a query param on every Places request
- `jsonPlaceholderBaseURI` — host for the Post Management API (jsonplaceholder.typicode.com)

To add a new API service, add its base URI key to all three properties files and add a `requestSpecification` method in `Utils.java` that reads the new key.

### Patterns demonstrated across the two feature files

| Pattern | Where to find it |
|---|---|
| POJO serialisation (request) | `AddPlace` + `TestDataBuild.addPlacePayload()` |
| Optional request fields via `@JsonInclude(NON_NULL)` | `CreateUserRequest` + `TestDataBuild.createPostPayload(title, null)` |
| JsonPath response validation | `StepDefinition.in_response_body_is()` |
| Optional response field check (never fails) | `UserStepDefinition.optionally_is_present_in_response()` |
| POJO deserialisation (response) | `UserStepDefinition.response_is_deserialized_and_field_is()` → `response.as(GetUserResponse.class)` |
| `@JsonProperty` key mapping | `GetUserResponse.authorId` ← JSON key `"userId"` |
| Path parameters | `given().pathParam("id", postId)` in `UserStepDefinition` |
| Multiple HTTP methods | GET/POST/PUT/DELETE in `UserStepDefinition.user_calls_with_http_method()` |
| Cross-scenario state (hooks) | `Hooks.java` creates a place before `@DeletePlace` runs |

### Test Data Flow

The `@DeletePlace` scenario depends on a place being created first. `Hooks.java` handles this: before any `@DeletePlace` scenario runs, it checks if `place_id` is null and runs the AddPlace API call to create one. The `place_id` is stored as a static field shared across step definition instances.

### Test Runner
`src/test/java/cucumber/Options/TestRunner.java` — controls which tags run. Change the `tags` field here to switch between `@AddPlace` and `@DeletePlace` scenarios. The Surefire plugin has `testFailureIgnore=true` so the build continues even on assertion failures.
