package za.co.autotest.reports;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Compares the two most recent test executions.
 */
public class ExecutionComparisonAnalyzer {

    private static final Path HISTORY_FILE =
            Paths.get(
                    "test-reports",
                    "history",
                    "execution-history.csv"
            );

    private ExecutionComparisonAnalyzer() {
        // Prevent direct object creation.
    }

    /**
     * Compares the latest execution with the previous execution.
     */
    public static void compareLatestExecutions() {

        System.out.println();
        System.out.println("========================================");
        System.out.println(" AUTOTEST EXECUTION COMPARISON");
        System.out.println("========================================");

        if (!Files.exists(HISTORY_FILE)) {
            System.out.println("No execution history found.");
            return;
        }

        try {

            List<String> lines =
                    Files.readAllLines(HISTORY_FILE);

            List<String[]> records = new ArrayList<>();

            // Read valid execution records.
            for (int i = 1; i < lines.size(); i++) {

                if (lines.get(i).isBlank()) {
                    continue;
                }

                String[] fields = parseCsvRecord(lines.get(i));

                if (fields.length == 9) {
                    records.add(fields);
                }
            }

            if (records.size() < 2) {

                System.out.println(
                        "At least two executions are required."
                );

                System.out.println(
                        "Current executions available: "
                                + records.size()
                );

                return;
            }

            // Get the two most recent executions.
            String[] previous =
                    records.get(records.size() - 2);

            String[] latest =
                    records.get(records.size() - 1);

            // Read previous execution values.
            int previousTotal =
                    Integer.parseInt(previous[4]);

            int previousPassed =
                    Integer.parseInt(previous[5]);

            int previousFailed =
                    Integer.parseInt(previous[6]);

            int previousSkipped =
                    Integer.parseInt(previous[7]);

            long previousDuration =
                    Long.parseLong(previous[8]);

            // Read latest execution values.
            int latestTotal =
                    Integer.parseInt(latest[4]);

            int latestPassed =
                    Integer.parseInt(latest[5]);

            int latestFailed =
                    Integer.parseInt(latest[6]);

            int latestSkipped =
                    Integer.parseInt(latest[7]);

            long latestDuration =
                    Long.parseLong(latest[8]);

            System.out.println(
                    "Previous execution ID: " + previous[0]
            );

            System.out.println(
                    "Latest execution ID: " + latest[0]
            );

            System.out.println("----------------------------------------");

            displayComparison(
                    "Total tests",
                    previousTotal,
                    latestTotal
            );

            displayComparison(
                    "Passed tests",
                    previousPassed,
                    latestPassed
            );

            displayComparison(
                    "Failed tests",
                    previousFailed,
                    latestFailed
            );

            displayComparison(
                    "Skipped tests",
                    previousSkipped,
                    latestSkipped
            );

            displayComparison(
                    "Execution time (ms)",
                    previousDuration,
                    latestDuration
            );

            System.out.println("----------------------------------------");

            // Calculate pass rates.
            double previousPassRate =
                    previousTotal == 0
                            ? 0
                            : previousPassed * 100.0 / previousTotal;

            double latestPassRate =
                    latestTotal == 0
                            ? 0
                            : latestPassed * 100.0 / latestTotal;

            System.out.printf(
                    "Previous pass rate: %.2f%%%n",
                    previousPassRate
            );

            System.out.printf(
                    "Latest pass rate: %.2f%%%n",
                    latestPassRate
            );

            System.out.printf(
                    "Pass rate change: %+.2f percentage points%n",
                    latestPassRate - previousPassRate
            );

            System.out.println("----------------------------------------");

            // Evaluate the latest execution.
            if (latestFailed < previousFailed
                    && latestPassed >= previousPassed) {

                System.out.println(
                        "RESULT: TEST QUALITY IMPROVED."
                );

            } else if (latestFailed > previousFailed) {

                System.out.println(
                        "RESULT: TEST FAILURES INCREASED."
                );

            } else if (latestFailed == previousFailed
                    && latestPassed == previousPassed) {

                System.out.println(
                        "RESULT: TEST RESULTS ARE UNCHANGED."
                );

            } else {

                System.out.println(
                        "RESULT: TEST RESULTS HAVE CHANGED."
                );
            }

            if (latestDuration < previousDuration) {

                System.out.println(
                        "PERFORMANCE: Latest execution was faster."
                );

            } else if (latestDuration > previousDuration) {

                System.out.println(
                        "PERFORMANCE: Latest execution was slower."
                );

            } else {

                System.out.println(
                        "PERFORMANCE: Execution time is unchanged."
                );
            }

            System.out.println("========================================");

        } catch (IOException | NumberFormatException exception) {

            System.err.println(
                    "Comparison failed: "
                            + exception.getMessage()
            );
        }
    }

    /**
     * Displays the difference between two execution values.
     */
    private static void displayComparison(
            String label,
            long previous,
            long latest) {

        long difference = latest - previous;

        System.out.printf(
                "%-25s Previous: %-8d Latest: %-8d Change: %+d%n",
                label,
                previous,
                latest,
                difference
        );
    }

    /**
     * Parses CSV records while supporting quoted fields.
     */
    private static String[] parseCsvRecord(String record) {

        List<String> fields = new ArrayList<>();

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