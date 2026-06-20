package pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response POJO for GET /posts/{id} (jsonplaceholder.typicode.com).
 *
 * Demonstrates two deserialization concepts:
 *
 *  1. @JsonProperty — maps a JSON key whose name differs from the Java field name.
 *     The API returns "userId" but we store it as "authorId" in Java.
 *     Without this annotation Jackson would fail to populate the field.
 *
 *  2. Plain field mapping — "id", "title", "body" match the JSON keys exactly
 *     so no annotation is required for those fields.
 */
public class GetUserResponse {

    private int id;

    @JsonProperty("userId")
    private int authorId;   // "userId" in JSON → authorId in Java

    private String title;
    private String body;

    public int    getId()              { return id; }
    public void   setId(int id)        { this.id = id; }

    public int    getAuthorId()        { return authorId; }
    public void   setAuthorId(int a)   { this.authorId = a; }

    public String getTitle()           { return title; }
    public void   setTitle(String t)   { this.title = t; }

    public String getBody()            { return body; }
    public void   setBody(String b)    { this.body = b; }
}
