package utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import com.aventstack.extentreports.MediaEntityBuilder;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced ExtentReportManager with step-wise screenshot support
 * Thread-safe for parallel execution
 */
public class ExtentReportManager {

    private static ExtentReports extent;
    private static ExtentSparkReporter sparkReporter;
    private static final Map<String, ExtentTest> testMap = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> currentScenario = new ThreadLocal<>();
    private static String reportPath;

    /**
     * Initialize Extent Reports
     * ✅ Keeps all original logic
     * ✅ Adds support for inline <img> rendering in Spark report
     * ✅ Works with Extent 4.x / 5.x (no need for setCSS())
     */
    public static synchronized void initReports() {
        if (extent != null) {
            return; // Already initialized
        }

        try {
            // ==========================================================
            // 🟩 STEP 1: Disable HTML escaping BEFORE reporter creation
            // ==========================================================
            System.setProperty("extent.reporter.spark.out", "test-output/ExtentReport.html");
            System.setProperty("extent.reporter.spark.start", "true");
            System.setProperty("extent.reporter.spark.escape-html", "false"); // critical for inline <img> tags

            // ==========================================================
            // 🟩 STEP 2: Create report file with timestamp
            // ==========================================================
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            reportPath = System.getProperty("user.dir")
                    + "/test-output/ExtentReport_" + timestamp + ".html";

            // ==========================================================
            // 🟩 STEP 3: Initialize Spark reporter and configure basics
            // ==========================================================
            sparkReporter = new ExtentSparkReporter(reportPath);
            sparkReporter.config().setDocumentTitle("🧪 Automation Test Report");
            sparkReporter.config().setReportName("TestRail Integration Report");
            sparkReporter.config().setTheme(Theme.STANDARD);
            sparkReporter.config().setEncoding("utf-8");
            sparkReporter.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");

            // ==========================================================
            // 🟩 STEP 4: Initialize ExtentReports and attach reporter
            // ==========================================================
            extent = new ExtentReports();
            extent.attachReporter(sparkReporter);

            // ==========================================================
            // 🟩 STEP 5: Add environment/system info (kept from original)
            // ==========================================================
            extent.setSystemInfo("🖥️ OS", System.getProperty("os.name"));
            extent.setSystemInfo("👤 User", System.getProperty("user.name"));
            extent.setSystemInfo("☕ Java Version", System.getProperty("java.version"));
            extent.setSystemInfo("🌐 Environment", System.getProperty("env", "QA"));
            extent.setSystemInfo("🔧 Framework", "Cucumber + TestNG + Selenium");
            extent.setSystemInfo("📅 Execution Date",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")));

            System.out.println("✅ Extent Report initialized at: " + reportPath);

            // ==========================================================
            // 🟩 STEP 6: Append custom CSS at shutdown (safe fallback)
            // ==========================================================
            // This adds border/spacing for images without needing setCSS()
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    File file = new File(reportPath);
                    if (file.exists()) {
                        java.nio.file.Files.writeString(
                                file.toPath(),
                                "\n<style>img{border:1px solid #ccc;border-radius:8px;margin-top:5px;margin-bottom:10px;width:70%;}</style>\n",
                                java.nio.file.StandardOpenOption.APPEND);
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to append custom CSS: " + e.getMessage());
                }
            }));

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize Extent Reports: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Create a new test in the report
     */
    public static synchronized void createTest(String testName) {
        if (extent == null) {
            initReports();
        }

        ExtentTest test = extent.createTest(testName);
        testMap.put(testName, test);
        System.out.println("📝 Created test in Extent: " + testName);
    }

    /**
     * Set current scenario name (thread-safe)
     */
    public static void setCurrentScenario(String scenarioName) {
        currentScenario.set(scenarioName);
    }

    /**
     * Clear current scenario
     */
    public static void clearCurrentScenario() {
        currentScenario.remove();
    }

    /**
     * Get current test from thread-local scenario
     */
    private static ExtentTest getCurrentTest() {
        String scenario = currentScenario.get();
        if (scenario != null && testMap.containsKey(scenario)) {
            return testMap.get(scenario);
        }
        return null;
    }

