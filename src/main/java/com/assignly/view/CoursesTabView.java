package com.assignly.view;

import com.assignly.util.AppContext;
import com.assignly.util.ErrorReporter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.*;

public class CoursesTabView {
    private final VBox root = new VBox();
    private final AppContext context;
    private StackPane contentPane;
    
    // REDESIGN STATE
    private List<CourseSummary> courseList = new ArrayList<>();
    private CourseSummary selectedCourse = null;
    private String activeSegment = "overview"; // "overview", "attendance", "performance"
    private boolean isOfflineMode = false;
    
    // Internal Cache HTMLs
    private String summaryHtml = "";
    private String proceedingsHtml = "";
    private String marksHtml = "";
    private boolean proceedingsRequestCompleted = false;
    private boolean marksRequestCompleted = false;
    
    private static final String PROC_PAGE = "classproceedings.aspx";
    private static final String MARKS_PAGE = "QAMarks.aspx";
    private static final String[] MARKS_FALLBACKS = {"QASessMarks.aspx", "QASessionMarks.aspx", "Marks.aspx"};
    
    // Fields for navigation/filtering inside subtabs
    private String attendanceSearchQuery = "";
    private boolean filterPresentOnly = false;
    private boolean filterAbsentOnly = false;

    // Helper Models
    public static class CourseSummary {
        public String serial = "";
        public String code = "";
        public String title = "";
        public String className = "";
        public String faculty = "";
        public String presents = "0";
        public String absents = "0";
        public String totalClasses = "0";
        public String theoryPercentage = "N/A";
        public String labPercentage = "N/A";
        public String percentage = "0%";
        public String postbackTarget = "";
    }

    public static class LectureEntry {
        public String lectureNo = "";
        public String date = "";
        public String duration = "";
        public String topic = "";
        public String status = ""; // "P" or "A"
    }

    public static class MarkItem {
        public String title = "";
        public String date = "";
        public String totalMarks = "";
        public String obtainedMarks = "";
        public String percentage = "";
    }

    public static class MarksCategory {
        public String categoryName = "";
        public List<MarkItem> items = new ArrayList<>();
        public double averagePct = 0.0;
        public double totalMax = 0.0;
        public double totalObtained = 0.0;
    }

    public static class CourseHealthResult {
        public double combinedScore = Double.NaN;
        public String healthStatus = "Insufficient Data";
        public String quizAvgStr = "Not Available";
        public String assignAvgStr = "Not Available";
        public String sessAvgStr = "Not Available";
    }

    // Configurable thresholds for Health Score
    private double thresholdExcellent = 90.0;
    private double thresholdGood = 80.0;
    private double thresholdWarning = 75.0;

    public CourseHealthResult evaluateCourseHealth(CourseSummary c, String courseMarksHtml) {
        CourseHealthResult result = new CourseHealthResult();
        
        // 1. Check Attendance
        double attVal = Double.NaN;
        if (c.percentage != null && !c.percentage.equalsIgnoreCase("N/A") && !c.percentage.isBlank()) {
            try {
                attVal = Double.parseDouble(c.percentage.replace("%", "").trim());
            } catch (Exception ignored) {}
        }
        
        // Rule 1: If attendance data is unavailable
        if (Double.isNaN(attVal)) {
            result.healthStatus = "Awaiting Records";
            return result;
        }
        
        // Rule 2: If attendance is < 75% -> always At Risk
        if (attVal < 75.0) {
            result.combinedScore = attVal;
            result.healthStatus = "At Risk";
            return result;
        }
        
        // 2. Parse courseMarksHtml if available
        if (courseMarksHtml == null || courseMarksHtml.isBlank()) {
            result.healthStatus = "Insufficient Data";
            return result;
        }
        
        List<MarksCategory> categories = parseMarksCategories(courseMarksHtml);
        double quizAvg = Double.NaN;
        double assignAvg = Double.NaN;
        double sessAvg = Double.NaN;
        
        for (MarksCategory cat : categories) {
            if ("Quizzes".equalsIgnoreCase(cat.categoryName)) {
                quizAvg = cat.averagePct;
                result.quizAvgStr = String.format("%.1f%%", quizAvg);
            } else if ("Assignments".equalsIgnoreCase(cat.categoryName)) {
                assignAvg = cat.averagePct;
                result.assignAvgStr = String.format("%.1f%%", assignAvg);
            } else if ("Sessionals".equalsIgnoreCase(cat.categoryName)) {
                sessAvg = cat.averagePct;
                result.sessAvgStr = String.format("%.1f%%", sessAvg);
            }
        }
        
        // Determine available components and compute Health Score
        boolean hasQuiz = !Double.isNaN(quizAvg) && quizAvg >= 0;
        boolean hasAssign = !Double.isNaN(assignAvg) && assignAvg >= 0;
        boolean hasSess = !Double.isNaN(sessAvg) && sessAvg >= 0;
        
        // Case 4: Attendance >= 75% but assignments and quizzes are missing -> Insufficient Data
        if (!hasQuiz && !hasAssign) {
            result.healthStatus = "Insufficient Data";
            return result;
        }
        
        // Calculate combined score
        double totalWeight = 40.0;
        double weightedSum = 40.0 * attVal;
        
        if (hasQuiz) {
            totalWeight += 30.0;
            weightedSum += 30.0 * quizAvg;
        }
        if (hasAssign) {
            totalWeight += 20.0;
            weightedSum += 20.0 * assignAvg;
        }
        if (hasSess) {
            totalWeight += 10.0;
            weightedSum += 10.0 * sessAvg;
        }
        
        double score = weightedSum / totalWeight;
        result.combinedScore = score;
        
        // Classify health status based on configurable thresholds
        if (score >= thresholdExcellent) {
            result.healthStatus = "Excellent";
        } else if (score >= thresholdGood) {
            result.healthStatus = "Good";
        } else if (score >= thresholdWarning) {
            result.healthStatus = "Warning";
        } else {
            result.healthStatus = "At Risk";
        }
        
        return result;
    }

    public CoursesTabView(AppContext context) {
        this(context, "summary", null);
    }

    public CoursesTabView(AppContext context, String initialTab, String initialCourseQuery) {
        this.context = context;
        buildShell();
        
        // Handle deep-linking
        if (initialCourseQuery != null && !initialCourseQuery.isBlank()) {
            if ("proceedings".equalsIgnoreCase(initialTab)) {
                this.activeSegment = "attendance";
            } else if ("marks".equalsIgnoreCase(initialTab)) {
                this.activeSegment = "performance";
            } else {
                this.activeSegment = "overview";
            }
            loadCourseContextAndDashboard(initialCourseQuery);
        } else {
            selectedCourse = null;
            loadLandingPage(false);
        }
    }

    private void buildShell() {
        root.setFillWidth(true);
        root.setStyle("-fx-background-color: transparent;");

        contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        root.getChildren().add(contentPane);
    }

    private void showLoading(String msg) {
        Platform.runLater(() -> {
            contentPane.getChildren().clear();
            ScrollPane sp;
            if (msg.toLowerCase().contains("registered courses") || selectedCourse == null) {
                sp = new ScrollPane(com.assignly.util.ShimmerBuilder.buildCoursesShimmer());
            } else {
                sp = new ScrollPane(com.assignly.util.ShimmerBuilder.buildResultShimmer());
            }
            sp.setFitToWidth(true);
            sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
            contentPane.getChildren().add(sp);
        });
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            contentPane.getChildren().clear();
            VBox box = new VBox(14);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(40));
            
            Label icon = new Label("⚠️");
            icon.setStyle("-fx-font-size: 32px;");
            
            Label label = new Label(msg);
            label.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 13px; -fx-text-alignment: center;");
            label.setWrapText(true);
            
            Button backBtn = new Button("Return to My Courses");
            backBtn.getStyleClass().add("accent-button");
            backBtn.setOnAction(e -> {
                selectedCourse = null;
                loadLandingPage(true);
            });
            
