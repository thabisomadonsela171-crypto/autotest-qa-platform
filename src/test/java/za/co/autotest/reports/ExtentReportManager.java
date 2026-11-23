
package za.co.autotest.reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.io.File;

/**
 * Manages HTML test reports for AutoTest QA Platform.
 */
public class ExtentReportManager {

    private static ExtentReports extentReports;

    private ExtentReportManager() {
        // Prevent direct object creation.
    }

    /**
     * Creates and configures the HTML report.
     */
    public static synchronized ExtentReports getInstance() {

        if (extentReports == null) {

            // Create the report directory.
            File reportDirectory = new File("test-reports");

            if (!reportDirectory.exists()) {
                reportDirectory.mkdirs();
            }

            // Set the HTML report location.
            String reportPath = "test-reports/index.html";

            ExtentSparkReporter sparkReporter =
                    new ExtentSparkReporter(reportPath);

            // Configure report appearance.
            sparkReporter.config().setDocumentTitle(
                    "AutoTest QA Platform - Test Report"
            );

            sparkReporter.config().setReportName(
                    "Automated Testing Execution Report"
            );

            sparkReporter.config().setTimeStampFormat(
                    "dd MMM yyyy HH:mm:ss"
            );

            sparkReporter.config().setTheme(Theme.DARK);

            // Create the main report manager.
            extentReports = new ExtentReports();

            extentReports.attachReporter(sparkReporter);

            // Project information.
            extentReports.setSystemInfo(
                    "Project",
                    "AutoTest QA Platform"
            );

            extentReports.setSystemInfo(
                    "Automation Language",
                    "Java"
            );

            extentReports.setSystemInfo(
                    "Testing Framework",
                    "TestNG"
            );

            extentReports.setSystemInfo(
                    "Reporting Tool",
                    "ExtentReports"
            );

            // Environment information.
            extentReports.setSystemInfo(
                    "Environment",
                    System.getProperty(
                            "autotest.environment",
                            "local"
                    )
            );

            extentReports.setSystemInfo(
                    "Operating System",
                    System.getProperty("os.name")
            );

            extentReports.setSystemInfo(
                    "OS Version",
                    System.getProperty("os.version")
            );

            extentReports.setSystemInfo(
                    "System Architecture",
                    System.getProperty("os.arch")
            );

            // Java runtime information.
            extentReports.setSystemInfo(
                    "Java Version",
                    System.getProperty("java.version")
            );

            extentReports.setSystemInfo(
                    "Java Vendor",
                    System.getProperty("java.vendor")
            );

            // Automation browser information.
            extentReports.setSystemInfo(
                    "Browser",
                    System.getProperty(
                            "autotest.browser",
                            "Microsoft Edge"
                    )
            );
        }

        return extentReports;
    }

    /**
     * Saves all recorded test information.
     */
    public static synchronized void flushReport() {

        if (extentReports != null) {
            extentReports.flush();
        }
    }
}
