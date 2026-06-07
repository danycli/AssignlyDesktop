package com.assignly.view;

import com.assignly.util.AppContext;
import com.assignly.util.ErrorReporter;
import com.assignly.service.PortalRepository;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Clean multi-tab view for all the Course Portal sections, mirroring the CoursesTabView design.
 * Features beautifully civilized, native parsed grids for MCQ and Subjective tests, and scorecard details.
 */
public class CoursePortalTabView {
    private final VBox root = new VBox();
    private final AppContext context;
    private StackPane contentPane;
    private HBox tabBar;
    private String activeTab = "";
    private final List<Button> uploadButtons = new ArrayList<>();
    private final java.util.function.Consumer<Boolean> connectivityListener = this::onConnectivityChanged;
    private String pendingAssignmentsCacheHtml;

    // Redesign Fields & State
    private VBox overviewContainer;
    private String ccSearchQuery = "";
    private String ccSortOrder = "Date"; // Date or Name
    private boolean ccGridMode = true; // true for Grid, false for List
    private String currentCourseId;
    private String currentCourseTitle;
    private String currentCourseHtml;
    private String currentOriginalPageHtml;
    private final List<CourseFile> rawCourseFiles = new ArrayList<>();
    private String assignmentsSearchQuery = "";
    private String pendingAssignmentsSearchQuery = "";
    private boolean isPrefetching = false;

    // Parser record equivalents
    private static class PortalAssignment {
        String num = "";
        String course = "";
        String title = "";
        String startDate = "";
        String deadline = "";
        String submission = "";
        String status = "";
        String downloadUrl = "";
        String submitUrl = "";
    }

    private static class PortalMcqTest {
        String num = "";
        String course = "";
        String title = "";
        String startDate = "";
        String endDate = "";
        String status = "";
        String actionUrl = "";
        String actionText = "";
    }

    private static class CourseFile {
        String href = "";
        String title = "";
        String description = "";
        String uploadDate = "N/A";
    }

    public CoursePortalTabView(AppContext context) {
        this(context, "portal_mcq");
    }

    public CoursePortalTabView(AppContext context, String initialTab) {
        this.context = context;
        buildShell();
        loadTab(initialTab, false);
        context.addConnectivityListener(connectivityListener);
        startSilentPrefetch();
    }

    private void buildShell() {
        root.setFillWidth(true);
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(20, 28, 0, 28));

        Label heading = new Label("Course Portal");
        heading.getStyleClass().add("heading-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerRow.getChildren().addAll(heading, spacer);

        // Dashboard row container
        overviewContainer = new VBox();
        overviewContainer.setFillWidth(true);

        tabBar = new HBox(4);
        tabBar.setPadding(new Insets(12, 28, 0, 28));
        tabBar.getChildren().addAll(
            tabBtn("MCQ Test", "portal_mcq"),
            tabBtn("Subjective Test", "portal_subjective"),
            tabBtn("Course Contents", "portal_contents"),
            tabBtn("Assignments Summary", "portal_assign_summ"),
            tabBtn("Pending Assignments", "portal_pending")
        );

        contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        root.getChildren().addAll(headerRow, overviewContainer, tabBar, contentPane);
    }

