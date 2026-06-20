package resources;

/**
 * Central registry of every API endpoint path used across the framework.
 * Add new entries here when onboarding additional endpoints — no other
 * file needs changing to make the path available to step definitions.
 *
 * Paths that contain {id} are REST Assured path-param templates:
 *   given().pathParam("id", 1).when().get(GetPostAPI.getResource())
 */
public enum APIResources {

    // ── Places API (rahulshettyacademy.com) ──────────────────────────────────
    AddPlaceAPI   ("/maps/api/place/add/json"),
    getPlaceAPI   ("/maps/api/place/get/json"),
    deletePlaceAPI("/maps/api/place/delete/json"),

    // ── Post Management API (jsonplaceholder.typicode.com) ───────────────────
    CreatePostAPI("/posts"),
    GetPostAPI   ("/posts/{id}"),
    UpdatePostAPI("/posts/{id}"),
    DeletePostAPI("/posts/{id}");

    private final String resource;

    APIResources(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
