package com.assignly.service;

import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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

    public record StudentProfile(String name, String rollNo, String program) {}

    /** All data parsed from Dashboard.aspx */
    public record DashboardData(
            String photoUrl,
            Map<String, String> studentInfo,       // ordered: Name, Father Name, Roll No, etc.
            Map<String, Double> attendanceOverall   // courseName -> percentage
    ) {}


    // ---------- Constants ----------
    private static final String BASE_URL = "https://sis.cuiatd.edu.pk";
    private static final String BASE_HOST = "sis.cuiatd.edu.pk";
    private static final String LOGIN_URL = BASE_URL + "/Login.aspx";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final DateTimeFormatter PORTAL_DEADLINE_FMT =
            DateTimeFormatter.ofPattern("MMM dd ,yyyy HH:mm", Locale.US);
    private static final ZoneId PORTAL_ZONE = ZoneId.systemDefault();

    // ---------- Session state ----------
    private final Map<String, Map<String, Cookie>> cookieStore = new ConcurrentHashMap<>();
    private volatile String currentStudentName;
    private volatile String currentStudentPhotoUrl;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .cookieJar(new CookieJar() {
                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    Map<String, Cookie> host = cookieStore.computeIfAbsent(url.host(), k -> new ConcurrentHashMap<>());
                    for (Cookie c : cookies) host.put(c.name(), c);
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
            .build();

    // ---------- Public accessors ----------
    public String getPortalBaseUrl()   { return BASE_URL; }
    public String getPortalLoginUrl()  { return LOGIN_URL; }
    public String getCurrentStudentName() { return currentStudentName; }
    public String getCurrentStudentPhotoUrl() { return currentStudentPhotoUrl; }

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
                    info.put(key, value);
                }
            }
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
        // Try to find attendance-related text patterns in spans/tds
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

        return new DashboardData(photoUrl, info, attendance);
    }

    /** Download student photo as raw bytes (for JavaFX Image) */
    public byte[] fetchPhotoBytes(String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) return null;
        try {
            Request request = new Request.Builder()
                    .url(photoUrl)
                    .header("Referer", BASE_URL + "/Dashboard.aspx")
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
        // Find the select element by partial ID match
        Element select = null;
        for (Element el : doc.select("select")) {
            String id = el.attr("id").toLowerCase();
            String name = el.attr("name").toLowerCase();
            if (id.contains(dropdownIdFragment.toLowerCase()) || name.contains(dropdownIdFragment.toLowerCase())) {
                select = el;
                break;
            }
        }
        if (select == null) return options;
        for (Element opt : select.select("option")) {
            String val = opt.attr("value");
            String text = opt.text().trim();
            if (!val.isEmpty() && !text.toLowerCase().startsWith("select") && !text.startsWith("--")) {
                options.add(new String[]{val, text});
            }
        }
        return options;
    }

    /** Get the name attribute of a dropdown by partial ID */
    public String findDropdownName(String html, String dropdownIdFragment) {
        if (html == null) return null;
        Document doc = Jsoup.parse(html);
        for (Element el : doc.select("select")) {
            String id = el.attr("id").toLowerCase();
            String name = el.attr("name");
            if (id.contains(dropdownIdFragment.toLowerCase()) || name.toLowerCase().contains(dropdownIdFragment.toLowerCase())) {
                return name;
            }
        }
        return null;
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

            // 2. Build POST body with all hidden fields + dropdown value
            FormBody.Builder formBuilder = new FormBody.Builder();

            for (Element input : form.select("input[type=hidden]")) {
                String name = input.attr("name");
                String value = input.attr("value");
                if (!name.isEmpty()) {
                    formBuilder.add(name, value);
                }
            }

            // Add all other form fields with their current values
            for (Element el : form.select("select")) {
                String name = el.attr("name");
                if (name.equals(dropdownName)) {
                    formBuilder.add(name, selectedValue);
                } else {
                    Element selected = el.select("option[selected]").first();
                    String val = selected != null ? selected.attr("value") :
                            (el.select("option").first() != null ? el.select("option").first().attr("value") : "");
                    formBuilder.add(name, val);
                }
            }

            // ASP.NET event target for the dropdown (autopostback)
            formBuilder.add("__EVENTTARGET", dropdownName);
            formBuilder.add("__EVENTARGUMENT", "");

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

    // ---------- Helpers ported from Kotlin ----------
    private String normalizeToken(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private void clearSessionState() {
        cookieStore.clear();
        currentStudentName = null;
        currentStudentPhotoUrl = null;
    }

    private boolean hasSessionCookiesForHost(String host) {
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
}
