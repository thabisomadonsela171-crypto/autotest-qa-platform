
package za.co.autotest.api;

import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;
import java.util.Map;

import za.co.autotest.core.BaseTest;

public class ApiTest extends BaseTest {

    private final ApiClient apiClient =
            new ApiClient("https://jsonplaceholder.typicode.com");

    // Test 1: Retrieve an existing post.
    @Test
    public void shouldRetrievePostSuccessfully() {

        apiClient.get("/posts/1")

                .then()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("title", equalTo(
                    "sunt aut facere repellat provident occaecati excepturi optio reprehenderit"
                ));
    }

    // Test 2: Create a new post.
    @Test
    public void shouldCreatePostSuccessfully() {

        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("title", "AutoTest QA Platform");
        requestBody.put("body", "Testing API POST functionality using Java.");
        requestBody.put("userId", 1);

        apiClient.post("/posts", requestBody)

                .then()
                .statusCode(201)
                .body("title", equalTo("AutoTest QA Platform"))
                .body("body", equalTo(
                    "Testing API POST functionality using Java."
                ))
                .body("userId", equalTo(1));
    }

    // Test 3: Update an existing post using PUT.
    @Test
    public void shouldUpdatePostSuccessfully() {

        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("id", 1);
        requestBody.put("title", "Updated AutoTest QA Platform");
        requestBody.put("body", "Testing API PUT functionality using Java.");
        requestBody.put("userId", 1);

        apiClient.put("/posts/1", requestBody)

                .then()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("title", equalTo(
                    "Updated AutoTest QA Platform"
                ))
                .body("body", equalTo(
                    "Testing API PUT functionality using Java."
                ))
                .body("userId", equalTo(1));
    }

    // Test 4: Partially update an existing post using PATCH.
    @Test
    public void shouldPartiallyUpdatePostSuccessfully() {

        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("title", "Partially Updated AutoTest QA Platform");

        apiClient.patch("/posts/1", requestBody)

                .then()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("title", equalTo(
                    "Partially Updated AutoTest QA Platform"
                ));
    }
}
