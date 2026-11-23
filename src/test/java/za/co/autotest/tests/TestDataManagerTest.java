
package za.co.autotest.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

import za.co.autotest.core.BaseTest;
import za.co.autotest.core.TestDataManager;

public class TestDataManagerTest extends BaseTest {

    // Test 1: Verify valid user data.
    @Test
    public void shouldGenerateValidUserData() {

        Map<String, Object> userData =
                TestDataManager.getValidUserData();

        Assert.assertEquals(
                userData.get("name"),
                "AutoTest User"
        );

        Assert.assertTrue(
                userData.get("email").toString().contains("@example.test")
        );

        Assert.assertFalse(
                userData.get("password").toString().isEmpty()
        );
    }

    // Test 2: Verify invalid user data.
    @Test
    public void shouldGenerateInvalidUserData() {

        Map<String, Object> userData =
                TestDataManager.getInvalidUserData();

        Assert.assertEquals(userData.get("name"), "");
        Assert.assertEquals(userData.get("email"), "invalid-email");
        Assert.assertEquals(userData.get("password"), "");
    }

    // Test 3: Verify unique email generation.
    @Test
    public void shouldGenerateUniqueEmailAddresses() {

        String firstEmail =
                TestDataManager.generateUniqueEmail();

        String secondEmail =
                TestDataManager.generateUniqueEmail();

        Assert.assertNotEquals(
                firstEmail,
                secondEmail,
                "Generated email addresses should be unique."
        );
    }
}
