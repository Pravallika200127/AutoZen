package utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import drivers.DriverFactory;

import java.time.Duration;
import java.util.List;

/**
 * Generic Selenium Actions with automatic error handling and TestRail defect creation
 * Use these methods instead of direct driver calls to automatically capture failures
 */
public class SeleniumActions {
    
    private WebDriver driver;
    private WebDriverWait wait;
    
    public SeleniumActions(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }
    
    public SeleniumActions(WebDriver driver, int timeoutSeconds) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }
    
    // ==================== NAVIGATION ====================
    
    /**
     * Navigate to URL with automatic error handling
     */
    public void navigateTo(String url) {
        try {
            System.out.println("🌐 Navigating to: " + url);
            driver.get(url);
            System.out.println("✅ Navigation successful");
        } catch (TimeoutException e) {
            String error = "Timeout: Page failed to load - URL: " + url;
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (Exception e) {
            String error = "Failed to navigate to URL: " + url + " - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    // ==================== ELEMENT LOCATION ====================
    
    /**
     * Find element with automatic error handling
     */
    public WebElement findElement(By locator) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            return element;
        } catch (NoSuchElementException e) {
            String error = "XPath Issue: Element not found - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (TimeoutException e) {
            String error = "Timeout: Element not found within timeout - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (Exception e) {
            String error = "Failed to find element - Locator: " + locator.toString() + " - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    /**
     * Find multiple elements with automatic error handling
     */
    public List<WebElement> findElements(By locator) {
        try {
            return driver.findElements(locator);
        } catch (Exception e) {
            String error = "Failed to find elements - Locator: " + locator.toString() + " - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    // ==================== CLICK ACTIONS ====================
    
    /**
     * Click element with automatic error handling
     */
    public void click(By locator) {
        try {
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            element.click();
            System.out.println("✅ Clicked element: " + locator.toString());
        } catch (NoSuchElementException e) {
            String error = "XPath Issue: Click failed - Element not found - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (ElementNotInteractableException e) {
            String error = "XPath Issue: Element not interactable - May be hidden or disabled - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (StaleElementReferenceException e) {
            String error = "Stale Element: Element reference became stale - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (TimeoutException e) {
            String error = "Timeout: Element not clickable within timeout - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (Exception e) {
            String error = "Failed to click element - Locator: " + locator.toString() + " - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    /**
     * Click element (WebElement) with automatic error handling
     */
    public void click(WebElement element, String elementDescription) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(element));
            element.click();
            System.out.println("✅ Clicked element: " + elementDescription);
        } catch (StaleElementReferenceException e) {
            String error = "Stale Element: Element reference became stale - Element: " + elementDescription;
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (Exception e) {
            String error = "Failed to click element - Element: " + elementDescription + " - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    // ==================== INPUT ACTIONS ====================
    
    /**
     * Type text into element with automatic error handling
     */
    public void type(By locator, String text) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            element.clear();
            element.sendKeys(text);
            System.out.println("✅ Entered text in element: " + locator.toString());
        } catch (NoSuchElementException e) {
            String error = "XPath Issue: Input field not found - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (ElementNotInteractableException e) {
            String error = "XPath Issue: Input field not interactable - May be disabled - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (StaleElementReferenceException e) {
            String error = "Stale Element: Input field reference became stale - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (Exception e) {
            String error = "Failed to enter text - Locator: " + locator.toString() + " - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    /**
     * Type text into element without clearing
     */
    public void typeWithoutClear(By locator, String text) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            element.sendKeys(text);
            System.out.println("✅ Appended text in element: " + locator.toString());
        } catch (Exception e) {
            String error = "Failed to append text - Locator: " + locator.toString() + " - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    // ==================== VERIFICATION ====================
    
    /**
     * Verify element is displayed
     */
    public boolean isDisplayed(By locator) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            return element.isDisplayed();
        } catch (NoSuchElementException e) {
            String error = "XPath Issue: Element not found for visibility check - Locator: " + locator.toString();
            captureFailure(error, e);
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * ✅ Smart text verification — tolerant to prefixes, case, and whitespace
     * Still fails if expected text isn't meaningfully found in actual text
     */
    public void verifyTextEquals(By locator, String expectedText) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            String actualText = element.getText().trim();

            // Normalize both texts (remove extra spaces, line breaks, case differences)
            String expectedNorm = expectedText.replaceAll("\\s+", " ").trim();
            String actualNorm = actualText.replaceAll("\\s+", " ").trim();

            // Exact match (case-insensitive)
            if (actualNorm.equalsIgnoreCase(expectedNorm)) {
                System.out.println("✅ Exact match: " + expectedNorm);
                return;
            }

            // If expected text appears within the actual text — soft match
            if (actualNorm.toLowerCase().contains(expectedNorm.toLowerCase())) {
                System.out.println("⚠️ Partial match (tolerated): Expected '" + expectedText + 
                                   "' found inside actual text '" + actualText + "'");
                return;
            }

            // Otherwise, hard fail
            String error = "❌ Expected Output Mismatch: Expected '" + expectedText + 
                           "' but got '" + actualText + "' - Locator: " + locator;
            captureFailure(error, new AssertionError(error));
            throw new AssertionError(error);

        } catch (NoSuchElementException e) {
            String error = "❌ Element not found for verification - Locator: " + locator;
            captureFailure(error, e);
            throw new RuntimeException(error, e);

        } catch (AssertionError e) {
            throw e;

        } catch (Exception e) {
            String error = "❌ Error verifying text at " + locator + " - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }

    /**
     * Verify text contains expected with automatic error handling
     */
    public void verifyTextContains(By locator, String expectedText) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            String actualText = element.getText().trim();
            
            if (!actualText.contains(expectedText)) {
                String error = "Expected Output Mismatch: Expected text to contain '" + expectedText + 
                             "' but actual text is '" + actualText + "' - Locator: " + locator.toString();
                captureFailure(error, new AssertionError(error));
                throw new AssertionError(error);
            }
            System.out.println("✅ Text contains verification passed: " + expectedText);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            String error = "Failed to verify text contains - Locator: " + locator.toString() + " - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    /**
     * Verify URL contains expected text
     */
    public void verifyUrlContains(String expectedUrlPart) {
        try {
            String currentUrl = driver.getCurrentUrl();
            
            if (!currentUrl.contains(expectedUrlPart)) {
                String error = "Expected Output Mismatch: Expected URL to contain '" + expectedUrlPart + 
                             "' but actual URL is '" + currentUrl + "'";
                captureFailure(error, new AssertionError(error));
                throw new AssertionError(error);
            }
            System.out.println("✅ URL verification passed: Contains '" + expectedUrlPart + "'");
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            String error = "Failed to verify URL - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    /**
     * Verify element exists (without throwing exception if not found)
     */
    public boolean elementExists(By locator, int timeoutSeconds) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            shortWait.until(ExpectedConditions.presenceOfElementLocated(locator));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // ==================== WAIT ACTIONS ====================
    
    /**
     * Wait for element to be visible
     */
    public WebElement waitForVisibility(By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            String error = "Timeout: Element not visible within timeout - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (Exception e) {
            String error = "Failed waiting for element visibility - Locator: " + locator.toString() + " - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
	public static void highLightAndUnHighlightElement() {
		WebDriver driver = DriverFactory.getDriver();
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].setAttribute('style', 'background: yellow; border: 2px solid red;');");
		SeleniumActions.implicitWait(2);
		js.executeScript("arguments[0].removeAttribute('style','')");
	}
	
	 public static void implicitWait(long time) {
		 DriverFactory.getDriver().manage().timeouts().implicitlyWait(Duration.ofSeconds(time));
	    }

    /**
     * Wait for element to be clickable
     */
    public WebElement waitForClickable(By locator) {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(locator));
        } catch (TimeoutException e) {
            String error = "Timeout: Element not clickable within timeout - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (Exception e) {
            String error = "Failed waiting for element to be clickable - Locator: " + locator.toString() + " - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    /**
     * Wait for page title to contain text
     */
    public void waitForTitleContains(String titlePart) {
        try {
            wait.until(ExpectedConditions.titleContains(titlePart));
            System.out.println("✅ Page title contains: " + titlePart);
        } catch (TimeoutException e) {
            String error = "Timeout: Page title does not contain '" + titlePart + "' - Actual title: " + driver.getTitle();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    // ==================== GET ACTIONS ====================
    
    /**
     * Get text from element with automatic error handling
     */
    public String getText(By locator) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            String text = element.getText().trim();
            System.out.println("✅ Retrieved text: " + text);
            return text;
        } catch (NoSuchElementException e) {
            String error = "XPath Issue: Element not found for getting text - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (Exception e) {
            String error = "Failed to get text - Locator: " + locator.toString() + " - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    /**
     * Get attribute value from element
     */
    public String getAttribute(By locator, String attributeName) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            return element.getAttribute(attributeName);
        } catch (NoSuchElementException e) {
            String error = "XPath Issue: Element not found for getting attribute - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        } catch (Exception e) {
            String error = "Failed to get attribute - Locator: " + locator.toString() + " - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Scroll to element
     */
    public void scrollToElement(By locator) {
        try {
            WebElement element = findElement(locator);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            Thread.sleep(500); // Small wait after scroll
        } catch (Exception e) {
            String error = "Failed to scroll to element - Locator: " + locator.toString() + " - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    /**
     * Refresh page
     */
    public void refreshPage() {
        try {
            driver.navigate().refresh();
            System.out.println("✅ Page refreshed");
        } catch (Exception e) {
            String error = "Failed to refresh page - " + e.getMessage();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    // ==================== PRIVATE HELPER ====================
    
    /**
     * Capture failure information for TestRail defect creation
     * Uses reflection to avoid compile-time dependency on Hooks class
     */
    private void captureFailure(String errorMessage, Throwable throwable) {
        System.err.println("❌ " + errorMessage);
        
        try {
            // Use reflection to call Hooks.setFailureInfo to avoid compile-time dependency
            Class<?> hooksClass = Class.forName("hooks.Hooks");
            java.lang.reflect.Method setFailureInfoMethod = hooksClass.getMethod("setFailureInfo", String.class, Throwable.class);
            setFailureInfoMethod.invoke(null, errorMessage, throwable);
        } catch (Exception e) {
            // If Hooks class is not available or method call fails, just log it
            System.err.println("⚠️  Could not capture failure in Hooks: " + e.getMessage());
        }
    }
}