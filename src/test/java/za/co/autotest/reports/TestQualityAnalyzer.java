package za.co.autotest.reports;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Analyses historical executions and generates
 * automated software quality insights.
 */
public class TestQualityAnalyzer {

    private static final Path HISTORY_FILE =
            Paths.get(
                    "test-reports",
                    "history",
                    "execution-history.csv"
            );

    private TestQualityAnalyzer() {
        // Prevent direct object creation.
    }

    /**
     * Generates an overall test quality assessment.
     */
    public static void generateQualityInsights() {

        System.out.println();
        System.out.println("========================================");
        System.out.println(" AUTOTEST QUALITY INSIGHTS");
        System.out.println("========================================");

        if (!Files.exists(HISTORY_FILE)) {

            System.out.println("No execution history found.");
            return;
        }

        try {

            List<String> lines =
                    Files.readAllLines(HISTORY_FILE);

            List<String[]> executions = new ArrayList<>();

            // Read saved execution records.
            for (int i = 1; i < lines.size(); i++) {

                if (lines.get(i).isBlank()) {
                    continue;
                }

                String[] fields = parseCsvRecord(lines.get(i));

                if (fields.length == 9) {
                    executions.add(fields);
                }
            }

            if (executions.isEmpty()) {

                System.out.println(
                        "No valid execution records available."
                );

                return;
            }

            int totalTests = 0;
            int totalPassed = 0;
            int totalFailed = 0;
            int totalSkipped = 0;

            long totalDuration = 0;

            int executionsWithFailures = 0;

            // Analyse all executions.
            for (String[] execution : executions) {

                int tests = Integer.parseInt(execution[4]);
                int passed = Integer.parseInt(execution[5]);
                int failed = Integer.parseInt(execution[6]);
                int skipped = Integer.parseInt(execution[7]);

                long duration = Long.parseLong(execution[8]);

                totalTests += tests;
                totalPassed += passed;
                totalFailed += failed;
                totalSkipped += skipped;

                totalDuration += duration;

                if (failed > 0) {
                    executionsWithFailures++;
                }
            }

            double reliability = totalTests == 0
                    ? 0
                    : totalPassed * 100.0 / totalTests;

            double averageDuration =
                    totalDuration / (double) executions.size();

            System.out.println(
                    "Total executions: " + executions.size()
            );

            System.out.println(
                    "Total tests executed: " + totalTests
            );

            System.out.println(
                    "Total tests passed: " + totalPassed
            );

            System.out.println(
                    "Total tests failed: " + totalFailed
            );

            System.out.println(
                    "Total tests skipped: " + totalSkipped
            );

            System.out.printf(
                    "Overall reliability: %.2f%%%n",
                    reliability
            );

            System.out.printf(
                    "Average execution time: %.2f ms%n",
                    averageDuration
            );

            System.out.println("----------------------------------------");

            // Determine the overall quality status.
            String qualityStatus;

            if (totalTests == 0) {

                qualityStatus = "INSUFFICIENT TEST DATA";

            } else if (totalFailed == 0 && totalSkipped == 0) {

                qualityStatus = "EXCELLENT";

            } else if (reliability >= 90 && totalFailed == 0) {

                qualityStatus = "GOOD";

            } else if (reliability >= 70) {

                qualityStatus = "NEEDS ATTENTION";

            } else {

                qualityStatus = "POOR";
            }

            System.out.println(
                    "QUALITY STATUS: " + qualityStatus
            );

            System.out.println("----------------------------------------");

            // Generate automatic insights.
            System.out.println("AUTOMATED INSIGHTS:");

            if (totalFailed == 0) {

                System.out.println(
                        "- No test failures detected."
                );

            } else {

                System.out.println(
                        "- " + totalFailed
                                + " test failures require investigation."
                );
            }

            if (totalSkipped == 0) {

                System.out.println(
                        "- No skipped tests detected."
                );

            } else {

                System.out.println(
                        "- Review " + totalSkipped
                                + " skipped tests."
                );
            }

            if (executionsWithFailures == 0) {

                System.out.println(
                        "- All recorded executions passed without failures."
                );

            } else {

                System.out.println(
                        "- " + executionsWithFailures
                                + " executions contained failures."
                );
            }

            if (executions.size() >= 2) {

                String[] previous =
                        executions.get(executions.size() - 2);

                String[] latest =
                        executions.get(executions.size() - 1);

                long previousDuration =
                        Long.parseLong(previous[8]);

                long latestDuration =
                        Long.parseLong(latest[8]);

                if (latestDuration > previousDuration) {

                    System.out.println(
                            "- Latest execution was slower than the previous run."
                    );

                } else if (latestDuration < previousDuration) {

                    System.out.println(
                            "- Latest execution was faster than the previous run."
                    );

                } else {

                    System.out.println(
                            "- Execution duration remained unchanged."
                    );
                }
            }

            if (reliability == 100.0) {

                System.out.println(
                        "- All executed tests passed successfully."
                );
            }

            System.out.println("========================================");

        } catch (IOException | NumberFormatException exception) {

            System.err.println(
                    "Quality analysis failed: "
                            + exception.getMessage()
            );
        }
    }

    /**
     * Parses CSV records while supporting quoted values.
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