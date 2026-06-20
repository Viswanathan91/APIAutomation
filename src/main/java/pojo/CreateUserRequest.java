package pojo;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request POJO for POST /posts (jsonplaceholder.typicode.com).
 *
 * @JsonInclude(NON_NULL) instructs Jackson to skip any field whose value is null
 * when serialising, so callers can make fields optional simply by passing null —
 * they will not appear in the request body at all.
 *
 * Example with both fields:  {"title":"foo","body":"bar"}
 * Example with body omitted: {"title":"foo"}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateUserRequest {

    private String title;
    private String body;   // optional — pass null to omit from request

    public CreateUserRequest(String title, String body) {
        this.title = title;
        this.body  = body;
    }

    public String getTitle() { return title; }
    public void   setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void   setBody(String body) { this.body = body; }
}
