package com.sdet.performance;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.gui.ThreadGroupGui;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JMeterTestRunner - Programmatically executes JMeter performance tests
 *
 * OCC Relevance:
 * - Runs load, stress, soak tests via CI/CD (no manual GUI)
 * - Feeds results into SLAValidator automatically
 * - Chains into AWSS3Uploader for audit archival
 * - Attaches evidence to Allure for audit trail
 */
public class JMeterTestRunner {

    private static final Logger log = LogManager.getLogger(JMeterTestRunner.class);

    // JMeter home - set via environment or config
    private static final String JMETER_HOME = System.getProperty(
            "jmeter.home",
            System.getenv().getOrDefault("JMETER_HOME", "/usr/local/jmeter")
    );

    private static final String RESULTS_DIR = "target/jmeter-results/";
    private static final String REPORTS_DIR = "target/jmeter-reports/";

    private final SLAValidator slaValidator;
    private final String environment;

    public JMeterTestRunner(String environment) {
        this.environment = environment;
        this.slaValidator = new SLAValidator();
        initJMeter();
        createOutputDirectories();
    }

    public JMeterTestRunner(String environment,
                            long maxResponseMs,
                            double maxErrorRate,
                            long maxAvgResponseMs) {
        this.environment = environment;
        this.slaValidator = new SLAValidator(
                maxResponseMs, maxErrorRate, maxAvgResponseMs
        );
        initJMeter();
        createOutputDirectories();
    }

    /**
     * Initialize JMeter engine
     */
    private void initJMeter() {
        File jmeterProps = new File(JMETER_HOME + "/bin/jmeter.properties");
        JMeterUtils.setJMeterHome(JMETER_HOME);
        JMeterUtils.loadJMeterProperties(jmeterProps.getPath());
        JMeterUtils.initLocale();
        log.info("JMeter initialized from: {}", JMETER_HOME);
    }

    /**
     * LOAD TEST
     * Simulates expected normal production traffic
     * OCC: Validates system handles normal trading volume
     *
     * @param host       Target host (e.g. api.occ.com)
     * @param port       Target port
     * @param path       API endpoint path
     * @param users      Number of concurrent users (threads)
     * @param durationSec How long to run (seconds)
     */
    @Step("Run Load Test: {host}{path} | Users: {users} | Duration: {durationSec}s")
    public PerformanceTestResult runLoadTest(String host,
                                             int port,
                                             String path,
                                             int users,
                                             int durationSec) {
        log.info("Starting LOAD TEST → {}:{}{} | {} users | {}s",
                host, port, path, users, durationSec);

        return executeTest(
                "Load_Test",
                "LOAD",
                host, port, path,
                users,
                durationSec,
                1  // ramp-up: 1 second (immediate)
        );
    }

    /**
     * STRESS TEST
     * Pushes system beyond normal limits to find breaking point
     * OCC: Identifies max capacity before performance degrades
     *
     * @param peakUsers  Maximum users to ramp up to
     * @param rampUpSec  Time to gradually reach peak users
     */
    @Step("Run Stress Test: {host}{path} | Peak Users: {peakUsers}")
    public PerformanceTestResult runStressTest(String host,
                                               int port,
                                               String path,
                                               int peakUsers,
                                               int rampUpSec,
                                               int durationSec) {
        log.info("Starting STRESS TEST → {}:{}{} | Peak: {} users | Ramp: {}s",
                host, port, path, peakUsers, rampUpSec);

        return executeTest(
                "Stress_Test",
                "STRESS",
                host, port, path,
                peakUsers,
                durationSec,
                rampUpSec
        );
    }

