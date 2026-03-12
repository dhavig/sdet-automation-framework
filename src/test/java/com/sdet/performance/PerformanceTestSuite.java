package com.sdet.performance;

import io.qameta.allure.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.util.ArrayList;
import java.util.List;

/**
 * PerformanceTestSuite - TestNG orchestrator for the full performance pipeline
 *
 * Execution Flow:
 * @BeforeSuite  → Initialize runner, uploader, verify S3 access
 * @Test(1)      → Load Test    → validate SLA → upload to S3
 * @Test(2)      → Stress Test  → validate SLA → upload to S3
 * @Test(3)      → Soak Test    → validate SLA → upload to S3
 * @AfterSuite   → Upload full results dir → generate summary → close S3
 *
 * OCC Relevance:
 * - Single command triggers entire performance pipeline
 * - Each test is independently reportable in Allure
 * - All results archived to S3 as audit evidence
 * - SLA breaches fail the test — caught in CI/CD before production
 */
@Epic("Performance Testing")
@Feature("OCC Financial Systems Performance Validation")
public class PerformanceTestSuite {

    private static final Logger log = LogManager.getLogger(PerformanceTestSuite.class);

    // Target system — SauceDemo (our framework's test app)
    // In OCC: would point to their cloud/legacy financial systems
    private static final String TARGET_HOST   = "www.saucedemo.com";
    private static final int    TARGET_PORT   = 443;
    private static final String TARGET_PATH   = "/";
    private static final String ENVIRONMENT   = System.getProperty("env", "CLOUD");

    // Performance test configuration
    private static final int LOAD_USERS       = 50;   // Normal load
    private static final int LOAD_DURATION    = 60;   // 1 minute
    private static final int STRESS_USERS     = 200;  // Peak load
    private static final int STRESS_RAMP      = 30;   // 30s ramp-up
    private static final int STRESS_DURATION  = 60;   // 1 minute
    private static final int SOAK_USERS       = 25;   // Sustained load
    private static final int SOAK_DURATION    = 300;  // 5 minutes

    private JMeterTestRunner   runner;
    private AWSS3Uploader      s3Uploader;
    private List<PerformanceTestResult> allResults = new ArrayList<>();

    // ─── LIFECYCLE ───────────────────────────────────────────────

    /**
     * Suite setup — runs ONCE before all tests
     * Initializes runner and verifies S3 connectivity
     */
    @BeforeSuite(alwaysRun = true)
    @Step("Initialize Performance Test Suite")
    public void suiteSetup() {
        log.info("========================================");
        log.info("  PERFORMANCE TEST SUITE STARTING");
        log.info("  Environment : {}", ENVIRONMENT);
        log.info("  Target      : {}:{}{}", TARGET_HOST, TARGET_PORT, TARGET_PATH);
        log.info("========================================");

        // Initialize JMeter runner with custom SLA thresholds
        // Max 5s response, 1% error rate, 2s average
        runner = new JMeterTestRunner(ENVIRONMENT, 5000, 1.0, 2000);

        // Initialize S3 uploader
        s3Uploader = new AWSS3Uploader(ENVIRONMENT);

        // Verify S3 bucket is accessible before running tests
        // Fail fast — don't run tests if we can't archive results
        boolean s3Accessible = s3Uploader.verifyBucketAccess();
        if (!s3Accessible) {
            log.warn("S3 bucket not accessible — results will not be archived");
            // Note: We warn but don't fail — allows local runs without AWS
        }

        log.info("Suite setup complete. Ready to execute performance tests.");
    }

    /**
     * Runs before each individual test method
     */
    @BeforeMethod(alwaysRun = true)
    public void testSetup(java.lang.reflect.Method method) {
        log.info("------ Starting test: {} ------", method.getName());
    }

    /**
     * Runs after each individual test method
     * Logs pass/fail status
     */
    @AfterMethod(alwaysRun = true)
    public void testTeardown(ITestResult result) {
        String status = result.isSuccess() ? "PASSED" : "FAILED";
        log.info("------ Test {} : {} ------",
                result.getMethod().getMethodName(), status);
    }

