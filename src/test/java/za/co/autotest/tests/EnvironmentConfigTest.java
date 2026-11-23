
package za.co.autotest.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

import za.co.autotest.config.EnvironmentConfig;

public class EnvironmentConfigTest {

    @Test
    public void shouldLoadDefaultEnvironment() {

        Assert.assertEquals(
                EnvironmentConfig.getEnvironment(),
                "local"
        );
    }

    @Test
    public void shouldLoadWebBaseUrl() {

        Assert.assertEquals(
                EnvironmentConfig.getWebBaseUrl(),
                "http://localhost:8080"
        );
    }

    @Test
    public void shouldLoadApiBaseUrl() {

        Assert.assertEquals(
                EnvironmentConfig.getApiBaseUrl(),
                "http://localhost:8080/api"
        );
    }

    @Test
    public void shouldLoadTimeout() {

        Assert.assertEquals(
                EnvironmentConfig.getTimeoutSeconds(),
                10
        );
    }
}
