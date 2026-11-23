
package za.co.autotest.api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class ApiClient {

    private final RequestSpecification requestSpecification;

    public ApiClient(String baseUrl) {

        requestSpecification = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .build();
    }

    // Send a GET request.
    public Response get(String endpoint) {

        return given()
                .spec(requestSpecification)
                .when()
                .get(endpoint);
    }

    // Send a POST request.
    public Response post(String endpoint, Object requestBody) {

        return given()
                .spec(requestSpecification)
                .body(requestBody)
                .when()
                .post(endpoint);
    }

    // Send a PUT request.
    public Response put(String endpoint, Object requestBody) {

        return given()
                .spec(requestSpecification)
                .body(requestBody)
                .when()
                .put(endpoint);
    }

    // Send a PATCH request.
    public Response patch(String endpoint, Object requestBody) {

        return given()
                .spec(requestSpecification)
                .body(requestBody)
                .when()
                .patch(endpoint);
    }
}
