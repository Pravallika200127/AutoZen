package utils;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import config.ConfigReader;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Client {
    private final String apiBaseUrl;
    private final String username;
    private final String apiKey;
    private final int projectId;
    private final int suiteId;
    private final OkHttpClient http = new OkHttpClient();

    private int runId = 0;
    private int lastResultId = 0;

    public Client() {
        String baseUrl = ConfigReader.get("testrail.url");
        this.apiBaseUrl = baseUrl.endsWith("/") ? baseUrl + "index.php?/api/v2" : baseUrl + "/index.php?/api/v2";
        this.username = ConfigReader.get("testrail.username");
        this.apiKey = ConfigReader.get("testrail.apikey");
        this.projectId = Integer.parseInt(ConfigReader.get("testrail.projectId", "0"));
        this.suiteId = Integer.parseInt(ConfigReader.get("testrail.suiteId", "0"));
    }

    private String authHeader() {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + apiKey).getBytes());
    }

    private JSONObject postJson(String path, JSONObject payload) throws IOException {
        String url = path.startsWith("http") ? path : apiBaseUrl + "/" + path;
        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request req = new Request.Builder().url(url).addHeader("Authorization", authHeader()).post(body).build();
        try (Response resp = http.newCall(req).execute()) {
            String text = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) throw new IOException("POST failed: " + resp.code() + " " + text);
            return text.isEmpty() ? new JSONObject() : new JSONObject(text);
        }
    }

    private String get(String path) throws IOException {
        String url = apiBaseUrl + "/" + path;
        Request req = new Request.Builder().url(url).addHeader("Authorization", authHeader()).get().build();
        try (Response resp = http.newCall(req).execute()) {
            if (resp.isSuccessful() && resp.body() != null) return resp.body().string();
            throw new IOException("GET failed: " + resp.code());
        }
    }

    public int createTestRun(String name, List<Integer> cases) throws IOException {
        JSONObject payload = new JSONObject();
        payload.put("suite_id", suiteId);
        payload.put("name", name);
        payload.put("include_all", false);
        JSONArray ids = new JSONArray();
        cases.forEach(ids::put);
        payload.put("case_ids", ids);
        JSONObject res = postJson("add_run/" + projectId, payload);
        runId = res.optInt("id", 0);
        System.out.println("✅ Created TestRail Run: R" + runId);
        return runId;
    }

    public JSONObject getTestCaseFromTestRail(int caseId) throws IOException {
        System.out.println("🔍 Fetching test case C" + caseId + " from TestRail...");
        String json = get("get_case/" + caseId);
        System.out.println("✅ Test case fetched successfully");
        return new JSONObject(json);
    }

    /**
     * Update test result for specific case.
     * If cached run is invalid/closed, auto-create a new run and retry once.
     */
    public synchronized Integer updateTestResult(int caseId, boolean passed, String comment) throws IOException {
        if (runId == 0) {
            throw new IllegalStateException("❌ No active TestRail run found.");
        }

        String url = apiBaseUrl + "/add_result_for_case/" + runId + "/" + caseId;
        JSONObject payload = new JSONObject();
        payload.put("status_id", passed ? 1 : 5);
        payload.put("comment", comment);

        try {
            JSONObject resp = postJson(url, payload);
            int resultId = resp.optInt("id", 0);
            this.lastResultId = resultId;
            System.out.println("✅ Test result updated for C" + caseId + " (Result ID: " + resultId + ")");
            return resultId;

        } catch (IOException ex) {
            // If the run is invalid/closed, create a new one and retry once
            String msg = ex.getMessage().toLowerCase();
            if (msg.contains("run_id") || msg.contains("400") || msg.contains("404")) {
                System.err.println("⚠️ Cached run is invalid or closed. Creating a new run...");
                String newRunName = "AutoRecoveryRun - " + LocalDateTime.now();
                this.runId = createTestRun(newRunName, List.of(caseId));

                // retry once
                String retryUrl = apiBaseUrl + "/add_result_for_case/" + runId + "/" + caseId;
                JSONObject retryResp = postJson(retryUrl, payload);
                int resultId = retryResp.optInt("id", 0);
                this.lastResultId = resultId;
                System.out.println("✅ Recovered with new TestRail Run: R" + runId + " (Result ID: " + resultId + ")");
                return resultId;
            } else {
                throw ex;
            }
        }
    }

    // ============================================================
    // 🔹 ENHANCED DEFECT CREATION WITH DETAILED LOGGING
    // ============================================================
    /**
     * Creates a detailed defect entry in TestRail for failed test cases.
     * 
     * @param testCaseId The test case ID (e.g., "C296")
     * @param scenarioName The name of the failed scenario
     * @param failureMessage The failure message/assertion error
     * @param stackTrace The full stack trace (optional)
     * @return true if defect was created successfully, false otherwise
     */
    public boolean createDefect(String testCaseId, String scenarioName, String failureMessage, String stackTrace) {
        try {
            System.out.println("\n🐛 Creating detailed defect in TestRail...");
            
            if (runId == 0) {
                System.err.println("❌ No active runId for defect creation");
                return false;
            }

            // Extract numeric case ID from string like "C296"
            int caseId;
            try {
                caseId = Integer.parseInt(testCaseId.replace("C", "").replace("c", "").trim());
            } catch (NumberFormatException e) {
                System.err.println("❌ Invalid test case ID format: " + testCaseId);
                return false;
            }
            
            // Determine priority and failure type
            int priorityId = determinePriority(failureMessage);
            String failureType = getFailureType(failureMessage);
            String priorityLabel = getPriorityLabel(priorityId);
            
            System.out.println("   Test Case: " + testCaseId);
            System.out.println("   Scenario: " + scenarioName);
            System.out.println("   Failure Type: " + failureType);
            System.out.println("   Priority: " + priorityLabel);

            // Build comprehensive defect description
            String defectDescription = buildDefectDescription(
                testCaseId, 
                scenarioName, 
                failureType,
                failureMessage, 
                stackTrace
            );

            // Create defect ID
            String defectId = "AUTO-DEFECT-" + testCaseId + "-" + System.currentTimeMillis();

            // Update test result with defect information using the API endpoint
            JSONObject payload = new JSONObject();
            payload.put("status_id", 5); // 5 = Failed
            payload.put("comment", defectDescription);
            payload.put("defects", defectId);

            // Use the correct API path format
            String path = "add_result_for_case/" + runId + "/" + caseId;
            JSONObject resp = postJson(path, payload);
            int resultId = resp.optInt("id", 0);
            
            if (resultId > 0) {
                System.out.println("✅ Defect created successfully (Result ID: " + resultId + ")");
                System.out.println("   Defect ID: " + defectId);
                this.lastResultId = resultId;
                return true;
            } else {
                System.err.println("⚠️ Defect creation returned no result ID");
                System.err.println("   Response: " + resp.toString());
                return false;
            }

        } catch (JSONException e) {
            System.err.println("❌ JSON error during defect creation: " + e.getMessage());
            System.err.println("   This usually means the TestRail API response was unexpected");
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            System.err.println("❌ API call failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error during defect creation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Simplified defect creation for backward compatibility
     */
    public boolean createDefect(String description, int caseId, int resultId) {
        try {
            if (runId == 0) {
                System.err.println("❌ No active runId for defect creation");
                return false;
            }
            
            String url = apiBaseUrl + "/add_result_for_case/" + runId + "/" + caseId;
            JSONObject payload = new JSONObject();
            payload.put("status_id", 5);
            payload.put("comment", description);
            payload.put("defects", "AUTO-" + System.currentTimeMillis());

            JSONObject resp = postJson(url, payload);
            System.out.println("✅ Created defect-like test result: " + resp.optInt("id"));
            return true;
        } catch (Exception e) {
            System.err.println("⚠️ Defect creation failed: " + e.getMessage());
            return false;
        }
    }

    // ============================================================
    // 🔹 HELPER METHODS FOR DEFECT CREATION
    // ============================================================
    
    /**
     * Build a comprehensive defect description with formatting
     */
    private String buildDefectDescription(String testCaseId, String scenarioName, 
                                         String failureType, String failureMessage, 
                                         String stackTrace) {
        StringBuilder description = new StringBuilder();
        
        // Header
        description.append("═══════════════════════════════════════════════════════════\n");
        description.append("🔴 AUTOMATED TEST FAILURE - DEFECT REPORT\n");
        description.append("═══════════════════════════════════════════════════════════\n\n");
        
        // Test Information
        description.append("📋 TEST INFORMATION:\n");
        description.append("   • Test Case ID: ").append(testCaseId).append("\n");
        description.append("   • Scenario: ").append(scenarioName).append("\n");
        description.append("   • Failure Type: ").append(failureType).append("\n");
        description.append("   • Timestamp: ").append(getCurrentTimestamp()).append("\n\n");
        
        // Failure Details
        description.append("❌ FAILURE DETAILS:\n");
        description.append("───────────────────────────────────────────────────────────\n");
        description.append(failureMessage).append("\n");
        description.append("───────────────────────────────────────────────────────────\n\n");
        
        // Stack Trace (if available)
        if (stackTrace != null && !stackTrace.isEmpty()) {
            description.append("📜 STACK TRACE:\n");
            description.append("───────────────────────────────────────────────────────────\n");
            description.append(stackTrace).append("\n");
            description.append("───────────────────────────────────────────────────────────\n\n");
        }
        
        // Environment Information
        description.append("🖥️ ENVIRONMENT:\n");
        description.append("   • Browser: Chrome 142\n");
        description.append("   • OS: Windows\n");
        description.append("   • Test Framework: Cucumber + TestNG + Selenium\n");
        description.append("   • TestRail Run: R").append(runId).append("\n\n");
        
        // Next Steps
        description.append("🔍 RECOMMENDED ACTIONS:\n");
        description.append("   1. Review the failure message and stack trace\n");
        description.append("   2. Check attached screenshots for visual context\n");
        description.append("   3. Verify test data and expected values\n");
        description.append("   4. Reproduce the issue manually if needed\n");
        description.append("   5. Update test case or application as required\n\n");
        
        description.append("═══════════════════════════════════════════════════════════\n");
        
        return description.toString();
    }
    
    /**
     * Determine priority based on failure type
     */
    private int determinePriority(String failureMessage) {
        String lowerMsg = failureMessage.toLowerCase();
        
        // Critical - System/Framework errors
        if (lowerMsg.contains("nullpointer") || 
            lowerMsg.contains("timeout") ||
            lowerMsg.contains("nosuchelement") ||
            lowerMsg.contains("stale")) {
            return 1; // Critical
        }
        
        // High - Assertion/Verification failures
        if (lowerMsg.contains("assertion") || 
            lowerMsg.contains("expected") ||
            lowerMsg.contains("verify")) {
            return 2; // High
        }
        
        // Medium - Other failures
        return 3; // Medium
    }
    
    /**
     * Get priority label for display
     */
    private String getPriorityLabel(int priorityId) {
        switch (priorityId) {
            case 1: return "🔴 CRITICAL - Immediate Action Required";
            case 2: return "🟠 HIGH - Needs Investigation";
            case 3: return "🟡 MEDIUM - Requires Analysis";
            case 4: return "🟢 LOW - Minor Issue";
            default: return "⚪ UNKNOWN";
        }
    }
    
    /**
     * Get failure type from error message
     */
    private String getFailureType(String failureMessage) {
        String lowerMsg = failureMessage.toLowerCase();
        
        if (lowerMsg.contains("assertionerror") || lowerMsg.contains("assertion")) {
            return "ASSERTION_FAILURE";
        } else if (lowerMsg.contains("nosuchelement")) {
            return "ELEMENT_NOT_FOUND";
        } else if (lowerMsg.contains("timeout")) {
            return "TIMEOUT_ERROR";
        } else if (lowerMsg.contains("nullpointer")) {
            return "NULL_POINTER_EXCEPTION";
        } else if (lowerMsg.contains("stale")) {
            return "STALE_ELEMENT_REFERENCE";
        } else if (lowerMsg.contains("expected")) {
            return "VALIDATION_MISMATCH";
        } else {
            return "UNKNOWN_FAILURE";
        }
    }
    
    /**
     * Get current timestamp in readable format
     */
    private String getCurrentTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }

    // ============================================================
    // 🔹 EXISTING METHODS (UNCHANGED)
    // ============================================================

    /**
     * Get first test ID from run (with proper error handling)
     */
    private int getFirstTestIdFromRun() {
        try {
            String json = get("get_tests/" + runId);
            
            // Check if response is empty
            if (json == null || json.trim().isEmpty()) {
                System.err.println("⚠️ Empty response from get_tests API");
                return 0;
            }
            
            // Try to parse as JSONArray
            JSONArray arr = new JSONArray(json);
            if (arr.length() > 0) {
                return arr.getJSONObject(0).optInt("id", 0);
            } else {
                System.err.println("⚠️ No tests found in the run");
                return 0;
            }
        } catch (JSONException e) {
            System.err.println("⚠️ JSON parsing error in getFirstTestIdFromRun: " + e.getMessage());
            return 0;
        } catch (IOException e) {
            System.err.println("⚠️ API error in getFirstTestIdFromRun: " + e.getMessage());
            return 0;
        } catch (Exception e) {
            System.err.println("⚠️ Unexpected error in getFirstTestIdFromRun: " + e.getMessage());
            return 0;
        }
    }

    public List<Integer> getCaseIdsByLabel(int project, int suite, String label) {
        List<Integer> ids = new ArrayList<>();
        try {
            String url = String.format("get_cases/%d&suite_id=%d", project, suite);
            String json = get(url);

            if (json == null || json.trim().isEmpty()) {
                System.err.println("⚠️ Empty response from get_cases API");
                return ids;
            }

            json = json.trim();

            JSONArray casesArray = null;

            // ✅ Handle both { "cases": [...] } and plain [...]
            if (json.startsWith("{")) {
                JSONObject root = new JSONObject(json);
                if (root.has("cases")) {
                    casesArray = root.getJSONArray("cases");
                } else {
                    System.err.println("⚠️ No 'cases' key found in TestRail response");
                    return ids;
                }
            } else if (json.startsWith("[")) {
                casesArray = new JSONArray(json);
            } else {
                System.err.println("⚠️ Invalid JSON format: " + json.substring(0, Math.min(100, json.length())));
                return ids;
            }

            // ✅ Now filter by label
            for (int i = 0; i < casesArray.length(); i++) {
                JSONObject c = casesArray.getJSONObject(i);

                // Try label matching
                boolean hasLabel = false;
                if (c.has("labels")) {
                    JSONArray labels = c.getJSONArray("labels");
                    for (int j = 0; j < labels.length(); j++) {
                        JSONObject lbl = labels.getJSONObject(j);
                        String lblTitle = lbl.optString("title", "");
                        if (lblTitle.equalsIgnoreCase(label)) {
                            hasLabel = true;
                            break;
                        }
                    }
                }

                if (hasLabel) {
                    ids.add(c.getInt("id"));
                }
            }

            if (ids.isEmpty()) {
                System.err.println("⚠️ No test cases found for label: " + label);
            } else {
                System.out.println("✅ Found " + ids.size() + " test case(s) for label: " + label + " → " + ids);
            }

        } catch (JSONException e) {
            System.err.println("⚠️ JSON parsing error in getCaseIdsByLabel: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("⚠️ API error in getCaseIdsByLabel: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("⚠️ Unexpected error in getCaseIdsByLabel: " + e.getMessage());
        }
        return ids;
    }



    public synchronized boolean closeTestRun() {
        if (runId == 0) {
            System.out.println("⚠️ No active TestRail run to close.");
            return false;
        }

        try {
            System.out.println("\n🏁 Closing TestRail Run R" + runId + "...");
            String url = apiBaseUrl + "/close_run/" + runId;
            JSONObject resp = postJson(url, new JSONObject());

            if (resp.has("id") || resp.isEmpty()) {
                System.out.println("✅ Closed TestRail Run R" + runId);
                runId = 0;
                return true;
            } else {
                System.err.println("⚠️ Close run returned unexpected response: " + resp);
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to close TestRail Run R" + runId + ": " + e.getMessage());
            return false;
        }
    }

    public boolean uploadAttachmentToResult(int resultId, File file) {
        String url = apiBaseUrl + "/add_attachment_to_result/" + resultId;
        try {
            RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("attachment", file.getName(), fileBody)
                    .build();

            Request req = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", authHeader())
                    .post(requestBody)
                    .build();

            try (Response resp = http.newCall(req).execute()) {
                if (resp.isSuccessful()) {
                    System.out.println("✅ Uploaded attachment: " + file.getName());
                    return true;
                } else {
                    System.err.println("❌ Upload attachment failed with code " + resp.code());
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ uploadAttachmentToResult error: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get the last result ID from most recent test update
     */
    public int getLastResultId() {
        return lastResultId;
    }
    
    /**
     * Get the current run ID
     */
    public int getRunId() {
        return runId;
    }
}