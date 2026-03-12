package com.sdet.api.tests;

import com.sdet.api.base.ApiBaseTest;
import com.sdet.api.utils.ApiTestDataProvider;
import io.qameta.allure.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * GetUsersTest - REST Assured GET request tests
 *
 * OCC Relevance:
 * - Validates data retrieval from API endpoints
 * - Response time assertions = SLA compliance
 * - Data-driven via @DataProvider = multiple scenarios, one method
 * - Allure filter auto-attaches all req/res as audit evidence
 */
@Epic("API Testing")
@Feature("User Management API")
public class GetUsersTest extends ApiBaseTest {

    private static final Logger log = LogManager.getLogger(GetUsersTest.class);

    /**
     * GET /users?page=1
     * Validates paginated user list response
     * OCC: Verifies paginated data returns correct structure
     */
    @Test(priority = 1,
            description = "Verify GET /users returns paginated list with correct structure")
    @Story("Get Users List")
    @Severity(SeverityLevel.CRITICAL)
    public void getUsersList() {
        log.info("Testing GET /users?page=1");

        given()
                .spec(requestSpec)
                .queryParam("page", 1)
                .when()
                .get("/users")
                .then()
                .spec(responseSpec)
                .statusCode(200)
                // Validate response structure
                .body("page",         equalTo(1))
                .body("per_page",     equalTo(6))
                .body("data",         notNullValue())
                .body("data.size()",  greaterThan(0))
                // Validate first user has required fields
                .body("data[0].id",   notNullValue())
                .body("data[0].email",notNullValue())
                .body("data[0].first_name", notNullValue())
                // Validate response time SLA < 5s
                .time(lessThan(5000L));

        log.info("GET /users — PASSED");
    }

    /**
     * GET /users?page={page}
     * Data-driven pagination test
     * OCC: Financial data always paginated — must validate all pages
     */
    @Test(priority = 2,
            dataProvider = "paginationData",
            dataProviderClass = ApiTestDataProvider.class,
            description = "Verify paginated GET /users across multiple pages")
    @Story("Pagination Validation")
    @Severity(SeverityLevel.NORMAL)
    public void getUsersByPage(int page, int expectedPerPage, int expectedStatus) {
        log.info("Testing GET /users?page={}", page);

        var response = given()
                .spec(requestSpec)
                .queryParam("page", page)
                .when()
                .get("/users")
                .then()
                .spec(responseSpec)
                .statusCode(expectedStatus)
                .body("page", equalTo(page))
                .extract().response();

        log.info("Page {} returned {} users | Status: {}",
                page,
                response.jsonPath().getList("data").size(),
                response.statusCode());
    }

    /**
     * GET /users/{id}
     * Data-driven single user retrieval
     * OCC: Validates individual record retrieval — core data integrity check
     */
    @Test(priority = 3,
            dataProvider = "userData",
            dataProviderClass = ApiTestDataProvider.class,
            description = "Verify GET /users/{id} returns correct user data")
    @Story("Get Single User")
    @Severity(SeverityLevel.CRITICAL)
    public void getSingleUser(int userId,
                              int expectedStatus,
                              String expectedEmail) {
        log.info("Testing GET /users/{}", userId);

        if (expectedStatus == 200) {
            // Valid user — assert full response structure
            given()
                    .spec(requestSpec)
                    .when()
                    .get("/users/{id}", userId)
                    .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("data.id",    equalTo(userId))
                    .body("data.email", equalTo(expectedEmail))
                    .body("data.first_name", notNullValue())
                    .body("data.last_name",  notNullValue())
                    .body("data.avatar",     notNullValue())
                    .time(lessThan(5000L));

        } else {
            // Non-existent user — assert 404
            given()
                    .spec(requestSpec)
                    .when()
                    .get("/users/{id}", userId)
                    .then()
                    .statusCode(404)
                    .body(equalTo("{}"));
        }

        log.info("GET /users/{} — PASSED (status: {})",
                userId, expectedStatus);
    }

    /**
     * GET /users/{id} — Negative test
     * Verify proper 404 handling for missing resources
     * OCC: Financial systems must handle missing records gracefully
     */
    @Test(priority = 4,
            description = "Verify GET /users/{id} returns 404 for non-existent user")
    @Story("Error Handling")
    @Severity(SeverityLevel.NORMAL)
    public void getUserNotFound() {
        log.info("Testing GET /users/9999 — expect 404");

        given()
                .spec(requestSpec)
                .when()
                .get("/users/{id}", 9999)
                .then()
                .statusCode(404)
                .body(notNullValue())
                .time(lessThan(5000L));

        log.info("GET /users/9999 — 404 handled correctly");
    }

    /**
     * GET /users — Response headers validation
     * OCC: Financial APIs must return correct content-type headers
     */
    @Test(priority = 5,
            description = "Verify response headers contain correct content-type")
    @Story("Response Headers")
    @Severity(SeverityLevel.MINOR)
    public void validateResponseHeaders() {
        log.info("Testing response headers for GET /users");

        given()
                .spec(requestSpec)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .header("Content-Type", containsString("application/json"))
                .time(lessThan(5000L));

        log.info("Response headers — PASSED");
    }

    /**
     * GET /users — Extract and use response values
     * Demonstrates response extraction — useful for chaining API calls
     * OCC: Extract token from login → use in subsequent requests
     */
    @Test(priority = 6,
            description = "Extract response values and validate data types")
    @Story("Response Extraction")
    @Severity(SeverityLevel.NORMAL)
    public void extractAndValidateResponse() {
        log.info("Testing response extraction from GET /users");

        // Extract total user count from response
        int totalUsers = given()
                .spec(requestSpec)
                .queryParam("page", 1)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getInt("total");

        log.info("Total users in system: {}", totalUsers);

        // Assert total is a reasonable number
        assert totalUsers > 0 : "Total users should be greater than 0";
        assert totalUsers < 10000 : "Total users seems unreasonably high";

        log.info("Response extraction — PASSED | Total users: {}", totalUsers);
    }
}