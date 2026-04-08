package com.sdet.api.tests;

import com.sdet.api.base.ApiBaseTest;
import com.sdet.api.utils.ApiTestDataProvider;
import io.qameta.allure.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * AuthApiTest - Authentication API tests
 *
 * OCC Relevance:
 * - Financial systems require strict auth validation
 * - Tests both positive (valid login) and negative (invalid creds) paths
 * - Token extraction demonstrates API chaining — login then use token
 * - Error message validation ensures system fails gracefully
 */
@Epic("API Testing")
@Feature("Authentication API")
public class AuthApiTest extends ApiBaseTest {

    private static final Logger log = LogManager.getLogger(AuthApiTest.class);

    // Shared token — extracted from login, reused in authenticated tests
    private static String authToken;

    /**
     * POST /login — Successful authentication
     * Validates token is returned for valid credentials
     * OCC: Every system access starts with valid auth
     */
    @Test(priority = 1,
            description = "Verify POST /login returns token for valid credentials")
    @Story("Successful Login")
    @Severity(SeverityLevel.BLOCKER)
    public void loginWithValidCredentials() {
        log.info("Testing POST /login with valid credentials");

        String requestBody = "{\"email\": \"eve.holt@reqres.in\", \"password\": \"cityslicka\"}";

        authToken = given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                // Token must exist and not be empty
                .body("token",            notNullValue())
                .body("token",            not(emptyString()))
                .time(lessThan(5000L))
                // Extract token for use in subsequent tests
                .extract()
                .jsonPath()
                .getString("token");

        Assert.assertNotNull(authToken, "Auth token should not be null");
        Assert.assertFalse(authToken.isEmpty(), "Auth token should not be empty");

        log.info("Login PASSED — Token extracted: {}***",
                authToken.substring(0, Math.min(6, authToken.length())));
    }

    /**
     * POST /login — Data-driven invalid credentials
     * Validates system rejects bad auth with correct error response
     * OCC: System must never accept invalid credentials — security critical
     */
    @Test(priority = 2,
            dataProvider = "invalidLoginData",
            dataProviderClass = ApiTestDataProvider.class,
            description = "Verify POST /login rejects invalid credentials")
    @Story("Failed Login")
    @Severity(SeverityLevel.CRITICAL)
    public void loginWithInvalidCredentials(String email,
                                            String password,
                                            int expectedStatus) {
        log.info("Testing POST /login with invalid credentials: {}", email);

        String requestBody = String.format("{\"email\": \"%s\", \"password\": \"%s\"}", email, password);

        given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .post("/login")
                .then()
                .statusCode(expectedStatus)
                // Error message must be present — not just a status code
                .body("error", notNullValue())
                .body("error", not(emptyString()))
                .time(lessThan(5000L));

        log.info("Invalid login correctly rejected for: {}", email);
    }

    /**
     * POST /login — Missing password field
     * Validates specific error message for incomplete request
     * OCC: API must return descriptive errors — not generic 500s
     */
    @Test(priority = 3,
            description = "Verify POST /login returns descriptive error for missing password")
    @Story("Failed Login")
    @Severity(SeverityLevel.NORMAL)
    public void loginWithMissingPassword() {
        log.info("Testing POST /login with missing password");

        String requestBody = "{\"email\": \"eve.holt@reqres.in\"}";

        given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .post("/login")
                .then()
                .statusCode(400)
                .body("error", equalTo("Missing password"))
                .time(lessThan(5000L));

        log.info("Missing password — correctly returned 400 with error message");
    }

    /**
     * POST /login — Missing email field
     * OCC: Every field validation must be tested explicitly
     */
    @Test(priority = 4,
            description = "Verify POST /login returns descriptive error for missing email")
    @Story("Failed Login")
    @Severity(SeverityLevel.NORMAL)
    public void loginWithMissingEmail() {
        log.info("Testing POST /login with missing email");

        String requestBody = "{\"password\": \"cityslicka\"}";

        given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .post("/login")
                .then()
                .statusCode(400)
                .body("error", equalTo("Missing email or username"))
                .time(lessThan(5000L));

        log.info("Missing email — correctly returned 400 with error message");
    }

    /**
     * POST /login then GET /users using extracted token
     * Demonstrates API chaining — login → use token in next request
     * OCC: Real-world API flows require token from one call
     *      to authenticate the next call
     */
    @Test(priority = 5,
            dependsOnMethods = "loginWithValidCredentials",
            description = "Verify extracted auth token works in subsequent requests")
    @Story("Token Based Auth")
    @Severity(SeverityLevel.CRITICAL)
    public void useTokenInSubsequentRequest() {
        log.info("Testing authenticated request with token: {}***",
                authToken.substring(0, Math.min(6, authToken.length())));

        Assert.assertNotNull(authToken,
                "Token must be set — run loginWithValidCredentials first");

        // Use extracted token as Bearer auth header
        given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("data", notNullValue())
                .time(lessThan(5000L));

        log.info("Authenticated request with token — PASSED");
    }

    /**
     * POST /register — Successful registration
     * Validates new user registration flow
     * OCC: Tests user creation pipeline end-to-end
     */
    @Test(priority = 6,
            description = "Verify POST /register creates new user and returns token")
    @Story("User Registration")
    @Severity(SeverityLevel.CRITICAL)
    public void registerNewUser() {
        log.info("Testing POST /register");

        String requestBody = "{\"email\": \"eve.holt@reqres.in\", \"password\": \"pistol\"}";

        given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .post("/register")
                .then()
                .statusCode(200)
                .body("id",    notNullValue())
                .body("token", notNullValue())
                .body("token", not(emptyString()))
                .time(lessThan(5000L));

        log.info("User registration — PASSED");
    }

    /**
     * POST /register — Missing password
     * Validates registration rejects incomplete data
     */
    @Test(priority = 7,
            description = "Verify POST /register returns error for missing password")
    @Story("Registration Error Handling")
    @Severity(SeverityLevel.NORMAL)
    public void registerWithMissingPassword() {
        log.info("Testing POST /register with missing password");

        String requestBody = "{\"email\": \"sydney@fife.com\"}";

        given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .post("/register")
                .then()
                .statusCode(400)
                .body("error", equalTo("Missing password"))
                .time(lessThan(5000L));

        log.info("Registration missing password — correctly rejected");
    }
}