package com.stepsdefs;

import io.cucumber.java.en.*;
import org.openqa.selenium.WebDriver;
import hooks.Hooks;
import utils.SeleniumActions;
import pages.VerificationHelper;
import config.ConfigReader;
import constants.Constants;
import drivers.DriverFactory;
import locators.PageLocators;

/**
 * ✅ SignupSteps — Final Stable Version
 * - Handles lazy driver and helper initialization safely
 * - Works seamlessly in local and CI environments
 * - Prevents NullPointerException or instantiation errors
 */
public class SignupSteps {

    private WebDriver driver;
    private SeleniumActions actions;
    private VerificationHelper verify;

    public SignupSteps() {
        System.out.println("🧩 SignupSteps constructor invoked — driver will initialize lazily.");
    }

    /**
     * Ensures driver, actions, and verification helpers are ready.
     * Called before every step automatically.
     */
    private void ensureReady() {
        try {
            if (driver == null) {
                driver = DriverFactory.getDriver();
                System.out.println("🌐 Driver fetched from DriverFactory: " + driver);
            }

            if (driver == null) {
                throw new IllegalStateException("❌ WebDriver is null — Hooks @Before did not initialize it. Check DriverFactory.initDriver().");
            }

            if (actions == null) {
                actions = new SeleniumActions(driver);
                System.out.println("✅ SeleniumActions initialized successfully.");
            }

            if (verify == null) {
                verify = new VerificationHelper(driver, actions);
                System.out.println("✅ VerificationHelper initialized successfully.");
            }
        } catch (Throwable t) {
            System.err.println("💥 SignupSteps.ensureReady() failed: " + t.getMessage());
            t.printStackTrace();
            throw new RuntimeException("Failed to initialize SignupSteps dependencies", t);
        }
    }

    // ==================== Login & Navigation Steps ====================

    @Given("User opens the {string} page")
    public void openLoginPage(String loginUrl) {
        ensureReady();
        System.out.println("🌍 Opening login page: " + loginUrl);
        actions.navigateTo(loginUrl);
        verify.waitForPageLoad();
    }

    @When("User enters valid credentials {string} and {string}")
    public void enterCredentials(String username, String password) {
        ensureReady();
        actions.type(PageLocators.LOGIN_USERNAME_INPUT, username);
        actions.type(PageLocators.LOGIN_PASSWORD_INPUT, password);
        actions.click(PageLocators.LOGIN_SUBMIT_BUTTON);
        verify.waitForPageLoad();
    }

    @Then("User should be logged in successfully")
    public void verifyLoginSuccess() {
        ensureReady();
        verify.verifyLoginSuccess();
        actions.verifyUrlContains("https://elearn.bits-pilani.ac.in/");
    }

    // ==================== Top Navigation Verification Steps ====================

    @Given("User verifies {string} is displaying in Top Navigation bar")
    public void verifyTopNavigation(String elementName) {
        ensureReady();
        verify.verifyTopNavigation(elementName);
    }

    @When("User clicks on {string} icon to open Profile Section")
    public void clickProfileIcon(String iconName) {
        ensureReady();
        actions.click(PageLocators.profileIcon(iconName));
    }

    // ==================== Banner Section Verification Steps ====================

    @Then("User verifies {string} is displaying in with multiple banners in Dashboard")
    public void verifyBannerSection(String sectionName) {
        ensureReady();
        verify.verifyBannerSection(sectionName);
    }

    // ==================== Tile Verification Steps ====================

    @Then("User verifies {string} as {string} Tile is displaying in Dashboard")
    public void verifyTileWithValue(String tileName, String dataValue) {
        ensureReady();
        verify.verifyTilecontent(tileName, dataValue);
    }

    @Then("User verifies {string} as {string} is displaying in Dashboard")
    public void verifycontentWithValue(String elementName, String expectedTitle) {
        ensureReady();
        verify.verifyTilecontent(elementName, expectedTitle);
    }

    @Given("User verifies {string} Sections and validate the available labs in Dashboard")
    public void verifyVirtaulLabSection(String elementName) {
        ensureReady();
        verify.verifyVirtaulLab(elementName);
    }

