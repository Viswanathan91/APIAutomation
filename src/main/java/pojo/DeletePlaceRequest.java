package pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeletePlaceRequest {

    @JsonProperty("place_id")
    private String placeId;

    public DeletePlaceRequest(String placeId) {
        this.placeId = placeId;
    }

    public String getPlaceId()              { return placeId; }
    public void   setPlaceId(String id)     { this.placeId = id; }
}
