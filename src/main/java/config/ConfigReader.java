package config;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * ✅ FINAL ConfigReader (CI/CD + Validation)
 * ------------------------------------------------------------
 * - Loads config.properties and testdata.json
 * - System properties (-Dkey=value) override file values
 * - Validates mandatory keys on startup
 * - Provides typed getters for string, int, boolean, JSON, etc.
 */
public class ConfigReader {

    private static final String DEFAULT_CONFIG_PATH = "src/test/resources/config.properties";
    private static final String DEFAULT_TESTDATA_PATH = "src/test/resources/testdata/testdata.json";

    private static final Properties PROPS = new Properties();
    private static JSONObject testData = new JSONObject();
    private static boolean propsLoaded = false;
    private static boolean dataLoaded = false;

    // ===============================
    // 🔹 Load & Validation
    // ===============================
    public static synchronized void loadProperties() {
        if (propsLoaded) return;
        String path = DEFAULT_CONFIG_PATH;
        try (InputStream is = new FileInputStream(path)) {
            PROPS.load(is);
            propsLoaded = true;
            System.out.println("✅ Configuration loaded successfully from: " + path);
        } catch (IOException e) {
            System.err.println("⚠️ Failed to load config.properties at: " + path + " — " + e.getMessage());
            try (InputStream is = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (is != null) {
                    PROPS.load(is);
                    propsLoaded = true;
                    System.out.println("✅ Configuration loaded from classpath: config.properties");
                } else {
                    throw new RuntimeException("config.properties not found");
                }
            } catch (IOException ex) {
                throw new RuntimeException("❌ Could not load configuration file", ex);
            }
        }

        // ✅ Run validation immediately after loading
        validateRequiredKeys();
    }

    public static synchronized void loadTestData() {
        if (dataLoaded) return;
        loadProperties();
        String path = get("testdata.path", DEFAULT_TESTDATA_PATH);
        try {
            if (Files.exists(Paths.get(path))) {
                String content = Files.readString(Paths.get(path));
                if (content.trim().startsWith("[")) {
                    testData = new JSONObject().put("data", new JSONArray(content));
                } else {
                    testData = new JSONObject(content);
                }
                System.out.println("✅ Test data loaded successfully from: " + path);
            } else {
                System.out.println("ℹ️ Test data file not found at: " + path + " (skipping)");
                testData = new JSONObject();
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to load test data from: " + path + " — " + e.getMessage());
            testData = new JSONObject();
        }
        dataLoaded = true;
    }

    // ===============================
    // 🔹 Validation of Mandatory Keys
    // ===============================
    private static void validateRequiredKeys() {
        List<String> missing = new ArrayList<>();

        String[] requiredKeys = {
                "app.url",
                "browser",
                "testrail.url",
                "testrail.username",
                "testrail.apikey",
                "testrail.projectId",
                "testrail.suiteId"
        };

        for (String key : requiredKeys) {
            String value = get(key);
            if (value == null || value.isBlank()) {
                missing.add(key);
            }
        }

        if (!missing.isEmpty()) {
            System.err.println("❌ Missing required configuration keys: " + String.join(", ", missing));
            throw new RuntimeException("Configuration validation failed. Missing keys: " + missing);
        }

        // Optional keys (warn only)
        String[] optionalKeys = {
                "headless",
                "testrail.labels",
                "testdata.path",
                "screenshot.captureAllSteps"
        };
        for (String key : optionalKeys) {
            if (get(key) == null) {
                System.out.println("⚠️ Optional key not set: " + key);
            }
        }

        System.out.println("✅ Configuration validation passed successfully.");
    }

    // ===============================
    // 🔹 Property Getters
    // ===============================
    public static String get(String key) {
        if (!propsLoaded) loadProperties();

        String sysValue = System.getProperty(key);
        if (sysValue != null && !sysValue.trim().isEmpty()) {
            return sysValue.trim();
        }

        String fileValue = PROPS.getProperty(key);
        return fileValue != null ? fileValue.trim() : null;
    }

    public static String get(String key, String defaultValue) {
        if (!propsLoaded) loadProperties();

        String sysValue = System.getProperty(key);
        if (sysValue != null && !sysValue.trim().isEmpty()) {
            return sysValue.trim();
        }

        return PROPS.getProperty(key, defaultValue);
    }

    public static int getInt(String key) {
        String value = get(key);
        if (value == null)
            throw new RuntimeException("Property '" + key + "' missing");
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Property '" + key + "' is not a valid integer: " + value);
        }
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key) {
        String value = get(key);
        return value != null && Boolean.parseBoolean(value.trim());
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    // ===============================
    // 🔹 JSON Test Data Accessors
    // ===============================
    public static JSONObject getJsonObject(String key) {
        if (!dataLoaded) loadTestData();
        return testData.optJSONObject(key);
    }

    public static JSONArray getJsonArray(String key) {
        if (!dataLoaded) loadTestData();
        return testData.optJSONArray(key);
    }

    public static String getJsonString(String path) {
        if (!dataLoaded) loadTestData();
        if (testData == null || testData.isEmpty()) return null;

        try {
            String[] keys = path.split("\\.");
            Object current = testData;
            for (String k : keys) {
                if (current instanceof JSONObject) {
                    current = ((JSONObject) current).get(k);
                } else {
                    throw new RuntimeException("Invalid JSON path: " + path);
                }
            }
            return current != null ? current.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONObject getFullTestData() {
        if (!dataLoaded) loadTestData();
        return testData != null ? testData : new JSONObject();
    }

    // ===============================
    // 🔹 Reset
    // ===============================
    public static void reload() {
        propsLoaded = false;
        dataLoaded = false;
        PROPS.clear();
        testData = new JSONObject();
        loadProperties();
        loadTestData();
    }
}
