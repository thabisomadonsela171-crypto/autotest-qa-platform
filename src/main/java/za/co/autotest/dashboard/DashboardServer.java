
package za.co.autotest.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import za.co.autotest.reports.PerformanceAnalyticsEngine;
import za.co.autotest.reports.PerformanceTrendAnalyzer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

public class DashboardServer {

    private static final int PORT = 8080;

    private static final Path HISTORY_FILE =
            Paths.get("test-reports", "history", "execution-history.csv");

    private static final Path PERFORMANCE_HISTORY_FILE =
            Paths.get("test-reports", "history", "performance-history.csv");

    private static final Path HTML_REPORT_FILE =
            Paths.get("test-reports", "index.html");

    private static final Path LIVE_EXECUTION_FILE =
            Paths.get("test-reports", "live-execution.properties");

    private static final Path LIVE_ACTIVITY_FILE =
            Paths.get("test-reports", "live-execution-activity.jsonl");

    private static final Path DASHBOARD_DIRECTORY =
            Paths.get("src", "main", "resources", "dashboard");

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    public static void main(String[] args) {

        try {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress("127.0.0.1", PORT), 0
            );

            server.createContext("/", DashboardServer::handleRequest);

            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            System.out.println("====================================");
            System.out.println(" AUTOTEST DASHBOARD SERVER");
            System.out.println("====================================");
            System.out.println("Dashboard: http://localhost:" + PORT);
            System.out.println("Dashboard Home: http://localhost:" + PORT + "/dashboard/");
            System.out.println("Dashboard API: http://localhost:" + PORT
                    + "/api/dashboard");
            System.out.println("Execution API: http://localhost:" + PORT
                    + "/api/executions/status");
            System.out.println("Live API: http://localhost:" + PORT
                    + "/api/executions/live");
            System.out.println("Activity API: http://localhost:" + PORT
                    + "/api/executions/activity");
            System.out.println("Performance API: http://localhost:" + PORT
                    + "/api/analytics/performance");
            System.out.println("Trends API: http://localhost:" + PORT
                    + "/api/analytics/trends");
            System.out.println("HTML Report: http://localhost:" + PORT
                    + "/reports/html");
            System.out.println("Execution CSV: http://localhost:" + PORT
                    + "/reports/execution-history.csv");
            System.out.println("Performance CSV: http://localhost:" + PORT
                    + "/reports/performance-history.csv");
            System.out.println("====================================");

        } catch (IOException exception) {
            System.err.println(
                    "Could not start server: " + exception.getMessage()
            );
        }
    }

    private static void handleRequest(HttpExchange exchange)
            throws IOException {

        String method = exchange.getRequestMethod().toUpperCase();
        String requestPath = exchange.getRequestURI().getPath();

        if (method.equals("GET")) {

            if (requestPath.equals("/api/dashboard")) {
                handleDashboardApi(exchange);
                return;
            }

            if (requestPath.equals("/api/executions/status")) {
                handleExecutionStatus(exchange);
                return;
            }

            if (requestPath.equals("/api/executions/live")) {
                handleLiveExecution(exchange);
                return;
            }

            if (requestPath.equals("/api/executions/activity")) {
                handleExecutionActivity(exchange);
                return;
            }

            if (requestPath.equals("/api/analytics/performance")) {
                handlePerformanceAnalytics(exchange);
                return;
            }

            if (requestPath.equals("/api/analytics/trends")) {
                handlePerformanceTrends(exchange);
                return;
            }

            // Reports Centre routes.
            if (requestPath.equals("/reports/html")) {
                serveReportFile(
                        exchange,
                        HTML_REPORT_FILE,
                        "text/html; charset=UTF-8",
                        null
                );
                return;
            }

            if (requestPath.equals("/reports/execution-history.csv")) {
                serveReportFile(
                        exchange,
                        HISTORY_FILE,
                        "text/csv; charset=UTF-8",
                        "execution-history.csv"
                );
                return;
            }

            if (requestPath.equals("/reports/performance-history.csv")) {
                serveReportFile(
                        exchange,
                        PERFORMANCE_HISTORY_FILE,
                        "text/csv; charset=UTF-8",
                        "performance-history.csv"
                );
                return;
            }

            serveDashboardFile(exchange, requestPath);
            return;
        }

        if (method.equals("POST")
                && requestPath.equals("/api/executions")) {

            handleStartExecution(exchange);
            return;
        }

        exchange.getResponseHeaders().set("Allow", "GET, POST");

        sendResponse(
                exchange,
                405,
                "application/json; charset=UTF-8",
                "{\"error\":\"Method not allowed\"}"
        );
    }

    // Serves existing report files without modifying them.
    private static void serveReportFile(
            HttpExchange exchange,
            Path reportFile,
            String contentType,
            String downloadFileName) throws IOException {

        if (!Files.exists(reportFile)
                || !Files.isRegularFile(reportFile)
                || !Files.isReadable(reportFile)) {

            sendResponse(
                    exchange,
                    404,
                    "text/plain; charset=UTF-8",
                    "Report file not found: " + reportFile.getFileName()
            );
            return;
        }

        byte[] reportContent = Files.readAllBytes(reportFile);

        if (downloadFileName != null) {
            exchange.getResponseHeaders().set(
                    "Content-Disposition",
                    "attachment; filename=\"" + downloadFileName + "\""
            );
        } else {
            exchange.getResponseHeaders().set(
                    "Content-Disposition",
                    "inline"
            );
        }

        sendBytes(
                exchange,
                200,
                contentType,
                reportContent
        );
    }

    // Reads live execution information from the shared properties file.
    private static void handleLiveExecution(HttpExchange exchange)
            throws IOException {

        Properties properties = new Properties();

        if (Files.exists(LIVE_EXECUTION_FILE)) {

            try (var input = Files.newInputStream(LIVE_EXECUTION_FILE)) {
                properties.load(input);
            }
        }

        String suiteName = properties.getProperty(
                "suiteName", "AutoTest QA Platform"
        );

        String status = properties.getProperty("status", "IDLE");

        String currentTest = properties.getProperty(
                "currentTest", "Waiting for tests..."
        );

        long total = getLongProperty(properties, "total", 0);
        long passed = getLongProperty(properties, "passed", 0);
        long failed = getLongProperty(properties, "failed", 0);
        long skipped = getLongProperty(properties, "skipped", 0);

        long startTime = getLongProperty(properties, "startTime", 0);
        long endTime = getLongProperty(properties, "endTime", 0);
        long duration = getLongProperty(properties, "duration", 0);

        if ("RUNNING".equalsIgnoreCase(status) && startTime > 0) {
            duration = Math.max(
                    0,
                    System.currentTimeMillis() - startTime
            );
        }

        String response = "{"
                + "\"suiteName\":\"" + escapeJson(suiteName) + "\","
                + "\"status\":\"" + escapeJson(status) + "\","
                + "\"currentTest\":\"" + escapeJson(currentTest) + "\","
                + "\"total\":" + total + ","
                + "\"passed\":" + passed + ","
                + "\"failed\":" + failed + ","
                + "\"skipped\":" + skipped + ","
                + "\"startTime\":" + startTime + ","
                + "\"endTime\":" + endTime + ","
                + "\"duration\":" + duration
                + "}";

        sendResponse(
                exchange,
                200,
                "application/json; charset=UTF-8",
                response
        );
    }

    // Reads individual test events from the JSONL activity file.
    private static void handleExecutionActivity(HttpExchange exchange)
            throws IOException {

        List<String> events = new ArrayList<>();

        if (Files.exists(LIVE_ACTIVITY_FILE)) {

            List<String> lines = Files.readAllLines(
                    LIVE_ACTIVITY_FILE,
                    StandardCharsets.UTF_8
            );

            for (String line : lines) {

                if (!line.isBlank()) {
                    events.add(line.trim());
                }
            }

            if (events.size() > 100) {
                events = new ArrayList<>(
                        events.subList(events.size() - 100, events.size())
                );
            }
        }

        StringBuilder response = new StringBuilder("[");

        for (int i = 0; i < events.size(); i++) {

            if (i > 0) {
                response.append(",");
            }

            response.append(events.get(i));
        }

        response.append("]");

        sendResponse(
                exchange,
                200,
                "application/json; charset=UTF-8",
                response.toString()
        );
    }

    // Returns individual test performance analytics.
    private static void handlePerformanceAnalytics(HttpExchange exchange)
            throws IOException {

        try {

            Map<String, Object> analytics =
                    PerformanceAnalyticsEngine.analyze();

            String response =
                    OBJECT_MAPPER.writeValueAsString(analytics);

            sendResponse(
                    exchange,
                    200,
                    "application/json; charset=UTF-8",
                    response
            );

        } catch (Exception exception) {

            exception.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "application/json; charset=UTF-8",
                    "{\"error\":\"Could not generate performance analytics\"}"
            );
        }
    }

    // Returns performance comparisons between test executions.
    private static void handlePerformanceTrends(HttpExchange exchange)
            throws IOException {

        try {

            Map<String, Object> trends =
                    PerformanceTrendAnalyzer.analyze();

            String response =
                    OBJECT_MAPPER.writeValueAsString(trends);

            sendResponse(
                    exchange,
                    200,
                    "application/json; charset=UTF-8",
                    response
            );

        } catch (Exception exception) {

            exception.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "application/json; charset=UTF-8",
                    "{\"error\":\"Could not generate performance trends\"}"
            );
        }
    }

    // Safely reads a numeric value from the properties file.
    private static long getLongProperty(
            Properties properties,
            String key,
            long defaultValue) {

        try {
            return Long.parseLong(
                    properties.getProperty(key, String.valueOf(defaultValue))
            );

        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static void handleExecutionStatus(HttpExchange exchange)
            throws IOException {

        Map<String, Object> execution = TestExecutionManager.getStatus();

        String response = "{"
                + "\"running\":" + execution.get("running") + ","
                + "\"suite\":\"" + escapeJson(
                        String.valueOf(execution.get("suite"))) + "\","
                + "\"status\":\"" + escapeJson(
                        String.valueOf(execution.get("status"))) + "\","
                + "\"message\":\"" + escapeJson(
                        String.valueOf(execution.get("message"))) + "\","
                + "\"startedAt\":\"" + escapeJson(
                        String.valueOf(execution.get("startedAt"))) + "\","
                + "\"finishedAt\":\"" + escapeJson(
                        String.valueOf(execution.get("finishedAt"))) + "\","
                + "\"exitCode\":" + execution.get("exitCode")
                + "}";

        sendResponse(
                exchange,
                200,
                "application/json; charset=UTF-8",
                response
        );
    }

    private static void handleStartExecution(HttpExchange exchange)
            throws IOException {

        String origin = exchange.getRequestHeaders().getFirst("Origin");

        if (origin != null
                && !origin.equals("http://localhost:" + PORT)
                && !origin.equals("http://127.0.0.1:" + PORT)) {

            sendResponse(
                    exchange,
                    403,
                    "application/json; charset=UTF-8",
                    "{\"error\":\"Request origin not allowed\"}"
            );
            return;
        }

        byte[] bodyBytes = exchange.getRequestBody().readNBytes(1025);

        if (bodyBytes.length > 1024) {

            sendResponse(
                    exchange,
                    413,
                    "application/json; charset=UTF-8",
                    "{\"error\":\"Request body is too large\"}"
            );
            return;
        }

        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        String requestedSuite;

        try {
            requestedSuite = parseRequestedSuite(body);

        } catch (IllegalArgumentException exception) {

            sendResponse(
                    exchange,
                    400,
                    "application/json; charset=UTF-8",
                    "{\"error\":\""
                            + escapeJson(exception.getMessage())
                            + "\"}"
            );
            return;
        }

        try {

            boolean started =
                    TestExecutionManager.startExecution(requestedSuite);

            if (!started) {

                sendResponse(
                        exchange,
                        409,
                        "application/json; charset=UTF-8",
                        "{\"error\":\"Another test execution is already running\"}"
                );
                return;
            }

            sendResponse(
                    exchange,
                    202,
                    "application/json; charset=UTF-8",
                    "{\"accepted\":true,\"suite\":\""
                            + requestedSuite
                            + "\",\"message\":\"Test execution started\"}"
            );

        } catch (IllegalArgumentException exception) {

            sendResponse(
                    exchange,
                    400,
                    "application/json; charset=UTF-8",
                    "{\"error\":\"Unsupported test suite\"}"
            );

        } catch (IOException exception) {

            exception.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "application/json; charset=UTF-8",
                    "{\"error\":\"Could not start test execution\"}"
            );
        }
    }

    private static String parseRequestedSuite(String body) {

        String requestedSuite = null;

        if (body.isBlank()) {
            throw new IllegalArgumentException(
                    "Suite selection is required."
            );
        }

        String[] parameters = body.split("&");

        for (String parameter : parameters) {

            String[] parts = parameter.split("=", 2);

            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid request format."
                );
            }

            String key = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8
            );

            String value = URLDecoder.decode(
                    parts[1], StandardCharsets.UTF_8
            );

            if (!key.equals("suite") || requestedSuite != null) {
                throw new IllegalArgumentException(
                        "Invalid suite selection."
                );
            }

            requestedSuite = value;
        }

        if (!"API".equals(requestedSuite)
                && !"WEB".equals(requestedSuite)
                && !"ALL".equals(requestedSuite)) {

            throw new IllegalArgumentException(
                    "Unsupported test suite."
            );
        }

        return requestedSuite;
    }

    private static void handleDashboardApi(HttpExchange exchange)
            throws IOException {

        try {

            sendResponse(
                    exchange,
                    200,
                    "application/json; charset=UTF-8",
                    generateDashboardData()
            );

        } catch (Exception exception) {

            exception.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "application/json; charset=UTF-8",
                    "{\"error\":\"Could not read execution history\"}"
            );
        }
    }

    // Serves dashboard HTML, CSS and JavaScript files.
    private static void serveDashboardFile(
            HttpExchange exchange,
            String requestPath) throws IOException {

        String fileName;
        String contentType;

        switch (requestPath) {

            case "/":
            case "/index.html":
            case "/dashboard":
            case "/dashboard/":
                fileName = "index.html";
                contentType = "text/html; charset=UTF-8";
                break;

            case "/style.css":
                fileName = "style.css";
                contentType = "text/css; charset=UTF-8";
                break;

            case "/app.js":
                fileName = "app.js";
                contentType = "application/javascript; charset=UTF-8";
                break;

            case "/live-monitor.js":
                fileName = "live-monitor.js";
                contentType = "application/javascript; charset=UTF-8";
                break;

            case "/live-monitor.css":
                fileName = "live-monitor.css";
                contentType = "text/css; charset=UTF-8";
                break;

            case "/performance-analytics.css":
                fileName = "performance-analytics.css";
                contentType = "text/css; charset=UTF-8";
                break;

            case "/performance-analytics.js":
                fileName = "performance-analytics.js";
                contentType = "application/javascript; charset=UTF-8";
                break;

            default:

                sendResponse(
                        exchange,
                        404,
                        "text/plain; charset=UTF-8",
                        "Page not found"
                );
                return;
        }

        Path file = DASHBOARD_DIRECTORY.resolve(fileName);

        if (!Files.exists(file) || !Files.isRegularFile(file)) {

            sendResponse(
                    exchange,
                    404,
                    "text/plain; charset=UTF-8",
                    "Dashboard file not found: " + fileName
            );
            return;
        }

        byte[] fileContent = Files.readAllBytes(file);

        sendBytes(exchange, 200, contentType, fileContent);
    }

    private static String generateDashboardData() throws IOException {

        List<String[]> executions = readExecutions();

        int totalTests = 0;
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        long totalDuration = 0;

        for (String[] execution : executions) {

            totalTests += Integer.parseInt(execution[4]);
            passed += Integer.parseInt(execution[5]);
            failed += Integer.parseInt(execution[6]);
            skipped += Integer.parseInt(execution[7]);
            totalDuration += Long.parseLong(execution[8]);
        }

        double passRate = totalTests == 0
                ? 0
                : passed * 100.0 / totalTests;

        double averageDuration = executions.isEmpty()
                ? 0
                : totalDuration / (double) executions.size();

        String latestExecutionId = executions.isEmpty()
                ? "N/A"
                : executions.get(executions.size() - 1)[0];

        String qualityStatus;

        if (totalTests == 0) {
            qualityStatus = "NO DATA";

        } else if (failed == 0 && skipped == 0) {
            qualityStatus = "EXCELLENT";

        } else if (passRate >= 90 && failed == 0) {
            qualityStatus = "GOOD";

        } else if (passRate >= 70) {
            qualityStatus = "NEEDS ATTENTION";

        } else {
            qualityStatus = "POOR";
        }

        StringBuilder historyJson = new StringBuilder("[");

        for (int i = 0; i < executions.size(); i++) {

            String[] execution = executions.get(i);

            if (i > 0) {
                historyJson.append(",");
            }

            historyJson.append("{")
                    .append("\"executionId\":\"")
                    .append(escapeJson(execution[0]))
                    .append("\",")
                    .append("\"suiteName\":\"")
                    .append(escapeJson(execution[1]))
                    .append("\",")
                    .append("\"startTime\":\"")
                    .append(escapeJson(execution[2]))
                    .append("\",")
                    .append("\"total\":").append(execution[4]).append(",")
                    .append("\"passed\":").append(execution[5]).append(",")
                    .append("\"failed\":").append(execution[6]).append(",")
                    .append("\"skipped\":").append(execution[7]).append(",")
                    .append("\"duration\":").append(execution[8])
                    .append("}");
        }

        historyJson.append("]");

        return "{"
                + "\"totalExecutions\":" + executions.size() + ","
                + "\"totalTests\":" + totalTests + ","
                + "\"passed\":" + passed + ","
                + "\"failed\":" + failed + ","
                + "\"skipped\":" + skipped + ","
                + "\"passRate\":" + String.format(
                        java.util.Locale.US, "%.2f", passRate
                ) + ","
                + "\"averageDuration\":" + String.format(
                        java.util.Locale.US, "%.2f", averageDuration
                ) + ","
                + "\"latestExecutionId\":\""
                + escapeJson(latestExecutionId) + "\","
                + "\"qualityStatus\":\"" + qualityStatus + "\","
                + "\"history\":" + historyJson
                + "}";
    }

    private static List<String[]> readExecutions() throws IOException {

        List<String[]> executions = new ArrayList<>();

        if (!Files.exists(HISTORY_FILE)) {
            return executions;
        }

        List<String> lines = Files.readAllLines(HISTORY_FILE);

        for (int i = 1; i < lines.size(); i++) {

            if (lines.get(i).isBlank()) {
                continue;
            }

            String[] fields = parseCsvRecord(lines.get(i));

            if (fields.length == 9) {
                executions.add(fields);
            }
        }

        return executions;
    }

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

    private static String escapeJson(String value) {

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

    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String contentType,
            String response) throws IOException {

        sendBytes(
                exchange,
                statusCode,
                contentType,
                response.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static void sendBytes(
            HttpExchange exchange,
            int statusCode,
            String contentType,
            byte[] responseBytes) throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type", contentType
        );

        exchange.getResponseHeaders().set(
                "Cache-Control", "no-store"
        );

        exchange.getResponseHeaders().set(
                "X-Content-Type-Options", "nosniff"
        );

        exchange.sendResponseHeaders(
                statusCode, responseBytes.length
        );

        try (var output = exchange.getResponseBody()) {
            output.write(responseBytes);
        }
    }
}
