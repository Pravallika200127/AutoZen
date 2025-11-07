package utils;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import config.ConfigReader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * 🔹 TestRail API Client - FINAL ALIGNED VERSION
 * 
 * ✅ Supports:
 *  - Fetching specific test cases by ID
 *  - Fetching all cases by label
 *  - Creating test runs
 *  - Updating results
 *  - Uploading attachments (screenshots / HTML)
 */
public class Client {
    private final String apiBaseUrl;
    private final String username;
    private final String apiKey;
    private final int projectId;
    private final int suiteId;
    private final OkHttpClient httpClient;

    private int runId = 0;
    private int testId = 0;
    private int lastResultId = 0;

    // ============================================================
    // 🔹 Constructor
    // ============================================================
    public Client() {
        String baseUrl = ConfigReader.get("testrail.url");
        this.apiBaseUrl = baseUrl.endsWith("/") ? baseUrl + "index.php?/api/v2" : baseUrl + "/index.php?/api/v2";
        this.username = ConfigReader.get("testrail.username");
        this.apiKey = ConfigReader.get("testrail.apikey");
        this.projectId = Integer.parseInt(ConfigReader.get("testrail.projectId", "0"));
        this.suiteId = Integer.parseInt(ConfigReader.get("testrail.suiteId", "0"));
        this.httpClient = new OkHttpClient();
    }

