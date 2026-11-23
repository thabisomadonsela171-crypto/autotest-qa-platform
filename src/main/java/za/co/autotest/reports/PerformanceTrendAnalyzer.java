package za.co.autotest.reports;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyses performance changes across multiple test executions.
 */
public final class PerformanceTrendAnalyzer {

    private PerformanceTrendAnalyzer() {
    }

    /**
     * Groups test performance records by execution ID.
     */
    public static Map<String, Object> analyze() {

        Map<String, Map<String, Object>> groupedExecutions =
                new LinkedHashMap<>();

        Map<String, Object> performance =
                PerformanceAnalyticsEngine.analyze();

        List<Map<String, Object>> records =
                (List<Map<String, Object>>) performance.get("records");

        for (Map<String, Object> record : records) {

            String executionId =
                    (String) record.get("executionId");

            Map<String, Object> execution =
                    groupedExecutions.get(executionId);

            if (execution == null) {

                execution = new LinkedHashMap<>();

                execution.put("executionId", executionId);
                execution.put("suiteName", record.get("suiteName"));
                execution.put("totalTests", 0);
                execution.put("passed", 0);
                execution.put("failed", 0);
                execution.put("skipped", 0);
                execution.put("totalDurationMs", 0L);

                groupedExecutions.put(executionId, execution);
            }

            long duration =
                    ((Number) record.get("durationMs")).longValue();

            execution.put(
                    "totalTests",
                    (Integer) execution.get("totalTests") + 1
            );

            execution.put(
                    "totalDurationMs",
                    (Long) execution.get("totalDurationMs") + duration
            );

            String status =
                    ((String) record.get("status")).toUpperCase();

            switch (status) {

                case "PASS" ->
                        execution.put(
                                "passed",
                                (Integer) execution.get("passed") + 1
                        );

                case "FAIL" ->
                        execution.put(
                                "failed",
                                (Integer) execution.get("failed") + 1
                        );

                case "SKIP" ->
                        execution.put(
                                "skipped",
                                (Integer) execution.get("skipped") + 1
                        );

                default -> {
                }
            }
        }

        List<Map<String, Object>> executions =
                new ArrayList<>(groupedExecutions.values());

        for (Map<String, Object> execution : executions) {

            int totalTests =
                    (Integer) execution.get("totalTests");

            long totalDuration =
                    (Long) execution.get("totalDurationMs");

            double averageDuration = totalTests == 0
                    ? 0
                    : (double) totalDuration / totalTests;

            execution.put("averageDurationMs", averageDuration);
        }

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("executionCount", executions.size());
        result.put("executions", executions);

        result.put("trendStatus", "INSUFFICIENT_DATA");
        result.put("previousAverageDurationMs", null);
        result.put("latestAverageDurationMs", null);
        result.put("percentageChange", null);

        if (executions.size() >= 2) {

            Map<String, Object> previousExecution =
                    executions.get(executions.size() - 2);

            Map<String, Object> latestExecution =
                    executions.get(executions.size() - 1);

            double previousAverage =
                    ((Number) previousExecution.get(
                            "averageDurationMs"
                    )).doubleValue();

            double latestAverage =
                    ((Number) latestExecution.get(
                            "averageDurationMs"
                    )).doubleValue();

            result.put(
                    "previousAverageDurationMs",
                    previousAverage
            );

            result.put(
                    "latestAverageDurationMs",
                    latestAverage
            );

            if (previousAverage > 0) {

                double percentageChange =
                        ((latestAverage - previousAverage)
                                / previousAverage) * 100;

                result.put("percentageChange", percentageChange);

                if (Math.abs(percentageChange) < 1.0) {
                    result.put("trendStatus", "STABLE");
                } else if (latestAverage < previousAverage) {
                    result.put("trendStatus", "FASTER");
                } else {
                    result.put("trendStatus", "SLOWER");
                }

            } else {

                result.put("trendStatus", "INSUFFICIENT_DATA");
            }
        }

        return result;
    }

    /**
     * Allows performance trends to be checked from the command line.
     */
    public static void main(String[] args) {

        Map<String, Object> trends = analyze();

        System.out.println();
        System.out.println("==========================================");
        System.out.println(" AUTOTEST QA PLATFORM");
        System.out.println(" PERFORMANCE TREND ANALYSIS");
        System.out.println("==========================================");

        System.out.println("Execution Count: "
                + trends.get("executionCount"));

        System.out.println("Trend Status: "
                + trends.get("trendStatus"));

        System.out.println("------------------------------------------");

        List<Map<String, Object>> executions =
                (List<Map<String, Object>>) trends.get("executions");

        for (int i = 0; i < executions.size(); i++) {

            Map<String, Object> execution = executions.get(i);

            System.out.println("Execution " + (i + 1));

            System.out.println("Suite: "
                    + execution.get("suiteName"));

            System.out.println("Total Tests: "
                    + execution.get("totalTests"));

            System.out.println("Passed: "
                    + execution.get("passed"));

            System.out.println("Failed: "
                    + execution.get("failed"));

            System.out.println("Average Duration: "
                    + String.format(
                            "%.2f",
                            ((Number) execution.get(
                                    "averageDurationMs"
                            )).doubleValue()
                    )
                    + " ms");

            System.out.println("------------------------------------------");
        }

        if (trends.get("percentageChange") != null) {

            System.out.println("Previous Average: "
                    + trends.get("previousAverageDurationMs") + " ms");

            System.out.println("Latest Average: "
                    + trends.get("latestAverageDurationMs") + " ms");

            System.out.printf("Performance Change: %.2f%%%n",
                    ((Number) trends.get("percentageChange")).doubleValue());
        } else {

            System.out.println(
                    "Run another test execution to compare performance."
            );
        }

        System.out.println("==========================================");
        System.out.println(" TREND ANALYSIS COMPLETED");
        System.out.println("==========================================");
    }
}