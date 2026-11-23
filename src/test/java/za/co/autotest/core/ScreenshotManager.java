
package za.co.autotest.core;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures and stores browser screenshots.
 */
public class ScreenshotManager {

    private ScreenshotManager() {
        // Prevent direct object creation.
    }

    public static String captureScreenshot(
            WebDriver driver,
            String testName) {

        if (driver == null) {
            System.out.println("Screenshot skipped: browser is unavailable.");
            return null;
        }

        try {

            Path screenshotDirectory = Paths.get("screenshots");

            Files.createDirectories(screenshotDirectory);

            String timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            );

            String fileName = testName + "_" + timestamp + ".png";

            Path destination = screenshotDirectory.resolve(fileName);

            File screenshot = ((TakesScreenshot) driver)
                    .getScreenshotAs(OutputType.FILE);

            Files.copy(screenshot.toPath(), destination);

            System.out.println("Screenshot saved: " + destination);

            return destination.toAbsolutePath().toString();

        } catch (IOException | RuntimeException exception) {

            System.out.println(
                    "Could not capture screenshot: "
                            + exception.getMessage()
            );

            return null;
        }
    }
}
