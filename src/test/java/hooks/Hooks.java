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
 * ✅ FINAL Hooks:
 * - Stepwise Base64 screenshots with Extent Reports
 * - Parallel-safe scenario tracking via ThreadLocal
 * - Full TestRail integration with run, result & defect management
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
                failureError.set(error);
                failureStep.set(stepText);
                ExtentReportManager.logStep("fail", stepText + " " + durationInfo, drv, error);
            } else {
                ExtentReportManager.logStep("pass", stepText + " " + durationInfo, drv, null);
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
        System.out.println("🧠 Starting Scenario: " + scenario.getName());
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
            ExtentReportManager.logFail(scenario.getName(), "⚠️ Scenario setup failed: " + e.getMessage());
        }
    }

    // ==========================================================
    // 🔹 After Each Scenario
    // ==========================================================
    @After
    public void afterScenario(Scenario scenario) {
        WebDriver drv = driver.get();
        boolean passed = !scenario.isFailed(); // keep a concrete flag for TestRail update

        try {
            // Log scenario result in Extent
            if (scenario.isFailed()) {
                passed = false;
                ExtentReportManager.logFail(scenario.getName(),
                        "❌ Scenario failed at step: " + scenario.getName());
                if (drv != null)
                    ExtentReportManager.captureAndAttachScreenshot(scenario.getName(), drv, "Failure Screenshot");
            } else {
                ExtentReportManager.logPass(scenario.getName(), "✅ Scenario passed successfully");
            }

            // ✅ Update TestRail
            if (testRailEnabled && testRailClient != null) {
                Integer caseId = extractCaseId(scenario);
                if (caseId != null) {
                    String comment = "**Scenario:** " + scenario.getName() +
                            "\n**Status:** " + (passed ? "✅ PASSED" : "❌ FAILED") +
                            "\n**Execution Time:** " + LocalDateTime.now()
                                    .format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                    Integer resultId = testRailClient.updateTestResult(caseId, passed, comment);

                 // Force flush before upload to ensure ExtentReport.html exists
                    ExtentReportManager.flushReports();

                    File extentFile = new File(ExtentReportManager.getReportPath());
                    if (extentFile.exists() && extentFile.length() > 1000) {
                        System.out.println("📤 Uploading ExtentReport.html to TestRail...");
                        testRailClient.uploadAttachmentToResult(resultId, extentFile);
                    } else {
                        System.err.println("⚠️ ExtentReport.html missing or empty — skipping TestRail upload.");
                    }
                } }}
         catch (Exception e) {
            System.err.println("⚠️ afterScenario error: " + e.getMessage());
        } finally {
            if (drv != null) DriverFactory.quitDriver();
            driver.remove();
            ExtentReportManager.clearCurrentScenario();
        }
    }
            

    // ==========================================================
    // 🔹 Utility Methods
    // ==========================================================
    private static File saveScreenshotToFile(byte[] bytes, String scenarioName) {
        try {
            File dir = new File("test-output/screenshots");
            if (!dir.exists()) dir.mkdirs();
            String fname = scenarioName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + System.currentTimeMillis() + ".png";
            File file = new File(dir, fname);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
            }
            return file;
        } catch (Exception e) {
            System.err.println("⚠️ Screenshot saving failed: " + e.getMessage());
            return null;
        }
    }

    private Integer extractCaseId(Scenario s) {
        for (String tag : s.getSourceTagNames())
            if (tag.matches("@CaseID_\\d+") || tag.matches("@C\\d+"))
                return Integer.parseInt(tag.replaceAll("[^0-9]", ""));
        return null;
    }

    private void createDetailedDefect(Scenario scenario, int caseId, int resultId) {
        Throwable error = failureError.get();
        String failedStep = failureStep.get();

        String failureType = analyzeFailureType(error);
        String priority = determinePriority(failureType);

        String failureMessage = buildFailureMessage(error, failedStep, failureType, priority);
        String stack = getStackTrace(error);

        testRailClient.createDefect("C" + caseId, scenario.getName(), failureMessage, stack);
    }

    private String buildFailureMessage(Throwable error, String step, String type, String pr) {
        return "📍 Failed Step: " + step + "\n📌 Type: " + type + "\n⚠️ Priority: " + pr +
                (error != null ? "\n\n❌ Error: " + error.getMessage() : "");
    }

    private String getStackTrace(Throwable e) {
        if (e == null) return "No stack trace";
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private String analyzeFailureType(Throwable e) {
        if (e == null) return "UNKNOWN";
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        String cls = e.getClass().getSimpleName().toLowerCase();
        if (msg.contains("nosuchelement")) return "ELEMENT_NOT_FOUND";
        if (msg.contains("timeout")) return "TIMEOUT";
        if (msg.contains("assert")) return "ASSERTION_FAILURE";
        return cls.toUpperCase();
    }

    private String determinePriority(String type) {
        return switch (type) {
            case "ELEMENT_NOT_FOUND", "TIMEOUT" -> "🔴 HIGH";
            case "ASSERTION_FAILURE" -> "🟡 MEDIUM";
            default -> "🟢 LOW";
        };
    }

    // ==========================================================
    // 🔹 After All
    // ==========================================================
    @AfterAll
    public static void afterAll() {
        try {
            System.out.println("📄 Finalizing Extent Reports...");
            ExtentReportManager.flushReports();
            if (testRailEnabled && runId > 0)
                testRailClient.closeTestRun();
        } catch (Exception ignored) {}
        System.out.println("💾 Extent Report flushed successfully at suite end.");
    }

}
