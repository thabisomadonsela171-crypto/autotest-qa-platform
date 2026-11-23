
package za.co.autotest.reports;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


public class ExecutionHistoryManager {

    private static final Path HISTORY_FILE =
            Paths.get(
                    "test-reports",
                    "history",
                    "execution-history.csv"
            ).toAbsolutePath().normalize();

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    private ExecutionHistoryManager() {
        // Prevent direct object creation.
    }

    /**
     * Saves one completed test execution.
     *
     * Existing CSV records are preserved.
     * New execution records are appended.
     */
    public static synchronized void saveExecution(
            String executionId,
            String suiteName,
            int total,
            int passed,
            int failed,
            int skipped,
            long startTime,
            long endTime) {

        System.out.println(
                "[AUTOTEST] ExecutionHistoryManager.saveExecution() entered."
        );

        System.out.println(
                "[AUTOTEST] History file: " + HISTORY_FILE
        );

        try {

            // 1. Validate the shared execution ID.

            if (executionId == null || executionId.isBlank()) {

                throw new IllegalArgumentException(
                        "A valid shared execution ID is required."
                );
            }

            // 2. Validate execution statistics.

            if (total < 0 || passed < 0
                    || failed < 0 || skipped < 0) {

                throw new IllegalArgumentException(
                        "Execution statistics cannot be negative."
                );
            }

            if (total != passed + failed + skipped) {

                throw new IllegalArgumentException(
                        "Total tests do not match passed, failed and skipped."
                );
            }

            // 2. Create history directory if necessary.

            Files.createDirectories(
                    HISTORY_FILE.getParent()
            );

            // 3. Check existing CSV.

            boolean fileExists =
                    Files.exists(HISTORY_FILE);

            boolean fileIsEmpty =
                    !fileExists || Files.size(HISTORY_FILE) == 0;

            System.out.println(
                    "[AUTOTEST] History file exists: " + fileExists
            );

            // 4. Use the shared execution ID supplied by the listener.

            System.out.println(
                    "[AUTOTEST] Shared execution ID received: " + executionId
            );

            // 5. Format timestamps.

            DateTimeFormatter formatter =
                    DATE_FORMAT.withZone(
                            ZoneId.systemDefault()
                    );

            String start =
                    formatter.format(
                            Instant.ofEpochMilli(startTime)
                    );

            String end =
                    formatter.format(
                            Instant.ofEpochMilli(endTime)
                    );

            // 6. Calculate duration.

            long duration =
                    Math.max(0, endTime - startTime);

            // 7. Construct CSV record.

            String record = String.join(",",
                    executionId,
                    escapeCsv(suiteName),
                    start,
                    end,
                    String.valueOf(total),
                    String.valueOf(passed),
                    String.valueOf(failed),
                    String.valueOf(skipped),
                    String.valueOf(duration)
            );

            // 8. Add header only if the CSV is new or empty.

            if (fileIsEmpty) {

                String header =
                        "Execution ID,Suite Name,Start Time,"
                        + "End Time,Total Tests,Passed,"
                        + "Failed,Skipped,Duration (ms)"
                        + System.lineSeparator();

                Files.writeString(
                        HISTORY_FILE,
                        header,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );

                System.out.println(
                        "[AUTOTEST] CSV header created."
                );
            }

            // 9. Append the new record.

            Files.writeString(
                    HISTORY_FILE,
                    record + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            // 10. Confirm successful save.

            System.out.println(
                    "[AUTOTEST] Execution history saved successfully."
            );

            System.out.println(
                    "[AUTOTEST] Execution ID: " + executionId
            );

            System.out.println(
                    "[AUTOTEST] Saved record: " + record
            );

            System.out.println(
                    "[AUTOTEST] CSV location: " + HISTORY_FILE
            );

            System.out.println(
                    "[AUTOTEST] CSV file size: "
                    + Files.size(HISTORY_FILE)
                    + " bytes"
            );

        } catch (IOException exception) {

            System.err.println(
                    "[AUTOTEST] CSV WRITE ERROR: "
                    + exception.getMessage()
            );

            exception.printStackTrace();

        } catch (Exception exception) {

            System.err.println(
                    "[AUTOTEST] HISTORY RECORD ERROR: "
                    + exception.getMessage()
            );

            exception.printStackTrace();
        }
    }

    /**
     * Escapes CSV values correctly.
     */
    private static String escapeCsv(String value) {

        if (value == null) {
            return "\"\"";
        }

        return "\""
                + value.replace("\"", "\"\"")
                + "\"";
    }
}
