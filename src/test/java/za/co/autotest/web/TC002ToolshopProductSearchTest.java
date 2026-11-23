
package za.co.autotest.web;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import za.co.autotest.core.BaseTest;

public class TC002ToolshopProductSearchTest extends BaseTest {

    private static final String TOOLSHOP_URL =
            "https://practicesoftwaretesting.com/";

    @Test(description = "TC-002 - Search for Pliers")
    public void shouldSearchForPliersSuccessfully() {

        System.out.println("====================================");
        System.out.println("TEST CASE: TC-002 - PRODUCT SEARCH");
        System.out.println("====================================");

        driver = WebDriverFactory.createEdgeDriver();

        driver.manage().timeouts()
                .pageLoadTimeout(Duration.ofSeconds(60));

        driver.get(TOOLSHOP_URL);

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(30));

        wait.until(d ->
                "complete".equals(
                        ((JavascriptExecutor) d)
                                .executeScript("return document.readyState")
                )
        );

        // STEP 1: Find and verify the search input.
        WebElement searchInput = wait.until(
                d -> d.findElement(
                        By.cssSelector("[data-test='search-query']")
                )
        );

        Assert.assertTrue(
                searchInput.isDisplayed(),
                "Search input is not visible."
        );

        System.out.println("PASS: Search input located.");

        // STEP 2: Enter the product name.
        searchInput.clear();
        searchInput.sendKeys("Pliers");

        System.out.println("PASS: Entered search term: Pliers");

        // STEP 3: Click the Search button.
        WebElement searchButton = driver.findElement(
                By.cssSelector("[data-test='search-submit']")
        );

        Assert.assertTrue(
                searchButton.isDisplayed(),
                "Search button is not visible."
        );

        searchButton.click();

        System.out.println("PASS: Search button clicked.");

        // STEP 4: Wait for a matching product to appear.
        Boolean matchingProductFound = wait.until(d -> {

            JavascriptExecutor js = (JavascriptExecutor) d;

            return (Boolean) js.executeScript("""
                const products = document.querySelectorAll(
                    "a[href*='/product/'], app-product, [data-test*='product']"
                );

                return Array.from(products).some(element => {
                    const visible =
                        element.offsetWidth > 0 ||
                        element.offsetHeight > 0 ||
                        element.getClientRects().length > 0;

                    const text = (element.innerText || '').toLowerCase();

                    return visible && text.includes('pliers');
                });
                """);
        });

        // STEP 5: Verify the search results.
        Assert.assertTrue(
                matchingProductFound,
                "No matching Pliers product was found."
        );

        System.out.println("PASS: Matching Pliers product found.");

        // STEP 6: Print matching product information.
        JavascriptExecutor js = (JavascriptExecutor) driver;

        List<?> matchingProducts = (List<?>) js.executeScript("""
            const products = document.querySelectorAll(
                "a[href*='/product/'], app-product, [data-test*='product']"
            );

            return Array.from(products)
                .filter(element => {
                    const visible =
                        element.offsetWidth > 0 ||
                        element.offsetHeight > 0 ||
                        element.getClientRects().length > 0;

                    return visible &&
                        (element.innerText || '')
                            .toLowerCase()
                            .includes('pliers');
                })
                .map(element => ({
                    tag: element.tagName,
                    text: (element.innerText || '').trim(),
                    href: element.getAttribute('href')
                }));
            """);

        System.out.println("\n===== MATCHING PRODUCTS =====");

        for (Object product : matchingProducts) {
            System.out.println(product);
        }

        // STEP 7: Inspect actual product links for TC-003.
        List<?> productLinks = (List<?>) js.executeScript("""
            const links = document.querySelectorAll(
                "a[href*='/product/']"
            );

            return Array.from(links)
                .filter(link => {
                    const visible =
                        link.offsetWidth > 0 ||
                        link.offsetHeight > 0 ||
                        link.getClientRects().length > 0;

                    return visible &&
                        (link.innerText || '')
                            .toLowerCase()
                            .includes('pliers');
                })
                .map(link => ({
                    productName: (link.innerText || '').trim(),
                    productHref: link.getAttribute('href'),
                    fullUrl: link.href
                }));
            """);

        System.out.println("\n===== PRODUCT LINKS FOR TC-003 =====");

        for (Object link : productLinks) {
            System.out.println(link);
        }

        Assert.assertFalse(
                productLinks.isEmpty(),
                "No clickable Pliers product links were found."
        );

        System.out.println("\n====================================");
        System.out.println("TC-002 RESULT: PASSED");
        System.out.println("====================================");
    }
}
