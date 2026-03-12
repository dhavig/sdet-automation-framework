package com.sdet.api.utils;

import org.testng.annotations.DataProvider;

/**
 * ApiTestDataProvider - Centralized test data for all API tests
 *
 * OCC Relevance:
 * - Data-driven testing = repeatable, scalable test coverage
 * - One test method runs multiple scenarios automatically
 * - Mirrors how financial systems test multiple account types,
 *   transaction amounts, and edge cases without code duplication
 */
public class ApiTestDataProvider {

    /**
     * Valid user credentials for login API
     * Format: { email, password, expectedStatus }
     */
    @DataProvider(name = "validLoginData")
    public static Object[][] validLoginData() {
        return new Object[][] {
                { "eve.holt@reqres.in",    "cityslicka", 200 },
                { "peter.parker@reqres.in","spiderman",  200 },
        };
    }

    /**
     * Invalid credentials — tests error handling
     * OCC: Financial systems must reject bad auth gracefully
     */
    @DataProvider(name = "invalidLoginData")
    public static Object[][] invalidLoginData() {
        return new Object[][] {
                { "invalid@email.com", "wrongpass",  400 },
                { "",                  "password",   400 },
                { "valid@email.com",   "",           400 },
        };
    }

    /**
     * User IDs for GET /users/{id}
     * Format: { userId, expectedStatus, expectedEmail }
     */
    @DataProvider(name = "userData")
    public static Object[][] userData() {
        return new Object[][] {
                { 1,  200, "george.bluth@reqres.in"  },
                { 2,  200, "janet.weaver@reqres.in"  },
                { 12, 200, "rachel.howell@reqres.in" },
                { 99, 404, ""                         }, // non-existent user
        };
    }

    /**
     * New user creation data for POST /users
     * Format: { name, job, expectedStatus }
     * OCC: Tests data creation pipelines
     */
    @DataProvider(name = "createUserData")
    public static Object[][] createUserData() {
        return new Object[][] {
                { "Dhanya Sridhar", "Senior SDET",      201 },
                { "Jane Doe",       "QA Engineer",       201 },
                { "John Smith",     "Performance Tester", 201 },
        };
    }

    /**
     * Update user data for PUT /users/{id}
     * Format: { userId, name, job, expectedStatus }
     */
    @DataProvider(name = "updateUserData")
    public static Object[][] updateUserData() {
        return new Object[][] {
                { 1, "Dhanya Sridhar", "Lead SDET",   200 },
                { 2, "Jane Doe",       "SDET Manager", 200 },
        };
    }

    /**
     * Page numbers for paginated GET /users
     * OCC: Financial systems return paginated data
     * Format: { page, expectedPerPage, expectedStatus }
     */
    @DataProvider(name = "paginationData")
    public static Object[][] paginationData() {
        return new Object[][] {
                { 1, 6, 200 },  // page 1 — 6 users per page
                { 2, 6, 200 },  // page 2
                { 3, 0, 200 },  // page 3 — empty (beyond data)
        };
    }
}