    /**
     * SOAK TEST
     * Sustained load over extended time — finds memory leaks, degradation
     * OCC: Critical for systems running 24/7 trading operations
     *
     * @param users       Steady number of concurrent users
     * @param durationSec Long duration (e.g. 1800 = 30 mins)
     */
    @Step("Run Soak Test: {host}{path} | Users: {users} | Duration: {durationSec}s")
    public PerformanceTestResult runSoakTest(String host,
                                             int port,
                                             String path,
                                             int users,
                                             int durationSec) {
        log.info("Starting SOAK TEST → {}:{}{} | {} users sustained for {}s",
                host, port, path, users, durationSec);

        return executeTest(
                "Soak_Test",
                "SOAK",
                host, port, path,
                users,
                durationSec,
                60  // gradual 60s ramp-up for soak
        );
    }

    /**
     * Core execution method — builds and runs JMeter test plan
     */
    private PerformanceTestResult executeTest(String testName,
                                              String testType,
                                              String host,
                                              int port,
                                              String path,
                                              int users,
                                              int durationSec,
                                              int rampUpSec) {

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String resultsFile = RESULTS_DIR + testName + "_" + timestamp + ".jtl";

        PerformanceTestResult result = new PerformanceTestResult(
                testName, testType, environment
        );

        long startTime = System.currentTimeMillis();

        try {
            // 1. Build the test plan
            HashTree testPlanTree = buildTestPlan(
                    testName, host, port, path,
                    users, durationSec, rampUpSec,
                    resultsFile
            );

            // 2. Run JMeter engine
            StandardJMeterEngine jmeter = new StandardJMeterEngine();
            jmeter.configure(testPlanTree);
            jmeter.run();

            long endTime = System.currentTimeMillis();
            result.setTestDurationSeconds((endTime - startTime) / 1000);

            // 3. Parse results from .jtl file
            parseResults(resultsFile, result);

            // 4. Validate against SLA
            slaValidator.validateResponseTime(
                    host + path, result.getMaxResponseTimeMs()
            );
            slaValidator.validateAverageResponseTime(
                    testName, result.getAvgResponseTimeMs()
            );
            slaValidator.validateErrorRate(
                    (int) result.getTotalRequests(),
                    (int) result.getFailedRequests()
            );

            result.setSlaBreached(!slaValidator.hasPassed());
            result.setSlaViolations(slaValidator.getViolations());
            result.setSlaWarnings(slaValidator.getWarnings());

            // 5. Attach summary to Allure (audit evidence)
            Allure.addAttachment(
                    testName + " Performance Summary",
                    "text/plain",
                    result.getSummary()
            );

            // 6. Generate SLA report
            slaValidator.generateReport(testName);

            log.info("Test completed: {}\n{}", testName, result.getSummary());

        } catch (Exception e) {
            log.error("JMeter test execution failed: {}", e.getMessage(), e);
            result.setSlaBreached(true);
            Allure.addAttachment(
                    testName + " ERROR",
                    "text/plain",
                    "Test failed: " + e.getMessage()
            );
        }

        return result;
    }

    /**
     * Build JMeter test plan programmatically
     * This replaces the need for .jmx files for basic tests
     */
    private HashTree buildTestPlan(String testName,
                                   String host,
                                   int port,
                                   String path,
                                   int users,
                                   int durationSec,
                                   int rampUpSec,
                                   String resultsFile) {
        // Test Plan
        TestPlan testPlan = new TestPlan(testName);
        testPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
        testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
        testPlan.setUserDefinedVariables((Arguments)
                new ArgumentsPanel().createTestElement());

        // Thread Group (Virtual Users)
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName(testName + "_ThreadGroup");
        threadGroup.setNumThreads(users);
        threadGroup.setRampUp(rampUpSec);
        threadGroup.setDuration(durationSec);
        threadGroup.setScheduler(true);
        threadGroup.setProperty(TestElement.TEST_CLASS,
                ThreadGroup.class.getName());
        threadGroup.setProperty(TestElement.GUI_CLASS,
                ThreadGroupGui.class.getName());

        // Loop Controller
        LoopController loopController = new LoopController();
        loopController.setLoops(-1); // infinite during duration
        loopController.setFirst(true);
        loopController.setProperty(TestElement.TEST_CLASS,
                LoopController.class.getName());
        loopController.setProperty(TestElement.GUI_CLASS,
                LoopControlPanel.class.getName());
        loopController.initialize();
        threadGroup.setSamplerController(loopController);

        // HTTP Sampler
        HTTPSamplerProxy httpSampler = new HTTPSamplerProxy();
        httpSampler.setDomain(host);
        httpSampler.setPort(port);
        httpSampler.setPath(path);
        httpSampler.setMethod("GET");
        httpSampler.setName(testName + "_HTTP_Request");
        httpSampler.setProperty(TestElement.TEST_CLASS,
                HTTPSamplerProxy.class.getName());
        httpSampler.setProperty(TestElement.GUI_CLASS,
                HttpTestSampleGui.class.getName());

        // Result Collector (saves .jtl results file)
        Summariser summariser = new Summariser("summary");
        ResultCollector resultCollector = new ResultCollector(summariser);
        resultCollector.setFilename(resultsFile);

        // Assemble tree
        HashTree testPlanTree = new HashTree();
        HashTree threadGroupTree = testPlanTree.add(testPlan, threadGroup);
        threadGroupTree.add(httpSampler);
        testPlanTree.add(testPlan, resultCollector);

        return testPlanTree;
    }

