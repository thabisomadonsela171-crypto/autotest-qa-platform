
package za.co.autotest.core;

import org.openqa.selenium.WebDriver;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import za.co.autotest.reports.ExtentTestListener;

import java.lang.reflect.Method;

/**
 * Base class containing common test lifecycle functionality.
 */
public class BaseTest {

    private long startTime;

    // Stores the browser used by the current test.
    protected WebDriver driver;

    // Runs before every test method.
    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {

        startTime = System.currentTimeMillis();

        driver = null;

        System.out.println();
        System.out.println("====================================");
        System.out.println("TEST STARTED: " + method.getName());
        System.out.println("====================================");
    }

    // Registers the browser with the base test.
    protected void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    // Runs after every test method.
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {

        long endTime = System.currentTimeMillis();

        long executionTime = endTime - startTime;

        System.out.println("------------------------------------");
        System.out.println("TEST FINISHED: " + result.getName());

        if (result.getStatus() == ITestResult.SUCCESS) {

            System.out.println("RESULT: PASSED");

        } else if (result.getStatus() == ITestResult.FAILURE) {

            System.out.println("RESULT: FAILED");

            // Capture screenshot before closing the browser.
            if (driver != null) {

                String screenshotPath =
                        ScreenshotManager.captureScreenshot(
                                driver,
                                result.getName()
                        );

                // Attach screenshot to ExtentReports.
                if (screenshotPath != null) {
                    ExtentTestListener.attachScreenshot(screenshotPath);
                }
            }

        } else if (result.getStatus() == ITestResult.SKIP) {

            System.out.println("RESULT: SKIPPED");
        }

        // Close browser after screenshot capture.
        if (driver != null) {

            try {

                driver.quit();

                System.out.println("Browser closed successfully.");

            } catch (Exception exception) {

                System.out.println(
                        "Browser cleanup warning: "
                                + exception.getMessage()
                );

            } finally {

                driver = null;
            }
        }

        System.out.println("EXECUTION TIME: " + executionTime + " ms");
        System.out.println("------------------------------------");
    }
}
