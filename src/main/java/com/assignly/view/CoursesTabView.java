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
        Label heading = new Label("Courses");
        heading.getStyleClass().add("heading-label");
        heading.setPadding(new Insets(24, 28, 0, 28));
        tabBar = new HBox(4);
        tabBar.setPadding(new Insets(12, 28, 0, 28));
        tabBar.getChildren().addAll(
            tabBtn("Summary","summary"), tabBtn("Class Proceedings","proceedings"), tabBtn("Q.A/Sess/Final Marks","marks"));
        contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        root.getChildren().addAll(heading, tabBar, contentPane);
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
                  : "-fx-background-color:white;-fx-text-fill:#666;-fx-font-size:12px;-fx-font-weight:500;-fx-background-radius:6;-fx-padding:6 14;-fx-border-color:#d5d0ce;-fx-border-radius:6;-fx-border-width:1;";
    }

    private void loadTab(String id) {
        loadTab(id, null);
    }

    private void loadTab(String tabKey, String extraParam) {
        if (activeTab.equals(tabKey) && !tabKey.endsWith("_postback")) return;
        activeTab = tabKey;
        isSummary = tabKey.equals("summary");

        for (var n : tabBar.getChildren()) {
            if (n instanceof Button b) {
                b.setStyle(tabStyle(tabKey.replace("_postback", "").equals(b.getUserData())));
            }
        }

        showLoading("Loading...");
        new Thread(() -> {
            switch (tabKey) {
                case "summary" -> loadSummary();
                case "proceedings" -> loadProceedings(extraParam);
                case "proceedings_postback" -> loadProceedingsFromSummary(extraParam);
                case "marks" -> loadMarksByText(extraParam);
                case "marks_postback" -> loadMarksFromSummary(extraParam);
            }
        }).start();
    }

    private void loadMarksFromSummary(String eventTarget) {
        String res = context.portalRepository().postbackEvent("Summary.aspx", eventTarget);
        try { java.nio.file.Files.writeString(java.nio.file.Paths.get("marks_postback_result.html"), res == null ? "null" : res); } catch(Exception e){}
        loadMarks(null);
    }

    private void loadProceedingsFromSummary(String eventTarget) {
        String res = context.portalRepository().postbackEvent("Summary.aspx", eventTarget);
        try { java.nio.file.Files.writeString(java.nio.file.Paths.get("summary_postback_result.html"), res == null ? "null" : res); } catch(Exception e){}
        loadProceedings(null);
    }

    private void showLoading(String msg) {
        Platform.runLater(() -> {
            contentPane.getChildren().clear();
            VBox box = new VBox(10); box.setAlignment(Pos.CENTER);
            ProgressIndicator sp = new ProgressIndicator(); sp.setMaxSize(28,28);
            Label l = new Label(msg); l.setStyle("-fx-text-fill:#888;-fx-font-size:12px;");
            box.getChildren().addAll(sp, l);
            contentPane.getChildren().add(new StackPane(box));
        });
    }

    // ==================== SUMMARY ====================
    private void loadSummary() {
        String html = context.portalRepository().fetchPageHtml("Summary.aspx");
        try { java.nio.file.Files.writeString(java.nio.file.Paths.get("summary_raw.html"), html); } catch(Exception e){}
        Platform.runLater(() -> {
            contentPane.getChildren().clear();
            if (html == null) { showError("Could not load Summary."); return; }
            contentPane.getChildren().add(buildSummaryView(html));
        });
    }

    private ScrollPane buildSummaryView(String html) {
        VBox content = new VBox(16); content.setPadding(new Insets(16,28,24,28)); content.setFillWidth(true);
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
    private void loadProceedingsByText(String text) {
        if (text == null) {
            loadProceedings(null);
            return;
        }
        String html = context.portalRepository().fetchPageHtml(PROC_PAGE);
        if (html != null) {
            List<String[]> courses = context.portalRepository().parseDropdownOptions(html, "course");
            if (courses.isEmpty()) courses.addAll(context.portalRepository().parseDropdownOptions(html, "ddl"));
            for (String[] c : courses) {
                if (c[1].equalsIgnoreCase(text.trim())) {
                    loadProceedings(c[0]);
                    return;
                }
            }
        }
        loadProceedings(null);
    }

    private void loadProceedings(String courseValue) {
        // First fetch the page to get the dropdown
        String html = context.portalRepository().fetchPageHtml(PROC_PAGE);
        if (html == null) { Platform.runLater(() -> showError("Could not load Class Proceedings page.")); return; }
        proceedingsPageHtml = html;

        try { java.nio.file.Files.writeString(java.nio.file.Paths.get("proceedings_raw.html"), html); } catch(Exception e){}

        // If a course is selected, do postback
        if (courseValue != null) {
            String ddName = context.portalRepository().findDropdownName(html, "course");
            if (ddName == null) ddName = context.portalRepository().findDropdownName(html, "ddl");
            if (ddName != null) {
                String result = context.portalRepository().postbackWithDropdown(PROC_PAGE, ddName, courseValue);
                if (result != null) {
                    html = result;
                    try { java.nio.file.Files.writeString(java.nio.file.Paths.get("proceedings_postback.html"), html); } catch(Exception e){}
                }
            }
        }
        final String finalHtml = html;
        final List<String[]> courses = context.portalRepository().parseDropdownOptions(proceedingsPageHtml, "course");
        if (courses.isEmpty()) {
            // Try alternate dropdown ID patterns
            courses.addAll(context.portalRepository().parseDropdownOptions(proceedingsPageHtml, "ddl"));
        }

        Platform.runLater(() -> {
            contentPane.getChildren().clear();
            contentPane.getChildren().add(buildProceedingsView(finalHtml, courses, courseValue));
        });
    }

    private ScrollPane buildProceedingsView(String html, List<String[]> courses, String selected) {
        VBox content = new VBox(14); content.setPadding(new Insets(16,28,24,28)); content.setFillWidth(true);

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
                if (val != null) { showLoading("Loading proceedings..."); new Thread(() -> loadProceedings(val)).start(); }
            });
            selectorRow.getChildren().addAll(lbl, combo);
            content.getChildren().add(selectorRow);
        }

        Document doc = Jsoup.parse(html);

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
    private void loadMarksByText(String text) {
        if (text == null) {
            loadMarks(null);
            return;
        }
        String page = MARKS_PAGE;
        String html = context.portalRepository().fetchPageHtml(page);
        if (html == null) {
            for (String fb : MARKS_FALLBACKS) { html = context.portalRepository().fetchPageHtml(fb); if (html != null) { page = fb; break; } }
        }
        if (html != null) {
            List<String[]> courses = context.portalRepository().parseDropdownOptions(html, "course");
            if (courses.isEmpty()) courses.addAll(context.portalRepository().parseDropdownOptions(html, "ddl"));
            for (String[] c : courses) {
                if (c[1].equalsIgnoreCase(text.trim())) {
                    loadMarks(c[0]);
                    return;
                }
            }
        }
        loadMarks(null);
    }
    private void loadMarks(String courseValue) {
        String html = null;
        String page = MARKS_PAGE;
        html = context.portalRepository().fetchPageHtml(page);
        if (html == null) {
            for (String fb : MARKS_FALLBACKS) { html = context.portalRepository().fetchPageHtml(fb); if (html != null) { page = fb; break; } }
        }
        if (html == null) { Platform.runLater(() -> showError("Could not load Marks page.")); return; }
        marksPageHtml = html;
        try { java.nio.file.Files.writeString(java.nio.file.Paths.get("marks_raw.html"), html); } catch(Exception e){}

        if (courseValue != null) {
            String ddName = context.portalRepository().findDropdownName(html, "course");
            if (ddName == null) ddName = context.portalRepository().findDropdownName(html, "ddl");
            if (ddName != null) {
                String result = context.portalRepository().postbackWithDropdown(page, ddName, courseValue);
                if (result != null) html = result;
            }
        }
        final String finalHtml = html;
        List<String[]> courses = context.portalRepository().parseDropdownOptions(marksPageHtml, "course");
        if (courses.isEmpty()) courses.addAll(context.portalRepository().parseDropdownOptions(marksPageHtml, "ddl"));
        final List<String[]> fc = courses;
        final String fp = page;

        Platform.runLater(() -> {
            contentPane.getChildren().clear();
            contentPane.getChildren().add(buildMarksView(finalHtml, fc, courseValue, fp));
        });
    }

    private ScrollPane buildMarksView(String html, List<String[]> courses, String selected, String page) {
        VBox content = new VBox(14); content.setPadding(new Insets(16,28,24,28)); content.setFillWidth(true);
        if (!courses.isEmpty()) {
            HBox selectorRow = new HBox(10); selectorRow.setAlignment(Pos.CENTER_LEFT);
            Label lbl = new Label("Select Course:"); lbl.setStyle("-fx-font-weight:600;-fx-text-fill:#004643;-fx-font-size:13px;");
            ComboBox<String> combo = new ComboBox<>();
            Map<String,String> vm = new LinkedHashMap<>();
            for (String[] c : courses) { combo.getItems().add(c[1]); vm.put(c[1], c[0]); }
            if (selected != null) for (String[] c : courses) { if (c[0].equals(selected)) { combo.setValue(c[1]); break; } }
            combo.setPromptText("Choose a course...");
            combo.setStyle("-fx-font-size:12px;");
            combo.setOnAction(e -> { String v = vm.get(combo.getValue()); if (v!=null) { showLoading("Loading marks..."); new Thread(()->loadMarks(v)).start(); }});
            selectorRow.getChildren().addAll(lbl, combo);
            content.getChildren().add(selectorRow);
        }
        Document doc = Jsoup.parse(html);

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
        card.setStyle("-fx-background-color:white;-fx-border-color:#e0e0e0;-fx-border-width:1;");

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
                    cl.setOnMouseClicked(e -> loadTab("marks_postback", finalEventTarget));
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

    public VBox getRoot() { return root; }
}
