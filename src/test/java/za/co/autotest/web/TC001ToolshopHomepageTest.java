package za.co.autotest.web;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import za.co.autotest.core.BaseTest;

public class TC001ToolshopHomepageTest extends BaseTest {

    private static final String TOOLSHOP_URL =
            "https://practicesoftwaretesting.com/";

    @Test(description = "TC-001 - Toolshop homepage loads successfully")
    public void shouldLoadToolshopHomepageSuccessfully() {

        System.out.println("TEST CASE: TC-001");

        driver = WebDriverFactory.createEdgeDriver();

        driver.manage().timeouts()
                .pageLoadTimeout(Duration.ofSeconds(60));

        driver.get(TOOLSHOP_URL);

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(30));

        wait.until(d ->
                "complete".equals(
                        ((org.openqa.selenium.JavascriptExecutor) d)
                                .executeScript("return document.readyState")
                )
        );

        String pageTitle = driver.getTitle();

        System.out.println("PAGE TITLE: " + pageTitle);

        Assert.assertTrue(
                pageTitle.toLowerCase().contains("toolshop"),
                "TC-001 FAILED: Incorrect page title."
        );

        Assert.assertTrue(
                driver.findElement(By.tagName("body")).isDisplayed(),
                "Homepage body is not visible."
        );

        System.out.println("PASS: Homepage loaded.");

        By products = By.cssSelector(
                "a[href*='/product/'], app-product, [data-test*='product']"
        );

        wait.until(d ->
                d.findElements(products).stream()
                        .anyMatch(WebElement::isDisplayed)
        );

        long visibleProducts = driver.findElements(products).stream()
                .filter(WebElement::isDisplayed)
                .count();

        Assert.assertTrue(
                visibleProducts > 0,
                "No visible product cards found."
        );

        System.out.println("PASS: Product catalogue visible.");

        String pageText = driver.findElement(By.tagName("body")).getText();

        Assert.assertTrue(
                pageText.toLowerCase().contains("search"),
                "Search text not found."
        );

        List<WebElement> visibleInputs =
                driver.findElements(By.tagName("input")).stream()
                        .filter(WebElement::isDisplayed)
                        .toList();

        Assert.assertTrue(
                !visibleInputs.isEmpty(),
                "No visible input field found."
        );

        System.out.println("PASS: Search area visible.");

        String lowerPageText = pageText.toLowerCase();

        Assert.assertTrue(
                lowerPageText.contains("category")
                        || lowerPageText.contains("categories"),
                "Category information not found."
        );

        System.out.println("PASS: Category information visible.");

        long navigationLinks = driver.findElements(
                By.cssSelector("nav a, header a")
        ).stream()
                .filter(WebElement::isDisplayed)
                .count();

        Assert.assertTrue(
                navigationLinks > 0,
                "No visible navigation links found."
        );

        System.out.println("PASS: Navigation links visible.");

        System.out.println("TC-001 RESULT: PASSED");
    }
}