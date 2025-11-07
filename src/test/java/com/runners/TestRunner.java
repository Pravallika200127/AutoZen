package com.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.DataProvider;

import utils.Client;
import utils.FeatureGenerator;
import utils.ExtentReportManager;
import config.ConfigReader;

import java.io.File;
import java.util.*;
import org.json.JSONObject;

/**
 * ✅ FINAL TestRunner — Unified TestRail + Cucumber + Extent Integration
 *
 * Features:
 * - Auto feature generation from TestRail by case IDs or labels
 * - Creates & reuses TestRail run
 * - Parallel execution supported (Thread-safe Extent)
 * - Auto headless mode for CI/CD
 * - Hooks picks up the same TestRail runId
 * - Stepwise Base64 screenshots with Extent integration
 */
@CucumberOptions(
    plugin = {
        "pretty",
        "html:test-output/cucumber-report.html",
        "json:test-output/cucumber.json",
        "timeline:test-output/",
        "hooks.Hooks"                     // ✅ Registers stepwise listener
    },
    features = "src/test/resources/features",
    		glue = {"com.stepsdefs", "hooks"},
    monochrome = true
)
public class TestRunner extends AbstractTestNGCucumberTests {

    private static Client client;
    private static final List<Integer> selectedCaseIds = new ArrayList<>();

    // ============================================================
    // 🔹 Static initializer: Generate features before suite start
    // ============================================================
    static {
        generateFeaturesBeforeCucumber();
    }

    // ============================================================
    // 🔹 BeforeSuite: Initialize Extent + TestRail Client
    // ============================================================
    @BeforeSuite(alwaysRun = true)
    public static void beforeSuite() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("🚀 Starting Automated Cucumber + TestRail Suite");
        System.out.println("=".repeat(100));

        try {
            // ✅ Detect CI/CD and set headless mode
            if (System.getenv("CI") != null || System.getenv("GITHUB_ACTIONS") != null) {
                System.setProperty("headless", "true");
                System.out.println("🧠 CI/CD environment detected — running in headless mode");
            }

            // ✅ Load configuration
            ConfigReader.loadProperties();

            // ✅ Initialize Extent Reports once globally
            ExtentReportManager.initReports();

            // ✅ Initialize TestRail Client
            client = new Client();
            System.out.println("🧩 TestRail Client initialized successfully");

        } catch (Exception e) {
            System.err.println("❌ Failed in beforeSuite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // 🔹 AfterSuite: Flush Extent + Close TestRail Run
    // ============================================================
    @AfterSuite(alwaysRun = true)
    public static void afterSuite() {
        try {
            if (client != null) {
                System.out.println("🏁 Closing TestRail run...");
                client.closeTestRun();
            }
        } catch (Exception e) {
            System.err.println("⚠️ Unable to close TestRail run: " + e.getMessage());
        } finally {
            try {
                ExtentReportManager.flushReports();
                System.out.println("✅ Extent Report flushed successfully.");
                System.out.println("📄 Report Location: " + ExtentReportManager.getReportPath());
            } catch (Exception ex) {
                System.err.println("⚠️ Error flushing Extent: " + ex.getMessage());
            }
        }
    }

    // ============================================================
    // 🔹 Data Provider: Parallel Execution Enabled
    // ============================================================
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }

    // ============================================================
    // 🔹 Feature Generation from TestRail
    // ============================================================
    private static void generateFeaturesBeforeCucumber() {
        try {
            System.out.println("\n🧩 Initializing TestRail Feature Generation...");
            ConfigReader.loadProperties();
            client = new Client();
            FeatureGenerator.cleanOldFeatures();

            // Load from config or system properties
            String caseIdsFromProp = Optional.ofNullable(System.getProperty("testrail.caseId"))
                    .orElse(ConfigReader.get("testrail.caseId"));
            String labelFromProp = Optional.ofNullable(System.getProperty("testrail.labels"))
                    .orElse(ConfigReader.get("testrail.labels"));

            if ((caseIdsFromProp == null || caseIdsFromProp.isBlank()) &&
                (labelFromProp == null || labelFromProp.isBlank())) {
                throw new RuntimeException("❌ You must provide either 'testrail.caseId' or 'testrail.labels'");
            }

            List<Integer> caseIds;
            if (caseIdsFromProp != null && !caseIdsFromProp.isBlank()) {
                caseIds = Arrays.stream(caseIdsFromProp.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(s -> s.replace("C", "").replace("c", ""))
                        .map(Integer::parseInt)
                        .distinct()
                        .toList();
            } else {
                int projectId = Integer.parseInt(ConfigReader.get("testrail.projectId", "0"));
                int suiteId   = Integer.parseInt(ConfigReader.get("testrail.suiteId", "0"));
                caseIds = client.getCaseIdsByLabel(projectId, suiteId, labelFromProp);
                if (caseIds.isEmpty()) {
                    throw new RuntimeException("❌ No test cases found for label: " + labelFromProp);
                }
            }

            selectedCaseIds.clear();
            selectedCaseIds.addAll(caseIds);

            // 🔹 Generate feature files (no run creation here)
            for (Integer caseId : caseIds) {
                JSONObject testCase = client.getTestCaseFromTestRail(caseId);
                FeatureGenerator.generateFeatureFile("C" + caseId, testCase);
            }

            System.out.println("✅ Feature generation completed successfully.");
        } catch (Exception e) {
            System.err.println("❌ Failed to generate feature files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    
}
