package com.sdet.api.base;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

/**
 * ApiBaseTest - Foundation for all REST Assured API tests
 *
 * OCC Relevance:
 * - Centralizes base URL, headers, auth — one place to update per environment
 * - AllureRestAssured filter auto-attaches request/response to Allure (audit evidence)
 * - RequestSpecification + ResponseSpecification = reusable, consistent test setup
 * - Mirrors how enterprise API frameworks are built in financial systems
 */
public class ApiBaseTest {

    private static final Logger log = LogManager.getLogger(ApiBaseTest.class);

    // Base URL — reads from system property for CI/CD flexibility
    // Default: reqres.in — a free REST API for testing (like SauceDemo for APIs)
    protected static final String BASE_URL = System.getProperty(
            "api.base.url",
            "https://reqres.in"
    );

    protected static final String API_VERSION = "/api";
    protected static final int    TIMEOUT_MS  = 5000; // SLA: 5s max

    // Shared specs — built once, reused across all tests
    protected static RequestSpecification  requestSpec;
    protected static ResponseSpecification responseSpec;

    /**
     * Suite-level setup — runs ONCE before all API tests
     * Builds RequestSpec and ResponseSpec
     */
    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        log.info("Initializing API Test Suite | Base URL: {}", BASE_URL);

        // Configure RestAssured globally
        RestAssured.baseURI  = BASE_URL;
        RestAssured.basePath = API_VERSION;

        // Build reusable RequestSpecification
        // Every test inherits these settings automatically
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(BASE_URL)
                .setBasePath(API_VERSION)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("x-framework", "sdet-automation-framework")
                .setRelaxedHTTPSValidation()     // Accept self-signed certs
                .addFilter(new AllureRestAssured()) // Auto-attach to Allure
                .log(LogDetail.ALL)              // Log all requests
                .build();

        // Build reusable ResponseSpecification
        // Defines baseline expectations for ALL responses
        responseSpec = new ResponseSpecBuilder()
                .expectResponseTime(
                        org.hamcrest.Matchers.lessThan((long) TIMEOUT_MS))
                .build();

        log.info("API Base configuration complete.");
    }

    /**
     * Class-level setup — runs before each test class
     */
    @BeforeClass(alwaysRun = true)
    public void classSetup() {
        log.info("Starting API test class: {}",
                this.getClass().getSimpleName());
    }

    /**
     * Helper — get base RequestSpecification
     * Tests call this to start building their requests
     */
    protected RequestSpecification givenRequest() {
        return RestAssured.given().spec(requestSpec);
    }
}