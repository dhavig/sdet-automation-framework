# Test Cases — SDET Automation Framework

> Document covering all test cases for the SDET Automation Framework.
> Each test case includes the objective, input, expected result, and status.

---

## Module 1 — Selenium UI Tests
**File:** `src/test/java/com/sdet/selenium/LoginTest.java`
**Goal:** Verify login functionality works correctly for all user scenarios.

| TC ID | Test Case Name | Input | Expected Result | Status |
|---|---|---|---|---|
| TC-001 | Valid Login | Valid username and password | User is redirected to products page | ✅ PASS |
| TC-002 | Invalid Login | Wrong username or password | Login fails with error message | ✅ PASS |
| TC-003 | Empty Credentials | Empty username and password fields | Login fails with validation message | ✅ PASS |
| TC-004 | Locked Out User | Locked out user credentials | User cannot login, error message shown | ✅ PASS |

---

## Module 2 — API Tests — Authentication
**File:** `src/test/java/com/sdet/api/tests/AuthApiTest.java`
**Goal:** Verify authentication API endpoints handle valid and invalid scenarios correctly.

| TC ID | Test Case Name | Input | Expected Result | Status |
|---|---|---|---|---|
| TC-005 | Login With Valid Credentials | POST /login — valid email and password | 200 OK with auth token returned | ✅ PASS |
| TC-006 | Login With Invalid Credentials | POST /login — invalid email/password combinations (data-driven) | 400 error with appropriate message | ✅ PASS |
| TC-007 | Login With Missing Password | POST /login — email only, no password | 400 error for missing password | ✅ PASS |
| TC-008 | Login With Missing Email | POST /login — password only, no email | 400 error for missing email | ✅ PASS |
| TC-009 | Use Token In Subsequent Request | Extracted auth token used in next API call | Token accepted, request succeeds | ✅ PASS |
| TC-010 | Register New User | POST /register — valid email and password | 200 OK with new token returned | ✅ PASS |
| TC-011 | Register With Missing Password | POST /register — email only | 400 error for missing password | ✅ PASS |

---

## Module 3 — API Tests — CRUD Operations
**File:** `src/test/java/com/sdet/api/tests/CRUDOperationsTest.java`
**Goal:** Verify Create, Read, Update, Delete operations work correctly on user resources.

| TC ID | Test Case Name | Input | Expected Result | Status |
|---|---|---|---|---|
| TC-012 | Create User | POST /users — name and job | 201 Created with generated ID returned | ✅ PASS |
| TC-013 | Create User Data Driven | POST /users — multiple name/job combinations | 201 Created for each data set | ✅ PASS |
| TC-014 | Update User With PUT | PUT /users/{id} — full user object (data-driven) | 200 OK, entire record replaced | ✅ PASS |
| TC-015 | Update User With PATCH | PATCH /users/{id} — partial user fields | 200 OK, only specified fields updated | ✅ PASS |
| TC-016 | PUT vs PATCH Difference | PUT replaces all fields, PATCH updates only specified fields | Behavior difference demonstrated correctly | ✅ PASS |
| TC-017 | Delete User | DELETE /users/{id} | 204 No Content returned | ✅ PASS |
| TC-018 | Full CRUD Lifecycle | Create → Read → Update → Delete on same resource | All operations succeed in sequence | ✅ PASS |

---

## Module 4 — API Tests — GET Users
**File:** `src/test/java/com/sdet/api/tests/GetUsersTest.java`
**Goal:** Verify GET endpoints return correct data, pagination, and error handling.

| TC ID | Test Case Name | Input | Expected Result | Status |
|---|---|---|---|---|
| TC-019 | Get Users List | GET /users | 200 OK with paginated list and correct structure | ✅ PASS |
| TC-020 | Get Users By Page | GET /users?page={n} — multiple pages (data-driven) | Correct users returned per page | ✅ PASS |
| TC-021 | Get Single User | GET /users/{id} — multiple valid IDs (data-driven) | Correct user data returned | ✅ PASS |
| TC-022 | Get User Not Found | GET /users/999 — non-existent user | 404 Not Found returned | ✅ PASS |
| TC-023 | Validate Response Headers | GET /users | Content-Type header is application/json | ✅ PASS |
| TC-024 | Extract And Validate Response | GET /users/{id} | Response values extracted and data types validated | ✅ PASS |