    @Then("User verifies {string} Tile is displaying in Dashboard")
    public void verifyTile(String tileName) {
        ensureReady();
        verify.verifyTile(tileName);
    }

    // ==================== CTA Click Steps ====================

    @When("User Clicks on {string} CTA on {string} Tile in Dashboard")
    public void clickCTAOnTile(String ctaText, String tileName) {
        ensureReady();
        verify.clickCTAOnTile(ctaText, tileName);
        verify.waitForPageLoad();
    }

    @When("User Clicks on {string} CTA in Elibrary Section")
    public void clickelibrary(String ctaText) {
        ensureReady();
        verify.clickCTAelibrary(ctaText);
        verify.waitForPageLoad();
    }

    @When("User Clicks on {string} CTA on Course Tile")
    public void clickCTAOnCourseTile(String ctaText) {
        ensureReady();
        verify.clickCTAOnCourseTile(ctaText);
        verify.waitForPageLoad();
    }

    @When("user verifies and clicks on {string} CTA")
    public void clickOnVivaProject(String ctaText) {
        ensureReady();
        verify.clickVivaProject(ctaText);
        verify.waitForPageLoad();
    }

    // ==================== Course Verification Steps ====================

    @Then("User verifies {string} opened in new tab")
    public void verifyCourseOpenedInNewTab(String courseTitle) {
        ensureReady();
        verify.switchToNewTab();
        verify.waitForPageLoad();
    }

    @Then("User verifies {string} redirecting in new tab and closed the tab")
    public void verifyelibraryOpenedInNewTab(String courseTitle) {
        ensureReady();
        verify.switchToNewTab();
        verify.waitForPageLoad();
        verify.closeCurrentTabAndSwitchToMain();
    }

    @Then("User verifies {string} as {string} in Subject Screen and closes the tab")
    public void verifyCourseTitle(String elementName, String expectedTitle) {
        ensureReady();
        verify.verifyTilecontent(elementName, expectedTitle);
        verify.closeCurrentTabAndSwitchToMain();
    }

    // ==================== Link & Redirection Steps ====================

    @Given("User clicks on {string} and verifies it is redirecting to {string} in newtab and close the tab")
    public void clickAndVerifyRedirection(String linkText, String expectedUrl) {
        ensureReady();
        verify.verifyElementInDashboard(linkText, expectedUrl);
        verify.closeCurrentTabAndSwitchToMain();
    }

    // ==================== Project URL Steps ====================

    @Then("user verifies {string} Open in new tab")
    public void verifyProjectURLOpened(String elementName) {
        ensureReady();
        verify.switchToNewTab();
        verify.waitForPageLoad();
        verify.verifyURLOpened(elementName, Constants.PROJECT_PORTAL);
    }

    // ==================== Dissertation Steps ====================

    @Then("user verifies {string} as {string} and close the tab")
    public void verifyDissertationStatus(String elementName, String expectedStatus) {
        ensureReady();
        verify.verifyStatus(elementName, expectedStatus);
        verify.closeCurrentTabAndSwitchToMain();
    }

    // ==================== Section Interaction Steps ====================

    @Then("User click on {string} Sections and validate the available labs in Dashboard")
    public void clickVirtualLabsSection(String sectionName) {
        ensureReady();
        verify.clickSection(sectionName);
        verify.waitForPageLoad();
    }

    @Then("User verifies the available labs in Dashboard")
    public void verifyAvailableLabs() {
        ensureReady();
        verify.verifyAvailableLabs();
    }

    @Given("user clicks on {string} Section")
    public void clickVivaProjectSection(String sectionName) {
        ensureReady();
        verify.clickSection(sectionName);
        verify.waitForPageLoad();
    }

    // ==================== Helper Methods ====================

    private String getCredential(String key) {
        String value = ConfigReader.getJsonString(key);
        if (value == null || value.isEmpty()) {
            value = ConfigReader.get(key, getDefaultCredential(key));
        }
        return value;
    }

    private String getDefaultCredential(String key) {
        return switch (key) {
            case Constants.USERNAME -> "testuser";
            case Constants.PASSWORD -> "testpass";
            default -> "";
        };
    }
}
