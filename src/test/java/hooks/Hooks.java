package hooks;

import io.cucumber.java.*;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import drivers.DriverFactory;
import config.ConfigReader;
import utils.Client;
import utils.ExtentReportManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ✅ Hooks - Complete TestRail Integration with Defect Creation
 * 
 * Features:
 * - Thread-safe execution for parallel tests
 * - Automatic TestRail result updates
 * - Detailed defect creation on failures
 * - Step-level screenshot capture
 * - Extent Report integration
 */
public class Hooks implements ConcurrentEventListener {

    private static Client testRailClient;
    private static boolean testRailEnabled = true;
    private static boolean captureAllSteps = true;
    private static int runId = 0;

    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static final ThreadLocal<String> currentStep = new ThreadLocal<>();
    private static final ThreadLocal<Throwable> failureError = new ThreadLocal<>();
    private static final ThreadLocal<String> failureStep = new ThreadLocal<>();

    // ==========================================================
    // 🔹 Register Cucumber event listeners
    // ==========================================================
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestStepStarted.class, this::onStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::onStepFinished);
    }

    private void onStepStarted(TestStepStarted event) {
        if (event.getTestStep() instanceof PickleStepTestStep step) {
            String stepText = step.getStep().getKeyword() + " " + step.getStep().getText();
            currentStep.set(stepText);
            System.out.println("📝 Executing Step: " + stepText);
        }
    }

    private void onStepFinished(TestStepFinished event) {
        WebDriver drv = driver.get();
        if (drv == null) return;

        String stepText = Optional.ofNullable(currentStep.get()).orElse("Unnamed Step");
        Throwable error = event.getResult().getError();

        try {
            if (error != null) {
                // Capture failure information
                failureError.set(error);
                failureStep.set(stepText);
                
                ExtentReportManager.logStepFail(drv, stepText, error);
                System.out.println("❌ Step Failed: " + stepText);
            } else if (captureAllSteps) {
                ExtentReportManager.logStepPass(drv, stepText);
                System.out.println("✅ Step Passed: " + stepText);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Step logging failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================================
    // 🔹 Before All (Initialize TestRail)
    // ==========================================================
    @BeforeAll
    public static void beforeAll() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔧 INITIALIZING TESTRAIL INTEGRATION");
        System.out.println("=".repeat(80));

        try {
            testRailEnabled = !"false".equalsIgnoreCase(ConfigReader.get("testrail.enabled", "true"));
            captureAllSteps = Boolean.parseBoolean(ConfigReader.get("screenshot.captureAllSteps", "true"));
            
            if (!testRailEnabled) {
                System.out.println("⚠️ TestRail integration disabled");
                return;
            }

            testRailClient = new Client();

            // Check if there's already an active run
            runId = testRailClient.getRunId();
            if (runId > 0) {
                System.out.println("♻️ Using existing TestRail Run: R" + runId);
                return;
            }

            // Create new run based on label or case IDs
            System.out.println("⚠️ No active run found; creating new run...");
            
            String labels = ConfigReader.get("testrail.labels");
            List<Integer> dynamicCases = new ArrayList<>();
            
            if (labels != null && !labels.isEmpty()) {
                dynamicCases = testRailClient.getCaseIdsByLabel(
                        Integer.parseInt(ConfigReader.get("testrail.projectId")),
                        Integer.parseInt(ConfigReader.get("testrail.suiteId")),
                        labels
                );
            } else {
                // Fallback: use specific case ID
                String caseIdStr = ConfigReader.get("testrail.caseId");
                if (caseIdStr != null && !caseIdStr.isEmpty()) {
                    dynamicCases.add(Integer.parseInt(caseIdStr.replace("C", "").replace("c", "")));
                }
            }

            if (!dynamicCases.isEmpty()) {
                String runName = "AutoRun - " + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                runId = testRailClient.createTestRun(runName, dynamicCases);
                System.out.println("✅ TestRail Run Created: R" + runId);
            } else {
                System.out.println("⚠️ No test cases found, skipping TestRail run creation.");
                testRailEnabled = false;
            }
        } catch (Exception e) {
            System.err.println("⚠️ TestRail setup failed: " + e.getMessage());
            e.printStackTrace();
            testRailEnabled = false;
        }
    }

    // ==========================================================
    // 🔹 Before Each Scenario
    // ==========================================================
    @Before
    public void beforeScenario(Scenario scenario) {
        System.out.println("\n🚀 Starting Scenario: " + scenario.getName());
        
        // Clear previous failure data
        failureError.remove();
        failureStep.remove();
        currentStep.remove();
        
        try {
            // Initialize driver
            DriverFactory.initDriver();
            driver.set(DriverFactory.getDriver());
            
            // Initialize Extent Report for this scenario
            ExtentReportManager.createTest(scenario.getName());
            ExtentReportManager.logInfo("📋 Scenario: " + scenario.getName());
            
            // Add tags as categories
            scenario.getSourceTagNames().forEach(tag ->
                    ExtentReportManager.assignCategory(tag.replace("@", "")));
            
            ExtentReportManager.logPass("✅ Browser initialized successfully");
            
        } catch (Exception e) {
            System.err.println("⚠️ Failed to start scenario setup: " + e.getMessage());
            e.printStackTrace();
            ExtentReportManager.logFail("❌ Scenario setup failed: " + e.getMessage());
        }
    }

    // ==========================================================
    // 🔹 After Each Scenario
    // ==========================================================
    @After
    public void afterScenario(Scenario scenario) {
        WebDriver drv = driver.get();
        boolean passed = !scenario.isFailed();
        Integer resultId = null;
        Integer caseId = null;

        try {
            // Log final scenario status to Extent
            if (passed) {
                ExtentReportManager.logPass("✅ Scenario passed successfully");
            } else {
                ExtentReportManager.logFail("❌ Scenario failed");
                
                // Capture final failure screenshot
                if (drv != null) {
                    ExtentReportManager.captureAndAttachScreenshot(drv, "🔍 Final Failure State");
                }
            }

            // Update TestRail
            if (testRailEnabled && testRailClient != null) {
                caseId = extractCaseId(scenario);
                if (caseId != null) {
                    System.out.println("\n📤 Updating TestRail result for C" + caseId + "...");
                    
                    String comment = buildTestResultComment(scenario, passed);
                    
                    // Save failure screenshot for TestRail upload
                    File failureScreenshot = null;
                    if (!passed && drv != null) {
                        try {
                            byte[] screenshot = ((TakesScreenshot) drv).getScreenshotAs(OutputType.BYTES);
                            failureScreenshot = saveScreenshotFile(screenshot, scenario.getName(), "failure");
                        } catch (Exception e) {
                            System.err.println("⚠️ Failed to capture failure screenshot: " + e.getMessage());
                        }
                    }
                    
                    // Update TestRail result
                    resultId = testRailClient.updateTestResult(caseId, passed, comment);
                    
                    if (resultId != null && resultId > 0) {
                        System.out.println("✅ TestRail Result Updated (Result ID: " + resultId + ")");
                        
                        // Upload failure screenshot
                        if (failureScreenshot != null && failureScreenshot.exists()) {
                            boolean uploaded = testRailClient.uploadExtentReportToResult(resultId, failureScreenshot);
                            if (uploaded) {
                                System.out.println("✅ Failure screenshot uploaded to TestRail");
                            }
                        }
                        
                        // Upload Extent Report
                        File extentFile = new File("test-output/ExtentReport.html");
                        if (extentFile.exists()) {
                            boolean uploaded = testRailClient.uploadExtentReportToResult(resultId, extentFile);
                            if (uploaded) {
                                System.out.println("✅ Extent Report uploaded to TestRail");
                            }
                        }
                        
                        // Create detailed defect if failed
                        if (!passed) {
                            createDetailedDefect(scenario, caseId, resultId);
                        }
                    } else {
                        System.err.println("❌ Failed to update TestRail result for C" + caseId);
                    }
                } else {
                    System.out.println("⚠️ No @CaseID tag found for scenario: " + scenario.getName());
                }
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Error in afterScenario: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup
            try {
                if (drv != null) {
                    DriverFactory.quitDriver();
                }
                driver.remove();
                currentStep.remove();
                failureError.remove();
                failureStep.remove();
            } catch (Exception e) {
                System.err.println("⚠️ Error during cleanup: " + e.getMessage());
            }
            ExtentReportManager.removeTest();
        }
    }

    // ==========================================================
    // 🔹 Create Detailed Defect
    // ==========================================================
    private void createDetailedDefect(Scenario scenario, int caseId, int resultId) {
        try {
            System.out.println("\n🐛 Creating detailed defect in TestRail...");
            
            Throwable error = failureError.get();
            String failedStep = failureStep.get();
            
            String failureType = analyzeFailureType(error);
            String priority = determinePriority(failureType);
            String title = generateDefectTitle(scenario, failureType);
            String description = generateDefectDescription(scenario, caseId, failureType, priority, error, failedStep);
            
            System.out.println("   Failure Type: " + failureType);
            System.out.println("   Priority: " + priority);
            System.out.println("   Failed Step: " + (failedStep != null ? failedStep : "Unknown"));
            
            boolean created = testRailClient.createDefect(title, description);
            
            if (created) {
                System.out.println("✅ Detailed defect created successfully in TestRail");
            } else {
                System.err.println("❌ Failed to create defect in TestRail");
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Error creating defect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Analyze failure type based on error
     */
    private String analyzeFailureType(Throwable error) {
        if (error == null) return "UNKNOWN_FAILURE";
        
        String errorMsg = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
        String errorClass = error.getClass().getSimpleName().toLowerCase();
        
        if (errorMsg.contains("nosuchelement") || errorMsg.contains("unable to locate element") || 
            errorClass.contains("nosuchelement")) {
            return "ELEMENT_NOT_FOUND";
        }
        if (errorMsg.contains("timeout") || errorMsg.contains("timed out") || 
            errorClass.contains("timeout")) {
            return "TIMEOUT_ISSUE";
        }
        if (errorMsg.contains("stale") || errorMsg.contains("stale element reference")) {
            return "STALE_ELEMENT";
        }
        if (errorMsg.contains("assertion") || errorMsg.contains("expected") || errorMsg.contains("mismatch")) {
            return "ASSERTION_FAILURE";
        }
        if (errorMsg.contains("clickable") || errorMsg.contains("not interactable")) {
            return "ELEMENT_NOT_CLICKABLE";
        }
        
        return "GENERAL_FAILURE";
    }

    /**
     * Determine priority based on failure type
     */
    private String determinePriority(String failureType) {
        return switch (failureType) {
            case "ELEMENT_NOT_FOUND", "TIMEOUT_ISSUE" -> "🔴 HIGH - Blocks Execution";
            case "ASSERTION_FAILURE", "ELEMENT_NOT_CLICKABLE" -> "🟡 MEDIUM - Needs Investigation";
            case "STALE_ELEMENT", "GENERAL_FAILURE" -> "🟢 LOW - Intermittent Issue";
            default -> "🟡 MEDIUM - Requires Analysis";
        };
    }

    /**
     * Generate defect title
     */
    private String generateDefectTitle(Scenario scenario, String failureType) {
        String prefix = switch (failureType) {
            case "ELEMENT_NOT_FOUND" -> "[LOCATOR ISSUE]";
            case "TIMEOUT_ISSUE" -> "[TIMEOUT]";
            case "ASSERTION_FAILURE" -> "[ASSERTION FAILED]";
            case "STALE_ELEMENT" -> "[STALE ELEMENT]";
            case "ELEMENT_NOT_CLICKABLE" -> "[INTERACTION ISSUE]";
            default -> "[TEST FAILURE]";
        };
        return prefix + " " + scenario.getName();
    }

    /**
     * Generate comprehensive defect description
     */
    private String generateDefectDescription(Scenario scenario, int caseId, String failureType, 
                                            String priority, Throwable error, String failedStep) {
        StringBuilder desc = new StringBuilder();
        
        desc.append("# 🐛 Automated Test Failure Report\n\n");
        desc.append("## 📊 Priority: ").append(priority).append("\n\n---\n\n");
        
        // Test Information
        desc.append("## 📋 Test Information\n");
        desc.append("- **Test Case ID:** C").append(caseId).append("\n");
        desc.append("- **Scenario:** ").append(scenario.getName()).append("\n");
        desc.append("- **Feature File:** ").append(scenario.getUri()).append("\n");
        desc.append("- **Tags:** ").append(scenario.getSourceTagNames()).append("\n");
        desc.append("- **Failure Type:** ").append(failureType).append("\n");
        desc.append("- **Failed At:** ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append("\n");
        desc.append("- **Failed Step:** ").append(failedStep != null ? failedStep : "Unknown").append("\n");
        desc.append("- **Browser:** ").append(ConfigReader.get("browser", "chrome")).append("\n");
        desc.append("- **Test Run:** R").append(runId).append("\n\n");
        
        // Error Details
        if (error != null) {
            desc.append("## ❌ Error Details\n");
            desc.append("**Exception Type:** `").append(error.getClass().getSimpleName()).append("`\n\n");
            desc.append("**Message:**\n```\n").append(error.getMessage() != null ? error.getMessage() : "No message").append("\n```\n\n");
            
            desc.append("**Stack Trace (Top 10 lines):**\n```\n");
            StringWriter sw = new StringWriter();
            error.printStackTrace(new PrintWriter(sw));
            String[] lines = sw.toString().split("\n");
            for (int i = 0; i < Math.min(10, lines.length); i++) {
                desc.append(lines[i]).append("\n");
            }
            if (lines.length > 10) {
                desc.append("... (").append(lines.length - 10).append(" more lines)\n");
            }
            desc.append("```\n\n");
        }
        
        // Root Cause Analysis
        desc.append("## 🔍 Root Cause Analysis\n");
        appendRootCauseAnalysis(desc, failureType, error);
        
        // Environment Details
        desc.append("\n## 🖥️ Environment Details\n");
        desc.append("- **Environment:** ").append(ConfigReader.get("environment", "Test")).append("\n");
        desc.append("- **OS:** ").append(System.getProperty("os.name")).append("\n");
        desc.append("- **Java Version:** ").append(System.getProperty("java.version")).append("\n");
        desc.append("- **User:** ").append(System.getProperty("user.name")).append("\n\n");
        
        // Reproduction Steps
        desc.append("## 🧪 Steps to Reproduce\n");
        desc.append("1. Open TestRail Run: **R").append(runId).append("**\n");
        desc.append("2. Navigate to Test Case: **C").append(caseId).append("**\n");
        desc.append("3. Review attached screenshots & Extent report\n");
        desc.append("4. Re-run locally using:\n");
        desc.append("   ```bash\n   mvn clean test -Dtestrail.caseId=\"C").append(caseId).append("\"\n   ```\n\n");
        
        // Attachments
        desc.append("## 📎 Attachments\n");
        desc.append("- 🖼️ Failure Screenshot (attached to TestRail result)\n");
        desc.append("- 📄 Extent Report (attached to TestRail result)\n\n");
        
        // Recommended Actions
        desc.append("## ✅ Recommended Actions\n");
        appendRecommendedActions(desc, failureType);
        
        desc.append("\n---\n_This defect was automatically generated by the Automation Framework_\n");
        
        return desc.toString();
    }

    /**
     * Append root cause analysis
     */
    private void appendRootCauseAnalysis(StringBuilder desc, String failureType, Throwable error) {
        switch (failureType) {
            case "ELEMENT_NOT_FOUND" -> desc.append("**Issue Type:** Element/Locator Not Found\n\n")
                    .append("**Probable Causes:**\n")
                    .append("- Locator (ID/XPath/CSS) has changed in the UI\n")
                    .append("- Element is not present on the page\n")
                    .append("- Page not fully loaded before element access\n")
                    .append("- Element in iframe or shadow DOM\n\n");
            case "TIMEOUT_ISSUE" -> desc.append("**Issue Type:** Timeout / Wait Condition Failed\n\n")
                    .append("**Probable Causes:**\n")
                    .append("- Page/Element taking longer than expected to load\n")
                    .append("- Network latency or API response delay\n")
                    .append("- Insufficient wait time configured\n")
                    .append("- Backend processing taking longer\n\n");
            case "ASSERTION_FAILURE" -> desc.append("**Issue Type:** Assertion/Validation Failed\n\n")
                    .append("**Probable Causes:**\n")
                    .append("- Expected value in test data is outdated\n")
                    .append("- Application logic or UI text changed\n")
                    .append("- Business rule updated without test sync\n")
                    .append("- Data inconsistency in test environment\n\n");
            case "STALE_ELEMENT" -> desc.append("**Issue Type:** Stale Element Reference\n\n")
                    .append("**Probable Causes:**\n")
                    .append("- DOM updated/refreshed after element was located\n")
                    .append("- Dynamic content reload (React/Vue/Angular)\n")
                    .append("- Missing re-location before action\n")
                    .append("- Page navigation during element interaction\n\n");
            case "ELEMENT_NOT_CLICKABLE" -> desc.append("**Issue Type:** Element Not Clickable/Interactable\n\n")
                    .append("**Probable Causes:**\n")
                    .append("- Element overlapped by another element (modal, popup)\n")
                    .append("- Element not visible or disabled\n")
                    .append("- Element outside viewport\n")
                    .append("- JavaScript not finished loading\n\n");
            default -> desc.append("**Issue Type:** General Test Failure\n\n")
                    .append("**Probable Causes:**\n")
                    .append("- Unexpected application behavior\n")
                    .append("- Test script needs update\n")
                    .append("- Browser/WebDriver compatibility issue\n")
                    .append("- Environment-specific problem\n\n");
        }
    }

    /**
     * Append recommended actions
     */
    private void appendRecommendedActions(StringBuilder desc, String failureType) {
        switch (failureType) {
            case "ELEMENT_NOT_FOUND" -> desc.append("1. Validate and update the locator strategy\n")
                    .append("2. Add proper explicit waits (WebDriverWait)\n")
                    .append("3. Check if element is in iframe or shadow DOM\n")
                    .append("4. Verify page URL and navigation flow\n");
            case "TIMEOUT_ISSUE" -> desc.append("1. Increase timeout durations\n")
                    .append("2. Optimize page load time (work with dev team)\n")
                    .append("3. Add proper synchronization points\n")
                    .append("4. Check network latency and API response times\n");
            case "ASSERTION_FAILURE" -> desc.append("1. Verify expected values in test data\n")
                    .append("2. Confirm with BA/QA about expected behavior\n")
                    .append("3. Update assertions or test data\n")
                    .append("4. Check for recent application changes\n");
            case "STALE_ELEMENT" -> desc.append("1. Re-locate element after DOM updates\n")
                    .append("2. Add retry mechanism for stale elements\n")
                    .append("3. Avoid storing WebElement references\n")
                    .append("4. Use fresh element lookup before each action\n");
            case "ELEMENT_NOT_CLICKABLE" -> desc.append("1. Scroll element into view before clicking\n")
                    .append("2. Close overlapping modals/popups\n")
                    .append("3. Use JavaScript click as fallback\n")
                    .append("4. Add wait for element to be clickable\n");
            default -> desc.append("1. Review screenshots and stack trace\n")
                    .append("2. Re-run test to confirm reproducibility\n")
                    .append("3. Check application logs\n")
                    .append("4. Escalate to development team if persistent\n");
        }
    }

    // ==========================================================
    // 🔹 Helper Methods
    // ==========================================================
    private Integer extractCaseId(Scenario scenario) {
        for (String tag : scenario.getSourceTagNames()) {
            if (tag.matches("@CaseID_\\d+") || tag.matches("@C\\d+")) {
                return Integer.parseInt(tag.replaceAll("[^0-9]", ""));
            }
        }
        return null;
    }

    private String buildTestResultComment(Scenario scenario, boolean passed) {
        StringBuilder comment = new StringBuilder();
        comment.append("**Automated Test Execution**\n\n");
        comment.append("**Scenario:** ").append(scenario.getName()).append("\n");
        comment.append("**Status:** ").append(passed ? "✅ PASSED" : "❌ FAILED").append("\n");
        comment.append("**Executed:** ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        comment.append("**Browser:** ").append(ConfigReader.get("browser", "chrome")).append("\n");
        comment.append("**Test Run:** R").append(runId).append("\n");
        
        if (!passed) {
            String failedStep = failureStep.get();
            Throwable error = failureError.get();
            
            if (failedStep != null) {
                comment.append("\n**Failed Step:** ").append(failedStep).append("\n");
            }
            if (error != null) {
                comment.append("\n**Error:**\n```\n").append(error.getMessage()).append("\n```\n");
            }
        }
        
        return comment.toString();
    }

    private File saveScreenshotFile(byte[] screenshot, String scenarioName, String type) {
        try {
            File dir = new File("test-output/screenshots");
            if (!dir.exists()) dir.mkdirs();
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = type + "_" + scenarioName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".png";
            File file = new File(dir, fileName);
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(screenshot);
            }
            
            return file;
        } catch (Exception e) {
            System.err.println("⚠️ Failed to save screenshot: " + e.getMessage());
            return null;
        }
    }

    // ==========================================================
    // 🔹 After All
    // ==========================================================
    @AfterAll
    public static void afterAll() {
        try {
            System.out.println("\n🏁 Finalizing test execution...");
            
            // Flush Extent Reports
            ExtentReportManager.flushReports();
            
            // Close TestRail run
            if (testRailEnabled && testRailClient != null && runId > 0) {
                System.out.println("\n🏁 Closing TestRail run R" + runId + "...");
                testRailClient.closeTestRun();
                System.out.println("✅ TestRail Run R" + runId + " closed successfully");
            }
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("✅ TEST EXECUTION COMPLETED");
            System.out.println("=".repeat(80));
            
        } catch (Exception e) {
            System.err.println("⚠️ Error in afterAll: " + e.getMessage());
            e.printStackTrace();
        }
    }
}