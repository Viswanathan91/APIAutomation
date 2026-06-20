package resources;

import pojo.AddPlace;
import pojo.CreateUserRequest;
import pojo.DeletePlaceRequest;
import pojo.Location;

import java.util.Arrays;

public class TestDataBuild {

    // ── Places API: fixed fields not driven by scenarios ─────────────────────
    private static final int    DEFAULT_ACCURACY     = 50;
    private static final String DEFAULT_PHONE        = "(+91) 983 893 3937";
    private static final String DEFAULT_WEBSITE      = "http://google.com";
    private static final double DEFAULT_LAT          = -38.383494;
    private static final double DEFAULT_LNG          =  33.427362;

    // ── Places API payloads ───────────────────────────────────────────────────

    public AddPlace addPlacePayload(String name, String language, String address) {
        AddPlace ap = new AddPlace();
        ap.setAccuracy(DEFAULT_ACCURACY);
        ap.setAddress(address);
        ap.setLanguage(language);
        ap.setName(name);
        ap.setPhoneNumber(DEFAULT_PHONE);
        ap.setWebsite(DEFAULT_WEBSITE);
        ap.setTypes(Arrays.asList("shoe park", "shop"));

        Location l = new Location();
        l.setLat(DEFAULT_LAT);
        l.setLng(DEFAULT_LNG);
        ap.setLocation(l);

        return ap;
    }

    // Returns a typed POJO — REST Assured serialises it to {"place_id":"<id>"}
    public DeletePlaceRequest deletePlacePayload(String placeId) {
        return new DeletePlaceRequest(placeId);
    }

    // ── Post Management API payloads ──────────────────────────────────────────

    public CreateUserRequest createPostPayload(String title, String body) {
        return new CreateUserRequest(title, body);
    }
}
