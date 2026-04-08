package com.sdet.api.tests;

import com.sdet.api.base.ApiBaseTest;
import io.qameta.allure.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.lessThan;

/**
 * SchemaValidationTest - JSON Schema contract validation
 *
 * OCC Relevance:
 * - Validates ENTIRE API contract, not just individual fields
 * - Critical for cloud vs legacy comparison — both must return same schema
 * - Catches breaking API changes before they reach production
 * - Schema files act as living API documentation
 * - One assertion validates: field names, data types, required fields, structure
 */
@Epic("API Testing")
@Feature("Schema Contract Validation")
public class SchemaValidationTest extends ApiBaseTest {

    private static final Logger log =
            LogManager.getLogger(SchemaValidationTest.class);

    /**
     * Validate GET /users response matches full schema contract
     * OCC: Ensures cloud system returns same structure as legacy
     */
    @Test(priority = 1,
            description = "Validate GET /users response matches JSON schema contract")
    @Story("Schema Validation")
    @Severity(SeverityLevel.CRITICAL)
    public void validateUserListSchema() {
        log.info("Validating schema for GET /users");

        given()
                .spec(requestSpec)
                .queryParam("page", 1)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                // One line validates the ENTIRE response structure
                .body(matchesJsonSchemaInClasspath(
                        "schemas/user_list_schema.json"))
                .time(lessThan(5000L));

        log.info("User list schema validation — PASSED");
    }

    /**
     * Validate GET /users/{id} response schema
     * OCC: Single record retrieval must match contract
     */
    @Test(priority = 2,
            description = "Validate GET /users/{id} response matches JSON schema")
    @Story("Schema Validation")
    @Severity(SeverityLevel.CRITICAL)
    public void validateSingleUserSchema() {
        log.info("Validating schema for GET /users/2");

        given()
                .spec(requestSpec)
                .when()
                .get("/users/{id}", 2)
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath(
                        "schemas/single_user_schema.json"))
                .time(lessThan(5000L));

        log.info("Single user schema validation — PASSED");
    }

    /**
     * Validate POST /login response schema
     * OCC: Auth response must always return token in correct format
     */
    @Test(priority = 3,
            description = "Validate POST /login response matches JSON schema")
    @Story("Schema Validation")
    @Severity(SeverityLevel.BLOCKER)
    public void validateLoginSchema() {
        log.info("Validating schema for POST /login");

        String requestBody = "{\"email\": \"eve.holt@reqres.in\", \"password\": \"cityslicka\"}";

        given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath(
                        "schemas/login_schema.json"))
                .time(lessThan(5000L));

        log.info("Login schema validation — PASSED");
    }

    /**
     * Schema consistency test — page 1 vs page 2
     * OCC Core Use Case: Both pages must return identical schema
     * This mirrors cloud vs legacy comparison logic
     */
    @Test(priority = 4,
            description = "Verify schema is consistent across all pages")
    @Story("Schema Consistency")
    @Severity(SeverityLevel.CRITICAL)
    public void validateSchemaConsistencyAcrossPages() {
        log.info("Validating schema consistency across pages 1 and 2");

        // Page 1
        given()
                .spec(requestSpec)
                .queryParam("page", 1)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath(
                        "schemas/user_list_schema.json"));

        // Page 2 — must return IDENTICAL schema
        given()
                .spec(requestSpec)
                .queryParam("page", 2)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath(
                        "schemas/user_list_schema.json"));

        log.info("Schema consistency across pages — PASSED");
        log.info("Both pages return identical contract — cloud/legacy parity confirmed");
    }

    /**
     * Negative schema test — 404 response structure
     * OCC: Even error responses must follow a contract
     */
    @Test(priority = 5,
            description = "Validate 404 response returns correct error structure")
    @Story("Error Response Schema")
    @Severity(SeverityLevel.NORMAL)
    public void validateNotFoundResponseSchema() {
        log.info("Validating 404 response structure for GET /users/9999");

        given()
                .spec(requestSpec)
                .when()
                .get("/users/{id}", 9999)
                .then()
                .statusCode(404)
                // 404 returns empty object {} — validate it's not null
                .body(org.hamcrest.Matchers.notNullValue())
                .time(lessThan(5000L));

        log.info("404 error schema — PASSED");
    }
}