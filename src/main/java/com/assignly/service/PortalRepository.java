package com.assignly.service;

import com.assignly.database.DatabaseManager;
import com.assignly.security.EncryptionUtil;
import com.assignly.util.ErrorReporter;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ported from the Android app's PortalRepository.kt – handles all direct HTTP
 * communication with the COMSATS SIS portal, exactly mirroring the Android
 * networking patterns (form-field discovery, ASP.NET postback handling, login
 * verification via protected page, session cookie management, CAPTCHA/security
 * verification detection).
 */
public class PortalRepository {

    // ---------- Result types (mirror Kotlin sealed classes) ----------
    public sealed interface LoginResult {
        record Success() implements LoginResult {}
        record InvalidCredentials() implements LoginResult {}
        record CaptchaRequired() implements LoginResult {}
        record Error(String message) implements LoginResult {}
    }

    public sealed interface UploadResult {
        record Success() implements UploadResult {}
        record NetworkError() implements UploadResult {}
        record Timeout() implements UploadResult {}
        record Rejected(String reason) implements UploadResult {}
        record Error(String message) implements UploadResult {}
    }

    public sealed interface DownloadResult {
        record Success(byte[] bytes, String fileName, String mimeType) implements DownloadResult {}
        record NetworkError() implements DownloadResult {}
        record Rejected(String reason) implements DownloadResult {}
        record Error(String message) implements DownloadResult {}
    }

    public record InstructionFile(String fileName, String downloadLink) {}

    public sealed interface InstructionFilesResult {
        record Success(List<InstructionFile> files) implements InstructionFilesResult {}
        record NetworkError() implements InstructionFilesResult {}
        record Rejected(String reason) implements InstructionFilesResult {}
        record Error(String message) implements InstructionFilesResult {}
    }

    public record PostBackInfo(String target, String argument) {}
    public record PostBackLink(PostBackInfo info, String sourcePageUrl) {}
    public record HtmlDownloadCandidate(String url, PostBackInfo postBackInfo) {}

    public record StudentProfile(String name, String rollNo, String program) {}

    /** All data parsed from Dashboard.aspx */
    public record DashboardData(
            String photoUrl,
            Map<String, String> studentInfo,       // ordered: Name, Father Name, Roll No, etc.
            Map<String, Double> attendanceOverall   // courseName -> percentage
    ) {}

    public record GpaHistoryData(
            String semesterTitle,
            double sgpa,
            double cgpa,
            double creditHours
    ) {}

    public record ScholarshipTable(
            String title,
            List<String> headers,
            List<List<String>> data
    ) {}

    // ---------- Added for Settings Sub-Tabs ----------
    public record ProfileInfo(String cellNetwork, String cellNumber, String email) {}
    public record LoginHistoryEntry(String no, String time, String date, String ip) {}


    // ---------- Constants ----------
    private static final String BASE_URL = "https://sis.cuiatd.edu.pk";
    private static final String BASE_HOST = "sis.cuiatd.edu.pk";
    private static final String LOGIN_URL = BASE_URL + "/Login.aspx";
    public static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Safari/605.1.15";
    private static final DateTimeFormatter PORTAL_DEADLINE_FMT =
            DateTimeFormatter.ofPattern("MMM dd ,yyyy HH:mm", Locale.US);
    private static final ZoneId PORTAL_ZONE = ZoneId.systemDefault();
    private static final Pattern COURSE_CODE_PATTERN = Pattern.compile("([A-Z]{2,4}\\s*-?\\d{2,4})");
    private static final Pattern ATTENDANCE_PATTERN =
            Pattern.compile("([A-Z]{2,4}\\s*-?\\d{2,4})[^0-9]{0,10}(\\d{1,3}(?:\\.\\d+)?)\\s*%", Pattern.CASE_INSENSITIVE);
    private static final Pattern GPA_PAIR_PATTERN =
            Pattern.compile("SGPA\\s*(?::|=)?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*CGPA\\s*(?::|=)?\\s*([0-9]+(?:\\.[0-9]+)?)",
                    Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> INFO_KEY_ALIASES = Map.ofEntries(
            Map.entry("name", "Name"),
            Map.entry("student name", "Name"),
            Map.entry("father name", "Father Name"),
            Map.entry("roll no", "Roll No"),
            Map.entry("roll number", "Roll No"),
            Map.entry("registration no", "Registration No"),
            Map.entry("registration number", "Registration No"),
            Map.entry("reg no", "Registration No"),
            Map.entry("program", "Program"),
            Map.entry("degree", "Program"),
            Map.entry("semester", "Semester"),
            Map.entry("batch", "Batch"),
            Map.entry("section", "Section")
    );

    // ---------- Session state ----------
    private final DatabaseManager databaseManager;
    private volatile boolean offlineMode = false;
    private final Map<String, Map<String, Cookie>> cookieStore = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.CompletableFuture<String>> inFlightRequests = new ConcurrentHashMap<>();
    private volatile String currentStudentName;
    private volatile String currentStudentPhotoUrl;
    private Runnable onSessionExpiredCallback;
    private volatile boolean suppressSessionExpiredCallback = false;

    public PortalRepository() {
        this.databaseManager = null;
    }

    public PortalRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
    }

    public void setOnSessionExpiredCallback(Runnable callback) {
        this.onSessionExpiredCallback = callback;
    }

    public void setSuppressSessionExpiredCallback(boolean suppress) {
        this.suppressSessionExpiredCallback = suppress;
    }

    private final OkHttpClient client = new OkHttpClient.Builder()
            .cookieJar(new CookieJar() {
                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    Map<String, Cookie> host = cookieStore.computeIfAbsent(url.host(), k -> new ConcurrentHashMap<>());
                    for (Cookie c : cookies) host.put(c.name(), c);
                    saveSessionCookies();
                }
                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    Map<String, Cookie> host = cookieStore.get(url.host());
                    return host == null ? List.of() : new ArrayList<>(host.values());
                }
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .addInterceptor(chain -> {
                okhttp3.Request request = chain.request();
                String requestUrl = request.url().toString();
                
                // Track authentication state (cookies) before request is sent
                List<Cookie> cookies = new ArrayList<>();
                Map<String, Cookie> hostCookies = cookieStore.get(request.url().host());
                if (hostCookies != null) {
                    cookies.addAll(hostCookies.values());
                }
                
                boolean hasAuth = hasSessionCookiesForHost(request.url().host());
                StringBuilder cookieLog = new StringBuilder();
                cookieLog.append(hasAuth ? "Authenticated (Session/Auth Cookies Present)" : "Not Authenticated (No Session/Auth Cookies)");
                if (!cookies.isEmpty()) {
                    cookieLog.append(" | Active Cookies: ");
                    for (Cookie c : cookies) {
                        cookieLog.append(c.name()).append("=").append(c.value()).append("; ");
                    }
                }
                String authState = cookieLog.toString();

                okhttp3.Response response = null;
                Exception exception = null;
                try {
                    response = chain.proceed(request);
                    
                    String urlLower = request.url().toString().toLowerCase();
                    if (!urlLower.contains("login.aspx")) {
                        if (response.isSuccessful() && response.body() != null) {
                            okhttp3.MediaType mediaType = response.body().contentType();
                            if (mediaType != null && mediaType.subtype().equalsIgnoreCase("html")) {
                                String bodyString = response.peekBody(1024 * 1024).string();
                                if (bodyString.contains("txtUsername") || bodyString.contains("btnLogin")) {
                                    // Check if this is actually a Cloudflare challenge page, not a real login redirect
                                    String bodyLower = bodyString.toLowerCase();
                                    boolean isCloudflareChallenge = bodyLower.contains("cf_chl")
                                            || bodyLower.contains("challenges.cloudflare.com")
                                            || bodyLower.contains("cf-browser-verification")
                                            || bodyLower.contains("cf-turnstile");
                                    if (onSessionExpiredCallback != null && !suppressSessionExpiredCallback && !isCloudflareChallenge) {
                                        onSessionExpiredCallback.run();
                                    }
                                    throw new SessionExpiredException("Session expired. Portal redirected to Login.");
                                }
                            }
                        }
                    }
                    return response;
                } catch (Exception e) {
                    exception = e;
                    throw e;
                } finally {
                    StringBuilder logBuilder = new StringBuilder();
                    logBuilder.append("\n========================================\n");
                    logBuilder.append("Request:\n").append(requestUrl).append("\n\n");
                    logBuilder.append("Authentication State:\n").append(authState).append("\n\n");
                    
                    if (exception != null) {
                        logBuilder.append("Status:\n").append("FAILED\n\n");
                        logBuilder.append("Exception Message:\n").append(exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName()).append("\n");
                    } else if (response != null) {
                        int code = response.code();
                        logBuilder.append("Status:\n").append(code).append("\n\n");
                        
                        if (code >= 300 && code < 400) {
                            String redirectUrl = response.header("Location");
                            logBuilder.append("Redirect Target:\n").append(redirectUrl != null ? redirectUrl : "Unknown").append("\n\n");
                        }
                        
                        long len = -1;
                        if (response.body() != null) {
                            len = response.body().contentLength();
                            if (len <= 0) {
                                try {
                                    len = response.peekBody(1024 * 1024).bytes().length;
                                } catch (Exception ignored) {}
                            }
                        }
                        logBuilder.append("Response Length:\n").append(len).append("\n");
                    }
                    logBuilder.append("========================================\n");
                    System.out.println(logBuilder.toString());
                    
                    // Also write to assignly.log
                    synchronized (ErrorReporter.class) {
                        try {
                            java.nio.file.Files.writeString(
                                com.assignly.util.AppDirectoryHelper.getLogPath(), 
                                logBuilder.toString(), 
                                java.nio.file.StandardOpenOption.CREATE, 
                                java.nio.file.StandardOpenOption.APPEND
                            );
                        } catch (Exception ignored) {}
                    }
                }
            })
            .build();

    // ---------- Public accessors ----------
    public String getPortalBaseUrl()   { return BASE_URL; }
    public String getPortalLoginUrl()  { return LOGIN_URL; }
    public String getCurrentStudentName() { return currentStudentName; }
    public String getCurrentStudentPhotoUrl() { return currentStudentPhotoUrl; }

    public boolean checkConnectivity() {
        try {
            // Using a lightweight HEAD request to check if the portal is reachable
            okhttp3.Request request = new okhttp3.Request.Builder()
                .url(BASE_URL)
                .head()
                .build();
            try (okhttp3.Response response = client.newCall(request).execute()) {
                // Any HTTP response means the portal server is responding and we are online.
                return true;
            }
        } catch (java.io.IOException e) {
            return false;
        }
    }

    // ---------- Login (mirroring Kotlin login()) ----------
    public LoginResult login(String username, String password) {
        try {
            String normalizedUser = username.trim().toUpperCase(Locale.ROOT);
            String[] parts = normalizedUser.split("-");
            if (parts.length != 3 || password.isBlank()) {
                return new LoginResult.InvalidCredentials();
            }
            clearSessionState();

            String sessCode = parts[0];
            String progCode = parts[1];
            String rollNumber = parts[2];
            String sessSeason = sessCode.toUpperCase().contains("SP") ? "Spring" : "Fall";
            String sessYear = sessCode.replaceAll("[^0-9]", "");

            // 1. GET login page
            Request initialGet = new Request.Builder()
                    .url(LOGIN_URL)
                    .header("User-Agent", USER_AGENT)
                    .build();
            String[] initialPayload;
            try (Response response = client.newCall(initialGet).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                String resolvedUrl = response.request().url().toString();
                if (!response.isSuccessful()) {
                    if (isSecurityVerificationResponse(response, resolvedUrl, body))
                        return new LoginResult.CaptchaRequired();
                    return new LoginResult.Error("HTTP " + response.code());
                }
                if (isSecurityVerificationResponse(response, resolvedUrl, body))
                    return new LoginResult.CaptchaRequired();
                if (body.isBlank()) return new LoginResult.Error("Empty server response");
                initialPayload = new String[]{resolvedUrl, body};
            }

            if (isSecurityVerificationPage(initialPayload[0], initialPayload[1]))
                return new LoginResult.CaptchaRequired();

            Document doc = Jsoup.parse(initialPayload[1]);
            Element form = doc.select("form").first();
            if (form == null) return new LoginResult.Error("Form not found");

            FormBody.Builder formBuilder = new FormBody.Builder();

            // Discover field names
            String sessionFieldName = "", programFieldName = "", rollnoFieldName = "";
            String userFieldName = "", passFieldName = "", btnFieldName = "", btnValue = "Login";

            for (Element el : form.select("input, select")) {
                String name = el.attr("name");
                String id = el.attr("id");
                String type = el.attr("type");
                String nName = normalizeToken(name);
                String nId = normalizeToken(id);

                if ("select".equals(el.tagName())) {
                    if (nName.contains("session") || nId.contains("session") || name.contains("Session"))
                        sessionFieldName = name;
                    else if (nName.contains("program") || nId.contains("program") || name.contains("Program"))
                        programFieldName = name;
                } else {
                    if ("password".equalsIgnoreCase(type) || nName.contains("password") || nId.contains("password"))
                        passFieldName = name;
                    else if (nName.contains("rollno") || nName.contains("roll") || nId.contains("rollno") || nId.contains("roll") || name.contains("RollNo"))
                        rollnoFieldName = name;
                    else if (nName.contains("username") || nName.contains("userid") || nId.contains("username") || name.contains("Username"))
                        userFieldName = name;
                }
            }

            // Find login button
            for (Element el : form.select("input[type=submit], button[type=submit], button[name]")) {
                String name = el.attr("name");
                if (normalizeToken(name).contains("login") || normalizeToken(name).contains("signin") || name.toLowerCase().contains("btn")) {
                    btnFieldName = name;
                    btnValue = el.attr("value").isEmpty() ? (el.text().isEmpty() ? "Login" : el.text()) : el.attr("value");
                }
            }

            // Populate ALL form fields (hidden tokens, dropdowns, etc.)
            for (Element el : form.select("input, select")) {
                String name = el.attr("name");
                if (name.isEmpty() || name.equals(userFieldName) || name.equals(passFieldName) ||
                    name.equals(btnFieldName) || name.equals(sessionFieldName) ||
                    name.equals(programFieldName) || name.equals(rollnoFieldName)) continue;

                String value = el.attr("value");
                if ("select".equals(el.tagName())) {
                    Element selected = el.select("option[selected]").first();
                    value = selected != null ? selected.attr("value") : (el.select("option").first() != null ? el.select("option").first().attr("value") : "");
                }
                formBuilder.add(name, value);
            }

            // Session dropdown
            if (!sessionFieldName.isEmpty()) {
                Element dropdown = form.selectFirst("select[name=" + sessionFieldName + "]");
                if (dropdown != null) {
                    Elements options = dropdown.select("option");
                    String finalSessSeason = sessSeason;
                    String finalSessYear = sessYear;
                    String finalSessCode = sessCode;
                    Element matched = options.stream()
                            .filter(o -> (o.text().toLowerCase().contains(finalSessSeason.toLowerCase()) && o.text().contains(finalSessYear))
                                    || o.attr("value").toLowerCase().contains(finalSessCode.toLowerCase()))
                            .findFirst().orElse(options.first());
                    formBuilder.add(sessionFieldName, matched != null ? matched.attr("value") : "");
                }
            }

            // Program dropdown
            if (!programFieldName.isEmpty()) {
                Element dropdown = form.selectFirst("select[name=" + programFieldName + "]");
                if (dropdown != null) {
                    Elements options = dropdown.select("option");
                    Element matched = options.stream()
                            .filter(o -> o.text().equalsIgnoreCase(progCode) || o.attr("value").equalsIgnoreCase(progCode)
                                    || normalizeToken(o.text()).contains(normalizeToken(progCode)))
                            .findFirst().orElse(options.first());
                    formBuilder.add(programFieldName, matched != null ? matched.attr("value") : "");
                }
            }

            // Roll number
            if (!rollnoFieldName.isEmpty()) formBuilder.add(rollnoFieldName, rollNumber);
            if (!passFieldName.isEmpty()) formBuilder.add(passFieldName, password);
            if (!btnFieldName.isEmpty()) formBuilder.add(btnFieldName, btnValue);

            // 2. POST login
            String formAction = form.attr("action");
            String postUrl = formAction.isBlank() ? LOGIN_URL
                    : formAction.startsWith("http") ? formAction
                    : formAction.startsWith("/") ? BASE_URL + formAction
                    : BASE_URL + "/" + formAction;

            Request postRequest = new Request.Builder()
                    .url(postUrl)
                    .post(formBuilder.build())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Referer", LOGIN_URL)
                    .header("Origin", BASE_URL)
                    .header("User-Agent", USER_AGENT)
                    .build();

            String[] finalPayload;
            try (Response response = client.newCall(postRequest).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                String resolvedUrl = response.request().url().toString();
                if (!response.isSuccessful()) {
                    if (isSecurityVerificationResponse(response, resolvedUrl, body)) return new LoginResult.CaptchaRequired();
                    return new LoginResult.Error("HTTP " + response.code());
                }
                if (isSecurityVerificationResponse(response, resolvedUrl, body)) return new LoginResult.CaptchaRequired();
                if (body.isBlank()) return new LoginResult.Error("Empty server response");
                finalPayload = new String[]{resolvedUrl, body};
            }

            if (isSecurityVerificationPage(finalPayload[0], finalPayload[1])) return new LoginResult.CaptchaRequired();

            // 3. Verify on protected page
            Request verifyRequest = new Request.Builder()
                    .url(BASE_URL + "/CoursePortal.aspx")
                    .header("Referer", LOGIN_URL)
                    .header("User-Agent", USER_AGENT)
                    .build();

            String[] verifyPayload;
            try (Response response = client.newCall(verifyRequest).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                String resolvedUrl = response.request().url().toString();
                if (!response.isSuccessful()) {
                    if (isSecurityVerificationResponse(response, resolvedUrl, body)) return new LoginResult.CaptchaRequired();
                    return new LoginResult.Error("HTTP " + response.code());
                }
                if (isSecurityVerificationResponse(response, resolvedUrl, body)) return new LoginResult.CaptchaRequired();
                if (body.isBlank()) return new LoginResult.Error("Empty server response");
                verifyPayload = new String[]{resolvedUrl, body};
            }

            if (isSecurityVerificationPage(verifyPayload[0], verifyPayload[1])) return new LoginResult.CaptchaRequired();

            StudentProfile profile = parseStudentProfileFromHtml(verifyPayload[1]);
            boolean profileMatches = doesProfileMatchRequestedUsername(normalizedUser, profile, verifyPayload[1]);
            currentStudentName = profile.name() != null ? profile.name() : parseStudentNameFromHtml(finalPayload[1]);
            currentStudentPhotoUrl = parseStudentPhotoUrlFromHtml(verifyPayload[1], verifyPayload[0]);

            boolean verifyShowsLogin = isLoginPage(verifyPayload[0], verifyPayload[1]);
            boolean hasSession = hasSessionCookiesForHost(BASE_HOST);

            if (!verifyShowsLogin && hasSession && profileMatches) {
                return new LoginResult.Success();
            } else {
                clearSessionState();
                return new LoginResult.InvalidCredentials();
            }
        } catch (Exception e) {
            clearSessionState();
            return new LoginResult.Error(e.getMessage() != null ? e.getMessage() : "Network error");
        }
    }

