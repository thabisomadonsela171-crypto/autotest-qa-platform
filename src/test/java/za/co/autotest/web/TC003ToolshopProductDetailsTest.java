
package za.co.autotest.web;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import za.co.autotest.core.BaseTest;

public class TC003ToolshopProductDetailsTest extends BaseTest {

    private static final String PRODUCT_URL =
            "https://practicesoftwaretesting.com/product/01M7TX6FJA6SG48PXXAKSKP42Z";

    @Test(description = "TC-003 - Verify Pliers Product Details")
    public void shouldVerifyPliersProductDetailsSuccessfully() {

        System.out.println("====================================");
        System.out.println("TEST CASE: TC-003 - PRODUCT DETAILS");
        System.out.println("====================================");

        driver = WebDriverFactory.createEdgeDriver();

        driver.manage().timeouts()
                .pageLoadTimeout(Duration.ofSeconds(60));

        driver.get(PRODUCT_URL);

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(30));

        wait.until(d ->
                "complete".equals(
                        ((JavascriptExecutor) d)
                                .executeScript("return document.readyState")
                )
        );

        // STEP 1: Verify the page title.
        wait.until(d -> d.getTitle().contains("Pliers"));

        Assert.assertTrue(
                driver.getTitle().contains("Pliers"),
                "Product page title does not contain Pliers."
        );

        System.out.println("PASS: Product page title verified.");

        // STEP 2: Verify the product name.
        WebElement productName = wait.until(
                d -> d.findElement(By.xpath(
                        "//*[self::h1 or self::h2][normalize-space()='Pliers']"
                ))
        );

        Assert.assertTrue(
                productName.isDisplayed(),
                "Product name is not visible."
        );

        System.out.println("PASS: Product name: " + productName.getText());

        // STEP 3: Verify the product price.
        WebElement productPrice = wait.until(
                d -> d.findElement(By.xpath(
                        "//*[normalize-space()='$12.01']"
                ))
        );

        Assert.assertTrue(
                productPrice.isDisplayed(),
                "Product price is not visible."
        );

        System.out.println("PASS: Product price: " + productPrice.getText());

        // STEP 4: Verify the product description.
        WebElement description = wait.until(
                d -> d.findElement(By.xpath(
                        "//*[contains(normalize-space(), 'Reliable general-purpose pliers')]"
                ))
        );

        Assert.assertTrue(
                description.isDisplayed(),
                "Product description is not visible."
        );

        Assert.assertTrue(
                description.getText().trim().length() > 50,
                "Product description appears incomplete."
        );

        System.out.println("PASS: Product description is visible.");

        // STEP 5: Verify the quantity control.
        WebElement quantityLabel = wait.until(
                d -> d.findElement(By.xpath(
                        "//*[normalize-space()='Quantity']"
                ))
        );

        Assert.assertTrue(
                quantityLabel.isDisplayed(),
                "Quantity label is not visible."
        );

        System.out.println("PASS: Quantity section is visible.");

        // STEP 6: Verify Add to cart without clicking it.
        WebElement addToCartButton = wait.until(
                d -> d.findElement(By.xpath(
                        "//button[contains(normalize-space(), 'Add to cart')]"
                ))
        );

        Assert.assertTrue(
                addToCartButton.isDisplayed(),
                "Add to cart button is not visible."
        );

        Assert.assertTrue(
                addToCartButton.isEnabled(),
                "Add to cart button is disabled."
        );

        System.out.println(
                "PASS: Add to cart button is visible and enabled."
        );

        // STEP 7: Verify specifications are displayed.
        WebElement specifications = wait.until(
                d -> d.findElement(By.xpath(
                        "//*[normalize-space()='Specifications']"
                ))
        );

        Assert.assertTrue(
                specifications.isDisplayed(),
                "Specifications section is not visible."
        );

        System.out.println("PASS: Specifications section is visible.");

        System.out.println("\n====================================");
        System.out.println("TC-003 RESULT: PASSED");
        System.out.println("====================================");

        // The cart is deliberately not modified.
    }
}
