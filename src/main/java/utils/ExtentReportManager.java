package utils;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import config.ConfigReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * ✅ ExtentReportManager — Thread-Safe & Comprehensive Logging
 * -------------------------------------------------------
 * 🔹 Auto-initializes Extent report & embeds screenshots
 * 🔹 Thread-safe per-test ExtentTest for parallel execution
 * 🔹 Adds Base64 images inline for all steps
 * 🔹 Proper step-level pass/fail tracking
 */
public class ExtentReportManager {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> test = new ThreadLocal<>();
    private static final ThreadLocal<ExtentTest> currentStepNode = new ThreadLocal<>();

    private static final String SCREENSHOT_DIR = "test-output/screenshots/";
    private static final String EXTENT_PROPERTIES = "src/test/resources/extent.properties";

    // ==========================================================
    // 🔹 Initialize Reports (Singleton)
    // ==========================================================
    public static synchronized void initReports() {
        if (extent != null) return;

        System.out.println("🔧 Initializing Extent Reports...");
        try {
            Files.createDirectories(Paths.get(SCREENSHOT_DIR));

            Properties props = loadExtentProperties();
            String reportPath = props.getProperty("extent.reporter.spark.out", "test-output/ExtentReport.html");

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);

            // Load extent-config.xml if defined
            String configPath = props.getProperty("extent.reporter.spark.config");
            if (configPath != null && Files.exists(Paths.get(configPath))) {
                try {
                    spark.loadXMLConfig(configPath);
                    System.out.println("✅ Loaded extent-config.xml from: " + configPath);
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to load XML config: " + e.getMessage());
                    configureReporter(spark, props);
                }
            } else {
                configureReporter(spark, props);
            }

            extent = new ExtentReports();
            extent.attachReporter(spark);
            setSystemInfo(props);
            System.out.println("✅ Extent Reports initialized successfully");

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize Extent Report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Properties loadExtentProperties() {
        Properties props = new Properties();
        File file = new File(EXTENT_PROPERTIES);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                System.out.println("✅ Loaded extent.properties");
            } catch (IOException e) {
                System.err.println("⚠️ Could not load extent.properties: " + e.getMessage());
            }
        }
        return props;
    }

    private static void configureReporter(ExtentSparkReporter reporter, Properties props) {
        reporter.config().setTheme(
                "DARK".equalsIgnoreCase(props.getProperty("theme")) ? Theme.DARK : Theme.STANDARD);
        reporter.config().setReportName(props.getProperty("reportName", "Automation Execution Report"));
        reporter.config().setDocumentTitle(props.getProperty("documentTitle", "Execution Summary"));
        reporter.config().setEncoding("UTF-8");
        reporter.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");
    }

    private static void setSystemInfo(Properties props) {
        if (extent == null) return;
        extent.setSystemInfo("OS", System.getProperty("os.name"));
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        extent.setSystemInfo("User", System.getProperty("user.name"));
        extent.setSystemInfo("Execution Time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        try {
            extent.setSystemInfo("Browser", ConfigReader.get("browser", "chrome"));
            extent.setSystemInfo("Environment", ConfigReader.get("environment", "Test"));
        } catch (Exception ignored) {}
    }

    // ==========================================================
    // 🔹 Test Lifecycle
    // ==========================================================
    public static synchronized void createTest(String testName) {
        initReports();
        ExtentTest t = extent.createTest(testName);
        test.set(t);
        System.out.println("✅ ExtentTest created: " + testName);
    }

    public static ExtentTest getTest() {
        ExtentTest t = test.get();
        if (t == null) {
            System.err.println("⚠️ No active ExtentTest found! Creating default test...");
            createTest("Default Test - " + Thread.currentThread().getName());
            t = test.get();
        }
        return t;
    }

    public static synchronized void removeTest() {
        currentStepNode.remove();
        test.remove();
    }

    public static synchronized ExtentReports getExtent() {
        if (extent == null) initReports();
        return extent;
    }

    // ==========================================================
    // 🔹 Category Assignment
    // ==========================================================
    public static void assignCategory(String category) {
        ExtentTest t = getTest();
        if (t != null) {
            t.assignCategory(category);
        }
    }

    // ==========================================================
    // 🔹 Screenshot Capture
    // ==========================================================
    public static String captureScreenshotBase64(WebDriver driver) {
        try {
            if (driver == null) {
                System.err.println("⚠️ Driver is null, cannot capture screenshot");
                return "";
            }
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to capture screenshot: " + e.getMessage());
            return "";
        }
    }

    public static File captureScreenshotAsFile(WebDriver driver, String fileName) {
        try {
            if (driver == null) return null;
            
            Files.createDirectories(Paths.get(SCREENSHOT_DIR));
            
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String safeFileName = fileName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".png";
            Path filePath = Paths.get(SCREENSHOT_DIR, safeFileName);
            
            Files.write(filePath, screenshot);
            System.out.println("📸 Screenshot saved: " + filePath);
            return filePath.toFile();
            
        } catch (Exception e) {
            System.err.println("⚠️ Failed to save screenshot: " + e.getMessage());
            return null;
        }
    }

    // ==========================================================
    // 🔹 Step-Level Logging with Screenshots
    // ==========================================================
    public static synchronized void logStepPass(WebDriver driver, String step) {
        try {
            ExtentTest currentTest = getTest();
            if (currentTest == null) {
                System.err.println("⚠️ No active ExtentTest for PASS step: " + step);
                return;
            }

            String base64 = captureScreenshotBase64(driver);
            ExtentTest stepNode = currentTest.createNode("✅ " + step);
            
            if (base64 != null && !base64.isEmpty()) {
                stepNode.pass("Step executed successfully",
                        MediaEntityBuilder.createScreenCaptureFromBase64String(base64, step).build());
            } else {
                stepNode.pass("Step executed successfully");
            }
            
            currentStepNode.set(stepNode);
            System.out.println("✅ Logged PASS: " + step);
            
        } catch (Exception e) {
            System.err.println("⚠️ Failed to log PASS step: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized void logStepFail(WebDriver driver, String step, Throwable error) {
        try {
            ExtentTest currentTest = getTest();
            if (currentTest == null) {
                System.err.println("⚠️ No active ExtentTest for FAIL step: " + step);
                return;
            }

            String errorMsg = error != null ? error.getMessage() : "Unknown error";
            String base64 = captureScreenshotBase64(driver);
            
            ExtentTest stepNode = currentTest.createNode("❌ " + step);
            
            StringBuilder failMessage = new StringBuilder();
            failMessage.append("<b>Error:</b> ").append(errorMsg).append("<br>");
            
            if (error != null) {
                failMessage.append("<b>Exception Type:</b> ").append(error.getClass().getSimpleName()).append("<br>");
                
                // Add stack trace (first 5 lines)
                StackTraceElement[] stackTrace = error.getStackTrace();
                if (stackTrace != null && stackTrace.length > 0) {
                    failMessage.append("<b>Stack Trace:</b><br><pre>");
                    for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                        failMessage.append(stackTrace[i].toString()).append("\n");
                    }
                    failMessage.append("</pre>");
                }
            }
            
            if (base64 != null && !base64.isEmpty()) {
                stepNode.fail(failMessage.toString(),
                        MediaEntityBuilder.createScreenCaptureFromBase64String(base64, "Failure Screenshot").build());
            } else {
                stepNode.fail(failMessage.toString());
            }
            
            currentStepNode.set(stepNode);
            System.out.println("❌ Logged FAIL: " + step);
            
        } catch (Exception e) {
            System.err.println("⚠️ Failed to log FAIL step: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================================
    // 🔹 General Logging Methods
    // ==========================================================
    public static void logInfo(String message) {
        try {
            getTest().info("ℹ️ " + message);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to log INFO: " + e.getMessage());
        }
    }

    public static void logPass(String message) {
        try {
            getTest().pass("✅ " + message);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to log PASS: " + e.getMessage());
        }
    }

    public static void logFail(String message) {
        try {
            getTest().fail("❌ " + message);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to log FAIL: " + e.getMessage());
        }
    }

    public static void logWarning(String message) {
        try {
            getTest().warning("⚠️ " + message);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to log WARNING: " + e.getMessage());
        }
    }

    public static void logSkip(String message) {
        try {
            getTest().skip("⏭️ " + message);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to log SKIP: " + e.getMessage());
        }
    }

    // ==========================================================
    // 🔹 Attach Screenshot with Title
    // ==========================================================
    public static void captureAndAttachScreenshot(WebDriver driver, String title) {
        try {
            String base64 = captureScreenshotBase64(driver);
            if (base64 != null && !base64.isEmpty()) {
                getTest().info(title,
                        MediaEntityBuilder.createScreenCaptureFromBase64String(base64, title).build());
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to attach screenshot: " + e.getMessage());
        }
    }

    // ==========================================================
    // 🔹 Flush Reports
    // ==========================================================
    public static synchronized void flushReports() {
        if (extent != null) {
            extent.flush();
            System.out.println("✅ Extent Report flushed successfully");
            System.out.println("📄 Report Location: test-output/ExtentReport.html");
        }
    }
}