            box.getChildren().addAll(icon, label, backBtn);
            contentPane.getChildren().add(box);
        });
    }

    // ==================== LANDING PAGE CONTROLLER ====================
    private void loadLandingPage(boolean forceRefresh) {
        showLoading("Loading your registered courses...");
        new Thread(() -> {
            try {
                String html = null;
                if (!forceRefresh) {
                    html = context.dataCacheService().getCachedHtml("Summary.aspx").orElse(null);
                }
                if (html == null) {
                    html = context.fetchAndCacheHtml("Summary.aspx");
                    if (html == null) {
                        html = context.dataCacheService().getCachedHtml("Summary.aspx").orElse(null);
                        isOfflineMode = true;
                    } else {
                        isOfflineMode = false;
                    }
                }
                summaryHtml = html;

                if (html == null) {
                    showError("Unable to load Courses summary. Please check your internet connection.");
                    return;
                }

                String resultHtml = context.dataCacheService().getCachedHtml("StudentResultCard.aspx").orElse(null);
                if (resultHtml == null && !isOfflineMode) {
                    resultHtml = context.fetchAndCacheHtml("StudentResultCard.aspx");
                }
                if (resultHtml == null) {
                    resultHtml = context.dataCacheService().getCachedHtml("StudentResultCard.aspx").orElse(null);
                }
                Map<String, String> courseNames = context.portalRepository().parseCourseNames(resultHtml);
                courseList = parseSummaryCourses(html, courseNames);
                final List<CourseSummary> finalCourses = courseList;
                final boolean finalOffline = isOfflineMode;

                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildLandingView(finalCourses, finalOffline));
                });
            } catch (Exception e) {
                ErrorReporter.logError("CoursesTabView#loadLandingPage", e);
                showError("An error occurred: " + e.getMessage());
            }
        }).start();
    }

    private ScrollPane buildLandingView(List<CourseSummary> courses, boolean isOffline) {
        VBox container = new VBox(20);
        container.setPadding(new Insets(24, 28, 28, 28));
        container.setFillWidth(true);

        if (isOffline) {
            container.getChildren().add(buildOfflineBanner());
        }

        // Header Title
        Label mainTitle = new Label("My Courses");
        mainTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        container.getChildren().add(mainTitle);

        if (courses.isEmpty()) {
            VBox emptyState = new VBox(12);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(40));
            emptyState.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12; -fx-border-color: -color-border; -fx-border-width: 1;");
            
            Label icon = new Label("📚");
            icon.setStyle("-fx-font-size: 32px;");
            Label lbl = new Label("No registered courses found.");
            lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
            
            emptyState.getChildren().addAll(icon, lbl);
            container.getChildren().add(emptyState);
        } else {
            // FlowPane for responsive grid card layout
            FlowPane grid = new FlowPane(16, 16);
            grid.setPrefWrapLength(900);
            grid.setStyle("-fx-background-color: transparent;");

            for (CourseSummary c : courses) {
                VBox card = buildCourseCard(c);
                grid.getChildren().add(card);
            }
            container.getChildren().add(grid);
        }

        ScrollPane sp = new ScrollPane(container);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        return sp;
    }

    private VBox buildCourseCard(CourseSummary c) {
        VBox card = new VBox(12);
        card.getStyleClass().add("course-card-container");
        card.setPrefWidth(270);
        card.setMinWidth(270);
        card.setMaxWidth(270);

        // Header: Code and Class Name badge
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        Label code = new Label(c.code.isEmpty() ? "COURSE" : c.code);
        code.getStyleClass().add("course-card-code");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Class Name badge
        Label classBadge = new Label(c.className.isEmpty() || c.className.equalsIgnoreCase("N/A") ? "Class: N/A" : "Class: " + c.className);
        classBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #004643; -fx-background-color: rgba(0, 70, 67, 0.08); -fx-padding: 2 8; -fx-background-radius: 8;");
        
        topRow.getChildren().addAll(code, spacer, classBadge);

        // Title and Teacher
        Label title = new Label(c.title);
        title.getStyleClass().add("course-card-title");
        title.setWrapText(true);
        title.setMinHeight(40);
        title.setAlignment(Pos.TOP_LEFT);

        VBox infoCol = new VBox(4);
        Label teacherLabel = new Label("Faculty:");
        teacherLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
        Label teacher = new Label("👤 " + c.faculty);
        teacher.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-main; -fx-font-weight: 500;");
        teacher.setWrapText(true);
        infoCol.getChildren().addAll(teacherLabel, teacher);

        // Lectures Row: total, presents, absents
        VBox lecturesCol = new VBox(2);
        Label lecturesLabel = new Label("Lectures:");
        lecturesLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
        Label lecturesVal = new Label(String.format("%s (P: %s, A: %s)", c.totalClasses, c.presents, c.absents));
        lecturesVal.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-main; -fx-font-weight: 500;");
        lecturesCol.getChildren().addAll(lecturesLabel, lecturesVal);

        // Attendance Row (Theory / Lab split if lab exists)
        VBox attCol = new VBox(2);
        Label attLabel = new Label("Attendance:");
        attLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");

        String attStr = "N/A";
        String attColor = "#64748b";
        boolean hasLab = c.labPercentage != null && !c.labPercentage.equalsIgnoreCase("N/A") && !c.labPercentage.isBlank();
        boolean hasTheory = c.theoryPercentage != null && !c.theoryPercentage.equalsIgnoreCase("N/A") && !c.theoryPercentage.isBlank();
        
        if (hasLab) {
            attStr = String.format("Theory: %s | Lab: %s", c.theoryPercentage, c.labPercentage);
        } else if (hasTheory) {
            attStr = c.theoryPercentage;
            try {
                double val = Double.parseDouble(c.theoryPercentage.replace("%", "").trim());
                if (val < 75.0) attColor = "#dc2626";
                else if (val < 85.0) attColor = "#d97706";
                else attColor = "#004643";
            } catch (Exception ignored) {}
        } else if (c.percentage != null && !c.percentage.equalsIgnoreCase("N/A") && !c.percentage.isBlank()) {
            attStr = c.percentage;
            try {
                double val = Double.parseDouble(c.percentage.replace("%", "").trim());
                if (val < 75.0) attColor = "#dc2626";
                else if (val < 85.0) attColor = "#d97706";
                else attColor = "#004643";
            } catch (Exception ignored) {}
        }
        
        Label attValLbl = new Label(attStr);
        attValLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: " + attColor + ";");
        attCol.getChildren().addAll(attLabel, attValLbl);

        // Stats Row (combines Lectures and Attendance in side-by-side or stacked format)
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.getChildren().addAll(lecturesCol, attCol);

        // Bottom Row for Status/Badge & Action
        HBox bottomRow = new HBox(8);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Label statusBadge = new Label();
        if (c.percentage == null || c.percentage.equalsIgnoreCase("N/A") || c.percentage.isBlank()) {
            statusBadge.setText("No Data");
            statusBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #64748b; -fx-background-color: #f1f5f9; -fx-padding: 3 8; -fx-background-radius: 8;");
        } else {
            statusBadge.setText("Active");
            statusBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #059669; -fx-background-color: #d1fae5; -fx-padding: 3 8; -fx-background-radius: 8;");
        }

        Region spacerBottom = new Region();
        HBox.setHgrow(spacerBottom, Priority.ALWAYS);

        Button openBtn = new Button("Open Course");
        openBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6;");
        openBtn.setCursor(javafx.scene.Cursor.HAND);
        openBtn.setOnAction(e -> openCourseDashboard(c));
        
        bottomRow.getChildren().addAll(statusBadge, spacerBottom, openBtn);

        card.getChildren().addAll(topRow, title, infoCol, statsRow, bottomRow);

        // TranslateTransition click trigger
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                openCourseDashboard(c);
            }
        });

        // Setup TranslateTransition on hover
        card.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
            tt.setToY(-3);
            tt.play();
        });
        card.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
            tt.setToY(0);
            tt.play();
        });

        return card;
    }

    // ==================== DEEP ROUTING RESOLUTION ====================
    private void loadCourseContextAndDashboard(String query) {
        showLoading("Locating your course...");
        new Thread(() -> {
            try {
                String html = context.dataCacheService().getCachedHtml("Summary.aspx").orElse(null);
                if (html == null) {
                    html = context.fetchAndCacheHtml("Summary.aspx");
                    if (html == null) {
                        html = context.dataCacheService().getCachedHtml("Summary.aspx").orElse(null);
                        isOfflineMode = true;
                    }
                }
                summaryHtml = html;
                if (html != null) {
                    String resultHtml = context.dataCacheService().getCachedHtml("StudentResultCard.aspx").orElse(null);
                    if (resultHtml == null && !isOfflineMode) {
                        resultHtml = context.fetchAndCacheHtml("StudentResultCard.aspx");
                    }
                    if (resultHtml == null) {
                        resultHtml = context.dataCacheService().getCachedHtml("StudentResultCard.aspx").orElse(null);
                    }
                    Map<String, String> courseNames = context.portalRepository().parseCourseNames(resultHtml);
                    courseList = parseSummaryCourses(html, courseNames);
                    String cleanQ = query.trim().toLowerCase();
                    for (CourseSummary c : courseList) {
                        if (c.title.toLowerCase().contains(cleanQ) || c.code.toLowerCase().contains(cleanQ)) {
                            selectedCourse = c;
                            break;
                        }
                    }
                }

                if (selectedCourse == null) {
                    loadLandingPage(true);
                } else {
                    loadCourseDataAndDashboard(selectedCourse);
                }
            } catch (Exception e) {
                ErrorReporter.logError("CoursesTabView#loadCourseContextAndDashboard", e);
                loadLandingPage(true);
            }
        }).start();
    }

    // ==================== DASHBOARD CONTROLLER ====================
    private void openCourseDashboard(CourseSummary course) {
        selectedCourse = course;
        loadCourseDataAndDashboard(course);
    }

    private void loadCourseDataAndDashboard(CourseSummary course) {
        showLoading("Loading course dashboard for " + course.title + "...");
        proceedingsRequestCompleted = false;
        marksRequestCompleted = false;
        new Thread(() -> {
            try {
                String cleanCode = course.code.isEmpty() ? course.title : course.code;
                boolean isOffline = !context.portalRepository().checkConnectivity();
                
                String pTarget = course.postbackTarget;
                
                // Write debug log to assignly.log
                synchronized (ErrorReporter.class) {
                    try {
                        java.nio.file.Files.writeString(
                            com.assignly.util.AppDirectoryHelper.getLogPath(), 
                            "\n========================================\n" +
                            "DEBUG: loadCourseDataAndDashboard started\n" +
                            "Course Title: " + course.title + "\n" +
                            "Course Code: " + course.code + "\n" +
                            "Postback Target (input): " + (pTarget != null ? pTarget : "null") + "\n" +
                            "Is Offline: " + isOffline + "\n" +
                            "========================================\n",
                            java.nio.file.StandardOpenOption.CREATE, 
                            java.nio.file.StandardOpenOption.APPEND
                        );
                    } catch (Exception ignored) {}
                }
                if (!isOffline) {
                    try {
                        if (pTarget == null || pTarget.isEmpty()) {
                            String summaryHtml = context.dataCacheService().getCachedHtml("Summary.aspx").orElse(null);
                            if (summaryHtml == null) {
                                summaryHtml = context.fetchAndCacheHtml("Summary.aspx");
                            }
                            if (summaryHtml != null) {
                                String resultHtml = context.dataCacheService().getCachedHtml("StudentResultCard.aspx").orElse(null);
                                if (resultHtml == null) {
                                    resultHtml = context.fetchAndCacheHtml("StudentResultCard.aspx");
                                }
                                Map<String, String> courseNames = context.portalRepository().parseCourseNames(resultHtml);
                                List<CourseSummary> courses = parseSummaryCourses(summaryHtml, courseNames);
                                for (CourseSummary cs : courses) {
                                    String csCleanCode = cs.code.isEmpty() ? cs.title : cs.code;
                                    if (csCleanCode.equalsIgnoreCase(cleanCode) || cs.title.equalsIgnoreCase(course.title)) {
                                        pTarget = cs.postbackTarget;
                                        break;
                                    }
                                }
                            }
                        }

                        // Log resolved postback target
                        synchronized (ErrorReporter.class) {
                            try {
                                java.nio.file.Files.writeString(
                                    com.assignly.util.AppDirectoryHelper.getLogPath(), 
                                    "DEBUG: Resolved postbackTarget = " + (pTarget != null ? pTarget : "null") + "\n",
                                    java.nio.file.StandardOpenOption.CREATE, 
                                    java.nio.file.StandardOpenOption.APPEND
                                );
                            } catch (Exception ignored) {}
                        }

                        if (pTarget != null && !pTarget.isEmpty()) {
                            context.portalRepository().postbackEventStandard("Summary.aspx", pTarget);
                        }
                    } catch (Exception e) {
                        ErrorReporter.logError("CoursesTabView#loadCourseDataAndDashboard (select course via postback)", e);
                    }
                }

                // 1. Fetch class proceedings html
                String procKey = PROC_PAGE + "_" + cleanCode;
                String procHtml = null;
                boolean procCompleted = false;
                
                if (!isOffline) {
                    try {
                        String rawProc = context.portalRepository().fetchPageHtml(PROC_PAGE);
                        if (rawProc != null && !rawProc.contains("Please select a course")) {
                            procHtml = rawProc;
                            context.dataCacheService().cacheHtml(procKey, rawProc);
                            procCompleted = true;
                        }
                    } catch (Exception e) {
                        ErrorReporter.logError("CoursesTabView#loadCourseDataAndDashboard (live class proceedings)", e);
                    }
                }
                
                if (procHtml == null) {
                    procHtml = context.dataCacheService().getCachedHtml(procKey).orElse(null);
                    if (procHtml != null) {
                        procCompleted = true;
                        if (!isOffline) {
                            Platform.runLater(() -> context.notificationService().showError("Unable to retrieve latest data."));
                        }
                    }
                }
                
                if (procHtml == null) {
                    procHtml = context.dataCacheService().getCachedHtml(PROC_PAGE).orElse(null);
                }
                proceedingsHtml = procHtml;
                proceedingsRequestCompleted = procCompleted;

                // 2. Fetch marks html
                String marksKey = MARKS_PAGE + "_" + cleanCode;
                String mHtml = null;
                boolean marksCompleted = false;
                
                if (!isOffline) {
                    try {
                        String rawMarks = context.portalRepository().fetchPageHtml(MARKS_PAGE);
                        if (rawMarks != null && !rawMarks.contains("Please select a course")) {
                            mHtml = rawMarks;
                            context.dataCacheService().cacheHtml(marksKey, rawMarks);
                            marksCompleted = true;
                        }
                    } catch (Exception e) {
                        ErrorReporter.logError("CoursesTabView#loadCourseDataAndDashboard (live marks)", e);
                    }
                }
                
                if (mHtml == null) {
                    mHtml = context.dataCacheService().getCachedHtml(marksKey).orElse(null);
                    if (mHtml != null) {
                        marksCompleted = true;
                        if (!isOffline) {
                            Platform.runLater(() -> context.notificationService().showError("Unable to retrieve latest data."));
                        }
                    } else {
                        for (String fb : MARKS_FALLBACKS) {
                            mHtml = context.dataCacheService().getCachedHtml(fb + "_" + cleanCode).orElse(null);
                            if (mHtml != null) {
                                marksCompleted = true;
                                if (!isOffline) {
                                    Platform.runLater(() -> context.notificationService().showError("Unable to retrieve latest data."));
                                }
                                break;
                            }
                        }
                    }
                }
                
                if (mHtml == null) {
                    mHtml = context.dataCacheService().getCachedHtml(MARKS_PAGE).orElse(null);
                    if (mHtml == null) {
                        for (String fb : MARKS_FALLBACKS) {
                            mHtml = context.dataCacheService().getCachedHtml(fb).orElse(null);
                            if (mHtml != null) break;
                        }
                    }
                }
                marksHtml = mHtml;
                marksRequestCompleted = marksCompleted;
                isOfflineMode = isOffline;

                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildDashboardView());
                });

            } catch (Exception e) {
                ErrorReporter.logError("CoursesTabView#loadCourseDataAndDashboard", e);
                showError("Failed to fetch course details: " + e.getMessage());
            }
        }).start();
    }

    private ScrollPane buildDashboardView() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20, 28, 28, 28));
        layout.setFillWidth(true);

        if (isOfflineMode) {
            layout.getChildren().add(buildOfflineBanner());
        }

        // BREADCRUMB
        HBox breadcrumb = new HBox(8);
        breadcrumb.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("← Back to My Courses");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-accent; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0;");
        backBtn.setOnAction(e -> {
            selectedCourse = null;
            loadLandingPage(false);
        });
        breadcrumb.getChildren().add(backBtn);
        layout.getChildren().add(breadcrumb);

        // SECTION 1: HEADER INFO CARD
        VBox headerCard = buildDashboardHeaderCard();
        layout.getChildren().add(headerCard);

        // SECTION 2: MODERN SEGMENT NAVIGATION CONTROLS
        HBox segmentedControl = new HBox(4);
        segmentedControl.setStyle("-fx-background-color: -color-bg-card; -fx-padding: 4; -fx-background-radius: 8; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 8; -fx-max-width: 450;");
        
        Button overviewBtn = buildSegmentBtn("📊 Overview", "overview");
        Button attendanceBtn = buildSegmentBtn("📅 Attendance", "attendance");
        Button performanceBtn = buildSegmentBtn("📝 Performance", "performance");
        
        segmentedControl.getChildren().addAll(overviewBtn, attendanceBtn, performanceBtn);
        layout.getChildren().add(segmentedControl);

        // SECTION 3-5: TAB CONTENT DOCK
        StackPane subTabContent = new StackPane();
        VBox.setVgrow(subTabContent, Priority.ALWAYS);

        if ("overview".equals(activeSegment)) {
            subTabContent.getChildren().add(buildOverviewTab());
        } else if ("attendance".equals(activeSegment)) {
            subTabContent.getChildren().add(buildAttendanceTab());
        } else {
            subTabContent.getChildren().add(buildPerformanceTab());
        }
        
        layout.getChildren().add(subTabContent);

        ScrollPane sp = new ScrollPane(layout);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        return sp;
    }

    private Button buildSegmentBtn(String label, String segmentKey) {
        Button btn = new Button(label);
        btn.getStyleClass().add("segment-btn");
        HBox.setHgrow(btn, Priority.ALWAYS);
        btn.setMaxWidth(Double.MAX_VALUE);
        
        if (segmentKey.equals(activeSegment)) {
            btn.getStyleClass().add("segment-btn-active");
        }
        
        btn.setOnAction(e -> {
            if (!activeSegment.equals(segmentKey)) {
                activeSegment = segmentKey;
                contentPane.getChildren().clear();
                contentPane.getChildren().add(buildDashboardView());
            }
        });
        return btn;
    }

    private VBox buildDashboardHeaderCard() {
        VBox card = new VBox(12);
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 16;" +
            "-fx-padding: 20;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 70, 67, 0.03), 8, 0, 0, 4);"
        );

        HBox mainRow = new HBox(20);
        mainRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleCol = new VBox(4);
        HBox codeRow = new HBox(8);
        codeRow.setAlignment(Pos.CENTER_LEFT);
        
        Label code = new Label(selectedCourse.code);
        code.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-accent; -fx-background-color: rgba(0, 70, 67, 0.06); -fx-padding: 3 8; -fx-background-radius: 6;");
        Label statusBadge = new Label("Active Status");
        statusBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #059669; -fx-background-color: #d1fae5; -fx-padding: 2 8; -fx-background-radius: 8;");
        codeRow.getChildren().addAll(code, statusBadge);

        Label title = new Label(selectedCourse.title);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");

        Label faculty = new Label("👤 Instructor: " + selectedCourse.faculty);
        faculty.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500;");

        titleCol.getChildren().addAll(codeRow, title, faculty);
        HBox.setHgrow(titleCol, Priority.ALWAYS);

        // Stats grid
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(16);
        statsGrid.setVgap(8);
        statsGrid.setAlignment(Pos.CENTER_RIGHT);

        statsGrid.add(buildCompactStat("LECTURES", selectedCourse.totalClasses), 0, 0);
        statsGrid.add(buildCompactStat("PRESENTS", selectedCourse.presents), 1, 0);
        statsGrid.add(buildCompactStat("ABSENTS", selectedCourse.absents), 2, 0);
        
        double attPct = 0.0;
        try { attPct = Double.parseDouble(selectedCourse.percentage.replace("%","")); } catch(Exception ignored){}
        VBox pctBox = buildCompactStat("ATTENDANCE %", selectedCourse.percentage);
        pctBox.getChildren().get(1).setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: " + (attPct < 75.0 ? "#dc2626;" : "-color-accent;"));
        statsGrid.add(pctBox, 3, 0);

        mainRow.getChildren().addAll(titleCol, statsGrid);
        card.getChildren().add(mainRow);
        return card;
    }

    private VBox buildCompactStat(String header, String val) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: -color-bg-main; -fx-padding: 8 16; -fx-background-radius: 8; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 8; -fx-min-width: 90;");
        
        Label h = new Label(header);
        h.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        Label v = new Label(val);
        v.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        
        box.getChildren().addAll(h, v);
        return box;
    }

    // ==================== SECTION 3: OVERVIEW TAB ====================
    private VBox buildOverviewTab() {
        VBox tab = new VBox(20);
        tab.setFillWidth(true);

        HBox gridRow = new HBox(20);
        gridRow.setFillHeight(true);

        // Course Info Card
        VBox infoCard = new VBox(12);
        infoCard.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12; -fx-padding: 20;");
        HBox.setHgrow(infoCard, Priority.ALWAYS);

        Label title = new Label("📘 Course Details");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");

        VBox infoList = new VBox(8);
        infoList.getChildren().addAll(
            buildInfoRow("Course Title:", selectedCourse.title),
            buildInfoRow("Course Code:", selectedCourse.code),
            buildInfoRow("Faculty Instructor:", selectedCourse.faculty),
            buildInfoRow("Class Section:", selectedCourse.className)
        );

        infoCard.getChildren().addAll(title, infoList);

        // Attendance Summary Card
        VBox attSummaryCard = new VBox(12);
        attSummaryCard.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12; -fx-padding: 20;");
        HBox.setHgrow(attSummaryCard, Priority.ALWAYS);

        Label title2 = new Label("📊 Attendance Statistics");
        title2.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");

        VBox attList = new VBox(8);
        attList.getChildren().addAll(
            buildInfoRow("Total Lectures:", selectedCourse.totalClasses),
            buildInfoRow("Presents:", selectedCourse.presents),
            buildInfoRow("Absents:", selectedCourse.absents)
        );

        // Progress bar for Theory Attendance %
        double theoryPctVal = Double.NaN;
        boolean hasTheory = selectedCourse.theoryPercentage != null && !selectedCourse.theoryPercentage.equalsIgnoreCase("N/A") && !selectedCourse.theoryPercentage.isBlank();
        if (hasTheory) {
            try {
                theoryPctVal = Double.parseDouble(selectedCourse.theoryPercentage.replace("%", "").trim());
            } catch (Exception ignored) {}
        }

        VBox theoryProgressBox = new VBox(4);
        HBox theoryHeaderRow = new HBox(8);
        Label theoryLabel = new Label("Theory Attendance:");
        theoryLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500; -fx-min-width: 160;");
        Label theoryValue = new Label(selectedCourse.theoryPercentage);
        theoryValue.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-main; -fx-font-weight: bold;");
        theoryHeaderRow.getChildren().addAll(theoryLabel, theoryValue);
        theoryProgressBox.getChildren().add(theoryHeaderRow);

        if (!Double.isNaN(theoryPctVal)) {
            ProgressBar theoryPb = new ProgressBar(theoryPctVal / 100.0);
            theoryPb.setMaxWidth(Double.MAX_VALUE);
            theoryPb.setStyle("-fx-accent: -color-accent; -fx-pref-height: 6px; -fx-min-height: 6px; -fx-max-height: 6px;");
            theoryProgressBox.getChildren().add(theoryPb);
        }
        attList.getChildren().add(theoryProgressBox);

        // Progress bar for Lab Attendance %
        double labPctVal = Double.NaN;
        boolean hasLab = selectedCourse.labPercentage != null && !selectedCourse.labPercentage.equalsIgnoreCase("N/A") && !selectedCourse.labPercentage.isBlank();
        if (hasLab) {
            try {
                labPctVal = Double.parseDouble(selectedCourse.labPercentage.replace("%", "").trim());
            } catch (Exception ignored) {}
        }

        VBox labProgressBox = new VBox(4);
        HBox labHeaderRow = new HBox(8);
        Label labLabel = new Label("Lab Attendance:");
        labLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500; -fx-min-width: 160;");
        Label labValue = new Label(selectedCourse.labPercentage);
        labValue.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-main; -fx-font-weight: bold;");
        labHeaderRow.getChildren().addAll(labLabel, labValue);
        labProgressBox.getChildren().add(labHeaderRow);

        if (!Double.isNaN(labPctVal)) {
            ProgressBar labPb = new ProgressBar(labPctVal / 100.0);
            labPb.setMaxWidth(Double.MAX_VALUE);
            labPb.setStyle("-fx-accent: #14b8a6; -fx-pref-height: 6px; -fx-min-height: 6px; -fx-max-height: 6px;");
            labProgressBox.getChildren().add(labPb);
        }
        attList.getChildren().add(labProgressBox);

        attSummaryCard.getChildren().addAll(title2, attList);
        gridRow.getChildren().addAll(infoCard, attSummaryCard);
        tab.getChildren().add(gridRow);

        return tab;
    }

    private HBox buildInfoRow(String k, String v) {
        HBox r = new HBox(8);
        Label kl = new Label(k);
        kl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500; -fx-min-width: 160;");
        Label vl = new Label(v);
        vl.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-main; -fx-font-weight: bold;");
        r.getChildren().addAll(kl, vl);
        return r;
    }

    // ==================== SECTION 4: ATTENDANCE TIMELINE TAB ====================
    private VBox buildAttendanceTab() {
        VBox tab = new VBox(20);
        tab.setFillWidth(true);

        List<LectureEntry> lectures = parseProceedings(proceedingsHtml);

        // OVERALL RADIAL STATS CONTAINER (Maintains visibility at top)
        HBox radialSummary = new HBox(20);
        radialSummary.setAlignment(Pos.CENTER_LEFT);
        radialSummary.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12; -fx-padding: 16;");
        
        double attVal = Double.NaN;
        boolean attExists = false;
        if (selectedCourse.percentage != null && !selectedCourse.percentage.equalsIgnoreCase("N/A") && !selectedCourse.percentage.isBlank()) {
            try {
                attVal = Double.parseDouble(selectedCourse.percentage.replace("%", "").trim());
                attExists = !Double.isNaN(attVal);
            } catch (Exception ignored) {}
        }
        
        ProgressIndicator progressCircle = new ProgressIndicator(attExists ? attVal / 100.0 : -1.0);
        progressCircle.setMinSize(64, 64);
        progressCircle.setMaxSize(64, 64);
        if (attExists) {
            progressCircle.setStyle("-fx-progress-color: " + (attVal < 75.0 ? "#dc2626;" : "-color-accent;"));
        } else {
            progressCircle.setStyle("-fx-progress-color: #64748b;");
        }

        VBox rateCol = new VBox(4);
        rateCol.setAlignment(Pos.CENTER_LEFT);
        Label pctLbl = new Label((attExists ? selectedCourse.percentage : "N/A") + " Presence Rate");
        pctLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        
        Label statusLbl = new Label();
        statusLbl.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 11px; -fx-font-weight: bold;");
        String detailsStr = String.format("Total Lectures: %s  •  Presents: %s  •  Absents: %s", selectedCourse.totalClasses, selectedCourse.presents, selectedCourse.absents);
        boolean hasTheory = selectedCourse.theoryPercentage != null && !selectedCourse.theoryPercentage.equalsIgnoreCase("N/A") && !selectedCourse.theoryPercentage.isBlank();
        boolean hasLab = selectedCourse.labPercentage != null && !selectedCourse.labPercentage.equalsIgnoreCase("N/A") && !selectedCourse.labPercentage.isBlank();
        if (hasTheory) {
            if (hasLab) {
                detailsStr += String.format("  •  Theory: %s  •  Lab: %s", selectedCourse.theoryPercentage, selectedCourse.labPercentage);
            } else {
                detailsStr += String.format("  •  Theory: %s", selectedCourse.theoryPercentage);
            }
        }
        statusLbl.setText(detailsStr);
        rateCol.getChildren().addAll(pctLbl, statusLbl);

        radialSummary.getChildren().addAll(progressCircle, rateCol);

        // Spacer to push filters to the right
        Region statsFiltersSpacer = new Region();
        HBox.setHgrow(statsFiltersSpacer, Priority.ALWAYS);
        radialSummary.getChildren().add(statsFiltersSpacer);

        // Compact filters inline
        HBox inlineFilters = new HBox(12);
        inlineFilters.setAlignment(Pos.CENTER_RIGHT);

        TextField searchField = new TextField(attendanceSearchQuery);
        searchField.setPromptText("Search topics...");
        searchField.setStyle("-fx-font-size: 11px;");
        searchField.setPrefWidth(160);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            attendanceSearchQuery = newVal;
            refreshTimeline(tab, lectures);
        });

        CheckBox showPresent = new CheckBox("P");
        showPresent.setTooltip(new Tooltip("Show Present Lectures"));
        showPresent.setStyle("-fx-font-size: 11px;");
        showPresent.setSelected(filterPresentOnly);
        showPresent.setOnAction(e -> {
            filterPresentOnly = showPresent.isSelected();
            if (filterPresentOnly) {
                filterAbsentOnly = false;
            }
            refreshTimeline(tab, lectures);
        });

        CheckBox showAbsent = new CheckBox("A");
        showAbsent.setTooltip(new Tooltip("Show Absent Lectures"));
        showAbsent.setStyle("-fx-font-size: 11px;");
        showAbsent.setSelected(filterAbsentOnly);
        showAbsent.setOnAction(e -> {
            filterAbsentOnly = showAbsent.isSelected();
            if (filterAbsentOnly) {
                filterPresentOnly = false;
            }
            refreshTimeline(tab, lectures);
        });

        inlineFilters.getChildren().addAll(new Label("🔍"), searchField, showPresent, showAbsent);
        radialSummary.getChildren().add(inlineFilters);

        tab.getChildren().add(radialSummary);

        // TIMELINE CONTAINER
        VBox timelineBox = new VBox(0);
        timelineBox.setStyle("-fx-background-color: transparent;");
        tab.getChildren().add(timelineBox);

        // Populates list row cells
        populateTimeline(timelineBox, lectures);

        return tab;
    }

    private void refreshTimeline(VBox tab, List<LectureEntry> lectures) {
        if (tab.getChildren().size() >= 2 && tab.getChildren().get(1) instanceof VBox) {
            VBox box = (VBox) tab.getChildren().get(1);
            populateTimeline(box, lectures);
        }
    }

    private void populateTimeline(VBox container, List<LectureEntry> lectures) {
        container.getChildren().clear();
        
        List<LectureEntry> filtered = new ArrayList<>();
        for (LectureEntry l : lectures) {
            boolean matchesSearch = attendanceSearchQuery.isEmpty() || l.topic.toLowerCase().contains(attendanceSearchQuery.toLowerCase());
            boolean matchesPresent = !filterPresentOnly || "P".equalsIgnoreCase(l.status) || "Present".equalsIgnoreCase(l.status);
            boolean matchesAbsent = !filterAbsentOnly || "A".equalsIgnoreCase(l.status) || "Absent".equalsIgnoreCase(l.status);
            
            if (matchesSearch && matchesPresent && matchesAbsent) {
                filtered.add(l);
            }
        }

        if (!proceedingsRequestCompleted) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(20));
            Label icon = new Label("⚠");
            icon.setStyle("-fx-font-size: 24px; -fx-text-fill: -color-text-muted;");
            Label lbl = new Label("Class Proceedings request did not complete.");
            lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
            empty.getChildren().addAll(icon, lbl);
            container.getChildren().add(empty);
            return;
        }

        if (lectures.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(20));
            Label icon = new Label("🗓️");
            icon.setStyle("-fx-font-size: 24px;");
            Label lbl = new Label("No Attendance Records Available");
            lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
            empty.getChildren().addAll(icon, lbl);
            container.getChildren().add(empty);
            return;
        }

        if (filtered.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(20));
            Label icon = new Label("🔍");
            icon.setStyle("-fx-font-size: 24px;");
            Label lbl = new Label("No matching lectures found.");
            lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
            empty.getChildren().addAll(icon, lbl);
            container.getChildren().add(empty);
            return;
        }

        // Reverse to show latest lecture at top
        Collections.reverse(filtered);

        for (int i = 0; i < filtered.size(); i++) {
            LectureEntry l = filtered.get(i);
            
            HBox itemRow = new HBox(12);
            itemRow.setAlignment(Pos.TOP_LEFT);
            itemRow.setPadding(new Insets(0, 0, 10, 0));

            // Left graphic indicator: status dot + line
            VBox dotLineCol = new VBox(0);
            dotLineCol.setAlignment(Pos.TOP_CENTER);
            
            Label dot = new Label("●");
            boolean isP = "P".equalsIgnoreCase(l.status) || "Present".equalsIgnoreCase(l.status);
            dot.setStyle("-fx-font-size: 16px; -fx-text-fill: " + (isP ? "#059669;" : "#dc2626;"));
            
            Region line = new Region();
            line.getStyleClass().add("timeline-line");
            VBox.setVgrow(line, Priority.ALWAYS);
            line.setMinHeight(20);
            
            dotLineCol.getChildren().add(dot);
            if (i < filtered.size() - 1) {
                dotLineCol.getChildren().add(line);
            }

            // Right timeline card
            VBox timelineCard = new VBox(6);
            timelineCard.getStyleClass().add("timeline-card");
            HBox.setHgrow(timelineCard, Priority.ALWAYS);

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label("Lecture " + l.lectureNo);
            name.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
            
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label date = new Label("📅 " + l.date + (l.duration.isEmpty() ? "" : " (" + l.duration + " Hrs)"));
            date.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

            Label badge = new Label(isP ? "Present" : "Absent");
            badge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: " + (isP ? "#059669;" : "#dc2626;") + " -fx-background-color: " + (isP ? "#d1fae5;" : "#fee2e2;") + " -fx-padding: 2 8; -fx-background-radius: 8;");
            
            header.getChildren().addAll(name, date, sp, badge);

            Label topic = new Label(l.topic);
            topic.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-main; -fx-font-weight: 500;");
            topic.setWrapText(true);

            timelineCard.getChildren().addAll(header, topic);
            
            itemRow.getChildren().addAll(dotLineCol, timelineCard);
            container.getChildren().add(itemRow);
        }
    }

    // ==================== SECTION 5: PERFORMANCE TAB ====================
    // ==================== SECTION 5: PERFORMANCE TAB ====================
    private VBox buildPerformanceTab() {
        VBox tab = new VBox(20);
        tab.setFillWidth(true);

        if (!marksRequestCompleted) {
            VBox notCompletedBox = new VBox(12);
            notCompletedBox.setAlignment(Pos.CENTER);
            notCompletedBox.setPadding(new Insets(40));
            notCompletedBox.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12;");
            
            Label icon = new Label("⚠");
            icon.setStyle("-fx-font-size: 24px; -fx-text-fill: -color-text-muted;");
            Label lbl = new Label("QAMarks request did not complete.");
            lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
            notCompletedBox.getChildren().addAll(icon, lbl);
            
            tab.getChildren().add(notCompletedBox);
            return tab;
        }

        List<MarksCategory> categories = parseMarksCategories(marksHtml);

        VBox quizSection = buildCategorySection("📚 Quiz Marks", categories, "Quizzes", "No quiz records available.", "-color-accent");
        VBox assignSection = buildCategorySection("📝 Assignment Marks", categories, "Assignments", "No assignment records available.", "#14b8a6");
        VBox sessSection = buildCategorySection("📊 Sessional Marks", categories, "Sessionals", "No sessional marks available.", "#f59e0b");
        VBox finalSection = buildCategorySection("🏆 Final Exam Marks", categories, "Final Exam", "No final marks available.", "#8b5cf6");

        List<VBox> activeCards = new ArrayList<>();
        if (quizSection != null) activeCards.add(quizSection);
        if (assignSection != null) activeCards.add(assignSection);
        if (sessSection != null) activeCards.add(sessSection);
        if (finalSection != null) activeCards.add(finalSection);

        if (activeCards.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            emptyBox.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12;");
            
            Label icon = new Label("📝");
            icon.setStyle("-fx-font-size: 24px;");
            Label lbl = new Label("No Quiz Records Available");
            lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
            emptyBox.getChildren().addAll(icon, lbl);
            
            tab.getChildren().add(emptyBox);
            return tab;
        }

        if (activeCards.size() == 1) {
            VBox card = activeCards.get(0);
            card.setMaxWidth(Double.MAX_VALUE);
            tab.getChildren().add(card);
        } else if (activeCards.size() == 2) {
            HBox hbox = new HBox(20);
            hbox.setFillHeight(false);
            hbox.setAlignment(Pos.TOP_LEFT);
            for (VBox card : activeCards) {
                HBox.setHgrow(card, Priority.ALWAYS);
                card.setMaxWidth(Double.MAX_VALUE);
                hbox.getChildren().add(card);
            }
            tab.getChildren().add(hbox);
        } else {
            GridPane grid = new GridPane();
            grid.setHgap(20);
            grid.setVgap(20);
            
            ColumnConstraints col1 = new ColumnConstraints();
            col1.setPercentWidth(50);
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setPercentWidth(50);
            grid.getColumnConstraints().addAll(col1, col2);

            for (int i = 0; i < activeCards.size(); i++) {
                VBox card = activeCards.get(i);
                int row = i / 2;
                int col = i % 2;
                grid.add(card, col, row);
            }
            tab.getChildren().add(grid);
        }

        return tab;
    }

    private VBox buildCategorySection(String title, List<MarksCategory> categories, String name, String emptyMessage, String colorTheme) {
        MarksCategory matched = null;
        for (MarksCategory c : categories) {
            String catName = c.categoryName.toLowerCase();
            boolean match = false;
            if (name.equals("Quizzes") && catName.contains("quiz")) match = true;
            else if (name.equals("Assignments") && catName.contains("assign")) match = true;
            else if (name.equals("Sessionals") && catName.contains("sess")) match = true;
            else if (name.equals("Final Exam") && (catName.contains("final") || catName.contains("term"))) match = true;
            
            if (match) {
                if (matched == null) {
                    matched = new MarksCategory();
                    matched.categoryName = name;
                }
                matched.items.addAll(c.items);
                matched.totalMax += c.totalMax;
                matched.totalObtained += c.totalObtained;
            }
        }

        if (matched == null || matched.items.isEmpty()) {
            return null;
        }

        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12; -fx-padding: 16;");
        HBox.setHgrow(card, Priority.ALWAYS);
        
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        card.getChildren().add(t);

        VBox list = new VBox(10);
        for (MarkItem item : matched.items) {
            VBox row = new VBox(4);
            row.setStyle("-fx-background-color: -color-bg-main; -fx-padding: 8 12; -fx-background-radius: 8; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 8;");
            
            HBox top = new HBox(8);
            top.setAlignment(Pos.CENTER_LEFT);
            
            VBox labelCol = new VBox(2);
            Label titleLbl = new Label(item.title);
            titleLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
            labelCol.getChildren().add(titleLbl);
            
            if (item.date != null && !item.date.isBlank()) {
                Label dateLbl = new Label("📅 " + item.date);
                dateLbl.setStyle("-fx-font-size: 9px; -fx-text-fill: -color-text-muted;");
                labelCol.getChildren().add(dateLbl);
            }
            
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label marks = new Label(item.obtainedMarks + " / " + item.totalMarks);
            marks.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");

            double pct = 0.0;
            try { pct = Double.parseDouble(item.percentage.replace("%","")); } catch(Exception ignored){}
            
            Label badge = new Label(item.percentage);
            badge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: " + (pct < 60.0 ? "#dc2626;" : "#059669;") + " -fx-background-color: " + (pct < 60.0 ? "#fee2e2;" : "#d1fae5;") + " -fx-padding: 2 6; -fx-background-radius: 6;");

            top.getChildren().addAll(labelCol, sp, marks, badge);

            ProgressBar pb = new ProgressBar(pct / 100.0);
            pb.setMaxWidth(Double.MAX_VALUE);
            pb.setPrefHeight(4);
            pb.setMinHeight(4);
            pb.setMaxHeight(4);
            pb.setStyle("-fx-accent: " + colorTheme + "; -fx-pref-height: 4px; -fx-min-height: 4px; -fx-max-height: 4px;");

            row.getChildren().addAll(top, pb);
            list.getChildren().add(row);
        }

        card.getChildren().add(list);
        return card;
    }

    // ==================== SHARED HTML PARSERS ====================
    public static List<CourseSummary> parseSummaryCourses(String html) {
        return parseSummaryCourses(html, null);
    }

    public static List<CourseSummary> parseSummaryCourses(String html, Map<String, String> courseNames) {
        List<CourseSummary> list = new ArrayList<>();
        if (html == null || html.isBlank()) return list;
        Document doc = Jsoup.parse(html);

        for (Element table : doc.select("table")) {
            String tt = table.text().toLowerCase();
            if (tt.contains("father name") && tt.contains("roll no")) continue;
            if (tt.contains("cnic") && tt.contains("date of birth")) continue;
            Elements rows = table.select("tr");
            if (rows.size() < 2) continue;

            Element hdr = null;
            int hdrIdx = -1;
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).select("th, td").size() > 1) {
                    hdr = rows.get(i);
                    hdrIdx = i;
                    break;
                }
            }
            if (hdr == null) continue;

            Elements ths = hdr.select("th, td");
            List<String> headers = new ArrayList<>();
            for (Element c : ths) headers.add(c.text().trim().toLowerCase());

            int codeIdx = -1, titleIdx = -1, classIdx = -1, facIdx = -1, totIdx = -1, presIdx = -1, absIdx = -1, thyIdx = -1, labIdx = -1, pctIdx = -1;
            for (int i = 0; i < headers.size(); i++) {
                String h = headers.get(i);
                if (h.contains("code")) codeIdx = i;
                else if (h.contains("title") || h.contains("subject")) titleIdx = i;
                else if (h.equals("class")) classIdx = i;
                else if (h.contains("faculty") || h.contains("teacher") || h.contains("member")) facIdx = i;
                else if (h.contains("lectures") || h.contains("total")) totIdx = i;
                else if (h.equals("p") || h.contains("present")) presIdx = i;
                else if (h.equals("a") || h.contains("absent")) absIdx = i;
                else if (h.contains("thy%") || h.equals("thy")) thyIdx = i;
                else if (h.contains("lab%") || h.equals("lab")) labIdx = i;
                else if (h.contains("percentage") || h.contains("%")) pctIdx = i;
            }

            if (codeIdx == -1 && titleIdx == -1) {
                codeIdx = -1; titleIdx = 1; classIdx = 2; facIdx = 3; totIdx = 4; presIdx = 5; absIdx = 6; thyIdx = 7; labIdx = 8;
            }

            for (int r = hdrIdx + 1; r < rows.size(); r++) {
                Elements cells = rows.get(r).select("td");
                if (cells.size() <= Math.max(codeIdx, titleIdx)) continue;

                CourseSummary cs = new CourseSummary();
                cs.serial = String.valueOf(r - hdrIdx);
                cs.code = codeIdx >= 0 && codeIdx < cells.size() ? cells.get(codeIdx).text().trim() : "";
                cs.title = titleIdx >= 0 && titleIdx < cells.size() ? cells.get(titleIdx).text().trim() : "";
                cs.className = classIdx >= 0 && classIdx < cells.size() ? cells.get(classIdx).text().trim() : "N/A";
                cs.faculty = facIdx >= 0 && facIdx < cells.size() ? cells.get(facIdx).text().trim() : "N/A";
                cs.totalClasses = totIdx >= 0 && totIdx < cells.size() ? cells.get(totIdx).text().trim() : "0";
                cs.presents = presIdx >= 0 && presIdx < cells.size() ? cells.get(presIdx).text().trim() : "0";
                cs.absents = absIdx >= 0 && absIdx < cells.size() ? cells.get(absIdx).text().trim() : "0";
                cs.theoryPercentage = thyIdx >= 0 && thyIdx < cells.size() ? cells.get(thyIdx).text().trim() : "N/A";
                cs.labPercentage = labIdx >= 0 && labIdx < cells.size() ? cells.get(labIdx).text().trim() : "N/A";
                
                if (pctIdx >= 0 && pctIdx < cells.size()) {
                    cs.percentage = cells.get(pctIdx).text().trim();
                } else {
                    cs.percentage = cs.theoryPercentage;
                }

                if (cs.code.isEmpty() && !cs.title.isEmpty() && cs.title.contains("\n")) {
                    String[] parts = cs.title.split("\n");
                    cs.code = parts[0].trim();
                    cs.title = parts[1].trim();
                } else if (cs.code.isEmpty() && !cs.title.isEmpty()) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("^([A-Za-z]{2,4}-?\\d{3})[\\s\\-•]*(.*)$").matcher(cs.title);
                    if (m.find()) {
                        cs.code = m.group(1).trim();
                        cs.title = m.group(2).trim();
                    }
                }

                Element aTag = null;
                if (titleIdx >= 0 && titleIdx < cells.size()) aTag = cells.get(titleIdx).select("a").first();
                if (aTag == null && codeIdx >= 0 && codeIdx < cells.size()) aTag = cells.get(codeIdx).select("a").first();
                if (aTag != null && aTag.attr("href").contains("__doPostBack")) {
                    String href = aTag.attr("href");
                    int start = href.indexOf("'") + 1;
                    int end = href.indexOf("'", start);
                    if (start > 0 && end > start) {
                        cs.postbackTarget = href.substring(start, end);
                    }
                }

                if (!cs.title.isEmpty() || !cs.code.isEmpty()) {
                    list.add(cs);
                }
            }
        }
        if (courseNames != null) {
            resolveCourseCodes(list, courseNames);
        }
        return list;
    }

    public static void resolveCourseCodes(List<CourseSummary> courses, Map<String, String> courseNames) {
        if (courses == null || courseNames == null || courseNames.isEmpty()) return;
        for (CourseSummary cs : courses) {
            if (cs.code == null || cs.code.isEmpty()) {
                String cleanTitle = cs.title.trim().toLowerCase().replaceAll("\\s+|-|•|–", "");
                for (Map.Entry<String, String> entry : courseNames.entrySet()) {
                    String cleanMapVal = entry.getValue().trim().toLowerCase().replaceAll("\\s+|-|•|–", "");
                    if (!cleanMapVal.isEmpty() && (cleanMapVal.contains(cleanTitle) || cleanTitle.contains(cleanMapVal))) {
                        cs.code = entry.getKey();
                        break;
                    }
                }
            }
        }
    }

    private List<LectureEntry> parseProceedings(String html) {
        List<LectureEntry> list = new ArrayList<>();
        if (html == null || html.isBlank()) return list;
        Document doc = Jsoup.parse(html);

        for (Element table : doc.select("table")) {
            Elements rows = table.select("tr");
            if (rows.size() < 2) continue;
            Element hdr = rows.first();
            String hdrText = hdr.text().toLowerCase();
            if (hdrText.contains("lecture") || hdrText.contains("date") || hdrText.contains("topic") || hdrText.contains("status")) {
                Elements ths = hdr.select("th, td");
                List<String> headers = new ArrayList<>();
                for (Element c : ths) headers.add(c.text().trim().toLowerCase());

                int numIdx = -1, dateIdx = -1, durIdx = -1, topicIdx = -1, statusIdx = -1;
                for (int i = 0; i < headers.size(); i++) {
                    String h = headers.get(i);
                    if (h.contains("lecture") || h.contains("s#") || h.equals("s")) numIdx = i;
                    else if (h.contains("date")) dateIdx = i;
                    else if (h.contains("duration")) durIdx = i;
                    else if (h.contains("topic") || h.contains("particular") || h.contains("description")) topicIdx = i;
                    else if (h.contains("status")) statusIdx = i;
                }

                for (int r = 1; r < rows.size(); r++) {
                    Elements cells = rows.get(r).select("td");
                    if (cells.size() <= Math.max(numIdx, dateIdx)) continue;

                    LectureEntry le = new LectureEntry();
                    le.lectureNo = numIdx >= 0 && numIdx < cells.size() ? cells.get(numIdx).text().trim() : String.valueOf(r);
                    le.date = dateIdx >= 0 && dateIdx < cells.size() ? cells.get(dateIdx).text().trim() : "";
                    le.duration = durIdx >= 0 && durIdx < cells.size() ? cells.get(durIdx).text().trim() : "";
                    le.topic = topicIdx >= 0 && topicIdx < cells.size() ? cells.get(topicIdx).text().trim() : "No Topic Specified";
                    le.status = statusIdx >= 0 && statusIdx < cells.size() ? cells.get(statusIdx).text().trim() : "";

                    if (!le.date.isEmpty()) {
                        list.add(le);
                    }
                }
            }
        }
        return list;
    }

    public static List<MarksCategory> parseMarksCategories(String html) {
        List<MarksCategory> list = new ArrayList<>();
        if (html == null || html.isBlank()) return list;
        Document doc = Jsoup.parse(html);

        for (Element table : doc.select("table")) {
            Elements rows = table.select("tr");
            if (rows.size() < 2) continue;

            Element hdrRow = null;
            int hdrIdx = -1;
            String tableTitle = "";
            for (int i = 0; i < rows.size(); i++) {
                Elements cells = rows.get(i).select("th, td");
                if (cells.size() == 1 && hdrRow == null) {
                    tableTitle = cells.first().text().trim();
                }
                if (cells.size() > 1) {
                    hdrRow = rows.get(i);
                    hdrIdx = i;
                    break;
                }
            }
            if (hdrRow == null) continue;

            Elements ths = hdrRow.select("th, td");
            List<String> headers = new ArrayList<>();
            for (Element c : ths) headers.add(c.text().trim().toLowerCase());

            // Skip profile info tables
            boolean skip = false;
            for (String h : headers) {
                if (h.contains("father name") || h.contains("cnic") || h.contains("advisor") || h.contains("roll no")) {
                    skip = true;
                    break;
                }
            }
            if (skip) continue;

            int titleIdx = -1, dateIdx = -1, totalIdx = -1, obtIdx = -1, pctIdx = -1;
            for (int i = 0; i < headers.size(); i++) {
                String h = headers.get(i);
                if (h.contains("quiz") || h.contains("assignment") || h.contains("particular") || h.contains("topic") || h.contains("title") || h.contains("subject") || h.contains("name")) titleIdx = i;
                else if (h.contains("date")) dateIdx = i;
                else if (h.contains("total") || h.contains("max")) totalIdx = i;
                else if (h.contains("obtain") || h.contains("obt") || h.contains("marks")) {
                    if (!h.contains("total")) obtIdx = i;
                }
                else if (h.contains("percentage") || h.contains("%")) pctIdx = i;
            }

            if (titleIdx >= 0) {
                MarksCategory category = new MarksCategory();
                category.categoryName = tableTitle.isEmpty() ? "Assessment Details" : tableTitle;
                
                String lowerName = category.categoryName.toLowerCase();
                if (lowerName.contains("quiz")) category.categoryName = "Quizzes";
                else if (lowerName.contains("assignment")) category.categoryName = "Assignments";
                else if (lowerName.contains("sessional")) category.categoryName = "Sessionals";
                else if (lowerName.contains("final") || lowerName.contains("terminal")) category.categoryName = "Final Exam";

                for (int r = hdrIdx + 1; r < rows.size(); r++) {
                    Element row = rows.get(r);
                    String rowText = row.text().toLowerCase();
                    if (!row.select(".GridFooter").isEmpty() || rowText.contains("projected") || rowText.contains("aggregate") || rowText.contains("total marks") || rowText.trim().equals("=")) {
                        continue;
                    }
                    Elements cells = row.select("td");
                    if (cells.size() <= titleIdx) continue;

                    MarkItem item = new MarkItem();
                    item.title = cells.get(titleIdx).text().trim();
                    item.date = dateIdx >= 0 && dateIdx < cells.size() ? cells.get(dateIdx).text().trim() : "";
                    item.totalMarks = totalIdx >= 0 && totalIdx < cells.size() ? cells.get(totalIdx).text().trim() : "10";
                    item.obtainedMarks = obtIdx >= 0 && obtIdx < cells.size() ? cells.get(obtIdx).text().trim() : "";
                    item.percentage = pctIdx >= 0 && pctIdx < cells.size() ? cells.get(pctIdx).text().trim() : "";
                    if (item.obtainedMarks.isEmpty()) continue;

                    try {
                        double max = Double.parseDouble(item.totalMarks.replaceAll("[^0-9.]", ""));
                        double obt = Double.parseDouble(item.obtainedMarks.replaceAll("[^0-9.]", ""));
                        category.totalMax += max;
                        category.totalObtained += obt;
                        if (item.percentage.isEmpty() && max > 0) {
                            item.percentage = String.format("%.0f%%", (obt / max) * 100);
                        }
                    } catch (Exception ignored) {}

                    category.items.add(item);
                }

                if (!category.items.isEmpty()) {
                    if (category.totalMax > 0) {
                        category.averagePct = (category.totalObtained / category.totalMax) * 100;
                    }
                    list.add(category);
                }
            }
        }
        return list;
    }

    private String findCourseDropdownValue(String pageHtml, String title, String code) {
        if (pageHtml == null || pageHtml.isBlank()) return null;
        List<String[]> options = context.portalRepository().parseDropdownOptions(pageHtml, "course");
        if (options.isEmpty()) {
            options.addAll(context.portalRepository().parseDropdownOptions(pageHtml, "ddl"));
        }

        String cleanTitle = title.trim().toLowerCase().replaceAll("\\s+|-|•|–", "");
        String cleanCode = code.trim().toLowerCase().replaceAll("\\s+|-|•|–", "");
        for (String[] opt : options) {
            String cleanLabel = opt[1].trim().toLowerCase().replaceAll("\\s+|-|•|–", "");
            if (!cleanCode.isEmpty() && (cleanLabel.contains(cleanCode) || cleanCode.contains(cleanLabel))) {
                return opt[0];
            }
            if (!cleanTitle.isEmpty() && (cleanLabel.contains(cleanTitle) || cleanTitle.contains(cleanLabel))) {
                return opt[0];
            }
        }
        return null;
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
