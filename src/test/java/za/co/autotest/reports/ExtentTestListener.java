
package za.co.autotest.reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ExtentTestListener implements ITestListener {

    private static final ExtentReports extentReports =
            ExtentReportManager.getInstance();

    private static final ConcurrentMap<Long, ExtentTest> testMap =
            new ConcurrentHashMap<>();

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(
                    () -> new SimpleDateFormat("dd MMM yyyy HH:mm:ss")
            );

    private static final ConcurrentMap<String, RunStats> RUNS =
            new ConcurrentHashMap<>();

    private static final AtomicInteger ACTIVE_RUNS =
            new AtomicInteger(0);

    private static final AtomicBoolean EXECUTION_STARTED =
            new AtomicBoolean(false);

    private static final AtomicBoolean HOOK_REGISTERED =
            new AtomicBoolean(false);

    private static volatile String performanceExecutionId;

    private static final Path DEBUG_FILE = Paths.get(
            System.getProperty("user.dir"),
            "test-reports",
            "history",
            "listener-debug.log"
    );

    static {
        debug("Listener class loaded. user.dir="
                + System.getProperty("user.dir"));
    }

    private static final class RunStats {

        final String name;

        volatile String executionId;

        final long startTime = System.currentTimeMillis();

        final AtomicInteger passed = new AtomicInteger();

        final AtomicInteger failed = new AtomicInteger();

        final AtomicInteger skipped = new AtomicInteger();

        final AtomicBoolean saved = new AtomicBoolean(false);

        RunStats(String name) {
            this.name = name;
        }
    }

    // =========================================================
    // TEST SUITE START
    // =========================================================

    @Override
    public synchronized void onStart(ITestContext context) {

        String suiteName = context.getSuite().getName();

        String runName = suiteName + " / " + context.getName();

        RunStats runStats = new RunStats(runName);

        RUNS.put(runName, runStats);

        ACTIVE_RUNS.incrementAndGet();

        registerShutdownHook();

        debug("onStart: " + runName);

        if (EXECUTION_STARTED.compareAndSet(false, true)) {

            LiveExecutionTracker.startExecution(suiteName);

            performanceExecutionId =
                    PerformanceHistoryManager.startExecution();

            System.out.println(
                    "[AUTOTEST] Performance tracking started."
            );

            System.out.println(
                    "[AUTOTEST] Execution ID: "
                    + performanceExecutionId
            );
        }

        runStats.executionId = performanceExecutionId;

        if (runStats.executionId == null || runStats.executionId.isBlank()) {
            throw new IllegalStateException(
                    "The shared execution ID was not initialized."
            );
        }

        debug("Shared execution ID assigned to " + runName + ": " + runStats.executionId);
    }

    // =========================================================
    // INDIVIDUAL TEST START
    // =========================================================

    @Override
    public void onTestStart(ITestResult result) {

        String testName = getFullTestName(result);

        LiveExecutionTracker.testStarted(testName);

        ExtentTest extentTest =
                extentReports.createTest(
                        result.getMethod().getMethodName()
                );

        extentTest.assignCategory(
                result.getTestClass().getName()
        );

        extentTest.info("Test execution started.");

        extentTest.info(
                "Start time: "
                + DATE_FORMAT.get().format(
                        new Date(result.getStartMillis())
                )
        );

        testMap.put(
                Thread.currentThread().getId(),
                extentTest
        );
    }

    // =========================================================
    // TEST SUCCESS
    // =========================================================

    @Override
    public void onTestSuccess(ITestResult result) {

        long duration = getExecutionDuration(result);

        countResult(result, "PASS");

        LiveExecutionTracker.testPassed(
                getFullTestName(result),
                duration
        );

        savePerformanceResult(result, "PASS");

        ExtentTest extentTest = getCurrentTest();

        if (extentTest != null) {

            extentTest.log(
                    Status.PASS,
                    "Test passed successfully."
            );

            recordExecutionDetails(extentTest, result);
        }

        removeCurrentTest();
    }

    // =========================================================
    // TEST FAILURE
    // =========================================================

    @Override
    public void onTestFailure(ITestResult result) {

        long duration = getExecutionDuration(result);

        countResult(result, "FAIL");

        String reason = result.getThrowable() == null
                ? "Test failed."
                : result.getThrowable().getMessage();

        LiveExecutionTracker.testFailed(
                getFullTestName(result),
                reason,
                duration
        );

        savePerformanceResult(result, "FAIL");

        ExtentTest extentTest = getCurrentTest();

        if (extentTest != null) {

            extentTest.log(
                    Status.FAIL,
                    "Test failed."
            );

            if (result.getThrowable() != null) {
                extentTest.fail(result.getThrowable());
            }

            recordExecutionDetails(extentTest, result);
        }

        removeCurrentTest();
    }

    // =========================================================
    // TEST SKIPPED
    // =========================================================

    @Override
    public void onTestSkipped(ITestResult result) {

        long duration = getExecutionDuration(result);

        countResult(result, "SKIP");

        LiveExecutionTracker.testSkipped(
                getFullTestName(result),
                duration
        );

        savePerformanceResult(result, "SKIP");

        ExtentTest extentTest = getCurrentTest();

        if (extentTest != null) {

            extentTest.log(
                    Status.SKIP,
                    "Test was skipped."
            );

            if (result.getThrowable() != null) {
                extentTest.skip(result.getThrowable());
            }

            recordExecutionDetails(extentTest, result);
        }

        removeCurrentTest();
    }

    // =========================================================
    // SAVE INDIVIDUAL TEST PERFORMANCE
    // =========================================================

    private void savePerformanceResult(
            ITestResult result,
            String status) {

        try {

            String executionId = performanceExecutionId;

            if (executionId == null || executionId.isBlank()) {

                System.err.println(
                        "[AUTOTEST] Performance record skipped: "
                        + "execution ID is missing."
                );

                return;
            }

            PerformanceHistoryManager.saveTestResult(
                    executionId,
                    result.getTestContext().getSuite().getName(),
                    result.getTestClass().getName(),
                    result.getMethod().getMethodName(),
                    status,
                    result.getStartMillis(),
                    result.getEndMillis()
            );

        } catch (Throwable error) {

            debug("savePerformanceResult failed: " + error);
        }
    }

    private void recordExecutionDetails(
            ExtentTest extentTest,
            ITestResult result) {

        extentTest.info(
                "Completion time: "
                + DATE_FORMAT.get().format(
                        new Date(result.getEndMillis())
                )
        );

        extentTest.info(
                "Execution duration: "
                + getExecutionDuration(result)
                + " ms"
        );
    }

    private static long getExecutionDuration(ITestResult result) {

        return Math.max(
                0,
                result.getEndMillis() - result.getStartMillis()
        );
    }

    private static String getFullTestName(ITestResult result) {

        return result.getTestClass().getName()
                + "."
                + result.getMethod().getMethodName();
    }

    // =========================================================
    // COUNT TEST RESULTS
    // =========================================================

    private static void countResult(
            ITestResult result,
            String status) {

        String runName =
                result.getTestContext().getSuite().getName()
                + " / "
                + result.getTestContext().getName();

        RunStats stats = RUNS.get(runName);

        if (stats == null) {

            debug("WARNING: RunStats not found for " + runName);

            return;
        }

        switch (status) {

            case "PASS":
                stats.passed.incrementAndGet();
                break;

            case "FAIL":
                stats.failed.incrementAndGet();
                break;

            default:
                stats.skipped.incrementAndGet();
                break;
        }
    }

    // =========================================================
    // ATTACH FAILURE SCREENSHOT
    // =========================================================

    public static void attachScreenshot(String screenshotPath) {

        ExtentTest extentTest = getCurrentTest();

        if (extentTest != null && screenshotPath != null) {

            try {

                extentTest.addScreenCaptureFromPath(
                        screenshotPath,
                        "Browser failure screenshot"
                );

                extentTest.info(
                        "Failure screenshot attached successfully."
                );

            } catch (Exception exception) {

                extentTest.warning(
                        "Could not attach screenshot: "
                        + exception.getMessage()
                );
            }
        }
    }

    // =========================================================
    // TEST SUITE FINISH - SAVE EXECUTION HISTORY
    // =========================================================

    @Override
    public void onFinish(ITestContext context) {

        String runName =
                context.getSuite().getName()
                + " / "
                + context.getName();

        System.out.println(
                "[AUTOTEST] TestNG onFinish() invoked."
        );

        debug("onFinish invoked: " + runName);

        try {

            int passed = context.getPassedTests().size();

            int failed = context.getFailedTests().size();

            int skipped = context.getSkippedTests().size();

            int total = passed + failed + skipped;

            long startTime = context.getStartDate() != null
                    ? context.getStartDate().getTime()
                    : System.currentTimeMillis();

            long endTime = context.getEndDate() != null
                    ? context.getEndDate().getTime()
                    : System.currentTimeMillis();

            if (endTime < startTime) {
                endTime = System.currentTimeMillis();
            }

            RunStats stats = RUNS.get(runName);

            System.out.println(
                    "[AUTOTEST] Calling ExecutionHistoryManager."
            );

            System.out.println(
                    "[AUTOTEST] Suite: " + runName
            );

            System.out.println(
                    "[AUTOTEST] Total: " + total
                    + " | Passed: " + passed
                    + " | Failed: " + failed
                    + " | Skipped: " + skipped
            );

            String executionId = stats != null
                    ? stats.executionId
                    : performanceExecutionId;

            if (executionId == null || executionId.isBlank()) {
                throw new IllegalStateException(
                        "Cannot save execution history without a shared execution ID."
                );
            }

            if (stats == null
                    || stats.saved.compareAndSet(false, true)) {

                ExecutionHistoryManager.saveExecution(
                        executionId,
                        runName,
                        total,
                        passed,
                        failed,
                        skipped,
                        startTime,
                        endTime
                );

                System.out.println(
                        "[AUTOTEST] Execution history recording completed."
                );

                debug(
                        "History saved for "
                        + runName
                        + " total="
                        + total
                );

            } else {

                debug(
                        "History already saved for "
                        + runName
                );
            }

        } catch (Throwable error) {

            System.err.println(
                    "[AUTOTEST] HISTORY RECORD ERROR: " + error
            );

            error.printStackTrace();

            debug(
                    "HISTORY RECORD ERROR: " + error
            );

        } finally {

            int remainingRuns = ACTIVE_RUNS.decrementAndGet();

            if (remainingRuns <= 0) {

                ACTIVE_RUNS.set(0);

                finishRun();
            }

            System.out.flush();

            System.err.flush();
        }
    }

    // =========================================================
    // SHUTDOWN HOOK - BACKUP HISTORY SAVING
    // =========================================================

    private static void registerShutdownHook() {

        if (!HOOK_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {

                    for (RunStats stats : RUNS.values()) {

                        if (stats.saved.compareAndSet(false, true)) {

                            int passed = stats.passed.get();

                            int failed = stats.failed.get();

                            int skipped = stats.skipped.get();

                            try {

                                debug(
                                        "Shutdown hook saving unsaved run: "
                                        + stats.name
                                );

                                ExecutionHistoryManager.saveExecution(
                                        stats.executionId,
                                        stats.name,
                                        passed + failed + skipped,
                                        passed,
                                        failed,
                                        skipped,
                                        stats.startTime,
                                        System.currentTimeMillis()
                                );

                            } catch (Throwable error) {

                                debug(
                                        "Shutdown hook save failed: "
                                        + error
                                );
                            }
                        }
                    }

                    try {

                        extentReports.flush();

                    } catch (Throwable error) {

                        debug(
                                "Shutdown hook report flush failed: "
                                + error
                        );
                    }

                }, "autotest-history-shutdown")
        );
    }

    // =========================================================
    // FINISH EXECUTION
    // =========================================================

    private static void finishRun() {

        try {

            LiveExecutionTracker.finishExecution();

        } catch (Throwable error) {

            System.err.println(
                    "[AUTOTEST] Live tracking cleanup error: "
                    + error
            );
        }

        try {

            extentReports.flush();

        } catch (Throwable error) {

            System.err.println(
                    "[AUTOTEST] ExtentReports flush error: "
                    + error
            );
        }

        testMap.clear();

        EXECUTION_STARTED.set(false);

        performanceExecutionId = null;

        System.out.println(
                "[AUTOTEST] Performance history location: "
                + PerformanceHistoryManager.getPerformanceFile()
        );

        System.out.println(
                "[AUTOTEST] Test execution completed."
        );
    }

    // =========================================================
    // DEBUG FILE
    // =========================================================

    private static void debug(String message) {

        try {

            Files.createDirectories(
                    DEBUG_FILE.getParent()
            );

            Files.write(
                    DEBUG_FILE,
                    (
                            new Date()
                            + "  "
                            + message
                            + System.lineSeparator()
                    ).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (IOException ignored) {

            // Debug logging must never interrupt test execution.
        }
    }

    // =========================================================
    // CURRENT TEST HELPERS
    // =========================================================

    private static ExtentTest getCurrentTest() {

        return testMap.get(
                Thread.currentThread().getId()
        );
    }

    private static void removeCurrentTest() {

        testMap.remove(
                Thread.currentThread().getId()
        );
    }
}
