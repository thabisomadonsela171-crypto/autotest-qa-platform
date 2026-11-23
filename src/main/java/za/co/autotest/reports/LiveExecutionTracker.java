
package za.co.autotest.reports;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class LiveExecutionTracker {

    private static final Path STATE_FILE = Paths.get(
            "test-reports",
            "live-execution.properties"
    ).toAbsolutePath();

    private static final Path ACTIVITY_FILE = Paths.get(
            "test-reports",
            "live-execution-activity.jsonl"
    ).toAbsolutePath();

    private static int total;
    private static int passed;
    private static int failed;
    private static int skipped;

    private static String suiteName = "AutoTest QA Platform";
    private static String currentTest = "Waiting for tests...";
    private static String status = "IDLE";

    private static long startTime;
    private static long endTime;

    static {
        loadPreviousExecution();
    }

    private LiveExecutionTracker() {
    }

    public static synchronized void startExecution(String name) {

        total = 0;
        passed = 0;
        failed = 0;
        skipped = 0;

        suiteName = (name == null || name.isBlank())
                ? "AutoTest QA Platform"
                : name;

        currentTest = "Preparing tests...";
        status = "RUNNING";

        startTime = System.currentTimeMillis();
        endTime = 0;

        clearActivityFile();

        recordActivity(
                "EXECUTION_STARTED",
                suiteName,
                "Test execution started.",
                0
        );

        persistSnapshot();

        System.out.println("[AUTOTEST] Execution started: " + suiteName);
    }

    public static synchronized void testStarted(String testName) {

        if (!"RUNNING".equals(status)) {
            return;
        }

        total++;

        currentTest = (testName == null || testName.isBlank())
                ? "Unknown test"
                : testName;

        recordActivity(
                "TEST_STARTED",
                currentTest,
                "Test execution started.",
                0
        );

        persistSnapshot();

        System.out.println("[AUTOTEST] Test started: " + currentTest);
    }

    public static synchronized void testPassed(
            String testName,
            long duration
    ) {

        if (!"RUNNING".equals(status)) {
            return;
        }

        passed++;

        recordActivity(
                "TEST_PASSED",
                testName,
                "Test passed successfully.",
                duration
        );

        persistSnapshot();

        System.out.println("[AUTOTEST] Test passed: " + testName);
    }

    public static synchronized void testFailed(
            String testName,
            String reason,
            long duration
    ) {

        if (!"RUNNING".equals(status)) {
            return;
        }

        failed++;

        recordActivity(
                "TEST_FAILED",
                testName,
                reason == null ? "Test failed." : reason,
                duration
        );

        persistSnapshot();

        System.out.println("[AUTOTEST] Test failed: " + testName);
    }

    public static synchronized void testSkipped(
            String testName,
            long duration
    ) {

        if (!"RUNNING".equals(status)) {
            return;
        }

        skipped++;

        recordActivity(
                "TEST_SKIPPED",
                testName,
                "Test was skipped.",
                duration
        );

        persistSnapshot();

        System.out.println("[AUTOTEST] Test skipped: " + testName);
    }

    public static synchronized void finishExecution() {

        if (!"RUNNING".equals(status)) {
            return;
        }

        endTime = System.currentTimeMillis();

        currentTest = "Execution finished";
        status = "COMPLETED";

        recordActivity(
                "EXECUTION_COMPLETED",
                suiteName,
                "Execution completed successfully.",
                endTime - startTime
        );

        persistSnapshot();

        System.out.println("[AUTOTEST] Execution completed.");
    }

    public static synchronized void failExecution() {

        endTime = System.currentTimeMillis();

        currentTest = "Execution failed";
        status = "FAILED";

        recordActivity(
                "EXECUTION_FAILED",
                suiteName,
                "Execution failed.",
                endTime - startTime
        );

        persistSnapshot();

        System.out.println("[AUTOTEST] Execution failed.");
    }

    public static synchronized Map<String, Object> getSnapshot() {

        Map<String, Object> data = new LinkedHashMap<>();

        data.put("suiteName", suiteName);
        data.put("status", status);
        data.put("currentTest", currentTest);

        data.put("total", total);
        data.put("passed", passed);
        data.put("failed", failed);
        data.put("skipped", skipped);

        data.put("startTime", startTime);
        data.put("endTime", endTime);

        long duration;

        if (endTime > 0) {
            duration = endTime - startTime;
        } else if (startTime > 0) {
            duration = System.currentTimeMillis() - startTime;
        } else {
            duration = 0;
        }

        data.put("duration", Math.max(duration, 0));

        return data;
    }

    /**
     * Provides the activity file location to the dashboard server.
     */
    public static Path getActivityFile() {
        return ACTIVITY_FILE;
    }

    /**
     * Clears activity events before a new execution.
     */
    private static synchronized void clearActivityFile() {

        try {

            Files.createDirectories(ACTIVITY_FILE.getParent());

            Files.writeString(
                    ACTIVITY_FILE,
                    "",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

        } catch (IOException exception) {

            System.err.println(
                    "[AUTOTEST] Could not clear activity file: "
                            + exception.getMessage()
            );
        }
    }

    /**
     * Appends one JSON event per line.
     */
    private static synchronized void recordActivity(
            String event,
            String testName,
            String message,
            long duration
    ) {

        String json = "{"
                + "\"timestamp\":\"" + jsonEscape(Instant.now().toString()) + "\","
                + "\"event\":\"" + jsonEscape(event) + "\","
                + "\"testName\":\"" + jsonEscape(testName) + "\","
                + "\"message\":\"" + jsonEscape(message) + "\","
                + "\"duration\":" + Math.max(duration, 0)
                + "}"
                + System.lineSeparator();

        try {

            Files.createDirectories(ACTIVITY_FILE.getParent());

            Files.writeString(
                    ACTIVITY_FILE,
                    json,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (IOException exception) {

            System.err.println(
                    "[AUTOTEST] Could not record activity: "
                            + exception.getMessage()
            );
        }
    }

    private static String jsonEscape(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static synchronized void loadPreviousExecution() {

        if (!Files.exists(STATE_FILE)) {
            return;
        }

        Properties properties = new Properties();

        try (InputStream input = Files.newInputStream(STATE_FILE)) {

            properties.load(input);

            suiteName = properties.getProperty(
                    "suiteName",
                    "AutoTest QA Platform"
            );

            status = properties.getProperty("status", "IDLE");

            currentTest = properties.getProperty(
                    "currentTest",
                    "Waiting for tests..."
            );

            total = getInt(properties, "total");
            passed = getInt(properties, "passed");
            failed = getInt(properties, "failed");
            skipped = getInt(properties, "skipped");

            startTime = getLong(properties, "startTime");
            endTime = getLong(properties, "endTime");

            System.out.println(
                    "[AUTOTEST] Previous execution restored: " + status
            );

        } catch (IOException | NumberFormatException exception) {

            System.err.println(
                    "[AUTOTEST] Could not restore previous execution: "
                            + exception.getMessage()
            );
        }
    }

    private static int getInt(Properties properties, String key) {

        return Integer.parseInt(
                properties.getProperty(key, "0")
        );
    }

    private static long getLong(Properties properties, String key) {

        return Long.parseLong(
                properties.getProperty(key, "0")
        );
    }

    private static synchronized void persistSnapshot() {

        Properties properties = new Properties();

        Map<String, Object> snapshot = getSnapshot();

        snapshot.forEach((key, value) ->
                properties.setProperty(
                        key,
                        String.valueOf(value)
                )
        );

        Path temporaryFile = STATE_FILE.resolveSibling(
                "live-execution.properties.tmp"
        );

        try {

            Files.createDirectories(STATE_FILE.getParent());

            try (OutputStream output = Files.newOutputStream(temporaryFile)) {

                properties.store(
                        output,
                        "AutoTest QA Platform Live Execution State"
                );
            }

            try {

                Files.move(
                        temporaryFile,
                        STATE_FILE,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (IOException exception) {

                Files.move(
                        temporaryFile,
                        STATE_FILE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } catch (IOException exception) {

            System.err.println(
                    "[AUTOTEST] Could not update live execution state: "
                            + exception.getMessage()
            );
        }
    }
}
