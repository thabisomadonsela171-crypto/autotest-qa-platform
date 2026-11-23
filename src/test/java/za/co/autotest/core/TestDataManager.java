
package za.co.autotest.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages reusable test data for automated tests.
 */
public class TestDataManager {

    private TestDataManager() {
        // Prevent direct object creation.
    }

    /**
     * Generates a unique email address for testing.
     */
    public static String generateUniqueEmail() {

        return "autotest_"
                + UUID.randomUUID()
                + "@example.test";
    }

    /**
     * Creates valid sample registration data.
     */
    public static Map<String, Object> getValidUserData() {

        Map<String, Object> userData = new HashMap<>();

        userData.put("name", "AutoTest User");
        userData.put("email", generateUniqueEmail());
        userData.put("password", "TestPassword123!");

        return userData;
    }

    /**
     * Creates invalid sample registration data.
     */
    public static Map<String, Object> getInvalidUserData() {

        Map<String, Object> userData = new HashMap<>();

        userData.put("name", "");
        userData.put("email", "invalid-email");
        userData.put("password", "");

        return userData;
    }
}