    // ============================================================
    // 🔹 Helpers
    // ============================================================
    private String getBasicAuthHeader() {
        String credentials = username + ":" + apiKey;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    private String get(String path) throws IOException {
        String url = path.startsWith("http") ? path : apiBaseUrl + "/" + path;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", getBasicAuthHeader())
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null)
                return response.body().string();
            String error = response.body() != null ? response.body().string() : "No error details";
            throw new IOException("GET failed: " + response.code() + " - " + error);
        }
    }

    private JSONObject postJson(String url, JSONObject payload) throws IOException {
        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", getBasicAuthHeader())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String resp = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("POST failed: " + response.code() + " - " + resp);
            }
            return resp.isEmpty() ? new JSONObject() : new JSONObject(resp);
        }
    }

    // ============================================================
    // 🔹 Core Test Case APIs
    // ============================================================

    /** Fetch single case by ID */
    public JSONObject getTestCaseFromTestRail(int caseId) throws IOException {
        System.out.println("🔍 Fetching test case C" + caseId + " from TestRail...");
        String json = get("get_case/" + caseId);
        System.out.println("✅ Test case fetched successfully");
        return new JSONObject(json);
    }

    /** Fetch all cases in a project/suite */
    public List<JSONObject> fetchCases(int projectId, Integer suiteId) throws IOException {
        System.out.println("📡 Fetching test cases from TestRail...");
        String path = suiteId == null
                ? ("get_cases/" + projectId)
                : ("get_cases/" + projectId + "&suite_id=" + suiteId);
        String json = get(path);

        List<JSONObject> cases = new ArrayList<>();
        try {
            JSONObject responseObj = new JSONObject(json);
            if (responseObj.has("cases")) {
                JSONArray arr = responseObj.getJSONArray("cases");
                for (int i = 0; i < arr.length(); i++) cases.add(arr.getJSONObject(i));
            }
        } catch (Exception e) {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) cases.add(arr.getJSONObject(i));
        }

        System.out.println("✅ Fetched " + cases.size() + " test cases");
        return cases;
    }

    // ============================================================
    // 🔹 Label-Based Case Fetching
    // ============================================================

    /** Get all case IDs that contain the given label (stored in refs or custom field) */
    /**
     * Get all case IDs that contain the given label (in refs or any custom field)
     */
    /**
     * Get all case IDs that contain the given label (in refs or any custom label field)
     */
    /**
     * Get all case IDs that contain the given label (supports 'labels' array and all known custom fields)
     */
    public List<Integer> getCaseIdsByLabel(int projectId, int suiteId, String label) throws IOException {
        System.out.println("📡 Resolving cases with label: " + label);
        String url = apiBaseUrl + "/get_cases/" + projectId + "&suite_id=" + suiteId;
        String responseBody = get(url);

        List<Integer> caseIds = new ArrayList<>();
        try {
            JSONObject responseObj = new JSONObject(responseBody);
            JSONArray arr = responseObj.has("cases")
                    ? responseObj.getJSONArray("cases")
                    : new JSONArray(responseBody);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject c = arr.getJSONObject(i);

                boolean matchFound = false;

                // 1️⃣ Check the new standard TestRail "labels" array
                if (c.has("labels") && c.get("labels") instanceof JSONArray) {
                    JSONArray labelsArray = c.getJSONArray("labels");
                    for (int j = 0; j < labelsArray.length(); j++) {
                        JSONObject labelObj = labelsArray.optJSONObject(j);
                        if (labelObj != null) {
                            String title = labelObj.optString("title", "");
                            if (title.equalsIgnoreCase(label)) {
                                matchFound = true;
                                break;
                            }
                        }
                    }
                }

                // 2️⃣ Check other known custom fields (fallbacks)
                if (!matchFound) {
                    List<String> labelFields = Arrays.asList(
                        "refs", "custom_labels", "custom_label",
                        "custom_labels_multi", "custom_label_value",
                        "custom_tags", "custom_tag"
                    );

                    for (String field : labelFields) {
                        if (c.has(field)) {
                            String fieldValue = c.optString(field, "").toLowerCase();
                            if (fieldValue.contains(label.toLowerCase())) {
                                matchFound = true;
                                break;
                            }
                        }
                    }
                }

                if (matchFound) {
                    caseIds.add(c.getInt("id"));
                }
            }

            if (caseIds.isEmpty()) {
                System.out.println("⚠️ No cases found for label: " + label +
                        " (searched refs, custom_labels, and labels[].title)");
            } else {
                System.out.println("✅ Found " + caseIds.size() + " cases for label '" + label + "': " + caseIds);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error parsing label response: " + e.getMessage());
        }

        return caseIds;
    }




    // ============================================================
    // 🔹 Test Run Management
    // ============================================================

    /** Create a run including given case IDs */
    public int createTestRun(String runName, List<Integer> caseIds) throws IOException {
        System.out.println("\n🏃 Creating test run in TestRail...");
        String url = apiBaseUrl + "/add_run/" + projectId;

        JSONObject payload = new JSONObject();
        payload.put("suite_id", suiteId);
        payload.put("name", runName);
        payload.put("description", "Automated test run - " + java.time.LocalDateTime.now());
        payload.put("include_all", false);

        JSONArray caseArr = new JSONArray();
        caseIds.stream().distinct().forEach(caseArr::put);
        payload.put("case_ids", caseArr);

        JSONObject runResponse = postJson(url, payload);
        this.runId = runResponse.optInt("id", 0);
        System.out.println("✅ Test run created: R" + runId);
        return runId;
    }

    /** Close the current TestRail run */
    public boolean closeTestRun() throws IOException {
        if (runId == 0) {
            System.out.println("⚠️ No test run to close");
            return false;
        }
        System.out.println("\n🏁 Closing test run R" + runId + "...");
        String url = apiBaseUrl + "/close_run/" + runId;
        JSONObject resp = postJson(url, new JSONObject());
        return resp.has("id") || resp.isEmpty();
    }

    // ============================================================
    // 🔹 Results and Attachments
    // ============================================================

    /** Update test result for specific case */
    public Integer updateTestResult(int caseId, boolean passed, String comment) throws IOException {
        if (runId == 0) {
            createTestRun("AutoRun-" + java.time.LocalDateTime.now(), Collections.singletonList(caseId));
        }

        String url = apiBaseUrl + "/add_result_for_case/" + runId + "/" + caseId;
        JSONObject payload = new JSONObject();
        payload.put("status_id", passed ? 1 : 5);
        payload.put("comment", comment);

        JSONObject resp = postJson(url, payload);
        int resultId = resp.optInt("id", 0);
        this.lastResultId = resultId;
        System.out.println("✅ Result added for C" + caseId + " (Result ID: " + resultId + ")");
        return resultId;
    }

    /** Upload file attachment to a specific result (screenshot, report, etc.) */
    public boolean uploadExtentReportToResult(int resultId, File file) throws IOException {
        if (resultId == 0) {
            System.err.println("❌ Invalid resultId");
            return false;
        }
        if (file == null || !file.exists()) {
            System.err.println("❌ File not found: " + (file == null ? "null" : file.getAbsolutePath()));
            return false;
        }

        String url = apiBaseUrl + "/add_attachment_to_result/" + resultId;
        String mimeType = detectMimeType(file.getName());

        RequestBody fileBody = RequestBody.create(file, MediaType.parse(mimeType));
        RequestBody multipart = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("attachment", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", getBasicAuthHeader())
                .post(multipart)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String resp = response.body() != null ? response.body().string() : "";
            if (response.isSuccessful()) {
                System.out.println("✅ Uploaded '" + file.getName() + "' to Result ID: " + resultId);
                return true;
            } else {
                System.err.println("⚠️ Upload failed: " + response.code() + " - " + resp);
                return false;
            }
        }
    }

    private String detectMimeType(String fileName) {
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) return "text/html";
        if (fileName.endsWith(".zip")) return "application/zip";
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }

    // ============================================================
    // 🔹 Getters
    // ============================================================
    public int getRunId() { return runId; }
    public int getLastResultId() { return lastResultId; }
    // ============================================================
    // 🔹 Defect Creation (Failure Reporting)
    // ============================================================

    /**
     * Creates a defect entry in TestRail by adding a failed result with details.
     * This doesn't create a "bug ticket" in Jira; instead, it marks a failed result
     * with a rich description inside TestRail's result history.
     *
     * @param title       Defect title (brief summary)
     * @param description Detailed markdown/HTML description of the defect
     * @return true if successfully added, false otherwise
     */
    public boolean createDefect(String title, String description) {
        try {
            if (runId == 0) {
                System.err.println("⚠️ No active TestRail run. Creating a temporary run to record defect...");
                createTestRun("AutoDefect-" + java.time.LocalDateTime.now(), Collections.emptyList());
            }

            // Attempt to pick any existing test from the run
            int targetTestId = getFirstTestIdFromRun();
            if (targetTestId == 0) {
                System.err.println("⚠️ Could not find a valid test ID to attach defect. Skipping defect creation.");
                return false;
            }

            String url = apiBaseUrl + "/add_result/" + targetTestId;

            JSONObject payload = new JSONObject();
            payload.put("status_id", 5); // 5 = Failed
            payload.put("comment", "🐛 **DEFECT:** " + title + "\n\n" + description);
            payload.put("defects", title);

            JSONObject response = postJson(url, payload);

            if (response.has("id")) {
                int resultId = response.optInt("id", 0);
                System.out.println("✅ Defect result created in TestRail (Result ID: " + resultId + ")");
                this.lastResultId = resultId;
                return true;
            } else {
                System.err.println("⚠️ TestRail did not return a result ID for defect creation.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to create defect in TestRail: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Helper: Gets the first test ID from the current run (used for attaching defects)
     */
    private int getFirstTestIdFromRun() {
        try {
            if (runId == 0) return 0;
            String url = apiBaseUrl + "/get_tests/" + runId;
            String resp = get(url);

            try {
                JSONObject obj = new JSONObject(resp);
                if (obj.has("tests")) {
                    JSONArray tests = obj.getJSONArray("tests");
                    if (tests.length() > 0) return tests.getJSONObject(0).optInt("id", 0);
                }
            } catch (Exception ex) {
                JSONArray tests = new JSONArray(resp);
                if (tests.length() > 0) return tests.getJSONObject(0).optInt("id", 0);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Unable to get first test ID from run: " + e.getMessage());
        }
        return 0;
    }
}
