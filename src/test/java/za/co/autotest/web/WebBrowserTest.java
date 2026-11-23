
package za.co.autotest.web;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import za.co.autotest.core.BaseTest;

public class WebBrowserTest extends BaseTest {

    @Test
    public void shouldOpenWebsiteSuccessfully() {

        // Create a Microsoft Edge browser.
        driver = WebDriverFactory.createEdgeDriver();

        // Open a website.
        driver.get("https://example.com");

        // Read the website title.
        String pageTitle = driver.getTitle();

        // Verify that the correct website opened.
        Assert.assertEquals(
                pageTitle,
                "Example Domain",
                "The website title is incorrect!"
        );

        System.out.println("Website opened successfully!");
        System.out.println("Page title: " + pageTitle);
    }
}
