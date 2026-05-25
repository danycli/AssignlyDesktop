package com.assignly.view;

import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Node;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.*;

public class CoursesTabView {
    private final VBox root = new VBox();
    private final AppContext context;
    private StackPane contentPane;
    private HBox tabBar;
    private String activeTab = "";
    // Cache page HTML so we can extract dropdown info
    private String proceedingsPageHtml;
    private String marksPageHtml = "";
    private boolean isSummary = false;

    private static final Set<String> SKIP = Set.of(
        "name","father name","roll no","program","current section",
        "total registered courses","registered courses","current advisor",
        "date of birth","cnic","thesis title","missing documents / disciplinary case","missing documents"
    );
    private static final String PROC_PAGE = "classproceedings.aspx";
    private static final String MARKS_PAGE = "QAMarks.aspx";
    private static final String[] MARKS_FALLBACKS = {"QASessMarks.aspx", "QASessionMarks.aspx", "Marks.aspx"};

    public CoursesTabView(AppContext context) {
        this.context = context;
        buildShell();
        loadTab("summary");
    }

    private void buildShell() {
        root.setFillWidth(true);
        
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(24, 28, 0, 28));

        Label heading = new Label("Courses");
        heading.getStyleClass().add("heading-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄");
        refreshBtn.setStyle("-fx-background-color:transparent;-fx-font-size:18px;-fx-cursor:hand;");
        refreshBtn.setOnAction(e -> loadTab(activeTab, null, true));

        headerRow.getChildren().addAll(heading, spacer, refreshBtn);

        tabBar = new HBox(4);
        tabBar.setPadding(new Insets(12, 28, 0, 28));
        tabBar.getChildren().addAll(
            tabBtn("Summary","summary"), tabBtn("Class Proceedings","proceedings"), tabBtn("Q.A/Sess/Final Marks","marks"));
        
        contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        root.getChildren().addAll(headerRow, tabBar, contentPane);
    }

    private Button tabBtn(String label, String id) {
        Button b = new Button(label);
        b.setUserData(id);
        b.setCursor(javafx.scene.Cursor.HAND);
        b.setStyle(tabStyle(false));
        b.setOnAction(e -> loadTab(id));
        return b;
    }

    private String tabStyle(boolean on) {
        return on ? "-fx-background-color:#004643;-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:600;-fx-background-radius:6;-fx-padding:6 14;"
                  : "-fx-background-color: -color-bg-card;-fx-text-fill:#666;-fx-font-size:12px;-fx-font-weight:500;-fx-background-radius:6;-fx-padding:6 14;-fx-border-color:#d5d0ce;-fx-border-radius:6;-fx-border-width:1;";
    }

    private void loadTab(String id) {
        loadTab(id, null, false);
    }

    private String lastSelectedCourseTitle = null;
    private boolean hasExplicitlySelectedCourse = false;
    private int loadRequestId = 0;

    private void loadTab(String tabKey, String extraParam, boolean forceRefresh) {
        if (!forceRefresh && activeTab.equals(tabKey) && !tabKey.endsWith("_postback")) return;
        activeTab = tabKey;
        isSummary = tabKey.equals("summary");

        for (var n : tabBar.getChildren()) {
            if (n instanceof Button b) {
                b.setStyle(tabStyle(tabKey.replace("_postback", "").equals(b.getUserData())));
            }
        }

        final int currentRequestId = ++loadRequestId;
        showLoading("Loading...");
        new Thread(() -> {
            switch (tabKey) {
                case "summary" -> loadSummary(forceRefresh, currentRequestId);
                case "proceedings" -> {
                    if (lastSelectedCourseTitle != null && extraParam == null) {
                        loadProceedingsByText(lastSelectedCourseTitle, forceRefresh, currentRequestId);
                    } else {
                        loadProceedings(extraParam, forceRefresh, currentRequestId);
                    }
                }
                case "proceedings_postback" -> loadProceedingsFromSummary(extraParam, currentRequestId);
                case "marks" -> {
                    if (lastSelectedCourseTitle != null && extraParam == null) {
                        loadMarksByText(lastSelectedCourseTitle, forceRefresh, currentRequestId);
                    } else {
                        loadMarksByText(extraParam, forceRefresh, currentRequestId);
                    }
                }
                case "marks_postback" -> loadMarksFromSummary(extraParam, currentRequestId);
            }
        }).start();
    }

    private void loadMarksFromSummary(String eventTarget, int currentRequestId) {
        new Thread(() -> {
            context.portalRepository().postbackEvent("Summary.aspx", eventTarget);
            if (currentRequestId == loadRequestId) {
                loadMarks(null, true, currentRequestId);
            }
        }).start();
    }

    private void loadProceedingsFromSummary(String eventTarget, int currentRequestId) {
        new Thread(() -> {
            context.portalRepository().postbackEvent("Summary.aspx", eventTarget);
            if (currentRequestId == loadRequestId) {
                loadProceedings(null, true, currentRequestId);
            }
        }).start();
    }

    private void setContentAnimated(javafx.scene.Node node) {
        contentPane.getChildren().setAll(node);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(200), node);
        tt.setFromY(10);
        tt.setToY(0);
        
        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(ft, tt);
        pt.play();
    }

    private void showLoading(String msg) {
        Platform.runLater(() -> {
            contentPane.getChildren().clear();
            VBox box = new VBox(10); box.setAlignment(Pos.CENTER);
            ProgressIndicator sp = new ProgressIndicator(); sp.setMaxSize(28,28);
            Label l = new Label(msg); l.setStyle("-fx-text-fill:#888;-fx-font-size:12px;");
            box.getChildren().addAll(sp, l);
            setContentAnimated(new StackPane(box));
        });
    }

    // ==================== SUMMARY ====================
    private void loadSummary(boolean forceRefresh, int currentRequestId) {
        try {
            String html = null;
            boolean isOffline = false;

            if (!forceRefresh) {
                html = context.dataCacheService().getCachedHtml("Summary.aspx").orElse(null);
            }

            if (html == null) {
                html = context.fetchAndCacheHtml("Summary.aspx");
                if (html == null) {
                    html = context.dataCacheService().getCachedHtml("Summary.aspx").orElse(null);
                    isOffline = true;
                }
            }

            final String finalHtml = html;
            final boolean finalOffline = isOffline;
            
            Platform.runLater(() -> {
                if (currentRequestId != loadRequestId) return;
                contentPane.getChildren().clear();
                if (finalHtml == null) { showError("Could not load Summary and no offline data available."); return; }
                setContentAnimated(buildSummaryView(finalHtml, finalOffline));
            });
        } catch (Exception e) {
            Platform.runLater(() -> { if (currentRequestId == loadRequestId) showError("Error: " + e.getMessage()); });
        }
    }

    private ScrollPane buildSummaryView(String html, boolean isOffline) {
        VBox content = new VBox(16); content.setPadding(new Insets(16,28,24,28)); content.setFillWidth(true);
        if (isOffline) {
            content.getChildren().add(buildOfflineBanner());
        }
        Document doc = Jsoup.parse(html);
        for (Element table : doc.select("table")) {
            String tt = table.text().toLowerCase();
            if (tt.contains("father name") && tt.contains("roll no")) continue;
            if (tt.contains("cnic") && tt.contains("date of birth")) continue;
            Elements rows = table.select("tr");
            if (rows.size() < 2) continue;
            VBox card = buildNativeTable(rows);
            if (card != null) content.getChildren().add(card);
        }
        if (content.getChildren().isEmpty()) showError("No course data found.");
        ScrollPane sp = new ScrollPane(content); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    // ==================== CLASS PROCEEDINGS ====================
    private void loadProceedingsByText(String text, boolean forceRefresh, int currentRequestId) {
        if (text == null || text.isBlank()) {
            loadProceedings(null, forceRefresh, currentRequestId);
            return;
        }
        
        String html = null;
        if (!forceRefresh) html = context.dataCacheService().getCachedHtml(PROC_PAGE).orElse(null);
        if (html == null) {
            html = context.fetchAndCacheHtml(PROC_PAGE);
            if (html == null) {
                html = context.dataCacheService().getCachedHtml(PROC_PAGE).orElse(null);
            }
        }

        if (html != null) {
            List<String[]> courses = context.portalRepository().parseDropdownOptions(html, "course");
            if (courses.isEmpty()) courses.addAll(context.portalRepository().parseDropdownOptions(html, "ddl"));
            String lowerText = text.trim().toLowerCase();
            for (String[] c : courses) {
                if (c[1].toLowerCase().contains(lowerText) || lowerText.contains(c[1].toLowerCase())) {
                    loadProceedings(c[0], forceRefresh, currentRequestId);
                    return;
                }
            }
            
            // If we are here, the cached HTML does not have a dropdown with the course we want.
            // This usually happens if the cache is stale (e.g. an empty 'Please select a course' page).
            // We must force a network fetch to see if the server session is already correct.
            if (!forceRefresh) {
                loadProceedingsByText(text, true, currentRequestId);
                return;
            }
        }
        loadProceedings(null, forceRefresh, currentRequestId);
    }

    private void loadProceedings(String courseValue, boolean forceRefresh, int currentRequestId) {
        // First fetch the page to get the dropdown
        String html = null;
        boolean isOffline = false;

        if (!forceRefresh) html = context.dataCacheService().getCachedHtml(PROC_PAGE).orElse(null);
        if (html == null) {
            html = context.fetchAndCacheHtml(PROC_PAGE);
            if (html == null) {
                html = context.dataCacheService().getCachedHtml(PROC_PAGE).orElse(null);
                isOffline = true;
            }
        }
        if (html == null) { Platform.runLater(() -> showError("Could not load Class Proceedings page and no offline data available.")); return; }
        proceedingsPageHtml = html;

        // If a course is selected, do postback (network required)
        if (courseValue != null && !isOffline) {
            String ddName = context.portalRepository().findDropdownName(html, "course");
            if (ddName == null) ddName = context.portalRepository().findDropdownName(html, "ddl");
            if (ddName != null) {
                String result = context.portalRepository().postbackWithDropdown(PROC_PAGE, ddName, courseValue);
                if (result != null) html = result;
            }
        }
        final String finalHtml = html;
        final boolean finalOffline = isOffline;
        final List<String[]> courses = context.portalRepository().parseDropdownOptions(proceedingsPageHtml, "course");
        if (courses.isEmpty()) {
            courses.addAll(context.portalRepository().parseDropdownOptions(proceedingsPageHtml, "ddl"));
        }

        Platform.runLater(() -> {
            if (currentRequestId != loadRequestId) return;
            setContentAnimated(buildProceedingsView(finalHtml, courses, courseValue, finalOffline));
        });
    }

    private ScrollPane buildProceedingsView(String html, List<String[]> courses, String selected, boolean isOffline) {
        VBox content = new VBox(14); content.setPadding(new Insets(16,28,24,28)); content.setFillWidth(true);
        if (isOffline) {
            content.getChildren().add(buildOfflineBanner());
        }

        // Course selector
        if (!courses.isEmpty()) {
            HBox selectorRow = new HBox(10); selectorRow.setAlignment(Pos.CENTER_LEFT);
            Label lbl = new Label("Select Course:"); lbl.setStyle("-fx-font-weight:600;-fx-text-fill:#004643;-fx-font-size:13px;");
            ComboBox<String> combo = new ComboBox<>();
            Map<String, String> valueMap = new LinkedHashMap<>();
            for (String[] c : courses) { combo.getItems().add(c[1]); valueMap.put(c[1], c[0]); }
            if (selected != null) {
                for (String[] c : courses) { if (c[0].equals(selected)) { combo.setValue(c[1]); break; } }
            }
            combo.setPromptText("Choose a course...");
            combo.setStyle("-fx-font-size:12px;");
            combo.setOnAction(e -> {
                String val = valueMap.get(combo.getValue());
                if (val != null) { 
                    lastSelectedCourseTitle = combo.getValue();
                    hasExplicitlySelectedCourse = true;
                    showLoading("Loading proceedings..."); 
                    final int reqId = ++loadRequestId;
                    new Thread(() -> loadProceedings(val, false, reqId)).start(); 
                }
            });
            selectorRow.getChildren().addAll(lbl, combo);
            content.getChildren().add(selectorRow);
        }

        Document doc = Jsoup.parse(html);

        // Check for server messages
        Element msg = doc.selectFirst("#DataContent_lblMessage");
        if (msg == null) msg = doc.selectFirst(".notification.information");
        if (msg != null && !msg.text().isBlank()) {
            Label msgLbl = new Label(msg.text().replace("Select Course", ""));
            msgLbl.setStyle("-fx-font-size:14px; -fx-text-fill:#e53e3e; -fx-font-weight:bold;");
            content.getChildren().add(msgLbl);
        } else if (!hasExplicitlySelectedCourse) {
            Label msgLbl = new Label("Please select a course from the dropdown above or from the Summary tab.");
            msgLbl.setStyle("-fx-font-size:14px; -fx-text-fill:#e53e3e; -fx-font-weight:bold;");
            content.getChildren().add(msgLbl);
        }

        if (hasExplicitlySelectedCourse) {
            // Parse course info header (Course, Faculty, Total Classes, Presents, Absents, Percentage)
            VBox infoCard = parseCourseHeader(doc);
            if (infoCard != null) content.getChildren().add(infoCard);

            // Parse lecture table
            for (Element table : doc.select("table")) {
                String tt = table.text().toLowerCase();
                if (tt.contains("father name") || tt.contains("cnic")) continue;
                Elements rows = table.select("tr");
                if (rows.size() < 2) continue;
                Element hdr = rows.first();
                String hdrText = hdr.text().toLowerCase();
                if (hdrText.contains("lecture") || hdrText.contains("date") || hdrText.contains("topic") || hdrText.contains("status")) {
                    VBox card = buildNativeTable(rows);
                    if (card != null) content.getChildren().add(card);
                }
            }
        }

        ScrollPane sp = new ScrollPane(content); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    private VBox parseCourseHeader(Document doc) {
        Map<String, String> info = new LinkedHashMap<>();
        String[] keys = {"Course", "Faculty Member", "Faculty", "Total Classes", "Presents", "Absents", "Percentage"};
        for (String key : keys) {
            if (info.containsKey(key.replace(" Member", ""))) continue; // Avoid Faculty duplicate
            for (Element el : doc.select("span, td, th, label")) {
                String text = el.text().trim();
                if (text.toLowerCase().startsWith(key.toLowerCase())) {
                    String val = "";
                    if (text.contains(":")) {
                        val = text.substring(text.indexOf(":") + 1).trim();
                    }
                    if (val.isEmpty()) {
                        Element next = el.nextElementSibling();
                        if (next != null) val = next.text().trim();
                    }
                    if (!val.isEmpty()) {
                        info.put(key.replace(" Member", ""), val);
                        break; // Found it, move to next key
                    }
                }
            }
        }
        if (info.isEmpty()) return null;

        VBox card = new VBox(6); card.getStyleClass().add("card"); card.setPadding(new Insets(14, 18, 14, 18));
        for (var e : info.entrySet()) {
            HBox row = new HBox(8);
            Label k = new Label(e.getKey()); k.setStyle("-fx-font-size:12px;-fx-text-fill:#888;-fx-font-weight:500;"); k.setMinWidth(110);
            Label v = new Label(e.getValue()); v.setStyle("-fx-font-size:13px;-fx-text-fill:#1a1a1a;-fx-font-weight:600;");
            // Color percentage
            if (e.getKey().toLowerCase().contains("percentage")) v.setStyle(v.getStyle() + "-fx-text-fill:#004643;");
            row.getChildren().addAll(k, v);
            card.getChildren().add(row);
        }

        // Attendance progress bar
        String pctStr = info.getOrDefault("Percentage", "");
        try {
            double pct = Double.parseDouble(pctStr.replaceAll("[^0-9.]", ""));
            StackPane barBg = new StackPane();
            barBg.setStyle("-fx-background-color:#e0dbd9;-fx-background-radius:4;"); barBg.setMinHeight(10); barBg.setMaxHeight(10);
            Region barFill = new Region(); barFill.setMinHeight(10); barFill.setMaxHeight(10);
            String col = pct >= 80 ? "#004643" : pct >= 60 ? "#e6930a" : "#d94452";
            barFill.setStyle("-fx-background-color:" + col + ";-fx-background-radius:4;");
            barFill.maxWidthProperty().bind(barBg.widthProperty().multiply(pct / 100.0));
            StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
            barBg.getChildren().add(barFill);
            card.getChildren().add(barBg);
        } catch (Exception ignored) {}

        return card;
    }

    // ==================== MARKS ====================
    private void loadMarksByText(String text, boolean forceRefresh, int currentRequestId) {
        if (text == null || text.isBlank()) {
            loadMarks(null, forceRefresh, currentRequestId);
            return;
        }
        String page = MARKS_PAGE;
        String html = null;
        if (!forceRefresh) html = context.dataCacheService().getCachedHtml(page).orElse(null);
        if (html == null) {
            html = context.fetchAndCacheHtml(page);
            if (html == null) html = context.dataCacheService().getCachedHtml(page).orElse(null);
        }
        
        if (html == null) {
            for (String fb : MARKS_FALLBACKS) { 
                html = context.dataCacheService().getCachedHtml(fb).orElse(null);
                if (html == null) html = context.fetchAndCacheHtml(fb);
                if (html == null) html = context.dataCacheService().getCachedHtml(fb).orElse(null);
                if (html != null) { page = fb; break; } 
            }
        }
        if (html != null) {
            List<String[]> courses = context.portalRepository().parseDropdownOptions(html, "course");
            if (courses.isEmpty()) courses.addAll(context.portalRepository().parseDropdownOptions(html, "ddl"));
            String lowerText = text.trim().toLowerCase();
            for (String[] c : courses) {
                if (c[1].toLowerCase().contains(lowerText) || lowerText.contains(c[1].toLowerCase())) {
                    loadMarks(c[0], forceRefresh, currentRequestId);
                    return;
                }
            }
            
            // Same stale cache invalidation logic as proceedings
            if (!forceRefresh) {
                loadMarksByText(text, true, currentRequestId);
                return;
            }
        }
        loadMarks(null, forceRefresh, currentRequestId);
    }
    
    private void loadMarks(String courseValue, boolean forceRefresh, int currentRequestId) {
        String html = null;
        String page = MARKS_PAGE;
        boolean isOffline = false;

        if (!forceRefresh) html = context.dataCacheService().getCachedHtml(page).orElse(null);
        if (html == null) {
            html = context.fetchAndCacheHtml(page);
            if (html == null) {
                html = context.dataCacheService().getCachedHtml(page).orElse(null);
                isOffline = true;
            }
        }
        if (html == null) {
            for (String fb : MARKS_FALLBACKS) { 
                html = context.dataCacheService().getCachedHtml(fb).orElse(null);
                if (html == null) {
                    html = context.fetchAndCacheHtml(fb);
                    if (html == null) {
                        html = context.dataCacheService().getCachedHtml(fb).orElse(null);
                        isOffline = true;
                    }
                }
                if (html != null) { page = fb; break; } 
            }
        }
        if (html == null) { Platform.runLater(() -> showError("Could not load Marks page and no offline data available.")); return; }
        marksPageHtml = html;

        if (courseValue != null && !isOffline) {
            String ddName = context.portalRepository().findDropdownName(html, "course");
            if (ddName == null) ddName = context.portalRepository().findDropdownName(html, "ddl");
            if (ddName != null) {
                String result = context.portalRepository().postbackWithDropdown(page, ddName, courseValue);
                if (result != null) html = result;
            }
        }
        final String finalHtml = html;
        final boolean finalOffline = isOffline;
        
        try { java.nio.file.Files.writeString(java.nio.file.Paths.get("debug_marks.html"), marksPageHtml == null ? "null" : marksPageHtml); } catch(Exception e){}

        List<String[]> courses = context.portalRepository().parseDropdownOptions(marksPageHtml, "course");
        if (courses.isEmpty()) courses.addAll(context.portalRepository().parseDropdownOptions(marksPageHtml, "ddl"));
        final List<String[]> fc = courses;
        final String fp = page;

        Platform.runLater(() -> {
            if (currentRequestId != loadRequestId) return;
            setContentAnimated(buildMarksView(finalHtml, fc, courseValue, fp, finalOffline));
        });
    }

    private ScrollPane buildMarksView(String html, List<String[]> courses, String selected, String page, boolean isOffline) {
        VBox content = new VBox(14); content.setPadding(new Insets(16,28,24,28)); content.setFillWidth(true);
        if (isOffline) {
            content.getChildren().add(buildOfflineBanner());
        }
        if (!courses.isEmpty()) {
            HBox selectorRow = new HBox(10); selectorRow.setAlignment(Pos.CENTER_LEFT);
            Label lbl = new Label("Select Course:"); lbl.setStyle("-fx-font-weight:600;-fx-text-fill:#004643;-fx-font-size:13px;");
            ComboBox<String> combo = new ComboBox<>();
            Map<String,String> vm = new LinkedHashMap<>();
            for (String[] c : courses) { combo.getItems().add(c[1]); vm.put(c[1], c[0]); }
            if (selected != null) for (String[] c : courses) { if (c[0].equals(selected)) { combo.setValue(c[1]); break; } }
            combo.setPromptText("Choose a course...");
            combo.setStyle("-fx-font-size:12px;");
            combo.setOnAction(e -> { 
                String v = vm.get(combo.getValue()); 
                if (v!=null) { 
                    lastSelectedCourseTitle = combo.getValue();
                    hasExplicitlySelectedCourse = true;
                    showLoading("Loading marks..."); 
                    final int reqId = ++loadRequestId;
                    new Thread(()->loadMarks(v, false, reqId)).start(); 
                }
            });
            selectorRow.getChildren().addAll(lbl, combo);
            content.getChildren().add(selectorRow);
        }
        Document doc = Jsoup.parse(html);

        // Check for server messages
        Element msg = doc.selectFirst("#DataContent_lblMessage");
        if (msg == null) msg = doc.selectFirst(".notification.information");
        if (msg != null && !msg.text().isBlank()) {
            Label msgLbl = new Label(msg.text().replace("Select Course", ""));
            msgLbl.setStyle("-fx-font-size:14px; -fx-text-fill:#e53e3e; -fx-font-weight:bold;");
            content.getChildren().add(msgLbl);
        } else if (!hasExplicitlySelectedCourse) {
            Label msgLbl = new Label("Please select a course from the dropdown above or from the Summary tab.");
            msgLbl.setStyle("-fx-font-size:14px; -fx-text-fill:#e53e3e; -fx-font-weight:bold;");
            content.getChildren().add(msgLbl);
        }

        if (hasExplicitlySelectedCourse) {
            // Parse course info header
            VBox infoCard = parseCourseHeader(doc);
            if (infoCard != null) content.getChildren().add(infoCard);

            for (Element table : doc.select("table")) {
                String tt = table.text().toLowerCase();
                if (tt.contains("father name") || tt.contains("cnic")) continue;
                Elements rows = table.select("tr");
                if (rows.size() < 2) continue;
                VBox card = buildNativeTable(rows);
                if (card != null) content.getChildren().add(card);
            }
        }
        ScrollPane sp = new ScrollPane(content); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    // ==================== SHARED TABLE BUILDER ====================
    private VBox buildNativeTable(Elements rows) {
        Element hdrRow = null;
        int hdrIndex = 0;
        String tableTitle = null;

        for (int i = 0; i < rows.size(); i++) {
            Element r = rows.get(i);
            Elements c = r.select("th, td");
            if (c.size() == 1 && hdrRow == null) {
                tableTitle = c.first().text().trim();
            }
            if (c.size() > 1) {
                hdrRow = r;
                hdrIndex = i;
                break;
            }
        }
        if (hdrRow == null) return null;

        Elements hdrCells = hdrRow.select("th, td");
        List<String> headers = new ArrayList<>();
        for (Element c : hdrCells) { 
            String text = c.text().trim();
            if (!text.isEmpty()) headers.add(text); 
        }
        for (String h : headers) if (SKIP.contains(h.toLowerCase())) return null;
        if (headers.stream().allMatch(String::isEmpty)) return null;

        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: -color-bg-card;-fx-border-color:#e0e0e0;-fx-border-width:1;");

        if (tableTitle != null && !tableTitle.isEmpty()) {
            Label lblTitle = new Label(tableTitle);
            lblTitle.setStyle("-fx-font-size:16px;-fx-font-weight:700;-fx-text-fill:#333333;-fx-padding:14 18 10 18;");
            card.getChildren().add(lblTitle);
        }

        HBox hdrBox = new HBox(0);
        hdrBox.setStyle("-fx-background-color:#f9f9f9;-fx-border-color:#eeeeee;-fx-border-width:0 0 1 0;-fx-padding:10 0;");
        for (int i = 0; i < headers.size(); i++) {
            String hText = headers.get(i).equals("S.No") ? "Serial" : headers.get(i);
            Label h = new Label(hText);
            h.setStyle("-fx-text-fill:#666666;-fx-font-size:12px;-fx-font-weight:700;-fx-padding:0 12;");
            double w = colW(headers.get(i), i, headers.size());
            h.setMinWidth(w); h.setMaxWidth(w);
            hdrBox.getChildren().add(h);
        }
        card.getChildren().add(hdrBox);

        for (int r = hdrIndex + 1; r < rows.size(); r++) {
            Elements cells = rows.get(r).select("td");
            if (cells.isEmpty()) continue;
            HBox dataRow = new HBox(0);
            String bg = (r % 2 == 0) ? "#f9f9f9" : "white";
            dataRow.setStyle("-fx-background-color:" + bg + ";-fx-padding:10 0;-fx-border-color:#eeeeee;-fx-border-width:0 0 1 0;");
            
            for (int c = 0; c < cells.size(); c++) {
                if (c >= headers.size()) break;
                
                String htmlTxt = cells.get(c).html().replaceAll("(?i)<br[^>]*>", "\n");
                String rawTxt = Jsoup.parse(htmlTxt).wholeText();
                StringBuilder cleanTxt = new StringBuilder();
                for (String line : rawTxt.split("\n")) {
                    String t = line.trim();
                    if (!t.isEmpty()) {
                        if (cleanTxt.length() > 0) cleanTxt.append("\n");
                        cleanTxt.append(t);
                    }
                }
                String txt = cleanTxt.toString();

                Element aTag = cells.get(c).select("a").first();
                String eventTarget = "";
                if (aTag != null && aTag.attr("href").contains("__doPostBack")) {
                    String href = aTag.attr("href");
                    int start = href.indexOf("'") + 1;
                    int end = href.indexOf("'", start);
                    if (start > 0 && end > start) {
                        eventTarget = href.substring(start, end);
                    }
                }

                Label cl = new Label(txt);
                cl.setWrapText(true);
                String style = "-fx-font-size:12px;-fx-padding:0 12;-fx-text-fill:#444444;";
                String col = headers.get(c).toLowerCase();

                boolean isClickableCourse = isSummary && c == 1 && c < headers.size() && headers.get(c).toLowerCase().contains("title") && !txt.isEmpty();
                Node cellNode;

                if (col.contains("status")) {
                    style += "P".equals(txt) ? "-fx-text-fill:#339933;-fx-font-weight:700;" : "A".equals(txt) ? "-fx-text-fill:#e53e3e;-fx-font-weight:700;" : "-fx-text-fill:#444444;";
                    cl.setStyle(style);
                    cellNode = cl;
                } else if (col.contains("%") || col.contains("thy") || col.contains("lab")) {
                    if (isSummary && txt.matches("\\d+(\\.\\d+)?%?")) {
                        // Render inline progress bar only for summary tab
                        double pct = Double.parseDouble(txt.replace("%",""));
                        StackPane barBg = new StackPane();
                        barBg.setStyle("-fx-background-color:#e0dbd9;-fx-background-radius:4;"); barBg.setMinHeight(10); barBg.setMaxHeight(10);
                        Region barFill = new Region(); barFill.setMinHeight(10); barFill.setMaxHeight(10);
                        String color = pct >= 80 ? "#38a169" : pct >= 60 ? "#d69e2e" : "#e53e3e";
                        barFill.setStyle("-fx-background-color:" + color + ";-fx-background-radius:4;");
                        barFill.maxWidthProperty().bind(barBg.widthProperty().multiply(pct / 100.0));
                        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
                        barBg.getChildren().add(barFill);
                        
                        HBox pctBox = new HBox(8); pctBox.setAlignment(Pos.CENTER_LEFT);
                        Label l = new Label(txt); l.setStyle("-fx-font-size:11px;-fx-text-fill:" + color + ";-fx-font-weight:bold;");
                        l.setMinWidth(30);
                        HBox.setHgrow(barBg, Priority.ALWAYS);
                        pctBox.getChildren().addAll(barBg, l);
                        pctBox.setStyle("-fx-padding:0 12;");
                        cellNode = pctBox;
                    } else {
                        cl.setStyle(style);
                        cellNode = cl;
                    }
                } else if (isClickableCourse && !eventTarget.isEmpty()) {
                    style += "-fx-text-fill:#0066cc;-fx-underline:true;-fx-cursor:hand;";
                    String finalEventTarget = eventTarget;
                    String courseTitle = txt;
                    cl.setOnMouseClicked(e -> {
                        lastSelectedCourseTitle = courseTitle;
                        hasExplicitlySelectedCourse = true;
                        loadTab("marks_postback", finalEventTarget, false);
                    });
                    cl.setStyle(style);
                    cellNode = cl;
                } else {
                    cl.setStyle(style);
                    cellNode = cl;
                }
                
                double w = colW(headers.get(c), c, headers.size());
                if (cellNode instanceof Region reg) {
                    reg.setMinWidth(w); reg.setMaxWidth(w);
                }
                dataRow.getChildren().add(cellNode);
            }
            card.getChildren().add(dataRow);
        }
        return card;
    }

    private double colW(String h, int i, int total) {
        String l = h.toLowerCase();
        if (l.contains("s#") || l.equals("s") || l.contains("lecture#")) return 60;
        if (l.contains("course") || l.contains("title") || l.contains("topic") || l.contains("subject")) return 240;
        if (l.contains("faculty") || l.contains("teacher") || l.contains("date")) return 160;
        if (l.contains("class")) return 70;
        if (l.contains("lecture")) return 70;
        if (l.contains("status")) return 50;
        if (l.contains("thy") || l.contains("lab") || l.contains("%")) return 60;
        if (l.equals("p") || l.equals("a")) return 40;
        return 100;
    }

    private String colorPct(String t) {
        if (t.equalsIgnoreCase("N/A") || t.equalsIgnoreCase("NA")) return "-fx-text-fill:#aaa;";
        try { double v = Double.parseDouble(t.replaceAll("[^0-9.]","")); if (v>=80) return "-fx-text-fill:#004643;-fx-font-weight:700;"; if (v>=60) return "-fx-text-fill:#e6930a;-fx-font-weight:600;"; return "-fx-text-fill:#d94452;-fx-font-weight:700;"; } catch (Exception e) { return "-fx-text-fill:#1a1a1a;"; }
    }

    private void showError(String msg) { contentPane.getChildren().clear(); Label l = new Label(msg); l.setStyle("-fx-text-fill:#888;-fx-font-size:13px;-fx-padding:30;"); l.setWrapText(true); contentPane.getChildren().add(l); }

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
