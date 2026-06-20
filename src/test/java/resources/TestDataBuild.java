package resources;

import pojo.AddPlace;
import pojo.CreateUserRequest;
import pojo.Location;

import java.util.ArrayList;

public class TestDataBuild {

    // ── Places API payloads ───────────────────────────────────────────────────

    public AddPlace addPlacePayload(String name, String language, String address) {
        AddPlace ap = new AddPlace();
        ap.setAccuracy(50);
        ap.setAddress(address);
        ap.setLanguage(language);
        ap.setName(name);
        ap.setPhone_number("(+91) 983 893 3937");
        ap.setWebsite("http://google.com");

        ArrayList<String> al = new ArrayList<>();
        al.add("shoe park");
        al.add("shop");
        ap.setTypes(al);

        Location l = new Location();
        l.setLat(-38.383494);
        l.setLng(33.427362);
        ap.setLocation(l);

        return ap;
    }

    public String deletePlacePayload(String placeId) {
        return "{\"place_id\":\"" + placeId + "\"}";
    }

    // ── Post Management API payloads ──────────────────────────────────────────

    /**
     * Builds a post request with title and an optional body.
     * Pass null for body to omit it entirely from the serialised JSON —
     * the @JsonInclude(NON_NULL) annotation on CreateUserRequest handles this.
     *
     *   createPostPayload("Hello", "World") → {"title":"Hello","body":"World"}
     *   createPostPayload("Hello", null)    → {"title":"Hello"}
     */
    public CreateUserRequest createPostPayload(String title, String body) {
        return new CreateUserRequest(title, body);
    }
}
