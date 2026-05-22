package com.assignly.view;

import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Native timetable view – parses Timetable.aspx and renders
 * a clean grid with days as rows and time slots as columns.
 */
public class TimetableTabView {

    private final VBox root = new VBox();
    private final AppContext context;

    // Accent colors for course cards (cycled per unique course)
    private static final String[][] CARD_COLORS = {
        {"#eff6ff", "#1e40af", "#bfdbfe"},  // blue
        {"#f0fdf4", "#166534", "#bbf7d0"},  // green
        {"#fef3c7", "#92400e", "#fde68a"},  // amber
        {"#fdf2f8", "#9d174d", "#fbcfe8"},  // pink
        {"#f5f3ff", "#5b21b6", "#ddd6fe"},  // violet
        {"#ecfdf5", "#065f46", "#a7f3d0"},  // emerald
        {"#fff7ed", "#9a3412", "#fed7aa"},  // orange
        {"#f0f9ff", "#075985", "#bae6fd"},  // sky
    };

    private final List<String> seenCourses = new ArrayList<>();

    public TimetableTabView(AppContext context) {
        this.context = context;
        buildLoading();
        fetchTimetable();
    }

    public VBox getRoot() { return root; }

    private void buildLoading() {
        StackPane loading = new StackPane();
        loading.setStyle("-fx-background-color: #F0EDEC;");
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(28, 28);
        Label msg = new Label("Loading timetable...");
        msg.setStyle("-fx-text-fill: #888888; -fx-font-size: 13px;");
        box.getChildren().addAll(spinner, msg);
        loading.getChildren().add(box);
        VBox.setVgrow(loading, Priority.ALWAYS);
        root.getChildren().add(loading);
    }

    private void fetchTimetable() {
        new Thread(() -> {
            try {
                String html = context.portalRepository().fetchPageHtml("Timetable.aspx");
                if (html == null) {
                    Platform.runLater(() -> {
                        root.getChildren().clear();
                        root.getChildren().add(buildError("Unable to load timetable.",
                                "Failed to connect to the portal. Please check your internet connection."));
                    });
                    return;
                }
                Platform.runLater(() -> {
                    root.getChildren().clear();
                    try {
                        root.getChildren().add(buildTimetableView(html));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        root.getChildren().clear();
                        root.getChildren().add(buildError("Parsing Error",
                                "Could not parse the timetable page: " + ex.getMessage()));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    root.getChildren().clear();
                    root.getChildren().add(buildError("Error", e.getMessage()));
                });
            }
        }).start();
    }

    // ==================== Build View ====================

    private ScrollPane buildTimetableView(String html) {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);

        VBox content = new VBox(16);
        content.setPadding(new Insets(24, 28, 24, 28));
        content.setFillWidth(true);

        Document doc = Jsoup.parse(html);

        // Page heading
        String heading = "Student Time Table";
        Element h3 = doc.select("h3").first();
        if (h3 != null && !h3.text().trim().isEmpty()) {
            heading = h3.text().trim();
        }
        Label title = new Label(heading);
        title.setStyle("-fx-font-size:18px;-fx-font-weight:800;-fx-text-fill:#1e293b;");
        content.getChildren().add(title);

        // Find the timetable table — skip student info tables
        Element table = null;
        Elements allTables = doc.select("table");
        for (Element t : allTables) {
            String txt = t.text().toLowerCase();
            // The timetable grid has day names (Monday, Tuesday) or "DayTitle" header
            boolean hasDays = txt.contains("monday") || txt.contains("tuesday")
                    || txt.contains("wednesday") || txt.contains("thursday")
                    || txt.contains("daytitle");
            // Skip tables that look like student info (Name:, Roll No:, Father Name:)
            boolean looksLikeStudentInfo = txt.contains("father name")
                    || txt.contains("roll no") || txt.contains("thesis title")
                    || txt.contains("current section") || txt.contains("registered courses");
            if (hasDays && !looksLikeStudentInfo) {
                table = t;
                break;
            }
        }

        if (table == null) {
            Label noData = new Label("No timetable data found.");
            noData.setStyle("-fx-text-fill:#64748b;-fx-font-size:13px;-fx-padding:20;");
            content.getChildren().add(noData);
            sp.setContent(content);
            return sp;
        }

        Elements rows = table.select("tr");
        if (rows.isEmpty()) {
            Label noData = new Label("Timetable is empty.");
            noData.setStyle("-fx-text-fill:#64748b;-fx-font-size:13px;-fx-padding:20;");
            content.getChildren().add(noData);
            sp.setContent(content);
            return sp;
        }

        // Parse header row for time slots
        Element headerRow = rows.first();
        Elements headerCells = headerRow.select("th, td");
        List<String> timeSlots = new ArrayList<>();
        for (Element hc : headerCells) {
            timeSlots.add(hc.text().trim());
        }

        // Build the grid
        VBox tableCard = new VBox(0);
        tableCard.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                + "-fx-border-color:#e2e8f0;-fx-border-width:1;-fx-border-radius:12;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.04),12,0,0,3);");