    private long calculateRemainingDays(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.isBlank()) return 999;
        try {
            java.time.format.DateTimeFormatter[] formatters = {
                java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            };
            
            String cleanStr = deadlineStr.trim();
            java.time.LocalDateTime ldt = null;
            for (var f : formatters) {
                try {
                    if (cleanStr.length() <= 11) { // Date only
                        java.time.LocalDate date = java.time.LocalDate.parse(cleanStr, f);
                        ldt = date.atTime(23, 59, 59);
                    } else {
                        ldt = java.time.LocalDateTime.parse(cleanStr, f);
                    }
                    break;
                } catch (Exception ignored) {}
            }
            
            if (ldt != null) {
                return java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDateTime.now(), ldt);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse deadline: " + deadlineStr);
        }
        return 999;
    }

    private java.time.LocalDate parseUploadDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || "n/a".equalsIgnoreCase(dateStr.trim())) {
            return java.time.LocalDate.MIN;
        }
        try {
            java.time.format.DateTimeFormatter[] formatters = {
                java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("d-MMM-yyyy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("d-MMM-yy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                java.time.format.DateTimeFormatter.ofPattern("dd MMM,yyyy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("d MMM,yyyy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("dd MMM, yyyy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("d MMM, yyyy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("dd MMMM,yyyy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("d MMMM,yyyy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("dd MMMM, yyyy", java.util.Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("d MMMM, yyyy", java.util.Locale.ENGLISH)
            };
            String clean = dateStr.trim();
            for (var f : formatters) {
                try {
                    return java.time.LocalDate.parse(clean, f);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("Failed to parse upload date: " + dateStr);
        }
        return java.time.LocalDate.MIN;
    }

    private List<PortalAssignment> parseAssignmentsFromHtml(String html, String submitSourcePage) {
        List<PortalAssignment> list = new ArrayList<>();
        if (html == null || html.isBlank()) return list;
        try {
            Document doc = Jsoup.parse(html);
            Element table = findMainGridTable(doc, "DataContent_gvPortalSummary");
            if (table != null) {
                Elements rows = table.select("tr");
                if (rows.size() > 1) {
                    Element headerRow = rows.first();
                    Elements headerCells = headerRow.select("th, td");
                    List<String> headers = new ArrayList<>();
                    for (Element hc : headerCells) {
                        headers.add(hc.text().trim().toLowerCase());
                    }

                    for (int r = 1; r < rows.size(); r++) {
                        Element row = rows.get(r);
                        Elements cells = row.select("td");
                        if (cells.isEmpty()) continue;

                        PortalAssignment pa = new PortalAssignment();
                        for (int c = 0; c < cells.size(); c++) {
                            if (c >= headers.size()) break;
                            Element cell = cells.get(c);
                            String txt = cell.text().trim().replaceAll("\\s+", " ");
                            String header = headers.get(c);

                            if (header.contains("#")) {
                                pa.num = txt;
                            } else if (header.contains("course")) {
                                pa.course = txt;
                            } else if (header.contains("title")) {
                                pa.title = txt;
                            } else if (header.contains("start")) {
                                pa.startDate = txt;
                            } else if (header.contains("deadline") || header.contains("due")) {
                                pa.deadline = txt;
                            } else if (header.contains("submission")) {
                                pa.submission = txt;
                            } else if (header.contains("status")) {
                                pa.status = txt;
                            }

                            // Extract download URL
                            if (header.contains("download") || header.contains("dowload")) {
                                pa.downloadUrl = context.portalRepository().extractAssignmentDownloadLink(cell);
                            }
                            // Extract submit URL
                            if (header.contains("submit")) {
                                pa.submitUrl = context.portalRepository().extractAssignmentSubmitLink(cell, submitSourcePage);
                            }
                        }
                        list.add(pa);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing assignments: " + e.getMessage());
        }
        return list;
    }

    private List<PortalMcqTest> parseMcqTestsFromHtml(String html) {
        List<PortalMcqTest> list = new ArrayList<>();
        if (html == null || html.isBlank()) return list;
        try {
            Document doc = Jsoup.parse(html);
            Element table = findMainGridTable(doc, "DataContent_gvCTSdashboard");
            if (table != null) {
                Elements rows = table.select("tr");
                if (rows.size() > 1) {
                    Element headerRow = rows.first();
                    Elements headerCells = headerRow.select("th, td");
                    List<String> headers = new ArrayList<>();
                    for (Element hc : headerCells) {
                        headers.add(hc.text().trim().toLowerCase());
                    }

                    for (int r = 1; r < rows.size(); r++) {
                        Element row = rows.get(r);
                        Elements cells = row.select("td");
                        if (cells.isEmpty()) continue;

                        PortalMcqTest mt = new PortalMcqTest();
                        for (int c = 0; c < cells.size(); c++) {
                            if (c >= headers.size()) break;
                            Element cell = cells.get(c);
                            String txt = cell.text().trim().replaceAll("\\s+", " ");
                            String header = headers.get(c);

                            if (header.contains("#")) {
                                mt.num = txt;
                            } else if (header.contains("course")) {
                                mt.course = txt;
                            } else if (header.contains("test") || header.contains("title")) {
                                mt.title = txt;
                            } else if (header.contains("start")) {
                                mt.startDate = txt;
                            } else if (header.contains("end")) {
                                mt.endDate = txt;
                            } else if (header.contains("status")) {
                                mt.status = txt;
                            }

                            Element aTag = cell.select("a").first();
                            if (aTag != null && !aTag.attr("href").isEmpty()) {
                                mt.actionUrl = aTag.attr("href");
                                mt.actionText = aTag.text().trim();
                            }
                        }
                        list.add(mt);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing MCQ tests: " + e.getMessage());
        }
        return list;
    }

    private int countUpcomingTests() {
        int count = 0;
        // MCQ Tests
        String mcqHtml = context.dataCacheService().getCachedHtml("CTS/CTSdashboard.aspx").orElse(null);
        if (mcqHtml != null) {
            List<PortalMcqTest> mcqs = parseMcqTestsFromHtml(mcqHtml);
            for (PortalMcqTest mcq : mcqs) {
                String status = mcq.status != null ? mcq.status.toLowerCase() : "";
                String actText = mcq.actionText != null ? mcq.actionText.toLowerCase() : "";
                if (!status.contains("completed") && !status.contains("submitted") && 
                    (actText.contains("start") || actText.contains("take") || status.contains("active") || status.contains("upcoming"))) {
                    count++;
                }
            }
        }
        // Subjective Tests
        String subHtml = context.dataCacheService().getCachedHtml("CoursePortal.aspx?isTest=1").orElse(null);
        if (subHtml != null) {
            List<PortalAssignment> subs = parseAssignmentsFromHtml(subHtml, "CoursePortal.aspx?isTest=1");
            for (PortalAssignment sub : subs) {
                String subText = sub.submission != null ? sub.submission.toLowerCase() : "";
                String status = sub.status != null ? sub.status.toLowerCase() : "";
                if (!subText.contains("submitted") && status.contains("open")) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countFilesFromDetailHtml(String html) {
        Document doc = Jsoup.parse(html);
        Element table = findMainGridTable(doc, "DataContent_gvPortalSummary");
        int count = 0;
        if (table != null) {
            Elements rows = table.select("tr");
            for (int r = 1; r < rows.size(); r++) {
                Element row = rows.get(r);
                Elements cells = row.select("td");
                if (cells.size() >= 3) {
                    for (Element cell : cells) {
                        Element aTag = cell.select("a").first();
                        if (aTag != null && !aTag.attr("href").isEmpty() && aTag.attr("href").toLowerCase().contains("download")) {
                            count++;
                            break;
                        }
                    }
                }
            }
        }
        return count;
    }

    private void prefetchCourseContentsFiles(List<String[]> courses, String contentsHtml) {
        if (!context.isOnline()) return;
        synchronized (this) {
            if (isPrefetching) return;
            isPrefetching = true;
        }
        new Thread(() -> {
            try {
                String ddName = context.portalRepository().findDropdownName(contentsHtml, "course");
                if (ddName == null) ddName = context.portalRepository().findDropdownName(contentsHtml, "ddl");
                if (ddName == null) {
                    synchronized (this) { isPrefetching = false; }
                    return;
                }

                for (String[] course : courses) {
                    String courseId = course[0];
                    String cacheKey = "CoursePortalContentsSummary.aspx_" + courseId;
                    
                    String resultHtml = context.portalRepository().postbackWithDropdown("CoursePortalContentsSummary.aspx", ddName, courseId);
                    if (resultHtml != null) {
                        context.dataCacheService().cacheHtml(cacheKey, resultHtml);
                    }
                }
                
                Platform.runLater(() -> {
                    updateOverviewStats();
                    if ("portal_contents".equals(activeTab) && currentCourseId == null) {
                        String cachedHtml = context.dataCacheService().getCachedHtml("CoursePortalContentsSummary.aspx").orElse(contentsHtml);
                        List<String[]> currentCourses = context.portalRepository().parseDropdownOptions(cachedHtml, "course");
                        if (currentCourses.isEmpty()) {
                            currentCourses.addAll(context.portalRepository().parseDropdownOptions(cachedHtml, "ddl"));
                        }
                        contentPane.getChildren().clear();
                        if (!currentCourses.isEmpty()) {
                            contentPane.getChildren().add(buildCourseContentsSelectorView(currentCourses, cachedHtml));
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("Silent prefetch failed: " + e.getMessage());
            } finally {
                synchronized (this) {
                    isPrefetching = false;
                }
            }
        }).start();
    }

    private void startSilentPrefetch() {
        if (!context.isOnline()) return;
        new Thread(() -> {
            try {
                String mainHtml = context.fetchAndCacheHtml("CoursePortalContentsSummary.aspx");
                if (mainHtml == null) {
                    mainHtml = context.dataCacheService().getCachedHtml("CoursePortalContentsSummary.aspx").orElse(null);
                }
                if (mainHtml == null) return;

                List<String[]> courses = context.portalRepository().parseDropdownOptions(mainHtml, "course");
                if (courses.isEmpty()) {
                    courses.addAll(context.portalRepository().parseDropdownOptions(mainHtml, "ddl"));
                }
                if (courses.isEmpty()) return;

                prefetchCourseContentsFiles(courses, mainHtml);
            } catch (Exception e) {
                System.err.println("Silent initial prefetch failed: " + e.getMessage());
            }
        }).start();
    }

    private void updateOverviewStats() {
        // Compute active courses
        int activeCourses = 0;
        String contentsHtml = context.dataCacheService().getCachedHtml("CoursePortalContentsSummary.aspx").orElse(null);
        List<String[]> courses = new ArrayList<>();
        if (contentsHtml != null) {
            courses = context.portalRepository().parseDropdownOptions(contentsHtml, "course");
            if (courses.isEmpty()) {
                courses = context.portalRepository().parseDropdownOptions(contentsHtml, "ddl");
            }
            activeCourses = courses.size();
        }

        // Compute materials & downloads
        int totalFiles = 0;
        for (String[] course : courses) {
            String courseId = course[0];
            String detailHtml = context.dataCacheService().getCachedHtml("CoursePortalContentsSummary.aspx_" + courseId).orElse(null);
            if (detailHtml != null) {
                totalFiles += countFilesFromDetailHtml(detailHtml);
            }
        }

        // Compute assignments
        int pendingAssigns = 0;
        int completedAssigns = 0;
        String assignmentsHtml = context.dataCacheService().getCachedHtml("CoursePortal.aspx").orElse(null);
        if (assignmentsHtml != null) {
            List<PortalAssignment> allAssigns = parseAssignmentsFromHtml(assignmentsHtml, "CoursePortal.aspx");
            for (var a : allAssigns) {
                String sub = a.submission != null ? a.submission.toLowerCase() : "";
                if (sub.contains("submitted") && !sub.contains("not")) {
                    completedAssigns++;
                }
            }
        }
        
        String pendingHtml = context.dataCacheService().getCachedHtml("CoursePortalPendingAssignments.aspx").orElse(null);
        if (pendingHtml != null) {
            List<PortalAssignment> pAssigns = parseAssignmentsFromHtml(pendingHtml, "CoursePortalPendingAssignments.aspx");
            pendingAssigns = pAssigns.size();
        } else {
            if (assignmentsHtml != null) {
                List<PortalAssignment> allAssigns = parseAssignmentsFromHtml(assignmentsHtml, "CoursePortal.aspx");
                for (var a : allAssigns) {
                    String sub = a.submission != null ? a.submission.toLowerCase() : "";
                    String status = a.status != null ? a.status.toLowerCase() : "";
                    if (!sub.contains("submitted") && !status.contains("closed")) {
                        pendingAssigns++;
                    }
                }
            }
        }

        // Compute upcoming tests
        int upcomingTests = countUpcomingTests();

        int downloads = totalFiles; // downloadable content is course files

        final int fActive = activeCourses;
        final int fFiles = totalFiles;
        final int fPending = pendingAssigns;
        final int fCompleted = completedAssigns;
        final int fTests = upcomingTests;
        final int fDownloads = downloads;

        Platform.runLater(() -> {
            overviewContainer.getChildren().clear();
            
            HBox cardRow = new HBox(12);
            cardRow.setPadding(new Insets(16, 28, 4, 28));
            cardRow.setAlignment(Pos.CENTER);
            
            VBox cActive = buildOverviewCard("📚", "Active Courses", String.valueOf(fActive), "Registered modules");
            VBox cFiles = buildOverviewCard("📂", "Course Materials", String.valueOf(fFiles), "Total lecture notes");
            VBox cPending = buildOverviewCard("📌", "Pending Assignments", String.valueOf(fPending), "Require attention");
            VBox cCompleted = buildOverviewCard("✅", "Completed", String.valueOf(fCompleted), "Submitted tasks");
            VBox cTests = buildOverviewCard("📝", "Upcoming Tests", String.valueOf(fTests), "MCQ & Subjective");
            VBox cDownloads = buildOverviewCard("⬇️", "Downloads", String.valueOf(fDownloads), "Available files");
            
            cardRow.getChildren().addAll(cActive, cFiles, cPending, cCompleted, cTests, cDownloads);
            overviewContainer.getChildren().add(cardRow);
        });
    }

    private VBox buildOverviewCard(String emoji, String title, String value, String desc) {
        VBox card = new VBox(4);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-background-radius: 10;" +
            "-fx-border-radius: 10;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.01), 4, 0, 0, 2);"
        );

        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 15px;");
        Label titleLbl = new Label(title.toUpperCase());
        titleLbl.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted; -fx-letter-spacing: 0.5px;");
        topRow.getChildren().addAll(icon, titleLbl);

        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: -color-accent;");
        
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");

        card.getChildren().addAll(topRow, valueLbl, descLbl);

        // Add micro-animation
        card.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(120), card);
            tt.setToY(-2);
            tt.play();
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-background-radius: 10;" +
                "-fx-border-radius: 10;" +
                "-fx-border-color: -color-accent;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.08), 8, 0, 0, 4);"
            );
        });
        card.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(120), card);
            tt.setToY(0);
            tt.play();
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-background-radius: 10;" +
                "-fx-border-radius: 10;" +
                "-fx-border-color: -color-border;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.01), 4, 0, 0, 2);"
            );
        });

        return card;
    }

    private Button tabBtn(String label, String id) {
        Button b = new Button(label);
        b.setUserData(id);
        b.getStyleClass().add("custom-tab");
        b.setOnAction(e -> loadTab(id, false));
        return b;
    }

    private void loadTab(String tabKey) {
        loadTab(tabKey, false);
    }

    private void loadTab(String tabKey, boolean forceReload) {
        if (activeTab.equals(tabKey) && !forceReload) return;
        activeTab = tabKey;

        for (var n : tabBar.getChildren()) {
            if (n instanceof Button b) {
                boolean isActive = tabKey.equals(b.getUserData());
                b.getStyleClass().remove("custom-tab-active");
                if (isActive) {
                    b.getStyleClass().add("custom-tab-active");
                }
            }
        }

        contentPane.getChildren().clear();
        uploadButtons.clear();
        switch (tabKey) {
            case "portal_mcq" -> loadMcqData(forceReload);
            case "portal_subjective" -> loadSubjectiveData(forceReload);
            case "portal_contents" -> loadCourseContentsData(forceReload);
            case "portal_assign_summ" -> loadAssignmentsData(forceReload);
            case "portal_pending" -> loadPendingAssignmentsData(forceReload);
        }
    }

    private void showLoading(String msg) {
        Platform.runLater(() -> {
            contentPane.getChildren().clear();
            ScrollPane sp = new ScrollPane(com.assignly.util.ShimmerBuilder.buildCoursePortalShimmer());
            sp.setFitToWidth(true);
            sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
            contentPane.getChildren().add(sp);
        });
    }

    private VBox buildErrorState(String title, String message) {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40, 28, 40, 28));
        box.getStyleClass().add("panel-info-danger");
        box.setStyle("-fx-background-radius:12;-fx-border-radius:12;-fx-max-width:550;");

        Label icon = new Label("⚠️");
        icon.setStyle("-fx-font-size:32px;");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("status-error");
        titleLbl.setStyle("-fx-font-size:16px;");

        Label msgLbl = new Label(message);
        msgLbl.getStyleClass().add("status-error");
        msgLbl.setStyle("-fx-font-size:12px;-fx-text-alignment:center;");
        msgLbl.setWrapText(true);

        box.getChildren().addAll(icon, titleLbl, msgLbl);
        
        VBox outer = new VBox(box);
        outer.setAlignment(Pos.CENTER);
        outer.setPadding(new Insets(40));
        return outer;
    }

    private void loadMcqData(boolean forceRefresh) {
        showLoading("Loading MCQ Tests...");
        new Thread(() -> {
            try {
                String html = null;
                if (!forceRefresh) {
                    html = context.dataCacheService().getCachedHtml("CTS/CTSdashboard.aspx").orElse(null);
                }
                if (html == null) {
                    html = context.fetchAndCacheHtml("CTS/CTSdashboard.aspx");
                    if (html == null) {
                        html = context.dataCacheService().getCachedHtml("CTS/CTSdashboard.aspx").orElse(null);
                    }
                }

                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Unable to load MCQ Tests",
                            "Failed to connect to the portal database. Please check your internet connection and try again."
                        ));
                    });
                    return;
                }

                try {
                    java.nio.file.Files.writeString(java.nio.file.Path.of(com.assignly.util.AppDirectoryHelper.getAppDataDir(), "cts_dashboard_raw.html"), html);
                } catch (IOException ex) {
                    ErrorReporter.logError("CoursePortalTabView#loadMcqData write cts_dashboard_raw.html", ex);
                }

                final String finalHtml = html;
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    try {
                        contentPane.getChildren().add(buildMcqView(finalHtml));
                        updateOverviewStats();
                    } catch (Exception ex) {
                        ErrorReporter.notify(context, "Failed to parse MCQ tests. Portal layout may have changed.", "CoursePortalTabView#loadMcqData", ex);
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Parsing Error",
                            "Failed to read the MCQ table layout cleanly. The portal database may be undergoing updates."
                        ));
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load MCQ tests. Please try again.", "CoursePortalTabView#loadMcqData", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while loading MCQ tests: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private ScrollPane buildMcqView(String html) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        if (!context.isOnline()) {
            content.getChildren().add(buildOfflineBanner());
        }

        Document doc = Jsoup.parse(html);
        
        String headingText = resolvePortalHeading(doc, "COMSATS University Islamabad Online Testing Service Dashboard");

        Label subHeading = new Label(headingText);
        subHeading.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;-fx-padding:0 0 4 0;");
        content.getChildren().add(subHeading);

        List<PortalMcqTest> tests = parseMcqTestsFromHtml(html);

        if (!tests.isEmpty()) {
            FlowPane cardsFlow = new FlowPane(16, 16);
            cardsFlow.setPadding(new Insets(8, 0, 16, 0));
            for (PortalMcqTest mt : tests) {
                cardsFlow.getChildren().add(buildMcqTestCard(mt));
            }
            cardsFlow.widthProperty().addListener((obs, oldVal, newVal) -> adjustFlowGrid(cardsFlow, 300, 450, 16));
            Platform.runLater(() -> adjustFlowGrid(cardsFlow, 300, 450, 16));
            content.getChildren().add(cardsFlow);
        } else {
            renderEmptyState(content);
        }

        sp.setContent(content);
        return sp;
    }

    private void renderEmptyState(VBox content) {
        VBox emptyCard = new VBox(12);
        emptyCard.setAlignment(Pos.CENTER);
        emptyCard.setPadding(new Insets(32, 24, 32, 24));
        emptyCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        Label icon = new Label("📝");
        icon.setStyle("-fx-font-size:28px;");

        Label label = new Label("No MCQ Tests Found");
        label.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:13px;-fx-font-weight:bold;");

        Label desc = new Label("There are currently no active or completed multiple choice tests listed on the portal.");
        desc.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:11px;-fx-text-alignment:center;");
        desc.setWrapText(true);

        emptyCard.getChildren().addAll(icon, label, desc);
        content.getChildren().add(emptyCard);
    }

    private VBox buildMcqTestCard(PortalMcqTest mt) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(300);
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 3);"
        );

        Label courseLbl = new Label(mt.course.toUpperCase());
        courseLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-accent; -fx-background-color: rgba(20, 184, 166, 0.08); -fx-padding: 3 8; -fx-background-radius: 6;");
        courseLbl.setWrapText(true);

        Label titleLbl = new Label(mt.title);
        titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        titleLbl.setWrapText(true);
        VBox.setVgrow(titleLbl, Priority.ALWAYS);

        VBox dateCol = new VBox(4);
        dateCol.setStyle("-fx-background-color: -color-bg-main; -fx-padding: 8 10; -fx-background-radius: 6;");
        
        Label startLbl = new Label("📅 Start: " + mt.startDate);
        startLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");
        
        Label endLbl = new Label("⏰ End:   " + mt.endDate);
        endLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");
        
        dateCol.getChildren().addAll(startLbl, endLbl);

        HBox bottomRow = new HBox(8);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Label statusBadge = new Label(mt.status.isEmpty() ? "ACTIVE" : mt.status.toUpperCase());
        String st = mt.status.toLowerCase();
        if (st.contains("completed") || st.contains("submitted") || st.contains("expired")) {
            statusBadge.getStyleClass().add("badge-muted");
        } else if (st.contains("active") || st.contains("start")) {
            statusBadge.getStyleClass().add("badge-success");
        } else {
            statusBadge.getStyleClass().add("badge-info");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bottomRow.getChildren().addAll(statusBadge, spacer);

        if (mt.actionUrl != null && !mt.actionUrl.isEmpty()) {
            Button actBtn = new Button(mt.actionText != null && !mt.actionText.isEmpty() ? mt.actionText : "Start Test");
            actBtn.setCursor(javafx.scene.Cursor.HAND);
            
            boolean isStart = actBtn.getText().toLowerCase().contains("start") || actBtn.getText().toLowerCase().contains("take");
            if (isStart) {
                actBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;");
            } else {
                actBtn.setStyle("-fx-background-color: -color-bg-main; -fx-text-fill: -color-accent; -fx-border-color: -color-accent; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 11;");
            }
            
            actBtn.setOnAction(e -> loadMcqPaperView(mt.actionUrl, mt.actionText));
            applyOfflineStateIfOffline(actBtn, actBtn.getText());
            uploadButtons.add(actBtn);
            bottomRow.getChildren().add(actBtn);
        }

        card.getChildren().addAll(courseLbl, titleLbl, dateCol, bottomRow);

        card.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(120), card);
            tt.setToY(-3);
            tt.play();
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-border-color: -color-accent;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.08), 12, 0, 0, 6);"
            );
        });
        card.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(120), card);
            tt.setToY(0);
            tt.play();
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-border-color: -color-border;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 3);"
            );
        });

        String highlight = context.getPendingSearchHighlight();
        if (highlight != null && !highlight.isBlank() && 
            (mt.title.toLowerCase().contains(highlight.toLowerCase()) || 
             mt.course.toLowerCase().contains(highlight.toLowerCase()))) {
            card.setStyle("-fx-background-color: rgba(20, 184, 166, 0.18); -fx-border-color: -color-accent; -fx-border-width: 1; -fx-border-radius: 12;");
            Platform.runLater(() -> card.requestFocus());
            context.clearPendingSearchHighlight();
        }

        return card;
    }

    private void loadMcqPaperView(String relativeUrl, String title) {
        String urlTemp = relativeUrl;
        if (!urlTemp.startsWith("CTS/") && !urlTemp.startsWith("/CTS/")) {
            urlTemp = "CTS/" + urlTemp;
        }
        final String pageUrl = urlTemp;

        showLoading("Loading MCQ Test details...");

        new Thread(() -> {
            try {
                String html = context.portalRepository().fetchPageHtml(pageUrl);
                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Connection Error",
                            "Failed to load MCQ Test details. Please check your internet connection."
                        ));
                    });
                    return;
                }

                try {
                    java.nio.file.Files.writeString(java.nio.file.Path.of(com.assignly.util.AppDirectoryHelper.getAppDataDir(), "cts_paper_raw.html"), html);
                } catch (IOException ex) {
                    ErrorReporter.logError("CoursePortalTabView#loadMcqDetails write cts_paper_raw.html", ex);
                }

                Document doc = Jsoup.parse(html);
                boolean isCompleted = doc.select("#DataContent_dvStudentOnlineTestResult").first() != null
                        || html.contains("online test is completed")
                        || doc.getElementById("DataContent_lblObtainMarks") != null;

                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    if (isCompleted) {
                        try {
                            contentPane.getChildren().add(buildNativeResultView(doc, pageUrl));
                        } catch (Exception ex) {
                            ErrorReporter.logError("CoursePortalTabView#loadMcqDetails render native MCQ view", ex);
                            renderWebViewPaper(pageUrl, title);
                        }
                    } else {
                        renderWebViewPaper(pageUrl, title);
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load MCQ details. Please try again.", "CoursePortalTabView#loadMcqDetails", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while loading MCQ details: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private VBox buildNativeResultView(Document doc, String pageUrl) {
        VBox wrapper = new VBox(20);
        wrapper.setPadding(new Insets(16, 28, 24, 28));
        wrapper.setFillWidth(true);

        Button backBtn = new Button("← Back to MCQ Tests List");
        backBtn.setCursor(javafx.scene.Cursor.HAND);
        backBtn.setStyle("-fx-background-color: -color-bg-card;-fx-text-fill: -color-accent;-fx-border-color: -color-accent;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:6 12;");
        backBtn.setOnAction(e -> {
            activeTab = ""; // Force reload
            loadTab("portal_mcq");
        });

        HBox topBar = new HBox(backBtn);
        topBar.setStyle("-fx-padding:0 0 10 0;");
        wrapper.getChildren().add(topBar);

        String testTitle = "Multiple Choice Questions";
        Element titleEl = doc.getElementById("DataContent_lblTestTitleResult");
        if (titleEl != null) testTitle = titleEl.text().trim();

        String totalQ = "N/A";
        Element totalQEl = doc.getElementById("DataContent_lblTotalQuestion");
        if (totalQEl != null) totalQ = totalQEl.text().trim();

        String attempted = "N/A";
        Element attemptedEl = doc.getElementById("DataContent_lblTotalAttempted");
        if (attemptedEl != null) attempted = attemptedEl.text().trim();

        String totalMarks = "N/A";
        Element totalMarksEl = doc.getElementById("DataContent_lblTotalMarks");
        if (totalMarksEl != null) totalMarks = totalMarksEl.text().trim();

        String obtainMarks = "N/A";
        Element obtainMarksEl = doc.getElementById("DataContent_lblObtainMarks");
        if (obtainMarksEl == null) obtainMarksEl = doc.select("[id$=lblObtainMarks]").first();
        if (obtainMarksEl != null) obtainMarks = obtainMarksEl.text().trim();

        VBox scorecard = new VBox(20);
        scorecard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:12;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:12;-fx-padding:24;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.03),15,0,0,3);");
        
        HBox scoreHeader = new HBox(12);
        scoreHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label statusBadge = new Label("COMPLETED");
        statusBadge.getStyleClass().add("badge-success");
        
        Label titleLabel = new Label("MCQ Test Scorecard");
        titleLabel.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
        
        scoreHeader.getChildren().addAll(titleLabel, statusBadge);
        scorecard.getChildren().add(scoreHeader);

        VBox detailBox = new VBox(0);
        detailBox.setStyle("-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;-fx-background-color: -color-bg-main;");
        
        detailBox.getChildren().addAll(
            resultRow("Test Title", testTitle, true),
            resultRow("Total Questions", totalQ, false),
            resultRow("Total Attempted", attempted, true),
            resultRow("Total Marks", totalMarks, false),
            resultRow("Obtained Marks", obtainMarks, true)
        );

        HBox visualScoreContainer = new HBox(32);
        visualScoreContainer.setAlignment(Pos.CENTER);
        visualScoreContainer.setPadding(new Insets(16, 0, 8, 0));

        VBox bigScoreCircle = new VBox(4);
        bigScoreCircle.setAlignment(Pos.CENTER);
        bigScoreCircle.setStyle("-fx-background-color: -color-accent;-fx-background-radius:100;-fx-min-width:110;-fx-min-height:110;-fx-max-width:110;-fx-max-height:110;-fx-effect:dropshadow(three-pass-box,rgba(0,70,67,0.2),10,0,0,4);");
        
        Label obtainScoreLbl = new Label(obtainMarks);
        obtainScoreLbl.setStyle("-fx-text-fill:white;-fx-font-size:32px;-fx-font-weight:bold;");
        
        Label totalScoreLbl = new Label("out of " + totalMarks);
        totalScoreLbl.setStyle("-fx-text-fill:#a5d6a7;-fx-font-size:10px;-fx-font-weight:bold;");
        
        bigScoreCircle.getChildren().addAll(obtainScoreLbl, totalScoreLbl);

        VBox percentageSection = new VBox(6);
        percentageSection.setAlignment(Pos.CENTER_LEFT);
        
        double pctVal = 0.0;
        try {
            double o = Double.parseDouble(obtainMarks);
            double t = Double.parseDouble(totalMarks);
            if (t > 0) pctVal = (o / t) * 100.0;
        } catch (NumberFormatException ex) {
            ErrorReporter.logError("CoursePortalTabView#buildNativeResultView parse score", ex);
        }
        
        Label pctLbl = new Label(String.format("%.1f%% Score", pctVal));
        pctLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill: -color-accent;");
        
        Label feedbackLbl = new Label();
        if (pctVal >= 90) {
            feedbackLbl.setText("Outstanding achievement!");
            feedbackLbl.setStyle("-fx-text-fill:#15803d;-fx-font-weight:600;-fx-font-size:12px;");
        } else if (pctVal >= 80) {
            feedbackLbl.setText("Excellent effort!");
            feedbackLbl.setStyle("-fx-text-fill:#166534;-fx-font-weight:600;-fx-font-size:12px;");
        } else if (pctVal >= 60) {
            feedbackLbl.setText("Good score, keep it up.");
            feedbackLbl.setStyle("-fx-text-fill:#b45309;-fx-font-weight:600;-fx-font-size:12px;");
        } else {
            feedbackLbl.setText("Review recommended.");
            feedbackLbl.setStyle("-fx-text-fill:#b91c1c;-fx-font-weight:600;-fx-font-size:12px;");
        }
        
        percentageSection.getChildren().addAll(pctLbl, feedbackLbl);
        visualScoreContainer.getChildren().addAll(bigScoreCircle, percentageSection);
        
        scorecard.getChildren().addAll(visualScoreContainer, detailBox);
        wrapper.getChildren().add(scorecard);

        return wrapper;
    }

    private HBox resultRow(String key, String val, boolean bg) {
        HBox r = new HBox();
        r.setStyle("-fx-padding:12 16;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;" + (bg ? "-fx-background-color: -color-bg-card;" : "-fx-background-color: -color-bg-main;"));
        
        Label kl = new Label(key);
        kl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill: -color-text-muted;");
        kl.setMinWidth(150);

        Label vl = new Label(val);
        vl.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-main;-fx-font-weight:500;");
        vl.setWrapText(true);
        HBox.setHgrow(vl, Priority.ALWAYS);

        r.getChildren().addAll(kl, vl);
        return r;
    }

    private void renderWebViewPaper(String pageUrl, String title) {
        contentPane.getChildren().clear();
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(16, 28, 16, 28));

        Button backBtn = new Button("← Back to MCQ Tests List");
        backBtn.setCursor(javafx.scene.Cursor.HAND);
        backBtn.setStyle("-fx-background-color: -color-bg-card;-fx-text-fill: -color-accent;-fx-border-color: -color-accent;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:6 12;");
        backBtn.setOnAction(e -> {
            activeTab = ""; // Force reload
            loadTab("portal_mcq");
        });

        HBox topBar = new HBox(backBtn);
        topBar.setStyle("-fx-padding:0 0 10 0;");

        WebPortalTabView webView = new WebPortalTabView(context, pageUrl, title);
        VBox.setVgrow(webView.getRoot(), Priority.ALWAYS);

        wrapper.getChildren().addAll(topBar, webView.getRoot());
        contentPane.getChildren().add(wrapper);
    }

    private void loadSubjectiveData(boolean forceRefresh) {
        showLoading("Loading Subjective Tests...");
        new Thread(() -> {
            try {
                String html = null;
                if (!forceRefresh) {
                    html = context.dataCacheService().getCachedHtml("CoursePortal.aspx?isTest=1").orElse(null);
                }
                if (html == null) {
                    html = context.fetchAndCacheHtml("CoursePortal.aspx?isTest=1");
                    if (html == null) {
                        html = context.dataCacheService().getCachedHtml("CoursePortal.aspx?isTest=1").orElse(null);
                    }
                }

                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Unable to load Subjective Tests", 
                            "Failed to connect to the portal database. Please check your internet connection and try again."
                        ));
                    });
                    return;
                }

                try {
                    java.nio.file.Files.writeString(java.nio.file.Path.of(com.assignly.util.AppDirectoryHelper.getAppDataDir(), "cts_subjective_raw.html"), html);
                } catch (IOException ex) {
                    ErrorReporter.logError("CoursePortalTabView#loadSubjectiveData write cts_subjective_raw.html", ex);
                }

                final String finalHtml = html;
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    try {
                        contentPane.getChildren().add(buildSubjectiveView(finalHtml));
                        updateOverviewStats();
                    } catch (Exception ex) {
                        ErrorReporter.notify(context, "Failed to parse Subjective Tests. Portal layout may have changed.", "CoursePortalTabView#loadSubjectiveData", ex);
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Parsing Error", 
                            "Failed to read the Subjective Tests table. The portal layout might have changed."
                        ));
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load Subjective Tests. Please try again.", "CoursePortalTabView#loadSubjectiveData", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while loading Subjective tests: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private ScrollPane buildSubjectiveView(String html) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        if (!context.isOnline()) {
            content.getChildren().add(buildOfflineBanner());
        }

        Document doc = Jsoup.parse(html);
        
        String headingText = resolvePortalHeading(doc, "Subjective Tests Summary");

        Label subHeading = new Label(headingText);
        subHeading.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;-fx-padding:0 0 4 0;");
        content.getChildren().add(subHeading);

        List<PortalAssignment> tests = parseSubjectiveTestsFromHtml(html);

        if (!tests.isEmpty()) {
            FlowPane cardsFlow = new FlowPane(16, 16);
            cardsFlow.setPadding(new Insets(8, 0, 16, 0));
            for (PortalAssignment sub : tests) {
                cardsFlow.getChildren().add(buildSubjectiveTestCard(sub));
            }
            cardsFlow.widthProperty().addListener((obs, oldVal, newVal) -> adjustFlowGrid(cardsFlow, 300, 450, 16));
            Platform.runLater(() -> adjustFlowGrid(cardsFlow, 300, 450, 16));
            content.getChildren().add(cardsFlow);
        } else {
            renderEmptySubjectiveState(content);
        }

        sp.setContent(content);
        return sp;
    }

    private void renderEmptySubjectiveState(VBox content) {
        VBox emptyCard = new VBox(12);
        emptyCard.setAlignment(Pos.CENTER);
        emptyCard.setPadding(new Insets(32, 24, 32, 24));
        emptyCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        Label icon = new Label("📝");
        icon.setStyle("-fx-font-size:28px;");

        Label label = new Label("No Subjective Tests Found");
        label.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:13px;-fx-font-weight:bold;");

        Label desc = new Label("There are currently no active or completed subjective tests/assignments listed for your registered courses.");
        desc.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:11px;-fx-text-alignment:center;");
        desc.setWrapText(true);

        emptyCard.getChildren().addAll(icon, label, desc);
        content.getChildren().add(emptyCard);
    }

    private VBox buildSubjectiveTestCard(PortalAssignment sub) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(300);
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 3);"
        );

        Label courseLbl = new Label(sub.course.toUpperCase());
        courseLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-accent; -fx-background-color: rgba(20, 184, 166, 0.08); -fx-padding: 3 8; -fx-background-radius: 6;");
        courseLbl.setWrapText(true);

        Label titleLbl = new Label(sub.title);
        titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        titleLbl.setWrapText(true);
        VBox.setVgrow(titleLbl, Priority.ALWAYS);

        VBox dateCol = new VBox(4);
        dateCol.setStyle("-fx-background-color: -color-bg-main; -fx-padding: 8 10; -fx-background-radius: 6;");
        
        Label startLbl = new Label("📅 Start: " + sub.startDate);
        startLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");
        
        Label endLbl = new Label("⏰ Due:   " + sub.deadline);
        endLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");
        
        dateCol.getChildren().addAll(startLbl, endLbl);

        HBox bottomRow = new HBox(6);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Label subBadge = new Label(sub.submission.toUpperCase());
        String subLower = sub.submission.toLowerCase();
        if (subLower.contains("submitted") && !subLower.contains("not")) {
            subBadge.getStyleClass().add("badge-success");
        } else {
            subBadge.getStyleClass().add("badge-danger");
        }

        Label statusBadge = new Label(sub.status.toUpperCase());
        if (sub.status.toLowerCase().contains("closed")) {
            statusBadge.getStyleClass().add("badge-muted");
        } else {
            statusBadge.getStyleClass().add("badge-info");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bottomRow.getChildren().addAll(subBadge, statusBadge, spacer);

        if (sub.submitUrl != null && !sub.submitUrl.isEmpty()) {
            Button actBtn = new Button("View/Submit");
            actBtn.setCursor(javafx.scene.Cursor.HAND);
            actBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;");
            actBtn.setOnAction(e -> loadSubjectivePaperView(sub.submitUrl, sub.title));
            applyOfflineStateIfOffline(actBtn, "View/Submit");
            uploadButtons.add(actBtn);
            bottomRow.getChildren().add(actBtn);
        }

        card.getChildren().addAll(courseLbl, titleLbl, dateCol, bottomRow);

        card.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(120), card);
            tt.setToY(-3);
            tt.play();
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-border-color: -color-accent;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.08), 12, 0, 0, 6);"
            );
        });
        card.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(120), card);
            tt.setToY(0);
            tt.play();
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-border-color: -color-border;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 3);"
            );
        });

        String highlight = context.getPendingSearchHighlight();
        if (highlight != null && !highlight.isBlank() && 
            (sub.title.toLowerCase().contains(highlight.toLowerCase()) || 
             sub.course.toLowerCase().contains(highlight.toLowerCase()))) {
            card.setStyle("-fx-background-color: rgba(20, 184, 166, 0.18); -fx-border-color: -color-accent; -fx-border-width: 1; -fx-border-radius: 12;");
            Platform.runLater(() -> card.requestFocus());
            context.clearPendingSearchHighlight();
        }

        return card;
    }

    private double subjectiveColW(String h, int i, int total) {
        String l = h.toLowerCase();
        if (l.contains("#")) return 50;
        if (l.contains("test") || l.contains("title") || l.contains("assignment")) return 200;
        if (l.contains("course")) return 280;
        if (l.contains("date") || l.contains("time") || l.contains("due")) return 180;
        if (l.contains("action") || l.contains("status")) return 140;
        return 120;
    }

    private void loadSubjectivePaperView(String relativeUrl, String title) {
        showLoading("Loading Subjective Test details...");

        new Thread(() -> {
            try {
                String html;
                if (context.portalRepository().isPostBackDownloadLink(relativeUrl)) {
                    PortalRepository.PostBackLink postBackLink = context.portalRepository().extractPostBackLinkFromLink(relativeUrl);
                    if (postBackLink == null) {
                        throw new RuntimeException("Invalid postback link: " + relativeUrl);
                    }
                    String sourcePageUrl = postBackLink.sourcePageUrl();
                    if (sourcePageUrl == null || sourcePageUrl.isBlank()) {
                        sourcePageUrl = "CoursePortal.aspx?isTest=1";
                    }
                    html = context.portalRepository().postbackEvent(sourcePageUrl, postBackLink.info().target());
                } else {
                    String urlTemp = relativeUrl;
                    if (!urlTemp.startsWith("CTS/") && !urlTemp.startsWith("/CTS/")) {
                        urlTemp = "CTS/" + urlTemp;
                    }
                    html = context.portalRepository().fetchPageHtml(urlTemp);
                }

                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Connection Error",
                            "Failed to load Subjective Test details. Please check your internet connection."
                        ));
                    });
                    return;
                }

                Document doc = Jsoup.parse(html);
                
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    try {
                        contentPane.getChildren().add(buildNativeSubjectiveDetailView(doc, relativeUrl, title));
                    } catch (Exception ex) {
                        ErrorReporter.logError("CoursePortalTabView#loadSubjectivePaperView native render error, falling back to WebView", ex);
                        renderWebViewPaper(relativeUrl, title);
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load Subjective Test details. Please try again.", "CoursePortalTabView#loadSubjectiveDetails", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while loading Subjective details: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private ScrollPane buildNativeSubjectiveDetailView(Document doc, String relativeUrl, String title) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox wrapper = new VBox(20);
        wrapper.setPadding(new Insets(16, 28, 24, 28));
        wrapper.setFillWidth(true);

        Button backBtn = new Button("← Back to Subjective Tests List");
        backBtn.setCursor(javafx.scene.Cursor.HAND);
        backBtn.setStyle("-fx-background-color: -color-bg-card;-fx-text-fill: -color-accent;-fx-border-color: -color-accent;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:6 12;");
        backBtn.setOnAction(e -> {
            activeTab = ""; // Force reload
            loadTab("portal_subjective");
        });

        HBox topBar = new HBox(backBtn);
        topBar.setStyle("-fx-padding:0 0 4 0;");
        wrapper.getChildren().add(topBar);

        String headingText = resolvePortalHeading(doc, title);

        Label titleLabel = new Label(headingText);
        titleLabel.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");

        java.util.Map<String, String> metadata = new java.util.LinkedHashMap<>();
        
        record AttachmentInfo(String filename, String href) {}
        List<AttachmentInfo> teacherAttachments = new ArrayList<>();
        
        String submittedFilename = null;
        String submissionDate = null;
        String obtainedMarks = null;

        // Parse key-value metadata rows from tables
        for (Element table : doc.select("table")) {
            String tableId = table.id().toLowerCase();
            if (tableId.contains("sidebar") || tableId.contains("menu")) continue;
            
            for (Element row : table.select("tr")) {
                Elements cells = row.select("td, th");
                if (cells.size() == 2) {
                    String key = cells.get(0).text().trim().replaceAll(":$", "").trim();
                    Element valCell = cells.get(1);
                    String val = valCell.text().trim();
                    
                    if (!key.isEmpty() && !val.isEmpty()) {
                        val = val.replaceAll("\\s+", " ");
                        
                        Elements anchors = valCell.select("a");
                        for (Element a : anchors) {
                            String href = a.attr("href");
                            String onClick = a.attr("onclick");
                            String aText = a.text().trim();
                            
                            if (href.toLowerCase().contains("download") || href.toLowerCase().contains("file") || 
                                onClick.toLowerCase().contains("download") || onClick.toLowerCase().contains("file") ||
                                href.toLowerCase().contains("assignmentfiles.aspx") || href.toLowerCase().contains("courseportalassignmentfiles.aspx")) {
                                
                                String finalHref = href;
                                if (finalHref.isEmpty() || finalHref.equals("#")) {
                                    PortalRepository.PostBackInfo postBackInfo = context.portalRepository().extractPostBackInfo(onClick);
                                    if (postBackInfo != null) {
                                        finalHref = context.portalRepository().toPostBackDownloadLink(postBackInfo, null);
                                    }
                                }
                                if (!finalHref.isEmpty() && !finalHref.equals("#")) {
                                    teacherAttachments.add(new AttachmentInfo(aText.isEmpty() ? "Download Instruction File" : aText, finalHref));
                                }
                            }
                        }
                        
                        String keyLower = key.toLowerCase();
                        if (keyLower.contains("submitted file") || keyLower.contains("submission file")) {
                            submittedFilename = val;
                        } else if (keyLower.contains("submission date") || keyLower.contains("submitted date")) {
                            submissionDate = val;
                        } else if (keyLower.contains("obtain") || keyLower.contains("grade") || keyLower.contains("score")) {
                            obtainedMarks = val;
                        }
                        
                        metadata.put(key, val);
                    }
                }
            }
        }

        // Fallback: search for explicit label tags or spans on the page with details
        for (Element span : doc.select("span[id*='lbl'], span[id*='lbl_']")) {
            String id = span.id().toLowerCase();
            String val = span.text().trim();
            if (val.isEmpty()) continue;
            
            if (id.contains("lblcourse") && !metadata.containsKey("Course")) {
                metadata.put("Course", val);
            } else if (id.contains("lbltitle") && !metadata.containsKey("Title")) {
                metadata.put("Title", val);
            } else if (id.contains("lbltotalmarks") && !metadata.containsKey("Total Marks")) {
                metadata.put("Total Marks", val);
            } else if (id.contains("lbldeadline") && !metadata.containsKey("Deadline")) {
                metadata.put("Deadline", val);
            } else if (id.contains("lblstatus") && !metadata.containsKey("Status")) {
                metadata.put("Status", val);
            } else if (id.contains("lblsubmittedfile") && !metadata.containsKey("Submitted File")) {
                submittedFilename = val;
                metadata.put("Submitted File", val);
            } else if (id.contains("lblsubmissiondate") && !metadata.containsKey("Submission Date")) {
                submissionDate = val;
                metadata.put("Submission Date", val);
            }
        }

        // Search in all anchors for potential download files
        for (Element a : doc.select("a[href]")) {
            String href = a.attr("href");
            String aText = a.text().trim();
            if (href.toLowerCase().contains("assignmentfiles.aspx") || href.toLowerCase().contains("courseportalassignmentfiles.aspx")) {
                boolean exists = false;
                for (AttachmentInfo exist : teacherAttachments) {
                    if (exist.href().equals(href)) { exists = true; break; }
                }
                if (!exists) {
                    teacherAttachments.add(new AttachmentInfo(aText.isEmpty() ? "Download Assignment File" : aText, href));
                }
            }
        }

        if (metadata.isEmpty()) {
            throw new RuntimeException("Could not parse test details from page.");
        }

        // 1. Info Card
        VBox scorecard = new VBox(20);
        scorecard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:12;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:12;-fx-padding:24;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.03),15,0,0,3);");
        
        HBox scoreHeader = new HBox(12);
        scoreHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label statusBadge = new Label("SUBJECTIVE TEST");
        statusBadge.getStyleClass().add("badge-info");
        
        scoreHeader.getChildren().addAll(titleLabel, statusBadge);
        scorecard.getChildren().add(scoreHeader);

        VBox detailBox = new VBox(0);
        detailBox.setStyle("-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-background-radius:8;-fx-background-color: -color-bg-main;");
        
        int rowIndex = 0;
        for (java.util.Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            
            if (key.length() > 50 || val.length() > 300) continue;
            if (key.toLowerCase().contains("validation") || key.toLowerCase().contains("error")) continue;
            
            boolean bg = (rowIndex % 2 == 0);
            detailBox.getChildren().add(resultRow(key, val, bg));
            rowIndex++;
        }
        
        scorecard.getChildren().add(detailBox);
        wrapper.getChildren().add(scorecard);

        // 2. Attachments Card
        if (!teacherAttachments.isEmpty()) {
            VBox attachmentsCard = new VBox(12);
            attachmentsCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:12;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:12;-fx-padding:20;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");
            
            Label attachTitle = new Label("Attachments / Instructions Files");
            attachTitle.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
            attachmentsCard.getChildren().add(attachTitle);
            
            VBox fileList = new VBox(8);
            for (AttachmentInfo attach : teacherAttachments) {
                HBox fileRow = new HBox(12);
                fileRow.setAlignment(Pos.CENTER_LEFT);
                fileRow.setStyle("-fx-padding:8 12;-fx-background-color: -color-bg-main;-fx-background-radius:6;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:6;");
                
                Label fileIcon = new Label("📄");
                fileIcon.setStyle("-fx-font-size:16px;");
                
                Label fileName = new Label(attach.filename());
                fileName.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-main;-fx-font-weight:bold;");
                HBox.setHgrow(fileName, Priority.ALWAYS);
                
                Button dlBtn = new Button("Download");
                dlBtn.setCursor(javafx.scene.Cursor.HAND);
                dlBtn.setStyle("-fx-background-color: -color-accent;-fx-text-fill: -color-bg-main;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:4 10;");
                
                final String finalCourse = metadata.getOrDefault("Course", "Subjective");
                dlBtn.setOnAction(e -> triggerAssignmentDownload(attach.href(), attach.filename(), finalCourse, dlBtn));
                applyOfflineStateIfOffline(dlBtn, "Download");
                uploadButtons.add(dlBtn);
                
                fileRow.getChildren().addAll(fileIcon, fileName, dlBtn);
                fileList.getChildren().add(fileRow);
            }
            attachmentsCard.getChildren().add(fileList);
            wrapper.getChildren().add(attachmentsCard);
        }

        // 3. Submission Card
        VBox submissionCard = new VBox(14);
        submissionCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:12;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:12;-fx-padding:20;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");
        
        Label subTitle = new Label("Your Submission");
        subTitle.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
        submissionCard.getChildren().add(subTitle);
        
        boolean hasSubmitted = (submittedFilename != null && !submittedFilename.isEmpty() && !submittedFilename.equals("-") && !submittedFilename.toLowerCase().contains("not submitted"));
        
        if (hasSubmitted) {
            VBox subInfo = new VBox(8);
            subInfo.getStyleClass().add("panel-info-success");
            
            HBox statusRow = new HBox(8);
            statusRow.setAlignment(Pos.CENTER_LEFT);
            Label checkIcon = new Label("✅");
            checkIcon.setStyle("-fx-font-size:16px;");
            Label statusLbl = new Label("Submitted Successfully");
            statusLbl.getStyleClass().add("status-success");
            statusLbl.setStyle("-fx-font-size:13px;");
            statusRow.getChildren().addAll(checkIcon, statusLbl);
            
            Label fileLbl = new Label("File: " + submittedFilename);
            fileLbl.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-main;-fx-font-weight:bold;");
            
            subInfo.getChildren().addAll(statusRow, fileLbl);
            if (submissionDate != null && !submissionDate.isEmpty()) {
                Label dateLbl = new Label("Submitted on: " + submissionDate);
                dateLbl.getStyleClass().add("status-success");
                dateLbl.setStyle("-fx-font-size:11px;");
                subInfo.getChildren().add(dateLbl);
            }
            submissionCard.getChildren().add(subInfo);
        } else {
            VBox noSubInfo = new VBox(8);
            noSubInfo.getStyleClass().add("panel-info-danger");
            
            HBox statusRow = new HBox(8);
            statusRow.setAlignment(Pos.CENTER_LEFT);
            Label warningIcon = new Label("⚠️");
            warningIcon.setStyle("-fx-font-size:16px;");
            Label statusLbl = new Label("Not Submitted");
            statusLbl.getStyleClass().add("status-error");
            statusLbl.setStyle("-fx-font-size:13px;");
            statusRow.getChildren().addAll(warningIcon, statusLbl);
            
            Label descLbl = new Label("You have not uploaded any solution file for this test yet.");
            descLbl.getStyleClass().add("status-error");
            descLbl.setStyle("-fx-font-size:11px;");
            
            noSubInfo.getChildren().addAll(statusRow, descLbl);
            submissionCard.getChildren().add(noSubInfo);
        }
        
        Element fileInput = doc.select("input[type=file]").first();
        boolean openForUpload = (fileInput != null);
        
        if (openForUpload) {
            VBox uploadForm = new VBox(10);
            uploadForm.setStyle("-fx-padding:16 0 0 0;-fx-border-color: -color-border;-fx-border-width:1 0 0 0;");
            
            Label selectFileLbl = new Label(hasSubmitted ? "Update Submission File:" : "Upload Submission File:");
            selectFileLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
            
            Label selectedFileLabel = new Label("No file chosen");
            selectedFileLabel.setStyle("-fx-font-size:11px;-fx-text-fill: -color-text-muted;");
            
            Button browseBtn = new Button("📁 Choose File...");
            browseBtn.setCursor(javafx.scene.Cursor.HAND);
            browseBtn.setStyle("-fx-background-color: -color-bg-main;-fx-text-fill: -color-text-main;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-font-size:11px;-fx-padding:5 10;");
            applyOfflineStateIfOffline(browseBtn, "📁 Choose File...");
            uploadButtons.add(browseBtn);
            
            final File[] selectedFileHolder = new File[1];
            browseBtn.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Solution File to Upload");
                fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Files", "*.*"),
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                    new FileChooser.ExtensionFilter("ZIP Files", "*.zip"),
                    new FileChooser.ExtensionFilter("Word Documents", "*.doc", "*.docx")
                );
                File selected = fileChooser.showOpenDialog(context.stage());
                if (selected != null) {
                    selectedFileHolder[0] = selected;
                    selectedFileLabel.setText(selected.getName() + " (" + formatFileSize(selected.length()) + ")");
                    selectedFileLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#15803d;-fx-font-weight:bold;");
                }
            });
            
            Button submitBtn = new Button(hasSubmitted ? "Change File Submission" : "Submit File");
            submitBtn.setCursor(javafx.scene.Cursor.HAND);
            submitBtn.setStyle("-fx-background-color: -color-accent;-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:4;-fx-padding:6 14;");
            submitBtn.setDisable(true);
            
            selectedFileLabel.textProperty().addListener((o, oldVal, newVal) -> {
                submitBtn.setDisable(selectedFileHolder[0] == null);
            });
            
            submitBtn.setOnAction(e -> {
                if (selectedFileHolder[0] != null) {
                    triggerNativeSubjectiveUpload(relativeUrl, selectedFileHolder[0], submitBtn, title);
                }
            });
            
            HBox browseRow = new HBox(8, browseBtn, selectedFileLabel);
            browseRow.setAlignment(Pos.CENTER_LEFT);
            
            HBox submitRow = new HBox(submitBtn);
            submitRow.setStyle("-fx-padding:8 0 0 0;");
            
            uploadForm.getChildren().addAll(selectFileLbl, browseRow, submitRow);
            submissionCard.getChildren().add(uploadForm);
            
            uploadButtons.add(submitBtn);
            applyOfflineStateIfOffline(submitBtn, submitBtn.getText());
        } else {
            Label closedLbl = new Label("🔒 Test submission is closed.");
            closedLbl.setStyle("-fx-text-fill:#b91c1c;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:10 0 0 0;");
            submissionCard.getChildren().add(closedLbl);
        }
        
        wrapper.getChildren().add(submissionCard);
        
        sp.setContent(wrapper);
        return sp;
    }

    private void triggerNativeSubjectiveUpload(String submitUrl, File file, Button btn, String title) {
        if (!context.isOnline()) {
            context.notificationService().showError("Upload Offline", "Cannot upload files in offline mode.");
            return;
        }
        
        if (btn != null) {
            Platform.runLater(() -> btn.setDisable(true));
        }
        showLoading("Uploading solution file...");
        
        new Thread(() -> {
            try {
                PortalRepository.UploadResult result = context.portalRepository().uploadAssignment(submitUrl, file);
                
                Platform.runLater(() -> {
                    if (btn != null) btn.setDisable(false);
                    
                    if (result instanceof PortalRepository.UploadResult.Success) {
                        context.notificationService().showSuccess("Upload Complete", "Subjective solution uploaded successfully!");
                        loadSubjectivePaperView(submitUrl, title);
                    } else {
                        loadSubjectivePaperView(submitUrl, title);
                        
                        if (result instanceof PortalRepository.UploadResult.NetworkError) {
                            context.notificationService().showError("Upload Failed", "Network connection error. Please try again.");
                        } else if (result instanceof PortalRepository.UploadResult.Timeout) {
                            context.notificationService().showError("Upload Failed", "Request timed out. The server might be slow or file might be too large.");
                        } else if (result instanceof PortalRepository.UploadResult.Rejected rejected) {
                            context.notificationService().showError("Upload Rejected", rejected.reason());
                        } else if (result instanceof PortalRepository.UploadResult.Error err) {
                            context.notificationService().showError("Upload Failed", err.message());
                        }
                    }
                });
            } catch (Exception ex) {
                ErrorReporter.logError("CoursePortalTabView#triggerNativeSubjectiveUpload", ex);
                Platform.runLater(() -> {
                    if (btn != null) btn.setDisable(false);
                    loadSubjectivePaperView(submitUrl, title);
                    context.notificationService().showError("Upload Error", "An unexpected error occurred: " + ex.getMessage());
                });
            }
        }).start();
    }
