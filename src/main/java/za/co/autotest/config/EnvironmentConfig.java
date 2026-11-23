
package za.co.autotest.config;

/**
 * Stores configuration settings for the AutoTest QA Platform.
 */
public class EnvironmentConfig {

    // Environment name
    private static final String ENVIRONMENT =
            System.getProperty("autotest.environment", "local");

    // Web application URL
    private static final String WEB_BASE_URL =
            System.getProperty("autotest.web.url", "http://localhost:8080");

    // API application URL
    private static final String API_BASE_URL =
            System.getProperty("autotest.api.url", "http://localhost:8080/api");

    // Browser wait timeout
    private static final int TIMEOUT_SECONDS =
            Integer.parseInt(System.getProperty("autotest.timeout", "10"));

    private EnvironmentConfig() {
        // Prevent creating objects of this utility class.
    }

    public static String getEnvironment() {
        return ENVIRONMENT;
    }

    public static String getWebBaseUrl() {
        return WEB_BASE_URL;
    }

    public static String getApiBaseUrl() {
        return API_BASE_URL;
    }

    public static int getTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
}
