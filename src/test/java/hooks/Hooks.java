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
 * ✅ Complete Hooks with:
 * - Step-wise Base64 screenshots with highlighting
 * - Parallel-safe scenario tracking
 * - Full TestRail integration with defect management
 */
public class Hooks implements ConcurrentEventListener {

    private static Client testRailClient;
    private static boolean testRailEnabled = true;
    private static int runId = 0;

    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static final ThreadLocal<String> currentStep = new ThreadLocal<>();
    private static final ThreadLocal<Long> stepStartTime = new ThreadLocal<>();
    private static final ThreadLocal<Throwable> failureError = new ThreadLocal<>();
    private static final ThreadLocal<String> failureStep = new ThreadLocal<>();

    // ==========================================================
    // 🔹 Register step events
    // ==========================================================
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestStepStarted.class, this::onStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::onStepFinished);

        // Ensure ExtentReport initialized early
        ExtentReportManager.initReports();
    }

    private void onStepStarted(TestStepStarted event) {
        if (event.getTestStep() instanceof PickleStepTestStep step) {
            String stepText = step.getStep().getKeyword() + " " + step.getStep().getText();
            currentStep.set(stepText);
            stepStartTime.set(System.currentTimeMillis());
            System.out.println("📍 Step started: " + stepText);
        }
    }

    private void onStepFinished(TestStepFinished event) {
        if (!(event.getTestStep() instanceof PickleStepTestStep)) return;

        String stepText = Optional.ofNullable(currentStep.get()).orElse("Unnamed Step");
        WebDriver drv = driver.get();
        Throwable error = event.getResult().getError();

        try {
            long duration = stepStartTime.get() != null
                    ? (System.currentTimeMillis() - stepStartTime.get())
                    : 0;
            String durationInfo = String.format("(⏱ %d ms)", duration);

            if (error != null) {
                // 🔴 Step failed - capture failure info
                failureError.set(error);
                failureStep.set(stepText);
                ExtentReportManager.logStep("fail", stepText + " " + durationInfo, drv, error);

                // 🟩 NEW: inline screenshot for failed step
                ExtentReportManager.logStepInline(drv, "FAIL", "Inline Failure Screenshot", error);

                System.err.println("❌ Step failed: " + stepText);
                System.err.println("    Error: " + error.getMessage());
            } else {
                // ✅ Step passed
                ExtentReportManager.logStep("pass", stepText + " " + durationInfo, drv, null);

                // 🟩 NEW: inline screenshot for passed step
                ExtentReportManager.logStepInline(drv, "PASS", stepText + " (Inline Screenshot)");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error logging step: " + e.getMessage());
        }
    }


    // ==========================================================
    // 🔹 Before All
    // ==========================================================
    @BeforeAll
    public static synchronized void beforeAll() {
        ExtentReportManager.initReports();
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔧 INITIALIZING TESTRAIL RUN");
        System.out.println("=".repeat(80));

        try {
            testRailEnabled = !"false".equalsIgnoreCase(ConfigReader.get("testrail.enabled", "true"));
            if (!testRailEnabled) {
                System.out.println("⚠️ TestRail disabled in config.");
                return;
            }

            testRailClient = new Client();

            List<Integer> caseIds = new ArrayList<>();
            String label = ConfigReader.get("testrail.labels");

            if (label != null && !label.isEmpty()) {
                caseIds = testRailClient.getCaseIdsByLabel(
                        Integer.parseInt(ConfigReader.get("testrail.projectId")),
                        Integer.parseInt(ConfigReader.get("testrail.suiteId")),
                        label
                );
            }

            if (caseIds.isEmpty()) {
                String caseIdStr = ConfigReader.get("testrail.caseId");
                if (caseIdStr != null && !caseIdStr.isEmpty()) {
                    caseIds.add(Integer.parseInt(caseIdStr.replaceAll("[^0-9]", "")));
                }
            }

            if (caseIds.isEmpty()) {
                throw new RuntimeException("❌ No case IDs found for creating TestRail run.");
            }

            String runName = "AutoRun - " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            runId = testRailClient.createTestRun(runName, caseIds);
            System.out.println("✅ Created TestRail Run: R" + runId);

        } catch (Exception e) {
            System.err.println("❌ TestRail init failed: " + e.getMessage());
            testRailEnabled = false;
        }
    }

    // ==========================================================
    // 🔹 Before Each Scenario
    // ==========================================================
    @Before
    public void beforeScenario(Scenario scenario) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🧠 Starting Scenario: " + scenario.getName());
        System.out.println("=".repeat(80));
        
        // Clear previous scenario data
        failureError.remove();
        failureStep.remove();
        currentStep.remove();

        try {
            DriverFactory.initDriver();
            WebDriver drv = DriverFactory.getDriver();
            driver.set(drv);

            ExtentReportManager.createTest(scenario.getName());
            ExtentReportManager.setCurrentScenario(scenario.getName());
            ExtentReportManager.logInfo(scenario.getName(), "📋 Scenario started: " + scenario.getName());

            scenario.getSourceTagNames().forEach(tag ->
                    ExtentReportManager.assignCategory(scenario.getName(), tag.replace("@", "")));

        } catch (Exception e) {
            System.err.println("❌ Scenario setup failed: " + e.getMessage());
            ExtentReportManager.logFail(scenario.getName(), "⚠️ Scenario setup failed: " + e.getMessage());
        }
    }

    // ==========================================================
    // 🔹 After Each Scenario - WITH DEFECT CREATION
    // ==========================================================
    @After
    public void afterScenario(Scenario scenario) {
        WebDriver drv = driver.get();
        boolean passed = !scenario.isFailed();

        try {
            // Log scenario result in Extent
            if (scenario.isFailed()) {
                passed = false;
                System.err.println("\n❌ SCENARIO FAILED: " + scenario.getName());
                
                ExtentReportManager.logFail(scenario.getName(),
                        "❌ Scenario failed at step: " + failureStep.get());
                
                if (drv != null) {
                    ExtentReportManager.captureAndAttachScreenshot(scenario.getName(), drv, "Final Failure Screenshot");
                }
            } else {
                System.out.println("\n✅ SCENARIO PASSED: " + scenario.getName());
                ExtentReportManager.logPass(scenario.getName(), "✅ Scenario passed successfully");
            }

            // ✅ Update TestRail & Create Defect if Failed
            if (testRailEnabled && testRailClient != null) {
                Integer caseId = extractCaseId(scenario);
                if (caseId != null) {
                    System.out.println("\n📊 Updating TestRail for Case C" + caseId);
                    
                    String comment = buildTestRailComment(scenario, passed);
                    Integer resultId = testRailClient.updateTestResult(caseId, passed, comment);
                    
                    System.out.println("✅ TestRail result updated (Result ID: " + resultId + ")");

                    // 🐛 CREATE DEFECT IF FAILED
                    if (!passed && resultId != null) {
                        System.out.println("\n🐛 Test failed - Creating detailed defect in TestRail...");
                        createDetailedDefect(scenario, caseId, resultId);
                    }

                    // Force flush before upload to ensure ExtentReport.html exists
                    ExtentReportManager.flushReports();

                    // Upload Extent Report to TestRail
                    File extentFile = new File(ExtentReportManager.getReportPath());
                    if (extentFile.exists() && extentFile.length() > 1000) {
                        System.out.println("📤 Uploading ExtentReport.html to TestRail...");
                        boolean uploaded = testRailClient.uploadAttachmentToResult(resultId, extentFile);
                        if (uploaded) {
                            System.out.println("✅ ExtentReport uploaded successfully");
                        }
                    } else {
                        System.err.println("⚠️ ExtentReport.html missing or empty — skipping upload.");
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ afterScenario error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (drv != null) {
                System.out.println("🔒 Closing browser...");
                DriverFactory.quitDriver();
            }
            driver.remove();
            ExtentReportManager.clearCurrentScenario();
            
            System.out.println("=".repeat(80) + "\n");
        }
    }

    // ==========================================================
    // 🔹 Utility Methods
    // ==========================================================
    
    /**
     * Build TestRail comment with execution details
     */
    private String buildTestRailComment(Scenario scenario, boolean passed) {
        StringBuilder comment = new StringBuilder();
        comment.append("**Scenario:** ").append(scenario.getName()).append("\n");
        comment.append("**Status:** ").append(passed ? "✅ PASSED" : "❌ FAILED").append("\n");
        comment.append("**Execution Time:** ").append(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ).append("\n");
        
        if (!passed && failureStep.get() != null) {
            comment.append("**Failed Step:** ").append(failureStep.get()).append("\n");
        }
        
        if (!passed && failureError.get() != null) {
            comment.append("**Error:** ").append(failureError.get().getMessage()).append("\n");
        }
        
        return comment.toString();
    }

    /**
     * Extract TestRail case ID from scenario tags
     */
    private Integer extractCaseId(Scenario s) {
        for (String tag : s.getSourceTagNames()) {
            if (tag.matches("@CaseID_\\d+") || tag.matches("@C\\d+")) {
                return Integer.parseInt(tag.replaceAll("[^0-9]", ""));
            }
        }
        return null;
    }

    /**
     * Create detailed defect in TestRail with full context
     */
    private void createDetailedDefect(Scenario scenario, int caseId, int resultId) {
        try {
            Throwable error = failureError.get();
            String failedStep = failureStep.get();

            if (error == null) {
                System.out.println("⚠️ No error information available for defect creation");
                return;
            }

            String failureType = analyzeFailureType(error);
            String priority = determinePriority(failureType);

            String failureMessage = buildFailureMessage(error, failedStep, failureType, priority);
            String stack = getStackTrace(error);

            System.out.println("   📋 Failure Type: " + failureType);
            System.out.println("   ⚠️ Priority: " + priority);
            System.out.println("   📍 Failed Step: " + failedStep);

            boolean defectCreated = testRailClient.createDefect(
                "C" + caseId, 
                scenario.getName(), 
                failureMessage, 
                stack
            );

            if (defectCreated) {
                System.out.println("✅ Defect created successfully in TestRail");
            } else {
                System.err.println("❌ Failed to create defect in TestRail");
            }

        } catch (Exception e) {
            System.err.println("❌ Error creating defect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Build comprehensive failure message
     */
    private String buildFailureMessage(Throwable error, String step, String type, String priority) {
        StringBuilder msg = new StringBuilder();
        msg.append("═══════════════════════════════════════════════════\n");
        msg.append("🔴 TEST FAILURE DETAILS\n");
        msg.append("═══════════════════════════════════════════════════\n\n");
        msg.append("📍 Failed Step: ").append(step != null ? step : "Unknown").append("\n");
        msg.append("📌 Failure Type: ").append(type).append("\n");
        msg.append("⚠️ Priority: ").append(priority).append("\n");
        msg.append("⏰ Timestamp: ").append(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        )).append("\n\n");
        
        if (error != null) {
            msg.append("❌ Error Message:\n");
            msg.append("───────────────────────────────────────────────────\n");
            msg.append(error.getMessage()).append("\n");
            msg.append("───────────────────────────────────────────────────\n");
        }
        
        return msg.toString();
    }

    /**
     * Get full stack trace as string
     */
    private String getStackTrace(Throwable e) {
        if (e == null) return "No stack trace available";
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Analyze failure type from exception
     */
    private String analyzeFailureType(Throwable e) {
        if (e == null) return "UNKNOWN";
        
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        String cls = e.getClass().getSimpleName().toLowerCase();
        
        if (msg.contains("nosuchelement") || cls.contains("nosuchelement")) {
            return "ELEMENT_NOT_FOUND";
        }
        if (msg.contains("timeout") || cls.contains("timeout")) {
            return "TIMEOUT";
        }
        if (msg.contains("assertion") || msg.contains("assert") || cls.contains("assertion")) {
            return "ASSERTION_FAILURE";
        }
        if (msg.contains("stale") || cls.contains("stale")) {
            return "STALE_ELEMENT";
        }
        if (msg.contains("clickintercepted") || cls.contains("clickintercepted")) {
            return "CLICK_INTERCEPTED";
        }
        
        return cls.toUpperCase();
    }

    /**
     * Determine priority based on failure type
     */
    private String determinePriority(String type) {
        return switch (type) {
            case "ELEMENT_NOT_FOUND", "TIMEOUT", "STALE_ELEMENT" -> "🔴 HIGH";
            case "ASSERTION_FAILURE", "CLICK_INTERCEPTED" -> "🟡 MEDIUM";
            default -> "🟢 LOW";
        };
    }

    /**
     * Public method for SeleniumActions to set failure info
     */
    public static void setFailureInfo(String errorMessage, Throwable throwable) {
        failureError.set(throwable);
        
        // Extract step from current context if available
        String step = currentStep.get();
        if (step != null) {
            failureStep.set(step + " - " + errorMessage);
        } else {
            failureStep.set(errorMessage);
        }
        
        System.err.println("🔴 Failure captured: " + errorMessage);
    }

    // ==========================================================
    // 🔹 After All
    // ==========================================================
    @AfterAll
    public static void afterAll() {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("📄 Finalizing Test Suite...");
            System.out.println("=".repeat(80));
            
            ExtentReportManager.flushReports();
            
            if (testRailEnabled && runId > 0) {
                System.out.println("🏁 Closing TestRail Run R" + runId + "...");
                testRailClient.closeTestRun();
            }
            
            System.out.println("✅ Test suite completed successfully");
            System.out.println("=".repeat(80));
            
        } catch (Exception e) {
            System.err.println("⚠️ Error in afterAll: " + e.getMessage());
        }
    }
}