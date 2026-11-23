package za.co.autotest.reports;

import java.util.List;
import java.util.Map;

/**
 * Runs and displays performance analytics from saved test history.
 */
public class PerformanceAnalyticsRunner {

    public static void main(String[] args) {

        Map<String, Object> analytics =
                PerformanceAnalyticsEngine.analyze();

        System.out.println();
        System.out.println("==========================================");
        System.out.println(" AUTOTEST QA PLATFORM");
        System.out.println(" TEST PERFORMANCE ANALYTICS");
        System.out.println("==========================================");

        System.out.println("Total Tests: "
                + analytics.get("totalTests"));

        System.out.println("Passed: "
                + analytics.get("passed"));

        System.out.println("Failed: "
                + analytics.get("failed"));

        System.out.println("Skipped: "
                + analytics.get("skipped"));

        System.out.println("Execution Count: "
                + analytics.get("executionCount"));

        System.out.println("------------------------------------------");

        System.out.printf("Average Duration: %.2f ms%n",
                ((Number) analytics.get("averageDurationMs")).doubleValue());

        System.out.println("Total Duration: "
                + analytics.get("totalDurationMs") + " ms");

        System.out.println("Pass Rate: "
                + String.format("%.2f", analytics.get("passRate")) + "%");

        System.out.println("Failure Rate: "
                + String.format("%.2f", analytics.get("failureRate")) + "%");

        System.out.println("------------------------------------------");

        Map<String, Object> fastest =
                (Map<String, Object>) analytics.get("fastestTest");

        Map<String, Object> slowest =
                (Map<String, Object>) analytics.get("slowestTest");

        if (fastest != null) {
            System.out.println("Fastest Test: "
                    + fastest.get("testName")
                    + " (" + fastest.get("durationMs") + " ms)");
        }

        if (slowest != null) {
            System.out.println("Slowest Test: "
                    + slowest.get("testName")
                    + " (" + slowest.get("durationMs") + " ms)");
        }

        System.out.println("------------------------------------------");

        System.out.println("Slow Test Threshold: "
                + analytics.get("slowTestThresholdMs") + " ms");

        System.out.println("Slow Test Count: "
                + analytics.get("slowTestCount"));

        List<Map<String, Object>> slowTests =
                (List<Map<String, Object>>) analytics.get("slowTests");

        for (Map<String, Object> test : slowTests) {
            System.out.println("SLOW: "
                    + test.get("testName")
                    + " - " + test.get("durationMs") + " ms");
        }

        System.out.println("==========================================");
        System.out.println(" PERFORMANCE ANALYSIS COMPLETED");
        System.out.println("==========================================");
    }
}