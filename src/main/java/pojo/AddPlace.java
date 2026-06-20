package pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddPlace {

    private int          accuracy;
    private String       name;

    @JsonProperty("phone_number")
    private String       phoneNumber;

    private String       address;
    private List<String> types;
    private String       website;
    private String       language;
    private Location     location;

    public int           getAccuracy()                   { return accuracy; }
    public void          setAccuracy(int accuracy)       { this.accuracy = accuracy; }

    public String        getName()                       { return name; }
    public void          setName(String name)            { this.name = name; }

    public String        getPhoneNumber()                { return phoneNumber; }
    public void          setPhoneNumber(String phone)    { this.phoneNumber = phone; }

    public String        getAddress()                    { return address; }
    public void          setAddress(String address)      { this.address = address; }

    public List<String>  getTypes()                      { return types; }
    public void          setTypes(List<String> types)    { this.types = types; }

    public String        getWebsite()                    { return website; }
    public void          setWebsite(String website)      { this.website = website; }

    public String        getLanguage()                   { return language; }
    public void          setLanguage(String language)    { this.language = language; }

    public Location      getLocation()                   { return location; }
    public void          setLocation(Location location)  { this.location = location; }
}
