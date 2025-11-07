package drivers;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
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
            driver.set(createDriver());
        }
    }

    // ==========================================================
    // 🔹 Get Current Driver (or Init if Needed)
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
        browserName = ConfigReader.get("browser", "chrome").toLowerCase();

        // ✅ Headless mode (config or system property)
        boolean isHeadless = Boolean.parseBoolean(System.getProperty(
                "headless", ConfigReader.get("headless", "false")));

        WebDriver webDriver;
        System.out.println("🌐 Initializing " + browserName + " browser..."
                + (isHeadless ? " (Headless Mode)" : ""));

        try {
            switch (browserName) {
                case "chrome":
                    WebDriverManager.chromedriver().setup();
                    ChromeOptions chromeOptions = new ChromeOptions();

                    if (isHeadless) {
                        chromeOptions.addArguments("--headless=new");
                        chromeOptions.addArguments("--no-sandbox");
                        chromeOptions.addArguments("--disable-dev-shm-usage");
                        chromeOptions.addArguments("--disable-gpu");
                        chromeOptions.addArguments("--window-size=1920,1080");
                    } else {
                        chromeOptions.addArguments("--start-maximized");
                    }

                    chromeOptions.addArguments("--disable-notifications");
                    chromeOptions.addArguments("--disable-popup-blocking");
                    chromeOptions.addArguments("--remote-allow-origins=*");

                    webDriver = new ChromeDriver(chromeOptions);
                    break;

                case "firefox":
                    WebDriverManager.firefoxdriver().setup();
                    FirefoxOptions firefoxOptions = new FirefoxOptions();

                    if (isHeadless) {
                        firefoxOptions.addArguments("--headless");
                        firefoxOptions.addArguments("--width=1920");
                        firefoxOptions.addArguments("--height=1080");
                    } else {
                        firefoxOptions.addArguments("--width=1920");
                        firefoxOptions.addArguments("--height=1080");
                    }

                    webDriver = new FirefoxDriver(firefoxOptions);
                    break;

                case "edge":
                    WebDriverManager.edgedriver().setup();
                    EdgeOptions edgeOptions = new EdgeOptions();

                    if (isHeadless) {
                        edgeOptions.addArguments("--headless=new");
                        edgeOptions.addArguments("--no-sandbox");
                        edgeOptions.addArguments("--disable-dev-shm-usage");
                        edgeOptions.addArguments("--disable-gpu");
                        edgeOptions.addArguments("--window-size=1920,1080");
                    } else {
                        edgeOptions.addArguments("--start-maximized");
                    }

                    edgeOptions.addArguments("--disable-notifications");
                    edgeOptions.addArguments("--disable-popup-blocking");
                    edgeOptions.addArguments("--remote-allow-origins=*");

                    webDriver = new EdgeDriver(edgeOptions);
                    break;

                default:
                    throw new IllegalArgumentException("❌ Unsupported browser: " + browserName);
            }

        } catch (Exception e) {
            System.err.println("⚠️ Failed to initialize WebDriver for: " + browserName);
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize WebDriver for: " + browserName, e);
        }

        System.out.println("✅ Browser initialized successfully: " + browserName
                + (isHeadless ? " (Headless)" : ""));
        return webDriver;
    }

    // ==========================================================
    // 🔹 Quit Browser (Thread Safe)
    // ==========================================================
    public static void quitDriver() {
        WebDriver drv = driver.get();
        if (drv != null) {
            try {
                System.out.println("🔒 Quitting browser...");
                drv.quit();
                driver.remove(); // ✅ prevent memory leaks
                System.out.println("✅ Browser closed successfully");
            } catch (Exception e) {
                System.err.println("⚠️ Error closing browser: " + e.getMessage());
            }
        }
    }

    // ==========================================================
    // 🔹 Utility
    // ==========================================================
    public static String getBrowserName() {
        return browserName != null ? browserName : "unknown";
    }
}
