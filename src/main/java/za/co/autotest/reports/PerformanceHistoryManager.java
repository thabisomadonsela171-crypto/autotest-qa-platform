package za.co.autotest.reports;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores individual test execution performance history.
 */
public final class PerformanceHistoryManager {

    private static final Path PERFORMANCE_FILE =
            Paths.get(
                    "test-reports",
                    "history",
                    "performance-history.csv"
            );

    private static final String HEADER =
            "Execution ID,Suite Name,Test Class,Test Name,"
            + "Status,Start Time,End Time,Duration (ms)";

    private PerformanceHistoryManager() {
        // Prevent object creation.
    }

    public static String startExecution() {
        return UUID.randomUUID().toString();
    }

    public static synchronized void saveTestResult(
            String executionId,
            String suiteName,
            String testClass,
            String testName,
            String status,
            long startTime,
            long endTime) {

        try {
            Files.createDirectories(PERFORMANCE_FILE.getParent());

            boolean fileExists =
                    Files.exists(PERFORMANCE_FILE)
                    && Files.size(PERFORMANCE_FILE) > 0;

            if (!fileExists) {
                Files.writeString(
                        PERFORMANCE_FILE,
                        HEADER + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            }

            long duration = Math.max(0, endTime - startTime);

            String record = String.join(",",
                    escapeCsv(executionId),
                    escapeCsv(suiteName),
                    escapeCsv(testClass),
                    escapeCsv(testName),
                    escapeCsv(status),
                    escapeCsv(formatTimestamp(startTime)),
                    escapeCsv(formatTimestamp(endTime)),
                    String.valueOf(duration)
            );

            Files.writeString(
                    PERFORMANCE_FILE,
                    record + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            System.out.println(
                    "[AUTOTEST] Performance record saved: "
                    + testName + " | " + duration + " ms"
            );

        } catch (IOException exception) {
            System.err.println(
                    "[AUTOTEST] Could not save performance record: "
                    + exception.getMessage()
            );
        }
    }

    private static String formatTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }

        return Instant.ofEpochMilli(timestamp).toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            value = "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public static Path getPerformanceFile() {
        return PERFORMANCE_FILE;
    }
}
