package com.sdet.performance;

import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * SLAValidator - Validates performance test results against defined SLA thresholds
 * OCC Relevance: SLA validation for cloud vs legacy DB2 systems
 */
public class SLAValidator {

    private static final Logger log = LogManager.getLogger(SLAValidator.class);

    // SLA Thresholds - configurable per environment
    private final long maxResponseTimeMs;
    private final double maxErrorRatePercent;
    private final long maxAvgResponseTimeMs;

    private final List<String> violations = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    // Default SLA thresholds (OCC financial systems)
    public SLAValidator() {
        this.maxResponseTimeMs = 5000;      // 5s max response time
        this.maxErrorRatePercent = 1.0;     // 1% max error rate
        this.maxAvgResponseTimeMs = 2000;   // 2s avg response time
    }

    // Custom SLA thresholds
    public SLAValidator(long maxResponseTimeMs,
                        double maxErrorRatePercent,
                        long maxAvgResponseTimeMs) {
        this.maxResponseTimeMs = maxResponseTimeMs;
        this.maxErrorRatePercent = maxErrorRatePercent;
        this.maxAvgResponseTimeMs = maxAvgResponseTimeMs;
    }

    /**
     * Validate a single response time against SLA
     */
    public boolean validateResponseTime(String endpoint, long responseTimeMs) {
        if (responseTimeMs > maxResponseTimeMs) {
            String violation = String.format(
                    "SLA BREACH: %s responded in %dms (max allowed: %dms)",
                    endpoint, responseTimeMs, maxResponseTimeMs
            );
            violations.add(violation);
            log.error(violation);
            return false;
        }

        if (responseTimeMs > maxAvgResponseTimeMs) {
            String warning = String.format(
                    "SLA WARNING: %s responded in %dms (avg threshold: %dms)",
                    endpoint, responseTimeMs, maxAvgResponseTimeMs
            );
            warnings.add(warning);
            log.warn(warning);
        }

        log.info("SLA PASS: {} responded in {}ms", endpoint, responseTimeMs);
        return true;
    }

    /**
     * Validate error rate across all requests
     */
    public boolean validateErrorRate(int totalRequests, int failedRequests) {
        if (totalRequests == 0) return true;

        double errorRate = ((double) failedRequests / totalRequests) * 100;

        if (errorRate > maxErrorRatePercent) {
            String violation = String.format(
                    "SLA BREACH: Error rate %.2f%% exceeds threshold %.2f%%",
                    errorRate, maxErrorRatePercent
            );
            violations.add(violation);
            log.error(violation);
            return false;
        }

        log.info("SLA PASS: Error rate {:.2f}%", errorRate);
        return true;
    }

    /**
     * Validate average response time
     */
    public boolean validateAverageResponseTime(String testName,
                                               long avgResponseTimeMs) {
        if (avgResponseTimeMs > maxAvgResponseTimeMs) {
            String violation = String.format(
                    "SLA BREACH: %s avg response %dms exceeds threshold %dms",
                    testName, avgResponseTimeMs, maxAvgResponseTimeMs
            );
            violations.add(violation);
            log.error(violation);
            return false;
        }

        log.info("SLA PASS: {} avg response {}ms", testName, avgResponseTimeMs);
        return true;
    }

    /**
     * Generate full SLA report and attach to Allure
     */
    public SLAReport generateReport(String testSuiteName) {
        SLAReport report = new SLAReport(
                testSuiteName,
                violations,
                warnings,
                violations.isEmpty()
        );

        // Attach to Allure for audit evidence (OCC requirement)
        String reportContent = formatReportContent(report);
        Allure.addAttachment("SLA Validation Report - " + testSuiteName,
                "text/plain", reportContent);

        log.info("SLA Report generated for: {} | Status: {}",
                testSuiteName, report.isPassed() ? "PASSED" : "FAILED");

        return report;
    }

    private String formatReportContent(SLAReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SLA VALIDATION REPORT ===\n");
        sb.append("Test Suite: ").append(report.getTestSuiteName()).append("\n");
        sb.append("Status: ").append(report.isPassed() ? "✅ PASSED" : "❌ FAILED").append("\n\n");

        if (!report.getViolations().isEmpty()) {
            sb.append("VIOLATIONS:\n");
            report.getViolations().forEach(v -> sb.append("  ❌ ").append(v).append("\n"));
        }

        if (!report.getWarnings().isEmpty()) {
            sb.append("\nWARNINGS:\n");
            report.getWarnings().forEach(w -> sb.append("  ⚠️  ").append(w).append("\n"));
        }

        sb.append("\nThresholds:\n");
        sb.append("  Max Response Time: ").append(maxResponseTimeMs).append("ms\n");
        sb.append("  Max Avg Response Time: ").append(maxAvgResponseTimeMs).append("ms\n");
        sb.append("  Max Error Rate: ").append(maxErrorRatePercent).append("%\n");

        return sb.toString();
    }

    // Getters
    public List<String> getViolations() { return violations; }
    public List<String> getWarnings() { return warnings; }
    public boolean hasPassed() { return violations.isEmpty(); }

    /**
     * SLA Report data class
     */
    public static class SLAReport {
        private final String testSuiteName;
        private final List<String> violations;
        private final List<String> warnings;
        private final boolean passed;

        public SLAReport(String testSuiteName, List<String> violations,
                         List<String> warnings, boolean passed) {
            this.testSuiteName = testSuiteName;
            this.violations = violations;
            this.warnings = warnings;
            this.passed = passed;
        }

        public String getTestSuiteName() { return testSuiteName; }
        public List<String> getViolations() { return violations; }
        public List<String> getWarnings() { return warnings; }
        public boolean isPassed() { return passed; }
    }
}