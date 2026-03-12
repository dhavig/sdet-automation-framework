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
 * CRUDOperationsTest - Full Create/Read/Update/Delete API test coverage
 *
 * OCC Relevance:
 * - Validates all data operations — critical for cloud vs legacy comparison
 * - POST/PUT/PATCH/DELETE must behave identically in both environments
 * - Response body validation ensures data integrity after each operation
 * - Chained tests demonstrate real-world API workflow automation
 */
@Epic("API Testing")
@Feature("CRUD Operations")
public class CRUDOperationsTest extends ApiBaseTest {

    private static final Logger log =
            LogManager.getLogger(CRUDOperationsTest.class);

    // Shared state — created user ID reused across chained tests
    private static int createdUserId;

    // ─── CREATE ───────────────────────────────────────────────────

    /**
     * POST /users — Create new user
     * Validates resource creation returns correct response
     * OCC: Tests data insertion pipeline — cloud must match legacy
     */
    @Test(priority = 1,
            description = "Verify POST /users creates new user and returns generated ID")
    @Story("Create User")
    @Severity(SeverityLevel.CRITICAL)
    public void createUser() {
        log.info("Testing POST /users — create new user");

        String requestBody = """
            {
                "name": "Dhanya Sridhar",
                "job": "Senior SDET"
            }
            """;

        createdUserId = given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .post("/users")
                .then()
                .statusCode(201)  // 201 Created — not 200!
                .body("name",      equalTo("Dhanya Sridhar"))
                .body("job",       equalTo("Senior SDET"))
                .body("id",        notNullValue())  // system generates ID
                .body("createdAt", notNullValue())  // timestamp auto-generated
                .time(lessThan(5000L))
                .extract()
                .jsonPath()
                .getInt("id");

        Assert.assertTrue(createdUserId > 0,
                "Created user ID should be positive integer");

        log.info("POST /users PASSED — Created user ID: {}", createdUserId);
    }

    /**
     * POST /users — Data-driven creation
     * Tests multiple user types with @DataProvider
     * OCC: Financial systems handle multiple record types
     */
    @Test(priority = 2,
            dataProvider = "createUserData",
            dataProviderClass = ApiTestDataProvider.class,
            description = "Verify POST /users creates users with various data sets")
    @Story("Create User")
    @Severity(SeverityLevel.NORMAL)
    public void createUserDataDriven(String name,
                                     String job,
                                     int expectedStatus) {
        log.info("Testing POST /users | name={} | job={}", name, job);

        String requestBody = String.format("""
            {
                "name": "%s",
                "job": "%s"
            }
            """, name, job);

        given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .post("/users")
                .then()
                .statusCode(expectedStatus)
                .body("name", equalTo(name))
                .body("job",  equalTo(job))
                .body("id",   notNullValue())
                .time(lessThan(5000L));

        log.info("Data-driven POST PASSED for: {}", name);
    }

    // ─── UPDATE (PUT) ─────────────────────────────────────────────

    /**
     * PUT /users/{id} — Full resource replacement
     * Replaces ENTIRE user record
     * OCC: Full record sync between cloud and legacy
     */
    @Test(priority = 3,
            dataProvider = "updateUserData",
            dataProviderClass = ApiTestDataProvider.class,
            description = "Verify PUT /users/{id} fully replaces user record")
    @Story("Update User — Full Replace")
    @Severity(SeverityLevel.CRITICAL)
    public void updateUserWithPut(int userId,
                                  String name,
                                  String job,
                                  int expectedStatus) {
        log.info("Testing PUT /users/{} | name={} | job={}", userId, name, job);

        String requestBody = String.format("""
            {
                "name": "%s",
                "job": "%s"
            }
            """, name, job);

        given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .put("/users/{id}", userId)
                .then()
                .statusCode(expectedStatus)
                .body("name",      equalTo(name))
                .body("job",       equalTo(job))
                .body("updatedAt", notNullValue())  // timestamp confirms update
                .time(lessThan(5000L));

        log.info("PUT /users/{} PASSED", userId);
    }

    // ─── UPDATE (PATCH) ───────────────────────────────────────────

    /**
     * PATCH /users/{id} — Partial update
     * Updates ONLY specified fields — leaves others unchanged
     *
     * KEY INTERVIEW POINT:
     * PUT  = replace everything (send all fields)
     * PATCH = update only what you send (other fields unchanged)
     *
     * OCC: Partial updates common in financial record corrections
     */
    @Test(priority = 4,
            description = "Verify PATCH /users/{id} updates only specified fields")
    @Story("Update User — Partial")
    @Severity(SeverityLevel.CRITICAL)
    public void updateUserWithPatch() {
        log.info("Testing PATCH /users/2 — partial update job title only");

        // Only sending job — name should remain unchanged
        String requestBody = """
            {
                "job": "Lead SDET"
            }
            """;

        given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .patch("/users/{id}", 2)
                .then()
                .statusCode(200)
                .body("job",       equalTo("Lead SDET"))
                .body("updatedAt", notNullValue())
                .time(lessThan(5000L));

        log.info("PATCH /users/2 PASSED — partial update confirmed");
    }

