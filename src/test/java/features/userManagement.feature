Feature: Post Management API (jsonplaceholder.typicode.com)

  # ── Pattern 1: POST with all fields present ──────────────────────────────
  #
  # Both "title" and "body" are serialised into the request body.
  # Response is validated field-by-field using JsonPath (no POJO needed here).
  # The Scenario Outline shows how one set of steps covers multiple data rows.
  # "optionally" step demonstrates checking a field that may or may not appear.

  @CreatePost
  Scenario Outline: Create post with title and body — both fields in request
    Given Create Post payload with title "<title>" and body "<body>"
    When user calls "CreatePostAPI" with "POST" http method
    Then the API call is successful with status code 201
    And response field "title" is "<title>"
    And response field "body" is "<body>"
    And optionally "id" is present in response

    Examples:
      | title        | body                   |
      | First Post   | Content for first post |
      | Second Post  | Content for second     |

  # ── Pattern 2: Optional request field excluded ────────────────────────────
  #
  # "body" is passed as null in createPostPayload().
  # @JsonInclude(NON_NULL) on CreateUserRequest means Jackson never writes the
  # "body" key into the JSON at all — the field is absent, not null.
  # Sent body: {"title":"Title Only"}
  #
  # The optional check on "body" in the response shows graceful handling:
  # jsonplaceholder echoes back only what you sent, so "body" won't appear.
  # The step logs "NOT present — skipping" without failing.

  @CreatePostOptional
  Scenario: Create post with only mandatory title — optional body excluded from request
    Given Create Post payload with only title "Title Only"
    When user calls "CreatePostAPI" with "POST" http method
    Then the API call is successful with status code 201
    And response field "title" is "Title Only"
    And optionally "body" is present in response

  # ── Pattern 3: POJO deserialisation + @JsonProperty ──────────────────────
  #
  # response.as(GetUserResponse.class) maps the full JSON body into a typed Java
  # object using Jackson. Two key points:
  #
  #   a) "userId" in JSON → authorId in Java (via @JsonProperty("userId"))
  #      This is the most common real-world scenario: the API uses one naming
  #      convention and your Java code uses another.
  #
  #   b) The object is deserialised once and reused for all "And" steps,
  #      so Jackson is only invoked a single time per scenario.
  #
  # After this step runs, you have a strongly-typed PostResponse object instead
  # of raw JSON — you can use normal Java null-safety, IDE auto-complete, etc.

  @GetPost
  Scenario: Fetch post by id and validate fields via POJO deserialisation
    Given I request post with id 1
    When user calls "GetPostAPI" with "GET" http method
    Then the API call is successful with status code 200
    And response is deserialized and "id" is "1"
    And response is deserialized and "authorId" is "1"
    And optionally "title" is present in response

  # ── Pattern 4: PUT with optional field omitted from request ───────────────
  #
  # Only "title" is sent (body is null → excluded by @JsonInclude(NON_NULL)).
  # Sent body: {"title":"Updated Title"}
  # jsonplaceholder echoes back only what you sent, so only "title" and "id"
  # appear in the response. The optional "body" check will show "NOT present".

  @UpdatePost
  Scenario: Update post title only — optional body absent from request and response
    Given Update Post payload with title "Updated Title" for post id 1
    When user calls "UpdatePostAPI" with "PUT" http method
    Then the API call is successful with status code 200
    And response field "title" is "Updated Title"
    And optionally "body" is present in response

  # ── Pattern 5: DELETE ─────────────────────────────────────────────────────
  #
  # Demonstrates DELETE and a non-201 success code.
  # jsonplaceholder returns 200 for DELETE (real production REST APIs often
  # return 204 No Content — the status code step handles any value you pass).

  @DeletePost
  Scenario: Delete post and verify successful response
    Given I request post with id 1
    When user calls "DeletePostAPI" with "DELETE" http method
    Then the API call is successful with status code 200
