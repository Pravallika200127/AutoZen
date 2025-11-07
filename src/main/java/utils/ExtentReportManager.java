package utils;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ FINAL ExtentReportManager
 * - Fully thread-safe
 * - Works with Hooks.java (ThreadLocal scenario context)
 * - Captures stepwise Base64 screenshots
 * - Ensures non-empty ExtentReport
 */
public class ExtentReportManager {

    private static ExtentReports extent;
    private static final Map<String, ExtentTest> scenarioMap = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> currentScenarioName = new ThreadLocal<>();

    private static final String REPORT_PATH = System.getProperty("user.dir") + "/test-output/ExtentReport.html";
    private static final boolean CAPTURE_ALL_STEPS = true;

    // ==========================================================
    // 🔹 INIT REPORT
    // ==========================================================
    public static synchronized void initReports() {
        if (extent != null) return;

        try {
            Files.createDirectories(Paths.get("test-output/screenshots"));
            ExtentSparkReporter spark = new ExtentSparkReporter(REPORT_PATH);
            spark.config().setTheme(Theme.STANDARD);
            spark.config().setDocumentTitle("Automation Execution Report");
            spark.config().setReportName("Test Execution Summary");
            spark.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Environment", "QA");
            extent.setSystemInfo("Framework", "Cucumber + Selenium + TestRail");
            extent.setSystemInfo("Generated On", LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            System.out.println("✅ Extent Report initialized at " + REPORT_PATH);
        } catch (Exception e) {
            System.err.println("❌ Extent initialization failed: " + e.getMessage());
        }
    }

    // ==========================================================
    // 🔹 SCENARIO HANDLING
    // ==========================================================
    public static synchronized ExtentTest createTest(String name) {
        if (extent == null) initReports();
        if (scenarioMap.containsKey(name)) return scenarioMap.get(name);

        ExtentTest test = extent.createTest(name);
        scenarioMap.put(name, test);
        return test;
    }

    public static ExtentTest getScenario(String name) {
        if (name == null) return null;
        return scenarioMap.get(name.trim());
    }

    public static void setCurrentScenario(String name) {
        currentScenarioName.set(name);
    }

    public static void clearCurrentScenario() {
        currentScenarioName.remove();
    }

    private static ExtentTest getCurrentScenarioNode() {
        String name = currentScenarioName.get();
        if (name == null) return null;
        ExtentTest test = scenarioMap.get(name.trim());
        if (test == null) test = createTest(name.trim());
        return test;
    }

    // ==========================================================
    // 🔹 STEP LOGGING (with screenshots)
    // ==========================================================
    public static synchronized void logStep(String status, String stepText, WebDriver driver, Throwable error) {
        try {
            ExtentTest test = getCurrentScenarioNode();
            if (test == null) {
                System.err.println("⚠️ No active scenario found for step: " + stepText);
                return;
            }

            String base64 = null;
            if (driver != null && CAPTURE_ALL_STEPS) {
                try {
                    base64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
                } catch (Exception e) {
                    System.err.println("⚠️ Screenshot capture failed: " + e.getMessage());
                }
            }

            ExtentTest node = test.createNode(stepText);
            System.out.println("📋 Logging Extent Step: [" + status.toUpperCase() + "] " + stepText);

            switch (status.toLowerCase()) {
                case "fail" -> {
                    node.fail(error != null ? error.getMessage() : "❌ Step failed");
                    if (base64 != null) node.addScreenCaptureFromBase64String(base64, stepText);
                    if (error != null) node.fail(error);

                    // 🔥 ensure scenario node also marked failed
                    test.fail("❌ Scenario failed due to step: " + stepText);
                }
                case "pass" -> {
                    node.pass("✅ Step Passed");
                    if (base64 != null) node.addScreenCaptureFromBase64String(base64, stepText);
                }
                default -> node.info(stepText);
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error logging step: " + e.getMessage());
        }
    }

    // ==========================================================
    // 🔹 SCENARIO-LEVEL LOGGING
    // ==========================================================
    public static void logInfo(String scenario, String msg) {
        ExtentTest test = getScenario(scenario);
        if (test != null) test.info(msg);
    }

    public static void logPass(String scenario, String msg) {
        ExtentTest test = getScenario(scenario);
        if (test != null) test.pass(msg);
    }

    public static void logFail(String scenario, String msg) {
        ExtentTest test = getScenario(scenario);
        if (test != null) test.fail(msg);
    }

    public static void captureAndAttachScreenshot(String scenario, WebDriver driver, String caption) {
        try {
            if (driver == null) return;
            String base64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
            ExtentTest test = getScenario(scenario);
            if (test != null && base64 != null) {
                test.info("📸 " + caption)
                    .addScreenCaptureFromBase64String(base64, caption);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Screenshot attach failed: " + e.getMessage());
        }
    }

    public static void assignCategory(String scenario, String category) {
        ExtentTest test = getScenario(scenario);
        if (test != null) test.assignCategory(category);
    }

    // ==========================================================
    // 🔹 FLUSH REPORT
    // ==========================================================
    public static synchronized void flushReports() {
        try {
            if (extent != null) {
                extent.flush();
                Thread.sleep(1000); // ensure file system has time to write changes
                System.out.println("💾 Extent Report flushed successfully to: " + REPORT_PATH);

                // Ensure file exists and is non-empty
                java.io.File file = new java.io.File(REPORT_PATH);
                if (file.exists() && file.length() > 1000) {
                    System.out.println("📊 Verified ExtentReport.html (size: " + file.length() / 1024 + " KB)");
                } else {
                    System.err.println("⚠️ ExtentReport.html not found or empty after flush!");
                }
            } else {
                System.err.println("⚠️ ExtentReports instance is null — cannot flush");
            }
        } catch (Exception e) {
            System.err.println("❌ Error flushing Extent report: " + e.getMessage());
        }
    }


    // ==========================================================
    // 🔹 Utility
    // ==========================================================
    public static String getReportPath() {
        return REPORT_PATH;
    }
}