    /**
     * Parse .jtl results file into PerformanceTestResult
     * JTL = JMeter's CSV-based results format
     */
    private void parseResults(String resultsFile,
                              PerformanceTestResult result) {
        try {
            var lines = Files.readAllLines(Paths.get(resultsFile));
            if (lines.size() <= 1) return; // header only

            long total = 0, failed = 0;
            long minMs = Long.MAX_VALUE, maxMs = 0, sumMs = 0;
            var responseTimes = new java.util.ArrayList<Long>();

            // JTL format: timeStamp,elapsed,label,responseCode,
            //             responseMessage,threadName,success,bytes,...
            for (int i = 1; i < lines.size(); i++) {
                String[] cols = lines.get(i).split(",");
                if (cols.length < 7) continue;

                long elapsed = Long.parseLong(cols[1].trim());
                boolean success = Boolean.parseBoolean(cols[6].trim());

                total++;
                if (!success) failed++;

                sumMs += elapsed;
                minMs = Math.min(minMs, elapsed);
                maxMs = Math.max(maxMs, elapsed);
                responseTimes.add(elapsed);
            }

            // Sort for percentile calculation
            responseTimes.sort(Long::compareTo);

            result.setTotalRequests(total);
            result.setFailedRequests(failed);
            result.setPassedRequests(total - failed);
            result.setMinResponseTimeMs(minMs == Long.MAX_VALUE ? 0 : minMs);
            result.setMaxResponseTimeMs(maxMs);
            result.setAvgResponseTimeMs(total > 0 ? sumMs / total : 0);
            result.setP90ResponseTimeMs(percentile(responseTimes, 90));
            result.setP95ResponseTimeMs(percentile(responseTimes, 95));
            result.setP99ResponseTimeMs(percentile(responseTimes, 99));

            // Throughput = requests per second
            if (result.getTestDurationSeconds() > 0) {
                result.setRequestsPerSecond(
                        (double) total / result.getTestDurationSeconds()
                );
            }

            log.info("Parsed {} results from {}", total, resultsFile);

        } catch (Exception e) {
            log.error("Failed to parse results file: {}", e.getMessage());
        }
    }

    /**
     * Calculate percentile from sorted list
     * Used for P90, P95, P99 calculations
     */
    private long percentile(java.util.List<Long> sortedData, int percentile) {
        if (sortedData.isEmpty()) return 0;
        int index = (int) Math.ceil((percentile / 100.0) * sortedData.size()) - 1;
        return sortedData.get(Math.max(0, Math.min(index, sortedData.size() - 1)));
    }

    private void createOutputDirectories() {
        try {
            Files.createDirectories(Paths.get(RESULTS_DIR));
            Files.createDirectories(Paths.get(REPORTS_DIR));
        } catch (Exception e) {
            log.error("Could not create output directories: {}", e.getMessage());
        }
    }
}