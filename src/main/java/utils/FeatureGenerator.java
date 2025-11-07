package utils;

import config.ConfigReader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeatureGenerator {

    private static final String FEATURES_DIR = "src/test/resources/features";

    public FeatureGenerator() {}

    /* -------------------------------------------------------
     * Public API
     * ------------------------------------------------------- */

    public static void cleanOldFeatures() {
        try {
            File dir = new File(FEATURES_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
                return;
            }
            File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".feature"));
            if (files != null) for (File f : files) f.delete();
        } catch (Exception ignored) {}
    }

    public static void generateFeatureFile(String caseKey, JSONObject testCase) {
        try {
            int caseId = Integer.parseInt(caseKey.replaceAll("[^0-9]", ""));
            String title = testCase.optString("title", "Untitled");

            // 1) Pull raw BDD (supports plain text, JSONArray, or stringified JSON)
            String raw = extractBDDSteps(testCase);
            if (raw == null || raw.trim().isEmpty())
                throw new RuntimeException("No Gherkin found for " + caseKey);

            // 2) Split out an Examples section (if any) from TestRail text
            String stepsSection = raw;
            String examplesSection = "";
            int idx = raw.toLowerCase(Locale.ROOT).indexOf("examples:");
            if (idx != -1) {
                stepsSection   = raw.substring(0, idx).trim();
                examplesSection = raw.substring(idx).trim();
            }

            // 3) Extract Gherkin step lines
            List<String> steps = extractGherkinLines(stepsSection);
            if (steps.isEmpty()) throw new RuntimeException("No valid Gherkin steps found");

            // 4) If steps still contain "" and TestRail has an Examples header row, map "" → <header> (optional)
            List<String> headersFromTR = extractExamplesHeaders(examplesSection);
            if (!headersFromTR.isEmpty() && containsEmptyStringPlaceholders(steps)) {
                steps = fillEmptyStringsUsingHeaders(steps, headersFromTR);
            }

            // 5) Build Examples table by scanning placeholders in the steps and fetching by name from testdata.json
            String examplesBlock = buildExamplesFromPlaceholders(caseKey, steps);

            // 6) Compose and write the feature file
            StringBuilder feature = new StringBuilder();
            feature.append("Feature: Test Case: ").append(caseKey).append(" --- ").append(oneLine(title)).append("\n\n");
            feature.append("@TestRail @CaseID_").append(caseId).append("\n");
            feature.append("Scenario Outline: ").append(oneLine(title)).append("\n");
            for (String s : steps) feature.append("  ").append(s).append("\n");
            feature.append(examplesBlock);

            Path out = Path.of(FEATURES_DIR, "TestCase_" + caseId + ".feature");
            Files.createDirectories(out.getParent());
            try (FileWriter fw = new FileWriter(out.toFile(), StandardCharsets.UTF_8)) {
                fw.write(feature.toString());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate feature for " + caseKey + ": " + e.getMessage(), e);
        }
    }

    /* -------------------------------------------------------
     * TestRail → Gherkin text
     * ------------------------------------------------------- */

    private static String extractBDDSteps(JSONObject testCase) {
        StringBuilder buff = new StringBuilder();
        List<String> fields = Arrays.asList(
                "custom_testrail_bdd_scenario",
                "custom_bdd_scenarios",
                "custom_steps",
                "custom_steps_separated"
        );

        for (String field : fields) {
            if (!testCase.has(field) || testCase.isNull(field)) continue;
            Object v = testCase.get(field);

            // JSONArray shape: [{"content":"..."}, ...]
            if (v instanceof JSONArray) {
                JSONArray arr = (JSONArray) v;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj != null) {
                        String content = obj.optString("content", "");
                        if (!content.isEmpty()) buff.append(cleanHtml(content)).append("\n");
                    } else {
                        buff.append(cleanHtml(arr.optString(i, ""))).append("\n");
                    }
                }
                if (buff.length() > 0) return buff.toString().trim();
            }

            // String shape: plain text or stringified JSON array
            if (v instanceof String) {
                String s = ((String) v).trim();
                if (s.startsWith("[") && s.contains("{\"content\"")) {
                    try {
                        JSONArray arr = new JSONArray(s);
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.optJSONObject(i);
                            if (obj != null) {
                                String content = obj.optString("content", "");
                                if (!content.isEmpty()) buff.append(cleanHtml(content)).append("\n");
                            }
                        }
                        if (buff.length() > 0) return buff.toString().trim();
                    } catch (Exception ignore) {}
                }
                if (!s.isEmpty()) return cleanHtml(s);
            }
        }
        return "";
    }

    private static String cleanHtml(String text) {
        if (text == null) return "";
        String t = text.replaceAll("<br\\s*/?>", "\n");
        t = t.replaceAll("<[^>]+>", "");
        t = t.replace("&lt;", "<")
             .replace("&gt;", ">")
             .replace("&amp;", "&")
             .replace("&nbsp;", " ")
             .replace("&quot;", "\"")
             .replace("&#39;", "'");
        return t.trim();
    }

    private static List<String> extractGherkinLines(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();

        List<String> out = new ArrayList<>();
        String[] lines = raw.replace("\r", "\n").split("\n");

        Pattern p = Pattern.compile("^(?:\\s*[-•*]*)?\\s*(given|when|then|and|but)\\b(.*)$", Pattern.CASE_INSENSITIVE);
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            String lt = t.toLowerCase(Locale.ROOT);
            if (lt.startsWith("feature:") || lt.startsWith("scenario") || lt.startsWith("examples:")) continue;

            Matcher m = p.matcher(t);
            if (m.find()) {
                String kw = capitalize(m.group(1));
                String rest = m.group(2).trim();
                out.add(kw + " " + rest);
            }
        }
        return out;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0,1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    /* -------------------------------------------------------
     * Examples and placeholders
     * ------------------------------------------------------- */

    // Parse only the header row(s) from a TestRail "Examples:" block (if present)
    private static List<String> extractExamplesHeaders(String examplesSection) {
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        if (examplesSection == null || examplesSection.isBlank()) return new ArrayList<>(headers);

        for (String line : examplesSection.split("\\R")) {
            if (!line.contains("|")) continue;
            String[] parts = line.split("\\|");
            for (String part : parts) {
                String c = part.trim();
                if (!c.isEmpty() && !c.equalsIgnoreCase("Examples:")) headers.add(c);
            }
        }
        return new ArrayList<>(headers);
    }

    private static boolean containsEmptyStringPlaceholders(List<String> steps) {
        for (String s : steps) if (s.contains("\"\"")) return true;
        return false;
    }

    // Optional: if steps still have "" and TestRail has headers, map "" in order of headers
    private static List<String> fillEmptyStringsUsingHeaders(List<String> steps, List<String> headers) {
        List<String> out = new ArrayList<>();
        int idx = 0;

        for (String step : steps) {
            Matcher m = Pattern.compile("\"\"").matcher(step);
            if (!m.find()) { out.add(step); continue; }
            m.reset();

            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String h = headers.get(idx % headers.size());
                m.appendReplacement(sb, Matcher.quoteReplacement("\"<" + h + ">\""));
                idx++;
            }
            m.appendTail(sb);
            out.add(sb.toString());
        }
        return out;
    }

    /** COMMON METHOD: builds `Examples:` by scanning placeholders in Gherkin and fetching values by name from testdata.json */
    private static String buildExamplesFromPlaceholders(String caseKey, List<String> steps) {
        // 1) Collect placeholders in the order they appear across all steps
        LinkedHashSet<String> placeholders = new LinkedHashSet<>();
        Pattern ph = Pattern.compile("<([A-Za-z0-9_]+)>");
        for (String s : steps) {
            Matcher m = ph.matcher(s);
            while (m.find()) placeholders.add(m.group(1));
        }

        if (placeholders.isEmpty()) return "\n";

        // 2) Load case data first, then global fallback
        JSONObject data = ConfigReader.getJsonObject(caseKey);
        if (data == null || data.isEmpty()) data = ConfigReader.getFullTestData();
        if (data == null) data = new JSONObject();

        // 3) Build table header and row by exact-name matching
        StringBuilder sb = new StringBuilder();
        sb.append("\nExamples:\n");
        sb.append("  | ");
        for (String h : placeholders) sb.append(h).append(" | ");
        sb.append("\n  | ");
        for (String h : placeholders) {
            String val = data.has(h) ? data.optString(h, "") : "<MISSING:" + h + ">";
            sb.append(val).append(" | ");
        }
        sb.append("\n");
        return sb.toString();
    }

    /* -------------------------------------------------------
     * Utils
     * ------------------------------------------------------- */

    private static String oneLine(String s) {
        return s == null ? "" : s.replace("\r", " ").replace("\n", " ").trim();
    }
}