    // ---------- Fetch Dashboard data ----------
    public DashboardData fetchDashboard() {
        String html = fetchPageHtml("Dashboard.aspx");
        if (html == null) return null;
        return parseDashboard(html);
    }

    public DashboardData parseDashboard(String html) {
        if (html == null || html.isBlank()) return null;
        Document doc = Jsoup.parse(html, BASE_URL + "/Dashboard.aspx");

        // 1. Parse student info table (key-value pairs from the top table)
        Map<String, String> info = new LinkedHashMap<>();
        for (Element row : doc.select("tr")) {
            Elements cells = row.select("th, td");
            if (cells.size() < 2) continue;
            // Handle rows with 4 cells (two key-value pairs per row)
            for (int i = 0; i + 1 < cells.size(); i += 2) {
                String key = cells.get(i).text().trim().replaceAll("\\s*:$", "");
                String value = cells.get(i + 1).text().trim();
                if (!key.isEmpty() && !value.isEmpty()) {
                    info.putIfAbsent(key, value);
                }
            }
        }

        // Fallback for registered courses if not parsed inside table rows
        Element regCoursesSpan = doc.selectFirst("[id*=lbl_RegisteredCourses], [id*=lblRegisteredCourses]");
        if (regCoursesSpan != null && !regCoursesSpan.text().trim().isEmpty()) {
            info.put("Registered Courses", regCoursesSpan.text().trim());
        }

        // 2. Parse photo URL
        String photoUrl = parseStudentPhotoUrlFromHtml(html, BASE_URL + "/Dashboard.aspx");
        if (photoUrl != null) currentStudentPhotoUrl = photoUrl;

        // Update student name from the info table
        String name = info.get("Name");
        if (name != null && !name.isBlank()) currentStudentName = name;

        // 3. Parse attendance data – look for overall attendance percentages
        // The page has a table/chart with course codes and percentages
        Map<String, Double> attendance = new LinkedHashMap<>();

        // Parse from ECharts scripts if present (primary source on COMSATS portal)
        for (Element script : doc.select("script")) {
            String scriptContent = script.data();
            if (scriptContent.contains("courseAttendanceChart")) {
                Pattern seriesPattern = Pattern.compile(
                    "name\\s*:\\s*['\"]([^'\"]+)['\"]\\s*,.*?data\\s*:\\s*\\[([^\\]]+)\\]", 
                    Pattern.DOTALL
                );
                Matcher matcher = seriesPattern.matcher(scriptContent);
                while (matcher.find()) {
                    String courseName = matcher.group(1).trim();
                    String dataStr = matcher.group(2);
                    for (String part : dataStr.split(",")) {
                        part = part.trim();
                        if (!part.equalsIgnoreCase("null") && !part.isEmpty()) {
                            try {
                                double val = Double.parseDouble(part);
                                if (val >= 0 && val <= 100) {
                                    attendance.put(courseName, val);
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }

        // Fallback: Try to find attendance-related text patterns in spans/tds/tables
        if (attendance.isEmpty()) {
            for (Element el : doc.select("span, td, th")) {
                String text = el.text().trim();
                // Look for patterns like course codes (3-4 letters + 3 digits)
                if (text.matches("^[A-Z]{2,4}\\d{2,4}$")) {
                    // Try to find a sibling or nearby element with a percentage
                    Element nextSib = el.nextElementSibling();
                    if (nextSib != null) {
                        String pct = nextSib.text().trim().replaceAll("[^0-9.]", "");
                        try {
                            double val = Double.parseDouble(pct);
                            if (val >= 0 && val <= 100) attendance.put(text, val);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            // Also try parsing from any table rows that look like "CourseName | percentage"
            for (Element row : doc.select("tr")) {
                Elements cells = row.select("td");
                if (cells.size() >= 2) {
                    String first = cells.first().text().trim();
                    String last = cells.last().text().trim().replaceAll("[^0-9.]", "");
                    if (first.matches(".*[A-Z]{2,4}\\d{2,4}.*") && !last.isEmpty()) {
                        try {
                            double val = Double.parseDouble(last);
                            if (val >= 0 && val <= 100) attendance.put(first, val);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        return new DashboardData(photoUrl, info, attendance);
    }

    public String fetchScholarshipConditionsPdfUrl() {
        String html = fetchPageHtml("scholarship/ViewScholarshipStatuse.aspx");
        if (html != null && !html.isBlank()) {
            Document doc = Jsoup.parse(html, BASE_URL + "/scholarship/ViewScholarshipStatuse.aspx");
            for (Element a : doc.select("a[href]")) {
                String href = a.attr("abs:href");
                String text = a.text().toLowerCase();
                if (href.toLowerCase().endsWith(".pdf") && (text.contains("condition") || text.contains("general") || href.toLowerCase().contains("condition"))) {
                    return href;
                }
            }
            for (Element a : doc.select("a[href]")) {
                String href = a.attr("abs:href");
                if (href.toLowerCase().endsWith(".pdf")) {
                    return href;
                }
            }
        }
        return "scholarship/Genral%20Scholarships%20Conditions%20April%202026.pdf";
    }

    public Map<String, String> parseCourseNames(String html) {
        Map<String, String> map = new HashMap<>();
        if (html == null || html.isBlank()) return map;
        try {
            Document doc = Jsoup.parse(html);
            for (Element table : doc.select("table")) {
                for (Element row : table.select("tr")) {
                    Elements cells = row.select("td");
                    if (cells.size() >= 2) {
                        String first = cells.get(0).text().trim();
                        String second = cells.get(1).text().trim();
                        if (first.matches("^[A-Z]{2,4}\\s*-?\\d{2,4}$") && !second.isEmpty()) {
                            map.put(first, second);
                            String normKey = first.replaceAll("\\s+|-", "").toUpperCase();
                            map.put(normKey, second);
                        }
                    }
                }
            }
        } catch (Exception e) {
            ErrorReporter.logError("PortalRepository#parseCourseNames", e);
        }
        return map;
    }

    public List<GpaHistoryData> parseGpaHistory(String html) {
        List<GpaHistoryData> history = new ArrayList<>();
        if (html == null || html.isBlank()) return history;
        Document doc = Jsoup.parse(html);
        
        Elements allTables = doc.select("table");
        for (int i = 0; i < allTables.size(); i++) {
            Element table = allTables.get(i);
            Elements rows = table.select("tr");
            if (rows.size() < 2) continue;
            
            // Validate that this is actually a transcript table (and not a personal info table)
            Elements firstRowCells = rows.first().select("th, td");
            boolean isTranscript = false;
            for (Element cell : firstRowCells) {
                String lower = cell.text().toLowerCase();
                if (lower.contains("course") || lower.contains("credit") || lower.contains("marks") || lower.contains("grade") || lower.contains("gpa")) {
                    isTranscript = true;
                    break;
                }
            }
            if (!isTranscript) continue;
            
            // Try to find SGPA and CGPA in this table (usually at the bottom)
            double sgpa = -1.0;
            double cgpa = -1.0;
            double semesterCredits = 0.0;
            String semesterTitle = "Semester";
            
            // Look for title above table
            Element prev = table.previousElementSibling();
            while (prev != null) {
                String txt = prev.text().trim();
                if (!txt.isEmpty()) {
                    semesterTitle = txt;
                    break;
                }
                prev = prev.previousElementSibling();
            }

            // Calculate total credits for this semester table
            for (Element row : rows) {
                Elements cells = row.select("td, th");
                if (cells.size() >= 4) {
                    String creditStr = "";
                    String gpStr = "";
                    String lgStr = "";
                    if (cells.size() >= 6) {
                        creditStr = cells.get(2).text().trim();
                        lgStr = cells.get(4).text().trim();
                        gpStr = cells.get(5).text().trim();
                    } else {
                        creditStr = cells.get(1).text().trim();
                        lgStr = cells.get(2).text().trim();
                        gpStr = cells.get(3).text().trim();
                    }
                    try {
                        double credit = Double.parseDouble(creditStr);
                        if (credit > 0) {
                            boolean isNonCredit = gpStr.toLowerCase().contains("non credit") || 
                                                  lgStr.toLowerCase().contains("non credit") ||
                                                  creditStr.toLowerCase().contains("non credit");
                            if (!isNonCredit) {
                                semesterCredits += credit;
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            for (Element row : rows) {
                String rowText = row.text().toUpperCase();
                // Strategy 1: Fallback checks for explicit ID patterns in rows or table siblings
                if (sgpa == -1.0) {
                    Element sSpan = table.parent().selectFirst("[id*=lblSGPA], [id*=sgpa]");
                    if (sSpan != null) {
                        try { sgpa = Double.parseDouble(sSpan.text().trim()); } catch (Exception ex) {
                            ErrorReporter.logError("PortalRepository#parseGpaHistory fallback sgpa", ex);
                        }
                    }
                }
                if (cgpa == -1.0) {
                    Element cSpan = table.parent().selectFirst("[id*=lblCGPA], [id*=cgpa]");
                    if (cSpan != null) {
                        try { cgpa = Double.parseDouble(cSpan.text().trim()); } catch (Exception ex) {
                            ErrorReporter.logError("PortalRepository#parseGpaHistory fallback cgpa", ex);
                        }
                    }
                }

                // Strategy 2: Simple regex to find SGPA and CGPA numbers in the row
                if ((sgpa == -1.0 || cgpa == -1.0) && (rowText.contains("SGPA") || rowText.contains("CGPA"))) {
                    Matcher mSgpa = Pattern.compile("SGPA\\s*(?::|\\-|=)?\\s*([0-9]+\\.[0-9]+)").matcher(rowText);
                    if (mSgpa.find()) {
                        try {
                            sgpa = Double.parseDouble(mSgpa.group(1));
                        } catch (NumberFormatException ex) {
                            ErrorReporter.logError("PortalRepository#parseGpaHistory sgpa", ex);
                        }
                    }
                    
                    Matcher mCgpa = Pattern.compile("CGPA\\s*(?::|\\-|=)?\\s*([0-9]+\\.[0-9]+)").matcher(rowText);
                    if (mCgpa.find()) {
                        try {
                            cgpa = Double.parseDouble(mCgpa.group(1));
                        } catch (NumberFormatException ex) {
                            ErrorReporter.logError("PortalRepository#parseGpaHistory cgpa", ex);
                        }
                    }
                }
            }

            // Strategy 3: Check the immediately following table for CGPA (COMSATS real portal layout)
            if (cgpa == -1.0 && i + 1 < allTables.size()) {
                Element nextTable = allTables.get(i + 1);
                String nextTableText = nextTable.text().toUpperCase();
                if (nextTableText.contains("CGPA")) {
                    Matcher mCgpa = Pattern.compile("CGPA\\s*(?::|\\-|=)?\\s*([0-9]+\\.[0-9]+)").matcher(nextTableText);
                    if (mCgpa.find()) {
                        try { cgpa = Double.parseDouble(mCgpa.group(1)); } catch (NumberFormatException ignored) {}
                    }
                    Matcher mSgpa = Pattern.compile("SGPA\\s*(?::|\\-|=)?\\s*([0-9]+\\.[0-9]+)").matcher(nextTableText);
                    if (mSgpa.find()) {
                        try { sgpa = Double.parseDouble(mSgpa.group(1)); } catch (NumberFormatException ignored) {}
                    }
                }
            }

            // Strategy 4: Dynamic SGPA Calculation from course rows as a fallback
            if (sgpa == -1.0) {
                double totalQualityPoints = 0.0;
                double totalCredits = 0.0;
                for (Element row : rows) {
                    Elements cells = row.select("td, th");
                    if (cells.size() >= 4) {
                        String creditStr = "";
                        String gpStr = "";
                        if (cells.size() >= 6) {
                            creditStr = cells.get(2).text().trim();
                            gpStr = cells.get(5).text().trim();
                        } else if (cells.size() >= 4) {
                            creditStr = cells.get(1).text().trim();
                            gpStr = cells.get(3).text().trim();
                        }
                        try {
                            double credit = Double.parseDouble(creditStr);
                            double gp = Double.parseDouble(gpStr);
                            if (credit > 0 && gp >= 0.0 && gp <= 4.0) {
                                totalQualityPoints += credit * gp;
                                totalCredits += credit;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
                if (totalCredits > 0) {
                    sgpa = totalQualityPoints / totalCredits;
                }
            }
            
            history.add(new GpaHistoryData(semesterTitle, sgpa, cgpa, semesterCredits));
        }
        
        return history;
    }

    /** Download student photo as raw bytes (for JavaFX Image) using Dashboard as referer */
    public byte[] fetchPhotoBytes(String photoUrl) {
        return fetchPhotoBytes(photoUrl, BASE_URL + "/Dashboard.aspx");
    }

    /** Download an image as raw bytes specifying a custom referer */
    public byte[] fetchPhotoBytes(String photoUrl, String referer) {
        if (offlineMode) return null;
        if (photoUrl == null || photoUrl.isBlank()) return null;
        try {
            Request request = new Request.Builder()
                    .url(photoUrl)
                    .header("Referer", referer)
                    .header("User-Agent", USER_AGENT)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return null;
                return response.body().bytes();
            }
        } catch (IOException e) {
            return null;
        }
    }

    /** Parse dropdown options from an ASPX page. Returns list of [value, text] pairs. */
    public List<String[]> parseDropdownOptions(String html, String dropdownIdFragment) {
        List<String[]> options = new ArrayList<>();
        if (html == null) return options;
        Document doc = Jsoup.parse(html);
        
        Element bestSelect = null;
        for (Element el : doc.select("select")) {
            String id = el.attr("id").toLowerCase();
            String name = el.attr("name").toLowerCase();
            if (id.contains(dropdownIdFragment.toLowerCase()) || name.contains(dropdownIdFragment.toLowerCase())) {
                bestSelect = el;
                break;
            }
        }
        
        // Fallback: If not found, try to find any dropdown that looks like it has course codes (e.g. CSC101, HUM100) or is the only dropdown
        if (bestSelect == null) {
            Elements selects = doc.select("select");
            for (Element el : selects) {
                if (el.text().matches(".*[A-Z]{3,4}-?\\d{3,4}.*") || el.text().toLowerCase().contains("select")) {
                    bestSelect = el;
                    break;
                }
            }
            if (bestSelect == null && !selects.isEmpty()) {
                bestSelect = selects.first(); // Ultimate fallback
            }
        }
        
        if (bestSelect == null) return options;
        
        for (Element opt : bestSelect.select("option")) {
            String val = opt.attr("value");
            String text = opt.text().trim();
            if (!val.isEmpty() && !text.toLowerCase().startsWith("select") && !text.startsWith("--")) {
                options.add(new String[]{val, text});
            }
        }
        return options;
    }

    public String findDropdownName(String html, String dropdownIdFragment) {
        if (html == null) return null;
        Document doc = Jsoup.parse(html);
        Element bestSelect = null;
        for (Element el : doc.select("select")) {
            String id = el.attr("id").toLowerCase();
            String name = el.attr("name").toLowerCase();
            if (id.contains(dropdownIdFragment.toLowerCase()) || name.contains(dropdownIdFragment.toLowerCase())) {
                bestSelect = el;
                break;
            }
        }
        
        if (bestSelect == null) {
            Elements selects = doc.select("select");
            for (Element el : selects) {
                if (el.text().matches(".*[A-Z]{3,4}-?\\d{3,4}.*") || el.text().toLowerCase().contains("select")) {
                    bestSelect = el;
                    break;
                }
            }
            if (bestSelect == null && !selects.isEmpty()) {
                bestSelect = selects.first();
            }
        }
        
        return bestSelect != null ? bestSelect.attr("name") : null;
    }

    /**
     * Perform an ASP.NET postback by event target (like clicking an LinkButton).
     */
    public String postbackEvent(String relativeUrl, String eventTarget) {
        try {
            String pageUrl = BASE_URL + "/" + relativeUrl;

            // 1. GET to grab viewstate
            Request getReq = new Request.Builder()
                    .url(pageUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", BASE_URL + "/Dashboard.aspx")
                    .build();
            String pageHtml;
            try (Response resp = client.newCall(getReq).execute()) {
                if (!resp.isSuccessful()) return null;
                pageHtml = resp.body() != null ? resp.body().string() : "";
            }

            Document doc = Jsoup.parse(pageHtml);
            Element form = doc.select("form").first();
            if (form == null) return null;

            // 2. Build POST body
            FormBody.Builder formBuilder = new FormBody.Builder();
            for (Element input : form.select("input[type=hidden]")) {
                String name = input.attr("name");
                String value = input.attr("value");
                if (!name.isEmpty() && !name.equals("__EVENTTARGET") && !name.equals("__EVENTARGUMENT")) {
                    formBuilder.add(name, value);
                }
            }
            formBuilder.add("__EVENTTARGET", eventTarget);
            formBuilder.add("__EVENTARGUMENT", "");

            // If the event target is inside an UpdatePanel, we must provide the ScriptManager parameter
            // We use a generic UpdatePanel ID but often ASP.NET AJAX also relies on the Delta header.
            formBuilder.add("ctl00$Manager", "ctl00$DataContent$upSummary|" + eventTarget);

            // 3. POST
            String formAction = form.attr("action");
            String postUrl = formAction.isBlank() ? pageUrl : (formAction.startsWith("http") ? formAction : BASE_URL + (formAction.startsWith("/") ? "" : "/") + formAction);

            Request postReq = new Request.Builder()
                    .url(postUrl)
                    .post(formBuilder.build())
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("Referer", pageUrl)
                    .header("Origin", BASE_URL)
                    .header("User-Agent", USER_AGENT)
                    .header("X-MicrosoftAjax", "Delta=true")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .build();

            try (Response resp = client.newCall(postReq).execute()) {
                if (!resp.isSuccessful()) return null;
                return resp.body() != null ? resp.body().string() : "";
            }
        } catch (IOException e) {
            return null;
        }
    }

    public String postbackEventStandard(String relativeUrl, String eventTarget) {
        try {
            String pageUrl = BASE_URL + "/" + relativeUrl;

            // 1. GET to grab viewstate
            Request getReq = new Request.Builder()
                    .url(pageUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", BASE_URL + "/Dashboard.aspx")
                    .build();
            String pageHtml;
            try (Response resp = client.newCall(getReq).execute()) {
                if (!resp.isSuccessful()) return null;
                pageHtml = resp.body() != null ? resp.body().string() : "";
            }

            Document doc = Jsoup.parse(pageHtml);
            Element form = doc.select("form").first();
            if (form == null) return null;

            // 2. Build POST body
            FormBody.Builder formBuilder = new FormBody.Builder();
            for (Element input : form.select("input")) {
                String name = input.attr("name");
                String value = input.attr("value");
                String type = input.attr("type").toLowerCase();
                if (!name.isEmpty() && (type.equals("hidden") || type.equals("text") || type.equals("password") || type.equals("radio") || type.equals("checkbox"))) {
                    if (!name.equals("__EVENTTARGET") && !name.equals("__EVENTARGUMENT")) {
                        formBuilder.add(name, value);
                    }
                }
            }
            formBuilder.add("__EVENTTARGET", eventTarget);
            formBuilder.add("__EVENTARGUMENT", "");

            // 3. POST
            String formAction = form.attr("action");
            String postUrl = formAction.isBlank() ? pageUrl : (formAction.startsWith("http") ? formAction : BASE_URL + (formAction.startsWith("/") ? "" : "/") + formAction);

            Request postReq = new Request.Builder()
                    .url(postUrl)
                    .post(formBuilder.build())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Referer", pageUrl)
                    .header("Origin", BASE_URL)
                    .header("User-Agent", USER_AGENT)
                    .build();

            try (Response resp = client.newCall(postReq).execute()) {
                if (!resp.isSuccessful()) return null;
                return resp.body() != null ? resp.body().string() : "";
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Perform an ASP.NET postback on a page by selecting a dropdown value.
     * This mimics what happens when the user selects a course from the dropdown.
     */
    public String postbackWithDropdown(String relativeUrl, String dropdownName, String selectedValue) {
        try {
            String pageUrl = BASE_URL + "/" + relativeUrl;

            // 1. GET the page first to get form tokens
            Request getReq = new Request.Builder()
                    .url(pageUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", BASE_URL + "/Dashboard.aspx")
                    .build();
            String pageHtml;
            try (Response resp = client.newCall(getReq).execute()) {
                if (!resp.isSuccessful()) return null;
                pageHtml = resp.body() != null ? resp.body().string() : "";
            }

            Document doc = Jsoup.parse(pageHtml);
            Element form = doc.select("form").first();
            if (form == null) return null;

            // 2. Build POST body
            FormBody.Builder formBuilder = new FormBody.Builder();

            for (Element input : form.select("input")) {
                String name = input.attr("name");
                String value = input.attr("value");
                String type = input.attr("type").toLowerCase();
                if (!name.isEmpty() && (type.equals("hidden") || type.equals("text") || type.equals("password") || type.equals("radio") || type.equals("checkbox"))) {
                    formBuilder.add(name, value);
                }
            }

            // Add all select fields
            Element targetDropdown = null;
            for (Element el : form.select("select")) {
                String name = el.attr("name");
                if (name.equals(dropdownName)) {
                    formBuilder.add(name, selectedValue);
                    targetDropdown = el;
                } else {
                    Element selected = el.select("option[selected]").first();
                    String val = selected != null ? selected.attr("value") :
                            (el.select("option").first() != null ? el.select("option").first().attr("value") : "");
                    formBuilder.add(name, val);
                }
            }

            // Check if it's AutoPostBack
            boolean isAutoPostBack = false;
            if (targetDropdown != null) {
                String onchange = targetDropdown.attr("onchange");
                if (onchange.contains("__doPostBack")) {
                    isAutoPostBack = true;
                }
            }

            if (isAutoPostBack) {
                formBuilder.add("__EVENTTARGET", dropdownName);
                formBuilder.add("__EVENTARGUMENT", "");
            } else {
                // Not AutoPostBack, we must simulate clicking the primary submit button
                Element submitBtn = form.selectFirst("input[type=submit], button[type=submit]");
                if (submitBtn != null) {
                    formBuilder.add(submitBtn.attr("name"), submitBtn.attr("value"));
                }
            }

            // 3. POST
            String formAction = form.attr("action");
            String postUrl = formAction.isBlank() ? pageUrl
                    : formAction.startsWith("http") ? formAction
                    : formAction.startsWith("/") ? BASE_URL + formAction
                    : BASE_URL + "/" + formAction;

            Request postReq = new Request.Builder()
                    .url(postUrl)
                    .post(formBuilder.build())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Referer", pageUrl)
                    .header("Origin", BASE_URL)
                    .header("User-Agent", USER_AGENT)
                    .build();

            try (Response resp = client.newCall(postReq).execute()) {
                if (!resp.isSuccessful()) return null;
                return resp.body() != null ? resp.body().string() : "";
            }
        } catch (IOException e) {
            return null;
        }
    }

    // ---------- Fetch page HTML (for tabbed portal views) ----------
    public String fetchPageHtml(String relativeUrl) {
        if (offlineMode) return null;
        boolean[] isCreator = {false};
        java.util.concurrent.CompletableFuture<String> future = inFlightRequests.computeIfAbsent(relativeUrl, k -> {
            isCreator[0] = true;
            return new java.util.concurrent.CompletableFuture<>();
        });

        if (isCreator[0]) {
            try {
                String result = fetchPageHtmlInternal(relativeUrl);
                future.complete(result);
                return result;
            } catch (Throwable t) {
                future.completeExceptionally(t);
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                return null;
            } finally {
                inFlightRequests.remove(relativeUrl);
            }
        } else {
            try {
                return future.join();
            } catch (java.util.concurrent.CompletionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                return null;
            }
        }
    }

    private String fetchPageHtmlInternal(String relativeUrl) {
        try {
            String url = BASE_URL + "/" + relativeUrl;
            Request request = new Request.Builder()
                    .url(url)
                    .header("Referer", BASE_URL + "/CoursePortal.aspx")
                    .header("User-Agent", USER_AGENT)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;
                String body = response.body() != null ? response.body().string() : "";
                if (isLoginPage(response.request().url().toString(), body)) return null;
                String name = parseStudentNameFromHtml(body);
                if (name != null) currentStudentName = name;
                return body;
            }
        } catch (IOException e) {
            return null;
        }
    }

    // ---------- Settings Sub-Tabs Implementations ----------
    
    private String extractPortalMessage(String html) {
        if (html == null) return "Network Error";
        Document doc = Jsoup.parse(html);
        Element msg = doc.select("span[id*=lblMsg], span[id*=lblMessage], span[id*=lblError], div.alert").first();
        if (msg != null && !msg.text().isBlank()) return msg.text().trim();
        return "Action completed.";
    }

    public ProfileInfo parseProfileInfo(String html) {
        if (html == null || html.isBlank()) return null;
        Document doc = Jsoup.parse(html);
        String network = "", number = "", email = "";
        for (Element input : doc.select("input")) {
            String name = input.attr("name").toLowerCase();
            if (name.contains("serviceno") || name.contains("network")) {
                network = input.attr("value");
            } else if (name.contains("cellno") || name.contains("mobile") || (name.contains("cell") && !name.contains("serviceno"))) {
                number = input.attr("value");
            } else if (name.contains("email")) {
                email = input.attr("value");
            }
        }
        return new ProfileInfo(network, number, email);
    }

    public List<LoginHistoryEntry> parseLoginHistory(String html) {
        List<LoginHistoryEntry> history = new ArrayList<>();
        if (html == null || html.isBlank()) return history;
        Document doc = Jsoup.parse(html);
        Element targetTable = null;
        for (Element t : doc.select("table")) {
            if (t.text().toLowerCase().contains("login time") || t.attr("id").toLowerCase().contains("gvloginhistory")) {
                targetTable = t;
                break;
            }
        }
        if (targetTable != null) {
            boolean first = true;
            for (Element row : targetTable.select("tr")) {
                if (first && row.select("th").size() > 0) { first = false; continue; } // skip header
                Elements tds = row.select("td");
                if (tds.size() >= 4) {
                    history.add(new LoginHistoryEntry(
                            tds.get(0).text().trim(),
                            tds.get(1).text().trim(),
                            tds.get(2).text().trim(),
                            tds.get(3).text().trim()
                    ));
                    if (history.size() >= 100) break;
                }
            }
        }
        return history;
    }

    private String findTableTitle(Element table, String fallback) {
        // Look at preceding siblings
        Element prev = table.previousElementSibling();
        int maxDistance = 4; // look up to 4 elements before
        for (int i = 0; i < maxDistance && prev != null; i++) {
            String tagName = prev.tagName().toLowerCase();
            if (tagName.matches("h[1-6]") || tagName.equals("span") || tagName.equals("label") || tagName.equals("p") || tagName.equals("div")) {
                String text = prev.text().trim();
                if (text.length() > 3 && text.length() < 100 && !text.toLowerCase().contains("welcome") && !text.toLowerCase().contains("log out")) {
                    return text;
                }
            }
            prev = prev.previousElementSibling();
        }
        
        // Search if parent container has header
        Element parent = table.parent();
        if (parent != null) {
            Element headerSpan = parent.selectFirst("span[id*=lblTitle], span[id*=Title], label[id*=lblTitle]");
            if (headerSpan != null && !headerSpan.text().trim().isBlank()) {
                return headerSpan.text().trim();
            }
            
            Element heading = parent.selectFirst("h1, h2, h3, h4, h5, h6");
            if (heading != null && !heading.text().trim().isBlank()) {
                return heading.text().trim();
            }
        }
        
        // Last resort: check page title
        if (table.ownerDocument() != null) {
            Element docTitle = table.ownerDocument().selectFirst("title");
            if (docTitle != null && !docTitle.text().trim().isBlank()) {
                return docTitle.text().trim();
            }
        }
        
        return fallback;
    }

    public List<ScholarshipTable> parseScholarships(String html) {
        List<ScholarshipTable> tables = new ArrayList<>();
        if (html == null || html.isBlank()) return tables;
        Document doc = Jsoup.parse(html);
        
        // Find all tables
        Elements allTables = doc.select("table");
        int tableIndex = 1;
        
        for (Element t : allTables) {
            // Skip outer/wrapper tables that contain nested tables to avoid duplicate parses
            if (t.select("table").size() > 1) {
                continue;
            }

            String text = t.text().toLowerCase();
            // Match tables containing "scholarship", "gridview", or having specific headers like "amount" & "status"
            boolean matches = text.contains("scholarship") 
                || t.attr("id").toLowerCase().contains("grid")
                || t.attr("id").toLowerCase().contains("gv")
                || (text.contains("amount") && text.contains("status"))
                || (text.contains("title") && text.contains("status"))
                || (text.contains("name") && text.contains("status"))
                || t.attr("class").toLowerCase().contains("data-grid")
                || t.attr("class").toLowerCase().contains("gridview");
                
            if (matches) {
                // Parse rows in this table
                List<String> headers = new ArrayList<>();
                List<List<String>> data = new ArrayList<>();
                
                Elements rows = t.select("tr");
                for (Element row : rows) {
                    // Check if it has th elements
                    Elements ths = row.select("th");
                    if (!ths.isEmpty() && headers.isEmpty()) {
                        // Skip single-cell title/banner <th> rows (colspan headers)
                        if (ths.size() == 1 && rows.size() > 2) {
                            continue; // likely a colspan title like "Scholarship Awarded Information"
                        }
                        for (Element th : ths) {
                            headers.add(th.text().trim());
                        }
                    } else {
                        Elements tds = row.select("td");
                        if (!tds.isEmpty()) {
                            // Skip single-cell rows (colspan title/banner rows)
                            if (tds.size() == 1 && rows.size() > 2) {
                                continue;
                            }
                            // If we haven't set headers yet, use this row as headers
                            if (headers.isEmpty()) {
                                for (Element td : tds) {
                                    headers.add(td.text().trim());
                                }
                            } else {
                                List<String> rowData = new ArrayList<>();
                                for (Element td : tds) {
                                    rowData.add(td.text().trim());
                                }
                                // Skip rows that are empty or have fewer cells
                                if (rowData.size() > 1 && rowData.stream().anyMatch(s -> !s.isBlank())) {
                                    data.add(rowData);
                                }
                            }
                        }
                    }
                }
                
                // Safety check: if headers count doesn't match data columns, re-derive
                if (!data.isEmpty() && !headers.isEmpty() && data.get(0).size() != headers.size()) {
                    // Headers were likely a single-cell title row; shift data[0] to headers
                    headers = data.remove(0);
                }
                
                // If we extracted a valid table, add it
                if (!headers.isEmpty() && !data.isEmpty()) {
                    String headersStr = headers.toString().toLowerCase();
                    if (headersStr.contains("father name") || headersStr.contains("registration no") || headersStr.contains("roll no") || headersStr.contains("cnic")) {
                        continue; // Skip personal info table
                    }
                    String title = findTableTitle(t, "Scholarship Status Table " + tableIndex++);
                    tables.add(new ScholarshipTable(title, headers, data));
                }
            }
        }
        
        // Fallback: If no tables matched the specific filters, check if any table exists
        if (tables.isEmpty() && !allTables.isEmpty()) {
            for (Element firstTable : allTables) {
                // Skip outer/wrapper tables that contain nested tables
                if (firstTable.select("table").size() > 1) {
                    continue;
                }
                List<String> headers = new ArrayList<>();
                List<List<String>> data = new ArrayList<>();
                Elements rows = firstTable.select("tr");
                for (Element row : rows) {
                    Elements cells = row.select("th, td");
                    // Skip single-cell colspan title rows
                    if (cells.size() == 1 && rows.size() > 2) {
                        continue;
                    }
                    List<String> rowData = new ArrayList<>();
                    for (Element cell : cells) {
                        rowData.add(cell.text().trim());
                    }
                    if (headers.isEmpty()) {
                        headers.addAll(rowData);
                    } else if (rowData.size() > 1 && rowData.stream().anyMatch(s -> !s.isBlank())) {
                        data.add(rowData);
                    }
                }
                // Safety check: if headers count doesn't match data columns, re-derive
                if (!data.isEmpty() && !headers.isEmpty() && data.get(0).size() != headers.size()) {
                    headers = data.remove(0);
                }
                if (!headers.isEmpty() && !data.isEmpty()) {
                    String headersStr = headers.toString().toLowerCase();
                    if (headersStr.contains("father name") || headersStr.contains("registration no") || headersStr.contains("roll no") || headersStr.contains("cnic")) {
                        continue; // Skip personal info table
                    }
                    String title = findTableTitle(firstTable, "Scholarship Information");
                    tables.add(new ScholarshipTable(title, headers, data));
                    break; // Only pick the first non-personal-info leaf table
                }
            }
        }
        
        return tables;
    }

    public String downloadAndExtractPdf(String relativeUrl) {
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/" + relativeUrl)
                    .header("User-Agent", USER_AGENT)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    byte[] pdfBytes = response.body().bytes();
                    com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdfBytes);
                    com.lowagie.text.pdf.parser.PdfTextExtractor extractor = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                        sb.append(extractor.getTextFromPage(i)).append("\n\n");
                    }
                    reader.close();
                    return sb.toString();
                } else {
                    return "Failed to download PDF. Server returned " + response.code();
                }
            }
        } catch (Exception e) {
            return "Error extracting PDF: " + e.getMessage();
        }
    }

    public String updateProfile(String newNetwork, String newNumber, String newEmail) {
        try {
            String pageUrl = BASE_URL + "/AddCellEmailInfo.aspx";
            String pageHtml = fetchPageHtml("AddCellEmailInfo.aspx");
            if (pageHtml == null) return "Could not fetch profile page.";
            
            Document doc = Jsoup.parse(pageHtml);
            Element form = doc.select("form").first();
            if (form == null) return "Form not found on profile page.";

            FormBody.Builder formBuilder = new FormBody.Builder();
            String btnName = "";
            int cellCount = 0;
            for (Element input : form.select("input, select")) {
                String name = input.attr("name");
                String type = input.attr("type").toLowerCase();
                String val = input.attr("value");
                if ("select".equals(input.tagName())) {
                     Element selected = input.select("option[selected]").first();
                     val = selected != null ? selected.attr("value") : "";
                     formBuilder.add(name, val);
                     continue;
                }

                if (type.equals("hidden")) {
                    formBuilder.add(name, val);
                } else if (name.toLowerCase().contains("btnsubmit") || name.toLowerCase().contains("btnsave") || type.equals("submit")) {
                    if (btnName.isEmpty()) {
                        btnName = name;
                        formBuilder.add(name, "Save");
                    }
                } else if (name.toLowerCase().contains("serviceno") || name.toLowerCase().contains("network")) {
                    formBuilder.add(name, newNetwork);
                } else if (name.toLowerCase().contains("cellno") || name.toLowerCase().contains("mobile") || (name.toLowerCase().contains("cell") && !name.toLowerCase().contains("serviceno"))) {
                    formBuilder.add(name, newNumber);
                } else if (name.toLowerCase().contains("email")) {
                    formBuilder.add(name, newEmail);
                } else if (type.equals("text") || type.equals("password")) {
                    formBuilder.add(name, val);
                }
            }

            Request postReq = new Request.Builder()
                    .url(pageUrl)
                    .post(formBuilder.build())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Referer", pageUrl)
                    .header("User-Agent", USER_AGENT)
                    .build();

            try (Response resp = client.newCall(postReq).execute()) {
                String body = resp.body() != null ? resp.body().string() : "";
                return extractPortalMessage(body);
            }
        } catch (Exception e) {
            return "Network Error: " + e.getMessage();
        }
    }

    public String changePassword(String currentPass, String newPass, String confirmPass) {
        try {
            String pageUrl = BASE_URL + "/changepassword.aspx";
            String pageHtml = fetchPageHtml("changepassword.aspx");
            if (pageHtml == null) return "Could not fetch password change page.";
            
            Document doc = Jsoup.parse(pageHtml);
            Element form = doc.select("form").first();
            if (form == null) return "Form not found on password change page.";

            FormBody.Builder formBuilder = new FormBody.Builder();
            String btnName = "";
            for (Element input : form.select("input, select")) {
                String name = input.attr("name");
                String type = input.attr("type").toLowerCase();
                String val = input.attr("value");
                if ("select".equals(input.tagName())) {
                     Element selected = input.select("option[selected]").first();
                     val = selected != null ? selected.attr("value") : "";
                     formBuilder.add(name, val);
                     continue;
                }

                if (type.equals("hidden")) {
                    formBuilder.add(name, val);
                } else if (name.toLowerCase().contains("btnchange") || name.toLowerCase().contains("btnsubmit") || type.equals("submit") || name.toLowerCase().contains("btn")) {
                    if (btnName.isEmpty() && !name.toLowerCase().contains("cancel")) {
                        btnName = name;
                        formBuilder.add(name, "Change Password");
                    }
                } else if (name.toLowerCase().contains("old") && (type.equals("password") || type.equals("text"))) {
                    formBuilder.add(name, currentPass);
                } else if (name.toLowerCase().contains("new") && (type.equals("password") || type.equals("text"))) {
                    formBuilder.add(name, newPass);
                } else if (name.toLowerCase().contains("confirm") && (type.equals("password") || type.equals("text"))) {
                    formBuilder.add(name, confirmPass);
                } else if (type.equals("text") || type.equals("password")) {
                    formBuilder.add(name, val);
                }
            }

            Request postReq = new Request.Builder()
                    .url(pageUrl)
                    .post(formBuilder.build())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Referer", pageUrl)
                    .header("User-Agent", USER_AGENT)
                    .build();

            try (Response resp = client.newCall(postReq).execute()) {
                String body = resp.body() != null ? resp.body().string() : "";
                return extractPortalMessage(body);
            }
        } catch (Exception e) {
            return "Network Error: " + e.getMessage();
        }
    }

    public String generateAppPassword(String currentPass) {
        try {
            String pageUrl = BASE_URL + "/GenerateAppPassword.aspx";
            String pageHtml = fetchPageHtml("GenerateAppPassword.aspx");
            if (pageHtml == null) return "Could not fetch app password page.";
            
            Document doc = Jsoup.parse(pageHtml);
            Element form = doc.select("form").first();
            if (form == null) return "Form not found on app password page.";

            FormBody.Builder formBuilder = new FormBody.Builder();
            String btnName = "";
            for (Element input : form.select("input, select")) {
                String name = input.attr("name");
                String type = input.attr("type").toLowerCase();
                String val = input.attr("value");
                if ("select".equals(input.tagName())) {
                     Element selected = input.select("option[selected]").first();
                     val = selected != null ? selected.attr("value") : "";
                     formBuilder.add(name, val);
                     continue;
                }

                if (type.equals("hidden")) {
                    formBuilder.add(name, val);
                } else if (name.toLowerCase().contains("btnsubmit") || type.equals("submit") || name.toLowerCase().contains("btn")) {
                    if (btnName.isEmpty()) {
                        btnName = name;
                        formBuilder.add(name, "Submit");
                    }
                } else if (type.equals("password") || name.toLowerCase().contains("pass")) {
                    formBuilder.add(name, currentPass);
                } else if (type.equals("text")) {
                    formBuilder.add(name, val);
                }
            }

            Request postReq = new Request.Builder()
                    .url(pageUrl)
                    .post(formBuilder.build())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Referer", pageUrl)
                    .header("User-Agent", USER_AGENT)
                    .build();

            try (Response resp = client.newCall(postReq).execute()) {
                String body = resp.body() != null ? resp.body().string() : "";
                return extractPortalMessage(body);
            }
        } catch (Exception e) {
            return "Network Error: " + e.getMessage();
        }
    }

    public String extractPasswordRules(String html) {
        if (html == null) return "Password must be at least 8 characters long, include a number, an uppercase letter, and a special character.";
        Document doc = Jsoup.parse(html);
        Element container = doc.select("div.alert, span[id*=lblRules], td, li").stream()
                .filter(e -> e.text().toLowerCase().contains("must contain") || e.text().toLowerCase().contains("policy"))
                .findFirst().orElse(null);
        if (container != null && !container.text().isBlank()) {
            return container.text().replaceAll("(?i)password policy:?", "").trim();
        }
        return "Password must be at least 8 characters long, include a number, an uppercase letter, and a special character.";
    }

    // ---------- Helpers ported from Kotlin ----------
    private String normalizeToken(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public void clearSessionState() {
        cookieStore.clear();
        inFlightRequests.clear();
        currentStudentName = null;
        currentStudentPhotoUrl = null;
        if (databaseManager != null) {
            try (Connection conn = databaseManager.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM session_cookies");
            } catch (Exception e) {
                System.err.println("Failed to delete session cookies on clearSessionState: " + e.getMessage());
            }
        }
    }

    public void addCookie(String domain, okhttp3.Cookie cookie) {
        String cleanDomain = domain.startsWith(".") ? domain.substring(1) : domain;
        Map<String, Cookie> hostCookies = cookieStore.computeIfAbsent(cleanDomain, k -> new ConcurrentHashMap<>());
        hostCookies.put(cookie.name(), cookie);
    }

    public void importCookiesFromDefaultManager() {
        try {
            java.net.CookieHandler handler = java.net.CookieHandler.getDefault();
            if (handler instanceof java.net.CookieManager cookieManager) {
                java.net.CookieStore netCookieStore = cookieManager.getCookieStore();
                List<java.net.HttpCookie> netCookies = netCookieStore.getCookies();
                for (java.net.HttpCookie netCookie : netCookies) {
                    String domain = netCookie.getDomain();
                    if (domain == null || domain.isEmpty()) {
                        domain = BASE_HOST;
                    }
                    if (domain.startsWith(".")) {
                        domain = domain.substring(1);
                    }
                    // Filter for our domain
                    if (!domain.toLowerCase().contains("sis.cuiatd.edu.pk")) {
                        continue;
                    }

                    String path = netCookie.getPath();
                    if (path == null || path.isEmpty()) {
                        path = "/";
                    }

                    long expiresAt = -1;
                    long maxAge = netCookie.getMaxAge();
                    if (maxAge > 0) {
                        expiresAt = System.currentTimeMillis() + (maxAge * 1000);
                    } else {
                        // persistent for session or long-lived
                        expiresAt = System.currentTimeMillis() + (24L * 3600 * 1000); // 1 day
                    }

                    okhttp3.Cookie.Builder builder = new okhttp3.Cookie.Builder()
                            .name(netCookie.getName())
                            .value(netCookie.getValue())
                            .domain(domain)
                            .path(path)
                            .expiresAt(expiresAt);

                    if (netCookie.getSecure()) {
                        builder.secure();
                    }
                    if (netCookie.isHttpOnly()) {
                        builder.httpOnly();
                    }

                    okhttp3.Cookie okCookie = builder.build();
                    addCookie(domain, okCookie);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to import cookies: " + e.getMessage());
        }
    }

    public synchronized void saveSessionCookies() {
        if (databaseManager == null) return;
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM session_cookies");
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO session_cookies (name, value, domain, path, expires_at, secure, http_only) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                for (Map<String, Cookie> domainCookies : cookieStore.values()) {
                    for (Cookie cookie : domainCookies.values()) {
                        if (cookie.expiresAt() > System.currentTimeMillis() && cookie.domain().toLowerCase().contains("sis.cuiatd.edu.pk")) {
                            pstmt.setString(1, cookie.name());
                            String encryptedVal = EncryptionUtil.encrypt(cookie.value());
                            pstmt.setString(2, encryptedVal);
                            pstmt.setString(3, cookie.domain());
                            pstmt.setString(4, cookie.path());
                            pstmt.setLong(5, cookie.expiresAt());
                            pstmt.setInt(6, cookie.secure() ? 1 : 0);
                            pstmt.setInt(7, cookie.httpOnly() ? 1 : 0);
                            pstmt.addBatch();
                        }
                    }
                }
                pstmt.executeBatch();
            }
        } catch (Exception e) {
            System.err.println("Failed to save session cookies: " + e.getMessage());
        }
    }

    public synchronized void loadSessionCookies() {
        if (databaseManager == null) return;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT name, value, domain, path, expires_at, secure, http_only FROM session_cookies")) {
            ResultSet rs = pstmt.executeQuery();
            long now = System.currentTimeMillis();
            while (rs.next()) {
                String name = rs.getString("name");
                String encryptedVal = rs.getString("value");
                String domain = rs.getString("domain");
                String path = rs.getString("path");
                long expiresAt = rs.getLong("expires_at");
                boolean secure = rs.getInt("secure") == 1;
                boolean httpOnly = rs.getInt("http_only") == 1;

                if (expiresAt > now) {
                    try {
                        String decryptedVal = EncryptionUtil.decrypt(encryptedVal);
                        Cookie.Builder builder = new Cookie.Builder()
                                .name(name)
                                .value(decryptedVal)
                                .domain(domain)
                                .path(path)
                                .expiresAt(expiresAt);
                        if (secure) builder.secure();
                        if (httpOnly) builder.httpOnly();
                        
                        Cookie cookie = builder.build();
                        addCookie(domain, cookie);
                    } catch (Exception ex) {
                        System.err.println("Failed to decrypt cookie " + name + ": " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load session cookies: " + e.getMessage());
        }
    }

    public boolean verifySession(String username) {
        try {
            String normalizedUser = username.trim().toUpperCase(Locale.ROOT);
            Request verifyRequest = new Request.Builder()
                    .url(BASE_URL + "/CoursePortal.aspx")
                    .header("Referer", LOGIN_URL)
                    .header("User-Agent", USER_AGENT)
                    .build();

            try (Response response = client.newCall(verifyRequest).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                String resolvedUrl = response.request().url().toString();
                if (!response.isSuccessful()) return false;
                if (isSecurityVerificationResponse(response, resolvedUrl, body)) return false;
                if (isSecurityVerificationPage(resolvedUrl, body)) return false;
                if (isLoginPage(resolvedUrl, body)) return false;

                StudentProfile profile = parseStudentProfileFromHtml(body);
                boolean profileMatches = doesProfileMatchRequestedUsername(normalizedUser, profile, body);
                
                if (profileMatches) {
                    currentStudentName = profile.name() != null ? profile.name() : parseStudentNameFromHtml(body);
                    currentStudentPhotoUrl = parseStudentPhotoUrlFromHtml(body, resolvedUrl);
                    return hasSessionCookiesForHost(BASE_HOST);
                }
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    List<Cookie> getCookiesForTest(String host) {
        Map<String, Cookie> cookies = cookieStore.get(host);
        return cookies == null ? List.of() : new ArrayList<>(cookies.values());
    }

    public boolean hasSessionCookiesForHost(String host) {
        Map<String, Cookie> cookies = cookieStore.get(host);
        if (cookies == null) return false;
        long now = System.currentTimeMillis();
        return cookies.values().stream().anyMatch(c -> {
            String n = c.name().toLowerCase();
            return c.expiresAt() > now && (n.contains("session") || n.contains("auth") || n.contains("asp.net"));
        });
    }

    private boolean isLoginPage(String url, String html) {
        if (url.toLowerCase().contains("login.aspx")) return true;
        Document doc = Jsoup.parse(html);
        return !doc.select("input[name*=txtUsername], input[id*=txtUsername], input[name*=btnLogin], input[id*=btnLogin]").isEmpty();
    }

    private boolean isSecurityVerificationPage(String url, String html) {
        if (url == null) return false;
        String lUrl = url.toLowerCase();
        if (lUrl.contains("/cdn-cgi/") || lUrl.contains("challenge-platform") || lUrl.startsWith("chrome-error://"))
            return true;
        if (html == null || html.isBlank()) return false;
        String lHtml = html.toLowerCase();
        boolean hasChallengeArtifacts = List.of("cf_chl", "cf-browser-verification", "challenge-platform", "cf-turnstile", "challenges.cloudflare.com")
                .stream().anyMatch(lHtml::contains);
        boolean hasChallengeLanguage = List.of("security verification", "verify you are human", "just a moment", "checking your browser")
                .stream().anyMatch(lHtml::contains);
        // Check if it's actually a real login form
        Document doc = Jsoup.parse(html);
        boolean hasLoginForm = !doc.select("input[name*=txtUsername], input[name*=RollNo], input[name*=roll]").isEmpty()
                && !doc.select("input[type=password]").isEmpty();
        if (hasLoginForm) return false;
        return hasChallengeArtifacts && hasChallengeLanguage;
    }

    private boolean isSecurityVerificationResponse(Response response, String resolvedUrl, String body) {
        if (isSecurityVerificationPage(resolvedUrl, body)) return true;
        int code = response.code();
        if (code != 403 && code != 429 && code != 503 && code != 525 && code != 526) return false;
        String server = response.header("Server");
        return server != null && server.toLowerCase().contains("cloudflare");
    }

    private String parseStudentNameFromHtml(String html) {
        Document doc = Jsoup.parse(html);
        Element idBased = doc.select("[id*=lblName], [id*=StudentName], [id*=FullName], [id*=txtName]").first();
        if (idBased != null) {
            String text = idBased.text().trim();
            if (!text.isEmpty() && !text.equalsIgnoreCase("Name") && !text.equalsIgnoreCase("Name :") && !text.equalsIgnoreCase("NA"))
                return text;
        }
        Element labelCell = doc.select("td, th, span, label").stream()
                .filter(e -> e.text().trim().replaceAll("\\s+", " ").matches("(?i)^name\\s*:?$"))
                .findFirst().orElse(null);
        if (labelCell != null) {
            Element sibling = labelCell.nextElementSibling();
            if (sibling != null) {
                String text = sibling.text().trim();
                if (!text.isEmpty() && !text.equalsIgnoreCase("NA")) return text;
            }
        }
        return null;
    }

    private StudentProfile parseStudentProfileFromHtml(String html) {
        Document doc = Jsoup.parse(html);
        Map<String, String> tablePairs = new LinkedHashMap<>();
        for (Element row : doc.select("tr")) {
            Elements cells = row.select("th, td");
            if (cells.size() < 2) continue;
            for (int i = 0; i + 1 < cells.size(); i += 2) {
                String key = cells.get(i).text().trim().replaceAll(":$", "").toLowerCase();
                String value = cells.get(i + 1).text().trim();
                if (!key.isEmpty() && !value.isEmpty() && !value.equalsIgnoreCase("NA"))
                    tablePairs.putIfAbsent(key, value);
            }
        }
        String rollNo = tablePairs.entrySet().stream()
                .filter(e -> e.getKey().contains("roll no") || e.getKey().contains("rollno") || e.getKey().contains("registration no"))
                .map(Map.Entry::getValue).findFirst().orElse(null);
        String program = tablePairs.entrySet().stream()
                .filter(e -> e.getKey().contains("program"))
                .map(Map.Entry::getValue).findFirst().orElse(null);
        String name = parseStudentNameFromHtml(html);
        return new StudentProfile(name, rollNo, program);
    }

    private String parseStudentPhotoUrlFromHtml(String html, String pageUrl) {
        Document doc = Jsoup.parse(html, pageUrl);
        Element best = null;
        int bestScore = -100;
        for (Element el : doc.select("img[src], input[type=image][src]")) {
            String src = el.attr("abs:src").trim();
            if (src.isBlank() || src.startsWith("data:")) continue;
            String fingerprint = (el.attr("id") + " " + el.attr("name") + " " + el.className() + " " + el.attr("alt") + " " + el.attr("title") + " " + src).toLowerCase();
            int score = 0;
            if (fingerprint.contains("student")) score += 4;
            if (fingerprint.contains("profile")) score += 3;
            if (fingerprint.contains("photo")) score += 3;
            if (fingerprint.contains("pic")) score += 2;
            if (fingerprint.contains("logo")) score -= 5;
            if (fingerprint.contains("banner")) score -= 4;
            if (fingerprint.contains("icon")) score -= 2;
            if (score > bestScore) { bestScore = score; best = el; }
        }
        return best != null ? best.attr("abs:src") : null;
    }

    private boolean doesProfileMatchRequestedUsername(String requestedUsername, StudentProfile profile, String html) {
        String[] p = requestedUsername.trim().split("-");
        if (p.length != 3) return false;
        String expectedComposite = normalizeToken(requestedUsername);
        String normalizedHtml = normalizeToken(html);
        if (!expectedComposite.isBlank() && normalizedHtml.contains(expectedComposite)) return true;
        String actualProgram = normalizeToken(profile.program());
        String actualRoll = normalizeToken(profile.rollNo());
        if (actualProgram.isBlank() || actualRoll.isBlank()) return false;
        return actualProgram.contains(normalizeToken(p[1]))
                && (actualRoll.contains(normalizeToken(p[2])));
    }

    /**
     * Download a file from the portal using the active authenticated session.
     */
    public Response downloadFile(String relativeUrl) throws IOException {
        if (offlineMode) {
            throw new IOException("Offline Mode Active");
        }
        String url = relativeUrl.startsWith("http") ? relativeUrl : BASE_URL + "/" + relativeUrl;
        Request request = new Request.Builder()
                .url(url)
                .header("Referer", BASE_URL + "/CoursePortalContentsSummary.aspx")
                .header("User-Agent", USER_AGENT)
                .build();
        return client.newCall(request).execute();
    }

    // ---------- Logging helpers ----------
    private static void logD(String tag, String message) {
        System.out.println("[" + tag + "] " + message);
    }
    private static void logE(String tag, String message) {
        System.err.println("[" + tag + "] " + message);
    }
    private static void logE(String tag, String message, Throwable t) {
        System.err.println("[" + tag + "] " + message);
        t.printStackTrace();
    }

    // ---------- Ported Helper & Utility Methods ----------
    private static final Pattern POSTBACK_PATTERN1 = Pattern.compile(
            "__doPostBack\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern POSTBACK_PATTERN2 = Pattern.compile(
            "WebForm_DoPostBackWithOptions\\s*\\(\\s*new\\s+WebForm_PostBackOptions\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*,\\s*['\"]([^'\"]*)['\"]\\s*", Pattern.CASE_INSENSITIVE);
    private static final List<Pattern> JS_URL_PATTERNS = List.of(
            Pattern.compile("window\\.open\\(\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("window\\.location(?:\\.href)?\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("location\\.replace\\(\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE)
    );

    public boolean isPostBackDownloadLink(String link) {
        return link != null && link.toLowerCase().startsWith("postback:");
    }

    public PostBackLink extractPostBackLinkFromLink(String link) {
        if (!isPostBackDownloadLink(link)) return null;
        String clean = link.substring("postback:".length());
        int index = clean.indexOf("|");
        String payload = (index != -1) ? clean.substring(0, index) : clean;
        String sourcePageUrl = null;
        if (index != -1) {
            String temp = clean.substring(index + 1);
            if (!temp.isBlank()) {
                sourcePageUrl = temp;
            }
        }
        String[] parts = payload.split(",", -1);
        if (parts.length < 2) return null;
        String target = decodePostBackPart(parts[0]);
        String argument = decodePostBackPart(parts[1]);
        return new PostBackLink(new PostBackInfo(target, argument), sourcePageUrl);
    }

    public String toPostBackDownloadLink(PostBackInfo info, String sourcePageUrl) {
        String payload = encodePostBackPart(info.target()) + "," + encodePostBackPart(info.argument());
        if (sourcePageUrl == null || sourcePageUrl.isBlank()) {
            return "postback:" + payload;
        } else {
            return "postback:" + payload + "|" + sourcePageUrl;
        }
    }

    private String encodePostBackPart(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }

    private String decodePostBackPart(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return value;
        }
    }

    public PostBackInfo extractPostBackInfo(String value) {
        if (value == null || value.isBlank()) return null;
        Matcher m1 = POSTBACK_PATTERN1.matcher(value);
        if (m1.find()) {
            return new PostBackInfo(m1.group(1), m1.group(2));
        }
        Matcher m2 = POSTBACK_PATTERN2.matcher(value);
        if (m2.find()) {
            return new PostBackInfo(m2.group(1), m2.group(2));
        }
        return null;
    }

    public String extractUrlFromJavascript(String value) {
        if (value == null || value.isBlank()) return null;
        for (Pattern p : JS_URL_PATTERNS) {
            Matcher m = p.matcher(value);
            if (m.find()) {
                String match = m.group(1);
                if (match != null && !match.isBlank()) {
                    return match;
                }
            }
        }
        return null;
    }

    public String extractAssignmentDownloadLink(Element downloadCell) {
        if (downloadCell == null) return "";
        Elements anchors = downloadCell.select("a");
        for (Element a : anchors) {
            String href = a.attr("href");
            String onClick = a.attr("onclick");
            PostBackInfo postBackInfo = extractPostBackInfo(href);
            if (postBackInfo == null) postBackInfo = extractPostBackInfo(onClick);
            if (postBackInfo != null) {
                return toPostBackDownloadLink(postBackInfo, null);
            }
            String jsUrl = extractUrlFromJavascript(href);
            if (jsUrl == null) jsUrl = extractUrlFromJavascript(onClick);
            if (jsUrl != null && !jsUrl.isBlank()) {
                return normalizeUrl(jsUrl);
            }
            String normalized = normalizeUrl(href);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        PostBackInfo cellPostBackInfo = extractPostBackInfo(downloadCell.attr("onclick"));
        if (cellPostBackInfo != null) {
            return toPostBackDownloadLink(cellPostBackInfo, null);
        }
        return normalizeUrl(extractUrlFromJavascript(downloadCell.attr("onclick")));
    }

    private PostBackInfo extractPostBackFromSubmitLikeControl(Element element) {
        if (element == null) return null;
        String tag = element.tagName().toLowerCase();
        String type = element.attr("type").toLowerCase();
        boolean isSubmitLike = tag.equals("button") || 
            (tag.equals("input") && (type.equals("submit") || type.equals("button") || type.equals("image") || type.isEmpty()));
        if (!isSubmitLike) return null;

        String controlName = element.attr("name").trim();
        if (controlName.isEmpty()) {
            controlName = element.attr("id").trim().replace("_", "$");
        }
        if (controlName.isEmpty()) return null;

        String fingerprint = (controlName + " " + element.attr("id") + " " + element.attr("value") + " " + element.text() + " " + element.className()).toLowerCase();

        boolean looksLikeSubmitAction = fingerprint.contains("submit") ||
            fingerprint.contains("upload") ||
            fingerprint.contains("change") ||
            fingerprint.contains("addfile") ||
            fingerprint.contains("updatefile") ||
            fingerprint.contains("assignment") ||
            fingerprint.contains("attach");

        return looksLikeSubmitAction ? new PostBackInfo(controlName, "") : null;
    }

    public String extractAssignmentSubmitLink(Element actionCell, String pageUrl) {
        if (actionCell == null) return "";
        Elements actionElements = actionCell.select("a, button, input, span");
        for (Element element : actionElements) {
            String href = element.attr("href");
            String onClick = element.attr("onclick");
            PostBackInfo postBackInfo = extractPostBackInfo(href);
            if (postBackInfo == null) postBackInfo = extractPostBackInfo(onClick);
            if (postBackInfo == null) postBackInfo = extractPostBackFromSubmitLikeControl(element);
            if (postBackInfo != null) {
                return toPostBackDownloadLink(postBackInfo, pageUrl);
            }
            String jsUrl = extractUrlFromJavascript(href);
            if (jsUrl == null) jsUrl = extractUrlFromJavascript(onClick);
            if (jsUrl != null && !jsUrl.isBlank()) {
                return normalizeUrl(jsUrl, pageUrl);
            }
            String normalized = normalizeUrl(href, pageUrl);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        PostBackInfo cellPostBackInfo = extractPostBackInfo(actionCell.attr("onclick"));
        if (cellPostBackInfo == null) cellPostBackInfo = extractPostBackFromSubmitLikeControl(actionCell);
        if (cellPostBackInfo != null) {
            return toPostBackDownloadLink(cellPostBackInfo, pageUrl);
        }
        return normalizeUrl(extractUrlFromJavascript(actionCell.attr("onclick")), pageUrl);
    }

    public boolean looksLikeHtmlPayload(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return false;
        int length = Math.min(bytes.length, 200);
        String prefix = new String(bytes, 0, length, StandardCharsets.UTF_8).trim().toLowerCase();
        return prefix.startsWith("<!doctype html") ||
                prefix.startsWith("<html") ||
                prefix.contains("<body") ||
                prefix.contains("<head") ||
                prefix.contains("<script");
    }

    public boolean isWebEndpointExtension(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase().trim().replaceFirst("^\\.", "");
        return ext.equals("aspx") || ext.equals("html") || ext.equals("htm") || ext.equals("php") || ext.equals("jsp") || ext.equals("asp");
    }

    public String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) return "bin";
        String clean = mimeType.split(";")[0].trim().toLowerCase();
        switch (clean) {
            case "application/pdf": return "pdf";
            case "application/zip":
            case "application/x-zip-compressed": return "zip";
            case "application/x-rar-compressed": return "rar";
            case "application/msword": return "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document": return "docx";
            case "application/vnd.ms-excel": return "xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": return "xlsx";
            case "image/jpeg": return "jpg";
            case "image/png": return "png";
            case "image/gif": return "gif";
            case "text/plain": return "txt";
            case "text/html": return "html";
            default: return "bin";
        }
    }

    public String extractNameFromUrl(String finalUrl) {
        try {
            HttpUrl httpUrl = HttpUrl.parse(finalUrl);
            if (httpUrl == null) return null;
            String path = httpUrl.encodedPath();
            String[] segments = path.split("/");
            String lastSegment = null;
            for (int i = segments.length - 1; i >= 0; i--) {
                if (!segments[i].isBlank()) {
                    lastSegment = segments[i];
                    break;
                }
            }
            if (lastSegment == null) return null;
            String decoded = URLDecoder.decode(lastSegment, StandardCharsets.UTF_8);
            int dotIdx = decoded.lastIndexOf('.');
            if (dotIdx != -1 && dotIdx < decoded.length() - 1) {
                String namePart = decoded.substring(0, dotIdx);
                String extPart = decoded.substring(dotIdx + 1);
                if (!namePart.isBlank() && !extPart.isBlank() && !isWebEndpointExtension(extPart)) {
                    return decoded;
                }
            }
        } catch (IllegalArgumentException ex) {
            ErrorReporter.logError("PortalRepository#extractNameFromUrl", ex);
        }
        return null;
    }

    public String extractFileName(String contentDisposition, String finalUrl, String mimeType) {
        if (contentDisposition != null && !contentDisposition.isBlank()) {
            List<Pattern> patterns = List.of(
                    Pattern.compile("filename\\*\\s*=\\s*UTF-8''([^;\\s]+)", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("filename\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("filename\\s*=\\s*([^;\\s]+)", Pattern.CASE_INSENSITIVE)
            );
            for (Pattern p : patterns) {
                Matcher m = p.matcher(contentDisposition);
                if (m.find()) {
                    String raw = m.group(1).trim();
                    String decoded = raw;
                    try {
                        decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);
                    } catch (IllegalArgumentException ex) {
                        ErrorReporter.logError("PortalRepository#extractFileName decode", ex);
                    }
                    String cleanName = sanitizeFileName(decoded);
                    if (!cleanName.isBlank()) return cleanName;
                }
            }
        }

        String urlName = extractNameFromUrl(finalUrl);
        if (urlName != null) {
            String clean = sanitizeFileName(urlName);
            if (!clean.isBlank()) return clean;
        }

        List<String> queryBasedName = List.of("filename", "file", "name", "download", "attachment", "doc", "document");
        try {
            HttpUrl httpUrl = HttpUrl.parse(finalUrl);
            if (httpUrl != null) {
                for (String param : queryBasedName) {
                    String value = httpUrl.queryParameter(param);
                    if (value != null && !value.isBlank()) {
                        String clean = sanitizeFileName(value);
                        if (!clean.isBlank()) {
                            String ext = getExtensionFromMimeType(mimeType);
                            return clean.contains(".") ? clean : clean + "." + ext;
                        }
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            ErrorReporter.logError("PortalRepository#extractFileName query params", ex);
        }

        String ext = getExtensionFromMimeType(mimeType);
        return "assignment_file." + ext;
    }

    public String sanitizeFileName(String name) {
        if (name == null) return "";
        return name
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public boolean looksLikeDownloadTrigger(Element element) {
        if (element == null) return false;
        String fingerprint = (element.text() + " " +
                element.attr("title") + " " +
                element.attr("aria-label") + " " +
                element.attr("id") + " " +
                element.attr("name") + " " +
                element.className() + " " +
                element.attr("href") + " " +
                element.attr("onclick")).toLowerCase();
        return fingerprint.contains("download") ||
                fingerprint.contains("attachment") ||
                fingerprint.contains("instruction") ||
                fingerprint.contains("file");
    }

    public HtmlDownloadCandidate extractCandidateFromClickable(Element element, String pageUrl) {
        if (element == null) return null;
        String href = element.attr("href");
        String onClick = element.attr("onclick");
        PostBackInfo postBackInfo = extractPostBackInfo(href);
        if (postBackInfo == null) postBackInfo = extractPostBackInfo(onClick);
        if (postBackInfo != null) {
            return new HtmlDownloadCandidate(null, postBackInfo);
        }
        String rawUrl = "";
        if (href.isBlank() || href.equals("#")) {
            rawUrl = extractUrlFromJavascript(onClick);
        } else if (href.toLowerCase().startsWith("javascript")) {
            String js = extractUrlFromJavascript(href);
            rawUrl = (js != null) ? js : extractUrlFromJavascript(onClick);
        } else {
            rawUrl = href;
        }
        String normalized = normalizeUrl(rawUrl, pageUrl);
        return !normalized.isBlank() ? new HtmlDownloadCandidate(normalized, null) : null;
    }

    public String extractInstructionFileNameFromRow(Element row) {
        if (row == null) return "instruction_file";
        Elements cells = row.select("td");
        if (!cells.isEmpty()) {
            List<String> cellTexts = new ArrayList<>();
            for (Element cell : cells) {
                String text = cell.text().trim();
                if (!text.isBlank() && !text.equalsIgnoreCase("download") && !text.matches("^\\d+$")) {
                    cellTexts.add(text);
                }
            }
            String extensionLike = null;
            for (String text : cellTexts) {
                if (text.matches(".*\\.[a-zA-Z0-9]{1,8}$")) {
                    extensionLike = text;
                    break;
                }
            }
            String best = extensionLike;
            if (best == null) {
                String maxLenText = null;
                for (String text : cellTexts) {
                    if (maxLenText == null || text.length() > maxLenText.length()) {
                        maxLenText = text;
                    }
                }
                best = maxLenText;
            }
            if (best != null && !best.isBlank()) {
                return sanitizeFileName(best);
            }
        }
        Elements links = row.select("a");
        String linkText = "";
        for (Element link : links) {
            String text = link.text().trim();
            if (!text.isBlank() && !text.toLowerCase().contains("download")) {
                linkText = text;
                break;
            }
        }
        return sanitizeFileName(linkText.isBlank() ? "instruction_file" : linkText);
    }

    public String extractDownloadCandidateFromElement(Element element, String pageUrl) {
        if (element == null) return null;
        String href = element.attr("href");
        String onClick = element.attr("onclick");
        PostBackInfo postBackInfo = extractPostBackInfo(href);
        if (postBackInfo == null) postBackInfo = extractPostBackInfo(onClick);
        if (postBackInfo != null) {
            return toPostBackDownloadLink(postBackInfo, pageUrl);
        }
        String rawUrl = "";
        if (href.isBlank() || href.equals("#")) {
            rawUrl = extractUrlFromJavascript(onClick);
        } else if (href.toLowerCase().startsWith("javascript")) {
            String js = extractUrlFromJavascript(href);
            rawUrl = (js != null) ? js : extractUrlFromJavascript(onClick);
        } else {
            rawUrl = href;
        }
        String normalized = normalizeUrl(rawUrl, pageUrl);
        return !normalized.isBlank() ? normalized : null;
    }

    public List<InstructionFile> parseInstructionFilesFromHtml(String html, String pageUrl) {
        Document doc = Jsoup.parse(html, pageUrl);
        List<InstructionFile> files = new ArrayList<>();
        Elements tableRows = doc.select("table tr");
        for (Element row : tableRows) {
            String rowText = row.text();
            if (rowText.isBlank()) continue;
            String rowLower = rowText.toLowerCase();
            if (!row.select("th").isEmpty()) continue;
            boolean hasDownloadWord = rowLower.contains("download");
            if (!hasDownloadWord) {
                for (Element el : row.select("a,button,input")) {
                    if (el.text().toLowerCase().contains("download") || el.attr("value").toLowerCase().contains("download")) {
                        hasDownloadWord = true;
                        break;
                    }
                }
            }
            if (!hasDownloadWord) continue;

            String fileName = extractInstructionFileNameFromRow(row);
            Elements clickable = row.select("a[href], a[onclick], button[onclick], input[onclick], span[onclick], input[type=submit], button");
            String link = null;
            for (Element el : clickable) {
                String temp = extractDownloadCandidateFromElement(el, pageUrl);
                if (temp != null && !temp.isBlank()) {
                    link = temp;
                    break;
                }
            }
            if (link != null && !link.isBlank()) {
                files.add(new InstructionFile(fileName, link));
            }
        }

        if (!files.isEmpty()) {
            return distinctInstructionFiles(files);
        }

        Elements allClickable = doc.select("a[href], a[onclick], button[onclick], input[onclick], span[onclick]");
        for (Element element : allClickable) {
            String text = element.text().isBlank() ? element.attr("value") : element.text();
            text = text.trim();
            if (!text.toLowerCase().contains("download")) continue;
            String link = extractDownloadCandidateFromElement(element, pageUrl);
            if (link == null) continue;
            String name = text.replaceAll("(?i)download", "").trim();
            if (name.isBlank()) name = "instruction_file";
            files.add(new InstructionFile(sanitizeFileName(name), link));
        }
        return distinctInstructionFiles(files);
    }

    private List<InstructionFile> distinctInstructionFiles(List<InstructionFile> files) {
        List<InstructionFile> distinct = new ArrayList<>();
        Set<String> links = new HashSet<>();
        for (InstructionFile f : files) {
            if (links.add(f.downloadLink())) {
                distinct.add(f);
            }
        }
        return distinct;
    }

    public String resolveOrigin(String url) {
        try {
            HttpUrl parsed = HttpUrl.parse(url);
            if (parsed != null) {
                boolean defaultPort = (parsed.scheme().equals("https") && parsed.port() == 443)
                        || (parsed.scheme().equals("http") && parsed.port() == 80);
                if (defaultPort) {
                    return parsed.scheme() + "://" + parsed.host();
                } else {
                    return parsed.scheme() + "://" + parsed.host() + ":" + parsed.port();
                }
            }
        } catch (IllegalArgumentException ex) {
            ErrorReporter.logError("PortalRepository#resolveOrigin", ex);
        }
        return BASE_URL;
    }

    public Request buildDownloadFollowRequest(String url, String referer) {
        return new Request.Builder()
                .url(url)
                .header("Referer", referer)
                .header("User-Agent", USER_AGENT)
                .build();
    }

    public Request buildPostBackRequestFromPage(String pageUrl, String html, PostBackInfo info) {
        Document doc = Jsoup.parse(html, pageUrl);
        Element form = null;
        for (Element f : doc.select("form")) {
            if (f.selectFirst("input[name=__VIEWSTATE]") != null) {
                form = f;
                break;
            }
        }
        if (form == null) {
            form = doc.selectFirst("form");
        }
        if (form == null) return null;

        FormBody.Builder postBuilder = new FormBody.Builder();
        for (Element hidden : form.select("input[type=hidden]")) {
            String name = hidden.attr("name");
            if (name.isBlank() || name.equals("__EVENTTARGET") || name.equals("__EVENTARGUMENT")) continue;
            postBuilder.add(name, hidden.attr("value"));
        }
        postBuilder.add("__EVENTTARGET", info.target());
        postBuilder.add("__EVENTARGUMENT", info.argument());

        String formAction = form.attr("action");
        String postUrl = "";
        if (formAction.isBlank()) {
            postUrl = pageUrl;
        } else if (formAction.toLowerCase().startsWith("http")) {
            postUrl = formAction;
        } else {
            postUrl = normalizeUrl(formAction, pageUrl);
        }
        if (postUrl.isBlank()) return null;

        return new Request.Builder()
                .url(postUrl)
                .post(postBuilder.build())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Referer", pageUrl)
                .header("Origin", resolveOrigin(pageUrl))
                .header("User-Agent", USER_AGENT)
                .build();
    }

    public HtmlDownloadCandidate findDownloadCandidateInHtml(String html, String pageUrl) {
        Document doc = Jsoup.parse(html, pageUrl);

        List<Element> assignmentFilesLinks = new ArrayList<>();
        for (Element row : doc.select("table tr")) {
            String rowText = row.text().toLowerCase();
            if (rowText.contains("download") && row.select("th").isEmpty()) {
                assignmentFilesLinks.addAll(row.select("a[href], a[onclick], button[onclick], input[onclick], span[onclick]"));
            }
        }
        for (Element element : assignmentFilesLinks) {
            HtmlDownloadCandidate candidate = extractCandidateFromClickable(element, pageUrl);
            if (candidate != null) return candidate;
        }

        Element metaRefresh = doc.selectFirst("meta[http-equiv~=(?i)refresh]");
        if (metaRefresh != null) {
            String refreshContent = metaRefresh.attr("content");
            if (refreshContent != null) {
                Matcher m = Pattern.compile("url\\s*=\\s*([^;]+)", Pattern.CASE_INSENSITIVE).matcher(refreshContent);
                if (m.find()) {
                    String refreshUrl = m.group(1).trim().replaceAll("^['\"]|['\"]$", "");
                    if (!refreshUrl.isBlank()) {
                        String normalizedRefresh = normalizeUrl(refreshUrl, pageUrl);
                        if (!normalizedRefresh.isBlank()) {
                            return new HtmlDownloadCandidate(normalizedRefresh, null);
                        }
                    }
                }
            }
        }

        for (Element element : doc.select("iframe[src], frame[src], embed[src], object[data]")) {
            String raw = element.hasAttr("src") ? element.attr("src") : element.attr("data");
            String normalized = normalizeUrl(raw, pageUrl);
            if (!normalized.isBlank()) {
                return new HtmlDownloadCandidate(normalized, null);
            }
        }

        Elements clickableElements = doc.select("a[href], a[onclick], button[onclick], input[onclick], span[onclick]");
        List<Element> prioritized = new ArrayList<>(clickableElements);
        prioritized.sort((e1, e2) -> {
            int score1 = looksLikeDownloadTrigger(e1) ? 1 : 0;
            int score2 = looksLikeDownloadTrigger(e2) ? 1 : 0;
            return Integer.compare(score2, score1); // descending
        });

        for (Element element : prioritized) {
            if (looksLikeDownloadTrigger(element)) {
                HtmlDownloadCandidate candidate = extractCandidateFromClickable(element, pageUrl);
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        for (Element element : prioritized) {
            HtmlDownloadCandidate candidate = extractCandidateFromClickable(element, pageUrl);
            if (candidate != null) {
                return candidate;
            }
        }

        return null;
    }

    // ---------- Ported Downloader Methods ----------
    public DownloadResult executeDownloadRequest(Request request) {
        return executeDownloadRequest(request, 0);
    }

    public DownloadResult executeDownloadRequest(Request request, int depth) {
        if (depth > 6) {
            return new DownloadResult.Rejected("Download redirect chain is too long.");
        }

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return new DownloadResult.Rejected("Server rejected download (HTTP " + response.code() + ").");
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return new DownloadResult.Error("Empty server response.");
            }
            String mimeType = null;
            if (responseBody.contentType() != null) {
                String temp = responseBody.contentType().toString();
                if (!temp.isBlank()) mimeType = temp;
            }
            String contentDisposition = response.header("Content-Disposition");
            boolean hasAttachmentHeader = contentDisposition != null &&
                    (contentDisposition.toLowerCase().contains("attachment") ||
                     contentDisposition.toLowerCase().contains("filename"));
            byte[] bytes = responseBody.bytes();
            String finalUrl = response.request().url().toString();

            if (finalUrl.toLowerCase().contains("login.aspx")) {
                return new DownloadResult.Rejected("Session expired. Please sign in again.");
            }

            boolean isHtmlLike = ((mimeType != null && mimeType.toLowerCase().contains("text/html"))
                    || looksLikeHtmlPayload(bytes)) && !hasAttachmentHeader;

            if (isHtmlLike) {
                String html = new String(bytes, StandardCharsets.UTF_8);
                if (isLoginPage(finalUrl, html)) {
                    return new DownloadResult.Rejected("Session expired. Please sign in again.");
                }

                String redirectUrl = extractRedirectUrlFromHtml(html);
                if (redirectUrl != null && !redirectUrl.isBlank()) {
                    String normalizedRedirect = normalizeUrl(redirectUrl, finalUrl);
                    if (!normalizedRedirect.isBlank() && !normalizedRedirect.equalsIgnoreCase(finalUrl)) {
                        Request followRequest = buildDownloadFollowRequest(normalizedRedirect, finalUrl);
                        return executeDownloadRequest(followRequest, depth + 1);
                    }
                }

                HtmlDownloadCandidate candidate = findDownloadCandidateInHtml(html, finalUrl);
                if (candidate != null) {
                    if (candidate.url() != null && !candidate.url().isBlank() && !candidate.url().equalsIgnoreCase(finalUrl)) {
                        Request candidateRequest = buildDownloadFollowRequest(candidate.url(), finalUrl);
                        return executeDownloadRequest(candidateRequest, depth + 1);
                    }
                    if (candidate.postBackInfo() != null) {
                        Request postBackRequest = buildPostBackRequestFromPage(finalUrl, html, candidate.postBackInfo());
                        if (postBackRequest != null) {
                            return executeDownloadRequest(postBackRequest, depth + 1);
                        }
                    }
                }

                if (html.toLowerCase().contains("__viewstate") || html.toLowerCase().contains("courseportal")) {
                    return new DownloadResult.Rejected("Portal did not return the assignment instruction file.");
                }
            }

            String fileName = extractFileName(contentDisposition, finalUrl, mimeType);
            return new DownloadResult.Success(bytes, fileName, mimeType != null ? mimeType : "application/octet-stream");
        } catch (IOException e) {
            return new DownloadResult.NetworkError();
        } catch (Exception e) {
            return new DownloadResult.Error(e.getMessage() != null ? e.getMessage() : "Download failed.");
        }
    }

    public String extractRedirectUrlFromHtml(String html) {
        if (html == null) return null;
        List<Pattern> patterns = List.of(
            Pattern.compile("window\\.location(?:\\.href)?\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("location\\.replace\\(\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("window\\.open\\(\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("url\\s*=\\s*([^;\"'>]+)", Pattern.CASE_INSENSITIVE)
        );
        for (Pattern p : patterns) {
            Matcher m = p.matcher(html);
            if (m.find()) {
                String val = m.group(1);
                if (val != null) {
                    val = val.trim().replaceAll("^['\"]|['\"]$", "");
                    if (!val.isEmpty()) return val;
                }
            }
        }
        return null;
    }

    public DownloadResult downloadAssignmentViaPostBack(PostBackInfo info) {
        String assignmentsUrl = BASE_URL + "/CoursePortal.aspx";
        Request getRequest = new Request.Builder()
                .url(assignmentsUrl)
                .header("Referer", assignmentsUrl)
                .header("User-Agent", USER_AGENT)
                .build();

        String finalUrl;
        String html;
        try (Response response = client.newCall(getRequest).execute()) {
            if (!response.isSuccessful()) {
                return new DownloadResult.Rejected("Server rejected download (HTTP " + response.code() + ").");
            }
            ResponseBody body = response.body();
            if (body == null) {
                return new DownloadResult.Error("Empty server response.");
            }
            finalUrl = response.request().url().toString();
            html = body.string();
        } catch (IOException e) {
            return new DownloadResult.NetworkError();
        }

        boolean notAuthenticated = isLoginPage(finalUrl, html) || !hasSessionCookiesForHost(BASE_HOST);
        if (notAuthenticated) {
            return new DownloadResult.Rejected("Session expired. Please sign in again.");
        }

        Document doc = Jsoup.parse(html);
        Element form = null;
        for (Element f : doc.select("form")) {
            if (f.selectFirst("input[name=__VIEWSTATE]") != null) {
                form = f;
                break;
            }
        }
        if (form == null) {
            form = doc.selectFirst("form");
        }
        if (form == null) {
            return new DownloadResult.Error("Portal form not found.");
        }

        FormBody.Builder postBuilder = new FormBody.Builder();
        for (Element hidden : form.select("input[type=hidden]")) {
            String name = hidden.attr("name");
            if (name.isBlank() || name.equals("__EVENTTARGET") || name.equals("__EVENTARGUMENT")) continue;
            postBuilder.add(name, hidden.attr("value"));
        }
        postBuilder.add("__EVENTTARGET", info.target());
        postBuilder.add("__EVENTARGUMENT", info.argument());

        String formAction = form.attr("action");
        String postUrl = "";
        if (formAction.isBlank()) {
            postUrl = assignmentsUrl;
        } else if (formAction.toLowerCase().startsWith("http")) {
            postUrl = formAction;
        } else if (formAction.startsWith("/")) {
            postUrl = BASE_URL + formAction;
        } else {
            postUrl = BASE_URL + "/" + formAction;
        }

        Request postRequest = new Request.Builder()
                .url(postUrl)
                .post(postBuilder.build())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Referer", assignmentsUrl)
                .header("Origin", BASE_URL)
                .header("User-Agent", USER_AGENT)
                .build();

        return executeDownloadRequest(postRequest);
    }

    public DownloadResult downloadAssignmentViaPostBack(PostBackLink postBackLink) {
        String sourcePageUrl = postBackLink.sourcePageUrl();
        if (sourcePageUrl == null || sourcePageUrl.isBlank()) {
            sourcePageUrl = BASE_URL + "/CoursePortal.aspx";
        } else {
            sourcePageUrl = normalizeUrl(sourcePageUrl);
        }
        Request getRequest = new Request.Builder()
                .url(sourcePageUrl)
                .header("Referer", sourcePageUrl)
                .header("User-Agent", USER_AGENT)
                .build();

        String finalUrl;
        String html;
        try (Response response = client.newCall(getRequest).execute()) {
            if (!response.isSuccessful()) {
                return new DownloadResult.Rejected("Server rejected download (HTTP " + response.code() + ").");
            }
            ResponseBody body = response.body();
            if (body == null) {
                return new DownloadResult.Error("Empty server response.");
            }
            finalUrl = response.request().url().toString();
            html = body.string();
        } catch (IOException e) {
            return new DownloadResult.NetworkError();
        }

        boolean notAuthenticated = isLoginPage(finalUrl, html) || !hasSessionCookiesForHost(BASE_HOST);
        if (notAuthenticated) {
            return new DownloadResult.Rejected("Session expired. Please sign in again.");
        }

        Request postRequest = buildPostBackRequestFromPage(finalUrl, html, postBackLink.info());
        if (postRequest == null) {
            return new DownloadResult.Error("Portal form not found.");
        }

        return executeDownloadRequest(postRequest);
    }

    public DownloadResult downloadAssignment(String downloadUrl) {
        try {
            if (downloadUrl == null || downloadUrl.isBlank()) {
                return new DownloadResult.Rejected("Download link is unavailable.");
            }

            if (isPostBackDownloadLink(downloadUrl)) {
                PostBackLink postBackLink = extractPostBackLinkFromLink(downloadUrl);
                if (postBackLink == null) {
                    return new DownloadResult.Rejected("Download link is invalid.");
                }
                return downloadAssignmentViaPostBack(postBackLink);
            }

            String normalizedUrl = normalizeUrl(downloadUrl);
            if (normalizedUrl.isBlank()) {
                return new DownloadResult.Rejected("Download link is invalid.");
            }

            Request request = new Request.Builder()
                    .url(normalizedUrl)
                    .header("Referer", BASE_URL + "/CoursePortal.aspx")
                    .header("User-Agent", USER_AGENT)
                    .build();

            return executeDownloadRequest(request);
        } catch (Exception e) {
            logE("PortalAuth", "Download error: " + e.getMessage(), e);
            return new DownloadResult.Error(e.getMessage() != null ? e.getMessage() : "Download failed.");
        }
    }

    public String normalizeUrl(String href) {
        return normalizeUrl(href, BASE_URL);
    }

    public String normalizeUrl(String href, String resolveBaseUrl) {
        if (href == null || href.isBlank()) return "";
        String trimmed = href.trim();
        if (trimmed.isEmpty() || trimmed.equals("#") || trimmed.toLowerCase().startsWith("javascript")) return "";
        if (trimmed.toLowerCase().startsWith("http")) return trimmed;

        String absBase = resolveBaseUrl;
        if (absBase == null || absBase.isBlank()) {
            absBase = BASE_URL + "/CoursePortal.aspx";
        } else if (!absBase.toLowerCase().startsWith("http")) {
            if (absBase.startsWith("/")) {
                absBase = BASE_URL + absBase;
            } else {
                absBase = BASE_URL + "/" + absBase;
            }
        }

        try {
            HttpUrl base = HttpUrl.parse(absBase);
            if (base != null) {
                HttpUrl resolved = base.resolve(trimmed);
                if (resolved != null) {
                    return resolved.toString();
                }
            }
        } catch (IllegalArgumentException ex) {
            ErrorReporter.logError("PortalRepository#normalizeUrl", ex);
        }
        return "";
    }

    // ---------- Ported Uploader Methods ----------
    public UploadResult uploadAssignment(String submitPageUrl, File file) {
        try {
            logD("PortalAuth", "=== UPLOAD START ===");
            logD("PortalAuth", "Submit URL: " + submitPageUrl);
            logD("PortalAuth", "File: " + file.getName() + " (" + file.length() + " bytes)");

            if (submitPageUrl != null && isPostBackDownloadLink(submitPageUrl)) {
                PostBackLink postBackLink = extractPostBackLinkFromLink(submitPageUrl);
                if (postBackLink == null) {
                    return new UploadResult.Rejected("Upload link is invalid.");
                }
                return uploadAssignmentViaPostBack(postBackLink, file);
            }

            if (submitPageUrl == null || submitPageUrl.isBlank()) {
                logE("PortalAuth", "Submit URL is empty or blank!");
                logD("PortalAuth", "This might be a re-upload of an already-submitted assignment");
                logD("PortalAuth", "Trying to fetch CoursePortal page instead...");

                String altUrl = BASE_URL + "/CoursePortal.aspx";
                logD("PortalAuth", "Using alternate URL: " + altUrl);

                Request getRequest = new Request.Builder()
                        .url(altUrl)
                        .header("Referer", BASE_URL + "/CoursePortal.aspx")
                        .header("User-Agent", USER_AGENT)
                        .build();

                String pageHtml;
                try (Response response = client.newCall(getRequest).execute()) {
                    if (!response.isSuccessful()) {
                        logE("PortalAuth", "Upload prefetch HTTP " + response.code());
                        return new UploadResult.Rejected("Server rejected request (HTTP " + response.code() + ").");
                    }
                    ResponseBody body = response.body();
                    pageHtml = body != null ? body.string() : "";
                }

                logD("PortalAuth", "Page HTML length: " + pageHtml.length());

                Document doc = Jsoup.parse(pageHtml);
                Element form = doc.select("form").first();
                if (form == null) {
                    logE("PortalAuth", "Form not found on alternate page");
                    return new UploadResult.Rejected("Upload form not found.");
                }

                logD("PortalAuth", "Using CoursePortal form for re-upload");
                return uploadWithForm(form, file, pageHtml);
            }

            logD("PortalAuth", "Step 1: Fetching submission page...");
            Request getRequest = new Request.Builder()
                    .url(submitPageUrl)
                    .header("Referer", BASE_URL + "/CoursePortal.aspx")
                    .header("User-Agent", USER_AGENT)
                    .build();

            String pageHtml;
            try (Response response = client.newCall(getRequest).execute()) {
                if (!response.isSuccessful()) {
                    logE("PortalAuth", "Upload page fetch HTTP " + response.code());
                    return new UploadResult.Rejected("Server rejected request (HTTP " + response.code() + ").");
                }
                ResponseBody body = response.body();
                pageHtml = body != null ? body.string() : "";
            }

            logD("PortalAuth", "Page HTML length: " + pageHtml.length());

            Document doc = Jsoup.parse(pageHtml);
            Element form = doc.select("form").first();
            if (form == null) {
                logE("PortalAuth", "Form not found");
                return new UploadResult.Rejected("Upload form not found.");
            }

            logD("PortalAuth", "Form found, action: " + form.attr("action") + ", method: " + form.attr("method"));

            logD("PortalAuth", "=== PAGE CONTENT ===");
            String allText = doc.body() != null ? doc.body().text() : "";
            logD("PortalAuth", "Page body text length: " + allText.length());

            Elements labels = doc.select("label");
            if (!labels.isEmpty()) {
                logD("PortalAuth", "Found " + labels.size() + " labels on page:");
                for (Element label : labels.subList(0, Math.min(10, labels.size()))) {
                    logD("PortalAuth", "  Label: " + label.text());
                }
            }

            Elements textareas = doc.select("textarea");
            if (!textareas.isEmpty()) {
                logD("PortalAuth", "Found " + textareas.size() + " textareas");
                for (Element ta : textareas) {
                    logD("PortalAuth", "  Textarea: name='" + ta.attr("name") + "', id='" + ta.attr("id") + "'");
                }
            }

            Elements visibleInputs = doc.select("input[type=text], input[type=password], input[type=email]");
            if (!visibleInputs.isEmpty()) {
                logD("PortalAuth", "Found " + visibleInputs.size() + " visible input fields");
                for (Element inp : visibleInputs) {
                    logD("PortalAuth", "  Input: name='" + inp.attr("name") + "', placeholder='" + inp.attr("placeholder") + "'");
                }
            }

            return uploadWithForm(form, file, pageHtml);
        } catch (IOException e) {
            return new UploadResult.NetworkError();
        } catch (Exception e) {
            logE("PortalAuth", "Upload error: " + e.getMessage(), e);
            return new UploadResult.Error(e.getMessage() != null ? e.getMessage() : "Upload failed.");
        }
    }

    public UploadResult uploadAssignmentViaPostBack(PostBackLink postBackLink, File file) {
        String sourcePageUrl = postBackLink.sourcePageUrl();
        if (sourcePageUrl == null || sourcePageUrl.isBlank()) {
            sourcePageUrl = BASE_URL + "/CoursePortal.aspx";
        } else {
            sourcePageUrl = normalizeUrl(sourcePageUrl);
        }
        Request getRequest = new Request.Builder()
                .url(sourcePageUrl)
                .header("Referer", sourcePageUrl)
                .header("User-Agent", USER_AGENT)
                .build();

        String finalUrl;
        String html;
        try (Response response = client.newCall(getRequest).execute()) {
            if (!response.isSuccessful()) {
                return new UploadResult.Rejected("Server rejected request (HTTP " + response.code() + ").");
            }
            ResponseBody body = response.body();
            if (body == null) {
                return new UploadResult.Error("Empty server response.");
            }
            finalUrl = response.request().url().toString();
            html = body.string();
        } catch (IOException e) {
            return new UploadResult.NetworkError();
        }

        boolean notAuthenticated = isLoginPage(finalUrl, html) || !hasSessionCookiesForHost(BASE_HOST);
        if (notAuthenticated) {
            return new UploadResult.Rejected("Session expired. Please sign in again.");
        }

        Request postRequest = buildPostBackRequestFromPage(finalUrl, html, postBackLink.info());
        if (postRequest == null) {
            return new UploadResult.Rejected("Upload form not found.");
        }

        String uploadPageUrl;
        String uploadPageHtml;
        try (Response response = client.newCall(postRequest).execute()) {
            if (!response.isSuccessful()) {
                return new UploadResult.Rejected("Server rejected request (HTTP " + response.code() + ").");
            }
            ResponseBody body = response.body();
            if (body == null) {
                return new UploadResult.Error("Empty server response.");
            }
            uploadPageUrl = response.request().url().toString();
            uploadPageHtml = body.string();
        } catch (IOException e) {
            return new UploadResult.NetworkError();
        }

        Document uploadDoc = Jsoup.parse(uploadPageHtml, uploadPageUrl);
        Element uploadForm = uploadDoc.select("form").stream().filter(f -> f.selectFirst("input[type=file]") != null).findFirst().orElse(null);
        if (uploadForm == null) {
            uploadForm = uploadDoc.select("form").stream().filter(f -> f.selectFirst("input[name=__VIEWSTATE]") != null).findFirst().orElse(null);
        }
        if (uploadForm == null) {
            uploadForm = uploadDoc.selectFirst("form");
        }
        if (uploadForm == null) {
            return new UploadResult.Rejected("Upload form not found.");
        }

        return uploadWithForm(uploadForm, file, uploadPageHtml);
    }

    public UploadResult uploadWithForm(Element form, File file, String pageHtml) {
        try {
            logD("PortalAuth", "Step 2: Processing form for upload...");

            Document doc = Jsoup.parse(pageHtml);

            Element targetForm = form;
            Element fileInput = form.select("input[type=file]").first();
            if (fileInput == null) {
                logD("PortalAuth", "File input not in main form, searching all forms on page...");
                Elements allForms = doc.select("form");
                logD("PortalAuth", "Found " + allForms.size() + " total forms on page");

                for (Element f : allForms) {
                    Element fi = f.select("input[type=file]").first();
                    if (fi != null) {
                        logD("PortalAuth", "Found file input in a different form!");
                        targetForm = f;
                        break;
                    }
                }
            }

            String viewState = targetForm.select("input[name='__VIEWSTATE']").attr("value");
            if (viewState == null) viewState = "";
            String eventValidation = targetForm.select("input[name='__EVENTVALIDATION']").attr("value");
            if (eventValidation == null) eventValidation = "";
            String viewStateGenerator = targetForm.select("input[name='__VIEWSTATEGENERATOR']").attr("value");
            if (viewStateGenerator == null) viewStateGenerator = "";

            logD("PortalAuth", "ViewState found: " + !viewState.isEmpty());
            logD("PortalAuth", "EventValidation found: " + !eventValidation.isEmpty());

            Elements hiddenFields = targetForm.select("input[type=hidden]");
            logD("PortalAuth", "Total hidden fields in form: " + hiddenFields.size());

            logD("PortalAuth", "Step 3: Building form with file...");
            MultipartBody.Builder formBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            logD("PortalAuth", "=== ALL FORM FIELDS ===");
            logD("PortalAuth", "Form tag attributes: action='" + targetForm.attr("action") + "', method='" + targetForm.attr("method") + "'");
            Elements allInputs = targetForm.select("input");
            logD("PortalAuth", "Total input fields: " + allInputs.size());
            for (Element input : allInputs) {
                String type = input.attr("type");
                String name = input.attr("name");
                String val = input.attr("value");
                if (val.length() > 50) val = val.substring(0, 50);
                logD("PortalAuth", "  " + type + ": " + name + " = " + val);
            }

            String defaultEventTarget = "ctl00$DataContent$btnAddFile";
            Elements submitButtons = targetForm.select("input[type=button], input[type=submit], button[type=button], button[type=submit], button[name]");
            logD("PortalAuth", "Found " + submitButtons.size() + " submit buttons");
            for (Element btn : submitButtons) {
                logD("PortalAuth", "  Button: name='" + btn.attr("name") + "', value='" + btn.attr("value") + "', text='" + btn.text() + "'");
            }

            Element preferredButton = null;
            int maxScore = -1;
            for (Element btn : submitButtons) {
                String fingerprint = (btn.attr("name") + " " +
                        btn.attr("id") + " " +
                        btn.attr("value") + " " +
                        btn.text() + " " +
                        btn.className()).toLowerCase();
                int score = 0;
                if (fingerprint.contains("addfile") || fingerprint.contains("updatefile")) score = 6;
                else if (fingerprint.contains("upload")) score = 5;
                else if (fingerprint.contains("change")) score = 4;
                else if (fingerprint.contains("submit")) score = 3;
                else if (fingerprint.contains("assignment") || fingerprint.contains("attach")) score = 2;

                if (score > maxScore) {
                    maxScore = score;
                    preferredButton = btn;
                }
            }

            String preferredButtonName = null;
            if (preferredButton != null) {
                String nameAttr = preferredButton.attr("name").trim();
                if (!nameAttr.isEmpty()) {
                    preferredButtonName = nameAttr;
                } else {
                    String idAttr = preferredButton.attr("id").trim();
                    if (!idAttr.isEmpty()) {
                        preferredButtonName = idAttr.replace("_", "$");
                    }
                }
            }

            boolean isSubmitButton = false;
            if (preferredButton != null) {
                String type = preferredButton.attr("type").toLowerCase();
                String tag = preferredButton.tagName().toLowerCase();
                isSubmitButton = (tag.equals("input") && (type.equals("submit") || type.equals("image"))) ||
                                 (tag.equals("button") && type.equals("submit"));
            }

            String eventTarget = preferredButtonName != null ? preferredButtonName : defaultEventTarget;
            if (!isSubmitButton) {
                logD("PortalAuth", "Using __EVENTTARGET: " + eventTarget);
                formBuilder.addFormDataPart("__EVENTTARGET", eventTarget);
                formBuilder.addFormDataPart("__EVENTARGUMENT", "");
            } else {
                logD("PortalAuth", "Button is a standard submit button, skipping __EVENTTARGET");
            }

            formBuilder.addFormDataPart("__VIEWSTATE", viewState);
            formBuilder.addFormDataPart("__EVENTVALIDATION", eventValidation);
            if (!viewStateGenerator.isEmpty()) {
                formBuilder.addFormDataPart("__VIEWSTATEGENERATOR", viewStateGenerator);
            }

            if (preferredButtonName != null && preferredButton != null) {
                String preferredButtonValue = preferredButton.attr("value").trim();
                if (preferredButtonValue.isEmpty()) {
                    preferredButtonValue = preferredButton.text().trim();
                }
                if (!preferredButtonValue.isEmpty()) {
                    logD("PortalAuth", "Adding trigger button field: " + preferredButtonName + " = " + preferredButtonValue);
                    formBuilder.addFormDataPart(preferredButtonName, preferredButtonValue);
                }
            }

            for (Element input : targetForm.select("input[type=hidden]")) {
                String name = input.attr("name");
                String value = input.attr("value");
                logD("PortalAuth", "  Hidden: " + name + " = " + (value.length() > 100 ? value.substring(0, 100) : value));
                if (!name.isEmpty() &&
                        !name.startsWith("__VIEWSTATE") &&
                        !name.startsWith("__EVENTVALIDATION") &&
                        !name.startsWith("__EVENTTARGET") &&
                        !name.startsWith("__EVENTARGUMENT")) {
                    formBuilder.addFormDataPart(name, value);
                }
            }

            Elements fileInputs = targetForm.select("input[type=file]");
            logD("PortalAuth", "Found " + fileInputs.size() + " file input fields:");
            if (fileInputs.isEmpty()) {
                return new UploadResult.Rejected("Upload dialog not found. The assignment may be closed or the portal layout has changed.");
            }
            
            String fileInputName = "";
            for (Element input : fileInputs) {
                String name = input.attr("name");
                logD("PortalAuth", "  File input name: '" + name + "'");
                if (!name.isEmpty()) {
                    fileInputName = name;
                    break; // Pick the FIRST file input as the primary submission slot!
                }
            }
            if (fileInputName.isEmpty()) {
                return new UploadResult.Rejected("Upload file input lacks a name attribute.");
            }

            logD("PortalAuth", "Adding file: " + file.getName());
            logD("PortalAuth", "Using file input name: " + fileInputName);
            RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
            formBuilder.addFormDataPart(fileInputName, file.getName(), fileBody);

            logD("PortalAuth", "Step 4: Submitting form with file...");
            MultipartBody formBody = formBuilder.build();
            logD("PortalAuth", "Form has " + formBody.parts().size() + " parts");

            String formAction = targetForm.attr("action");
            String postUrl = "";
            if (formAction.isBlank()) {
                postUrl = BASE_URL + "/CoursePortal.aspx";
            } else if (formAction.toLowerCase().startsWith("http")) {
                postUrl = formAction;
            } else if (formAction.startsWith("/")) {
                postUrl = BASE_URL + formAction;
            } else {
                postUrl = BASE_URL + "/" + formAction;
            }

            logD("PortalAuth", "Posting to: " + postUrl);

            Request postRequest = new Request.Builder()
                    .url(postUrl)
                    .post(formBody)
                    .header("Referer", BASE_URL + "/CoursePortal.aspx")
                    .header("Origin", BASE_URL)
                    .header("User-Agent", USER_AGENT)
                    .build();

            String responseUrl;
            String responseHtml;
            try (Response response = client.newCall(postRequest).execute()) {
                if (!response.isSuccessful()) {
                    logE("PortalAuth", "Upload submit HTTP " + response.code());
                    return new UploadResult.Rejected("Server rejected request (HTTP " + response.code() + ").");
                }
                ResponseBody body = response.body();
                responseUrl = response.request().url().toString();
                responseHtml = body != null ? body.string() : "";
            }

            logD("PortalAuth", "Response URL: " + responseUrl);
            logD("PortalAuth", "Response HTML length: " + responseHtml.length());

            if (isUploadSizeErrorRedirect(responseUrl, responseHtml)) {
                logD("PortalAuth", "Upload failed: redirected to portal upload error page");
                return new UploadResult.Rejected("Upload rejected: file too large.");
            }

            if (responseHtml.contains("</form>")) {
                int formEndIdx = responseHtml.indexOf("</form>");
                logD("PortalAuth", "Form snippet (last 500 chars): " +
                        responseHtml.substring(Math.max(0, formEndIdx - 500), formEndIdx));
            }

            if (responseHtml.toLowerCase().contains("aspnethidden")) {
                // Just an indicator of ASP.NET markup, not necessarily an error
            }
            if (responseHtml.toLowerCase().contains("__viewstate")) {
                logD("PortalAuth", "Response contains __VIEWSTATE - page reloaded");
            }

            Document doc2 = Jsoup.parse(responseHtml);
            List<String> visibleValidationMessages = new ArrayList<>();

            // Helper to check visibility
            class VisibilityChecker {
                boolean isLikelyVisible(Element el) {
                    Element node = el;
                    while (node != null) {
                        String style = node.attr("style").toLowerCase();
                        String className = node.className().toLowerCase();
                        if (node.hasAttr("hidden")) return false;
                        if (node.attr("aria-hidden").equalsIgnoreCase("true")) return false;
                        if (style.contains("display:none") || style.contains("visibility:hidden")) return false;
                        if (className.contains("d-none") || className.contains("invisible")) return false;
                        node = node.parent();
                    }
                    return true;
                }
            }
            VisibilityChecker checker = new VisibilityChecker();

            Elements validatorSpans = doc2.select("[id*='Validator']");
            if (!validatorSpans.isEmpty()) {
                logD("PortalAuth", "Found " + validatorSpans.size() + " validator elements");
                for (Element elem : validatorSpans) {
                    if (!checker.isLikelyVisible(elem)) continue;
                    String text = elem.text();
                    if (!text.isEmpty()) {
                        logD("PortalAuth", "  Validator text: " + text);
                        visibleValidationMessages.add(text);
                    }
                }
            }

            Elements errorDivs = doc2.select(".error, [id*='error'], .notification");
            if (!errorDivs.isEmpty()) {
                logD("PortalAuth", "Found " + errorDivs.size() + " error divs");
                for (Element elem : errorDivs) {
                    if (!checker.isLikelyVisible(elem)) continue;
                    String text = elem.text();
                    if (!text.isEmpty()) {
                        logD("PortalAuth", "  Error div: " + text);
                        visibleValidationMessages.add(text);
                    }
                }
            }

            Elements summaryControls = doc2.select("[id*='ValidationSummary'], [id*='Summary']");
            for (Element elem : summaryControls) {
                if (!checker.isLikelyVisible(elem)) continue;
                String text = elem.text();
                if (!text.isEmpty()) {
                    logD("PortalAuth", "  Summary control: " + text);
                    visibleValidationMessages.add(text);
                }
            }

            boolean hasFileInput = responseHtml.toLowerCase().contains("fileassignment1");
            boolean hasSuccessMessage = responseHtml.toLowerCase().contains("file once uploaded cannot be changed") ||
                    responseHtml.toLowerCase().contains("successfully uploaded") ||
                    responseHtml.toLowerCase().contains("submission successful") ||
                    responseHtml.toLowerCase().contains("file uploaded") ||
                    responseHtml.toLowerCase().contains("your file has been submitted") ||
                    responseHtml.toLowerCase().contains("assignment file updated succefully") ||
                    responseHtml.toLowerCase().contains("assignment file updated successfully");

            boolean hasViewstate = responseHtml.toLowerCase().contains("__viewstate");
            boolean hasForm = responseHtml.toLowerCase().contains("<form");

            List<String> cleanedValidationMessages = new ArrayList<>();
            for (String message : visibleValidationMessages) {
                String cleaned = message.replaceAll("\\s+", " ").trim();
                if (!cleaned.isEmpty()) {
                    cleanedValidationMessages.add(cleaned);
                }
            }

            String normalizedValidationText = String.join(" | ", cleanedValidationMessages).toLowerCase().replaceAll("\\s+", " ");
            logD("PortalAuth", "Visible validation text: " + normalizedValidationText);

            String portalSizeMessage = null;
            for (String message : cleanedValidationMessages) {
                String normalized = message.toLowerCase();
                boolean hasSizeToken = normalized.contains("size") || normalized.contains("mb") || normalized.contains("kb");
                boolean hasLimitToken = normalized.contains("max") ||
                        normalized.contains("maximum") ||
                        normalized.contains("limit") ||
                        normalized.contains("exceed") ||
                        normalized.contains("less than");
                if (hasSizeToken && hasLimitToken) {
                    portalSizeMessage = message;
                    break;
                }
            }

            String portalValidationErrorMessage = null;
            for (String message : cleanedValidationMessages) {
                String normalized = message.toLowerCase();
                if (normalized.contains("required") ||
                        normalized.contains("invalid") ||
                        normalized.contains("not allowed") ||
                        normalized.contains("closed") ||
                        normalized.contains("too large") ||
                        normalized.contains("maximum") ||
                        normalized.contains("exceed")) {
                    portalValidationErrorMessage = message;
                    break;
                }
            }

            boolean hasFormatError = normalizedValidationText.contains("only .zip,.rar,.doc,.docx and .pdf allowed") ||
                    normalizedValidationText.contains("format is not allowed") ||
                    normalizedValidationText.contains("file format is not allowed");
            boolean hasMissingFileError = normalizedValidationText.contains("required") &&
                    (normalizedValidationText.contains("fileuploadvalidator") || normalizedValidationText.contains("file"));
            boolean hasInvalidFileError = normalizedValidationText.contains("invalid file");
            boolean hasSizeError = (normalizedValidationText.contains("size") &&
                    (normalizedValidationText.contains("maximum") ||
                            normalizedValidationText.contains("max") ||
                            normalizedValidationText.contains("limit") ||
                            normalizedValidationText.contains("exceed") ||
                            normalizedValidationText.contains("less than"))) ||
                    portalSizeMessage != null;
            boolean hasClosedError = normalizedValidationText.contains("closed") && normalizedValidationText.contains("assignment");
            boolean hasGenericPortalError = portalValidationErrorMessage != null;

            boolean hasError = hasFormatError ||
                    hasMissingFileError ||
                    hasInvalidFileError ||
                    hasSizeError ||
                    hasClosedError ||
                    hasGenericPortalError;

            String rejectionReason = "Upload rejected by server.";
            if (hasFormatError) {
                rejectionReason = "Upload rejected: only .zip, .rar, .doc, .docx, .pdf are allowed.";
            } else if (hasMissingFileError) {
                rejectionReason = "Upload rejected: file missing or form not accepted.";
            } else if (hasInvalidFileError) {
                rejectionReason = "Upload rejected: invalid file.";
            } else if (hasSizeError) {
                rejectionReason = portalSizeMessage != null ? "Upload rejected: " + portalSizeMessage : "Upload rejected: file too large.";
            } else if (hasClosedError) {
                rejectionReason = "Upload rejected: assignment is closed.";
            } else if (hasGenericPortalError) {
                rejectionReason = "Upload rejected: " + portalValidationErrorMessage;
            }

            logD("PortalAuth", "Success indicators:");
            logD("PortalAuth", "  Has file input field: " + hasFileInput);
            logD("PortalAuth", "  Has success message: " + hasSuccessMessage);
            logD("PortalAuth", "  Has viewstate: " + hasViewstate);
            logD("PortalAuth", "  Has form: " + hasForm);
            logD("PortalAuth", "  Has error: " + hasError);
            logD("PortalAuth", "  Response length: " + responseHtml.length());

            if (hasSuccessMessage) {
                logD("PortalAuth", "Success: Found success message");
                return new UploadResult.Success();
            } else if (hasError) {
                logD("PortalAuth", "Failed: Found error message");
                return new UploadResult.Rejected(rejectionReason);
            } else if (hasForm && !hasFileInput) {
                logD("PortalAuth", "Success: File input disappeared and page reloaded");
                return new UploadResult.Success();
            } else if (visibleValidationMessages.isEmpty() && responseUrl.toLowerCase().contains("courseportal")) {
                logD("PortalAuth", "Success: Redirected to CoursePortal with no validation errors");
                return new UploadResult.Success();
            }

            logD("PortalAuth", "Upload result: FAILED");
            logD("PortalAuth", "=== UPLOAD END ===");
            return new UploadResult.Rejected(rejectionReason);
        } catch (Exception e) {
            logE("PortalAuth", "Upload error: " + e.getMessage(), e);
            if (e instanceof IOException) {
                return new UploadResult.NetworkError();
            } else {
                return new UploadResult.Error(e.getMessage() != null ? e.getMessage() : "Upload failed.");
            }
        }
    }

    public boolean isUploadSizeErrorRedirect(String responseUrl, String responseHtml) {
        try {
            HttpUrl parsed = HttpUrl.parse(responseUrl);
            if (parsed == null) return false;
            String path = parsed.encodedPath().toLowerCase();
            String aspxErrorPath = parsed.queryParameter("aspxerrorpath");
            if (aspxErrorPath == null) aspxErrorPath = "";
            aspxErrorPath = aspxErrorPath.toLowerCase();
            String normalizedUrl = responseUrl.toLowerCase();
            String normalizedHtml = responseHtml.toLowerCase();

            boolean isPortalUploadErrorPath = (
                (path.endsWith("/error.html") || normalizedUrl.contains("/error.html")) &&
                    (aspxErrorPath.contains("courseportalsubmitassignment.aspx") ||
                        normalizedUrl.contains("aspxerrorpath=%2fcourseportalsubmitassignment.aspx") ||
                        normalizedUrl.contains("aspxerrorpath=/courseportalsubmitassignment.aspx"))
            );

            if (isPortalUploadErrorPath) return true;

            boolean hasPortalSizeMessage = normalizedHtml.contains("maximum request length exceeded") ||
                normalizedHtml.contains("request entity too large") ||
                (normalizedHtml.contains("file") && normalizedHtml.contains("too large"));

            return hasPortalSizeMessage;
        } catch (IllegalArgumentException ex) {
            ErrorReporter.logError("PortalRepository#isUploadSizeErrorRedirect", ex);
        }
        return false;
    }
}
