
package za.co.autotest.dashboard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestExecutionManager {

    private static final AtomicBoolean RUNNING =
            new AtomicBoolean(false);

    private static volatile String suite = "";
    private static volatile String status = "IDLE";
    private static volatile String message =
            "Ready to execute tests.";
    private static volatile String startedAt = "";
    private static volatile String finishedAt = "";
    private static volatile Integer exitCode = null;

    private static final Path PROJECT_DIRECTORY =
            Paths.get("").toAbsolutePath().normalize();

    private static final Path LOG_FILE =
            PROJECT_DIRECTORY.resolve("test-reports")
                    .resolve("dashboard-execution.log");

    private static final Path LISTENER_CLASS =
            PROJECT_DIRECTORY.resolve("target")
                    .resolve("test-classes")
                    .resolve("za")
                    .resolve("co")
                    .resolve("autotest")
                    .resolve("reports")
                    .resolve("ExtentTestListener.class");

    private static final Map<String, String> SUITES = Map.of(
            "API", "src/test/resources/api-suite.xml",
            "WEB", "src/test/resources/web-suite.xml",
            "ALL", "src/test/resources/testng.xml"
    );

    private TestExecutionManager() {
    }

    public static synchronized boolean startExecution(
            String requestedSuite) throws IOException {

        if (!SUITES.containsKey(requestedSuite)) {
            throw new IllegalArgumentException(
                    "Unsupported test suite: " + requestedSuite
            );
        }

        if (!RUNNING.compareAndSet(false, true)) {
            return false;
        }

        try {

            String suiteFile = SUITES.get(requestedSuite);

            Path suitePath =
                    PROJECT_DIRECTORY.resolve(suiteFile);

            if (!Files.exists(
                    PROJECT_DIRECTORY.resolve("pom.xml"))
                    || !Files.exists(suitePath)) {

                throw new IOException(
                        "Project pom.xml or selected TestNG suite was not found."
                );
            }

            Files.createDirectories(LOG_FILE.getParent());

            suite = requestedSuite;
            status = "RUNNING";
            message = "Maven is starting the "
                    + requestedSuite + " test suite.";

            startedAt = Instant.now().toString();
            finishedAt = "";
            exitCode = null;

            writeLog("\n========================================");
            writeLog("AUTOTEST QA PLATFORM - EXECUTION STARTED");
            writeLog("========================================");
            writeLog("Execution suite: " + requestedSuite);
            writeLog("Suite configuration: " + suitePath);
            writeLog("Project directory: " + PROJECT_DIRECTORY);
            writeLog("Log file: " + LOG_FILE);
            writeLog("Start time: " + startedAt);
            writeLog("Listener class before execution exists: "
                    + Files.exists(LISTENER_CLASS));
            writeLog("----------------------------------------");

            Thread executionThread = new Thread(
                    () -> executeSuite(requestedSuite, suiteFile),
                    "autotest-execution-" + requestedSuite
            );

            executionThread.setDaemon(true);
            executionThread.start();

            return true;

        } catch (IOException | RuntimeException exception) {

            RUNNING.set(false);
            status = "FAILED";
            message = "Could not prepare test execution: "
                    + exception.getMessage();

            finishedAt = Instant.now().toString();

            writeLogSafely(
                    "[AUTOTEST ERROR] " + exception.getMessage()
            );

            throw exception;
        }
    }

    private static void executeSuite(
            String requestedSuite,
            String suiteFile) {

        try {

            writeLog("[AUTOTEST] Starting Maven process.");

            ProcessBuilder builder = new ProcessBuilder(
                    "cmd.exe",
                    "/c",
                    "mvn.cmd",
                    "test",
                    "-DsuiteXmlFile=" + suiteFile,
                    "-DsuiteName=" + requestedSuite
            );

            builder.directory(PROJECT_DIRECTORY.toFile());

            builder.redirectErrorStream(true);

            builder.redirectOutput(
                    ProcessBuilder.Redirect.appendTo(
                            LOG_FILE.toFile()
                    )
            );

            writeLog("[AUTOTEST] Maven command: "
                    + String.join(" ", builder.command()));

            writeLog("[AUTOTEST] Maven working directory: "
                    + builder.directory());

            Process process = builder.start();

            int result = process.waitFor();

            exitCode = result;
            finishedAt = Instant.now().toString();

            boolean listenerExists =
                    Files.exists(LISTENER_CLASS);

            writeLog("----------------------------------------");
            writeLog("[AUTOTEST] Maven process finished.");
            writeLog("[AUTOTEST] Exit code: " + result);
            writeLog("[AUTOTEST] Listener class exists: "
                    + listenerExists);
            writeLog("[AUTOTEST] Finish time: " + finishedAt);

            if (result == 0) {

                status = "COMPLETED";

                message = requestedSuite
                        + " test suite completed successfully.";

                writeLog(
                        "[AUTOTEST] Maven execution completed successfully."
                );

            } else {

                status = "FAILED";

                message = requestedSuite
                        + " test suite failed. Check the execution log.";

                writeLog(
                        "[AUTOTEST ERROR] Maven returned exit code: "
                                + result
                );
            }

        } catch (IOException exception) {

            status = "FAILED";

            message = "Could not start Maven. Check Maven installation.";

            finishedAt = Instant.now().toString();

            writeLogSafely(
                    "[AUTOTEST ERROR] Maven startup failed: "
                            + exception.getMessage()
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            status = "FAILED";

            message = "Test execution was interrupted.";

            finishedAt = Instant.now().toString();

            writeLogSafely(
                    "[AUTOTEST ERROR] Execution interrupted: "
                            + exception.getMessage()
            );

        } finally {

            RUNNING.set(false);

            writeLogSafely(
                    "[AUTOTEST] Execution manager released."
            );
        }
    }

    private static synchronized void writeLog(String content)
            throws IOException {

        Files.writeString(
                LOG_FILE,
                content + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    private static void writeLogSafely(String content) {

        try {
            writeLog(content);

        } catch (IOException exception) {

            System.err.println(
                    "[AUTOTEST LOG ERROR] "
                            + exception.getMessage()
            );
        }
    }

    public static Map<String, Object> getStatus() {

        return Map.of(
                "running", RUNNING.get(),
                "suite", suite,
                "status", status,
                "message", message,
                "startedAt", startedAt,
                "finishedAt", finishedAt,
                "exitCode", exitCode == null ? -1 : exitCode
        );
    }
}
