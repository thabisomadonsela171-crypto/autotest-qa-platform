package za.co.autotest.reports;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Analyses historical automated test executions.
 */
public class ExecutionHistoryAnalyzer {

    private static final Path HISTORY_FILE =
            Paths.get(
                    "test-reports",
                    "history",
                    "execution-history.csv"
            );

    private ExecutionHistoryAnalyzer() {
        // Prevent direct object creation.
    }

    /**
     * Reads and analyses all saved test executions.
     */
    public static void displayHistorySummary() {

        System.out.println();
        System.out.println("========================================");
        System.out.println(" AUTOTEST EXECUTION HISTORY ANALYSIS");
        System.out.println("========================================");

        if (!Files.exists(HISTORY_FILE)) {

            System.out.println("No execution history found.");
            return;
        }

        try {

            List<String> records =
                    Files.readAllLines(HISTORY_FILE);

            if (records.size() <= 1) {

                System.out.println("No execution records available.");
                return;
            }

            int totalRuns = 0;
            int totalTests = 0;
            int totalPassed = 0;
            int totalFailed = 0;
            int totalSkipped = 0;

            long totalDuration = 0;

            int highestFailures = 0;

            String latestExecution = "";

            for (int i = 1; i < records.size(); i++) {

                String record = records.get(i);

                if (record.isBlank()) {
                    continue;
                }

                String[] fields = parseCsvRecord(record);

                if (fields.length != 9) {
                    System.out.println(
                            "Skipping invalid record: " + record
                    );
                    continue;
                }

                int tests = Integer.parseInt(fields[4]);
                int passed = Integer.parseInt(fields[5]);
                int failed = Integer.parseInt(fields[6]);
                int skipped = Integer.parseInt(fields[7]);

                long duration = Long.parseLong(fields[8]);

                totalRuns++;

                totalTests += tests;
                totalPassed += passed;
                totalFailed += failed;
                totalSkipped += skipped;

                totalDuration += duration;

                if (failed > highestFailures) {
                    highestFailures = failed;
                }

                latestExecution = fields[0];
            }

            if (totalRuns == 0) {
                System.out.println("No valid execution records found.");
                return;
            }

            double passRate = totalTests == 0
                    ? 0
                    : (totalPassed * 100.0) / totalTests;

            double averageDuration =
                    totalDuration / (double) totalRuns;

            System.out.println("Total executions: " + totalRuns);

            System.out.println("Total tests executed: " + totalTests);

            System.out.println("Total tests passed: " + totalPassed);

            System.out.println("Total tests failed: " + totalFailed);

            System.out.println("Total tests skipped: " + totalSkipped);

            System.out.printf(
                    "Overall pass rate: %.2f%%%n",
                    passRate
            );

            System.out.printf(
                    "Average execution time: %.2f ms%n",
                    averageDuration
            );

            System.out.println(
                    "Highest failures in one execution: "
                            + highestFailures
            );

            System.out.println(
                    "Latest execution ID: " + latestExecution
            );

            System.out.println("========================================");

        } catch (IOException | NumberFormatException exception) {

            System.err.println(
                    "Could not analyse execution history: "
                            + exception.getMessage()
            );
        }
    }

    /**
     * Parses CSV values, including quoted fields.
     */
    private static String[] parseCsvRecord(String record) {

        List<String> fields = new java.util.ArrayList<>();

        StringBuilder field = new StringBuilder();

        boolean insideQuotes = false;

        for (int i = 0; i < record.length(); i++) {

            char character = record.charAt(i);

            if (character == '"') {

                if (insideQuotes
                        && i + 1 < record.length()
                        && record.charAt(i + 1) == '"') {

                    field.append('"');
                    i++;

                } else {

                    insideQuotes = !insideQuotes;
                }

            } else if (character == ',' && !insideQuotes) {

                fields.add(field.toString());
                field.setLength(0);

            } else {

                field.append(character);
            }
        }

        fields.add(field.toString());

        return fields.toArray(new String[0]);
    }
}