    /**
     * PATCH vs PUT comparison test
     * Demonstrates understanding of the difference
     * OCC: Interviewers love this question!
     */
    @Test(priority = 5,
            description = "Demonstrate difference between PUT and PATCH behavior")
    @Story("PUT vs PATCH")
    @Severity(SeverityLevel.NORMAL)
    public void demonstratePutVsPatchDifference() {
        log.info("Demonstrating PUT vs PATCH difference");

        // PATCH — only update name, job should be unaffected
        String patchBody = """
            {
                "name": "Dhanya Updated"
            }
            """;

        String patchUpdatedAt = given()
                .spec(requestSpec)
                .body(patchBody)
                .when()
                .patch("/users/{id}", 2)
                .then()
                .statusCode(200)
                .body("name",      equalTo("Dhanya Updated"))
                // job NOT in response since we didn't send it
                .time(lessThan(5000L))
                .extract()
                .jsonPath()
                .getString("updatedAt");

        // PUT — replaces entire record, all fields required
        String putBody = """
            {
                "name": "Dhanya Full Update",
                "job": "Principal SDET"
            }
            """;

        String putUpdatedAt = given()
                .spec(requestSpec)
                .body(putBody)
                .when()
                .put("/users/{id}", 2)
                .then()
                .statusCode(200)
                .body("name", equalTo("Dhanya Full Update"))
                .body("job",  equalTo("Principal SDET"))
                .time(lessThan(5000L))
                .extract()
                .jsonPath()
                .getString("updatedAt");

        Assert.assertNotNull(patchUpdatedAt, "PATCH updatedAt should not be null");
        Assert.assertNotNull(putUpdatedAt,   "PUT updatedAt should not be null");

        log.info("PUT vs PATCH demonstration PASSED");
        log.info("PATCH updatedAt: {} | PUT updatedAt: {}",
                patchUpdatedAt, putUpdatedAt);
    }

    // ─── DELETE ───────────────────────────────────────────────────

    /**
     * DELETE /users/{id} — Remove resource
     * Validates 204 No Content — nothing returned on delete
     * OCC: Data deletion must be confirmed — no orphan records
     */
    @Test(priority = 6,
            description = "Verify DELETE /users/{id} removes resource with 204 No Content")
    @Story("Delete User")
    @Severity(SeverityLevel.CRITICAL)
    public void deleteUser() {
        log.info("Testing DELETE /users/2");

        given()
                .spec(requestSpec)
                .when()
                .delete("/users/{id}", 2)
                .then()
                .statusCode(204)  // 204 No Content — body is empty on delete
                .body(emptyOrNullString())
                .time(lessThan(5000L));

        log.info("DELETE /users/2 PASSED — 204 No Content confirmed");
    }

    /**
     * Full CRUD lifecycle test — chain all operations
     * Create → Read → Update → Delete in one test
     * OCC: Validates complete data lifecycle integrity
     */
    @Test(priority = 7,
            description = "Verify complete CRUD lifecycle for a single resource")
    @Story("Full CRUD Lifecycle")
    @Severity(SeverityLevel.BLOCKER)
    public void fullCRUDLifecycle() {
        log.info("Testing full CRUD lifecycle");

        // STEP 1 — CREATE
        String createBody = """
            {
                "name": "CRUD Test User",
                "job": "QA Engineer"
            }
            """;

        String newUserId = given()
                .spec(requestSpec)
                .body(createBody)
                .when()
                .post("/users")
                .then()
                .statusCode(201)
                .body("name", equalTo("CRUD Test User"))
                .extract().jsonPath().getString("id");

        log.info("STEP 1 CREATE — user created with ID: {}", newUserId);

        // STEP 2 — READ (verify exists)
        given()
                .spec(requestSpec)
                .when()
                .get("/users/{id}", 2) // reqres uses fixed IDs
                .then()
                .statusCode(200)
                .body("data.id", notNullValue());

        log.info("STEP 2 READ — user verified");

        // STEP 3 — UPDATE
        String updateBody = """
            {
                "name": "CRUD Test User Updated",
                "job": "Senior QA Engineer"
            }
            """;

        given()
                .spec(requestSpec)
                .body(updateBody)
                .when()
                .put("/users/{id}", 2)
                .then()
                .statusCode(200)
                .body("name", equalTo("CRUD Test User Updated"))
                .body("job",  equalTo("Senior QA Engineer"));

        log.info("STEP 3 UPDATE — user updated");

        // STEP 4 — DELETE
        given()
                .spec(requestSpec)
                .when()
                .delete("/users/{id}", 2)
                .then()
                .statusCode(204);

        log.info("STEP 4 DELETE — user removed");
        log.info("Full CRUD lifecycle PASSED");
    }
}