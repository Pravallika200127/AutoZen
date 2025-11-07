package drivers;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.safari.SafariDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import config.ConfigReader;

public class DriverFactory {

    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static String browserName;

    // ==========================================================
    // 🔹 Initialize Driver (thread-safe)
    // ==========================================================
    public static void initDriver() {
        if (driver.get() == null) {
            WebDriver drv = createDriver();
            driver.set(drv);
        }
    }

    // ==========================================================
    // 🔹 Get Current Driver (Lazy Init)
    // ==========================================================
    public static WebDriver getDriver() {
        if (driver.get() == null) {
            initDriver();
        }
        return driver.get();
    }

    // ==========================================================
    // 🔹 Create WebDriver Based on Browser Config
    // ==========================================================
    private static WebDriver createDriver() {
        browserName = System.getProperty("browser",
                        ConfigReader.get("browser", "chrome")).toLowerCase();
        boolean isHeadless = Boolean.parseBoolean(System.getProperty(
                        "headless", ConfigReader.get("headless", "false")));

        WebDriver webDriver = null;
        System.out.println("🌐 Initializing " + browserName + " browser..."
                + (isHeadless ? " (Headless Mode)" : ""));

        try {
            switch (browserName) {
                case "chrome" -> {
                    WebDriverManager.chromedriver().setup();
                    ChromeOptions chromeOptions = new ChromeOptions();
                    chromeOptions.addArguments("--remote-allow-origins=*");
                    chromeOptions.addArguments("--disable-notifications");
                    chromeOptions.addArguments("--disable-popup-blocking");
                    if (isHeadless) {
                        chromeOptions.addArguments("--headless=new", "--window-size=1920,1080",
                                "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
                    } else chromeOptions.addArguments("--start-maximized");
                    webDriver = new ChromeDriver(chromeOptions);
                }

                case "firefox" -> {
                    WebDriverManager.firefoxdriver().setup();
                    FirefoxOptions firefoxOptions = new FirefoxOptions();
                    if (isHeadless)
                        firefoxOptions.addArguments("--headless", "--width=1920", "--height=1080");
                    webDriver = new FirefoxDriver(firefoxOptions);
                }

                case "edge" -> {
                    WebDriverManager.edgedriver().setup();
                    EdgeOptions edgeOptions = new EdgeOptions();
                    edgeOptions.addArguments("--remote-allow-origins=*");
                    if (isHeadless)
                        edgeOptions.addArguments("--headless=new", "--no-sandbox",
                                "--disable-dev-shm-usage", "--disable-gpu", "--window-size=1920,1080");
                    else edgeOptions.addArguments("--start-maximized");
                    webDriver = new EdgeDriver(edgeOptions);
                }

                case "safari" -> {
                    webDriver = new SafariDriver();
                }

                default -> throw new IllegalArgumentException("❌ Unsupported browser: " + browserName);
            }

            System.out.println("✅ Browser initialized successfully: " + browserName
                    + (isHeadless ? " (Headless)" : ""));
            return webDriver;

        } catch (Exception e) {
            System.err.println("⚠️ Failed to initialize WebDriver for: " + browserName);
            e.printStackTrace();
            throw new RuntimeException("WebDriver init failed for: " + browserName, e);
        }
    }

    // ==========================================================
    // 🔹 Quit Browser (Thread Safe)
    // ==========================================================
    public static void quitDriver() {
        WebDriver drv = driver.get();
        if (drv != null) {
            try {
                System.out.println("🔒 Quitting browser: " + browserName);
                drv.quit();
                driver.remove();
                System.out.println("✅ Browser closed successfully");
            } catch (Exception e) {
                System.err.println("⚠️ Error closing browser: " + e.getMessage());
            }
        }
    }

    public static String getBrowserName() {
        return browserName != null ? browserName : "unknown";
    }
}
