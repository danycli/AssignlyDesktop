package com.assignly.view;

import com.assignly.service.PortalRepository;
import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.chart.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * DashboardTabView - Modern, action-oriented student dashboard.
 * Preserves Cyprus Green (#004643) and Sand (#f0edec) branding.
 */
public class DashboardTabView {
    private final VBox root = new VBox();
    private final AppContext context;
    private StackPane contentPane;

    // Helper records for parsing
    private record ChallanInfo(String description, String target, String argument) {}
    private record FeeHistoryTable(String title, List<String> headers, List<List<String>> data) {}

    private List<ChallanInfo> parseChallans(String html) {
        List<ChallanInfo> list = new ArrayList<>();
        if (html == null || html.isBlank()) return list;
        try {
            Document doc = Jsoup.parse(html);
            for (Element row : doc.select("tr")) {
                Element link = row.selectFirst("a[href^=javascript:__doPostBack]");
                if (link != null) {
                    String linkText = link.text().toLowerCase();
                    if (linkText.contains("print") || linkText.contains("download") || linkText.contains("view")) {
                        StringBuilder descBuilder = new StringBuilder();
                        for (Element cell : row.select("td, th")) {
                            if (!cell.equals(link.parent())) {
                                String text = cell.text().trim();
                                if (!text.isEmpty()) descBuilder.append(text).append(" - ");
                            }
                        }
                        String description = descBuilder.toString();
                        if (description.endsWith(" - ")) description = description.substring(0, description.length() - 3);
                        list.add(new ChallanInfo(description, "", ""));
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    private List<FeeHistoryTable> parseFeeHistory(String html) {
        List<FeeHistoryTable> historyTables = new ArrayList<>();
        if (html == null || html.isBlank()) return historyTables;
        try {
            Document doc = Jsoup.parse(html);
            int tableIndex = 0;
            String[] expectedTitles = {
                "Semester Fee Current",
                "Boarding Fee Current",
                "Other Miscellaneous Fee Current"
            };
            for (Element table : doc.select("table")) {
                Elements rows = table.select("tr");
                if (rows.size() < 2) continue;
                Elements ths = rows.first().select("th");
                if (ths.isEmpty()) continue;
                
                String titleText = "Fee History";
                Element prev = table.previousElementSibling();
                while (prev != null) {
                    String txt = prev.text().trim();
                    if (!txt.isEmpty()) {
                        titleText = txt;
                        break;
                     }
                     prev = prev.previousElementSibling();
                }
                if (ths.size() == 1 && ths.first().hasAttr("colspan")) {
                    titleText = ths.first().text().trim();
                    rows.remove(0);
                    if (rows.isEmpty()) continue;
                    ths = rows.first().select("th");
                    if (ths.isEmpty()) ths = rows.first().select("td");
                }
                if (titleText.contains(":")) {
                    titleText = titleText.substring(titleText.indexOf(":") + 1).trim();
                }
                if (ths.size() > 1) {
                    List<String> headers = new ArrayList<>();
                    for (Element th : ths) headers.add(th.text().trim());
                    List<List<String>> data = new ArrayList<>();
                    for (int i = 1; i < rows.size(); i++) {
                        Elements cells = rows.get(i).select("td");
                        if (cells.isEmpty()) continue;
                        List<String> rowData = new ArrayList<>();
                        for (Element cell : cells) rowData.add(cell.text().trim());
                        if (rowData.size() <= headers.size() && !rowData.isEmpty()) {
                            data.add(rowData);
                        }
                    }
                    if (!data.isEmpty()) {
                        String finalTitle = titleText;
                        if (tableIndex < expectedTitles.length) {
                            finalTitle = expectedTitles[tableIndex];
                        }
                        historyTables.add(new FeeHistoryTable(finalTitle, headers, data));
                        tableIndex++;
                    }
                }
            }
        } catch (Exception ignored) {}
        return historyTables;
    }

    public DashboardTabView(AppContext context) {
        this(context, false);
    }

    public DashboardTabView(AppContext context, boolean forceRefresh) {
        this.context = context;
        buildShell();
        loadDashboard(forceRefresh);
    }

    private void buildShell() {
        root.setFillWidth(true);
        root.setStyle("-fx-background-color: transparent;");

        // Simple minimal header
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(20, 28, 10, 28));

        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerRow.getChildren().addAll(title, spacer);
        root.getChildren().add(headerRow);

        contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        root.getChildren().add(contentPane);
    }

    private void buildLoading() {
        contentPane.getChildren().clear();
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(com.assignly.util.ShimmerBuilder.buildDashboardShimmer());
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        contentPane.getChildren().add(sp);
    }

    private void loadDashboard(boolean forceRefresh) {
        buildLoading();
        new Thread(() -> {
            String html = null;
            boolean isOffline = false;

            if (!forceRefresh) {
                html = context.dataCacheService().getCachedHtml("CoursePortal.aspx").orElse(null);
            }

            if (html == null) {
                html = context.fetchAndCacheHtml("CoursePortal.aspx");
                if (html == null) {
                    html = context.dataCacheService().getCachedHtml("CoursePortal.aspx").orElse(null);
                    isOffline = true;
                }
            }

            PortalRepository.DashboardData data = null;
            if (html != null) {
                data = context.portalRepository().parseDashboard(html);
            }

            // Fetch GPA History
            String resultHtml = null;
            if (!forceRefresh) {
                resultHtml = context.dataCacheService().getCachedHtml("StudentResultCard.aspx").orElse(null);
            }
            if (resultHtml == null && !isOffline) {
                resultHtml = context.fetchAndCacheHtml("StudentResultCard.aspx");
            }
            if (resultHtml == null) {
                resultHtml = context.dataCacheService().getCachedHtml("StudentResultCard.aspx").orElse(null);
            }
            List<PortalRepository.GpaHistoryData> gpaHistory = context.portalRepository().parseGpaHistory(resultHtml);
            final List<PortalRepository.GpaHistoryData> finalGpaHistory = gpaHistory;
            Map<String, String> courseNames = context.portalRepository().parseCourseNames(resultHtml);
            final Map<String, String> finalCourseNames = courseNames;


            // Fetch Scholarships
            String scholarshipHtml = null;
            if (!forceRefresh) {
                scholarshipHtml = context.dataCacheService().getCachedHtml("scholarship/ViewScholarshipStatuse.aspx").orElse(null);
            }
            if (scholarshipHtml == null && !isOffline) {
                scholarshipHtml = context.fetchAndCacheHtml("scholarship/ViewScholarshipStatuse.aspx");
            }
            if (scholarshipHtml == null) {
                scholarshipHtml = context.dataCacheService().getCachedHtml("scholarship/ViewScholarshipStatuse.aspx").orElse(null);
            }
            List<PortalRepository.ScholarshipTable> scholarships = context.portalRepository().parseScholarships(scholarshipHtml);
            final List<PortalRepository.ScholarshipTable> finalScholarships = scholarships;

            // Fetch Fee Challans
            String feeChallansHtml = null;
            if (!forceRefresh) {
                feeChallansHtml = context.dataCacheService().getCachedHtml("FeeChallans.aspx").orElse(null);
            }
            if (feeChallansHtml == null && !isOffline) {
                feeChallansHtml = context.fetchAndCacheHtml("FeeChallans.aspx");
            }
            if (feeChallansHtml == null) {
                feeChallansHtml = context.dataCacheService().getCachedHtml("FeeChallans.aspx").orElse(null);
            }
            List<ChallanInfo> challans = parseChallans(feeChallansHtml);
            final List<ChallanInfo> finalChallans = challans;

            // Fetch Fee History
            String feeHistoryHtml = null;
            if (!forceRefresh) {
                feeHistoryHtml = context.dataCacheService().getCachedHtml("FeeHistorySFMS.aspx").orElse(null);
            }
            if (feeHistoryHtml == null && !isOffline) {
                feeHistoryHtml = context.fetchAndCacheHtml("FeeHistorySFMS.aspx");
            }
            if (feeHistoryHtml == null) {
                feeHistoryHtml = context.dataCacheService().getCachedHtml("FeeHistorySFMS.aspx").orElse(null);
            }
            List<FeeHistoryTable> feeHistory = parseFeeHistory(feeHistoryHtml);
            final List<FeeHistoryTable> finalFeeHistory = feeHistory;

            byte[] photoBytes = null;
            if (data != null && data.photoUrl() != null) {
                photoBytes = context.portalRepository().fetchPhotoBytes(data.photoUrl());
            }
            final byte[] photo = photoBytes;
            final boolean finalOffline = isOffline;
            final PortalRepository.DashboardData finalData = data;

            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                if (finalData == null) {
                    Label err = new Label("Could not load dashboard. No offline data available.");
                    err.setStyle("-fx-text-fill: -color-text-muted; -fx-padding: 40; -fx-font-size: 14px;");
                    contentPane.getChildren().add(err);
                    return;
                }
                buildDashboard(finalData, photo, finalOffline, finalGpaHistory, finalCourseNames, finalScholarships, finalChallans, finalFeeHistory);
            });
        }).start();
    }

    private void buildDashboard(
            PortalRepository.DashboardData data,
            byte[] photoBytes,
            boolean isOffline,
            List<PortalRepository.GpaHistoryData> gpaHistory,
            Map<String, String> courseNames,
            List<PortalRepository.ScholarshipTable> scholarships,
            List<ChallanInfo> challans,
            List<FeeHistoryTable> feeHistory) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(10, 28, 28, 28));
        content.setFillWidth(true);
        content.setStyle("-fx-background-color: transparent;");

        if (isOffline) {
            content.getChildren().add(buildOfflineBanner());
        }

        // 1. WELCOME AREA
        content.getChildren().add(buildWelcomeArea(data, photoBytes, gpaHistory));

        // 2. QUICK STATISTICS (3x2 Grid or 6-Column Layout depending on width, we use a neat 3x2 Grid)
        content.getChildren().add(buildStatsGrid(data, gpaHistory, scholarships, challans));

        // 3. ACADEMIC PROGRESS & ATTENDANCE OVERVIEW (Side-by-Side Charts)
        HBox analyticsRow = new HBox(20);
        analyticsRow.setFillHeight(true);
        VBox gpaCard = buildGpaChartCard(gpaHistory);
        VBox attendanceCard = buildAttendanceOverviewCard(data.attendanceOverall(), courseNames);
        HBox.setHgrow(gpaCard, Priority.ALWAYS);
        HBox.setHgrow(attendanceCard, Priority.ALWAYS);
        analyticsRow.getChildren().addAll(gpaCard, attendanceCard);
        content.getChildren().add(analyticsRow);

        // Responsive ScrollPane wrapper
        content.setMinWidth(900);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        contentPane.getChildren().add(scroll);
    }

    // ==================== SECTION 1: WELCOME AREA ====================
    private String getInfoValueSafe(Map<String, String> info, String... keyKeywords) {
        if (info == null || info.isEmpty()) return "";
        for (String kw : keyKeywords) {
            String kwLower = kw.toLowerCase();
            for (Map.Entry<String, String> entry : info.entrySet()) {
                String keyLower = entry.getKey().toLowerCase();
                if (keyLower.contains(kwLower)) {
                    return entry.getValue();
                }
            }
        }
        return "";
    }

    private HBox buildWelcomeArea(PortalRepository.DashboardData data, byte[] photoBytes, List<PortalRepository.GpaHistoryData> gpaHistory) {
        HBox card = new HBox(20);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 16;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 70, 67, 0.03), 8, 0, 0, 4);"
        );

        // Left welcome text
        VBox textCol = new VBox(4);
        String name = getInfoValueSafe(data.studentInfo(), "name");
        if (name.isEmpty()) name = "Student";
        String firstName = name.split("\\s+")[0];
        
        String greetingWord = "Welcome Back";
        Label greeting = new Label(greetingWord + ", " + firstName);
        greeting.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");

        String regNo = getInfoValueSafe(data.studentInfo(), "registration no", "roll no", "reg no", "registration number");
        String program = getInfoValueSafe(data.studentInfo(), "program", "degree");
        
        String semester = getInfoValueSafe(data.studentInfo(), "semester");
        if (semester.isEmpty() && gpaHistory != null && !gpaHistory.isEmpty()) {
            semester = String.valueOf(gpaHistory.size());
        }
        
        if (semester.isEmpty()) {
            semester = "Active Semester";
        } else if (semester.matches("\\d+")) {
            semester = "Semester " + semester;
        }

        String campus = "Campus";
        String rUpper = regNo.toUpperCase();
        if (rUpper.contains("ATD")) campus = "Abbottabad Campus";
        else if (rUpper.contains("ISB")) campus = "Islamabad Campus";
        else if (rUpper.contains("LHR")) campus = "Lahore Campus";
        else if (rUpper.contains("WAH")) campus = "Wah Campus";
        else if (rUpper.contains("SWL")) campus = "Sahiwal Campus";
        else if (rUpper.contains("VHR")) campus = "Vehari Campus";
        
        Label subtext = new Label(semester + "  •  " + program + "  •  " + campus);
        subtext.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500;");

        // Small quick summary metrics inline
        double currentCgpa = 0.0;
        boolean hasGpa = false;
        if (gpaHistory != null && !gpaHistory.isEmpty()) {
            if (gpaHistory.size() >= 2) {
                currentCgpa = gpaHistory.get(gpaHistory.size() - 2).cgpa();
                if (currentCgpa < 0) {
                    currentCgpa = gpaHistory.get(gpaHistory.size() - 1).cgpa();
                }
            } else {
                currentCgpa = gpaHistory.get(gpaHistory.size() - 1).cgpa();
            }
            hasGpa = currentCgpa > 0;
        }
        String gpaText = hasGpa ? String.format("%.2f", currentCgpa) : "N/A";
        
        String coursesStr = getInfoValueSafe(data.studentInfo(), "registered courses");
        if (coursesStr.isEmpty()) {
            coursesStr = String.valueOf(data.attendanceOverall().size());
        }
        
        Label summary = new Label(String.format("Academic Standing: Active  |  CGPA: %s  |  Courses Registered: %s", 
            gpaText, coursesStr));
        summary.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-accent; -fx-font-weight: 600; -fx-padding: 8 0 0 0;");

        HBox actionsRow = new HBox(12);
        actionsRow.setPadding(new Insets(12, 0, 0, 0));
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        Button viewResultsBtn = new Button("📊 View Results");
        viewResultsBtn.setCursor(javafx.scene.Cursor.HAND);
        viewResultsBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16; -fx-background-radius: 6;");
        viewResultsBtn.setOnAction(e -> context.navigateTo("result"));

        Button openPortalBtn = new Button("🌐 Open Course Portal");
        openPortalBtn.setCursor(javafx.scene.Cursor.HAND);
        openPortalBtn.setStyle("-fx-background-color: -color-bg-main; -fx-text-fill: -color-text-main; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16;");
        openPortalBtn.setOnAction(e -> context.navigateTo("portal"));

        actionsRow.getChildren().addAll(viewResultsBtn, openPortalBtn);

        textCol.getChildren().addAll(greeting, subtext, summary, actionsRow);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        card.getChildren().addAll(textCol);
        return card;
    }

    // ==================== SECTION 2: QUICK STATISTICS ====================
    private GridPane buildStatsGrid(
            PortalRepository.DashboardData data,
            List<PortalRepository.GpaHistoryData> gpaHistory,
            List<PortalRepository.ScholarshipTable> scholarships,
            List<ChallanInfo> challans) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        // Double column sizing constraints
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(33.33);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(33.33);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(33.33);
        grid.getColumnConstraints().addAll(col1, col2, col3);

        // Gather statistics
        double cgpa = 0.0;
        double prevCgpa = 0.0;
        if (gpaHistory != null && !gpaHistory.isEmpty()) {
            if (gpaHistory.size() >= 2) {
                cgpa = gpaHistory.get(gpaHistory.size() - 2).cgpa();
                if (cgpa < 0) {
                    cgpa = gpaHistory.get(gpaHistory.size() - 1).cgpa();
                    prevCgpa = gpaHistory.size() >= 2 ? gpaHistory.get(gpaHistory.size() - 2).cgpa() : 0.0;
                } else {
                    prevCgpa = gpaHistory.size() >= 3 ? gpaHistory.get(gpaHistory.size() - 3).cgpa() : 0.0;
                }
            } else {
                cgpa = gpaHistory.get(gpaHistory.size() - 1).cgpa();
            }
        }
        String cgpaStr = cgpa > 0 ? String.format("%.2f", cgpa) : "N/A";

        String coursesStr = getInfoValueSafe(data.studentInfo(), "registered courses");
        if (coursesStr.isEmpty()) {
            coursesStr = String.valueOf(data.attendanceOverall().size());
        }
        int coursesCount = 0;
        try {
            coursesCount = Integer.parseInt(coursesStr);
        } catch (NumberFormatException e) {
            coursesCount = data.attendanceOverall().size();
        }

        double avgAttendance = 0.0;
        if (!data.attendanceOverall().isEmpty()) {
            double sum = 0.0;
            for (double val : data.attendanceOverall().values()) sum += val;
            avgAttendance = sum / data.attendanceOverall().size();
        }
        String attendanceStr = avgAttendance > 0 ? String.format("%.1f%%", avgAttendance) : "N/A";

        // Estimate Scholarship & Fee statuses based on existing context
        String scholarshipStatus = "None";
        double totalScholarship = 0.0;
        if (scholarships != null && !scholarships.isEmpty()) {
            scholarshipStatus = "Active";
            for (PortalRepository.ScholarshipTable t : scholarships) {
                int totalIdx = -1;
                for (int i = 0; i < t.headers().size(); i++) {
                    String h = t.headers().get(i).toLowerCase();
                    if (h.contains("total") || h.contains("amount")) {
                        totalIdx = i;
                        break;
                    }
                }
                
                int nameIdx = -1;
                for (int i = 0; i < t.headers().size(); i++) {
                    String h = t.headers().get(i).toLowerCase();
                    if (h.contains("scholarship") || h.contains("name") || h.contains("type")) {
                        nameIdx = i;
                        break;
                    }
                }

                if (!t.data().isEmpty()) {
                    List<String> latestRow = t.data().get(t.data().size() - 1);
                    if (nameIdx >= 0 && nameIdx < latestRow.size()) {
                        String nameVal = latestRow.get(nameIdx).trim();
                        if (!nameVal.isEmpty()) {
                            scholarshipStatus = nameVal.split("\\s+")[0];
                        }
                    }
                    
                    if (totalIdx >= 0) {
                        for (List<String> row : t.data()) {
                            if (totalIdx < row.size()) {
                                try {
                                    String clean = row.get(totalIdx).replaceAll("[^0-9.]", "");
                                    if (!clean.isEmpty()) {
                                        totalScholarship += Double.parseDouble(clean);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
        }
        
        String feeBalance = "Paid";
        double feeAmount = 0.0;
        if (challans != null && !challans.isEmpty()) {
            String desc = challans.get(0).description();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{4,}").matcher(desc);
            if (m.find()) {
                feeAmount = Double.parseDouble(m.group());
                feeBalance = String.format("Rs %,.0f", feeAmount);
            } else {
                feeBalance = "Unpaid";
            }
        }
        
        double currentCredits = 0.0;
        if (gpaHistory != null && !gpaHistory.isEmpty()) {
            currentCredits = gpaHistory.get(gpaHistory.size() - 1).creditHours();
        }
        if (currentCredits <= 0.0) {
            currentCredits = coursesCount * 3.0;
        }
        String creditHours = String.format("%.0f", currentCredits);

        // CGPA Trend
        String cgpaTrend = "GPA status stable";
        String cgpaTrendType = "neutral";
        if (cgpa > 0 && prevCgpa > 0) {
            double diff = cgpa - prevCgpa;
            if (diff > 0) {
                cgpaTrend = String.format("▲ +%.2f", diff);
                cgpaTrendType = "positive";
            } else if (diff < 0) {
                cgpaTrend = String.format("▼ %.2f", diff);
                cgpaTrendType = "negative";
            } else {
                cgpaTrend = "No change";
                cgpaTrendType = "neutral";
            }
        }

        // Attendance Trend
        String attendanceTrend = "No attendance";
        String attendanceTrendType = "neutral";
        if (avgAttendance > 0) {
            double diff = avgAttendance - 75.0;
            if (diff >= 0) {
                attendanceTrend = String.format("▲ +%.1f%% above min", diff);
                attendanceTrendType = "positive";
            } else {
                attendanceTrend = String.format("▼ %.1f%% below min", Math.abs(diff));
                attendanceTrendType = "negative";
            }
        }

        // Scholarship Trend
        String scholarshipTrend = "No active grant";
        String scholarshipTrendType = "neutral";
        if (totalScholarship > 0) {
            scholarshipTrend = String.format("Rs %,.0f total", totalScholarship);
            scholarshipTrendType = "positive";
        } else if (!"None".equalsIgnoreCase(scholarshipStatus)) {
            scholarshipTrend = "Active status";
            scholarshipTrendType = "positive";
        }

        // Fee Trend
        String feeTrend = "✓ Up to date";
        String feeTrendType = "positive";
        if (feeAmount > 0) {
            feeTrend = "⚠ Unpaid challan";
            feeTrendType = "negative";
        } else if ("Unpaid".equals(feeBalance)) {
            feeTrend = "⚠ Unpaid challan";
            feeTrendType = "negative";
        }

        VBox cgpaCard = buildStatCard("📊", "Current CGPA", cgpaStr, "Cumulative Grade Points", cgpaTrend, cgpaTrendType);
        VBox coursesCard = buildStatCard("📚", "Registered Courses", coursesStr, "Active Course Modules", "Semester load", "neutral");
        VBox attendanceCard = buildStatCard("📅", "Attendance Avg", attendanceStr, "Overall Lecture Presence", attendanceTrend, attendanceTrendType);
        VBox scholarshipCard = buildStatCard("🎓", "Scholarship", scholarshipStatus, "Console Record Status", scholarshipTrend, scholarshipTrendType);
        VBox feeCard = buildStatCard("💰", "Outstanding Fee", feeBalance, "Portal Fee Balance", feeTrend, feeTrendType);
        VBox creditCard = buildStatCard("📈", "Credit Hours", creditHours + " Hrs", "Active Semester Credits", "Registered", "neutral");

        grid.add(cgpaCard, 0, 0);
        grid.add(coursesCard, 1, 0);
        grid.add(attendanceCard, 2, 0);
        grid.add(scholarshipCard, 0, 1);
        grid.add(feeCard, 1, 1);
        grid.add(creditCard, 2, 1);

        return grid;
    }

    private VBox buildStatCard(String emoji, String title, String value, String desc, String trendText, String trendType) {
        VBox card = new VBox(4);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-background-radius: 14;" +
            "-fx-border-radius: 14;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 70, 67, 0.02), 6, 0, 0, 3);"
        );

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(emoji);
        iconLbl.setStyle("-fx-font-size: 18px;");
        Label titleLbl = new Label(title.toUpperCase());
        titleLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted; -fx-letter-spacing: 0.5px;");
        topRow.getChildren().addAll(iconLbl, titleLbl);

        HBox valRow = new HBox(12);
        valRow.setAlignment(Pos.BASELINE_LEFT);
        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: -color-accent;");
        
        Label trendLbl = new Label(trendText);
        if ("positive".equals(trendType)) {
            trendLbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #059669; -fx-background-color: #d1fae5; -fx-padding: 2 8; -fx-background-radius: 10;");
        } else if ("negative".equals(trendType)) {
            trendLbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #dc2626; -fx-background-color: #fee2e2; -fx-padding: 2 8; -fx-background-radius: 10;");
        } else {
            trendLbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted; -fx-background-color: -color-bg-main; -fx-padding: 2 8; -fx-background-radius: 10;");
        }
        valRow.getChildren().addAll(valLbl, trendLbl);
        VBox.setMargin(valRow, new Insets(4, 0, 2, 0));

        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

        card.getChildren().addAll(topRow, valRow, descLbl);

        // Premium hover elevation via TranslateTransition
        card.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
            tt.setToY(-3);
            tt.play();
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-background-radius: 14;" +
                "-fx-border-radius: 14;" +
                "-fx-border-color: -color-accent;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0, 70, 67, 0.08), 12, 0, 0, 6);"
            );
        });
        card.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
            tt.setToY(0);
            tt.play();
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-background-radius: 14;" +
                "-fx-border-radius: 14;" +
                "-fx-border-color: -color-border;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0, 70, 67, 0.02), 6, 0, 0, 3);"
            );
        });

        return card;
    }


    // ==================== SECTION 5: ACADEMIC PROGRESS ====================
    private String abbreviateSemester(String title) {
        if (title == null) return "Sem";
        String t = title.toUpperCase().trim();
        String season = "";
        if (t.contains("SPRING") || t.contains("SP")) season = "SP";
        else if (t.contains("FALL") || t.contains("FA")) season = "FA";
        else if (t.contains("WINTER") || t.contains("WI") || t.contains("WS")) season = "WS";
        
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d{2,4})");
        java.util.regex.Matcher m = p.matcher(t);
        String year = "";
        if (m.find()) {
            year = m.group(1);
            if (year.length() == 4) {
                year = year.substring(2);
            }
        }
        return season + year;
    }

    private VBox buildGpaChartCard(List<PortalRepository.GpaHistoryData> gpaHistory) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18));
        card.setMinHeight(280);
        card.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: -color-border; -fx-border-width: 1;");

        Label title = new Label("📈 Academic Standing & Progress");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -color-text-main;");

        List<PortalRepository.GpaHistoryData> filteredHistory = new ArrayList<>();
        if (gpaHistory != null) {
            for (PortalRepository.GpaHistoryData entry : gpaHistory) {
                // Exclude semesters that are incomplete/ungraded (e.g. current semester)
                if (entry.sgpa() > 0.0 || entry.cgpa() > 0.0) {
                    filteredHistory.add(entry);
                }
            }
        }

        if (filteredHistory.isEmpty()) {
            Label empty = new Label("No graded GPA history available yet.");
            empty.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px; -fx-padding: 30 0;");
            card.getChildren().addAll(title, empty);
            return card;
        }

        double minGpa = 4.0;
        for (PortalRepository.GpaHistoryData entry : filteredHistory) {
            if (entry.sgpa() > 0.0 && entry.sgpa() < minGpa) minGpa = entry.sgpa();
            if (entry.cgpa() > 0.0 && entry.cgpa() < minGpa) minGpa = entry.cgpa();
        }

        // Calculate dynamic lower bound, with a maximum limit of 2.5
        double lowerBound = Math.max(0.0, Math.floor(minGpa * 2) / 2.0 - 0.5);
        if (lowerBound > 2.5) {
            lowerBound = 2.5;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setStyle("-fx-tick-label-fill: -color-text-muted; -fx-font-size: 9px;");
        NumberAxis yAxis = new NumberAxis(lowerBound, 4.0, 0.5);
        yAxis.setStyle("-fx-tick-label-fill: -color-text-muted; -fx-font-size: 9px;");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setLegendVisible(true);
        lineChart.setCreateSymbols(true);
        lineChart.setMinHeight(180);
        lineChart.setMaxHeight(200);
        lineChart.setStyle("-fx-background-color:transparent;");

        XYChart.Series<String, Number> sgpa = new XYChart.Series<>();
        sgpa.setName("SGPA");
        XYChart.Series<String, Number> cgpa = new XYChart.Series<>();
        cgpa.setName("CGPA");

        for (PortalRepository.GpaHistoryData entry : filteredHistory) {
            String label = abbreviateSemester(entry.semesterTitle());
            sgpa.getData().add(new XYChart.Data<>(label, entry.sgpa()));
            cgpa.getData().add(new XYChart.Data<>(label, entry.cgpa()));
        }

        lineChart.getData().addAll(sgpa, cgpa);
        card.getChildren().addAll(title, lineChart);

        // Apply dynamic stroke formatting on thread layout
        Platform.runLater(() -> {
            try {
                javafx.scene.Node l1 = lineChart.lookup(".default-color0.chart-series-line");
                if (l1 != null) l1.setStyle("-fx-stroke: -color-accent; -fx-stroke-width: 2.5px;");
                javafx.scene.Node l2 = lineChart.lookup(".default-color1.chart-series-line");
                if (l2 != null) l2.setStyle("-fx-stroke: #14b8a6; -fx-stroke-width: 2.5px;");

                // Bind hover scale animations and exact value Tooltips to symbols
                for (XYChart.Series<String, Number> series : lineChart.getData()) {
                    for (XYChart.Data<String, Number> d : series.getData()) {
                        javafx.scene.Node symbol = d.getNode();
                        if (symbol != null) {
                            symbol.setOnMouseEntered(e -> {
                                symbol.setScaleX(1.4);
                                symbol.setScaleY(1.4);
                                symbol.setCursor(javafx.scene.Cursor.HAND);
                            });
                            symbol.setOnMouseExited(e -> {
                                symbol.setScaleX(1.0);
                                symbol.setScaleY(1.0);
                            });

                            Tooltip tt = new Tooltip(String.format("%s: %.2f", series.getName(), d.getYValue().doubleValue()));
                            tt.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: -color-bg-card; -fx-text-fill: -color-text-main; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");
                            Tooltip.install(symbol, tt);
                        }
                    }
                }
            } catch (Exception ignored) {}
        });

        return card;
    }

    // ==================== SECTION 6: ATTENDANCE OVERVIEW ====================
    private VBox buildAttendanceOverviewCard(Map<String, Double> attendanceData, Map<String, String> courseNames) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: -color-border; -fx-border-width: 1;");

        Label title = new Label("🎯 Course Attendance Overview");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -color-text-main;");

        if (attendanceData == null || attendanceData.isEmpty()) {
            Label empty = new Label("No attendance data available.");
            empty.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px; -fx-padding: 30 0;");
            card.getChildren().addAll(title, empty);
            return card;
        }

        VBox courseList = new VBox(8);
        for (Map.Entry<String, Double> entry : attendanceData.entrySet()) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            String rawCode = entry.getKey();
            String displayName = rawCode;
            if (courseNames != null) {
                String normCode = rawCode.trim().replaceAll("\\s+|-", "").toUpperCase();
                String resolvedName = null;
                for (Map.Entry<String, String> nameEntry : courseNames.entrySet()) {
                    String normMapKey = nameEntry.getKey().trim().replaceAll("\\s+|-", "").toUpperCase();
                    if (normMapKey.equals(normCode)) {
                        resolvedName = nameEntry.getValue();
                        break;
                    }
                }
                if (resolvedName != null) {
                    displayName = rawCode + " • " + resolvedName;
                }
            }

            Label name = new Label(displayName);
            name.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-pref-width: 220; -fx-text-fill: -color-text-main;");
            name.setTooltip(new Tooltip(displayName));

            ProgressBar pb = new ProgressBar(entry.getValue() / 100.0);
            pb.setPrefWidth(120);
            pb.setPrefHeight(6);
            pb.setMinHeight(6);
            pb.setMaxHeight(6);
            HBox.setHgrow(pb, Priority.ALWAYS);
            if (entry.getValue() < 75.0) {
                pb.setStyle("-fx-accent: #dc2626; -fx-pref-height: 6px; -fx-min-height: 6px; -fx-max-height: 6px;"); // rose-red
            } else {
                pb.setStyle("-fx-accent: -color-accent; -fx-pref-height: 6px; -fx-min-height: 6px; -fx-max-height: 6px;"); // Cyprus green
            }

            Label val = new Label(String.format("%.0f%%", entry.getValue()));
            val.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + (entry.getValue() < 75.0 ? "#dc2626;" : "#059669;"));

            row.getChildren().addAll(name, pb, val);
            courseList.getChildren().add(row);
        }

        card.getChildren().addAll(title, courseList);
        return card;
    }

    // ==================== SECTION 7: FINANCIAL OVERVIEW ====================
    private VBox buildFinancialOverviewCard(
            List<PortalRepository.ScholarshipTable> scholarships,
            List<ChallanInfo> challans,
            List<FeeHistoryTable> history) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: -color-border; -fx-border-width: 1;");

        Label title = new Label("💰 Financial Overview");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -color-text-main;");

        HBox row = new HBox(16);
        row.setFillHeight(true);
        row.setPadding(new Insets(4, 0, 0, 0));

        // 1. Scholarship Status
        String shValText = "No active scholarships";
        String shBadgeText = "Inactive";
        String shBadgeType = "neutral";
        if (scholarships != null && !scholarships.isEmpty()) {
            PortalRepository.ScholarshipTable table = scholarships.get(0);
            if (!table.data().isEmpty()) {
                List<String> headers = table.headers();
                int nameIdx = -1;
                int amtIdx = -1;
                for (int i = 0; i < headers.size(); i++) {
                    String h = headers.get(i).toLowerCase();
                    if (h.contains("scholarship") || h.contains("name") || h.contains("type")) {
                        if (nameIdx == -1) nameIdx = i;
                    }
                    if (h.contains("amount") || h.contains("total") || h.contains("fee")) {
                        if (amtIdx == -1) amtIdx = i;
                    }
                }
                if (nameIdx == -1) nameIdx = 1;
                if (amtIdx == -1) amtIdx = 4;

                List<String> rRow = table.data().get(0);
                String name = rRow.size() > nameIdx ? rRow.get(nameIdx) : "Scholarship";
                String amount = rRow.size() > amtIdx ? rRow.get(amtIdx) : "";
                
                if (!amount.isBlank()) {
                    try {
                        String clean = amount.replaceAll("[^0-9.]", "");
                        if (!clean.isEmpty()) {
                            double val = Double.parseDouble(clean);
                            amount = String.format("Rs %,.0f", val);
                        }
                    } catch (Exception ignored) {}
                }
                
                shValText = name + (amount.isBlank() ? "" : " - " + amount);
                shBadgeText = "Active";
                shBadgeType = "positive";
            }
        }
        VBox shCard = buildFinancialMiniCard("🎓 SCHOLARSHIP ASSIST", shValText, shBadgeText, shBadgeType);

        // 2. Outstanding Balance
        String feeValText = "0.00 Rs";
        String feeBadgeText = "Paid";
        String feeBadgeType = "positive";
        if (challans != null && !challans.isEmpty()) {
            String desc = challans.get(0).description();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{4,}").matcher(desc);
            if (m.find()) {
                feeValText = "Rs " + String.format("%,.0f", Double.parseDouble(m.group()));
                feeBadgeText = "Unpaid";
                feeBadgeType = "negative";
            } else {
                feeValText = "Unpaid Challan";
                feeBadgeText = "Unpaid";
                feeBadgeType = "negative";
            }
        }
        VBox feeCard = buildFinancialMiniCard("💰 OUTSTANDING BALANCE", feeValText, feeBadgeText, feeBadgeType);

        // 3. Last Transaction
        String txSessionText = "No history";
        String txBadgeText = "N/A";
        String txBadgeType = "neutral";
        if (history != null && !history.isEmpty()) {
            for (FeeHistoryTable table : history) {
                List<String> headers = table.headers();
                int sessIdx = -1;
                int paidIdx = -1;
                for (int i = 0; i < headers.size(); i++) {
                    String h = headers.get(i).toLowerCase();
                    if (h.contains("session")) sessIdx = i;
                    if (h.contains("dues paid") || h.contains("fee paid") || h.contains("paid")) paidIdx = i;
                }
                
                if (sessIdx == -1) sessIdx = 1;
                if (paidIdx == -1) paidIdx = 7;

                if (!table.data().isEmpty()) {
                    List<String> latestRow = table.data().get(table.data().size() - 1);
                    String session = latestRow.size() > sessIdx ? latestRow.get(sessIdx).trim() : "N/A";
                    String amount = latestRow.size() > paidIdx ? latestRow.get(paidIdx).trim() : "";
                    
                    if (!amount.isEmpty()) {
                        try {
                            String clean = amount.replaceAll("[^0-9.]", "");
                            if (!clean.isEmpty()) {
                                double val = Double.parseDouble(clean);
                                amount = String.format("Rs %,.0f", val);
                                txBadgeText = "Paid";
                                txBadgeType = "positive";
                            } else {
                                amount = "Unpaid";
                                txBadgeText = "Unpaid";
                                txBadgeType = "negative";
                            }
                        } catch (Exception ignored) {
                            txBadgeText = "Paid";
                            txBadgeType = "positive";
                        }
                    }
                    txSessionText = session + (amount.isEmpty() ? "" : " - " + amount);
                    break;
                }
            }
        }
        VBox txCard = buildFinancialMiniCard("📄 LAST TRANSACTION", txSessionText, txBadgeText, txBadgeType);

        HBox.setHgrow(shCard, Priority.ALWAYS);
        HBox.setHgrow(feeCard, Priority.ALWAYS);
        HBox.setHgrow(txCard, Priority.ALWAYS);
        row.getChildren().addAll(shCard, feeCard, txCard);

        card.getChildren().addAll(title, row);
        return card;
    }

    private VBox buildFinancialMiniCard(String title, String val, String badgeText, String badgeType) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle(
            "-fx-background-color: -color-bg-main;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;"
        );
        
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted; -fx-letter-spacing: 0.3px;");
        
        HBox valRow = new HBox(8);
        valRow.setAlignment(Pos.CENTER_LEFT);
        
        Label valLbl = new Label(val);
        valLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        valLbl.setWrapText(true);
        HBox.setHgrow(valLbl, Priority.ALWAYS);
        
        Label badge = new Label(badgeText);
        if ("positive".equals(badgeType)) {
            badge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #059669; -fx-background-color: #d1fae5; -fx-padding: 2 8; -fx-background-radius: 8;");
        } else if ("negative".equals(badgeType)) {
            badge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #dc2626; -fx-background-color: #fee2e2; -fx-padding: 2 8; -fx-background-radius: 8;");
        } else {
            badge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted; -fx-background-color: -color-border; -fx-padding: 2 8; -fx-background-radius: 8;");
        }
        
        valRow.getChildren().addAll(valLbl, badge);
        
        card.getChildren().addAll(titleLbl, valRow);
        return card;
    }



    private HBox buildOfflineBanner() {
        HBox banner = new HBox(8);
        banner.setAlignment(Pos.CENTER);
        banner.setPadding(new Insets(8, 16, 8, 16));
        banner.setStyle("-fx-background-color:#FEF2F2;-fx-border-color:#FCA5A5;-fx-border-width:0 0 1 0;");
        
        Label icon = new Label("⚠");
        icon.setStyle("-fx-text-fill:#DC2626;-fx-font-size:14px;");
        Label text = new Label("Offline Mode: Displaying previously loaded data.");
        text.setStyle("-fx-text-fill:#991B1B;-fx-font-size:12px;-fx-font-weight:bold;");
        
        banner.getChildren().addAll(icon, text);
        return banner;
    }

    public VBox getRoot() { return root; }
}
