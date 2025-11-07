package utils;

import org.openqa.selenium.*;
import drivers.DriverFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility for capturing screenshots.
 * Returns relative path for embedding into Extent Reports.
 */
public class ScreenshotUtils {

    public static String captureScreenshot(String name) {
        try {
            WebDriver driver = DriverFactory.getDriver();
            if (driver == null) {
                System.err.println("⚠️ WebDriver is null. Cannot capture screenshot.");
                return null;
            }

            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String screenshotDir = "test-output/screenshots/";
            String screenshotName = name.replaceAll("[^a-zA-Z0-9_-]", "_") + "_" + timestamp + ".png";
            String fullPath = screenshotDir + screenshotName;

            File destFile = new File(fullPath);
            destFile.getParentFile().mkdirs();
            Files.copy(srcFile.toPath(), destFile.toPath());

            // Return relative path (for HTML report)
            return "../screenshots/" + screenshotName;

        } catch (IOException e) {
            System.err.println("⚠️ Failed to capture screenshot: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("⚠️ Unexpected error while capturing screenshot: " + e.getMessage());
            return null;
        }
    }
}