    /**
     * ✨ ENHANCED: Log step with Base64 screenshot INLINE
     * Each screenshot appears immediately after its action description
     */
    /**
     * Logs step message with inline screenshot (always appears below the message)
     */
    public static void logStepWithScreenshot(String status, String message, String base64Screenshot) {
        ExtentTest test = getCurrentTest();
        if (test == null) {
            System.err.println("⚠️ No active test to log step with screenshot");
            return;
        }

        try {
            Status stepStatus = Status.valueOf(status.toUpperCase());

            if (base64Screenshot != null && !base64Screenshot.isEmpty()) {
                String imgTag = "<br><img src='data:image/png;base64," + base64Screenshot + "' " +
                                "alt='screenshot' width='70%' style='border:1px solid #ccc; border-radius:8px; margin-top:5px; margin-bottom:10px;'/>";

                test.log(stepStatus, message + imgTag);
            } else {
                test.log(stepStatus, message);
            }

            System.out.println("📋 Logged step with inline screenshot: " + message);
        } catch (Exception e) {
            System.err.println("⚠️ Error logging step with screenshot: " + e.getMessage());
        }
    }


    /**
     * Log a step (pass/fail/info) with optional screenshot
     */
    public static void logStep(String status, String message, WebDriver driver, Throwable error) {
        ExtentTest test = getCurrentTest();
        if (test == null) {
            System.err.println("⚠️ No active test to log step: " + message);
            return;
        }

        try {
            Status stepStatus = Status.valueOf(status.toUpperCase());
            
            if (stepStatus == Status.FAIL && error != null) {
                // Log failure with error details
                test.log(stepStatus, message);
                test.log(stepStatus, "❌ Error: " + error.getMessage());
                test.log(stepStatus, "<pre>" + getStackTrace(error) + "</pre>");
                
                // Capture screenshot on failure
                if (driver != null) {
                    captureAndAttachScreenshot(currentScenario.get(), driver, "Failure Screenshot");
                }
            } else {
                test.log(stepStatus, message);
            }

            System.out.println("📋 Logging Extent Step: [" + status.toUpperCase() + "] " + message);

        } catch (Exception e) {
            System.err.println("⚠️ Error logging step: " + e.getMessage());
        }
    }

    /**
     * Log info message
     */
    public static void logInfo(String testName, String message) {
        ExtentTest test = testMap.get(testName);
        if (test != null) {
            test.info(message);
        }
    }

    /**
     * Log pass message
     */
    public static void logPass(String testName, String message) {
        ExtentTest test = testMap.get(testName);
        if (test != null) {
            test.pass(MarkupHelper.createLabel(message, ExtentColor.GREEN));
        }
    }

    /**
     * Log fail message
     */
    public static void logFail(String testName, String message) {
        ExtentTest test = testMap.get(testName);
        if (test != null) {
            test.fail(MarkupHelper.createLabel(message, ExtentColor.RED));
        }
    }

    /**
     * Log warning message
     */
    public static void logWarning(String testName, String message) {
        ExtentTest test = testMap.get(testName);
        if (test != null) {
            test.warning(MarkupHelper.createLabel(message, ExtentColor.ORANGE));
        }
    }

    /**
     * Assign category/tag to test
     */
    public static void assignCategory(String testName, String category) {
        ExtentTest test = testMap.get(testName);
        if (test != null) {
            test.assignCategory(category);
        }
    }

