package za.co.autotest.reports;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analyses historical individual test execution performance.
 */
public final class PerformanceAnalyticsEngine {

    private static final Path PERFORMANCE_FILE =
            PerformanceHistoryManager.getPerformanceFile();

    // Tests taking 3 seconds or longer are classified as slow.
    private static final long SLOW_TEST_THRESHOLD_MS = 3000;

    private PerformanceAnalyticsEngine() {
    }

    /**
     * Reads performance history and calculates analytics.
     */
    public static Map<String, Object> analyze() {

        List<Map<String, Object>> records = new ArrayList<>();
        List<Map<String, Object>> slowTests = new ArrayList<>();

        Set<String> executionIds = new HashSet<>();

        int passed = 0;
        int failed = 0;
        int skipped = 0;

        long totalDuration = 0;
        long fastestDuration = Long.MAX_VALUE;
        long slowestDuration = Long.MIN_VALUE;

        Map<String, Object> fastestTest = null;
        Map<String, Object> slowestTest = null;

        try {

            if (!Files.exists(PERFORMANCE_FILE)) {
                return emptyAnalytics();
            }

            List<String> lines = Files.readAllLines(
                    PERFORMANCE_FILE,
                    StandardCharsets.UTF_8
            );

            // Skip the CSV header.
            for (int i = 1; i < lines.size(); i++) {

                String line = lines.get(i);

                if (line == null || line.isBlank()) {
                    continue;
                }

                List<String> fields = parseCsvLine(line);

                if (fields.size() != 8) {
                    continue;
                }

                String executionId = fields.get(0);
                String suiteName = fields.get(1);
                String testClass = fields.get(2);
                String testName = fields.get(3);
                String status = fields.get(4);

                long duration;

                try {
                    duration = Long.parseLong(fields.get(7));
                } catch (NumberFormatException exception) {
                    continue;
                }

                Map<String, Object> record = new LinkedHashMap<>();

                record.put("executionId", executionId);
                record.put("suiteName", suiteName);
                record.put("testClass", testClass);
                record.put("testName", testName);
                record.put("status", status);
                record.put("durationMs", duration);

                records.add(record);

                executionIds.add(executionId);

                totalDuration += duration;

                switch (status.toUpperCase()) {
                    case "PASS" -> passed++;
                    case "FAIL" -> failed++;
                    case "SKIP" -> skipped++;
                    default -> {
                    }
                }

                if (duration < fastestDuration) {
                    fastestDuration = duration;
                    fastestTest = record;
                }

                if (duration > slowestDuration) {
                    slowestDuration = duration;
                    slowestTest = record;
                }

                if (duration >= SLOW_TEST_THRESHOLD_MS) {
                    slowTests.add(record);
                }
            }

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Could not read performance history: "
                            + exception.getMessage(),
                    exception
            );
        }

        int totalTests = records.size();

        double averageDuration = totalTests == 0
                ? 0
                : (double) totalDuration / totalTests;

        double passRate = totalTests == 0
                ? 0
                : (double) passed / totalTests * 100;

        double failureRate = totalTests == 0
                ? 0
                : (double) failed / totalTests * 100;

        Map<String, Object> analytics = new LinkedHashMap<>();

        analytics.put("totalTests", totalTests);
        analytics.put("passed", passed);
        analytics.put("failed", failed);
        analytics.put("skipped", skipped);

        analytics.put("averageDurationMs", averageDuration);
        analytics.put("totalDurationMs", totalDuration);

        analytics.put("passRate", passRate);
        analytics.put("failureRate", failureRate);

        analytics.put("executionCount", executionIds.size());

        analytics.put("fastestTest", fastestTest);
        analytics.put("slowestTest", slowestTest);

        analytics.put("slowTestThresholdMs", SLOW_TEST_THRESHOLD_MS);
        analytics.put("slowTestCount", slowTests.size());
        analytics.put("slowTests", slowTests);

        analytics.put("records", records);

        return analytics;
    }

    /**
     * Returns analytics when no history exists.
     */
    private static Map<String, Object> emptyAnalytics() {

        Map<String, Object> analytics = new LinkedHashMap<>();

        analytics.put("totalTests", 0);
        analytics.put("passed", 0);
        analytics.put("failed", 0);
        analytics.put("skipped", 0);

        analytics.put("averageDurationMs", 0.0);
        analytics.put("totalDurationMs", 0);

        analytics.put("passRate", 0.0);
        analytics.put("failureRate", 0.0);

        analytics.put("executionCount", 0);

        analytics.put("fastestTest", null);
        analytics.put("slowestTest", null);

        analytics.put("slowTestThresholdMs", SLOW_TEST_THRESHOLD_MS);
        analytics.put("slowTestCount", 0);
        analytics.put("slowTests", new ArrayList<>());

        analytics.put("records", new ArrayList<>());

        return analytics;
    }

    /**
     * Parses CSV fields while supporting quoted commas and escaped quotes.
     */
    private static List<String> parseCsvLine(String line) {

        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();

        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {

            char current = line.charAt(i);

            if (current == '"') {

                if (insideQuotes
                        && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {

                    field.append('"');
                    i++;

                } else {
                    insideQuotes = !insideQuotes;
                }

            } else if (current == ',' && !insideQuotes) {

                fields.add(field.toString());
                field.setLength(0);

            } else {

                field.append(current);
            }
        }

        fields.add(field.toString());

        return fields;
    }
}