package pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Nested "data" object inside the GET /api/users/{id} response.
 * @JsonProperty maps the snake_case JSON keys to conventional camelCase Java fields.
 */
public class UserData {

    private int    id;
    private String email;
    private String avatar;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    public int    getId()        { return id; }
    public void   setId(int id)  { this.id = id; }

    public String getEmail()           { return email; }
    public void   setEmail(String e)   { this.email = e; }

    public String getFirstName()           { return firstName; }
    public void   setFirstName(String fn)  { this.firstName = fn; }

    public String getLastName()           { return lastName; }
    public void   setLastName(String ln)  { this.lastName = ln; }

    public String getAvatar()           { return avatar; }
    public void   setAvatar(String a)   { this.avatar = a; }
}
