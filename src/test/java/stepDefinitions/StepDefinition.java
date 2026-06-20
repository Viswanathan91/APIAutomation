package stepDefinitions;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.util.ArrayList;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import static org.junit.Assert.*;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import pojo.AddPlace;
import pojo.Location;
import resources.APIResources;
import resources.TestDataBuild;
import resources.Utils;

public class StepDefinition extends Utils {
	
	RequestSpecification res;
	ResponseSpecification resSpec;
	Response response;
	//here we are mentioning the below variable as static because,the value of the variable remains same till all the scenarios are run,else it will be reset after the first scenario run
	static String placeId;
	
	TestDataBuild tb = new TestDataBuild();
	
	@Given("Add Place Payload with {string} {string} {string}")
	public void add_place_payload_with(String name, String language, String address) throws IOException {
	
		//Using Request Specification
		

		
		res = given().spec(requestSpecification())
			          .body(tb.addPlacePayload(name,language,address));
		
	//Using Response Specification
			
	}
	
	@When("user calls {string} with {string} http request")
	public void user_calls_with_http_request(String resource, String method) {
		
		//constructor will be called with value of resource which you pass
	  APIResources resourceAPI = APIResources.valueOf(resource);
	  //System.out.println(resourceAPI.getResource());
	  
		//resSpec = new ResponseSpecBuilder().expectStatusCode(200).expectContentType(ContentType.JSON).build();
		
		if(method.equalsIgnoreCase("POST"))
		response = res.when().post(resourceAPI.getResource());
		else if(method.equalsIgnoreCase("GET"))
		response = res.when().get(resourceAPI.getResource());
		
	}
	@Then("the API call got success with status code {int}")
	public void the_api_call_got_success_with_status_code(Integer int1) {

		assertEquals(int1.intValue(), response.getStatusCode());

	}
	@And("{string} in response body is {string}")
	public void in_response_body_is(String key, String value) {
	 
		
		assertEquals(getJsonPath(response,key),value);
		
	}
	
	@And("verify place_id created maps to {string} using {string}")
	public void verify_place_id_created_maps_to_using_get_place_api(String expectedName, String resource) throws IOException {
		
	    placeId = getJsonPath(response,"place_id");
		
		res = given().spec(requestSpecification()).queryParam("place_id", placeId);
		
		user_calls_with_http_request(resource,"GET");
		
		String actualName = getJsonPath(response,"name");
		
		assertEquals(actualName,expectedName);
			
	}
	
	@Given("DeletePlace payload")
	public void delete_place_payload() throws IOException {
	 
	 res = given().spec(requestSpecification()).body(tb.deletePlacePayload(placeId));
		
	}
	
	


}
