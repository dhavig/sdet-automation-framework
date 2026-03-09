package com.sdet.parallel;

public class ParallelTestResult {

    private final String environmentName;
    private final String testName;
    private final boolean passed;
    private final String actualValue;
    private final long executionTimeMs;
    private final String errorMessage;

    public ParallelTestResult(String environmentName, String testName,
                              boolean passed, String actualValue,
                              long executionTimeMs, String errorMessage) {
        this.environmentName = environmentName;
        this.testName = testName;
        this.passed = passed;
        this.actualValue = actualValue;
        this.executionTimeMs = executionTimeMs;
        this.errorMessage = errorMessage;
    }

    public String getEnvironmentName() { return environmentName; }
    public String getTestName()        { return testName; }
    public boolean isPassed()          { return passed; }
    public String getActualValue()     { return actualValue; }
    public long getExecutionTimeMs()   { return executionTimeMs; }
    public String getErrorMessage()    { return errorMessage; }

    @Override
    public String toString() {
        return String.format("[%s] Test: %s | Passed: %s | Value: %s | Time: %dms %s",
                environmentName, testName, passed, actualValue, executionTimeMs,
                errorMessage != null ? "| Error: " + errorMessage : "");
    }
}