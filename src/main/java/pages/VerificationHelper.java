package pages;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import constants.Constants;
import locators.PageLocators;
import utils.SeleniumActions;

public class VerificationHelper {
    
    private WebDriver driver;
    private static SeleniumActions actions;
    private String mainWindow;
    
    public VerificationHelper(WebDriver driver, SeleniumActions actions) {
        this.driver = driver;
        this.actions = actions;
    }

    // ==================== Highlight & Scroll Utility Methods ====================
    
    /**
     * Highlight element with yellow border
     */
    private void highlightElement(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].setAttribute('style', 'border: 3px solid yellow; box-shadow: 0 0 10px yellow;');", element);
        } catch (Exception e) {
            System.err.println("⚠️ Could not highlight element: " + e.getMessage());
        }
    }
    
    /**
     * Remove highlight from element
     */
    private void unhighlightElement(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].setAttribute('style', '');", element);
        } catch (Exception e) {
            System.err.println("⚠️ Could not unhighlight element: " + e.getMessage());
        }
    }
    
    /**
     * Scroll element to center of view
     */
    private void scrollToCenter(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center', inline: 'center'});", element);
            Thread.sleep(500); // Wait for smooth scroll to complete
        } catch (Exception e) {
            System.err.println("⚠️ Could not scroll to element: " + e.getMessage());
        }
    }
    
    /**
     * Highlight, scroll, and wait for screenshot - then unhighlight
     */
    private void highlightAndCapture(WebElement element) {
        try {
            scrollToCenter(element);
            highlightElement(element);
            Thread.sleep(3000); // Wait 3 seconds for screenshot
            unhighlightElement(element);
        } catch (Exception e) {
            System.err.println("⚠️ Error during highlight and capture: " + e.getMessage());
        }
    }
    
    /**
     * Overloaded method for By locator
     */
    private void highlightAndCapture(By locator) {
        try {
            WebElement element = driver.findElement(locator);
            highlightAndCapture(element);
        } catch (Exception e) {
            System.err.println("⚠️ Could not find element for highlighting: " + e.getMessage());
        }
    }

    // ==================== Main Verification Methods with Switch Cases ====================
    
    /**
     * Verify top navigation elements
     */
    public boolean verifyTopNavigation(String elementName) {
        System.out.println("🔍 Verifying top navigation: " + elementName);
        switch(elementName.toLowerCase()) {
            case "bits_logo":
                return verifyElementDisplayed(PageLocators.BITS_LOGO, elementName);
                
            case "student name":
                return verifyElementDisplayed(PageLocators.STUDENT_NAME, elementName);
                
            default:
                System.err.println("❌ Unknown navigation element: " + elementName);
                return false;
        }
    }
    
    /**
     * Verify virtual lab elements
     */
    public boolean verifyVirtaulLab(String elementName) {
        System.out.println("🔍 Verifying Virtual Lab: " + elementName);
        switch(elementName.toLowerCase()) {
            case Constants.MY_VIRTUAL_LABS:
                return verifyElementDisplayed(PageLocators.VIRTUAL_LABS_SECTION, elementName);
                
            default:
                System.err.println("❌ Unknown navigation element: " + elementName);
                return false;
        }
    }
    
    /**
     * Verify banner section
     */
    public boolean verifyBannerSection(String sectionName) {
        System.out.println("🔍 Verifying banner section: " + sectionName);
        switch(sectionName.toLowerCase()) {
            case "banner section":
                return verifyElementDisplayed(PageLocators.BANNER_SECTION, sectionName);
                
            default:
                return verifyElementDisplayed(PageLocators.BANNER_SECTION, sectionName);
        }
    }
    
    /**
     * Verify tiles in dashboard
     */
    public boolean verifyTile(String tileName) {
        System.out.println("🔍 Verifying tile: " + tileName);
        String dataKey = mapTileNameToDataKey(tileName);
        
        switch(tileName.toLowerCase()) {
            case "my academics":
                return verifyTileByDataAttribute(Constants.MY_ACADEMICS, tileName);
                
            case "examinations":
                return verifyTileByDataAttribute(Constants.EXAMINATIONS, tileName);
                
            case "student services":
                return verifyTileByDataAttribute(Constants.STUDENT_SERVICES, tileName);
                
            case "wilp policies":
                return verifyTileByDataAttribute(Constants.WILP_POLICIES, tileName);
                
            case "student support":
                return verifyTileByDataAttribute(Constants.STUDENT_SUPPORT, tileName);
                
            case "my courses":
                return verifyTileByDataAttribute(Constants.MY_COURSES, tileName);
                
            default:
                if(dataKey != null) {
                    return verifyTileByDataAttribute(dataKey, tileName);
                }
                return verifyElementDisplayed(PageLocators.elementByText(tileName), tileName);
        }
    }
    
    /**
     * Verify tile with specific data value
     */
    public void verifyTilecontent(String tileName, String dataValue) {
        By element = null;
        try {
            switch (tileName) {
                case Constants.MY_ACADEMICS:
                    element = PageLocators.MY_ACADEMICS_TILE;
                    break;
                case Constants.EXAMINATIONS:
                    element = PageLocators.EXAMINATIONS_TILE;
                    break;
                case Constants.STUDENT_SERVICES:
                    element = PageLocators.STUDENT_SERVICES_TILE;
                    break;
                case Constants.WILP_POLICIES:
                    element = PageLocators.WILP_POLICIES_TILE;
                    break;
                case Constants.STUDENT_SUPPORT:
                    element = PageLocators.STUDENT_SUPPORT_TILE;
                    break;
                case Constants.MY_COURSES:
                    element = PageLocators.MY_COURSES_TITLE;
                    break;
                case Constants.AVAILABLE_COURSE:
                    element = PageLocators.AVAILABLE_COURSES_TITLE;
                    break;
                case Constants.COURSE_TITLE:
                    element = PageLocators.COURSE_TITLE;
                    break;
                case Constants.ELIBRARY_URL:
                    element = PageLocators.ELIBRARY_SECTION_BUTTON;
                    break;
                case Constants.DISSERTATION_RATING:
                    element = PageLocators.DISSERTATION_STATUS;
                    break;
                default:
                    System.out.println("Component not recognized");
            }
            
            if (element != null) {
                actions.scrollToElement(element);
                highlightAndCapture(element);
                actions.verifyTextEquals(element, dataValue);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error verifying tile content: " + e.getMessage());
        }
    }
    
    /**
     * Verify elements in dashboard
     */
    public void verifyElementInDashboard(String elementName, String expectedUrl) {
        By element = null;
        try {
            switch (elementName) {
                case Constants.ELIBRARY_URL:
                    element = PageLocators.ELIBRARY_SECTION_BUTTON;
                    break;
                default:
                    System.out.println("Component not recognized");
            }
            
            if (element != null) {
                actions.scrollToElement(element);
                highlightAndCapture(element);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error verifying dashboard element: " + e.getMessage());
        }
    }
    
    /**
     * Verify course title
     */
    public boolean verifyCourseTitle(String elementName, String expectedTitle) {
        System.out.println("🔍 Verifying course title: " + expectedTitle);
        switch(elementName.toLowerCase()) {
            case "course title":
                return verifyCourseTitleInSubjectScreen(expectedTitle);
                
            default:
                return verifyElementDisplayed(PageLocators.courseTitle(expectedTitle), expectedTitle);
        }
    }
    
    /**
     * Verify redirection URLs
     */
    public boolean verifyRedirection(String linkText, String expectedUrl) {
        System.out.println("🔍 Verifying redirection for: " + linkText);
        switch(linkText.toLowerCase()) {
            case "elibrary":
                return verifyELibraryRedirection(expectedUrl);
                
            case "project url":
            case "project portal":
                return verifyProjectPortalUrl(expectedUrl);
                
            default:
                return verifyUrl(expectedUrl);
        }
    }
    
    /**
     * Verify opened URLs in new tab
     */
    public boolean verifyURLOpened(String elementName, String expectedUrl) {
        System.out.println("🔍 Verifying URL opened: " + elementName);
        switch(elementName.toLowerCase()) {
            case "project url":
                return verifyProjectPortalUrl(expectedUrl);
                
            case "elibrary":
                return verifyELibraryRedirection(expectedUrl);
                
            default:
                return verifyUrl(expectedUrl);
        }
    }
    
    /**
     * Verify status elements
     */
    public boolean verifyStatus(String elementName, String expectedStatus) {
        System.out.println("🔍 Verifying status: " + elementName);
        switch(elementName.toLowerCase()) {
            case "dissertation rating":
            case "dissertions rating":
                return verifyDissertationStatus(expectedStatus);
                
            default:
                return verifyElementDisplayed(PageLocators.elementByText(expectedStatus), expectedStatus);
        }
    }
    
    /**
     * Verify sections
     */
    public boolean verifySection(String sectionName) {
        System.out.println("🔍 Verifying section: " + sectionName);
        switch(sectionName.toLowerCase()) {
            case "my virtual labs":
                return verifySectionDisplayed(sectionName);
                
            case "viva/project":
                return verifySectionDisplayed(sectionName);
                
            default:
                return verifySectionDisplayed(sectionName);
        }
    }
    
    /**
     * Click on sections
     */
    public void clickSection(String sectionName) {
        System.out.println("🔍 Clicking section: " + sectionName);
        By locator;
        switch(sectionName.toLowerCase()) {
            case "my virtual labs":
                locator = PageLocators.sectionByDataAttribute(sectionName);
                highlightAndCapture(locator);
                actions.click(locator);
                break;
                
            case "viva/project":
                locator = PageLocators.sectionButton(sectionName);
                highlightAndCapture(locator);
                actions.click(locator);
                break;
                
            default:
                locator = PageLocators.sectionButton(sectionName);
                highlightAndCapture(locator);
                actions.click(locator);
                break;
        }
    }
    
    /**
     * Click CTA buttons
     */
    public void clickCTAOnCourseTile(String ctaText) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            
            // Wait for page to stabilize
            Thread.sleep(2000);
            
            // Primary XPath
            String primaryXpath = "//div[@class='card-footer text-center py-3 bg-body-tertiary text-primary']//a";
            
            // Fallback XPath
            String fallbackXpath = "(//div[contains(@class,'card-footer')]//a[contains(@class,'link-underline')])[11]";
            
            WebElement element = null;
            
            // Try primary xpath
            try {
                List<WebElement> elements = driver.findElements(By.xpath(primaryXpath));
                element = elements.stream()
                    .filter(WebElement::isDisplayed)
                    .findFirst()
                    .orElse(null);
                
            } catch (Exception e) {
                System.out.println("Primary xpath failed, trying fallback");
            }
            
            // Try fallback if primary failed
            if (element == null) {
                element = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath(fallbackXpath))
                );
            }
            
            if (element == null) {
                throw new NoSuchElementException("Could not find '" + ctaText + "' button");
            }
            
            // Highlight and capture
            highlightAndCapture(element);
            
            // Click with JS
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            
            System.out.println("Successfully clicked '" + ctaText + "'");
            
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to click '" + ctaText + "': " + e.getMessage(), 
                e
            );
        }
    }
    
    public void clickCTAelibrary(String ctaText) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // Small stabilization pause
            Thread.sleep(2000);

            String primaryXpath = "(//a[contains(@class,'btn btn-primary')])[2]";
            String fallbackXpath = "//div[@class='fs-3 fw-semibold text-primary text-nowrap']//small[contains(text(),'Go To MyAthens')]";

            WebElement element = null;

            // Try primary locator first
            try {
                List<WebElement> elements = driver.findElements(By.xpath(primaryXpath));
                element = elements.stream()
                        .filter(WebElement::isDisplayed)
                        .findFirst()
                        .orElse(null);
            } catch (Exception e) {
                System.out.println("⚠️ Primary locator lookup failed: " + e.getMessage());
            }

            // Fallback if primary element not found
            if (element == null) {
                element = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(fallbackXpath)));
            }

            if (element == null) {
                throw new NoSuchElementException("❌ Could not find CTA element for text: " + ctaText);
            }

            // ✅ Scroll element into center view
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center', inline: 'center'});",
                    element
            );
            Thread.sleep(500); // small delay to ensure smooth scroll

            // ✅ Highlight for visibility
            highlightAndCapture(element);

            // ✅ Click using JS for reliability
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);

            System.out.println("✅ Successfully clicked '" + ctaText + "'");

        } catch (Exception e) {
            String error = "❌ Failed to click '" + ctaText + "': " + e.getMessage();
           
            throw new RuntimeException(error, e);
        }
    }

    
    private void scrollToCenter(String ctaText) {
    	 try {
             JavascriptExecutor js = (JavascriptExecutor) driver;
             js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center', inline: 'center'});");
             Thread.sleep(500); // Wait for smooth scroll to complete
         } catch (Exception e) {
             System.err.println("⚠️ Could not scroll to element: " + e.getMessage());
         }
     }
		
	

	public void clickVivaProject(String ctaText) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            
            // Wait for page to stabilize
            Thread.sleep(2000);
            
            // Primary XPath
            String primaryXpath = "(//a[@class='btn btn-primary'])[5]";
            
            // Fallback XPath
            String fallbackXpath = "//div[@class='col-10 d-flex flex-column justify-content-center align-items-start']//small[contains(text(),'Go To Viva / Project Portal')]";
            
            WebElement element = null;
            
            // Try primary xpath
            try {
                List<WebElement> elements = driver.findElements(By.xpath(primaryXpath));
                element = elements.stream()
                    .filter(WebElement::isDisplayed)
                    .findFirst()
                    .orElse(null);
            } catch (Exception e) {
                System.out.println("Primary xpath failed, trying fallback");
            }
            
            // Try fallback if primary failed
            if (element == null) {
                element = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath(fallbackXpath))
                );
            }
            
            if (element == null) {
                throw new NoSuchElementException("Could not find '" + ctaText + "' button");
            }
            
            // Highlight and capture
            highlightAndCapture(element);
            
            // Click with JS
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            
            System.out.println("Successfully clicked '" + ctaText + "'");
            
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to click '" + ctaText + "': " + e.getMessage(), 
                e
            );
        }
    }
    
    /**
     * Click CTA on specific tile
     */
    public void clickCTAOnTile(String ctaText, String tileName) {
        try {
            // Build XPath
            String xpath = String.format(
                "//a[@class='card-text btn btn-sm w-100 btn-outline-primary' and @href='/academics/']",
                tileName, ctaText
            );
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            
            // Find the element
            WebElement element = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.xpath(xpath))
            );
            
            // Highlight and capture
            highlightAndCapture(element);
            
            // Click with JS
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].click();", 
                element
            );
            
            System.out.println("Clicked: " + ctaText + " on " + tileName);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to click: " + e.getMessage(), e);
        }
    }

    // ==================== Tile Verification Methods ====================
    
    private boolean verifyTileByDataAttribute(String dataValue, String tileName) {
        try {
            By tileLocator = By.xpath("//div[@data-tile='" + dataValue + "']");
            actions.waitForVisibility(tileLocator);
            highlightAndCapture(tileLocator);
            System.out.println("✅ Tile displayed: " + tileName + " (data-tile=" + dataValue + ")");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Tile not displayed: " + tileName);
            return false;
        }
    }
    
    public boolean verifyAllDashboardTiles() {
        System.out.println("🔍 Verifying all dashboard tiles...");
        boolean allTilesDisplayed = true;
        allTilesDisplayed &= verifyTile(Constants.MY_ACADEMICS);
        allTilesDisplayed &= verifyTile(Constants.EXAMINATIONS);
        allTilesDisplayed &= verifyTile(Constants.STUDENT_SERVICES);
        allTilesDisplayed &= verifyTile(Constants.WILP_POLICIES);
        allTilesDisplayed &= verifyTile(Constants.STUDENT_SUPPORT);
        allTilesDisplayed &= verifyTile(Constants.MY_COURSES);

        if (allTilesDisplayed) {
            System.out.println("✅ All dashboard tiles verified successfully");
        } else {
            System.err.println("❌ Some dashboard tiles verification failed");
        }
        return allTilesDisplayed;
    }

    // ==================== Course & Content Verification ====================
    
    private boolean verifyMyCoursesDisplayed() {
        try {
            By coursesLocator = By.xpath("//*[contains(text(),'My Courses')]");
            actions.waitForVisibility(coursesLocator);
            highlightAndCapture(coursesLocator);
            System.out.println("✅ My Courses displayed");
            return true;
        } catch (Exception e) {
            System.err.println("❌ My Courses not displayed");
            return false;
        }
    }

    private boolean verifyCourseTitleInSubjectScreen(String courseTitle) {
        try {
            By titleLocator = By.xpath("//h1[contains(text(),'" + courseTitle + "')]");
            actions.waitForVisibility(titleLocator);
            highlightAndCapture(titleLocator);
            actions.verifyTextContains(titleLocator, courseTitle);
            System.out.println("Course title verified: " + courseTitle);
            return true;
        } catch (Exception e) {
            System.err.println("Course title not found: " + courseTitle);
            return false;
        }
    }

    // ==================== Virtual Labs Verification ====================
    
    public boolean verifyAvailableLabs() {
        try {
            By labsLocator = By.xpath("//div[@class='labs-container']");
            actions.waitForVisibility(labsLocator);
            highlightAndCapture(labsLocator);
            System.out.println("✅ Available labs displayed");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Available labs not displayed");
            return false;
        }
    }

    public boolean verifyLabDisplayed(String labName) {
        try {
            By labLocator = By.xpath("//*[contains(text(),'" + labName + "')]");
            actions.waitForVisibility(labLocator);
            highlightAndCapture(labLocator);
            System.out.println("✅ Lab displayed: " + labName);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Lab not displayed: " + labName);
            return false;
        }
    }

    // ==================== URL Verification ====================
    
    private boolean verifyELibraryRedirection(String expectedUrl) {
        System.out.println("🔍 Verifying eLibrary URL redirection...");
        try {
            actions.verifyUrlContains(expectedUrl);
            System.out.println("✅ eLibrary URL verified");
            // Wait for screenshot
            Thread.sleep(3000);
            return true;
        } catch (Exception e) {
            System.err.println("❌ eLibrary URL verification failed");
            return false;
        }
    }

    private boolean verifyProjectPortalUrl(String expectedUrl) {
        System.out.println("🔍 Verifying Project Portal URL...");
        try {
            actions.verifyUrlContains(expectedUrl);
            System.out.println("✅ Project Portal URL verified");
            // Wait for screenshot
            Thread.sleep(3000);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Project Portal URL verification failed");
            return false;
        }
    }

    private boolean verifyUrl(String expectedUrl) {
        try {
            actions.verifyUrlContains(expectedUrl);
            System.out.println("✅ URL verified: " + expectedUrl);
            // Wait for screenshot
            Thread.sleep(3000);
            return true;
        } catch (Exception e) {
            System.err.println("❌ URL verification failed: " + expectedUrl);
            return false;
        }
    }

    // ==================== Dissertation & Status Verification ====================
    
    private boolean verifyDissertationStatus(String expectedStatus) {
        try {
            By statusLocator = By.xpath("//*[contains(text(),'" + expectedStatus + "')]");
            actions.waitForVisibility(statusLocator);
            highlightAndCapture(statusLocator);
            System.out.println("✅ Dissertation status verified: " + expectedStatus);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Dissertation status not found: " + expectedStatus);
            return false;
        }
    }

    // ==================== Section Verification ====================
    
    private boolean verifySectionDisplayed(String sectionName) {
        try {
            By sectionLocator = By.xpath("//button[contains(text(),'" + sectionName + "')]");
            actions.waitForVisibility(sectionLocator);
            highlightAndCapture(sectionLocator);
            System.out.println("✅ Section displayed: " + sectionName);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Section not displayed: " + sectionName);
            return false;
        }
    }

    // ==================== Login Verification ====================
    
    public boolean verifyLoginSuccess() {
        System.out.println("🔍 Verifying login success...");
        try {
            String currentUrl = driver.getCurrentUrl();
            boolean urlCorrect = currentUrl.contains("elearn.bits-pilani.ac.in") || 
                                currentUrl.contains("bits-pilani.ac.in");
            boolean dashboardVisible = actions.elementExists(
                By.xpath("//div[@class='container-fluid p-0 m-0']"), 3);
            
            if (urlCorrect && dashboardVisible) {
                System.out.println("✅ Login verified successfully");
                // Wait for screenshot
                Thread.sleep(3000);
                return true;
            } else {
                System.err.println("❌ Login verification failed");
                System.err.println("   URL Check: " + urlCorrect);
                System.err.println("   Dashboard Check: " + dashboardVisible);
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Login verification failed with exception: " + e.getMessage());
            return false;
        }
    }

    // ==================== Generic Element Verification ====================
    
    public boolean verifyElementDisplayed(By locator, String elementName) {
        try {
            actions.waitForVisibility(locator);
            highlightAndCapture(locator);
            System.out.println("✅ Element displayed: " + elementName);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Element not displayed: " + elementName);
            return false;
        }
    }

    public boolean verifyElementText(By locator, String expectedText, String elementName) {
        try {
            highlightAndCapture(locator);
            actions.verifyTextEquals(locator, expectedText);
            System.out.println("✅ Element text verified: " + elementName);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Element text mismatch: " + elementName);
            return false;
        }
    }

    public boolean verifyElementContainsText(By locator, String expectedText, String elementName) {
        try {
            highlightAndCapture(locator);
            actions.verifyTextContains(locator, expectedText);
            System.out.println("✅ Element contains text: " + elementName);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Element does not contain text: " + elementName);
            return false;
        }
    }

    // ==================== Tab Management ====================
    
    public void switchToNewTab() {
        mainWindow = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(mainWindow)) {
                driver.switchTo().window(handle);
                System.out.println("✅ Switched to new tab");
                break;
            }
        }
    }

    public void closeCurrentTabAndSwitchToMain() {
        driver.close();
        if (mainWindow != null) {
            driver.switchTo().window(mainWindow);
        } else {
            driver.switchTo().window(driver.getWindowHandles().iterator().next());
        }
        System.out.println("✅ Closed tab and returned to main window");
    }
    
    public void storeMainWindow() {
        mainWindow = driver.getWindowHandle();
    }
    
    public void switchToMainWindow() {
        if (mainWindow != null) {
            driver.switchTo().window(mainWindow);
        }
    }

    // ==================== Utility Methods ====================
    
    public void waitForPageLoad() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(webDriver ->
                ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState").equals("complete"));
            System.out.println("✅ Page loaded completely");
            // Wait for screenshot after page load
            Thread.sleep(3000);
        } catch (Exception e) {
            System.err.println("⚠️ Page load wait timeout");
        }
    }

    public void printVerificationSummary(String testName, boolean passed) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test: " + testName);
        System.out.println("Status: " + (passed ? "✅ PASSED" : "❌ FAILED"));
        System.out.println("=".repeat(60) + "\n");
    }

    // ==================== Helper Methods ====================
    
    private String mapTileNameToDataKey(String tileName) {
        switch(tileName.toLowerCase()) {
            case "my academics":
                return "myAcademics";
            case "examinations":
                return "examinations";
            case "student services":
                return "studentServices";
            case "wilp policies":
                return "wilpPolicies";
            case "student support":
                return "studentSupport";
            case "my courses":
                return "Mycourses";
            default:
                return null;
        }
    }
}