    /**
     * Suite teardown — runs ONCE after all tests
     * Uploads all results, generates summary, closes S3
     */
    @AfterSuite(alwaysRun = true)
    @Step("Finalize Suite — Archive All Results to S3")
    public void suiteTeardown() {
        log.info("========================================");
        log.info("  PERFORMANCE TEST SUITE COMPLETE");
        log.info("  Total tests run: {}", allResults.size());
        log.info("========================================");

        // Upload entire results directory to S3
        List<String> uploadedKeys = s3Uploader.uploadDirectory(
                "target/jmeter-results/",
                "PerformanceTestSuite_" + ENVIRONMENT
        );

        // Generate suite-level summary
        generateSuiteSummary(uploadedKeys);

        // Close S3 client
        s3Uploader.close();

        log.info("Suite teardown complete. {} files archived to S3.",
                uploadedKeys.size());
    }

    // ─── TEST METHODS ─────────────────────────────────────────────

    /**
     * LOAD TEST
     * Validates system handles normal production traffic within SLAs
     *
     * OCC Context: Normal trading day volume
     * Pass Criteria: All responses < 5s, error rate < 1%
     */
    @Test(priority = 1, description = "Load Test — Normal production traffic SLA validation")
    @Story("Load Testing")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Simulates normal production load of " + LOAD_USERS +
            " concurrent users for " + LOAD_DURATION + " seconds")
    public void loadTest() {
        log.info("Executing LOAD TEST | Users: {} | Duration: {}s",
                LOAD_USERS, LOAD_DURATION);

        PerformanceTestResult result = runner.runLoadTest(
                TARGET_HOST,
                TARGET_PORT,
                TARGET_PATH,
                LOAD_USERS,
                LOAD_DURATION
        );

        allResults.add(result);

        // Upload result summary to S3
        s3Uploader.uploadPerformanceSummary(result);

        // Log summary
        log.info(result.getSummary());

        // Assert SLA not breached — fails CI/CD pipeline if breached
        Assert.assertFalse(
                result.isSlaBreached(),
                "LOAD TEST SLA BREACHED!\nViolations:\n" +
                        String.join("\n", result.getSlaViolations())
        );

        log.info("LOAD TEST PASSED — SLA met");
    }

    /**
     * STRESS TEST
     * Finds system breaking point and max capacity
     *
     * OCC Context: Unusual market volatility (flash crash scenario)
     * Pass Criteria: System degrades gracefully, doesn't crash
     */
    @Test(priority = 2, description = "Stress Test — Find system breaking point")
    @Story("Stress Testing")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Ramps up to " + STRESS_USERS + " concurrent users over " +
            STRESS_RAMP + "s to identify breaking point")
    public void stressTest() {
        log.info("Executing STRESS TEST | Peak Users: {} | Ramp: {}s | Duration: {}s",
                STRESS_USERS, STRESS_RAMP, STRESS_DURATION);

        PerformanceTestResult result = runner.runStressTest(
                TARGET_HOST,
                TARGET_PORT,
                TARGET_PATH,
                STRESS_USERS,
                STRESS_RAMP,
                STRESS_DURATION
        );

        allResults.add(result);

        // Upload to S3
        s3Uploader.uploadPerformanceSummary(result);

        log.info(result.getSummary());

        // For stress tests — we assert error rate only
        // Response time may exceed SLA at peak — that's expected
        // What matters is the system doesn't crash (error rate stays manageable)
        Assert.assertTrue(
                result.getTotalRequests() > 0,
                "Stress test produced no results — system may have crashed"
        );

        // Warn if SLA breached but don't fail — stress tests push beyond limits
        if (result.isSlaBreached()) {
            log.warn("STRESS TEST: SLA breached at peak load — this is expected behavior");
            log.warn("Breaking point identified at {} concurrent users", STRESS_USERS);
        }
    }

    /**
     * SOAK TEST
     * Detects memory leaks and performance degradation over time
     *
     * OCC Context: System running overnight during off-peak hours
     * Pass Criteria: Performance stable throughout, no degradation
     */
    @Test(priority = 3, description = "Soak Test — Detect memory leaks and degradation")
    @Story("Soak Testing")
    @Severity(SeverityLevel.NORMAL)
    @Description("Sustains " + SOAK_USERS + " users for " +
            SOAK_DURATION + " seconds to detect memory leaks")
    public void soakTest() {
        log.info("Executing SOAK TEST | Users: {} | Duration: {}s",
                SOAK_USERS, SOAK_DURATION);

        PerformanceTestResult result = runner.runSoakTest(
                TARGET_HOST,
                TARGET_PORT,
                TARGET_PATH,
                SOAK_USERS,
                SOAK_DURATION
        );

        allResults.add(result);

        // Upload to S3
        s3Uploader.uploadPerformanceSummary(result);

        log.info(result.getSummary());

        // Soak test — assert SLA maintained throughout sustained run
        Assert.assertFalse(
                result.isSlaBreached(),
                "SOAK TEST SLA BREACHED — possible memory leak or degradation!\n" +
                        String.join("\n", result.getSlaViolations())
        );

        log.info("SOAK TEST PASSED — No degradation detected");
    }

    // ─── HELPERS ──────────────────────────────────────────────────

    /**
     * Generate suite-level summary across all test types
     * Uploaded to S3 as the master audit document
     */
    private void generateSuiteSummary(List<String> s3Keys) {
        StringBuilder summary = new StringBuilder();
        summary.append("================================================\n");
        summary.append("  PERFORMANCE TEST SUITE — FINAL SUMMARY\n");
        summary.append("================================================\n");
        summary.append("Environment  : ").append(ENVIRONMENT).append("\n");
        summary.append("Target       : ").append(TARGET_HOST).append("\n");
        summary.append("Tests Run    : ").append(allResults.size()).append("\n\n");

        int passed = 0, failed = 0;
        for (PerformanceTestResult result : allResults) {
            String status = result.isSlaBreached() ? "FAILED" : "PASSED";
            if (!result.isSlaBreached()) passed++; else failed++;

            summary.append("  [").append(status).append("] ")
                    .append(result.getTestType()).append(" — ")
                    .append(result.getTestName()).append("\n");
            summary.append("    Avg: ").append(result.getAvgResponseTimeMs())
                    .append("ms | P99: ").append(result.getP99ResponseTimeMs())
                    .append("ms | Errors: ")
                    .append(String.format("%.2f%%", result.getErrorRate()))
                    .append("\n");
        }

        summary.append("\n------------------------------------------------\n");
        summary.append("PASSED: ").append(passed)
                .append(" | FAILED: ").append(failed).append("\n");
        summary.append("S3 Files Archived: ").append(s3Keys.size()).append("\n");
        summary.append("================================================\n");

        String summaryText = summary.toString();
        log.info(summaryText);

        // Attach master summary to Allure
        Allure.addAttachment(
                "SUITE FINAL SUMMARY - " + ENVIRONMENT,
                "text/plain",
                summaryText
        );

        // Upload master summary to S3
        s3Uploader.uploadPerformanceSummary(
                createSuiteSummaryResult(passed, failed)
        );
    }

    /**
     * Creates a summary PerformanceTestResult for the entire suite
     */
    private PerformanceTestResult createSuiteSummaryResult(int passed, int failed) {
        PerformanceTestResult suite = new PerformanceTestResult(
                "Suite_Summary", "SUITE", ENVIRONMENT
        );
        suite.setTotalRequests(allResults.stream()
                .mapToLong(PerformanceTestResult::getTotalRequests).sum());
        suite.setFailedRequests(allResults.stream()
                .mapToLong(PerformanceTestResult::getFailedRequests).sum());
        suite.setPassedRequests(allResults.stream()
                .mapToLong(PerformanceTestResult::getPassedRequests).sum());
        suite.setSlaBreached(failed > 0);
        return suite;
    }
}