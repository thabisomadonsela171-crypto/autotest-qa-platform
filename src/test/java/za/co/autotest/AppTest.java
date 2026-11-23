
package za.co.autotest;

import org.testng.Assert;
import org.testng.annotations.Test;

public class AppTest {

    @Test
    public void applicationClassShouldBeCreated() {

        App app = new App();

        Assert.assertNotNull(
            app,
            "The AutoTest QA Platform application should be created successfully."
        );
    }
}
