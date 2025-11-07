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
 * - Parallel execution supported
 * - Auto headless mode for CI/CD
 * - Hooks picks up same runId
 */
@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"com.stepsdefs", "hooks"},
    plugin = {
        "pretty",
        "html:test-output/CucumberReport.html",
        "json:test-output/CucumberReport.json"
    },
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

    @BeforeSuite(alwaysRun = true)
    public static void beforeSuite() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🚀 Starting Automated Cucumber + TestRail Suite");
        System.out.println("=".repeat(80));

        // ✅ Enable headless automatically for CI/CD
        if (System.getenv("CI") != null || System.getenv("GITHUB_ACTIONS") != null) {
            System.setProperty("headless", "true");
            System.out.println("🧠 CI/CD environment detected — running in headless mode");
        }

        ExtentReportManager.initReports();
    }

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
            ExtentReportManager.flushReports();
            System.out.println("✅ Extent Report flushed successfully.");
        }
    }

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

            // Load configuration (caseId or labels)
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
                // ✅ Run by specific case IDs
                caseIds = Arrays.stream(caseIdsFromProp.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(s -> s.replace("C", "").replace("c", ""))
                        .map(Integer::parseInt)
                        .distinct()
                        .toList();
            } else {
                // ✅ Run by label
                int projectId = Integer.parseInt(ConfigReader.get("testrail.projectId", "0"));
                int suiteId = Integer.parseInt(ConfigReader.get("testrail.suiteId", "0"));
                caseIds = client.getCaseIdsByLabel(projectId, suiteId, labelFromProp);
                if (caseIds.isEmpty()) {
                    throw new RuntimeException("❌ No test cases found for label: " + labelFromProp);
                }
            }

            selectedCaseIds.clear();
            selectedCaseIds.addAll(caseIds);

            String runName = "AutoRun - " + new Date();
            int runId = client.createTestRun(runName, caseIds);

            // ✅ Share runId globally so Hooks can reuse
            System.setProperty("testrail.runId", String.valueOf(runId));

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
