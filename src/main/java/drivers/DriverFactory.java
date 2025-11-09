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
        browserName = System.getProperty("browser", ConfigReader.get("browser", "chrome")).toLowerCase();
        boolean isHeadless = Boolean.parseBoolean(System.getProperty("headless", ConfigReader.get("headless", "false")));
        WebDriver webDriver = null;

        System.out.println("🌐 Initializing " + browserName + " browser..." + (isHeadless ? " (Headless Mode)" : ""));

        try {
            switch (browserName) {
                // ==========================================================
                // 🟢 Chrome
                // ==========================================================
                case "chrome" -> {
                    WebDriverManager.chromedriver().setup();
                    ChromeOptions chromeOptions = new ChromeOptions();
                    chromeOptions.addArguments("--remote-allow-origins=*");
                    chromeOptions.addArguments("--disable-notifications", "--disable-popup-blocking");

                    if (isHeadless) {
                        chromeOptions.addArguments("--headless=new", "--window-size=1920,1080",
                                "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
                    } else {
                        chromeOptions.addArguments("--start-maximized");
                    }

                    webDriver = new ChromeDriver(chromeOptions);
                }

                // ==========================================================
                // 🔵 Firefox
                // ==========================================================
                case "firefox" -> {
                    WebDriverManager.firefoxdriver().setup();
                    FirefoxOptions firefoxOptions = new FirefoxOptions();

                    // 🔹 OPTIONAL: Set custom binary if needed
                    // firefoxOptions.setBinary("C:\\Program Files\\Mozilla Firefox\\firefox.exe");

                    if (isHeadless) {
                        firefoxOptions.addArguments("--headless");
                        firefoxOptions.addArguments("--width=1920", "--height=1080");
                    }

                    webDriver = new FirefoxDriver(firefoxOptions);
                }


                // ==========================================================
                // 🟣 Edge
                // ==========================================================
                case "edge" -> {
                    EdgeOptions edgeOptions = new EdgeOptions();
                    edgeOptions.addArguments("--remote-allow-origins=*");
                    edgeOptions.addArguments("--disable-notifications", "--disable-popup-blocking");

                    if (isHeadless) {
                        edgeOptions.addArguments("--headless", "--disable-gpu", "--no-sandbox",
                                "--disable-dev-shm-usage", "--window-size=1920,1080",
                                "--disable-extensions", "--disable-logging");
                    } else {
                        edgeOptions.addArguments("--start-maximized");
                    }

                    try {
                        System.out.println("🧩 Attempting EdgeDriver setup via WebDriverManager...");
                        WebDriverManager.edgedriver().setup();
                    } catch (Exception e) {
                        System.err.println("⚠️ WebDriverManager failed, using system-installed driver");
                        System.setProperty("webdriver.edge.driver", "/usr/local/bin/msedgedriver");
                    }

                    webDriver = new EdgeDriver(edgeOptions);
                }


                // ==========================================================
                // 🍏 Safari (MacOS)
                // ==========================================================
                case "safari" -> {
                    try {
                        System.out.println("🧩 Enabling SafariDriver...");
                        Runtime.getRuntime().exec("safaridriver --enable");
                        Thread.sleep(1500);

                        webDriver = new SafariDriver();
                        try {
                            webDriver.manage().window().maximize();
                        } catch (Exception ignored) {}
                        System.out.println("✅ Safari initialized successfully");
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to start Safari WebDriver: " + e.getMessage());
                        throw new RuntimeException("Safari WebDriver initialization failed", e);
                    }
                }

                // ==========================================================
                // ❌ Default - Unsupported Browser
                // ==========================================================
                default -> throw new IllegalArgumentException("❌ Unsupported browser: " + browserName);
            }

            System.out.println("✅ Browser initialized successfully: " + browserName + (isHeadless ? " (Headless)" : ""));
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
