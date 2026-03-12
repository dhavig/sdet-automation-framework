package com.sdet.performance;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * PerformanceTestResult - Standardized data model for all performance test results
 * Holds metrics from JMeter execution for SLA validation + S3 archival
 * OCC Relevance: Audit-ready, structured test evidence
 */
public class PerformanceTestResult {

    // Test metadata
    private String testName;
    private String testType;        // LOAD | STRESS | SOAK
    private String environment;     // CLOUD | LEGACY | LOCAL
    private String timestamp;

    // Core performance metrics
    private long totalRequests;
    private long failedRequests;
    private long passedRequests;

    private long minResponseTimeMs;
    private long maxResponseTimeMs;
    private long avgResponseTimeMs;
    private long p90ResponseTimeMs;  // 90th percentile
    private long p95ResponseTimeMs;  // 95th percentile
    private long p99ResponseTimeMs;  // 99th percentile

    // Throughput
    private double requestsPerSecond;
    private double throughputMbps;

    // SLA
    private boolean slaBreached;
    private List<String> slaViolations;
    private List<String> slaWarnings;

    // Test duration
    private long testDurationSeconds;

    // Constructor
    public PerformanceTestResult(String testName,
                                 String testType,
                                 String environment) {
        this.testName = testName;
        this.testType = testType;
        this.environment = environment;
        this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.slaViolations = new ArrayList<>();
        this.slaWarnings = new ArrayList<>();
        this.slaBreached = false;
    }

    /**
     * Summary for logging + Allure report
     * OCC needs audit-ready documentation
     */
    public String getSummary() {
        return String.format(
                """
                ====================================
                PERFORMANCE TEST RESULT SUMMARY
                ====================================
                Test Name    : %s
                Test Type    : %s
                Environment  : %s
                Timestamp    : %s
                Duration     : %ds
                ------------------------------------
                REQUESTS
                Total        : %d
                Passed       : %d
                Failed       : %d
                Error Rate   : %.2f%%
                ------------------------------------
                RESPONSE TIMES
                Min          : %dms
                Avg          : %dms
                Max          : %dms
                P90          : %dms
                P95          : %dms
                P99          : %dms
                ------------------------------------
                THROUGHPUT
                Req/sec      : %.2f
                Throughput   : %.2f Mbps
                ------------------------------------
                SLA STATUS   : %s
                ====================================
                """,
                testName, testType, environment, timestamp,
                testDurationSeconds,
                totalRequests, passedRequests, failedRequests,
                getErrorRate(),
                minResponseTimeMs, avgResponseTimeMs, maxResponseTimeMs,
                p90ResponseTimeMs, p95ResponseTimeMs, p99ResponseTimeMs,
                requestsPerSecond, throughputMbps,
                slaBreached ? "❌ BREACHED" : "✅ PASSED"
        );
    }

    /**
     * Calculate error rate percentage
     */
    public double getErrorRate() {
        if (totalRequests == 0) return 0.0;
        return ((double) failedRequests / totalRequests) * 100;
    }

    // ─── Getters & Setters ───────────────────────────────────────

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public String getTestType() { return testType; }
    public void setTestType(String testType) { this.testType = testType; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getTimestamp() { return timestamp; }

    public long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }

    public long getFailedRequests() { return failedRequests; }
    public void setFailedRequests(long failedRequests) { this.failedRequests = failedRequests; }

    public long getPassedRequests() { return passedRequests; }
    public void setPassedRequests(long passedRequests) { this.passedRequests = passedRequests; }

    public long getMinResponseTimeMs() { return minResponseTimeMs; }
    public void setMinResponseTimeMs(long minResponseTimeMs) { this.minResponseTimeMs = minResponseTimeMs; }

    public long getMaxResponseTimeMs() { return maxResponseTimeMs; }
    public void setMaxResponseTimeMs(long maxResponseTimeMs) { this.maxResponseTimeMs = maxResponseTimeMs; }

    public long getAvgResponseTimeMs() { return avgResponseTimeMs; }
    public void setAvgResponseTimeMs(long avgResponseTimeMs) { this.avgResponseTimeMs = avgResponseTimeMs; }

    public long getP90ResponseTimeMs() { return p90ResponseTimeMs; }
    public void setP90ResponseTimeMs(long p90ResponseTimeMs) { this.p90ResponseTimeMs = p90ResponseTimeMs; }

    public long getP95ResponseTimeMs() { return p95ResponseTimeMs; }
    public void setP95ResponseTimeMs(long p95ResponseTimeMs) { this.p95ResponseTimeMs = p95ResponseTimeMs; }

    public long getP99ResponseTimeMs() { return p99ResponseTimeMs; }
    public void setP99ResponseTimeMs(long p99ResponseTimeMs) { this.p99ResponseTimeMs = p99ResponseTimeMs; }

    public double getRequestsPerSecond() { return requestsPerSecond; }
    public void setRequestsPerSecond(double requestsPerSecond) { this.requestsPerSecond = requestsPerSecond; }

    public double getThroughputMbps() { return throughputMbps; }
    public void setThroughputMbps(double throughputMbps) { this.throughputMbps = throughputMbps; }

    public boolean isSlaBreached() { return slaBreached; }
    public void setSlaBreached(boolean slaBreached) { this.slaBreached = slaBreached; }

    public List<String> getSlaViolations() { return slaViolations; }
    public void setSlaViolations(List<String> slaViolations) { this.slaViolations = slaViolations; }

    public List<String> getSlaWarnings() { return slaWarnings; }
    public void setSlaWarnings(List<String> slaWarnings) { this.slaWarnings = slaWarnings; }

    public long getTestDurationSeconds() { return testDurationSeconds; }
    public void setTestDurationSeconds(long testDurationSeconds) { this.testDurationSeconds = testDurationSeconds; }
}