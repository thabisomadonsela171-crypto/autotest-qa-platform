
package za.co.autotest.web;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

/**
 * Creates and configures browser instances.
 */
public class WebDriverFactory {

    private WebDriverFactory() {
        // Prevent direct object creation.
    }

    public static WebDriver createEdgeDriver() {

        EdgeOptions options = new EdgeOptions();

        // Start the browser maximized.
        options.addArguments("--start-maximized");

        // Selenium Manager handles driver discovery.
        return new EdgeDriver(options);
    }
}