---

## Module 5 — API Tests — Schema Validation
**File:** `src/test/java/com/sdet/api/tests/SchemaValidationTest.java`
**Goal:** Verify all API responses match the expected JSON schema contract.

| TC ID | Test Case Name | Input | Expected Result | Status |
|---|---|---|---|---|
| TC-025 | Validate User List Schema | GET /users | Response matches user list JSON schema | ✅ PASS |
| TC-026 | Validate Single User Schema | GET /users/{id} | Response matches single user JSON schema | ✅ PASS |
| TC-027 | Validate Login Schema | POST /login | Response matches login JSON schema | ✅ PASS |
| TC-028 | Validate Schema Consistency Across Pages | GET /users — all pages | Schema is identical across all pages | ✅ PASS |
| TC-029 | Validate Not Found Response Schema | GET /users/999 | 404 response has correct error structure | ✅ PASS |

---

## Module 6 — Parallel Tests
**File:** `src/test/java/com/sdet/parallel/ParallelLoginTest.java`
**Goal:** Verify behavior is consistent across Legacy and Cloud environments running in parallel.

| TC ID | Test Case Name | Input | Expected Result | Status |
|---|---|---|---|---|
| TC-030 | Parallel Login Comparison | Login on Legacy and Cloud environments simultaneously | Login behavior identical across both environments | ✅ PASS |
| TC-031 | Parallel Product Page Comparison | Product page on Legacy and Cloud environments simultaneously | Product listing identical across both environments | ✅ PASS |

---

## Module 7 — Performance Tests
**File:** `src/test/java/com/sdet/performance/PerformanceTestSuite.java`
**Goal:** Verify the application meets performance SLAs under various load conditions.

| TC ID | Test Case Name | Input | Expected Result | Status |
|---|---|---|---|---|
| TC-032 | Load Test | 50 concurrent users for 60 seconds | All requests complete within SLA thresholds | ✅ PASS |
| TC-033 | Stress Test | 200 concurrent users ramped over 30 seconds | System breaking point identified and documented | ✅ PASS |
| TC-034 | Soak Test | 25 users for 300 seconds | No memory leaks or performance degradation detected | ✅ PASS |

---

## Module 8 — Cucumber BDD Tests
**File:** `src/test/resources/features/` + `src/test/java/com/sdet/cucumber/steps/LoginSteps.java`
**Goal:** Verify login scenarios using BDD-style feature files readable by non-technical stakeholders.

| TC ID | Test Case Name | Scenario | Expected Result | Status |
|---|---|---|---|---|
| TC-035 | BDD Valid Login | Given I am on login page, When I login with valid credentials, Then I am redirected to products page | Products page displayed | ✅ PASS |
| TC-036 | BDD Invalid Login | Given I am on login page, When I login with invalid credentials, Then I see an error message | Error message displayed | ✅ PASS |

---

## Test Summary

| Module | Total Tests | Passed | Failed |
|---|---|---|---|
| Selenium UI Tests | 4 | 4 | 0 |
| API — Authentication | 7 | 7 | 0 |
| API — CRUD Operations | 7 | 7 | 0 |
| API — GET Users | 6 | 6 | 0 |
| API — Schema Validation | 5 | 5 | 0 |
| Parallel Tests | 2 | 2 | 0 |
| Performance Tests | 3 | 3 | 0 |
| Cucumber BDD Tests | 2 | 2 | 0 |
| **Total** | **36** | **36** | **0** |

---

## Tech Stack

| Tool | Purpose |
|---|---|
| Java | Primary language |
| Selenium 4 | UI automation |
| REST Assured | API testing |
| TestNG | Test runner and data providers |
| Cucumber | BDD framework |
| Allure | Test reporting |
| GitHub Actions | CI/CD pipeline |

---

*Last updated: April 2026*
*Total: 36 test cases — 36 documented*
