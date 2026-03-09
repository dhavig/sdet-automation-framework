package com.sdet.parallel;

import com.sdet.config.EnvironmentConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.concurrent.*;

public class ParallelExecutor {

    private static final Logger log = LogManager.getLogger(ParallelExecutor.class);
    private static final int TIMEOUT_SECONDS = 60;

    public static ResponseComparator.ComparisonReport runInParallel(
            String testName,
            EnvironmentConfig env1,
            EnvironmentConfig env2,
            EnvironmentAction action) {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<ParallelTestResult> future1 = executor.submit(() -> {
            log.info("Starting test on ENV1: {}", env1.getName());
            return action.execute(env1);
        });

        Future<ParallelTestResult> future2 = executor.submit(() -> {
            log.info("Starting test on ENV2: {}", env2.getName());
            return action.execute(env2);
        });

        ParallelTestResult result1 = null;
        ParallelTestResult result2 = null;

        try {
            result1 = future1.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            result2 = future2.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("Parallel execution timed out: " + testName, e);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Parallel execution error: " + testName, e);
        } finally {
            executor.shutdown();
        }

        log.info("ENV1 Result: {}", result1);
        log.info("ENV2 Result: {}", result2);

        return ResponseComparator.compare(result1, result2);
    }

    @FunctionalInterface
    public interface EnvironmentAction {
        ParallelTestResult execute(EnvironmentConfig env) throws Exception;
    }
}