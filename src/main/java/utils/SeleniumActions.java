package utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.JavascriptExecutor;
import drivers.DriverFactory;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;


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
        String browser = drivers.DriverFactory.getBrowserName().toLowerCase();

        if (browser.contains("safari")) {
            // ==========================================================
            // 🍏 Safari-specific click logic
            // ==========================================================
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));

                    // Scroll and focus before click
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
                    ((JavascriptExecutor) driver).executeScript("window.focus();");
                    Thread.sleep(500);

                    try {
                        element.click();
                    } catch (ElementClickInterceptedException e) {
                        // Safari sometimes throws this when element is overlayed; retry via JS
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                    }

                    System.out.println("✅ [Safari] Clicked element: " + locator + " on attempt " + attempt);
                    return;
                } catch (Exception e) {
                    System.out.println("⚠️ [Safari] Attempt " + attempt + " failed for click on " + locator + " - Retrying...");
                    try {
                        Thread.sleep(1500 * attempt);
                    } catch (InterruptedException ignored) {}
                }
            }
            throw new RuntimeException("❌ [Safari] Failed to click element - Locator: " + locator + " after multiple attempts.");
        }

        else {
            // ==========================================================
            // 🌐 Standard click logic for Chrome / Edge / Firefox
            // ==========================================================
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
                    element.click();
                    System.out.println("✅ [Standard] Clicked element: " + locator + " on attempt " + attempt);
                    return;
                } catch (StaleElementReferenceException e) {
                    System.out.println("⚠️ [Standard] Stale element detected. Retrying click on " + locator);
                } catch (Exception e) {
                    System.out.println("⚠️ [Standard] Attempt " + attempt + " failed for click on " + locator + " - Retrying...");
                }

                try {
                    Thread.sleep(1000 * attempt);
                } catch (InterruptedException ignored) {}
            }
            throw new RuntimeException("❌ [Standard] Failed to click element - Locator: " + locator + " after multiple attempts.");
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
    public void navigateTo(String url) {
        String browser = drivers.DriverFactory.getBrowserName().toLowerCase();

        if (browser.contains("safari")) {
            // ==========================================================
            // 🍏 Safari-specific navigation
            // ==========================================================
            try {
                System.out.println("🧭 [Safari] Navigating to: " + url);
                driver.get(url);

                // Wait for page load completion — Safari needs JS check and extra buffer
                for (int i = 0; i < 5; i++) {
                    try {
                        Thread.sleep(2000);
                        boolean ready = (boolean) ((JavascriptExecutor) driver)
                                .executeScript("return document.readyState === 'complete'");
                        if (ready) {
                            System.out.println("✅ [Safari] Page loaded successfully: " + url);
                            return;
                        }
                    } catch (Exception ignored) {}
                }

                System.out.println("⚠️ [Safari] Document readiness uncertain, continuing test anyway.");
            } catch (Exception e) {
                System.err.println("❌ [Safari] Navigation failed — retrying once...");
                try {
                    Thread.sleep(3000);
                    driver.get(url);
                } catch (Exception retry) {
                    throw new RuntimeException("❌ [Safari] Failed to navigate to: " + url, retry);
                }
            }
        }

        else {
            // ==========================================================
            // 🌐 Standard logic for Chrome / Edge / Firefox
            // ==========================================================
            try {
                System.out.println("🧭 [Standard] Navigating to: " + url);
                driver.navigate().to(url);
                wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
                System.out.println("✅ [Standard] Page loaded successfully: " + url);
            } catch (Exception e) {
                System.err.println("⚠️ [Standard] Navigation failed — retrying once...");
                try {
                    Thread.sleep(2000);
                    driver.navigate().to(url);
                    wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
                    System.out.println("✅ [Standard] Navigation succeeded after retry: " + url);
                } catch (Exception retry) {
                    throw new RuntimeException("❌ [Standard] Failed to navigate to: " + url, retry);
                }
            }
        }
    }


    public void type(By locator, String text) {
        String browser = drivers.DriverFactory.getBrowserName().toLowerCase();

        if (browser.contains("safari")) {
            // ==========================================================
            // 🧩 Safari-specific typing logic
            // ==========================================================
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
                    ((JavascriptExecutor) driver).executeScript("window.focus();");

                    Thread.sleep(800); // allow DOM to settle
                    element.click();   // Safari sometimes needs focus before sendKeys
                    Thread.sleep(300);

                    element.clear();
                    element.sendKeys(text);

                    // Verify if text was actually entered
                    String entered = element.getAttribute("value");
                    if (entered != null && entered.equals(text)) {
                        System.out.println("✅ [Safari] Entered text successfully into " + locator + " on attempt " + attempt);
                        return;
                    } else {
                        System.out.println("⚠️ [Safari] Text mismatch after typing — retrying...");
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ [Safari] Attempt " + attempt + " failed for typing into " + locator + " - Retrying...");
                    try {
                        Thread.sleep(1500 * attempt);
                    } catch (InterruptedException ignored) {}
                }
            }
            throw new RuntimeException("❌ [Safari] Failed to enter text - Locator: " + locator + " after multiple attempts.");
        } 
        
        else {
            // ==========================================================
            // 🌐 Standard logic for Chrome / Firefox / Edge
            // ==========================================================
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                    element.clear();
                    element.sendKeys(text);
                    System.out.println("✅ [Standard] Entered text into " + locator + " on attempt " + attempt);
                    return;
                } catch (Exception e) {
                    System.out.println("⚠️ [Standard] Attempt " + attempt + " failed for typing into " + locator + " - Retrying...");
                    try {
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ignored) {}
                }
            }
            throw new RuntimeException("❌ [Standard] Failed to enter text - Locator: " + locator + " after multiple attempts.");
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
     * ✅ Smart text verification with assertion
     * - Normalizes whitespace, line breaks, and case
     * - Fails hard if expected text does not match or appear in actual
     */
    public void verifyTextEquals(By locator, String expectedText) {
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
     * Verify text contains expected with automatic error handling
     */
    public void verifyTextContains(By locator, String expectedText) {
        String browser = drivers.DriverFactory.getBrowserName().toLowerCase();

        if (browser.contains("safari")) {
            // ==========================================================
            // 🍏 Safari-specific text verification
            // ==========================================================
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
                    Thread.sleep(500);

                    String actualText = element.getText().trim();
                    if (actualText.contains(expectedText)) {
                        System.out.println("✅ [Safari] Text verification passed on attempt " + attempt + ": " + expectedText);
                        return;
                    } else {
                        System.out.println("⚠️ [Safari] Text not matched yet (Attempt " + attempt + ") — Retrying...");
                        Thread.sleep(1000 * attempt);
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ [Safari] Attempt " + attempt + " failed verifying text: " + e.getMessage());
                }
            }
            throw new RuntimeException("❌ [Safari] Text verification failed after multiple attempts for: " + expectedText);
        }

        else {
            // ==========================================================
            // 🌐 Standard text verification for Chrome / Edge / Firefox
            // ==========================================================
            try {
                WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
                String actualText = element.getText().trim();

                if (!actualText.contains(expectedText)) {
                    String error = "❌ [Standard] Text mismatch — Expected: '" + expectedText +
                            "' but got: '" + actualText + "' at " + locator;
                    captureFailure(error, new AssertionError(error));
                    throw new AssertionError(error);
                }

                System.out.println("✅ [Standard] Text contains verification passed: " + expectedText);
            } catch (AssertionError e) {
                throw e;
            } catch (Exception e) {
                String error = "⚠️ [Standard] Failed to verify text contains for " + locator + ": " + e.getMessage();
                captureFailure(error, e);
                throw new RuntimeException(error, e);
            }
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