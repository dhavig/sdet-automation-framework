# SDET Automation Framework

![Daily TestNG Run](https://github.com/dhavig/sdet-automation-framework/actions/workflows/daily-test-run.yml/badge.svg)
![Full Suite](https://github.com/dhavig/sdet-automation-framework/actions/workflows/full-suite.yml/badge.svg)

A production-grade test automation framework built with Java 11, covering UI, API, BDD, parallel, and performance testing — with full CI/CD integration and automated Allure reporting.

---

## Tech Stack

| Layer | Tool |
|-------|------|
| Language | Java 11 |
| UI Automation | Selenium WebDriver 4 + Page Object Model |
| API Testing | REST Assured 5 |
| BDD | Cucumber 7 (Gherkin) |
| Test Framework | TestNG 7 |
| Performance | Apache JMeter |
| Reporting | Allure 2 |
| CI/CD | GitHub Actions |
| Build | Maven |

---

## Framework Structure

```
src/
├── main/java/com/sdet/
│   ├── config/          # ConfigReader, EnvironmentConfig
│   ├── pages/           # Page Object Model (LoginPage, ProductsPage, BasePage)
│   ├── parallel/        # Parallel execution engine + response comparator
│   ├── performance/     # JMeter runner, SLA validator, S3 uploader
│   └── utils/           # DriverManager, ScreenshotUtil
│
└── test/java/com/sdet/
    ├── selenium/        # Selenium UI tests (LoginTest)
    ├── parallel/        # Side-by-side env comparison (ParallelLoginTest)
    ├── api/             # REST Assured tests (CRUD, Auth, Schema validation)
    ├── cucumber/        # BDD runner + step definitions
    └── performance/     # JMeter performance suite
```

---

## Running Tests

**Full suite (all modules):**
```bash
mvn test -Dsurefire.suiteXmlFiles=testng.xml
```

**API tests only:**
```bash
mvn test -Dtest="GetUsersTest,AuthApiTest,SchemaValidationTest,CRUDOperationsTest"
```

**Selenium tests (headless):**
```bash
mvn test -Dtest=LoginTest -Dwebdriver.chrome.headless=true
```

**Generate Allure report locally:**
```bash
mvn allure:serve
```

---

## CI/CD

| Workflow | Trigger | What it does |
|----------|---------|--------------|
| `daily-test-run.yml` | 9 AM UTC daily | Runs full TestNG suite, publishes Allure report, updates run log |
| `full-suite.yml` | 6 AM UTC + releases | UI + API + performance suite |
| `api-tests.yml` | Push to main/develop | API regression on every code change |
| `performance-tests.yml` | Push to main | JMeter load tests |

**Live Allure Report:** https://dhavig.github.io/sdet-automation-framework/daily/

---

## Key Design Patterns

- **Page Object Model (POM)** — All UI locators and actions encapsulated in page classes; tests are clean and readable
- **Data-Driven Testing** — `@DataProvider` in TestNG for parameterised API and UI scenarios
- **BDD / Gherkin** — Cucumber feature files readable by non-technical stakeholders
- **Parallel Execution** — `parallel="classes"` in TestNG + Java threads for simultaneous environment comparison
- **Allure Reporting** — Steps, screenshots on failure, and request/response logs auto-attached to every test run

---

## Test Coverage

| Module | Tests | Target App |
|--------|------:|------------|
| Selenium UI | Login, parallel env comparison | SauceDemo |
| REST Assured API | GET, POST, PUT, PATCH, DELETE, schema validation, auth | Reqres.in |
| BDD Cucumber | Login happy path, invalid credentials, empty fields | SauceDemo |
| Performance | Load test, SLA validation | Configurable |
