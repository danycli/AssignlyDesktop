package com.assignly.view;

import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Date Sheet tab – fetches date sheet data from the portal.
 * Tries dedicated DateSheet pages first, then falls back to
 * parsing exam-schedule-like tables from Dashboard.aspx.
 */
public class DateSheetTabView {
    private final VBox root = new VBox();
    private final AppContext context;

    private static final String[] DATE_SHEET_PAGES = {
            "DateSheet.aspx", "Datesheet.aspx", "ExamDateSheet.aspx", "ExamSchedule.aspx"
    };

    // Known profile keys to skip when scanning Dashboard tables
    private static final Set<String> PROFILE_KEYS = Set.of(
            "name", "father name", "roll no", "program", "current section",
            "total registered courses", "registered courses", "current advisor",
            "date of birth", "cnic", "thesis title",
            "missing documents / disciplinary case", "missing documents"
    );

    public record ExamEntry(String course, String date, String time, String venue) {}

    private StackPane contentPane;

    public DateSheetTabView(AppContext context) {
        this.context = context;
        buildShell();
        loadDateSheet(false);
    }

    private void buildShell() {
        root.setFillWidth(true);
        
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(24, 28, 16, 28));
        headerRow.setStyle("-fx-background-color: -color-bg-card;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");

        Label heading = new Label("Date Sheet");
        heading.setStyle("-fx-font-size:24px;-fx-font-weight:800;-fx-text-fill: -color-text-main;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄");
        refreshBtn.setStyle("-fx-background-color:transparent;-fx-font-size:18px;-fx-cursor:hand;");
        refreshBtn.setOnAction(e -> {
            contentPane.getChildren().clear();
            loadDateSheet(true);
        });

        headerRow.getChildren().addAll(heading, spacer, refreshBtn);
        root.getChildren().add(headerRow);

        contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        root.getChildren().add(contentPane);
    }

    private void buildLoading() {
        contentPane.getChildren().clear();
        StackPane loading = new StackPane();
        loading.setStyle("-fx-background-color: -color-bg-main;");
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(28, 28);
        Label msg = new Label("Loading date sheet...");
        msg.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 13px;");
        box.getChildren().addAll(spinner, msg);
        loading.getChildren().add(box);
        contentPane.getChildren().add(loading);
    }

    private void loadDateSheet(boolean forceRefresh) {
        buildLoading();
        new Thread(() -> {
            List<ExamEntry> entries = new ArrayList<>();
            boolean isOffline = false;

            // 1. Try dedicated date sheet pages
            for (String page : DATE_SHEET_PAGES) {
                String html = null;
                if (!forceRefresh) html = context.dataCacheService().getCachedHtml(page).orElse(null);
                
                if (html == null) {
                    html = context.fetchAndCacheHtml(page);
                    if (html == null) {
                        html = context.dataCacheService().getCachedHtml(page).orElse(null);
                        isOffline = true;
                    }
                }
                if (html != null && !html.isBlank()) {
                    entries = parseExamTables(html);
                    if (!entries.isEmpty()) break;
                }
            }

            // 2. Fallback: parse Dashboard.aspx for any non-profile tables
            if (entries.isEmpty()) {
                String dashHtml = null;
                if (!forceRefresh) dashHtml = context.dataCacheService().getCachedHtml("Dashboard.aspx").orElse(null);
                if (dashHtml == null) {
                    dashHtml = context.fetchAndCacheHtml("Dashboard.aspx");
                    if (dashHtml == null) {
                        dashHtml = context.dataCacheService().getCachedHtml("Dashboard.aspx").orElse(null);
                        isOffline = true;
                    }
                }
                if (dashHtml != null) {
                    entries = parseDashboardForDateSheet(dashHtml);
                }
            }

            final List<ExamEntry> finalEntries = entries;
            final boolean finalOffline = isOffline;
            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                buildContent(finalEntries, finalOffline);
            });
        }).start();
    }

    /** Parse standard exam schedule tables (columns: course, date, time, venue) */
    private List<ExamEntry> parseExamTables(String html) {
        List<ExamEntry> entries = new ArrayList<>();
        Document doc = Jsoup.parse(html);

        for (Element table : doc.select("table")) {
            Elements rows = table.select("tr");
            if (rows.size() < 2) continue;

            Element headerRow = rows.first();
            if (headerRow == null) continue;
            Elements headerCells = headerRow.select("th, td");
            String headerText = headerRow.text().toLowerCase();

            boolean looksLikeDateSheet = headerText.contains("course") ||
                    headerText.contains("date") || headerText.contains("exam") ||
                    headerText.contains("time") || headerText.contains("paper");

            if (!looksLikeDateSheet) continue;

            int courseIdx = -1, dateIdx = -1, timeIdx = -1, venueIdx = -1;
            for (int i = 0; i < headerCells.size(); i++) {
                String h = headerCells.get(i).text().toLowerCase().trim();
                if (h.contains("course") || h.contains("subject") || h.contains("paper")) courseIdx = i;
                else if (h.contains("date")) dateIdx = i;
                else if (h.contains("time")) timeIdx = i;
                else if (h.contains("room") || h.contains("venue") || h.contains("hall")) venueIdx = i;
            }

            if (courseIdx >= 0 || dateIdx >= 0) {
                for (int r = 1; r < rows.size(); r++) {
                    Elements cells = rows.get(r).select("td");
                    if (cells.isEmpty()) continue;
                    String course = safeCell(cells, courseIdx);
                    String date = safeCell(cells, dateIdx);
                    String time = safeCell(cells, timeIdx);
                    String venue = safeCell(cells, venueIdx);
                    if (!course.isBlank() || !date.isBlank()) {
                        entries.add(new ExamEntry(course, date, time, venue));
                    }
                }
            }
        }
        return entries;
    }

    /** Parse Dashboard.aspx for non-profile table data that looks like a date sheet */
    private List<ExamEntry> parseDashboardForDateSheet(String html) {
        List<ExamEntry> entries = new ArrayList<>();
        Document doc = Jsoup.parse(html);

        for (Element table : doc.select("table")) {
            Elements rows = table.select("tr");
            if (rows.size() < 2) continue;

            // Check if this is the profile table – skip it
            boolean isProfileTable = false;
            for (Element row : rows) {
                String rowText = row.text().toLowerCase().trim();
                if (rowText.contains("father name") || rowText.contains("roll no") || rowText.contains("cnic")) {
                    isProfileTable = true;
                    break;
                }
            }
            if (isProfileTable) continue;

            // Check header for date-sheet-like content
            Element headerRow = rows.first();
            Elements headerCells = headerRow.select("th, td");
            String headerText = headerRow.text().toLowerCase();

            if (headerText.contains("course") || headerText.contains("date") ||
                    headerText.contains("exam") || headerText.contains("paper") ||
                    headerText.contains("time")) {

                int courseIdx = -1, dateIdx = -1, timeIdx = -1, venueIdx = -1;
                for (int i = 0; i < headerCells.size(); i++) {
                    String h = headerCells.get(i).text().toLowerCase().trim();
                    if (h.contains("course") || h.contains("subject") || h.contains("paper")) courseIdx = i;
                    else if (h.contains("date")) dateIdx = i;
                    else if (h.contains("time")) timeIdx = i;
                    else if (h.contains("room") || h.contains("venue") || h.contains("hall")) venueIdx = i;
                }

                for (int r = 1; r < rows.size(); r++) {
                    Elements cells = rows.get(r).select("td");
                    if (cells.isEmpty()) continue;
                    String course = safeCell(cells, courseIdx);
                    String date = safeCell(cells, dateIdx);
                    String time = safeCell(cells, timeIdx);
                    String venue = safeCell(cells, venueIdx);
                    if (!course.isBlank() || !date.isBlank()) {
                        entries.add(new ExamEntry(course, date, time, venue));
                    }
                }
            }
        }
        return entries;
    }

    private String safeCell(Elements cells, int idx) {
        return (idx >= 0 && idx < cells.size()) ? cells.get(idx).text().trim() : "";
    }

    private void buildContent(List<ExamEntry> entries, boolean isOffline) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        if (isOffline) {
            content.getChildren().add(buildOfflineBanner());
        }

        if (entries.isEmpty()) {
            Label noData = new Label("No date sheet data available at this time.");
            noData.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 13px; -fx-padding: 20 0;");
            noData.setWrapText(true);
            content.getChildren().add(noData);
        } else {
            for (ExamEntry entry : entries) {
                VBox card = new VBox(4);
                card.getStyleClass().add("card");
                card.setPadding(new Insets(14, 18, 14, 18));

                Label courseLabel = new Label(entry.course().isBlank() ? "—" : entry.course());
                courseLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -color-text-main;");

                HBox details = new HBox(16);
                if (!entry.date().isBlank()) {
                    Label d = new Label("📅  " + entry.date());
                    d.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-accent; -fx-font-weight: 500;");
                    details.getChildren().add(d);
                }
                if (!entry.time().isBlank()) {
                    Label t = new Label("🕐  " + entry.time());
                    t.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-muted;");
                    details.getChildren().add(t);
                }
                if (!entry.venue().isBlank()) {
                    Label v = new Label("📍  " + entry.venue());
                    v.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-muted;");
                    details.getChildren().add(v);
                }

                card.getChildren().add(courseLabel);
                if (!details.getChildren().isEmpty()) card.getChildren().add(details);
                content.getChildren().add(card);
            }
        }

        // RESIZING FIX: Set a safe minimum width on the content container so cards and labels maintain proper grid alignments without visual compression
        content.setMinWidth(600);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        // RESIZING FIX: Allow horizontal scrollbars when the window is shrunk, making sure all dates, course titles, and venues remain accessible
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        contentPane.getChildren().add(scroll);
    }

    public VBox getRoot() { return root; }

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
}
