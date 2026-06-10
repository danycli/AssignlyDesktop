package com.assignly.view;

import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Modern Native Timetable View with a Today-First Academic Planner workflow.
 * Displays today's classes chronologically, computes starts-in countdown widgets,
 * shows quick stats summaries, provides Week Grid and List views, and opens a details
 * side panel showing cached attendance rates and recent class proceedings timeline logs.
 */
public class TimetableTabView {

    private final VBox root = new VBox();
    private final AppContext context;

    // Cache states
    private List<ClassSession> allSessions = new ArrayList<>();
    private String rawHtml = "";
    private Map<String, String> courseNamesMap = new HashMap<>();

    // UI state
    private String activeMode = "today"; // "today", "week", "list"
    private String searchQuery = "";
    private String selectedTypeFilter = "All Classes";
    private boolean showEmptyDays = false;
    private final HBox statsBarContainer = new HBox();

    // Layout nodes
    private BorderPane mainBorderPane;
    private Button btnToday;
    private Button btnWeek;
    private Button btnList;
    private ToggleButton btnEmptyDays;

    public static class ClassSession {
        public String day = "";
        public String timeRange = "";
        public String courseName = "";
        public String courseCode = "";
        public String section = "";
        public String teacher = "";
        public String room = "";
        public boolean isLab = false;
        public String color = "#64748b";
        public LocalTime startTime;
        public LocalTime endTime;
        public int startCol = -1;
        public int endCol = -1;
    }

    public static class LectureEntry {
        public String date = "";
        public String topic = "";
        public String status = "";
    }

    private static final String ACTIVE_TAB_STYLE = 
        "-fx-background-color: -color-accent;" +
        "-fx-text-fill: white;" +
        "-fx-font-weight: bold;" +
        "-fx-font-size: 11px;" +
        "-fx-padding: 6 14;" +
        "-fx-background-radius: 6;" +
        "-fx-cursor: hand;";
        
    private static final String INACTIVE_TAB_STYLE = 
        "-fx-background-color: transparent;" +
        "-fx-text-fill: -color-text-muted;" +
        "-fx-font-weight: bold;" +
        "-fx-font-size: 11px;" +
        "-fx-padding: 6 14;" +
        "-fx-background-radius: 6;" +
        "-fx-cursor: hand;";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    public TimetableTabView(AppContext context) {
        this.context = context;
        buildShell();
        loadTimetable(false);
    }

    public VBox getRoot() { return root; }

    private void buildShell() {
        root.setFillWidth(true);
        VBox.setVgrow(root, Priority.ALWAYS);
        root.setStyle("-fx-background-color: -color-bg-main;");
        
        // 1. Header Row
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(16, 20, 12, 20));
        headerRow.setStyle("-fx-background-color: -color-bg-card;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");

        Label title = new Label("Student Timetable");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Segmented tab switchers
        HBox tabGroup = new HBox(4);
        tabGroup.setStyle("-fx-background-color: -color-bg-main; -fx-background-radius: 8; -fx-padding: 4; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 8;");
        
        btnToday = new Button("Today");
        btnToday.setOnAction(e -> switchView("today"));
        
        btnWeek = new Button("Week Grid");
        btnWeek.setOnAction(e -> switchView("week"));
        
        btnList = new Button("All List");
        btnList.setOnAction(e -> switchView("list"));
        
        tabGroup.getChildren().addAll(btnToday, btnWeek, btnList);
        headerRow.getChildren().addAll(title, spacer, tabGroup);
        root.getChildren().add(headerRow);

        // 2. Filter bar row
        HBox filterBar = buildFilterBar();
        root.getChildren().add(filterBar);

        // 3. Summary stats bar (New)
        statsBarContainer.setAlignment(Pos.CENTER_LEFT);
        statsBarContainer.setPadding(new Insets(8, 20, 8, 20));
        statsBarContainer.setStyle("-fx-background-color: -color-bg-main; -fx-border-color: -color-border; -fx-border-width: 0 0 1 0;");
        statsBarContainer.setSpacing(16);
        root.getChildren().add(statsBarContainer);

