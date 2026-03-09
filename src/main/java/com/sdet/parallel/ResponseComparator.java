package com.sdet.parallel;

import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.List;

public class ResponseComparator {

    private static final Logger log = LogManager.getLogger(ResponseComparator.class);
    private static final long PERFORMANCE_SLA_MS = 5000;

    public static ComparisonReport compare(ParallelTestResult env1Result,
                                           ParallelTestResult env2Result) {
        List<String> differences = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Compare pass/fail status
        if (env1Result.isPassed() != env2Result.isPassed()) {
            differences.add(String.format(
                    "STATUS MISMATCH → %s: %s | %s: %s",
                    env1Result.getEnvironmentName(), env1Result.isPassed() ? "PASS" : "FAIL",
                    env2Result.getEnvironmentName(), env2Result.isPassed() ? "PASS" : "FAIL"
            ));
        }

        // 2. Compare actual output values
        if (env1Result.getActualValue() != null && env2Result.getActualValue() != null) {
            if (!env1Result.getActualValue().equalsIgnoreCase(env2Result.getActualValue())) {
                differences.add(String.format(
                        "OUTPUT MISMATCH → %s: [%s] | %s: [%s]",
                        env1Result.getEnvironmentName(), env1Result.getActualValue(),
                        env2Result.getEnvironmentName(), env2Result.getActualValue()
                ));
            }
        }

        // 3. SLA breach check
        if (env1Result.getExecutionTimeMs() > PERFORMANCE_SLA_MS) {
            warnings.add(String.format("SLA BREACH → %s took %dms (threshold: %dms)",
                    env1Result.getEnvironmentName(), env1Result.getExecutionTimeMs(), PERFORMANCE_SLA_MS));
        }
        if (env2Result.getExecutionTimeMs() > PERFORMANCE_SLA_MS) {
            warnings.add(String.format("SLA BREACH → %s took %dms (threshold: %dms)",
                    env2Result.getEnvironmentName(), env2Result.getExecutionTimeMs(), PERFORMANCE_SLA_MS));
        }

        // 4. Performance delta warning
        long delta = Math.abs(env1Result.getExecutionTimeMs() - env2Result.getExecutionTimeMs());
        if (delta > 2000) {
            warnings.add(String.format("PERFORMANCE DELTA → %dms difference between environments", delta));
        }

        ComparisonReport report = new ComparisonReport(
                env1Result.getTestName(), differences, warnings,
                env1Result.getExecutionTimeMs(), env2Result.getExecutionTimeMs()
        );

        logToAllure(report);
        return report;
    }

    private static void logToAllure(ComparisonReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PARALLEL TEST COMPARISON REPORT ===\n");
        sb.append("Test: ").append(report.getTestName()).append("\n\n");

        if (report.getDifferences().isEmpty()) {
            sb.append("✅ ENVIRONMENTS IN SYNC — No differences found\n");
        } else {
            sb.append("❌ DIFFERENCES DETECTED:\n");
            report.getDifferences().forEach(d -> sb.append("  • ").append(d).append("\n"));
        }

        if (!report.getWarnings().isEmpty()) {
            sb.append("\n⚠️  WARNINGS:\n");
            report.getWarnings().forEach(w -> sb.append("  • ").append(w).append("\n"));
        }

        sb.append("\nExecution Times → ENV1: ")
                .append(report.getEnv1TimeMs()).append("ms | ENV2: ")
                .append(report.getEnv2TimeMs()).append("ms");

        log.info(sb.toString());
        Allure.addAttachment("Parallel Comparison Report", sb.toString());
    }

    public static class ComparisonReport {
        private final String testName;
        private final List<String> differences;
        private final List<String> warnings;
        private final long env1TimeMs;
        private final long env2TimeMs;

        public ComparisonReport(String testName, List<String> differences,
                                List<String> warnings, long env1TimeMs, long env2TimeMs) {
            this.testName = testName;
            this.differences = differences;
            this.warnings = warnings;
            this.env1TimeMs = env1TimeMs;
            this.env2TimeMs = env2TimeMs;
        }

        public String getTestName()          { return testName; }
        public List<String> getDifferences() { return differences; }
        public List<String> getWarnings()    { return warnings; }
        public long getEnv1TimeMs()          { return env1TimeMs; }
        public long getEnv2TimeMs()          { return env2TimeMs; }
        public boolean isInSync()            { return differences.isEmpty(); }
    }
}