    /**
     * Capture and attach screenshot to current test
     */
    public static void captureAndAttachScreenshot(String testName, WebDriver driver, String screenshotName) {
        ExtentTest test = testMap.get(testName);
        if (test == null || driver == null) {
            System.err.println("⚠️ Cannot capture screenshot - test or driver is null");
            return;
        }

        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            String base64Screenshot = ts.getScreenshotAs(OutputType.BASE64);
            
            test.addScreenCaptureFromBase64String(base64Screenshot, screenshotName);
            System.out.println("📸 Screenshot attached to test: " + testName);

        } catch (Exception e) {
            System.err.println("⚠️ Failed to capture screenshot: " + e.getMessage());
        }
    }

    /**
     * Attach screenshot from file path
     */
    public static void attachScreenshot(String testName, String screenshotPath) {
        ExtentTest test = testMap.get(testName);
        if (test != null) {
            try {
                test.addScreenCaptureFromPath(screenshotPath);
                System.out.println("📸 Screenshot attached from path: " + screenshotPath);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to attach screenshot: " + e.getMessage());
            }
        }
    }

    /**
     * Flush and finalize report
     */
    public static synchronized void flushReports() {
        if (extent != null) {
            extent.flush();
            
            // Verify report file exists and has content
            File reportFile = new File(reportPath);
            if (reportFile.exists() && reportFile.length() > 0) {
                long sizeKB = reportFile.length() / 1024;
                System.out.println("💾 Extent Report flushed successfully to: " + reportPath);
                System.out.println("📊 Verified ExtentReport.html (size: " + sizeKB + " KB)");
            } else {
                System.err.println("⚠️ ExtentReport.html not created or is empty!");
            }
        }
    }

    /**
     * Get report file path
     */
    public static String getReportPath() {
        return reportPath;
    }

    /**
     * Get stack trace as string
     */
    private static String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            
            // Limit stack trace to first 10 elements for readability
            if (sb.length() > 2000) {
                sb.append("\t... (truncated)");
                break;
            }
        }
        
        return sb.toString();
    }

    /**
     * Create a collapsible section in report
     */
    public static void createCollapsibleSection(String testName, String title, String content) {
        ExtentTest test = testMap.get(testName);
        if (test != null) {
            String html = "<details><summary><b>" + title + "</b></summary><pre>" + content + "</pre></details>";
            test.info(html);
        }
    }

    /**
     * Add a table to the report
     */
    public static void addTable(String testName, String[][] data) {
        ExtentTest test = testMap.get(testName);
        if (test != null && data != null && data.length > 0) {
            StringBuilder table = new StringBuilder("<table style='border-collapse:collapse;width:100%'>");
            
            // Header row
            table.append("<tr style='background-color:#4CAF50;color:white'>");
            for (String header : data[0]) {
                table.append("<th style='border:1px solid #ddd;padding:8px'>").append(header).append("</th>");
            }
            table.append("</tr>");
            
            // Data rows
            for (int i = 1; i < data.length; i++) {
                table.append("<tr>");
                for (String cell : data[i]) {
                    table.append("<td style='border:1px solid #ddd;padding:8px'>").append(cell).append("</td>");
                }
                table.append("</tr>");
            }
            
            table.append("</table>");
            test.info(table.toString());
        }
    }
    /**
     * ✅ FIXED VERSION — Displays inline screenshot correctly in Spark HTML
     */
    public static void logStepInline(WebDriver driver, String status, String message) {
        ExtentTest test = getCurrentTest();
        if (test == null) return;

        try {
            Status stepStatus = Status.valueOf(status.toUpperCase());
            if (driver != null) {
                String base64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);

                // Embed screenshot as HTML IMG tag — this always displays inline
                String imgTag = "<br><img src='data:image/png;base64," + base64 + "' " +
                                "alt='screenshot' width='70%' style='border:1px solid #ddd; border-radius:8px; margin-top:5px; margin-bottom:10px;'/>";

                test.log(stepStatus, message + imgTag);
            } else {
                test.log(stepStatus, message);
            }
            System.out.println("📸 Inline image logged for step: " + message);
        } catch (Exception e) {
            System.err.println("⚠️ logStepInline failed: " + e.getMessage());
        }
    }

    /**
     * ✅ FIXED VERSION — Displays inline screenshot on failures
     */
    public static void logStepInline(WebDriver driver, String status, String message, Throwable error) {
        ExtentTest test = getCurrentTest();
        if (test == null) return;

        try {
            Status stepStatus = Status.valueOf(status.toUpperCase());
            StringBuilder msg = new StringBuilder();
            msg.append(message);
            if (error != null) {
                msg.append("<br><b style='color:red'>❌ Error:</b> ").append(error.getMessage());
            }

            if (driver != null) {
                String base64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
                String imgTag = "<br><img src='data:image/png;base64," + base64 + "' " +
                                "alt='failure' width='70%' style='border:2px solid red; border-radius:8px; margin-top:5px; margin-bottom:10px;'/>";

                msg.append(imgTag);
            }

            test.log(stepStatus, msg.toString());
            System.out.println("📋 Inline failure image logged: " + message);
        } catch (Exception e) {
            System.err.println("⚠️ logStepInline (error) failed: " + e.getMessage());
        }
    }


}