        // 4. Main content BorderPane
        mainBorderPane = new BorderPane();
        VBox.setVgrow(mainBorderPane, Priority.ALWAYS);
        root.getChildren().add(mainBorderPane);
    }

    private HBox buildFilterBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 20, 6, 20));
        bar.setStyle("-fx-background-color: -color-bg-main; -fx-border-color: -color-border; -fx-border-width: 0 0 1 0;");
        
        TextField searchTxt = new TextField();
        searchTxt.setPromptText("Search course name, code, room or teacher...");
        searchTxt.setPrefWidth(240);
        searchTxt.getStyleClass().add("search-bar");
        searchTxt.textProperty().addListener((obs, oldVal, newVal) -> {
            this.searchQuery = newVal.trim().toLowerCase();
            refreshCurrentView();
        });
        
        // Segmented filter control (New)
        HBox segFilterGroup = new HBox(6);
        segFilterGroup.setAlignment(Pos.CENTER_LEFT);
        
        Button btnAll = new Button("📚 All Classes");
        Button btnTheory = new Button("📖 Theory Sessions");
        Button btnLab = new Button("🧪 Lab Sessions");
        
        btnAll.setOnAction(e -> {
            selectedTypeFilter = "All Classes";
            updateFilterButtonStyles(btnAll, btnTheory, btnLab);
            refreshCurrentView();
        });
        btnTheory.setOnAction(e -> {
            selectedTypeFilter = "Theory Sessions";
            updateFilterButtonStyles(btnAll, btnTheory, btnLab);
            refreshCurrentView();
        });
        btnLab.setOnAction(e -> {
            selectedTypeFilter = "Lab Sessions";
            updateFilterButtonStyles(btnAll, btnTheory, btnLab);
            refreshCurrentView();
        });
        
        updateFilterButtonStyles(btnAll, btnTheory, btnLab);
        segFilterGroup.getChildren().addAll(btnAll, btnTheory, btnLab);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        btnEmptyDays = new ToggleButton("Show Empty Days");
        btnEmptyDays.setSelected(showEmptyDays);
        btnEmptyDays.setStyle(
            "-fx-background-color: " + (showEmptyDays ? "-color-accent" : "-color-bg-card") + ";" +
            "-fx-text-fill: " + (showEmptyDays ? "white" : "-color-text-main") + ";" +
            "-fx-border-color: -color-border;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 4 10;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;"
        );
        btnEmptyDays.setOnAction(e -> {
            showEmptyDays = btnEmptyDays.isSelected();
            btnEmptyDays.setStyle(
                "-fx-background-color: " + (showEmptyDays ? "-color-accent" : "-color-bg-card") + ";" +
                "-fx-text-fill: " + (showEmptyDays ? "white" : "-color-text-main") + ";" +
                "-fx-border-color: -color-border;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 4 10;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
            );
            refreshCurrentView();
        });
        
        bar.getChildren().addAll(searchTxt, segFilterGroup, spacer, btnEmptyDays);
        return bar;
    }

    private void updateFilterButtonStyles(Button btnAll, Button btnTheory, Button btnLab) {
        setFilterButtonStyle(btnAll, "All Classes".equals(selectedTypeFilter));
        setFilterButtonStyle(btnTheory, "Theory Sessions".equals(selectedTypeFilter));
        setFilterButtonStyle(btnLab, "Lab Sessions".equals(selectedTypeFilter));
    }
    
    private void setFilterButtonStyle(Button btn, boolean isSelected) {
        String baseStyle = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6 12; -fx-cursor: hand;";
        if (isSelected) {
            btn.setStyle(
                baseStyle +
                "-fx-background-color: -color-accent;" +
                "-fx-text-fill: white;" +
                "-fx-border-color: -color-accent;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;"
            );
            btn.setOnMouseEntered(null);
            btn.setOnMouseExited(null);
        } else {
            btn.setStyle(
                baseStyle +
                "-fx-background-color: -color-bg-card;" +
                "-fx-text-fill: -color-text-muted;" +
                "-fx-border-color: -color-border;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;"
            );
            
            // Set hover behavior
            btn.setOnMouseEntered(e -> {
                btn.setStyle(
                    baseStyle +
                    "-fx-background-color: -color-bg-hover;" +
                    "-fx-text-fill: -color-text-main;" +
                    "-fx-border-color: -color-border;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 6;" +
                    "-fx-background-radius: 6;"
                );
            });
            btn.setOnMouseExited(e -> {
                if (!selectedTypeFilter.equals(getFilterTypeFromButton(btn))) {
                    setFilterButtonStyle(btn, false);
                }
            });
        }
    }
    
    private String getFilterTypeFromButton(Button btn) {
        String txt = btn.getText();
        if (txt.contains("All Classes")) return "All Classes";
        if (txt.contains("Theory Sessions")) return "Theory Sessions";
        if (txt.contains("Lab Sessions")) return "Lab Sessions";
        return "All Classes";
    }

    private void switchView(String mode) {
        this.activeMode = mode;
        
        btnToday.setStyle(mode.equals("today") ? ACTIVE_TAB_STYLE : INACTIVE_TAB_STYLE);
        btnWeek.setStyle(mode.equals("week") ? ACTIVE_TAB_STYLE : INACTIVE_TAB_STYLE);
        btnList.setStyle(mode.equals("list") ? ACTIVE_TAB_STYLE : INACTIVE_TAB_STYLE);
        
        if (btnEmptyDays != null) {
            btnEmptyDays.setVisible(!mode.equals("today"));
            btnEmptyDays.setManaged(!mode.equals("today"));
        }
        
        updateSummaryStatsBar();
        
        Node viewNode = switch (mode) {
            case "today" -> buildTodayView();
            case "week" -> buildWeekView();
            case "list" -> buildListView();
            default -> new Label("Unknown View");
        };
        
        mainBorderPane.setCenter(viewNode);
    }

    private void refreshCurrentView() {
        switchView(activeMode);
    }

    private void buildLoading() {
        ScrollPane sp = new ScrollPane(com.assignly.util.ShimmerBuilder.buildTimetableShimmer());
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        mainBorderPane.setCenter(sp);
    }

    private void loadTimetable(boolean forceRefresh) {
        buildLoading();
        new Thread(() -> {
            try {
                String html = null;
                boolean isOffline = false;

                if (!forceRefresh) {
                    html = context.dataCacheService().getCachedHtml("Timetable.aspx").orElse(null);
                }

                if (html == null) {
                    html = context.fetchAndCacheHtml("Timetable.aspx");
                    if (html == null) {
                        html = context.dataCacheService().getCachedHtml("Timetable.aspx").orElse(null);
                        isOffline = true;
                    }
                }

                if (html == null) {
                    Platform.runLater(() -> {
                        mainBorderPane.setCenter(buildError("Unable to load timetable.", "Failed to connect to the portal and no offline data available."));
                    });
                    return;
                }
                
                Document doc = Jsoup.parse(html);
                rawHtml = html;
                allSessions = parseSessions(doc);
                System.out.println("[Timetable] Parsed " + allSessions.size() + " sessions from HTML.");
                
                // Pre-fetch course names mapping
                try {
                    String resultHtml = context.dataCacheService().getCachedHtml("StudentResultCard.aspx").orElse(null);
                    if (resultHtml != null) {
                        courseNamesMap = context.portalRepository().parseCourseNames(resultHtml);
                    }
                } catch (Exception ignored) {}

                boolean finalOffline = isOffline;
                Platform.runLater(() -> {
                    mainBorderPane.setTop(finalOffline ? buildOfflineBanner() : null);
                    switchView(activeMode);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    mainBorderPane.setCenter(buildError("Error Loading Timetable", e.getMessage()));
                });
            }
        }).start();
    }

    // ==================== Build Tab Layouts ====================

    private void updateSummaryStatsBar() {
        statsBarContainer.getChildren().clear();
        
        List<ClassSession> filtered = getFilteredSessions();
        if (filtered == null) filtered = new ArrayList<>();
        
        String todayDay = getTodayDayName();
        LocalTime now = LocalTime.now();
        
        int todayCount = 0;
        int labsThisWeek = 0;
        int totalWeekly = filtered.size();
        
        ClassSession nextClass = null;
        
        List<ClassSession> todaySessions = new ArrayList<>();
        for (ClassSession s : filtered) {
            if (s == null) continue;
            if (s.isLab) {
                labsThisWeek++;
            }
            if (s.day != null && s.day.equalsIgnoreCase(todayDay)) {
                todayCount++;
                todaySessions.add(s);
            }
        }
        
        todaySessions.sort((s1, s2) -> {
            if (s1.startTime == null && s2.startTime == null) return 0;
            if (s1.startTime == null) return 1;
            if (s2.startTime == null) return -1;
            return s1.startTime.compareTo(s2.startTime);
        });
        
        for (ClassSession s : todaySessions) {
            String status = getClassStatus(s, todayDay, now);
            if (status.equals("Upcoming")) {
                nextClass = s;
                break;
            }
        }
        
        String nextClassText = "None";
        if (nextClass != null) {
            nextClassText = nextClass.courseName + " (" + nextClass.timeRange + ")";
        }
        
        statsBarContainer.getChildren().addAll(
            buildStatCard("TODAY'S CLASSES", String.valueOf(todayCount), "-color-accent"),
            buildStatCard("NEXT CLASS", nextClassText, "#f59e0b"),
            buildStatCard("LABS THIS WEEK", String.valueOf(labsThisWeek), "#6366f1"),
            buildStatCard("WEEKLY CLASSES", String.valueOf(totalWeekly), "-color-text-main")
        );
    }
    
    private VBox buildStatCard(String label, String value, String valueColor) {
        VBox card = new VBox(2);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;"
        );
        card.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);
        
        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + valueColor + ";");
        valLbl.setWrapText(true);
        
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        
        card.getChildren().addAll(valLbl, lbl);
        return card;
    }

    private VBox buildProminentFocusCard(ClassSession s, String title, String accentColor, LocalTime now) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-border-color: " + accentColor + ";" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(three-pass-box, " + accentColor + "15, 10, 0, 0, 4);"
        );
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> showDetailsDrawer(s));
        
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        Label badge = new Label(title);
        badge.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: white; -fx-background-color: " + accentColor + "; -fx-padding: 3 8; -fx-background-radius: 6;");
        
        Label typeBadge = new Label(s.isLab ? "LAB SESSION" : "THEORY CLASS");
        typeBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: " + s.color + "; -fx-background-color: " + s.color + "15; -fx-padding: 3 8; -fx-background-radius: 6;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        String countdownText = "";
        if (s.startTime != null && s.endTime != null) {
            if (title.contains("LIVE")) {
                java.time.Duration duration = java.time.Duration.between(now, s.endTime);
                long diffMins = Math.max(0, duration.toMinutes());
                countdownText = String.format("Ends In: %02dh %02dm", diffMins / 60, diffMins % 60);
            } else {
                java.time.Duration duration = java.time.Duration.between(now, s.startTime);
                long diffMins = Math.max(0, duration.toMinutes());
                countdownText = String.format("Starts In: %02dh %02dm", diffMins / 60, diffMins % 60);
            }
        }
        
        Label countdownLbl = new Label(countdownText);
        countdownLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + accentColor + ";");
        
        topRow.getChildren().addAll(badge, typeBadge, spacer, countdownLbl);
        
        Label courseNameLabel = new Label(s.courseName);
        courseNameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        
        Label codeSec = new Label(s.courseCode + (s.section.isEmpty() ? "" : "  •  Section " + s.section));
        codeSec.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
        
        HBox infoRow = new HBox(20);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        infoRow.setPadding(new Insets(4, 0, 0, 0));
        
        Label timeLbl = new Label("🕒  " + s.timeRange);
        timeLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        
        Label roomLbl = new Label("📍  Room " + s.room);
        roomLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        
        Label facultyLbl = new Label("👤  " + s.teacher);
        facultyLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
        
        infoRow.getChildren().addAll(timeLbl, roomLbl, facultyLbl);
        card.getChildren().addAll(topRow, courseNameLabel, codeSec, infoRow);
        
        card.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
            tt.setToY(-2);
            tt.play();
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-border-color: " + accentColor + ";" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, " + accentColor + "25, 12, 0, 0, 6);"
            );
        });
        card.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
            tt.setToY(0);
            tt.play();
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-border-color: " + accentColor + ";" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, " + accentColor + "15, 10, 0, 0, 4);"
            );
        });
        
        return card;
    }

    private Node buildTodayView() {
        if (allSessions == null || allSessions.isEmpty()) {
            return buildError("No Timetable Data", "No timetable data available.");
        }
        
        List<ClassSession> allFiltered = getFilteredSessions();
        if (allFiltered == null) allFiltered = new ArrayList<>();

        String todayDay = getTodayDayName();
        LocalTime now = LocalTime.now();
        
        List<ClassSession> todaySessions = new ArrayList<>();
        for (ClassSession s : allFiltered) {
            if (s != null && s.day != null && s.day.equalsIgnoreCase(todayDay)) {
                todaySessions.add(s);
            }
        }
        
        // Sort chronologically
        todaySessions.sort((s1, s2) -> {
            if (s1.startTime == null && s2.startTime == null) return 0;
            if (s1.startTime == null) return 1;
            if (s2.startTime == null) return -1;
            return s1.startTime.compareTo(s2.startTime);
        });
        
        VBox mainCol = new VBox(16);
        mainCol.setPadding(new Insets(12, 20, 16, 20));
        
        HBox dateHeader = new HBox(8);
        dateHeader.setAlignment(Pos.BASELINE_LEFT);
        Label dayLbl = new Label("Today's Schedule");
        dayLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US);
        Label dateLbl = new Label(LocalDate.now().format(dateFormat));
        dateLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500;");
        
        Label countLbl = new Label("•  " + todaySessions.size() + " " + (todaySessions.size() == 1 ? "Class" : "Classes") + " Today");
        countLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-accent; -fx-font-weight: bold;");
        
        dateHeader.getChildren().addAll(dayLbl, dateLbl, countLbl);
        mainCol.getChildren().add(dateHeader);
        
        if (todaySessions.isEmpty()) {
            VBox emptyCard = new VBox(16);
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(40, 20, 40, 20));
            emptyCard.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12;");
            
            Label icon = new Label("🎉");
            icon.setStyle("-fx-font-size: 32px;");
            
            Label noClassText = new Label("No classes scheduled today.");
            noClassText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
            
            Label noClassSub = new Label("You have no lectures or practical sessions today. Enjoy your break!");
            noClassSub.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
            
            emptyCard.getChildren().addAll(icon, noClassText, noClassSub);
            mainCol.getChildren().add(emptyCard);
        } else {
            ClassSession liveClass = null;
            ClassSession nextClass = null;
            for (ClassSession s : todaySessions) {
                if (s == null) continue;
                String status = getClassStatus(s, todayDay, now);
                if (status.equals("Live Now") && liveClass == null) {
                    liveClass = s;
                }
                if (status.equals("Upcoming") && nextClass == null) {
                    nextClass = s;
                }
            }
            
            if (liveClass != null || nextClass != null) {
                HBox focusRow = new HBox(16);
                focusRow.setFillHeight(true);
                
                if (liveClass != null) {
                    VBox liveCard = buildProminentFocusCard(liveClass, "LIVE NOW", "-color-accent", now);
                    HBox.setHgrow(liveCard, Priority.ALWAYS);
                    focusRow.getChildren().add(liveCard);
                }
                
                if (nextClass != null) {
                    VBox nextCard = buildProminentFocusCard(nextClass, "UPCOMING CLASS", "#f59e0b", now);
                    HBox.setHgrow(nextCard, Priority.ALWAYS);
                    focusRow.getChildren().add(nextCard);
                }
                
                mainCol.getChildren().add(focusRow);
            }
            
            Label listTitle = new Label("All Today's Classes");
            listTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted; -fx-padding: 8 0 0 0;");
            mainCol.getChildren().add(listTitle);
            
            VBox listFlow = new VBox(12);
            for (int i = 0; i < todaySessions.size(); i++) {
                ClassSession s = todaySessions.get(i);
                if (s == null) continue;
                try {
                    listFlow.getChildren().add(buildTodayClassCard(s, todayDay, now));
                    
                    // Gap/free-time calculations
                    if (i < todaySessions.size() - 1) {
                        ClassSession next = todaySessions.get(i + 1);
                        if (next != null && s.endTime != null && next.startTime != null && next.startTime.isAfter(s.endTime)) {
                            java.time.Duration gap = java.time.Duration.between(s.endTime, next.startTime);
                            long gapMins = gap.toMinutes();
                            if (gapMins >= 15) {
                                HBox gapBox = new HBox(8);
                                gapBox.setAlignment(Pos.CENTER);
                                gapBox.setPadding(new Insets(6, 12, 6, 12));
                                gapBox.setStyle("-fx-background-color: -color-bg-main; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-style: dashed; -fx-border-radius: 8; -fx-background-radius: 8;");
                                
                                Label gapLbl = new Label("☕  Free Time: " + gapMins + " mins");
                                gapLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
                                gapBox.getChildren().add(gapLbl);
                                
                                listFlow.getChildren().add(gapBox);
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("[Timetable] Error building class card for: " + s.courseName + " - " + ex.getMessage());
                }
            }
            mainCol.getChildren().add(listFlow);
        }
        
        ScrollPane sp = new ScrollPane(mainCol);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        return sp;
    }

    private VBox buildTodayClassCard(ClassSession s, String todayDay, LocalTime now) {
        String status = getClassStatus(s, todayDay, now);
        
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        
        String cardBg = "-color-bg-card";
        String cardBorder = "-color-border";
        double opacity = 1.0;
        
        if (status.equals("Live Now")) {
            cardBorder = "-color-accent";
        } else if (status.equals("Completed")) {
            opacity = 0.55;
        }
        
        card.setStyle(
            "-fx-background-color: " + cardBg + ";" +
            "-fx-border-color: " + cardBorder + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 6, 0, 0, 2);"
        );
        card.setOpacity(opacity);
        card.setCursor(javafx.scene.Cursor.HAND);
        
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        Label typeBadge = new Label(s.isLab ? "LAB SESSION" : "THEORY CLASS");
        typeBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: " + s.color + "; -fx-background-color: " + s.color + "15; -fx-padding: 3 8; -fx-background-radius: 6;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label statusBadge = new Label(status.toUpperCase());
        if (status.equals("Live Now")) {
            statusBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: white; -fx-background-color: -color-accent; -fx-padding: 3 8; -fx-background-radius: 6;");
        } else if (status.equals("Completed")) {
            statusBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted; -fx-background-color: -color-bg-main; -fx-padding: 3 8; -fx-background-radius: 6;");
        } else {
            statusBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: #475467; -fx-background-color: #f1f5f9; -fx-padding: 3 8; -fx-background-radius: 6;");
        }
        
        topRow.getChildren().addAll(typeBadge, spacer, statusBadge);
        
        Label courseLbl = new Label(s.courseName);
        courseLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        
        Label codeSection = new Label(s.courseCode + (s.section.isEmpty() ? "" : "  •  Section " + s.section));
        codeSection.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
        
        HBox detailsRow = new HBox(20);
        detailsRow.setAlignment(Pos.CENTER_LEFT);
        detailsRow.setPadding(new Insets(4, 0, 0, 0));
        
        Label timeLbl = new Label("🕒  " + s.timeRange);
        timeLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        
        Label roomLbl = new Label("📍  Room " + s.room);
        roomLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        
        Label facultyLbl = new Label("👤  " + s.teacher);
        facultyLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
        
        detailsRow.getChildren().addAll(timeLbl, roomLbl, facultyLbl);
        card.getChildren().addAll(topRow, courseLbl, codeSection, detailsRow);
        
        card.setOnMouseClicked(e -> showDetailsDrawer(s));
        
        final String exitBorder = cardBorder;
        if (!status.equals("Completed")) {
            card.setOnMouseEntered(e -> {
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
                tt.setToY(-2);
                tt.play();
                card.setStyle(
                    "-fx-background-color: " + cardBg + ";" +
                    "-fx-border-color: -color-accent;" +
                    "-fx-border-width: 1.5;" +
                    "-fx-border-radius: 12;" +
                    "-fx-background-radius: 12;" +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,70,67,0.08), 12, 0, 0, 4);"
                );
            });
            card.setOnMouseExited(e -> {
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
                tt.setToY(0);
                tt.play();
                card.setStyle(
                    "-fx-background-color: " + cardBg + ";" +
                    "-fx-border-color: " + exitBorder + ";" +
                    "-fx-border-width: 1.5;" +
                    "-fx-border-radius: 12;" +
                    "-fx-background-radius: 12;" +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 6, 0, 0, 2);"
                );
            });
        }
        
        return card;
    }

    private VBox buildNextClassWidget(ClassSession s, LocalTime now) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 6, 0, 0, 2);"
        );
        
        Label nextHeader = new Label("NEXT CLASS");
        nextHeader.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted; -fx-letter-spacing: 0.5px;");
        card.getChildren().add(nextHeader);
        
        if (s == null) {
            Label noClass = new Label("No upcoming classes today");
            noClass.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
            card.getChildren().add(noClass);
        } else {
            Label courseLbl = new Label(s.courseName != null ? s.courseName : "Unknown Course");
            courseLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
            
            if (s.startTime == null) {
                Label noTime = new Label("Time not available");
                noTime.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-style: italic;");
                card.getChildren().addAll(courseLbl, noTime);
                return card;
            }
            java.time.Duration duration = java.time.Duration.between(now, s.startTime);
            long diffMins = Math.max(0, duration.toMinutes());
            long hrs = diffMins / 60;
            long mins = diffMins % 60;
            String countdown = String.format("%02dh %02dm", hrs, mins);
            
            HBox countdownRow = new HBox(6);
            countdownRow.setAlignment(Pos.CENTER_LEFT);
            Label startLbl = new Label("Starts In:");
            startLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
            Label countdownVal = new Label(countdown);
            countdownVal.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #f59e0b;");
            countdownRow.getChildren().addAll(startLbl, countdownVal);
            
            VBox specs = new VBox(4);
            specs.setPadding(new Insets(4, 0, 0, 0));
            Label time = new Label("🕒  " + s.timeRange);
            time.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
            Label room = new Label("📍  Room " + s.room);
            room.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
            specs.getChildren().addAll(time, room);
            
            card.getChildren().addAll(courseLbl, countdownRow, specs);
            card.setOnMouseClicked(e -> showDetailsDrawer(s));
            card.setCursor(javafx.scene.Cursor.HAND);
        }
        return card;
    }

    private VBox buildTodaySummaryWidget(List<ClassSession> todaySessions) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(12));
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 4, 0, 0, 1);"
        );
        
        Label statsHeader = new Label("TODAY'S SUMMARY");
        statsHeader.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted; -fx-letter-spacing: 0.5px;");
        card.getChildren().add(statsHeader);
        
        int classesCount = todaySessions.size();
        int labSessions = 0;
        double totalHours = 0.0;
        
        for (ClassSession s : todaySessions) {
            if (s != null) {
                if (s.isLab) labSessions++;
                if (s.startTime != null && s.endTime != null) {
                    java.time.Duration d = java.time.Duration.between(s.startTime, s.endTime);
                    totalHours += d.toMinutes() / 60.0;
                }
            }
        }
        int theorySessions = classesCount - labSessions;
        
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        
        grid.add(buildMiniStat("Classes", String.valueOf(classesCount)), 0, 0);
        grid.add(buildMiniStat("Hours", String.format("%.1fh", totalHours)), 1, 0);
        grid.add(buildMiniStat("Theory", String.valueOf(theorySessions)), 0, 1);
        grid.add(buildMiniStat("Lab", String.valueOf(labSessions)), 1, 1);
        
        card.getChildren().add(grid);
        return card;
    }

    private VBox buildMiniStat(String label, String value) {
        VBox box = new VBox(2);
        box.setPadding(new Insets(10, 12, 10, 12));
        box.setStyle("-fx-background-color: -color-bg-main; -fx-background-radius: 8;");
        box.setAlignment(Pos.CENTER_LEFT);
        
        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        
        Label lbl = new Label(label.toUpperCase());
        lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        
        box.getChildren().addAll(valLbl, lbl);
        GridPane.setHgrow(box, Priority.ALWAYS);
        box.setMinWidth(110);
        return box;
    }

    // ==================== Week Grid View ====================

    private Node buildWeekView() {
        if (allSessions == null || allSessions.isEmpty()) {
            return buildError("No Timetable Data", "No timetable data available.");
        }
        if (rawHtml == null || rawHtml.isEmpty()) {
            return buildError("No Timetable Data", "Timetables are not cached or synced from the portal.");
        }
        
        Document doc = Jsoup.parse(rawHtml);
        Element table = null;
        Elements allTables = doc.select("table");
        for (Element t : allTables) {
            String txt = t.text().toLowerCase();
            boolean hasDays = txt.contains("monday") || txt.contains("tuesday")
                    || txt.contains("wednesday") || txt.contains("thursday")
                    || txt.contains("daytitle");
            boolean looksLikeStudentInfo = txt.contains("father name")
                    || txt.contains("roll no") || txt.contains("thesis title")
                    || txt.contains("current section") || txt.contains("registered courses");
            if (hasDays && !looksLikeStudentInfo) {
                table = t;
                break;
            }
        }
        
        if (table == null) {
            return buildError("Parsing Error", "Timetable layout could not be resolved from cached HTML.");
        }
        
        Elements rows = table.select("tr");
        if (rows == null || rows.isEmpty()) {
            return buildError("Empty View", "Timetable has no rows.");
        }
        
        // Headers row
        Element headerRow = rows.first();
        if (headerRow == null) {
            return buildError("Parsing Error", "No header row found in timetable.");
        }
        Elements headerCells = headerRow.select("th, td");
        List<String> timeSlots = new ArrayList<>();
        for (Element hc : headerCells) {
            if (hc != null) {
                timeSlots.add(hc.text().trim());
            }
        }
        
        if (timeSlots.isEmpty()) {
            return buildError("Empty View", "Timetable headers are empty.");
        }
        
        System.out.println("[Timetable] buildWeekView: rows size = " + rows.size() + ", timeSlots size = " + timeSlots.size());
        
        // Active columns tracker
        boolean[] colActive = new boolean[timeSlots.size()];
        if (colActive.length > 0) colActive[0] = true;
        
        Map<String, List<ClassSession>> sessionsByDay = new HashMap<>();
        for (ClassSession s : getFilteredSessions()) {
            if (s == null) continue;
            sessionsByDay.computeIfAbsent(s.day.toLowerCase(), k -> new ArrayList<>()).add(s);
            for (int c = s.startCol; c <= s.endCol; c++) {
                if (c >= 0 && c < colActive.length) {
                    colActive[c] = true;
                }
            }
        }
        
        VBox tableCard = new VBox(0);
        tableCard.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 10, 0, 0, 3);"
        );
        
        ScrollPane hScroll = new ScrollPane();
        hScroll.setFitToHeight(true);
        hScroll.setFitToWidth(true);
        hScroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        hScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox innerTable = new VBox(0);
        
        // 1. Header row
        HBox headerBox = new HBox(0);
        headerBox.setStyle("-fx-background-color: -color-bg-card; -fx-padding: 10 0; -fx-border-color: -color-border; -fx-border-width: 0 0 2 0;");
        
        for (int c = 0; c < timeSlots.size(); c++) {
            if (!colActive[c]) continue;
            
            double w = (c == 0) ? 80 : 100;
            String headerText = timeSlots.get(c);
            if (c > 0) {
                headerText = formatHeaderTime(headerText);
            }
            
            Label hl = new Label(headerText);
            hl.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-border-color: -color-border;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-font-size: 9px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: -color-text-muted;" +
                "-fx-padding: 4 8;" +
                "-fx-alignment: center;" +
                "-fx-text-alignment: center;"
            );
            hl.setAlignment(Pos.CENTER);
            hl.setWrapText(true);
            hl.setMinWidth(w - 8);
            hl.setPrefWidth(w - 8);
            hl.setMaxWidth(w - 8);
            
            StackPane pillWrapper = new StackPane(hl);
            pillWrapper.setPadding(new Insets(4));
            pillWrapper.setMinWidth(w);
            pillWrapper.setPrefWidth(w);
            pillWrapper.setMaxWidth(w);
            
            headerBox.getChildren().add(pillWrapper);
        }
        innerTable.getChildren().add(headerBox);
        
        String todayDay = getTodayDayName();
        
        // 2. Data rows
        for (int r = 1; r < rows.size(); r++) {
            Element row = rows.get(r);
            if (row == null) continue;
            Elements cells = row.select("td");
            if (cells == null || cells.isEmpty()) continue;
            
            String dayName = cells.first() != null ? cells.first().text().trim() : "";
            
            if (!showEmptyDays) {
                List<ClassSession> daySessions = sessionsByDay.get(dayName.toLowerCase());
                if (daySessions == null || daySessions.isEmpty()) {
                    continue;
                }
            }
            
            boolean isCurrentDay = dayName.equalsIgnoreCase(todayDay);
            
            HBox dataRow = new HBox(0);
            String bg = isCurrentDay ? "rgba(20, 184, 166, 0.06)" : ((r % 2 == 0) ? "-color-bg-card" : "-color-bg-main");
            String borderColor = isCurrentDay ? "-color-accent" : "-color-border";
            double leftBorderWidth = isCurrentDay ? 5.0 : 0.0;
            
            dataRow.setStyle(
                "-fx-background-color: " + bg + ";" +
                "-fx-padding: 6 0;" +
                "-fx-border-color: transparent transparent -color-border " + borderColor + ";" +
                "-fx-border-width: 0 0 1 " + leftBorderWidth + ";"
            );
            dataRow.setAlignment(Pos.CENTER_LEFT);
            dataRow.setMinHeight(70);
            
            int colIndex = 0;
            for (Element cell : cells) {
                if (cell == null) continue;
                int colspan = 1;
                String csAttr = cell.attr("colspan");
                if (!csAttr.isEmpty()) {
                    try { colspan = Integer.parseInt(csAttr); } catch (NumberFormatException ignored) {}
                }
                
                String cellText = cell.text().trim();
                
                boolean anyColActive = false;
                double cellWidth = 0.0;
                for (int s = 0; s < colspan; s++) {
                    int cIdx = colIndex + s;
                    if (cIdx >= 0 && cIdx < colActive.length && colActive[cIdx]) {
                        anyColActive = true;
                        cellWidth += (cIdx == 0) ? 80 : 100;
                    }
                }
                
                if (anyColActive) {
                    if (colIndex == 0) {
                        Label dayLabel = new Label(dayName);
                        if (isCurrentDay) {
                            dayLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: -color-accent; -fx-padding: 8 10;");
                        } else {
                            dayLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: -color-text-main; -fx-padding: 8 10;");
                        }
                        dayLabel.setMinWidth(cellWidth);
                        dayLabel.setPrefWidth(cellWidth);
                        dayLabel.setMaxWidth(cellWidth);
                        dataRow.getChildren().add(dayLabel);
                    } else if (cellText.isEmpty() || cellText.equals("—") || cellText.equals("-")) {
                        Label empty = new Label("");
                        empty.setMinWidth(cellWidth);
                        empty.setPrefWidth(cellWidth);
                        empty.setMaxWidth(cellWidth);
                        dataRow.getChildren().add(empty);
                    } else {
                        ClassSession matchSession = null;
                        List<ClassSession> daySessions = sessionsByDay.get(dayName.toLowerCase());
                        if (daySessions != null) {
                            for (ClassSession cs : daySessions) {
                                if (cs != null && cs.startCol == colIndex) {
                                    matchSession = cs;
                                    break;
                                }
                            }
                        }
                        
                        if (matchSession != null) {
                            dataRow.getChildren().add(buildWeekCourseCard(matchSession, cellWidth));
                        } else {
                            Label empty = new Label("");
                            empty.setMinWidth(cellWidth);
                            empty.setPrefWidth(cellWidth);
                            empty.setMaxWidth(cellWidth);
                            dataRow.getChildren().add(empty);
                        }
                    }
                }
                colIndex += colspan;
            }
            innerTable.getChildren().add(dataRow);
        }
        
        hScroll.setContent(innerTable);
        tableCard.getChildren().add(hScroll);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        
        VBox wrapper = new VBox(tableCard);
        wrapper.setPadding(new Insets(12, 20, 16, 20));
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        return wrapper;
    }

    private String formatHeaderTime(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        if (raw.toLowerCase().contains("daytitle") || raw.toLowerCase().contains("monday") || raw.equalsIgnoreCase("day") || raw.toLowerCase().contains("time")) {
            return raw;
        }
        String start = extractStartTime(raw);
        String end = extractEndTime(raw);
        if (start != null && end != null) {
            return start + "\n" + end;
        }
        return raw.replace(" to ", "\n").replace("-", "\n");
    }

    private VBox buildWeekCourseCard(ClassSession s, double width) {
        VBox card = new VBox(3);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(5, 6, 5, 8));
        
        card.setStyle(
            "-fx-background-color: " + s.color + "12;" +
            "-fx-border-color: " + s.color + "30;" +
            "-fx-border-width: 1 1 1 2.5;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;"
        );
        card.setMinWidth(width - 8);
        card.setPrefWidth(width - 8);
        card.setMaxWidth(width - 8);
        card.setCursor(javafx.scene.Cursor.HAND);
        
        Label nameLabel = new Label(s.courseName);
        nameLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: " + s.color + ";");
        nameLabel.setWrapText(true);
        card.getChildren().add(nameLabel);
        
        Label teacherLabel = new Label(s.teacher);
        teacherLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: -color-text-muted; -fx-opacity: 0.8;");
        teacherLabel.setWrapText(true);
        card.getChildren().add(teacherLabel);
        
        Label roomLabel = new Label("📍 " + s.room);
        roomLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: 600; -fx-text-fill: -color-text-muted; -fx-opacity: 0.8;");
        card.getChildren().add(roomLabel);
        
        Tooltip tooltip = new Tooltip(s.courseName + "\n" + s.courseCode + "  •  " + s.section + "\n" + s.timeRange + "\nRoom " + s.room + "  •  " + s.teacher);
        tooltip.setShowDelay(javafx.util.Duration.millis(100));
        Tooltip.install(card, tooltip);
        
        card.setOnMouseClicked(e -> showDetailsDrawer(s));
        
        card.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
            tt.setToY(-1);
            tt.play();
            card.setStyle(
                "-fx-background-color: " + s.color + "18;" +
                "-fx-border-color: " + s.color + ";" +
                "-fx-border-width: 1 1 1 2.5;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-effect: dropshadow(three-pass-box, " + s.color + "25, 8, 0, 0, 2);"
            );
        });
        card.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
            tt.setToY(0);
            tt.play();
            card.setStyle(
                "-fx-background-color: " + s.color + "12;" +
                "-fx-border-color: " + s.color + "30;" +
                "-fx-border-width: 1 1 1 2.5;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-effect: null;"
            );
        });
        
        StackPane wrapper = new StackPane(card);
        wrapper.setPadding(new Insets(2, 4, 2, 4));
        wrapper.setMinWidth(width);
        wrapper.setPrefWidth(width);
        wrapper.setMaxWidth(width);
        
        VBox outer = new VBox(wrapper);
        outer.setMinWidth(width);
        outer.setPrefWidth(width);
        outer.setMaxWidth(width);
        return outer;
    }

    // ==================== List View ====================

    private Node buildListView() {
        if (allSessions == null || allSessions.isEmpty()) {
            return buildError("No Timetable Data", "No timetable data available.");
        }
        
        List<ClassSession> listSessions = getFilteredSessions();
        if (listSessions == null) listSessions = new ArrayList<>();
        
        VBox mainList = new VBox(20);
        mainList.setPadding(new Insets(12, 20, 16, 20));
        
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        boolean hasAnyClasses = false;
        String todayDay = getTodayDayName();
        LocalTime now = LocalTime.now();
        
        for (String day : days) {
            List<ClassSession> daySessions = new ArrayList<>();
            for (ClassSession s : listSessions) {
                if (s != null && s.day != null && s.day.equalsIgnoreCase(day)) {
                    daySessions.add(s);
                }
            }
            
            if (daySessions.isEmpty() && !showEmptyDays) {
                continue;
            }
            
            VBox dayGroup = new VBox(8);
            dayGroup.setFillWidth(true);
            
            Label dayHeader = new Label("📅  " + day);
            dayHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: -color-text-main; -fx-padding: 4 0;");
            dayGroup.getChildren().add(dayHeader);
            
            if (daySessions.isEmpty()) {
                Label noClassLbl = new Label("No classes scheduled");
                noClassLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-style: italic; -fx-padding: 4 12;");
                dayGroup.getChildren().add(noClassLbl);
            } else {
                hasAnyClasses = true;
                daySessions.sort((s1, s2) -> {
                    if (s1.startTime == null && s2.startTime == null) return 0;
                    if (s1.startTime == null) return 1;
                    if (s2.startTime == null) return -1;
                    return s1.startTime.compareTo(s2.startTime);
                });
                
                for (ClassSession s : daySessions) {
                    if (s != null) {
                        dayGroup.getChildren().add(buildTodayClassCard(s, todayDay, now));
                    }
                }
            }
            mainList.getChildren().add(dayGroup);
        }
        
        if (!hasAnyClasses) {
            VBox emptyCard = new VBox(16);
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(40));
            emptyCard.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12;");
            Label icon = new Label("🔍");
            icon.setStyle("-fx-font-size: 32px;");
            Label noClassText = new Label("No matching classes found.");
            noClassText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
            emptyCard.getChildren().addAll(icon, noClassText);
            mainList.getChildren().add(emptyCard);
        }
        
        ScrollPane sp = new ScrollPane(mainList);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        return sp;
    }

    // ==================== Course Details Side Panel Drawer ====================

    private void showDetailsDrawer(ClassSession s) {
        VBox drawer = new VBox(12);
        drawer.setMinWidth(280);
        drawer.setPrefWidth(280);
        drawer.setMaxWidth(280);
        drawer.setPadding(new Insets(14));
        drawer.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 0 0 0 1;"
        );
        
        HBox drawerHeader = new HBox(12);
        drawerHeader.setAlignment(Pos.CENTER_LEFT);
        Label headerTitle = new Label("Class Details");
        headerTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-text-muted; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2;");
        closeBtn.setOnAction(e -> mainBorderPane.setRight(null));
        
        drawerHeader.getChildren().addAll(headerTitle, spacer, closeBtn);
        drawer.getChildren().add(drawerHeader);
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        VBox scrollContent = new VBox(12);
        scrollContent.setFillWidth(true);
        
        VBox specsCard = new VBox(6);
        specsCard.setPadding(new Insets(10, 12, 10, 12));
        specsCard.setStyle("-fx-background-color: -color-bg-main; -fx-background-radius: 8;");
        
        Label courseTitle = new Label(s.courseName);
        courseTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        courseTitle.setWrapText(true);
        
        Label codeSec = new Label(s.courseCode + (s.section.isEmpty() ? "" : "  •  Section " + s.section));
        codeSec.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");
        
        Separator sep = new Separator();
        sep.setStyle("-fx-padding: 4 0;");
        
        VBox infoRows = new VBox(4);
        infoRows.getChildren().add(buildSpecRow("📍 Room", s.room));
        infoRows.getChildren().add(buildSpecRow("👤 Faculty", s.teacher));
        infoRows.getChildren().add(buildSpecRow("📅 Schedule", s.day));
        infoRows.getChildren().add(buildSpecRow("🕒 Time Slot", s.timeRange));
        infoRows.getChildren().add(buildSpecRow("🏷 Session Type", s.isLab ? "Lab Practical" : "Theory Session"));
        
        specsCard.getChildren().addAll(courseTitle, codeSec, sep, infoRows);
        scrollContent.getChildren().add(specsCard);
        
        // Fetch lookup logs async
        new Thread(() -> {
            try {
                String attPct = getCourseAttendance(s.courseName, courseNamesMap);
                if (attPct.equals("N/A") && !s.courseCode.isEmpty()) {
                    attPct = getCourseAttendance(s.courseCode, courseNamesMap);
                }
                
                List<LectureEntry> recentLectures = getRecentLectures(s.courseCode, s.courseName);
                
                final String finalAtt = attPct;
                final List<LectureEntry> finalLectures = recentLectures;
                
                Platform.runLater(() -> {
                    // Attendance Widget
                    VBox attBox = new VBox(6);
                    attBox.setPadding(new Insets(10, 12, 10, 12));
                    attBox.setStyle("-fx-background-color: -color-bg-main; -fx-background-radius: 8;");
                    
                    Label attHeader = new Label("📊 ATTENDANCE RATE");
                    attHeader.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted;");
                    
                    HBox attRow = new HBox(12);
                    attRow.setAlignment(Pos.CENTER_LEFT);
                    
                    double rate = -1.0;
                    boolean hasRate = false;
                    if (!finalAtt.equals("N/A")) {
                        try {
                            rate = Double.parseDouble(finalAtt.replace("%", "").trim());
                            hasRate = true;
                        } catch (Exception ignored) {}
                    }
                    
                    ProgressIndicator indicator = new ProgressIndicator(hasRate ? rate / 100.0 : -1.0);
                    indicator.setMinSize(36, 36);
                    indicator.setMaxSize(36, 36);
                    if (hasRate) {
                        indicator.setStyle("-fx-progress-color: " + (rate < 75.0 ? "#ef4444;" : "-color-accent;"));
                    }
                    
                    Label rateLbl = new Label(finalAtt + " Presence Rate");
                    rateLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
                    
                    attRow.getChildren().addAll(indicator, rateLbl);
                    attBox.getChildren().addAll(attHeader, attRow);
                    scrollContent.getChildren().add(attBox);
                    
                    // Proceedings Widget
                    VBox timelineBox = new VBox(8);
                    Label timelineHeader = new Label("📝 RECENT CLASS PROCEEDINGS");
                    timelineHeader.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted; -fx-padding: 4 0 2 0;");
                    timelineBox.getChildren().add(timelineHeader);
                    
                    if (finalLectures.isEmpty()) {
                        Label noLectures = new Label("No recent lecture logs cached.");
                        noLectures.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-style: italic;");
                        timelineBox.getChildren().add(noLectures);
                    } else {
                        List<LectureEntry> reversed = new ArrayList<>(finalLectures);
                        Collections.reverse(reversed);
                        
                        VBox timeline = new VBox(8);
                        timeline.setPadding(new Insets(2, 0, 0, 4));
                        
                        for (LectureEntry le : reversed) {
                            HBox item = new HBox(6);
                            item.setAlignment(Pos.TOP_LEFT);
                            
                            VBox dotContainer = new VBox(4);
                            dotContainer.setAlignment(Pos.TOP_CENTER);
                            Circle dot = new Circle(2.5);
                            boolean isPresent = le.status.equalsIgnoreCase("present");
                            dot.setStyle("-fx-fill: " + (isPresent ? "-color-accent;" : "#ef4444;"));
                            dotContainer.getChildren().add(dot);
                            
                            VBox entry = new VBox(1);
                            Label date = new Label(le.date + "  •  " + le.status.toUpperCase());
                            date.setStyle("-fx-font-size: 8.5px; -fx-font-weight: bold; -fx-text-fill: " + (isPresent ? "-color-accent;" : "#ef4444;"));
                            
                            Label topic = new Label(le.topic);
                            topic.setStyle("-fx-font-size: 9.5px; -fx-text-fill: -color-text-main;");
                            topic.setWrapText(true);
                            
                            entry.getChildren().addAll(date, topic);
                            item.getChildren().addAll(dotContainer, entry);
                            timeline.getChildren().add(item);
                        }
                        timelineBox.getChildren().add(timeline);
                    }
                    scrollContent.getChildren().add(timelineBox);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
        
        scrollPane.setContent(scrollContent);
        drawer.getChildren().add(scrollPane);
        mainBorderPane.setRight(drawer);
    }

    private HBox buildSpecRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");
        lbl.setPrefWidth(90);
        lbl.setMinWidth(90);
        
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-main; -fx-font-weight: 600;");
        val.setWrapText(true);
        HBox.setHgrow(val, Priority.ALWAYS);
        
        row.getChildren().addAll(lbl, val);
        return row;
    }

    // ==================== Parser Helpers ====================

    private List<ClassSession> parseSessions(Document doc) {
        List<ClassSession> sessions = new ArrayList<>();
        
        Element table = null;
        Elements allTables = doc.select("table");
        for (Element t : allTables) {
            String txt = t.text().toLowerCase();
            boolean hasDays = txt.contains("monday") || txt.contains("tuesday")
                    || txt.contains("wednesday") || txt.contains("thursday")
                    || txt.contains("daytitle");
            boolean looksLikeStudentInfo = txt.contains("father name")
                    || txt.contains("roll no") || txt.contains("thesis title")
                    || txt.contains("current section") || txt.contains("registered courses");
            if (hasDays && !looksLikeStudentInfo) {
                table = t;
                break;
            }
        }
        
        if (table == null) return sessions;
        
        Elements rows = table.select("tr");
        if (rows.isEmpty()) return sessions;
        
        // Parse time slot headers
        Element headerRow = rows.first();
        if (headerRow == null) return sessions;
        Elements headerCells = headerRow.select("th, td");
        List<String> timeSlots = new ArrayList<>();
        for (Element hc : headerCells) {
            timeSlots.add(hc.text().trim());
        }
        System.out.println("[Timetable] Parsed " + timeSlots.size() + " time slot headers: " + timeSlots);
        
        for (int r = 1; r < rows.size(); r++) {
            Element row = rows.get(r);
            if (row == null) continue;
            Elements cells = row.select("td");
            if (cells == null || cells.isEmpty()) continue;
            
            String dayName = cells.first() != null ? cells.first().text().trim() : "";
            int colIndex = 0;
            
            for (Element cell : cells) {
                int colspan = 1;
                String csAttr = cell.attr("colspan");
                if (!csAttr.isEmpty()) {
                    try { colspan = Integer.parseInt(csAttr); } catch (NumberFormatException ignored) {}
                }
                
                String cellText = cell.text().trim();
                
                // Skip day-name column (0), empty cells, dash placeholders, and lunch-break columns ("----")
                if (colIndex > 0 && !cellText.isEmpty() && !cellText.equals("—") && !cellText.equals("-") && !cellText.matches("^-+$")) {
                    ClassSession session = new ClassSession();
                    session.day = dayName;
                    session.startCol = colIndex;
                    session.endCol = colIndex + colspan - 1;
                    
                    int endSlotIdx = colIndex + colspan - 1;
                    if (colIndex >= 0 && colIndex < timeSlots.size() && endSlotIdx >= 0 && endSlotIdx < timeSlots.size()) {
                        String startSlot = timeSlots.get(colIndex);
                        String endSlot = timeSlots.get(endSlotIdx);
                        
                        // Extract start time from the start slot and end time from the end slot.
                        // Time slots may use "to", "-", or other separators: e.g. "09:00 to 10:30", "09:00-10:30"
                        String start = extractStartTime(startSlot);
                        String end = extractEndTime(endSlot);
                        
                        if (start != null && end != null) {
                            session.timeRange = start + " - " + end;
                            session.startTime = parseTime(start);
                            session.endTime = parseTime(end);
                        } else {
                            // Fallback: use what we can
                            session.timeRange = startSlot + " - " + endSlot;
                            session.startTime = parseTime(startSlot);
                            session.endTime = parseTime(endSlot);
                        }
                    } else {
                        System.err.println("[Timetable] Column index " + colIndex + " or endSlotIdx " + endSlotIdx + " out of bounds for timeSlots (size=" + timeSlots.size() + ")");
                        session.timeRange = "Unknown";
                    }
                    
                    String htmlContent = cell.html();
                    String[] htmlParts = htmlContent.split("(?i)<br\\s*/?>");
                    
                    String rawName = "";
                    if (htmlParts.length > 0) {
                        rawName = Jsoup.parse(htmlParts[0]).text().trim();
                    }
                    
                    // Room - extract the meaningful room identifier
                    Element roomEl = cell.select("[id=lblRoom], [ID=lblRoom]").first();
                    String roomRaw = (roomEl != null) ? roomEl.text().trim() : "";
                    
                    // Strip capacity markers like [50M], [50], etc.
                    String roomCleaned = roomRaw.replaceAll("\\[\\d+M?\\]", "").trim();
                    
                    // Determine if this is a named lab room (e.g. "Computer LAB 03 (S-312)")
                    // vs a regular room (e.g. "S207 (56M)")
                    String roomLower = roomCleaned.toLowerCase();
                    boolean isLabRoom = roomLower.contains("computer lab") || roomLower.contains("lab ")
                            || roomLower.startsWith("lab") || roomLower.endsWith("lab")
                            || roomLower.contains("laboratory");
                    
                    if (isLabRoom && roomCleaned.contains("(") && roomCleaned.contains(")")) {
                        // For lab rooms like "Computer LAB 03 (S-312)", the actual room code is inside parentheses
                        int open = roomCleaned.indexOf("(");
                        int close = roomCleaned.indexOf(")");
                        if (open < close) {
                            session.room = roomCleaned.substring(open + 1, close).trim();
                        } else {
                            session.room = roomCleaned.substring(0, open).trim();
                        }
                    } else if (roomCleaned.contains("(")) {
                        // For regular rooms like "S207 (56M)", keep the part before parentheses
                        session.room = roomCleaned.substring(0, roomCleaned.indexOf("(")).trim();
                    } else {
                        session.room = roomCleaned;
                    }
                    if (session.room.isEmpty()) {
                        session.room = "TBA";
                    }
                    
                    // Teacher
                    Element teacherEl = cell.select("font[color=blue], font[color=Blue], [id=lblTeacherName], [ID=lblTeacherName]").first();
                    if (teacherEl != null) {
                        session.teacher = teacherEl.text().trim();
                    } else {
                        Element link = cell.select("a").first();
                        session.teacher = (link != null) ? link.text().trim() : "";
                    }
                    if (session.teacher.isEmpty()) {
                        session.teacher = "Staff";
                    }
                    
                    session.courseName = rawName;
                    session.courseCode = "";
                    session.section = "";
                    
                    String nameLower = rawName.toLowerCase();
                    session.isLab = nameLower.contains("lab") || nameLower.contains("laboratory") || nameLower.contains("practical")
                            || isLabRoom;
                    
                    int codeSectionIndex = -1;
                    for (int i = rawName.length() - 1; i >= 0; i--) {
                        char ch = rawName.charAt(i);
                        if (Character.isDigit(ch)) {
                            if (i > 0 && rawName.charAt(i - 1) == ' ') {
                                codeSectionIndex = i;
                                break;
                            }
                        }
                    }
                    if (codeSectionIndex > 0) {
                        String codeSec = rawName.substring(codeSectionIndex).trim();
                        session.courseName = rawName.substring(0, codeSectionIndex).trim();
                        
                        if (codeSec.contains("(") && codeSec.contains(")")) {
                            int open = codeSec.indexOf("(");
                            int close = codeSec.indexOf(")");
                            if (open < close) {
                                session.courseCode = codeSec.substring(0, open).trim();
                                session.section = codeSec.substring(open + 1, close).trim();
                            }
                        } else {
                            session.courseCode = codeSec;
                        }
                    }
                    
                    session.color = getCourseColor(session.courseName);
                    sessions.add(session);
                }
                colIndex += colspan;
            }
        }
        return sessions;
    }

    /**
     * Extracts the START time from a time slot string.
     * Handles formats like: "09:00 to 10:30", "09:00-10:30", "9:00 AM", "09:00"
     */
    private String extractStartTime(String slot) {
        if (slot == null || slot.trim().isEmpty()) return null;
        String s = slot.trim();
        // Try splitting by " to " first (portal format: "09:00 to 10:30")
        if (s.toLowerCase().contains(" to ")) {
            String[] parts = s.split("(?i)\\s+to\\s+");
            return parts.length > 0 ? parts[0].trim() : null;
        }
        // Try splitting by "-" ("09:00-10:30")
        if (s.contains("-") && s.indexOf(":") >= 0) {
            String[] parts = s.split("-");
            return parts.length > 0 ? parts[0].trim() : null;
        }
        // Single time value or unparseable — return as-is if it looks like a time
        if (s.contains(":")) return s;
        return null;
    }

    /**
     * Extracts the END time from a time slot string.
     * Handles formats like: "09:00 to 10:30", "09:00-10:30", "10:30 AM", "10:30"
     */
    private String extractEndTime(String slot) {
        if (slot == null || slot.trim().isEmpty()) return null;
        String s = slot.trim();
        // Try splitting by " to " first (portal format: "09:00 to 10:30")
        if (s.toLowerCase().contains(" to ")) {
            String[] parts = s.split("(?i)\\s+to\\s+");
            return parts.length > 1 ? parts[1].trim() : (parts.length > 0 ? parts[0].trim() : null);
        }
        // Try splitting by "-" ("09:00-10:30")
        if (s.contains("-") && s.indexOf(":") >= 0) {
            String[] parts = s.split("-");
            return parts.length > 1 ? parts[1].trim() : (parts.length > 0 ? parts[0].trim() : null);
        }
        // Single time value or unparseable — return as-is if it looks like a time
        if (s.contains(":")) return s;
        return null;
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;
        String cleaned = timeStr.trim().toUpperCase().replaceAll("\\s+", "");
        
        // Skip obvious non-time strings
        if (cleaned.matches("^-+$") || cleaned.equalsIgnoreCase("DAYTITLE") || cleaned.equalsIgnoreCase("UNKNOWN")) {
            return null;
        }
        
        if (cleaned.endsWith("AM") && !cleaned.contains(" AM")) {
            cleaned = cleaned.replace("AM", " AM");
        }
        if (cleaned.endsWith("PM") && !cleaned.contains(" PM")) {
            cleaned = cleaned.replace("PM", " PM");
        }
        
        if (cleaned.indexOf(":") == 1) {
            cleaned = "0" + cleaned;
        }
        
        // Try 12-hour format with AM/PM first
        try {
            return LocalTime.parse(cleaned, TIME_FORMATTER);
        } catch (Exception ignored) {}
        
        // Try 24-hour format (e.g. "09:00", "13:30")
        try {
            return LocalTime.parse(timeStr.trim(), DateTimeFormatter.ofPattern("H:mm"));
        } catch (Exception ignored) {}
        
        try {
            return LocalTime.parse(timeStr.trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception ignored) {}
        
        return null;
    }

    private String getCourseColor(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("data structure")) return "#22c55e"; // Green
        if (lower.contains("database")) return "#3b82f6"; // Blue
        if (lower.contains("calculus") || lower.contains("analytic")) return "#a855f7"; // Purple
        if (lower.contains("software")) return "#f97316"; // Orange
        if (lower.contains("digital logic") || lower.contains("dld")) return "#ef4444"; // Red
        
        String[] fallbacks = {"#06b6d4", "#ec4899", "#f59e0b", "#6366f1", "#14b8a6"};
        int idx = Math.abs(name.hashCode()) % fallbacks.length;
        return fallbacks[idx];
    }

    private String getTodayDayName() {
        return LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US);
    }

    private String getClassStatus(ClassSession s, String todayDay, LocalTime now) {
        if (!s.day.equalsIgnoreCase(todayDay)) return "Upcoming";
        if (s.startTime == null || s.endTime == null) return "Upcoming";
        if (now.isBefore(s.startTime)) return "Upcoming";
        if (now.isAfter(s.endTime)) return "Completed";
        return "Live Now";
    }

    // ==================== Async Details Lookups ====================

    private String getCourseAttendance(String courseName, Map<String, String> courseNames) {
        try {
            String summaryHtml = context.dataCacheService().getCachedHtml("Summary.aspx").orElse(null);
            if (summaryHtml != null) {
                List<com.assignly.view.CoursesTabView.CourseSummary> courses = com.assignly.view.CoursesTabView.parseSummaryCourses(summaryHtml, courseNames);
                for (com.assignly.view.CoursesTabView.CourseSummary c : courses) {
                    if (c.title.equalsIgnoreCase(courseName) || c.code.equalsIgnoreCase(courseName)) {
                        return c.percentage;
                    }
                }
            }
        } catch (Exception ignored) {}
        return "N/A";
    }

    private List<LectureEntry> getRecentLectures(String courseCode, String courseName) {
        String html = null;
        if (courseCode != null && !courseCode.isEmpty()) {
            html = context.dataCacheService().getCachedHtml("classproceedings.aspx_" + courseCode).orElse(null);
        }
        if (html == null && courseName != null && !courseName.isEmpty()) {
            html = context.dataCacheService().getCachedHtml("classproceedings.aspx_" + courseName).orElse(null);
        }
        if (html != null) {
            List<LectureEntry> allLectures = parseProceedings(html);
            if (allLectures.size() > 4) {
                return allLectures.subList(allLectures.size() - 4, allLectures.size());
            }
            return allLectures;
        }
        return new ArrayList<>();
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

                int dateIdx = -1, topicIdx = -1, statusIdx = -1;
                for (int i = 0; i < headers.size(); i++) {
                    String h = headers.get(i);
                    if (h.contains("date")) dateIdx = i;
                    else if (h.contains("topic") || h.contains("particular") || h.contains("description")) topicIdx = i;
                    else if (h.contains("status")) statusIdx = i;
                }

                for (int r = 1; r < rows.size(); r++) {
                    Elements cells = rows.get(r).select("td");
                    if (cells.size() <= dateIdx) continue;

                    LectureEntry le = new LectureEntry();
                    le.date = dateIdx >= 0 && dateIdx < cells.size() ? cells.get(dateIdx).text().trim() : "";
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

    private List<ClassSession> getFilteredSessions() {
        List<ClassSession> filtered = new ArrayList<>();
        if (allSessions == null) return filtered;
        for (ClassSession s : allSessions) {
            if (s == null) continue;
            if (searchQuery != null && !searchQuery.isEmpty()) {
                String query = searchQuery.toLowerCase();
                boolean matchCourse = s.courseName != null && s.courseName.toLowerCase().contains(query);
                boolean matchCode = s.courseCode != null && s.courseCode.toLowerCase().contains(query);
                boolean matchTeacher = s.teacher != null && s.teacher.toLowerCase().contains(query);
                boolean matchRoom = s.room != null && s.room.toLowerCase().contains(query);
                if (!matchCourse && !matchCode && !matchTeacher && !matchRoom) {
                    continue;
                }
            }
            
            if ("Theory Sessions".equals(selectedTypeFilter) && s.isLab) {
                continue;
            }
            if ("Lab Sessions".equals(selectedTypeFilter) && !s.isLab) {
                continue;
            }
            
            filtered.add(s);
        }
        return filtered;
    }

    // ==================== Error & Banner Widgets ====================

    private VBox buildError(String title, String message) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        VBox.setVgrow(box, Priority.ALWAYS);

        Label icon = new Label("⚠");
        icon.setStyle("-fx-font-size:28px;-fx-text-fill:#ef4444;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-muted;-fx-text-alignment:center;");
        msgLabel.setWrapText(true);

        box.getChildren().addAll(icon, titleLabel, msgLabel);
        return box;
    }

    private HBox buildOfflineBanner() {
        HBox banner = new HBox(8);
        banner.setAlignment(Pos.CENTER);
        banner.setPadding(new Insets(8, 16, 8, 16));
        banner.setStyle("-fx-background-color:#FEF2F2;-fx-border-color:#FCA5A5;-fx-border-width:0 0 1 0;");
        
        Label icon = new Label("⚠");
        icon.setStyle("-fx-text-fill:#DC2626;-fx-font-size:12px;");
        Label text = new Label("Offline Mode: Displaying previously loaded timetable data.");
        text.setStyle("-fx-text-fill:#991B1B;-fx-font-size:11px;-fx-font-weight:bold;");
        
        banner.getChildren().addAll(icon, text);
        return banner;
    }
}