private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void loadAssignmentsData(boolean forceRefresh) {
        showLoading("Loading Assignments Summary...");
        new Thread(() -> {
            try {
                String html = null;
                if (!forceRefresh) {
                    html = context.dataCacheService().getCachedHtml("CoursePortal.aspx").orElse(null);
                }
                if (html == null) {
                    html = context.fetchAndCacheHtml("CoursePortal.aspx");
                    if (html == null) {
                        html = context.dataCacheService().getCachedHtml("CoursePortal.aspx").orElse(null);
                    }
                }

                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Unable to load Assignments Summary",
                            "Failed to connect to the portal database. Please check your internet connection and try again."
                        ));
                    });
                    return;
                }

                try {
                    java.nio.file.Files.writeString(java.nio.file.Path.of(com.assignly.util.AppDirectoryHelper.getAppDataDir(), "cts_assignments_raw.html"), html);
                } catch (IOException ex) {
                    ErrorReporter.logError("CoursePortalTabView#loadAssignmentsData write cts_assignments_raw.html", ex);
                }

                final String finalHtml = html;
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    try {
                        contentPane.getChildren().add(buildAssignmentsView(finalHtml));
                    } catch (Exception ex) {
                        ErrorReporter.notify(context, "Failed to parse Assignments Summary. Portal layout may have changed.", "CoursePortalTabView#loadAssignmentsData", ex);
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Parsing Error",
                            "Failed to read the Assignments Summary table cleanly."
                        ));
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load Assignments Summary. Please try again.", "CoursePortalTabView#loadAssignmentsData", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while loading assignments: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private ScrollPane buildAssignmentsView(String html) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        Document doc = Jsoup.parse(html);
        
        String headingText = resolvePortalHeading(doc, "Course Portal Summary");

        Label subHeading = new Label(headingText);
        subHeading.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;-fx-padding:0 0 4 0;");
        content.getChildren().add(subHeading);

        List<PortalAssignment> list = parseAssignmentsFromHtml(html, "CoursePortal.aspx");
        if (list.isEmpty()) {
            renderEmptyAssignmentsState(content);
        } else {
            HBox explorerBar = new HBox(12);
            explorerBar.setAlignment(Pos.CENTER_LEFT);
            explorerBar.setPadding(new Insets(4, 0, 4, 0));

            TextField searchField = new TextField(assignmentsSearchQuery);
            searchField.setPromptText("Search assignments by title or course...");
            HBox.setHgrow(searchField, Priority.ALWAYS);
            searchField.setStyle("-fx-background-color: -color-bg-card; -fx-border-color: -color-border; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: -color-text-main; -fx-padding: 8 12;");
            explorerBar.getChildren().add(searchField);
            content.getChildren().add(explorerBar);

            FlowPane flow = new FlowPane(16, 16);
            flow.setPadding(new Insets(8, 0, 16, 0));

            Runnable filterAndRender = () -> {
                flow.getChildren().clear();
                List<PortalAssignment> filtered = new ArrayList<>();
                for (PortalAssignment pa : list) {
                    if (assignmentsSearchQuery.isEmpty() || 
                        pa.title.toLowerCase().contains(assignmentsSearchQuery.toLowerCase()) || 
                        pa.course.toLowerCase().contains(assignmentsSearchQuery.toLowerCase())) {
                        filtered.add(pa);
                    }
                }
                
                if (filtered.isEmpty()) {
                    VBox emptyCard = new VBox(12);
                    emptyCard.setUserData("empty");
                    emptyCard.setAlignment(Pos.CENTER);
                    emptyCard.setPadding(new Insets(32, 24, 32, 24));
                    emptyCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;");
                    Label icon = new Label("🔍");
                    icon.setStyle("-fx-font-size:24px;");
                    Label label = new Label("No matching assignments found");
                    label.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:12px;-fx-font-weight:bold;");
                    emptyCard.getChildren().addAll(icon, label);
                    flow.getChildren().add(emptyCard);
                } else {
                    for (PortalAssignment pa : filtered) {
                        flow.getChildren().add(buildAssignmentCard(pa, "CoursePortal.aspx"));
                    }
                }
                adjustFlowGrid(flow, 320, 450, 16);
            };

            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                assignmentsSearchQuery = newVal.trim();
                filterAndRender.run();
            });

            flow.widthProperty().addListener((obs, oldVal, newVal) -> adjustFlowGrid(flow, 320, 450, 16));
            Platform.runLater(() -> {
                filterAndRender.run();
                adjustFlowGrid(flow, 320, 450, 16);
            });

            content.getChildren().add(flow);
        }

        sp.setContent(content);
        return sp;
    }

    private void renderEmptyAssignmentsState(VBox content) {
        VBox emptyCard = new VBox(12);
        emptyCard.setAlignment(Pos.CENTER);
        emptyCard.setPadding(new Insets(32, 24, 32, 24));
        emptyCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size:28px;");

        Label label = new Label("No Assignments Found");
        label.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:13px;-fx-font-weight:bold;");

        Label desc = new Label("There are currently no active or closed assignments listed in your course portal.");
        desc.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:11px;-fx-text-alignment:center;");
        desc.setWrapText(true);

        emptyCard.getChildren().addAll(icon, label, desc);
        content.getChildren().add(emptyCard);
    }

    private void triggerAssignmentDownload(String href, String assignmentTitle, String courseTitle, Button btn) {
        if (btn != null) {
            Platform.runLater(() -> btn.setDisable(true));
        }

        new Thread(() -> {
            try {
                PortalRepository.DownloadResult result = context.portalRepository().downloadAssignment(href);

                Platform.runLater(() -> {
                    if (btn != null) btn.setDisable(false);

                    if (result instanceof PortalRepository.DownloadResult.Success success) {
                        String extension = "";
                        String fileName = success.fileName();
                        if (fileName != null && !fileName.isBlank()) {
                            int dotIdx = fileName.lastIndexOf('.');
                            if (dotIdx > 0) {
                                extension = fileName.substring(dotIdx);
                            }
                        }
                        if (extension.isEmpty() && success.mimeType() != null) {
                            String extFromMime = context.portalRepository().getExtensionFromMimeType(success.mimeType());
                            if (extFromMime != null && !extFromMime.isEmpty() && !extFromMime.equals("bin")) {
                                extension = "." + extFromMime;
                            }
                        }
                        if (extension.isEmpty()) {
                            extension = ".pdf";
                        }

                        final String finalFilename = getCustomFilename(assignmentTitle, courseTitle, extension);
                        final byte[] fileBytes = success.bytes();

                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Save Assignment Document");
                        fileChooser.setInitialFileName(finalFilename);

                        if (!extension.isEmpty()) {
                            fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter(extension.toUpperCase().substring(1) + " Files", "*" + extension)
                            );
                        }
                        fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("All Files", "*.*")
                        );

                        File file = fileChooser.showSaveDialog(context.stage());
                        if (file != null) {
                            new Thread(() -> {
                                try {
                                    java.nio.file.Files.write(file.toPath(), fileBytes);
                                    Platform.runLater(() -> context.notificationService().showSuccess("Download Complete", file.getName() + " downloaded"));
                                } catch (Exception ex) {
                                    ErrorReporter.logError("CoursePortalTabView#triggerAssignmentDownload save file", ex);
                                    Platform.runLater(() -> context.notificationService().showError("Download Failed", "Error saving file: " + ex.getMessage()));
                                }
                            }).start();
                        }
                    } else if (result instanceof PortalRepository.DownloadResult.NetworkError) {
                        context.notificationService().showError("Download Error", "Network connection failed. Please check your internet connection.");
                    } else if (result instanceof PortalRepository.DownloadResult.Rejected rejected) {
                        context.notificationService().showError("Download Rejected", rejected.reason());
                    } else if (result instanceof PortalRepository.DownloadResult.Error err) {
                        context.notificationService().showError("Download Error", err.message());
                    }
                });
            } catch (Exception ex) {
                ErrorReporter.logError("CoursePortalTabView#triggerAssignmentDownload", ex);
                Platform.runLater(() -> {
                    if (btn != null) btn.setDisable(false);
                    context.notificationService().showError("Download Error", "An unexpected error occurred: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void triggerAssignmentUpload(String submitUrl, Button btn) {
        if (!context.isOnline()) {
            context.notificationService().showError("Upload Offline", "Cannot upload assignments in offline mode.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Assignment File to Upload");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Files", "*.*"),
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
            new FileChooser.ExtensionFilter("ZIP Files", "*.zip"),
            new FileChooser.ExtensionFilter("Word Documents", "*.doc", "*.docx")
        );
        
        File selectedFile = fileChooser.showOpenDialog(context.stage());
        if (selectedFile == null) {
            return;
        }

        if (btn != null) {
            Platform.runLater(() -> btn.setDisable(true));
        }
        showLoading("Uploading assignment file...");

        new Thread(() -> {
            try {
                PortalRepository.UploadResult result = context.portalRepository().uploadAssignment(submitUrl, selectedFile);

                Platform.runLater(() -> {
                    if (btn != null) btn.setDisable(false);
                    
                    if (result instanceof PortalRepository.UploadResult.Success) {
                        context.notificationService().showSuccess("Upload Complete", "Assignment uploaded successfully!");
                        if ("pending_assignments".equals(activeTab)) {
                            loadPendingAssignmentsData(true);
                        } else {
                            loadAssignmentsData(true);
                        }
                    } else {
                        if ("pending_assignments".equals(activeTab)) {
                            loadPendingAssignmentsData(true);
                        } else {
                            loadAssignmentsData(true);
                        }
                        if (result instanceof PortalRepository.UploadResult.NetworkError) {
                            context.notificationService().showError("Upload Failed", "Network connection error. Please try again.");
                        } else if (result instanceof PortalRepository.UploadResult.Timeout) {
                            context.notificationService().showError("Upload Failed", "Request timed out. The server might be slow or file might be too large.");
                        } else if (result instanceof PortalRepository.UploadResult.Rejected rejected) {
                            context.notificationService().showError("Upload Rejected", rejected.reason());
                        } else if (result instanceof PortalRepository.UploadResult.Error err) {
                            context.notificationService().showError("Upload Failed", err.message());
                        }
                    }
                });
            } catch (Exception ex) {
                ErrorReporter.logError("CoursePortalTabView#triggerAssignmentUpload", ex);
                Platform.runLater(() -> {
                    if (btn != null) btn.setDisable(false);
                    if ("pending_assignments".equals(activeTab)) {
                        loadPendingAssignmentsData(true);
                    } else {
                        loadAssignmentsData(true);
                    }
                    context.notificationService().showError("Upload Error", "An unexpected error occurred: " + ex.getMessage());
                });
            }
        }).start();
    }


    private double assignmentsColW(String h, int i, int total) {
        String l = h.toLowerCase();
        if (l.contains("#")) return 40;
        if (l.contains("course")) return 240;
        if (l.contains("title")) return 200;
        if (l.contains("start")) return 110;
        if (l.contains("deadline") || l.contains("due")) return 140;
        if (l.contains("submission")) return 140;
        if (l.contains("status")) return 130;
        if (l.contains("download") || l.contains("dowload")) return 100;
        if (l.contains("submit")) return 120;
        return 100;
    }

    private VBox buildAssignmentCard(PortalAssignment pa, String pageSource) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(320);
        card.setMaxWidth(320);

        long remainingDays = calculateRemainingDays(pa.deadline);
        String leftBorderColor = "-color-border";

        boolean isClosed = pa.status.toLowerCase().contains("closed");
        boolean isSubmitted = pa.submission.toLowerCase().contains("submitted") && !pa.submission.toLowerCase().contains("not");

        if (isClosed) {
            leftBorderColor = "-color-border";
        } else if (isSubmitted) {
            leftBorderColor = "#22c55e"; // Green for completed
        } else {
            // Pending and open
            if (remainingDays < 1) {
                leftBorderColor = "#ef4444"; // Red for < 1 day
            } else if (remainingDays < 3) {
                leftBorderColor = "#eab308"; // Yellow for < 3 days
            } else {
                leftBorderColor = "#14b8a6"; // Teal for others
            }
        }

        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: -color-border -color-border -color-border " + leftBorderColor + ";" +
            "-fx-border-width: 1 1 1 4;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.02), 8, 0, 0, 3);"
        );

        Label courseBadge = new Label(pa.course.toUpperCase());
        courseBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: -color-accent; -fx-background-color: rgba(20, 184, 166, 0.08); -fx-padding: 3 8; -fx-background-radius: 6;");
        courseBadge.setWrapText(true);

        Label titleLbl = new Label(pa.title);
        titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        titleLbl.setWrapText(true);

        VBox infoBox = new VBox(6);
        infoBox.setStyle("-fx-background-color: -color-bg-main; -fx-padding: 10; -fx-background-radius: 8;");

        Label startLbl = new Label("📅 Assigned: " + pa.startDate);
        startLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");

        String deadlineText = "⏰ Due:      " + pa.deadline;
        if (!isClosed && !isSubmitted && remainingDays != 999) {
            if (remainingDays < 0) {
                deadlineText += " (Overdue)";
            } else if (remainingDays == 0) {
                deadlineText += " (Due today)";
            } else {
                deadlineText += " (" + remainingDays + " days left)";
            }
        }
        Label endLbl = new Label(deadlineText);
        endLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");

        infoBox.getChildren().addAll(startLbl, endLbl);

        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Label subBadge = new Label(pa.submission.toUpperCase());
        if (isSubmitted) {
            subBadge.getStyleClass().add("badge-success");
        } else {
            subBadge.getStyleClass().add("badge-danger");
        }

        Label stateBadge = new Label(pa.status.toUpperCase());
        if (isClosed) {
            stateBadge.getStyleClass().add("badge-muted");
        } else {
            stateBadge.getStyleClass().add("badge-info");
        }

        statusRow.getChildren().addAll(subBadge, stateBadge);

        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);
        
        if (pa.downloadUrl != null && !pa.downloadUrl.isEmpty()) {
            Button dlBtn = new Button("Download");
            dlBtn.setCursor(javafx.scene.Cursor.HAND);
            dlBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: -color-bg-main; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;");
            dlBtn.setOnAction(e -> triggerAssignmentDownload(pa.downloadUrl, pa.title, pa.course, dlBtn));
            applyOfflineStateIfOffline(dlBtn, "Download");
            uploadButtons.add(dlBtn);
            actionsRow.getChildren().add(dlBtn);
        }

        if (!isClosed && pa.submitUrl != null && !pa.submitUrl.isEmpty()) {
            Button subBtn = new Button("Submit");
            subBtn.setCursor(javafx.scene.Cursor.HAND);
            subBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;");
            subBtn.setOnAction(e -> triggerAssignmentUpload(pa.submitUrl, subBtn));
            uploadButtons.add(subBtn);
            applyOfflineStateIfOffline(subBtn, "Submit");
            actionsRow.getChildren().add(subBtn);
        }

        card.getChildren().addAll(courseBadge, titleLbl, infoBox, statusRow, actionsRow);

        // Highlight functionality
        String highlight = context.getPendingSearchHighlight();
        if (highlight != null && !highlight.isBlank() && 
            (pa.title.toLowerCase().contains(highlight.toLowerCase()) || 
             pa.course.toLowerCase().contains(highlight.toLowerCase()))) {
            card.setStyle(
                card.getStyle() +
                "-fx-background-color: rgba(20, 184, 166, 0.12);" +
                "-fx-border-color: -color-accent;" +
                "-fx-border-width: 1 1 1 4;"
            );
            Platform.runLater(() -> card.requestFocus());
            context.clearPendingSearchHighlight();
        }

        // Hover translation and shadow glow animations
        card.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(120), card);
            tt.setToY(-2);
            tt.play();
            card.setStyle(
                card.getStyle() +
                "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.08), 8, 0, 0, 4);"
            );
        });
        card.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(120), card);
            tt.setToY(0);
            tt.play();
            card.setStyle(
                card.getStyle() +
                "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.02), 8, 0, 0, 3);"
            );
        });

        return card;
    }

    // ==================== COURSE CONTENTS ====================
    private void loadCourseContentsData(boolean forceRefresh) {
        showLoading("Loading Course Contents...");
        new Thread(() -> {
            try {
                String html = null;
                if (!forceRefresh) {
                    html = context.dataCacheService().getCachedHtml("CoursePortalContentsSummary.aspx").orElse(null);
                }
                if (html == null) {
                    html = context.fetchAndCacheHtml("CoursePortalContentsSummary.aspx");
                    if (html == null) {
                        html = context.dataCacheService().getCachedHtml("CoursePortalContentsSummary.aspx").orElse(null);
                    }
                }

                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Unable to load Course Contents",
                            "Failed to connect to the portal. Please check your internet connection and try again."
                        ));
                    });
                    return;
                }

                try {
                    java.nio.file.Files.writeString(java.nio.file.Path.of(com.assignly.util.AppDirectoryHelper.getAppDataDir(), "course_contents_raw.html"), html);
                } catch (IOException ex) {
                    ErrorReporter.logError("CoursePortalTabView#loadCourseContentsData write course_contents_raw.html", ex);
                }

                List<String[]> courses = context.portalRepository().parseDropdownOptions(html, "course");
                if (courses.isEmpty()) {
                    courses.addAll(context.portalRepository().parseDropdownOptions(html, "ddl"));
                }

                prefetchCourseContentsFiles(courses, html);

                final String finalHtml = html;
                final List<String[]> finalCourses = courses;

                Platform.runLater(() -> {
                    try {
                        contentPane.getChildren().clear();
                        if (finalCourses.isEmpty()) {
                            contentPane.getChildren().add(buildErrorState(
                                "No Courses Found",
                                "No registered courses found in the Course Contents portal."
                            ));
                        } else {
                            contentPane.getChildren().add(buildCourseContentsSelectorView(finalCourses, finalHtml));
                        }
                    } catch (Exception ex) {
                        ErrorReporter.notify(context, "Failed to render Course Contents. Portal layout may have changed.", "CoursePortalTabView#loadCourseContentsData", ex);
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "UI Render Error",
                            "Failed to render the subject selection list: " + ex.getMessage()
                        ));
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load Course Contents. Please try again.", "CoursePortalTabView#loadCourseContentsData", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while loading contents: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private ScrollPane buildCourseContentsSelectorView(List<String[]> courses, String rawHtml) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        if (!context.isOnline()) {
            content.getChildren().add(buildOfflineBanner());
        }

        Label subHeading = new Label("Course Contents (Lecture Notes, Quiz/Assignment Solutions, Research Papers etc)");
        subHeading.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;-fx-padding:0 0 4 0;");
        content.getChildren().add(subHeading);

        FlowPane flow = new FlowPane(16, 16);
        flow.setPadding(new Insets(8, 0, 16, 0));
        flow.widthProperty().addListener((obs, oldVal, newVal) -> adjustFlowGrid(flow, 300, 450, 16));
        Platform.runLater(() -> adjustFlowGrid(flow, 300, 450, 16));

        for (String[] course : courses) {
            String courseId = course[0];
            String courseTitle = course[1];

            List<CourseFile> files = getCachedCourseFiles(courseId);
            int filesCount = files.size();

            String lastUpload = "N/A";
            LocalDate maxDate = LocalDate.MIN;
            List<String> topics = new ArrayList<>();

            for (CourseFile f : files) {
                LocalDate d = parseUploadDate(f.uploadDate);
                if (d.isAfter(maxDate)) {
                    maxDate = d;
                    lastUpload = f.uploadDate;
                }
                if (topics.size() < 3 && !f.title.isBlank()) {
                    topics.add(f.title);
                }
            }

            VBox card = new VBox(12);
            card.getStyleClass().add("card");
            card.setPadding(new Insets(16));
            card.setPrefWidth(300);
            card.setMaxWidth(300);
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-border-color: -color-border;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.02), 8, 0, 0, 3);"
            );

            Label titleLbl = new Label(courseTitle);
            titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
            titleLbl.setWrapText(true);
            VBox.setVgrow(titleLbl, Priority.ALWAYS);

            VBox infoCol = new VBox(4);
            infoCol.setStyle("-fx-background-color: -color-bg-main; -fx-padding: 8; -fx-background-radius: 6;");

            Label countLbl = new Label("📁 Files: " + (filesCount == 0 && !context.isOnline() ? "No cached files" : filesCount + " files"));
            countLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");

            Label dateLbl = new Label("📅 Updated: " + lastUpload);
            dateLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");

            infoCol.getChildren().addAll(countLbl, dateLbl);

            VBox topicsBox = new VBox(4);
            if (!topics.isEmpty()) {
                Label topicsHeader = new Label("RECENT TOPICS");
                topicsHeader.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
                topicsBox.getChildren().add(topicsHeader);
                for (String t : topics) {
                    Label topicLbl = new Label("• " + (t.length() > 32 ? t.substring(0, 30) + "..." : t));
                    topicLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: -color-text-main;");
                    topicLbl.setWrapText(true);
                    topicsBox.getChildren().add(topicLbl);
                }
            }

            Button viewBtn = new Button(context.isOnline() ? "View Files" : "Explore Offline");
            viewBtn.setCursor(javafx.scene.Cursor.HAND);
            viewBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: -color-bg-main; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;");
            viewBtn.setOnAction(e -> loadCourseContentsDetails(courseId, courseTitle, rawHtml));

            HBox bottomRow = new HBox(viewBtn);
            bottomRow.setAlignment(Pos.CENTER_RIGHT);

            card.getChildren().addAll(titleLbl, infoCol, topicsBox, bottomRow);

            // Hover effects
            card.setOnMouseEntered(e -> {
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(120), card);
                tt.setToY(-2);
                tt.play();
                card.setStyle(
                    "-fx-background-color: -color-bg-card;" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-radius: 12;" +
                    "-fx-border-color: -color-accent;" +
                    "-fx-border-width: 1;" +
                    "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.08), 8, 0, 0, 4);"
                );
            });
            card.setOnMouseExited(e -> {
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(120), card);
                tt.setToY(0);
                tt.play();
                card.setStyle(
                    "-fx-background-color: -color-bg-card;" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-radius: 12;" +
                    "-fx-border-color: -color-border;" +
                    "-fx-border-width: 1;" +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.02), 8, 0, 0, 3);"
                );
            });

            flow.getChildren().add(card);
        }

        content.getChildren().add(flow);
        sp.setContent(content);
        return sp;
    }

    private List<CourseFile> getCachedCourseFiles(String courseId) {
        String detailHtml = context.dataCacheService().getCachedHtml("CoursePortalContentsSummary.aspx_" + courseId).orElse(null);
        List<CourseFile> list = new ArrayList<>();
        if (detailHtml == null) return list;
        try {
            Document doc = Jsoup.parse(detailHtml);
            Element table = findMainGridTable(doc, "DataContent_gvPortalSummary");
            if (table != null) {
                Elements rows = table.select("tr");
                for (int r = 1; r < rows.size(); r++) {
                    Element row = rows.get(r);
                    Elements cells = row.select("td");
                    if (cells.size() >= 3) {
                        CourseFile cf = new CourseFile();
                        cf.title = cells.get(1).text().trim().replaceAll("\\s+", " ");
                        cf.description = cells.size() > 2 ? cells.get(2).text().trim().replaceAll("\\s+", " ") : "";
                        if (cells.size() > 3) {
                            cf.uploadDate = cells.get(3).text().trim();
                        }
                        Element aTag = cellSelectDownload(cells);
                        if (aTag != null) {
                            cf.href = aTag.attr("href");
                        }
                        list.add(cf);
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    private Element cellSelectDownload(Elements cells) {
        for (Element cell : cells) {
            Element aTag = cell.select("a").first();
            if (aTag != null && !aTag.attr("href").isEmpty() && aTag.attr("href").toLowerCase().contains("download")) {
                return aTag;
            }
        }
        return null;
    }

    private void loadCourseContentsDetails(String courseId, String courseTitle, String pageHtml) {
        showLoading("Loading contents for " + courseTitle + "...");
        currentCourseId = courseId;
        currentCourseTitle = courseTitle;
        currentOriginalPageHtml = pageHtml;

        new Thread(() -> {
            try {
                String resultHtml = null;
                if (context.isOnline()) {
                    String ddName = context.portalRepository().findDropdownName(pageHtml, "course");
                    if (ddName == null) ddName = context.portalRepository().findDropdownName(pageHtml, "ddl");
                    if (ddName != null) {
                        resultHtml = context.portalRepository().postbackWithDropdown("CoursePortalContentsSummary.aspx", ddName, courseId);
                        if (resultHtml != null) {
                            context.dataCacheService().cacheHtml("CoursePortalContentsSummary.aspx_" + courseId, resultHtml);
                        }
                    }
                }

                if (resultHtml == null) {
                    resultHtml = context.dataCacheService().getCachedHtml("CoursePortalContentsSummary.aspx_" + courseId).orElse(null);
                }

                if (resultHtml == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Content Not Available",
                            "This course content has not been cached yet and you are currently offline."
                        ));
                    });
                    return;
                }

                currentCourseHtml = resultHtml;

                // Parse files list
                List<CourseFile> files = new ArrayList<>();
                Document doc = Jsoup.parse(resultHtml);
                Element table = findMainGridTable(doc, "DataContent_gvPortalSummary");
                if (table != null) {
                    Elements rows = table.select("tr");
                    for (int r = 1; r < rows.size(); r++) {
                        Element row = rows.get(r);
                        Elements cells = row.select("td");
                        if (cells.size() >= 3) {
                            CourseFile cf = new CourseFile();
                            cf.title = cells.get(1).text().trim().replaceAll("\\s+", " ");
                            cf.description = cells.size() > 2 ? cells.get(2).text().trim().replaceAll("\\s+", " ") : "";
                            if (cells.size() > 3) {
                                cf.uploadDate = cells.get(3).text().trim();
                            }
                            Element aTag = cellSelectDownload(cells);
                            if (aTag != null) {
                                cf.href = aTag.attr("href");
                            }
                            files.add(cf);
                        }
                    }
                }

                synchronized (rawCourseFiles) {
                    rawCourseFiles.clear();
                    rawCourseFiles.addAll(files);
                }

                final String finalResult = resultHtml;
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    try {
                        contentPane.getChildren().add(buildCourseContentsDetailView(courseId, courseTitle, finalResult, pageHtml));
                    } catch (Exception ex) {
                        ErrorReporter.notify(context, "Failed to parse Course Contents. Portal layout may have changed.", "CoursePortalTabView#loadCourseContentsDetails", ex);
                        contentPane.getChildren().add(buildErrorState(
                            "Parsing Error",
                            "Failed to read the course content table cleanly."
                        ));
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load Course Contents. Please try again.", "CoursePortalTabView#loadCourseContentsDetails", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while fetching details: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private ScrollPane buildCourseContentsDetailView(String courseId, String courseTitle, String html, String originalPageHtml) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        if (!context.isOnline()) {
            content.getChildren().add(buildOfflineBanner());
        }

        Button backBtn = new Button("← Back to Subjects");
        backBtn.setCursor(javafx.scene.Cursor.HAND);
        backBtn.setStyle("-fx-background-color: -color-bg-card;-fx-text-fill: -color-accent;-fx-border-color: -color-accent;-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:6 12;");
        backBtn.setOnAction(e -> {
            activeTab = ""; // Force reload
            loadTab("portal_contents");
        });

        // Filter out download links to download all
        List<String[]> filesToDownload = new ArrayList<>();
        synchronized (rawCourseFiles) {
            for (CourseFile cf : rawCourseFiles) {
                if (cf.href != null && !cf.href.isEmpty() && cf.href.toLowerCase().contains("download")) {
                    filesToDownload.add(new String[]{cf.href, cf.title, cf.description});
                }
            }
        }

        HBox topBar = new HBox(12);
        topBar.setStyle("-fx-padding:0 0 4 0;");
        topBar.getChildren().add(backBtn);

        if (!filesToDownload.isEmpty()) {
            Button downloadAllBtn = new Button("⬇ Download All (" + filesToDownload.size() + " files)");
            downloadAllBtn.setCursor(javafx.scene.Cursor.HAND);
            downloadAllBtn.setStyle("-fx-background-color: -color-accent;-fx-text-fill: -color-bg-main;-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:6 12;");
            downloadAllBtn.setOnAction(e -> triggerDownloadAll(filesToDownload, courseId, courseTitle, html, originalPageHtml));
            applyOfflineStateIfOffline(downloadAllBtn, "⬇ Download All (" + filesToDownload.size() + " files)");
            uploadButtons.add(downloadAllBtn);
            topBar.getChildren().add(downloadAllBtn);
        }
        content.getChildren().add(topBar);

        Label titleLabel = new Label(courseTitle + " - Course Contents");
        titleLabel.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
        content.getChildren().add(titleLabel);

        // explorerBar building
        HBox explorerBar = new HBox(12);
        explorerBar.setAlignment(Pos.CENTER_LEFT);
        explorerBar.setPadding(new Insets(8, 0, 8, 0));

        TextField searchField = new TextField(ccSearchQuery);
        searchField.setPromptText("Search files by name or description...");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setStyle("-fx-background-color: -color-bg-card; -fx-border-color: -color-border; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: -color-text-main; -fx-padding: 8 12;");

        Button sortDateBtn = new Button("Sort by Date");
        Button sortNameBtn = new Button("Sort by Name");
        sortDateBtn.setCursor(javafx.scene.Cursor.HAND);
        sortNameBtn.setCursor(javafx.scene.Cursor.HAND);

        Button gridBtn = new Button("Grid Mode");
        Button listBtn = new Button("List Mode");
        gridBtn.setCursor(javafx.scene.Cursor.HAND);
        listBtn.setCursor(javafx.scene.Cursor.HAND);

        VBox filesContainer = new VBox(12);
        filesContainer.setFillWidth(true);

        var updateBtnStyles = new Runnable() {
            public void run() {
                if ("Date".equals(ccSortOrder)) {
                    sortDateBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: -color-bg-main; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;");
                    sortNameBtn.setStyle("-fx-background-color: -color-bg-card; -fx-text-fill: -color-text-muted; -fx-border-color: -color-border; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 12;");
                } else {
                    sortNameBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: -color-bg-main; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;");
                    sortDateBtn.setStyle("-fx-background-color: -color-bg-card; -fx-text-fill: -color-text-muted; -fx-border-color: -color-border; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 12;");
                }

                if (ccGridMode) {
                    gridBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: -color-bg-main; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;");
                    listBtn.setStyle("-fx-background-color: -color-bg-card; -fx-text-fill: -color-text-muted; -fx-border-color: -color-border; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 12;");
                } else {
                    listBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: -color-bg-main; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 12;");
                    gridBtn.setStyle("-fx-background-color: -color-bg-card; -fx-text-fill: -color-text-muted; -fx-border-color: -color-border; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 12;");
                }
            }
        };

        updateBtnStyles.run();

        sortDateBtn.setOnAction(e -> {
            ccSortOrder = "Date";
            updateBtnStyles.run();
            renderFilesList(filesContainer);
        });

        sortNameBtn.setOnAction(e -> {
            ccSortOrder = "Name";
            updateBtnStyles.run();
            renderFilesList(filesContainer);
        });

        gridBtn.setOnAction(e -> {
            ccGridMode = true;
            updateBtnStyles.run();
            renderFilesList(filesContainer);
        });

        listBtn.setOnAction(e -> {
            ccGridMode = false;
            updateBtnStyles.run();
            renderFilesList(filesContainer);
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            ccSearchQuery = newVal.trim();
            renderFilesList(filesContainer);
        });

        explorerBar.getChildren().addAll(searchField, sortDateBtn, sortNameBtn, gridBtn, listBtn);
        content.getChildren().add(explorerBar);
        content.getChildren().add(filesContainer);

        // Initial files rendering
        renderFilesList(filesContainer);

        sp.setContent(content);
        return sp;
    }

    private void renderFilesList(VBox filesContainer) {
        filesContainer.getChildren().clear();

        List<CourseFile> filtered = new ArrayList<>();
        synchronized (rawCourseFiles) {
            for (CourseFile cf : rawCourseFiles) {
                if (ccSearchQuery.isEmpty() || 
                    cf.title.toLowerCase().contains(ccSearchQuery.toLowerCase()) || 
                    cf.description.toLowerCase().contains(ccSearchQuery.toLowerCase())) {
                    filtered.add(cf);
                }
            }
        }

        if (ccSortOrder.equals("Date")) {
            filtered.sort((f1, f2) -> {
                LocalDate d1 = parseUploadDate(f1.uploadDate);
                LocalDate d2 = parseUploadDate(f2.uploadDate);
                return d2.compareTo(d1); // Newest first
            });
        } else {
            filtered.sort(Comparator.comparing(f -> f.title.toLowerCase())); // A-Z
        }

        if (filtered.isEmpty()) {
            VBox emptyCard = new VBox(12);
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(32, 24, 32, 24));
            emptyCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;");

            Label icon = new Label("🔍");
            icon.setStyle("-fx-font-size:24px;");

            Label label = new Label("No matching files found");
            label.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:12px;-fx-font-weight:bold;");

            emptyCard.getChildren().addAll(icon, label);
            filesContainer.getChildren().add(emptyCard);
            return;
        }

        if (ccGridMode) {
            FlowPane gridFlow = new FlowPane(16, 16);
            gridFlow.setPadding(new Insets(8, 0, 8, 0));

            for (CourseFile cf : filtered) {
                VBox card = new VBox(10);
                card.getStyleClass().add("card");
                card.setPadding(new Insets(14));
                card.setPrefWidth(280);
                card.setMaxWidth(280);
                card.setStyle(
                    "-fx-background-color: -color-bg-card;" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-radius: 10;" +
                    "-fx-border-color: -color-border;" +
                    "-fx-border-width: 1;" +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.02), 6, 0, 0, 2);"
                );

                HBox iconRow = new HBox(8);
                iconRow.setAlignment(Pos.CENTER_LEFT);
                Label fileIcon = new Label(getFileIconEmoji(cf.title));
                fileIcon.setStyle("-fx-font-size: 16px;");
                Label dateLbl = new Label(cf.uploadDate);
                dateLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
                iconRow.getChildren().addAll(fileIcon, dateLbl);

                Label fileTitle = new Label(cf.title);
                fileTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
                fileTitle.setWrapText(true);
                VBox.setVgrow(fileTitle, Priority.ALWAYS);

                Label fileDesc = new Label(cf.description.isEmpty() ? "No description available." : cf.description);
                fileDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");
                fileDesc.setWrapText(true);

                Button dlBtn = new Button("Download");
                dlBtn.setCursor(javafx.scene.Cursor.HAND);
                dlBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: -color-bg-main; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 4 10;");
                
                if (cf.href == null || cf.href.isBlank()) {
                    dlBtn.setDisable(true);
                    dlBtn.setText("Locked");
                } else {
                    dlBtn.setOnAction(e -> triggerFileDownload(cf.href, cf.title, cf.description, dlBtn));
                    applyOfflineStateIfOffline(dlBtn, "Download");
                    uploadButtons.add(dlBtn);
                }

                HBox bottomRow = new HBox(dlBtn);
                bottomRow.setAlignment(Pos.CENTER_RIGHT);

                card.getChildren().addAll(iconRow, fileTitle, fileDesc, bottomRow);

                // Hover
                card.setOnMouseEntered(e -> {
                    javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(120), card);
                    tt.setToY(-2);
                    tt.play();
                    card.setStyle(
                        "-fx-background-color: -color-bg-card;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: -color-accent;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.06), 6, 0, 0, 3);"
                    );
                });
                card.setOnMouseExited(e -> {
                    javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(120), card);
                    tt.setToY(0);
                    tt.play();
                    card.setStyle(
                        "-fx-background-color: -color-bg-card;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: -color-border;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.02), 6, 0, 0, 2);"
                    );
                });

                gridFlow.getChildren().add(card);
            }
            gridFlow.widthProperty().addListener((obs, oldVal, newVal) -> adjustFlowGrid(gridFlow, 280, 400, 16));
            Platform.runLater(() -> adjustFlowGrid(gridFlow, 280, 400, 16));
            filesContainer.getChildren().add(gridFlow);
        } else {
            VBox listCol = new VBox(0);
            listCol.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 8; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 8;");

            for (int i = 0; i < filtered.size(); i++) {
                CourseFile cf = filtered.get(i);
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                String bg = (i % 2 == 1) ? "-color-bg-card" : "-color-bg-main";
                row.setStyle("-fx-background-color: " + bg + "; -fx-padding: 10 16; -fx-border-color: -color-border; -fx-border-width: 0 0 1 0;");

                Label fileIcon = new Label(getFileIconEmoji(cf.title));
                fileIcon.setStyle("-fx-font-size: 16px;");

                VBox textCol = new VBox(2);
                Label fileTitle = new Label(cf.title);
                fileTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
                Label fileDesc = new Label(cf.description.isEmpty() ? "No description" : cf.description);
                fileDesc.setStyle("-fx-font-size: 9px; -fx-text-fill: -color-text-muted;");
                textCol.getChildren().addAll(fileTitle, fileDesc);
                HBox.setHgrow(textCol, Priority.ALWAYS);

                Label dateLbl = new Label(cf.uploadDate);
                dateLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted; -fx-pref-width: 100;");

                Button dlBtn = new Button("Download");
                dlBtn.setCursor(javafx.scene.Cursor.HAND);
                dlBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: -color-bg-main; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 4 10;");
                
                if (cf.href == null || cf.href.isBlank()) {
                    dlBtn.setDisable(true);
                    dlBtn.setText("Locked");
                } else {
                    dlBtn.setOnAction(e -> triggerFileDownload(cf.href, cf.title, cf.description, dlBtn));
                    applyOfflineStateIfOffline(dlBtn, "Download");
                    uploadButtons.add(dlBtn);
                }

                row.getChildren().addAll(fileIcon, textCol, dateLbl, dlBtn);
                listCol.getChildren().add(row);
            }
            filesContainer.getChildren().add(listCol);
        }
    }

    private String getFileIconEmoji(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "📕";
        if (lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".tar") || lower.endsWith(".gz")) return "📦";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "📘";
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return "🟢";
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) return "📙";
        if (lower.endsWith(".txt") || lower.endsWith(".md")) return "📝";
        if (lower.contains("assignment")) return "📌";
        if (lower.contains("quiz")) return "⚡";
        if (lower.contains("paper") || lower.contains("exam")) return "📄";
        return "📄";
    }

    private HBox buildOfflineBanner() {
        HBox banner = new HBox(8);
        banner.setAlignment(Pos.CENTER);
        banner.setPadding(new Insets(8, 16, 8, 16));
        banner.setStyle("-fx-background-color:#FEF2F2;-fx-border-color:#FCA5A5;-fx-border-width:0 0 1 0;");
        
        Label icon = new Label("⚠");
        icon.setStyle("-fx-text-fill:#DC2626;-fx-font-size:12px;");
        Label text = new Label("Offline Mode: Displaying previously loaded portal data.");
        text.setStyle("-fx-text-fill:#991B1B;-fx-font-size:11px;-fx-font-weight:bold;");
        
        banner.getChildren().addAll(icon, text);
        return banner;
    }

    private List<PortalAssignment> parseSubjectiveTestsFromHtml(String html) {
        return parseAssignmentsFromHtml(html, "CoursePortal.aspx?isTest=1");
    }

    // ==================== COURSE CONTENTS ====================
    private String sanitizeFilename(String name) {
        if (name == null) return "";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private String getCustomFilename(String docTitle, String docDesc, String extension) {
        String titlePart = sanitizeFilename(docTitle);
        String descPart = sanitizeFilename(docDesc);
        
        String filename = titlePart;
        if (!descPart.isEmpty()) {
            if (filename.isEmpty()) {
                filename = descPart;
            } else {
                filename = filename + " - " + descPart;
            }
        }
        
        if (filename.isEmpty()) {
            filename = "document_" + System.currentTimeMillis();
        }
        
        if (extension != null && !extension.isEmpty()) {
            if (!extension.startsWith(".")) {
                extension = "." + extension;
            }
            if (!filename.toLowerCase().endsWith(extension.toLowerCase())) {
                filename = filename + extension;
            }
        }
        return filename;
    }

    private Element findMainGridTable(Document doc, String preferredId) {
        if (preferredId != null && !preferredId.isEmpty()) {
            Element tbl = doc.getElementById(preferredId);
            if (tbl != null) return tbl;
        }
        for (Element tbl : doc.select("table.Grid")) {
            boolean isStudentDetails = tbl.parents().stream()
                .anyMatch(p -> p.hasClass("studentdetails") || p.id().equals("studentdetails") || p.tagName().equals("li"));
            if (isStudentDetails) continue;

            String text = tbl.text().toLowerCase();
            if (text.contains("name :") || text.contains("roll no :") || text.contains("father name :")) {
                continue;
            }
            return tbl;
        }
        return null;
    }

    private void triggerFileDownload(String href, String docTitle, String docDesc, Button btn) {
        if (btn != null) {
            Platform.runLater(() -> btn.setDisable(true));
        }

        new Thread(() -> {
            try {
                try (Response response = context.portalRepository().downloadFile(href)) {
                    if (!response.isSuccessful()) {
                        Platform.runLater(() -> {
                            if (btn != null) btn.setDisable(false);
                            context.notificationService().showError("Download Error", "Server returned HTTP " + response.code());
                        });
                        return;
                    }

                    ResponseBody body = response.body();
                    if (body == null) {
                        Platform.runLater(() -> {
                            if (btn != null) btn.setDisable(false);
                            context.notificationService().showError("Download Error", "Empty response body received.");
                        });
                        return;
                    }

                    String extension = ".pdf";
                    String disposition = response.header("Content-Disposition");
                    String serverFilename = "";
                    if (disposition != null && disposition.contains("filename=")) {
                        int index = disposition.indexOf("filename=");
                        String rawFilename = disposition.substring(index + 9).trim();
                        if (rawFilename.startsWith("\"") && rawFilename.endsWith("\"")) {
                            rawFilename = rawFilename.substring(1, rawFilename.length() - 1);
                        } else if (rawFilename.contains(";")) {
                            rawFilename = rawFilename.substring(0, rawFilename.indexOf(";")).trim();
                        }
                        serverFilename = rawFilename;
                    }

                    int dotIdx = serverFilename.lastIndexOf('.');
                    if (dotIdx > 0) {
                        extension = serverFilename.substring(dotIdx);
                    } else if (href.contains("fileType=q")) {
                        extension = ".pdf";
                    }

                    final String finalFilename = getCustomFilename(docTitle, docDesc, extension);

                    // Write to temporary file
                    File tempFile = File.createTempFile("assignly_download_", ".tmp");
                    tempFile.deleteOnExit();
                    try (InputStream is = body.byteStream();
                         OutputStream os = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                    }

                    final String finalExt = extension;
                    Platform.runLater(() -> {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Save Course Document");
                        fileChooser.setInitialFileName(finalFilename);

                        if (!finalExt.isEmpty()) {
                            fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter(finalExt.toUpperCase().substring(1) + " Files", "*" + finalExt)
                            );
                        }
                        fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("All Files", "*.*")
                        );

                        File file = fileChooser.showSaveDialog(context.stage());

                        if (file != null) {
                            new Thread(() -> {
                                try {
                                    java.nio.file.Files.copy(tempFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                    Platform.runLater(() -> {
                                        if (btn != null) btn.setDisable(false);
                                        context.notificationService().showSuccess("Download Complete", file.getName() + " downloaded");
                                    });
                                } catch (Exception ex) {
                                    ErrorReporter.logError("CoursePortalTabView#triggerCourseDocumentDownload save file", ex);
                                    Platform.runLater(() -> {
                                        if (btn != null) btn.setDisable(false);
                                        context.notificationService().showError("Download Failed", "Error saving file: " + ex.getMessage());
                                    });
                                } finally {
                                    tempFile.delete();
                                }
                            }).start();
                        } else {
                            if (btn != null) btn.setDisable(false);
                            tempFile.delete();
                        }
                    });
                }
            } catch (Exception ex) {
                ErrorReporter.logError("CoursePortalTabView#triggerCourseDocumentDownload", ex);
                Platform.runLater(() -> {
                    if (btn != null) btn.setDisable(false);
                    context.notificationService().showError("Download Error", "Connection failed: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void triggerDownloadAll(List<String[]> filesToDownload, String courseId, String courseTitle, String html, String originalPageHtml) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Directory to Save All Files for " + courseTitle);
        File selectedDir = dirChooser.showDialog(context.stage());
        if (selectedDir == null) return;

        showLoading("Downloading all files to " + selectedDir.getName() + "...");

        new Thread(() -> {
            int successCount = 0;
            int total = filesToDownload.size();
            List<String> failedFiles = new ArrayList<>();

            for (int i = 0; i < total; i++) {
                String[] fileInfo = filesToDownload.get(i);
                String href = fileInfo[0];
                String docTitle = fileInfo[1];
                String docDesc = fileInfo[2];

                final int currentIdx = i + 1;
                Platform.runLater(() -> {
                    showLoading("Downloading file " + currentIdx + " of " + total + ": " + docTitle + "...");
                });

                try {
                    try (Response response = context.portalRepository().downloadFile(href)) {
                        if (!response.isSuccessful() || response.body() == null) {
                            failedFiles.add(docTitle + " (HTTP " + response.code() + ")");
                            continue;
                        }

                        String extension = ".pdf";
                        String disposition = response.header("Content-Disposition");
                        String serverFilename = "";
                        if (disposition != null && disposition.contains("filename=")) {
                            int idx = disposition.indexOf("filename=");
                            String rawFilename = disposition.substring(idx + 9).trim();
                            if (rawFilename.startsWith("\"") && rawFilename.endsWith("\"")) {
                                rawFilename = rawFilename.substring(1, rawFilename.length() - 1);
                            } else if (rawFilename.contains(";")) {
                                rawFilename = rawFilename.substring(0, rawFilename.indexOf(";")).trim();
                            }
                            serverFilename = rawFilename;
                        }

                        int dotIdx = serverFilename.lastIndexOf('.');
                        if (dotIdx > 0) {
                            extension = serverFilename.substring(dotIdx);
                        } else if (href.contains("fileType=q")) {
                            extension = ".pdf";
                        }

                        String finalFilename = getCustomFilename(docTitle, docDesc, extension);

                        File destFile = new File(selectedDir, finalFilename);
                        try (InputStream is = response.body().byteStream();
                             OutputStream os = new FileOutputStream(destFile)) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                os.write(buffer, 0, read);
                            }
                            successCount++;
                        }
                    }
                } catch (Exception ex) {
                    ErrorReporter.logError("CoursePortalTabView#triggerDownloadAll", ex);
                    failedFiles.add(docTitle + " (" + ex.getMessage() + ")");
                }
            }

            final int finalSuccess = successCount;
            final List<String> finalFailed = failedFiles;
            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                try {
                    contentPane.getChildren().add(buildCourseContentsDetailView(courseId, courseTitle, html, originalPageHtml));
                } catch (Exception ex) {
                    ErrorReporter.logError("CoursePortalTabView#triggerDownloadAll refresh view", ex);
                    activeTab = "";
                    loadTab("portal_contents");
                }

                if (finalFailed.isEmpty()) {
                    context.notificationService().showSuccess("Download All Complete", "Successfully downloaded all " + finalSuccess + " files to " + selectedDir.getAbsolutePath());
                } else {
                    String msg = "Downloaded " + finalSuccess + " of " + total + " files.\nFailed files:\n" + String.join("\n", finalFailed);
                    context.notificationService().showError("Download All Completed with Errors", msg);
                }
            });
        }).start();
    }

    // ==================== PENDING ASSIGNMENTS ====================
    private void loadPendingAssignmentsData(boolean forceRefresh) {
        showLoading("Loading Pending Assignments...");
        new Thread(() -> {
            try {
                String html = null;
                if (!forceRefresh) {
                    html = context.dataCacheService().getCachedHtml("CoursePortalPendingAssignments.aspx").orElse(null);
                }
                if (html == null) {
                    html = context.fetchAndCacheHtml("CoursePortalPendingAssignments.aspx");
                    if (html == null) {
                        html = context.dataCacheService().getCachedHtml("CoursePortalPendingAssignments.aspx").orElse(null);
                    }
                }

                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Unable to load Pending Assignments",
                            "Failed to connect to the portal. Please check your internet connection and try again."
                        ));
                    });
                    return;
                }

                final String finalHtml = html;
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    try {
                        pendingAssignmentsCacheHtml = finalHtml;
                        contentPane.getChildren().add(buildPendingAssignmentsView(finalHtml));
                    } catch (Exception ex) {
                        ErrorReporter.notify(context, "Failed to parse Pending Assignments. Portal layout may have changed.", "CoursePortalTabView#loadPendingAssignmentsData", ex);
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState(
                            "Parsing Error",
                            "Failed to read the Pending Assignments table cleanly."
                        ));
                    }
                });
            } catch (Exception e) {
                ErrorReporter.notify(context, "Failed to load Pending Assignments. Please try again.", "CoursePortalTabView#loadPendingAssignmentsData", e);
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState(
                        "Loading Error",
                        "An unexpected error occurred while loading pending assignments: " + e.getMessage()
                    ));
                });
            }
        }).start();
    }

    private ScrollPane buildPendingAssignmentsView(String html) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        Document doc = Jsoup.parse(html);
        
        String headingText = resolvePortalHeading(doc, "Pending Assignments");

        Label subHeading = new Label(headingText);
        subHeading.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;-fx-padding:0 0 4 0;");
        content.getChildren().add(subHeading);

        List<PortalAssignment> list = parseAssignmentsFromHtml(html, "CoursePortalPendingAssignments.aspx");
        if (list.isEmpty()) {
            renderEmptyPendingState(content);
        } else {
            HBox explorerBar = new HBox(12);
            explorerBar.setAlignment(Pos.CENTER_LEFT);
            explorerBar.setPadding(new Insets(4, 0, 4, 0));

            TextField searchField = new TextField(pendingAssignmentsSearchQuery);
            searchField.setPromptText("Search assignments by title or course...");
            HBox.setHgrow(searchField, Priority.ALWAYS);
            searchField.setStyle("-fx-background-color: -color-bg-card; -fx-border-color: -color-border; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: -color-text-main; -fx-padding: 8 12;");
            explorerBar.getChildren().add(searchField);
            content.getChildren().add(explorerBar);

            FlowPane flow = new FlowPane(16, 16);
            flow.setPadding(new Insets(8, 0, 16, 0));

            Runnable filterAndRender = () -> {
                flow.getChildren().clear();
                List<PortalAssignment> filtered = new ArrayList<>();
                for (PortalAssignment pa : list) {
                    if (pendingAssignmentsSearchQuery.isEmpty() || 
                        pa.title.toLowerCase().contains(pendingAssignmentsSearchQuery.toLowerCase()) || 
                        pa.course.toLowerCase().contains(pendingAssignmentsSearchQuery.toLowerCase())) {
                        filtered.add(pa);
                    }
                }
                
                if (filtered.isEmpty()) {
                    VBox emptyCard = new VBox(12);
                    emptyCard.setUserData("empty");
                    emptyCard.setAlignment(Pos.CENTER);
                    emptyCard.setPadding(new Insets(32, 24, 32, 24));
                    emptyCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;");
                    Label icon = new Label("🔍");
                    icon.setStyle("-fx-font-size:24px;");
                    Label label = new Label("No matching assignments found");
                    label.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:12px;-fx-font-weight:bold;");
                    emptyCard.getChildren().addAll(icon, label);
                    flow.getChildren().add(emptyCard);
                } else {
                    for (PortalAssignment pa : filtered) {
                        flow.getChildren().add(buildAssignmentCard(pa, "CoursePortalPendingAssignments.aspx"));
                    }
                }
                adjustFlowGrid(flow, 320, 450, 16);
            };

            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                pendingAssignmentsSearchQuery = newVal.trim();
                filterAndRender.run();
            });

            flow.widthProperty().addListener((obs, oldVal, newVal) -> adjustFlowGrid(flow, 320, 450, 16));
            Platform.runLater(() -> {
                filterAndRender.run();
                adjustFlowGrid(flow, 320, 450, 16);
            });

            content.getChildren().add(flow);
        }

        sp.setContent(content);
        return sp;
    }

    private void renderEmptyPendingState(VBox content) {
        VBox emptyCard = new VBox(12);
        emptyCard.setAlignment(Pos.CENTER);
        emptyCard.setPadding(new Insets(32, 24, 32, 24));
        emptyCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        Label icon = new Label("📌");
        icon.setStyle("-fx-font-size:28px;");

        Label label = new Label("No Pending Assignments");
        label.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:13px;-fx-font-weight:bold;");

        Label desc = new Label("There are currently no pending assignments requiring your attention. All assignments have been submitted or closed.");
        desc.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:11px;-fx-text-alignment:center;");
        desc.setWrapText(true);

        emptyCard.getChildren().addAll(icon, label, desc);
        content.getChildren().add(emptyCard);
    }

    private void adjustFlowGrid(FlowPane flow, double minCardWidth, double maxCardWidth, double gap) {
        double w = flow.getWidth();
        if (w <= 0) return;
        w = w - 8; // safety padding margin
        int cols = (int) Math.max(1, Math.floor((w + gap) / (minCardWidth + gap)));
        double cardW = (w - (cols - 1) * gap) / cols;
        if (cardW > maxCardWidth) {
            cardW = maxCardWidth;
        }
        for (Node node : flow.getChildren()) {
            if (node instanceof Region r) {
                if ("empty".equals(r.getUserData())) {
                    r.setPrefWidth(w);
                    r.setMaxWidth(w);
                } else {
                    r.setPrefWidth(cardW);
                    r.setMaxWidth(cardW);
                }
            }
        }
    }

    private String resolvePortalHeading(Document doc, String fallback) {
        if (doc == null) return fallback;
        Element header = doc.selectFirst(".content-box-header h3");
        if (header != null) {
            String text = header.text().trim();
            if (!text.isEmpty() && !text.toLowerCase().contains("messages")) return text;
        }
        for (Element h3 : doc.select("h3")) {
            if (isInsideSidebarOrMessages(h3)) continue;
            String text = h3.text().trim();
            if (!text.isEmpty() && !text.toLowerCase().contains("messages")) return text;
        }
        return fallback;
    }

    private boolean isInsideSidebarOrMessages(Element element) {
        Element current = element;
        while (current != null) {
            String id = current.id();
            if (id != null && (id.equalsIgnoreCase("messages") || id.equalsIgnoreCase("sidebar") || id.equalsIgnoreCase("sidebar-wrapper"))) {
                return true;
            }
            String className = current.className();
            if (className != null && (className.contains("sidebar") || className.contains("messages"))) {
                return true;
            }
            current = current.parent();
        }
        return false;
    }



    private void applyOfflineStateIfOffline(Button btn, String originalText) {
        if (!context.isOnline()) {
            btn.setDisable(true);
            btn.setText("🔒 " + originalText);
            btn.setTooltip(new javafx.scene.control.Tooltip("Connection required for this action"));
        }
    }

    private void onConnectivityChanged(boolean isOnline) {
        Platform.runLater(() -> {
            for (Button btn : uploadButtons) {
                String originalText = btn.getText();
                if (isOnline) {
                    btn.setDisable(false);
                    if (originalText.startsWith("🔒 ")) {
                        btn.setText(originalText.substring("🔒 ".length()));
                    }
                    btn.setTooltip(null);
                } else {
                    btn.setDisable(true);
                    if (!originalText.startsWith("🔒 ")) {
                        btn.setText("🔒 " + originalText);
                    }
                    btn.setTooltip(new javafx.scene.control.Tooltip("Connection required for this action"));
                }
            }
        });
    }

    public VBox getRoot() { return root; }
  }