        // Wrap in a horizontal scroll pane for wide timetables
        ScrollPane hScroll = new ScrollPane();
        hScroll.setFitToHeight(true);
        hScroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        hScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox innerTable = new VBox(0);

        // Header row
        HBox headerBox = new HBox(0);
        headerBox.setStyle("-fx-background-color:#f1f5f9;-fx-padding:10 0;"
                + "-fx-border-color:#e2e8f0;-fx-border-width:0 0 2 0;");
        for (int i = 0; i < timeSlots.size(); i++) {
            Label hl = new Label(timeSlots.get(i));
            hl.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:#475569;"
                    + "-fx-padding:6 8;-fx-text-alignment:center;-fx-alignment:center;");
            hl.setAlignment(Pos.CENTER);
            double w = (i == 0) ? 90 : 110;
            hl.setMinWidth(w);
            hl.setPrefWidth(w);
            hl.setMaxWidth(w);
            hl.setWrapText(true);
            headerBox.getChildren().add(hl);
        }
        innerTable.getChildren().add(headerBox);

        // Data rows
        for (int r = 1; r < rows.size(); r++) {
            Element row = rows.get(r);
            Elements cells = row.select("td");
            if (cells.isEmpty()) continue;

            HBox dataRow = new HBox(0);
            String bg = (r % 2 == 0) ? "#f8fafc" : "white";
            dataRow.setStyle("-fx-background-color:" + bg + ";-fx-padding:4 0;"
                    + "-fx-border-color:#f1f5f9;-fx-border-width:0 0 1 0;");
            dataRow.setAlignment(Pos.CENTER_LEFT);
            dataRow.setMinHeight(70);

            int colIndex = 0;
            for (Element cell : cells) {
                int colspan = 1;
                String csAttr = cell.attr("colspan");
                if (!csAttr.isEmpty()) {
                    try { colspan = Integer.parseInt(csAttr); } catch (NumberFormatException ignored) {}
                }

                String cellText = cell.text().trim();
                double baseW = (colIndex == 0) ? 90 : 110;
                double cellWidth = baseW;
                if (colspan > 1) {
                    // First cell is 90, rest are 110
                    cellWidth = 0;
                    for (int s = 0; s < colspan; s++) {
                        cellWidth += (colIndex + s == 0) ? 90 : 110;
                    }
                }

                if (colIndex == 0) {
                    // Day name column
                    Label dayLabel = new Label(cellText);
                    dayLabel.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#334155;"
                            + "-fx-padding:8 10;");
                    dayLabel.setMinWidth(cellWidth);
                    dayLabel.setPrefWidth(cellWidth);
                    dayLabel.setMaxWidth(cellWidth);
                    dataRow.getChildren().add(dayLabel);
                } else if (cellText.isEmpty() || cellText.equals("—") || cellText.equals("-")) {
                    // Empty slot
                    Label empty = new Label("—");
                    empty.setAlignment(Pos.CENTER);
                    empty.setStyle("-fx-text-fill:#cbd5e1;-fx-font-size:12px;-fx-padding:8;");
                    empty.setMinWidth(cellWidth);
                    empty.setPrefWidth(cellWidth);
                    empty.setMaxWidth(cellWidth);
                    dataRow.getChildren().add(empty);
                } else {
                    // Course cell - parse course info
                    VBox courseCard = buildCourseCard(cell, cellWidth);
                    dataRow.getChildren().add(courseCard);
                }

                colIndex += colspan;
            }

            innerTable.getChildren().add(dataRow);
        }

        hScroll.setContent(innerTable);
        tableCard.getChildren().add(hScroll);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        content.getChildren().add(tableCard);

        sp.setContent(content);
        return sp;
    }

    private VBox buildCourseCard(Element cell, double width) {
        // Parse the cell content: typically course name, code(section), teacher
        // The cell may have <br> tags or just text nodes
        String fullText = cell.text().trim();
        Elements links = cell.select("a");

        String courseName = "";
        String codeSection = "";
        String teacher = "";

        // Try to extract from links and text nodes
        if (!links.isEmpty()) {
            // First link is usually the teacher
            teacher = links.last().text().trim();
        }

        // Split by known patterns: text lines separated by the link text
        String[] parts = fullText.split("\\s{2,}");
        if (parts.length == 1) {
            // Try splitting by the teacher name
            if (!teacher.isEmpty() && fullText.contains(teacher)) {
                String before = fullText.substring(0, fullText.lastIndexOf(teacher)).trim();
                // before might be like "Data Structures 2214(M)"
                int parenIdx = before.lastIndexOf('(');
                if (parenIdx > 0) {
                    // Find the start of the code — look for a digit sequence before '('
                    int codeStart = parenIdx;
                    while (codeStart > 0 && (Character.isDigit(before.charAt(codeStart - 1))
                            || before.charAt(codeStart - 1) == ' ')) {
                        codeStart--;
                    }
                    courseName = before.substring(0, codeStart).trim();
                    codeSection = before.substring(codeStart).trim();
                } else {
                    courseName = before;
                }
            } else {
                courseName = fullText;
            }
        } else if (parts.length >= 2) {
            courseName = parts[0];
            codeSection = parts.length > 1 ? parts[1] : "";
            if (parts.length > 2) teacher = parts[2];
        }

        // If courseName still has the code embedded, try to separate
        if (codeSection.isEmpty() && courseName.matches(".*\\d+\\(.*\\).*")) {
            int idx = -1;
            for (int i = 0; i < courseName.length(); i++) {
                if (Character.isDigit(courseName.charAt(i))) {
                    // Check if preceded by space
                    if (i > 0 && courseName.charAt(i - 1) == ' ') {
                        idx = i;
                        break;
                    }
                }
            }
            if (idx > 0) {
                codeSection = courseName.substring(idx).trim();
                courseName = courseName.substring(0, idx).trim();
            }
        }

        // Get color for this course
        String courseKey = courseName.isEmpty() ? fullText : courseName;
        int colorIdx = getColorIndex(courseKey);
        String bgColor = CARD_COLORS[colorIdx][0];
        String textColor = CARD_COLORS[colorIdx][1];
        String borderColor = CARD_COLORS[colorIdx][2];

        VBox card = new VBox(2);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(6, 8, 6, 8));
        card.setStyle("-fx-background-color:" + bgColor + ";"
                + "-fx-border-color:" + borderColor + ";"
                + "-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;"
                + "-fx-margin:3;");
        card.setMinWidth(width - 8);
        card.setPrefWidth(width - 8);
        card.setMaxWidth(width - 8);

        if (!courseName.isEmpty()) {
            Label nameLabel = new Label(courseName);
            nameLabel.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:" + textColor + ";");
            nameLabel.setWrapText(true);
            card.getChildren().add(nameLabel);
        }

        if (!codeSection.isEmpty()) {
            Label codeLabel = new Label(codeSection);
            codeLabel.setStyle("-fx-font-size:10px;-fx-text-fill:" + textColor + ";-fx-opacity:0.7;");
            card.getChildren().add(codeLabel);
        }

        if (!teacher.isEmpty()) {
            Label teacherLabel = new Label(teacher);
            teacherLabel.setStyle("-fx-font-size:10px;-fx-font-weight:600;-fx-text-fill:" + textColor + ";-fx-opacity:0.8;");
            teacherLabel.setWrapText(true);
            card.getChildren().add(teacherLabel);
        }

        // If nothing was parsed, just show the full text
        if (card.getChildren().isEmpty()) {
            Label fallback = new Label(fullText);
            fallback.setStyle("-fx-font-size:10px;-fx-text-fill:" + textColor + ";");
            fallback.setWrapText(true);
            card.getChildren().add(fallback);
        }

        // Wrap in a padded container
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

    private int getColorIndex(String courseName) {
        String key = courseName.toLowerCase().trim();
        int idx = seenCourses.indexOf(key);
        if (idx < 0) {
            seenCourses.add(key);
            idx = seenCourses.size() - 1;
        }
        return idx % CARD_COLORS.length;
    }

    // ==================== Error State ====================

    private VBox buildError(String title, String message) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        VBox.setVgrow(box, Priority.ALWAYS);

        Label icon = new Label("⚠");
        icon.setStyle("-fx-font-size:28px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#334155;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;-fx-text-alignment:center;");
        msgLabel.setWrapText(true);

        box.getChildren().addAll(icon, titleLabel, msgLabel);
        return box;
    }
}
