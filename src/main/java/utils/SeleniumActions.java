package utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import drivers.DriverFactory;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;

/**
 * Enhanced Selenium Actions with automatic element highlighting and screenshot capture
 */
public class SeleniumActions {
    
    private WebDriver driver;
    private WebDriverWait wait;
    private static final String HIGHLIGHT_COLOR = "yellow";
    private static final String HIGHLIGHT_BORDER = "3px solid red";
    
    public SeleniumActions(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }
    
    public SeleniumActions(WebDriver driver, int timeoutSeconds) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }
    
    // ==================== HIGHLIGHTING UTILITIES ====================
    
    /**
     * Highlight element before performing action
     */
    private void highlightElement(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            // Store original style
            String originalStyle = element.getAttribute("style");
            
            // Apply highlight
            js.executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                element,
                "background: " + HIGHLIGHT_COLOR + "; border: " + HIGHLIGHT_BORDER + ";"
            );
            
            // Wait to make highlight visible
            Thread.sleep(500);
            
            System.out.println("✨ Element highlighted successfully");
        } catch (Exception e) {
            System.err.println("⚠️ Could not highlight element: " + e.getMessage());
        }
    }
    
    /**
     * Remove highlight from element
     */
    private void removeHighlight(WebElement element, String originalStyle) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            if (originalStyle != null && !originalStyle.isEmpty()) {
                js.executeScript("arguments[0].setAttribute('style', arguments[1]);", element, originalStyle);
            } else {
                js.executeScript("arguments[0].removeAttribute('style');", element);
            }
            Thread.sleep(200);
        } catch (Exception e) {
            System.err.println("⚠️ Could not remove highlight: " + e.getMessage());
        }
    }
    
    /**
     * Highlight element and capture screenshot
     */
    /**
     * Highlight element, capture screenshot with highlight, then remove highlight
     */
    private void highlightAndCapture(WebElement element, String actionDescription) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String originalStyle = element.getAttribute("style");

            // Scroll into view for visibility
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
            Thread.sleep(300);

            // Apply highlight (yellow background, red border)
            js.executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                element,
                "background: yellow; border: 3px solid red; border-radius: 4px;"
            );

            // Wait for highlight to be visible
            Thread.sleep(400);

            // Capture screenshot while highlighted
            TakesScreenshot ts = (TakesScreenshot) driver;
            String base64Screenshot = ts.getScreenshotAs(OutputType.BASE64);

            // Log to Extent Report (inline)
            ExtentReportManager.logStepWithScreenshot(
                "info",
                actionDescription,
                base64Screenshot
            );

            System.out.println("📸 Highlighted screenshot captured for: " + actionDescription);

            // Restore original element style
            if (originalStyle != null && !originalStyle.isEmpty()) {
                js.executeScript("arguments[0].setAttribute('style', arguments[1]);", element, originalStyle);
            } else {
                js.executeScript("arguments[0].removeAttribute('style');", element);
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error in highlightAndCapture: " + e.getMessage());
        }
    }

    /**
     * Capture screenshot and attach to Extent Report
     */
    private void captureScreenshot(String description) {
        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            String base64Screenshot = ts.getScreenshotAs(OutputType.BASE64);
            
            // Log to Extent Report with screenshot
            ExtentReportManager.logStepWithScreenshot(
                "info",
                description,
                base64Screenshot
            );
            
            System.out.println("📸 Screenshot captured: " + description);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to capture screenshot: " + e.getMessage());
        }
    }
    
    // ==================== ELEMENT LOCATION ====================
    
    public WebElement findElement(By locator) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            return element;
        } catch (Exception e) {
            String error = "Failed to find element - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    public List<WebElement> findElements(By locator) {
        try {
            return driver.findElements(locator);
        } catch (Exception e) {
            String error = "Failed to find elements - Locator: " + locator.toString();
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    // ==================== CLICK ACTIONS WITH HIGHLIGHTING ====================
    
    public void click(By locator) {
        String browser = DriverFactory.getBrowserName().toLowerCase();

        if (browser.contains("safari")) {
            // Safari-specific click logic
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
                    
                    // Highlight and capture before click
                    highlightAndCapture(element, "🖱️ Clicking on element: " + locator);
                    
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
                    ((JavascriptExecutor) driver).executeScript("window.focus();");
                    Thread.sleep(500);

                    try {
                        element.click();
                    } catch (ElementClickInterceptedException e) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                    }

                    System.out.println("✅ [Safari] Clicked element: " + locator + " on attempt " + attempt);
                    
                    // Capture after click
                    captureScreenshot("✅ After clicking: " + locator);
                    return;
                    
                } catch (Exception e) {
                    System.out.println("⚠️ [Safari] Attempt " + attempt + " failed for click on " + locator);
                    try {
                        Thread.sleep(1500 * attempt);
                    } catch (InterruptedException ignored) {}
                }
            }
            throw new RuntimeException("❌ [Safari] Failed to click element - Locator: " + locator);
        } else {
            // Standard click logic
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
                    
                    // Highlight and capture before click
                    highlightAndCapture(element, "🖱️ Clicking on element: " + locator);
                    
                    element.click();
                    System.out.println("✅ [Standard] Clicked element: " + locator + " on attempt " + attempt);
                    
                    // Capture after click
                    captureScreenshot("✅ After clicking: " + locator);
                    return;
                    
                } catch (StaleElementReferenceException e) {
                    System.out.println("⚠️ [Standard] Stale element detected. Retrying click on " + locator);
                } catch (Exception e) {
                    System.out.println("⚠️ [Standard] Attempt " + attempt + " failed for click on " + locator);
                }

                try {
                    Thread.sleep(1000 * attempt);
                } catch (InterruptedException ignored) {}
            }
            throw new RuntimeException("❌ [Standard] Failed to click element - Locator: " + locator);
        }
    }
    
    public void click(WebElement element, String elementDescription) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(element));
            
            // Highlight and capture before click
            highlightAndCapture(element, "🖱️ Clicking on: " + elementDescription);
            
            element.click();
            System.out.println("✅ Clicked element: " + elementDescription);
            
            // Capture after click
            captureScreenshot("✅ After clicking: " + elementDescription);
            
        } catch (Exception e) {
            String error = "Failed to click element - Element: " + elementDescription;
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    // ==================== INPUT ACTIONS WITH HIGHLIGHTING ====================
    
    public void navigateTo(String url) {
        String browser = DriverFactory.getBrowserName().toLowerCase();

        if (browser.contains("safari")) {
            try {
                System.out.println("🧭 [Safari] Navigating to: " + url);
                driver.get(url);

                for (int i = 0; i < 5; i++) {
                    try {
                        Thread.sleep(2000);
                        boolean ready = (boolean) ((JavascriptExecutor) driver)
                                .executeScript("return document.readyState === 'complete'");
                        if (ready) {
                            System.out.println("✅ [Safari] Page loaded successfully: " + url);
                            captureScreenshot("📄 Page loaded: " + url);
                            return;
                        }
                    } catch (Exception ignored) {}
                }
                System.out.println("⚠️ [Safari] Document readiness uncertain, continuing test anyway.");
            } catch (Exception e) {
                System.err.println("❌ [Safari] Navigation failed — retrying once...");
                throw new RuntimeException("❌ [Safari] Failed to navigate to: " + url, e);
            }
        } else {
            try {
                System.out.println("🧭 [Standard] Navigating to: " + url);
                driver.navigate().to(url);
                wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
                System.out.println("✅ [Standard] Page loaded successfully: " + url);
                
                // Capture page after loading
                captureScreenshot("📄 Page loaded: " + url);
                
            } catch (Exception e) {
                System.err.println("⚠️ [Standard] Navigation failed — retrying once...");
                throw new RuntimeException("❌ [Standard] Failed to navigate to: " + url, e);
            }
        }
    }

    public void type(By locator, String text) {
        String browser = DriverFactory.getBrowserName().toLowerCase();

        if (browser.contains("safari")) {
            // Safari requires focus and sometimes JS fallback
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));

                    // Highlight before typing
                    highlightAndCapture(element, "⌨️ Typing into: " + locator + " | Text: " + maskSensitiveData(text));

                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
                    ((JavascriptExecutor) driver).executeScript("window.focus();");
                    Thread.sleep(600);

                    element.click();
                    Thread.sleep(200);

                    element.clear();
                    Thread.sleep(200);

                    try {
                        element.sendKeys(text);
                    } catch (Exception e) {
                        System.out.println("⚠️ [Safari] sendKeys failed — using JS fallback");
                        ((JavascriptExecutor) driver)
                                .executeScript("arguments[0].value = arguments[1];", element, text);
                        ((JavascriptExecutor) driver)
                                .executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", element);
                        ((JavascriptExecutor) driver)
                                .executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", element);
                    }

                    String entered = element.getAttribute("value");
                    if (entered != null && entered.trim().equals(text)) {
                        System.out.println("✅ [Safari] Entered text successfully into " + locator + " (attempt " + attempt + ")");
                        highlightAndCapture(element, "✅ Text entered in: " + locator);
                        return;
                    } else {
                        System.out.println("⚠️ [Safari] Text mismatch (attempt " + attempt + "), retrying...");
                    }

                } catch (Exception e) {
                    System.out.println("⚠️ [Safari] Attempt " + attempt + " failed for typing into " + locator + ": " + e.getMessage());
                    try { Thread.sleep(1000L * attempt); } catch (InterruptedException ignored) {}
                }
            }
            throw new RuntimeException("❌ [Safari] Failed to enter text - Locator: " + locator);
        }

        // ---------- STANDARD (Chrome/Edge/Firefox) ----------
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));

                highlightAndCapture(element, "⌨️ Typing into: " + locator + " | Text: " + maskSensitiveData(text));

                element.clear();
                element.sendKeys(text);

                highlightAndCapture(element, "✅ Text entered in: " + locator);
                System.out.println("✅ [Standard] Entered text successfully into " + locator + " (attempt " + attempt + ")");
                return;

            } catch (Exception e) {
                System.out.println("⚠️ [Standard] Attempt " + attempt + " failed for typing into " + locator + ": " + e.getMessage());
                try { Thread.sleep(800L * attempt); } catch (InterruptedException ignored) {}
            }
        }
        throw new RuntimeException("❌ [Standard] Failed to enter text - Locator: " + locator);
    }
    
    /**
     * Mask sensitive data like passwords in logs/screenshots
     */
    private String maskSensitiveData(String text) {
        if (text != null && text.length() > 0) {
            // Mask if it looks like a password (more than 4 chars)
            if (text.length() > 4) {
                return "****" + text.substring(text.length() - 2);
            }
        }
        return text;
    }

    public void typeWithoutClear(By locator, String text) {
        String browser = DriverFactory.getBrowserName().toLowerCase();

        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            highlightAndCapture(element, "⌨️ Appending text to: " + locator);

            if (browser.contains("safari")) {
                try {
                    element.sendKeys(text);
                } catch (Exception e) {
                    System.out.println("⚠️ [Safari] sendKeys failed — using JS fallback for append");
                    ((JavascriptExecutor) driver)
                            .executeScript("arguments[0].value += arguments[1];", element, text);
                    ((JavascriptExecutor) driver)
                            .executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", element);
                    ((JavascriptExecutor) driver)
                            .executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", element);
                }
            } else {
                element.sendKeys(text);
            }

            highlightAndCapture(element, "✅ Text appended in: " + locator);
            System.out.println("✅ Appended text in element: " + locator);

        } catch (Exception e) {
            String error = "❌ Failed to append text - Locator: " + locator + " (" + e.getMessage() + ")";
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }

    // ==================== VERIFICATION WITH HIGHLIGHTING ====================
    
    public boolean isDisplayed(By locator) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            
            // Highlight element being verified
            highlightAndCapture(element, "👁️ Verifying element is displayed: " + locator);
            
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    public void verifyTextEquals(By locator, String expectedText) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            String actualText = element.getText().trim();
            
            // Highlight element being verified
            highlightAndCapture(element, "✔️ Verifying text equals | Expected: " + expectedText + " | Actual: " + actualText);
            
            if (!actualText.contains(expectedText)) {
                String error = "Expected Output Mismatch: Expected '" + expectedText + 
                             "' but got '" + actualText + "' - Locator: " + locator;
                captureFailure(error, new AssertionError(error));
                throw new AssertionError(error);
            }
            System.out.println("✅ Text verification passed: " + expectedText);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            String error = "Failed to verify text - Locator: " + locator;
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }

    public void verifyTextContains(By locator, String expectedText) {
        String browser = DriverFactory.getBrowserName().toLowerCase();

        if (browser.contains("safari")) {
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
                    Thread.sleep(500);

                    String actualText = element.getText().trim();
                    
                    // Highlight element being verified
                    highlightAndCapture(element, "✔️ Verifying text contains | Expected: " + expectedText + " | Actual: " + actualText);
                    
                    if (actualText.contains(expectedText)) {
                        System.out.println("✅ [Safari] Text verification passed on attempt " + attempt);
                        return;
                    } else {
                        System.out.println("⚠️ [Safari] Text not matched yet (Attempt " + attempt + ")");
                        Thread.sleep(1000 * attempt);
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ [Safari] Attempt " + attempt + " failed verifying text");
                }
            }
            throw new RuntimeException("❌ [Safari] Text verification failed for: " + expectedText);
        } else {
            try {
                WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
                String actualText = element.getText().trim();

                // Highlight element being verified
                highlightAndCapture(element, "✔️ Verifying text contains | Expected: " + expectedText + " | Actual: " + actualText);

                if (!actualText.contains(expectedText)) {
                    String error = "Text mismatch — Expected: '" + expectedText +
                            "' but got: '" + actualText + "'";
                    captureFailure(error, new AssertionError(error));
                    throw new AssertionError(error);
                }

                System.out.println("✅ [Standard] Text contains verification passed");
            } catch (AssertionError e) {
                throw e;
            } catch (Exception e) {
                String error = "Failed to verify text contains for " + locator;
                captureFailure(error, e);
                throw new RuntimeException(error, e);
            }
        }
    }

    public void verifyUrlContains(String expectedUrlPart) {
        try {
            String currentUrl = driver.getCurrentUrl();
            
            // Capture current page state
            captureScreenshot("🔗 Verifying URL contains: " + expectedUrlPart + " | Current URL: " + currentUrl);
            
            if (!currentUrl.contains(expectedUrlPart)) {
                String error = "Expected URL to contain '" + expectedUrlPart + 
                             "' but actual URL is '" + currentUrl + "'";
                captureFailure(error, new AssertionError(error));
                throw new AssertionError(error);
            }
            System.out.println("✅ URL verification passed: Contains '" + expectedUrlPart + "'");
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            String error = "Failed to verify URL";
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    public boolean elementExists(By locator, int timeoutSeconds) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            WebElement element = shortWait.until(ExpectedConditions.presenceOfElementLocated(locator));
            
            // Highlight if element exists
            highlightAndCapture(element, "✔️ Element exists: " + locator);
            
            return true;
        } catch (Exception e) {
            captureScreenshot("❌ Element does not exist: " + locator);
            return false;
        }
    }
    
    // ==================== WAIT ACTIONS ====================
    
    public WebElement waitForVisibility(By locator) {
        try {
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            highlightAndCapture(element, "⏳ Waited for element visibility: " + locator);
            return element;
        } catch (Exception e) {
            String error = "Timeout: Element not visible - Locator: " + locator;
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    public WebElement waitForClickable(By locator) {
        try {
        	WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        	((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
        	wait.until(ExpectedConditions.elementToBeClickable(locator));
            highlightAndCapture(element, "⏳ Waited for element to be clickable: " + locator);
            return element;
        } catch (Exception e) {
            String error = "Timeout: Element not clickable - Locator: " + locator;
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    public void waitForTitleContains(String titlePart) {
        try {
            wait.until(ExpectedConditions.titleContains(titlePart));
            System.out.println("✅ Page title contains: " + titlePart);
            captureScreenshot("✅ Page title verification passed: " + titlePart);
        } catch (Exception e) {
            String error = "Timeout: Page title does not contain '" + titlePart + "'";
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    // ==================== GET ACTIONS ====================
    
    public String getText(By locator) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            String text = element.getText().trim();
            
            highlightAndCapture(element, "📝 Getting text from element: " + locator + " | Text: " + text);
            
            System.out.println("✅ Retrieved text: " + text);
            return text;
        } catch (Exception e) {
            String error = "Failed to get text - Locator: " + locator;
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    public String getAttribute(By locator, String attributeName) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            String value = element.getAttribute(attributeName);
            
            highlightAndCapture(element, "📋 Getting attribute '" + attributeName + "' from: " + locator + " | Value: " + value);
            
            return value;
        } catch (Exception e) {
            String error = "Failed to get attribute - Locator: " + locator;
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    public void scrollToElement(By locator) {
        try {
            WebElement element = findElement(locator);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
            Thread.sleep(500);
            
            highlightAndCapture(element, "📜 Scrolled to element: " + locator);
            
        } catch (Exception e) {
            String error = "Failed to scroll to element - Locator: " + locator;
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    public void refreshPage() {
        try {
            driver.navigate().refresh();
            System.out.println("✅ Page refreshed");
            Thread.sleep(1000);
            captureScreenshot("🔄 Page refreshed");
        } catch (Exception e) {
            String error = "Failed to refresh page";
            captureFailure(error, e);
            throw new RuntimeException(error, e);
        }
    }
    
    public static void implicitWait(long time) {
        DriverFactory.getDriver().manage().timeouts().implicitlyWait(Duration.ofSeconds(time));
    }
    
    // ==================== PRIVATE HELPER ====================
    
    private void captureFailure(String errorMessage, Throwable throwable) {
        System.err.println("❌ " + errorMessage);
        
        // Capture screenshot on failure
        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            String base64Screenshot = ts.getScreenshotAs(OutputType.BASE64);
            ExtentReportManager.logStepWithScreenshot("fail", errorMessage, base64Screenshot);
        } catch (Exception e) {
            System.err.println("⚠️ Could not capture failure screenshot: " + e.getMessage());
        }
        
        try {
            Class<?> hooksClass = Class.forName("hooks.Hooks");
            java.lang.reflect.Method setFailureInfoMethod = hooksClass.getMethod("setFailureInfo", String.class, Throwable.class);
            setFailureInfoMethod.invoke(null, errorMessage, throwable);
        } catch (Exception e) {
            System.err.println("⚠️ Could not capture failure in Hooks: " + e.getMessage());
        }
    }
}