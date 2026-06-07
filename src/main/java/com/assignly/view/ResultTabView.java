package com.assignly.view;

import com.assignly.service.PdfExportService;
import com.assignly.service.PortalRepository.GpaHistoryData;
import com.assignly.util.AppContext;
import com.assignly.util.ErrorReporter;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.*;

public class ResultTabView {

    private final VBox root = new VBox();
    private final AppContext context;

    public record SemesterResultTable(String title, List<String> headers, List<List<String>> data) {}

    public ResultTabView(AppContext context) {
        this.context = context;
        buildLoading();
        loadData(false);
    }

    public VBox getRoot() { return root; }

    private void buildLoading() {
        ScrollPane sp = new ScrollPane(com.assignly.util.ShimmerBuilder.buildResultShimmer());
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        root.getChildren().add(sp);
    }

    private void loadData(boolean forceRefresh) {
        new Thread(() -> {
            try {
                String html = null;
                boolean isOffline = false;

                if (!forceRefresh) {
                    html = context.dataCacheService().getCachedHtml("StudentResultCard.aspx").orElse(null);
                }

                if (html == null) {
                    html = context.fetchAndCacheHtml("StudentResultCard.aspx");
                    if (html == null) {
                        html = context.dataCacheService().getCachedHtml("StudentResultCard.aspx").orElse(null);
                        isOffline = true;
                    }
                }

                if (html == null) {
                    showError("Unable to load Result Card", "Failed to connect to the portal and no offline data available.");
                    return;
                }

                Document doc = Jsoup.parse(html);
                List<SemesterResultTable> resultTables = new ArrayList<>();
                Elements allTables = doc.select("table");

                for (Element table : allTables) {
                    Elements rows = table.select("tr");
                    if (rows.size() < 2) continue;

                    String titleText = "Semester Result";
                    int headerRowIndex = -1;

                    // Scan rows to find the actual header row (has multiple columns)
                    // and capture any title row that comes before it
                    for (int i = 0; i < rows.size(); i++) {
                        Elements cells = rows.get(i).select("th, td");
                        if (cells.size() > 3) {
                            headerRowIndex = i;
                            break;
                        } else if (cells.size() == 1) {
                            String possibleTitle = cells.first().text().trim();
                            if (!possibleTitle.isEmpty()) {
                                titleText = possibleTitle;
                            }
                        }
                    }

                    if (headerRowIndex == -1) continue;

                    // If title wasn't inside the table, check preceding siblings
                    if (titleText.equals("Semester Result")) {
                        Element prev = table.previousElementSibling();
                        while (prev != null) {
                            String txt = prev.text().trim();
                            if (!txt.isEmpty()) {
                                titleText = txt;
                                break;
                            }
                            prev = prev.previousElementSibling();
                        }
                    }

                    // Extract the headers
                    Elements headerCells = rows.get(headerRowIndex).select("th, td");
                    List<String> headers = new ArrayList<>();
                    for (Element th : headerCells) headers.add(th.text().trim());

                    // Validate that this is actually a transcript table
                    boolean isTranscript = false;
                    for (String header : headers) {
                        String lower = header.toLowerCase();
                        if (lower.contains("course") || lower.contains("credit") || lower.contains("marks") || lower.contains("grade") || lower.equals("lg")) {
                            isTranscript = true;
                            break;
                        }
                    }
                    if (!isTranscript) continue;

                    // Extract the data rows
                    List<List<String>> data = new ArrayList<>();
                    for (int i = headerRowIndex + 1; i < rows.size(); i++) {
                        Elements cells = rows.get(i).select("td, th");
                        if (cells.isEmpty()) continue;
                        
                        List<String> rowData = new ArrayList<>();
                        for (Element cell : cells) rowData.add(cell.text().trim());
                        
                        if (!rowData.isEmpty() && String.join("", rowData).trim().length() > 0) {
                            while (rowData.size() < headers.size()) rowData.add("");
                            data.add(rowData);
                        }
                    }

                    if (!data.isEmpty()) {
                        resultTables.add(new SemesterResultTable(titleText, headers, data));
                    }
                }

                List<GpaHistoryData> gpaHistory = context.portalRepository().parseGpaHistory(html);
                boolean finalOffline = isOffline;
                List<SemesterResultTable> finalResultTables = resultTables;
                
                Platform.runLater(() -> {
                    root.getChildren().clear();
                    if (finalOffline) root.getChildren().add(buildOfflineBanner());
                    if (finalResultTables.isEmpty()) {
                        root.getChildren().add(buildEmptyView());
                    } else {
                        root.getChildren().add(buildMultiTableView(finalResultTables, gpaHistory));
                    }
                });

            } catch (Exception e) {
                ErrorReporter.logError("ResultTabView#loadData", e);
                showError("Error", "An unexpected error occurred: " + e.getMessage());
            }
        }).start();
    }

    private VBox buildEmptyView() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24, 28, 24, 28));
        content.setFillWidth(true);
        VBox.setVgrow(content, Priority.ALWAYS);

        Label title = new Label("Result Card");
        title.setStyle("-fx-font-size:24px;-fx-font-weight:800;-fx-text-fill: -color-text-main;");
        content.getChildren().add(title);

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(32));
        card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:14;"
                + "-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:14;"
                + "-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        Label icon = new Label("🎓");
        icon.setStyle("-fx-font-size:32px;");

        Label noResultLabel = new Label("No Result Card Available");
        noResultLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-muted;");
        
        Label descLabel = new Label("There are currently no transcripts available for your profile.");
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-muted;");

        card.getChildren().addAll(icon, noResultLabel, descLabel);
        content.getChildren().add(card);

        return content;
    }

    private VBox buildMultiTableView(List<SemesterResultTable> tables, List<GpaHistoryData> gpaHistory) {
        VBox wrapper = new VBox(20);
        wrapper.setPadding(new Insets(24, 28, 40, 28));
        wrapper.setFillWidth(true);
        VBox.setVgrow(wrapper, Priority.ALWAYS);

        // Header Title
        VBox titleBox = new VBox(4);
        Label title = new Label("Transcript & Results");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        Label subTitle = new Label("Semester Performance & Academic History");
        subTitle.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500;");
        titleBox.getChildren().addAll(title, subTitle);
        wrapper.getChildren().add(titleBox);

        // 1. Academic Snapshot Row
        double currentCgpa = -1.0;
        if (gpaHistory != null && !gpaHistory.isEmpty()) {
            for (int i = 0; i < gpaHistory.size(); i++) {
                if (gpaHistory.get(i).cgpa() > 0 && gpaHistory.get(i).cgpa() <= 4.0) {
                    // Update current CGPA with the latest valid computed cumulative CGPA
                    currentCgpa = gpaHistory.get(i).cgpa();
                }
            }
        }
        String cgpaVal = currentCgpa > 0 ? String.format("%.2f", currentCgpa) : "N/A";
        
        double earnedCredits = calculateTotalEarnedCredits(tables);
        String earnedCreditsVal = String.format("%.1f", earnedCredits);
        
        int completedSems = countCompletedSemesters(tables, gpaHistory);
        String completedSemsVal = String.valueOf(completedSems);
        
        String standingVal = "Good Standing";
        String standingDesc = "Maintaining strong academic progress";
        String standingTooltip = "CGPA 3.00 - 3.49";
        
        if (currentCgpa > 0) {
            if (currentCgpa >= 3.50) {
                standingVal = "Excellent Standing";
                standingDesc = "Outstanding academic performance";
                standingTooltip = "CGPA ≥ 3.50";
            } else if (currentCgpa >= 3.00) {
                standingVal = "Good Standing";
                standingDesc = "Maintaining strong academic progress";
                standingTooltip = "CGPA 3.00 - 3.49";
            } else if (currentCgpa >= 2.50) {
                standingVal = "Satisfactory Standing";
                standingDesc = "Performance is acceptable but can improve";
                standingTooltip = "CGPA 2.50 - 2.99";
            } else if (currentCgpa >= 2.00) {
                standingVal = "Academic Warning";
                standingDesc = "CGPA approaching risk threshold";
                standingTooltip = "CGPA 2.00 - 2.49";
            } else {
                standingVal = "Academic Probation";
                standingDesc = "Immediate academic improvement required";
                standingTooltip = "CGPA < 2.00";
            }
        } else {
            standingVal = "N/A";
            standingDesc = "No CGPA data available";
            standingTooltip = "CGPA not yet computed";
        }
        
        HBox snapshotRow = new HBox(16);
        snapshotRow.setFillHeight(true);
        
        VBox cgpaCard = buildCgpaCardWithStanding("Current CGPA", cgpaVal, standingVal, standingDesc, standingTooltip, "🏆");
        VBox creditsCard = buildCompactStat("Earned Credits", earnedCreditsVal, "Completed hours", "📈");
        VBox semsCard = buildCompactStat("Completed Terms", completedSemsVal, "Academic terms", "📅");
        
        Button exportBtn = new Button("📥 Export Transcript PDF");
        exportBtn.setOnAction(e -> exportResultPdf(tables, gpaHistory));
        VBox pdfCard = buildActionCard("Export Options", "Save transcript report", exportBtn, "📄");
        
        snapshotRow.getChildren().addAll(cgpaCard, creditsCard, semsCard, pdfCard);
        wrapper.getChildren().add(snapshotRow);

        // 2. CGPA Trend Chart Section
        if (gpaHistory != null && gpaHistory.size() >= 2) {
            VBox chartCard = buildTrendChartCard(gpaHistory);
            wrapper.getChildren().add(chartCard);
        }

        // 3. Semester Breakdown List
        Label semestersHeader = new Label("Semester Performance Breakdown");
        semestersHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: -color-text-main; -fx-padding: 8 0 0 0;");
        wrapper.getChildren().add(semestersHeader);

        VBox semestersList = new VBox(16);
        semestersList.setFillWidth(true);
        for (int i = 0; i < tables.size(); i++) {
            semestersList.getChildren().add(buildSemesterCard(tables.get(i), gpaHistory, i, tables.size()));
        }
        wrapper.getChildren().add(semestersList);

        ScrollPane mainScroll = new ScrollPane(wrapper);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(mainScroll, Priority.ALWAYS);
        return new VBox(mainScroll);
    }

    private VBox buildCgpaCardWithStanding(String header, String val, String standingVal, String standingDesc, String tooltipText, String icon) {
        VBox card = new VBox(4);
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-background-radius: 14;" +
            "-fx-border-radius: 14;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 18;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 70, 67, 0.02), 6, 0, 0, 3);"
        );
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMinWidth(160);
        
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18px;");
        Label titleLbl = new Label(header.toUpperCase());
        titleLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted; -fx-letter-spacing: 0.5px;");
        topRow.getChildren().addAll(iconLbl, titleLbl);
        
        Label valLbl = new Label(val);
        valLbl.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: -color-accent;");
        VBox.setMargin(valLbl, new Insets(4, 0, 4, 0));
        
        Label badge = new Label();
        String lowerStanding = standingVal.toLowerCase();
        if (lowerStanding.contains("excellent")) {
            badge.setText("🟢 " + standingVal);
            badge.getStyleClass().add("badge-success");
        } else if (lowerStanding.contains("good")) {
            badge.setText("💠 " + standingVal);
            badge.getStyleClass().add("badge-teal");
        } else if (lowerStanding.contains("satisfactory")) {
            badge.setText("🔵 " + standingVal);
            badge.getStyleClass().add("badge-info");
        } else if (lowerStanding.contains("warning")) {
            badge.setText("🟠 " + standingVal);
            badge.getStyleClass().add("badge-warning");
        } else if (lowerStanding.contains("probation")) {
            badge.setText("🔴 " + standingVal);
            badge.getStyleClass().add("badge-danger");
        } else {
            badge.setText("⚪ " + standingVal);
            badge.getStyleClass().add("badge-muted");
        }
        
        badge.setWrapText(true);
        badge.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        
        if (tooltipText != null && !tooltipText.isEmpty()) {
            javafx.scene.control.Tooltip tt = new javafx.scene.control.Tooltip(tooltipText);
            tt.setShowDelay(javafx.util.Duration.millis(300));
            badge.setTooltip(tt);
        }
        
        Label descLbl = new Label(standingDesc);
        descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
        descLbl.setWrapText(true);
        descLbl.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        VBox.setMargin(descLbl, new Insets(4, 0, 0, 0));
        
        card.getChildren().addAll(topRow, valLbl, badge, descLbl);
        
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
                "-fx-padding: 18;" +
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
                "-fx-padding: 18;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0, 70, 67, 0.02), 6, 0, 0, 3);"
            );
        });
        
        return card;
    }

    private VBox buildCompactStat(String header, String val, String sub, String icon) {
        VBox card = new VBox(4);
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-background-radius: 14;" +
            "-fx-border-radius: 14;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 18;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 70, 67, 0.02), 6, 0, 0, 3);"
        );
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMinWidth(160);
        
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18px;");
        Label titleLbl = new Label(header.toUpperCase());
        titleLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted; -fx-letter-spacing: 0.5px;");
        topRow.getChildren().addAll(iconLbl, titleLbl);
        
        Label valLbl = new Label(val);
        valLbl.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: -color-accent;");
        valLbl.setWrapText(true);
        valLbl.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        VBox.setMargin(valLbl, new Insets(4, 0, 2, 0));
        
        Label descLbl = new Label(sub);
        descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
        descLbl.setWrapText(true);
        descLbl.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        
        card.getChildren().addAll(topRow, valLbl, descLbl);
        
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
                "-fx-padding: 18;" +
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
                "-fx-padding: 18;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0, 70, 67, 0.02), 6, 0, 0, 3);"
            );
        });
        
        return card;
    }

    private VBox buildActionCard(String header, String desc, Button button, String icon) {
        VBox card = new VBox(6);
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-background-radius: 14;" +
            "-fx-border-radius: 14;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 18;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 70, 67, 0.02), 6, 0, 0, 3);"
        );
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMinWidth(170);
        
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18px;");
        Label titleLbl = new Label(header.toUpperCase());
        titleLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted;");
        topRow.getChildren().addAll(iconLbl, titleLbl);
        
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
        descLbl.setWrapText(true);
        VBox.setVgrow(descLbl, Priority.ALWAYS);
        
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("accent-button");
        button.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 8 12; -fx-background-radius: 6;");
        button.setCursor(javafx.scene.Cursor.HAND);
        
        card.getChildren().addAll(topRow, descLbl, button);
        
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
                "-fx-padding: 18;" +
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
                "-fx-padding: 18;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0, 70, 67, 0.02), 6, 0, 0, 3);"
            );
        });
        
        return card;
    }

    private VBox buildTrendChartCard(List<GpaHistoryData> gpaHistory) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: -color-border; -fx-border-width: 1; -fx-padding: 18;");
        
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("📈 Academic GPA Progression Trend");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -color-text-main;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_LEFT);
        
        HBox sgpaItem = new HBox(6);
        sgpaItem.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.shape.Circle sgpaDot = new javafx.scene.shape.Circle(4);
        sgpaDot.setStyle("-fx-fill: #f59e0b;");
        Label sgpaLbl = new Label("Semester GPA (SGPA)");
        sgpaLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500;");
        sgpaItem.getChildren().addAll(sgpaDot, sgpaLbl);
        
        HBox cgpaItem = new HBox(6);
        cgpaItem.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.shape.Circle cgpaDot = new javafx.scene.shape.Circle(4);
        cgpaDot.setStyle("-fx-fill: -color-accent;");
        Label cgpaLbl = new Label("Cumulative GPA (CGPA)");
        cgpaLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500;");
        cgpaItem.getChildren().addAll(cgpaDot, cgpaLbl);
        
        legend.getChildren().addAll(sgpaItem, cgpaItem);
        headerRow.getChildren().addAll(title, spacer, legend);
        
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Semesters");
        
        // Auto-scale lower bound logic to resolve flat line squeezing near the top
        double minVal = 4.0;
        boolean hasValues = false;
        for (GpaHistoryData g : gpaHistory) {
            if (g.cgpa() > 0 && g.cgpa() <= 4.0) { minVal = Math.min(minVal, g.cgpa()); hasValues = true; }
            if (g.sgpa() > 0 && g.sgpa() <= 4.0) { minVal = Math.min(minVal, g.sgpa()); hasValues = true; }
        }
        double lowerBound = 0.0;
        if (hasValues) {
            if (minVal >= 3.5) {
                lowerBound = 3.0;
            } else if (minVal >= 3.0) {
                lowerBound = 2.5;
            } else if (minVal >= 2.5) {
                lowerBound = 2.0;
            } else if (minVal >= 2.0) {
                lowerBound = 1.5;
            } else if (minVal >= 1.0) {
                lowerBound = 0.5;
            }
        }
        
        NumberAxis yAxis = new NumberAxis(lowerBound, 4.0, 0.5);
        yAxis.setLabel("GPA Scale");
        
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(true);
        lineChart.setLegendVisible(false);
        lineChart.setPrefHeight(145);
        lineChart.setMinHeight(145);
        lineChart.setMaxHeight(145);
        lineChart.setStyle("-fx-background-color: transparent;");
        
        XYChart.Series<String, Number> cgpaSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> sgpaSeries = new XYChart.Series<>();
        
        // Use chronological order directly (oldest first on left, newest on right)
        List<GpaHistoryData> chronological = new ArrayList<>(gpaHistory);
        
        for (GpaHistoryData g : chronological) {
            String sem = cleanSemesterTitle(g.semesterTitle());
            if (sem.length() > 18) {
                sem = sem.substring(0, 15) + "...";
            }
            if (g.cgpa() > 0 && g.cgpa() <= 4.0) {
                cgpaSeries.getData().add(new XYChart.Data<>(sem, g.cgpa()));
            }
            if (g.sgpa() > 0 && g.sgpa() <= 4.0) {
                sgpaSeries.getData().add(new XYChart.Data<>(sem, g.sgpa()));
            }
        }
        
        lineChart.getData().addAll(sgpaSeries, cgpaSeries);
        
        Platform.runLater(() -> {
            Node lineCgpa = cgpaSeries.getNode();
            if (lineCgpa != null) {
                lineCgpa.setStyle("-fx-stroke: -color-accent; -fx-stroke-width: 3.5px;");
            }
            Node lineSgpa = sgpaSeries.getNode();
            if (lineSgpa != null) {
                lineSgpa.setStyle("-fx-stroke: #f59e0b; -fx-stroke-width: 2.2px; -fx-stroke-dash-array: 4 4;");
            }

            for (XYChart.Data<String, Number> data : cgpaSeries.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    node.setStyle("-fx-background-color: -color-accent, -color-bg-card; -fx-background-insets: 0, 2; -fx-background-radius: 5px; -fx-padding: 5px;");
                    Tooltip tooltip = new Tooltip("CGPA: " + String.format("%.2f", data.getYValue().doubleValue()) + "\nSemester: " + data.getXValue());
                    tooltip.setShowDelay(javafx.util.Duration.millis(50));
                    Tooltip.install(node, tooltip);
                    node.setOnMouseEntered(e -> {
                        node.setScaleX(1.4);
                        node.setScaleY(1.4);
                        node.setCursor(javafx.scene.Cursor.HAND);
                    });
                    node.setOnMouseExited(e -> {
                        node.setScaleX(1.0);
                        node.setScaleY(1.0);
                        node.setCursor(javafx.scene.Cursor.DEFAULT);
                    });
                }
            }

            for (XYChart.Data<String, Number> data : sgpaSeries.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    node.setStyle("-fx-background-color: #f59e0b, -color-bg-card; -fx-background-insets: 0, 2; -fx-background-radius: 5px; -fx-padding: 5px;");
                    Tooltip tooltip = new Tooltip("SGPA: " + String.format("%.2f", data.getYValue().doubleValue()) + "\nSemester: " + data.getXValue());
                    tooltip.setShowDelay(javafx.util.Duration.millis(50));
                    Tooltip.install(node, tooltip);
                    node.setOnMouseEntered(e -> {
                        node.setScaleX(1.4);
                        node.setScaleY(1.4);
                        node.setCursor(javafx.scene.Cursor.HAND);
                    });
                    node.setOnMouseExited(e -> {
                        node.setScaleX(1.0);
                        node.setScaleY(1.0);
                        node.setCursor(javafx.scene.Cursor.DEFAULT);
                    });
                }
            }
        });
        
        card.getChildren().addAll(headerRow, lineChart);
        return card;
    }

    private String cleanSemesterTitle(String raw) {
        if (raw == null) return "";
        String clean = raw.trim();
        if (clean.contains(":")) {
            clean = clean.substring(clean.indexOf(":") + 1).trim();
        }
        return clean.replaceAll("\\s+", " ");
    }

    private VBox buildSemesterCard(SemesterResultTable table, List<GpaHistoryData> gpaHistory, int tableIndex, int totalTables) {
        VBox card = new VBox(12);
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-background-radius: 14;" +
            "-fx-border-radius: 14;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 16;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 70, 67, 0.01), 4, 0, 0, 2);"
        );
        
        double sgpa = -1.0;
        double credits = 0.0;
        
        // Resolve matching history GPA using cleaned semester titles
        String cleanTableTitle = cleanSemesterTitle(table.title());
        for (GpaHistoryData g : gpaHistory) {
            String cleanHistoryTitle = cleanSemesterTitle(g.semesterTitle());
            if (cleanHistoryTitle.equalsIgnoreCase(cleanTableTitle)) {
                sgpa = g.sgpa();
                credits = g.creditHours();
                break;
            }
        }
        
        // Fallback: If GPA not found or N/A (<= 0), calculate it dynamically from course rows
        if (sgpa <= 0) {
            sgpa = calculateSemesterGpa(table);
        }
        
        int coursesCount = 0;
        for (List<String> row : table.data()) {
            String joined = String.join(" ", row).toUpperCase();
            if (!joined.contains("SGPA") && !joined.contains("CGPA") && !joined.contains("GPA")) {
                coursesCount++;
            }
        }
        
        boolean isActive = isSemesterActiveOrIncomplete(table, gpaHistory, tableIndex, totalTables);
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("📅 " + table.title());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        
        String sgpaStr = sgpa > 0 ? String.format("%.2f", sgpa) : "N/A";
        String creditsStr = credits > 0 ? String.format("%.1f", credits) : String.valueOf(coursesCount * 3);
        
        Label statusLbl;
        Label completedBadge = null;
        if (isActive) {
            boolean hasGrades = hasPublishedGrades(table);
            if (hasGrades) {
                statusLbl = new Label("Result Pending");
                statusLbl.getStyleClass().add("badge-muted");
            } else {
                statusLbl = new Label("Semester In Progress");
                statusLbl.getStyleClass().add("badge-warning");
            }
        } else {
            statusLbl = new Label("GPA: " + sgpaStr);
            statusLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1967D2; -fx-background-color: #E8F0FE; -fx-padding: 3 8; -fx-background-radius: 6;");
            
            completedBadge = new Label("Completed");
            completedBadge.getStyleClass().add("badge-success");
        }
        
        Label credsLbl = new Label("Credits: " + creditsStr);
        credsLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #0f766e; -fx-background-color: #ccfbf1; -fx-padding: 3 8; -fx-background-radius: 6;");
        
        Label coursesLbl = new Label("Courses: " + coursesCount);
        coursesLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted; -fx-background-color: -color-bg-main; -fx-padding: 3 8; -fx-background-radius: 6;");
        
        if (completedBadge != null) {
            statsRow.getChildren().add(completedBadge);
        }
        statsRow.getChildren().addAll(statusLbl, credsLbl, coursesLbl);
        
        Button toggleBtn = new Button("Expand Details  ▼");
        toggleBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-accent; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8;");
        
        header.getChildren().addAll(title, spacer, statsRow, toggleBtn);
        
        VBox expandableDetails = new VBox(8);
        expandableDetails.setVisible(false);
        expandableDetails.setManaged(false);
        
        VBox courseList = buildCourseList(table, isActive);
        expandableDetails.getChildren().add(courseList);
        
        toggleBtn.setOnAction(e -> {
            boolean expanded = expandableDetails.isManaged();
            if (expanded) {
                expandableDetails.setVisible(false);
                expandableDetails.setManaged(false);
                toggleBtn.setText("Expand Details  ▼");
            } else {
                expandableDetails.setVisible(true);
                expandableDetails.setManaged(true);
                toggleBtn.setText("Hide Details  ▲");
            }
        });
        
        header.setCursor(javafx.scene.Cursor.HAND);
        header.setOnMouseClicked(e -> {
            if (e.getTarget() != toggleBtn) {
                toggleBtn.fire();
            }
        });
        
        card.getChildren().addAll(header, expandableDetails);
        return card;
    }

    private VBox buildCourseList(SemesterResultTable table, boolean isActive) {
        VBox list = new VBox(0);
        list.setStyle("-fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-background-color: -color-bg-card; -fx-overflow: hidden;");
        
        int codeIdx = -1, titleIdx = -1, creditIdx = -1, marksIdx = -1, gradeIdx = -1, gpIdx = -1;
        for (int i = 0; i < table.headers().size(); i++) {
            String h = table.headers().get(i).toLowerCase();
            if (h.contains("code")) codeIdx = i;
            else if (h.contains("title") || h.contains("subject")) titleIdx = i;
            else if (h.contains("credit") || h.contains("cr") || h.contains("hrs")) creditIdx = i;
            else if (h.contains("marks") || h.contains("obt") || h.contains("total")) marksIdx = i;
            else if (h.contains("grade") || h.equals("lg")) gradeIdx = i;
            else if (h.contains("gp") || h.contains("point")) gpIdx = i;
        }

        // Table Header
        HBox headerRow = new HBox(12);
        headerRow.setPadding(new Insets(10, 16, 10, 16));
        headerRow.setStyle("-fx-background-color: -color-bg-main; -fx-border-color: -color-border; -fx-border-width: 0 0 1 0;");
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label codeH = new Label("CODE");
        codeH.setPrefWidth(90);
        codeH.setMinWidth(90);
        codeH.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");

        Label titleH = new Label("COURSE TITLE");
        titleH.setPrefWidth(250);
        titleH.setMinWidth(200);
        HBox.setHgrow(titleH, Priority.ALWAYS);
        titleH.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");

        Label creditH = new Label("CREDITS");
        creditH.setPrefWidth(70);
        creditH.setMinWidth(70);
        creditH.setAlignment(Pos.CENTER);
        creditH.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");

        Label marksH = new Label("MARKS");
        marksH.setPrefWidth(100);
        marksH.setMinWidth(100);
        marksH.setAlignment(Pos.CENTER);
        marksH.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");

        Label gradeH = new Label("GRADE");
        gradeH.setPrefWidth(90);
        gradeH.setMinWidth(90);
        gradeH.setAlignment(Pos.CENTER);
        gradeH.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");

        Label gpH = new Label("GP");
        gpH.setPrefWidth(60);
        gpH.setMinWidth(60);
        gpH.setAlignment(Pos.CENTER);
        gpH.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");

        headerRow.getChildren().addAll(codeH, titleH, creditH, marksH, gradeH, gpH);
        list.getChildren().add(headerRow);

        // Course Rows
        for (int r = 0; r < table.data().size(); r++) {
            List<String> row = table.data().get(r);
            String joinedRowText = String.join(" ", row).toUpperCase();

            // Detect sessional/gpa summary row and format differently
            if (joinedRowText.contains("SGPA") || joinedRowText.contains("CGPA") || joinedRowText.contains("GPA") || joinedRowText.contains("CREDIT HOURS")) {
                HBox footerBox = new HBox(12);
                footerBox.setPadding(new Insets(12, 16, 12, 16));
                footerBox.setAlignment(Pos.CENTER_RIGHT);
                footerBox.setStyle("-fx-background-color: -color-bg-main; -fx-border-color: -color-border; -fx-border-width: 1 0 0 0;");
                
                Label summaryText = new Label(String.join("   •   ", row).trim().replaceAll("\\s+", " "));
                summaryText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -color-accent;");
                footerBox.getChildren().add(summaryText);
                list.getChildren().add(footerBox);
                continue;
            }

            HBox rowBox = new HBox(12);
            rowBox.setPadding(new Insets(12, 16, 12, 16));
            rowBox.setAlignment(Pos.CENTER_LEFT);
            
            rowBox.getStyleClass().add(r % 2 == 1 ? "result-row-odd" : "result-row-even");

            String codeVal = codeIdx >= 0 && codeIdx < row.size() ? row.get(codeIdx).trim() : "-";
            Label codeLabel = new Label(codeVal);
            codeLabel.setPrefWidth(90);
            codeLabel.setMinWidth(90);
            codeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");

            String titleVal = titleIdx >= 0 && titleIdx < row.size() ? row.get(titleIdx).trim() : "-";
            Label titleLabel = new Label(titleVal);
            titleLabel.setPrefWidth(250);
            titleLabel.setMinWidth(200);
            HBox.setHgrow(titleLabel, Priority.ALWAYS);
            titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -color-text-main;");
            titleLabel.setWrapText(true);

            String creditVal = creditIdx >= 0 && creditIdx < row.size() ? row.get(creditIdx).trim() : "-";
            Label creditLabel = new Label(creditVal);
            creditLabel.setPrefWidth(70);
            creditLabel.setMinWidth(70);
            creditLabel.setAlignment(Pos.CENTER);
            creditLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-main;");

            String marksVal = marksIdx >= 0 && marksIdx < row.size() ? row.get(marksIdx).trim() : "";
            String gradeVal = gradeIdx >= 0 && gradeIdx < row.size() ? row.get(gradeIdx).trim() : "";
            String gpVal = gpIdx >= 0 && gpIdx < row.size() ? row.get(gpIdx).trim() : "";

            boolean isPending = false;
            if (isActive) {
                boolean marksEmptyOrZero = marksVal.isEmpty() || marksVal.equals("0") || marksVal.equals("0.0") || marksVal.equalsIgnoreCase("null");
                boolean gradeEmptyOrDash = gradeVal.isEmpty() || gradeVal.equals("-") || gradeVal.equalsIgnoreCase("n/a") || gradeVal.equalsIgnoreCase("null");
                if (marksEmptyOrZero || gradeEmptyOrDash) {
                    isPending = true;
                }
            }

            String displayMarks = isPending ? "Result Pending" : (marksVal.isEmpty() ? "-" : marksVal);
            String displayGrade = isPending ? "Pending" : (gradeVal.isEmpty() ? "-" : gradeVal);
            String displayGp = isPending ? "-" : (gpVal.isEmpty() ? "-" : gpVal);

            Label marksLabel = new Label(displayMarks);
            marksLabel.setPrefWidth(100);
            marksLabel.setMinWidth(100);
            marksLabel.setAlignment(Pos.CENTER);
            if (isPending) {
                marksLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
            } else {
                marksLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-main;");
            }

            Label gradeBadge = new Label(displayGrade);
            gradeBadge.setPrefWidth(75);
            gradeBadge.setAlignment(Pos.CENTER);
            
            String badgeStyle = "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 8;";
            if (isPending) {
                badgeStyle += "-fx-background-color: #f1f5f9; -fx-text-fill: #475467;";
            } else {
                String gUpper = displayGrade.toUpperCase();
                if (gUpper.startsWith("A")) {
                    badgeStyle += "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;";
                } else if (gUpper.startsWith("B")) {
                    badgeStyle += "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;";
                } else if (gUpper.startsWith("C")) {
                    badgeStyle += "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;";
                } else if (gUpper.startsWith("D")) {
                    badgeStyle += "-fx-background-color: #ffedd5; -fx-text-fill: #9a3412;";
                } else if (gUpper.equals("F") || gUpper.equals("FA")) {
                    badgeStyle += "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
                } else {
                    badgeStyle += "-fx-background-color: #f3f4f6; -fx-text-fill: #374151;";
                }
            }
            gradeBadge.setStyle(badgeStyle);

            StackPane gradeWrapper = new StackPane(gradeBadge);
            gradeWrapper.setPrefWidth(90);
            gradeWrapper.setMinWidth(90);
            gradeWrapper.setAlignment(Pos.CENTER);

            Label gpLabel = new Label(displayGp);
            gpLabel.setPrefWidth(60);
            gpLabel.setMinWidth(60);
            gpLabel.setAlignment(Pos.CENTER);
            gpLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");

            rowBox.getChildren().addAll(codeLabel, titleLabel, creditLabel, marksLabel, gradeWrapper, gpLabel);

            // Highlight check
            String highlight = context.getPendingSearchHighlight();
            if (highlight != null && !highlight.isBlank()) {
                boolean matches = codeVal.toLowerCase().contains(highlight.toLowerCase()) ||
                                  titleVal.toLowerCase().contains(highlight.toLowerCase());
                if (matches) {
                    rowBox.setStyle("-fx-background-color: #fef9c3; -fx-border-color: #eab308; -fx-border-width: 1; -fx-border-radius: 4;");
                }
            }


            list.getChildren().add(rowBox);
        }

        return list;
    }

    private boolean hasPublishedGrades(SemesterResultTable table) {
        int gradeIdx = -1, marksIdx = -1;
        for (int i = 0; i < table.headers().size(); i++) {
            String h = table.headers().get(i).toLowerCase();
            if (h.contains("grade") || h.equals("lg")) gradeIdx = i;
            else if (h.contains("marks") || h.contains("obt")) marksIdx = i;
        }
        
        for (List<String> row : table.data()) {
            String joined = String.join(" ", row).toUpperCase();
            if (joined.contains("SGPA") || joined.contains("CGPA") || joined.contains("GPA") || joined.contains("CREDIT HOURS")) continue;
            
            boolean marksEmptyOrZero = true;
            boolean gradeEmptyOrDash = true;
            
            if (marksIdx != -1 && marksIdx < row.size()) {
                String m = row.get(marksIdx).trim().toLowerCase();
                if (!m.isEmpty() && !m.equals("0") && !m.equals("0.0") && !m.equalsIgnoreCase("null") && !m.contains("pending")) {
                    marksEmptyOrZero = false;
                }
            }
            if (gradeIdx != -1 && gradeIdx < row.size()) {
                String g = row.get(gradeIdx).trim().toLowerCase();
                if (!g.isEmpty() && !g.equals("-") && !g.equalsIgnoreCase("n/a") && !g.equalsIgnoreCase("null") && !g.contains("pending")) {
                    gradeEmptyOrDash = false;
                }
            }
            
            if (!marksEmptyOrZero || !gradeEmptyOrDash) {
                return true;
            }
        }
        return false;
    }

    private double calculateSemesterGpa(SemesterResultTable table) {
        int creditIdx = -1, gpIdx = -1, gradeIdx = -1;
        for (int i = 0; i < table.headers().size(); i++) {
            String h = table.headers().get(i).toLowerCase();
            if (h.contains("credit") || h.contains("cr") || h.contains("hrs")) creditIdx = i;
            else if (h.contains("gp") || h.contains("point")) gpIdx = i;
            else if (h.contains("grade") || h.equals("lg")) gradeIdx = i;
        }
        if (creditIdx == -1 || gpIdx == -1) return -1.0;
        
        double totalQualityPoints = 0.0;
        double totalCredits = 0.0;
        
        for (List<String> row : table.data()) {
            String joined = String.join(" ", row).toUpperCase();
            if (joined.contains("SGPA") || joined.contains("CGPA") || joined.contains("GPA")) continue;
            
            if (creditIdx < row.size() && gpIdx < row.size()) {
                try {
                    double credit = Double.parseDouble(row.get(creditIdx).trim());
                    String gpStr = row.get(gpIdx).trim();
                    
                    boolean isExcluded = false;
                    if (gradeIdx != -1 && gradeIdx < row.size()) {
                        String g = row.get(gradeIdx).trim().toUpperCase();
                        if (g.equals("W") || g.equals("I") || g.isEmpty() || g.equals("-") || g.contains("PENDING")) {
                            isExcluded = true;
                        }
                    }
                    if (gpStr.isEmpty() || gpStr.equals("-") || gpStr.equalsIgnoreCase("null")) {
                        isExcluded = true;
                    }
                    
                    if (!isExcluded && credit > 0) {
                        double gp = Double.parseDouble(gpStr);
                        totalQualityPoints += credit * gp;
                        totalCredits += credit;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        if (totalCredits > 0) {
            return totalQualityPoints / totalCredits;
        }
        return -1.0;
    }

    private boolean isSemesterActiveOrIncomplete(SemesterResultTable table, List<GpaHistoryData> gpaHistory, int tableIndex, int totalTables) {
        String cleanTableTitle = cleanSemesterTitle(table.title());
        double sgpa = -1.0;
        boolean foundInHistory = false;
        
        for (GpaHistoryData g : gpaHistory) {
            String cleanHistoryTitle = cleanSemesterTitle(g.semesterTitle());
            if (cleanHistoryTitle.equalsIgnoreCase(cleanTableTitle)) {
                sgpa = g.sgpa();
                foundInHistory = true;
                break;
            }
        }
        
        if (foundInHistory && sgpa <= 0) {
            return true;
        }

        double calculatedGpa = calculateSemesterGpa(table);
        if (calculatedGpa <= 0) {
            return true;
        }
        
        int gradeIdx = -1, marksIdx = -1;
        for (int i = 0; i < table.headers().size(); i++) {
            String h = table.headers().get(i).toLowerCase();
            if (h.contains("grade") || h.equals("lg")) gradeIdx = i;
            else if (h.contains("marks") || h.contains("obt")) marksIdx = i;
        }
        
        boolean hasPending = false;
        int courseRowsCount = 0;
        for (List<String> row : table.data()) {
            String joined = String.join(" ", row).toUpperCase();
            if (joined.contains("SGPA") || joined.contains("CGPA") || joined.contains("GPA")) continue;
            courseRowsCount++;
            
            if (gradeIdx != -1 && gradeIdx < row.size()) {
                String g = row.get(gradeIdx).trim().toLowerCase();
                if (g.isEmpty() || g.equals("-") || g.equals("n/a") || g.equals("0") || g.equals("0.0") || g.equals("null") || g.contains("pending")) {
                    hasPending = true;
                }
            }
            if (marksIdx != -1 && marksIdx < row.size()) {
                String m = row.get(marksIdx).trim().toLowerCase();
                if (m.isEmpty() || m.equals("-") || m.equals("n/a") || m.equals("0") || m.equals("0.0") || m.equals("null") || m.contains("pending")) {
                    hasPending = true;
                }
            }
        }
        
        if (hasPending && (tableIndex == totalTables - 1 || courseRowsCount == 0)) {
            return true;
        }

        return false;
    }

    private double calculateTotalEarnedCredits(List<SemesterResultTable> tables) {
        double total = 0.0;
        Set<String> passedCourseCodes = new HashSet<>();
        Map<String, Double> courseCreditsMap = new HashMap<>();

        for (SemesterResultTable t : tables) {
            int codeIdx = -1, creditIdx = -1, gradeIdx = -1;
            for (int i = 0; i < t.headers().size(); i++) {
                String h = t.headers().get(i).toLowerCase();
                if (h.contains("code") || h.contains("no")) codeIdx = i;
                else if (h.contains("credit") || h.contains("cr") || h.contains("hrs")) creditIdx = i;
                else if (h.contains("grade") || h.equals("lg")) gradeIdx = i;
            }
            if (creditIdx != -1) {
                for (List<String> row : t.data()) {
                    String joined = String.join(" ", row).toUpperCase();
                    if (joined.contains("SGPA") || joined.contains("CGPA") || joined.contains("GPA") || joined.contains("CREDIT HOURS")) continue;
                    
                    if (creditIdx < row.size()) {
                        String codeVal = codeIdx != -1 && codeIdx < row.size() ? row.get(codeIdx).trim().toUpperCase() : "";
                        String cleanCode = codeVal.replaceAll("\\s+", "");

                        try {
                            double credits = Double.parseDouble(row.get(creditIdx).trim());
                            boolean passed = true;

                            if (joined.contains("NON CREDIT") || joined.contains("NON-CREDIT")) {
                                passed = false;
                            }

                            if (gradeIdx != -1 && gradeIdx < row.size()) {
                                String g = row.get(gradeIdx).trim().toUpperCase();
                                if (g.equals("F") || g.equals("W") || g.equals("I") || g.equals("FA") || g.isEmpty() || g.equals("-") || g.contains("PENDING")) {
                                    passed = false;
                                }
                            }

                            if (!cleanCode.isEmpty()) {
                                courseCreditsMap.put(cleanCode, credits);
                                if (passed) {
                                    passedCourseCodes.add(cleanCode);
                                }
                            } else {
                                if (passed) {
                                    total += credits;
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        for (String cleanCode : passedCourseCodes) {
            Double credits = courseCreditsMap.get(cleanCode);
            if (credits != null) {
                total += credits;
            }
        }

        return total;
    }


    private int countCompletedSemesters(List<SemesterResultTable> tables, List<GpaHistoryData> gpaHistory) {
        int completed = 0;
        for (int i = 0; i < tables.size(); i++) {
            if (!isSemesterActiveOrIncomplete(tables.get(i), gpaHistory, i, tables.size())) {
                completed++;
            }
        }
        if (completed == 0 && !tables.isEmpty()) {
            return Math.max(1, tables.size() - 1);
        }
        return completed;
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            root.getChildren().clear();
            root.getChildren().add(buildErrorView(title, message));
        });
    }

    private VBox buildErrorView(String title, String message) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        VBox.setVgrow(box, Priority.ALWAYS);

        Label icon = new Label("⚠");
        icon.setStyle("-fx-font-size:28px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-muted;-fx-text-alignment:center;");
        msgLabel.setWrapText(true);

        box.getChildren().addAll(icon, titleLabel, msgLabel);
        return box;
    }

    private void exportResultPdf(List<SemesterResultTable> tables, List<GpaHistoryData> gpaHistory) {
        if (tables == null || tables.isEmpty()) {
            showExportError("Export Failed", "No result data available to export.");
            return;
        }

        List<SemesterResultTable> completedTables = new ArrayList<>();
        for (int i = 0; i < tables.size(); i++) {
            SemesterResultTable table = tables.get(i);
            if (!isSemesterActiveOrIncomplete(table, gpaHistory, i, tables.size())) {
                List<List<String>> cleanData = new ArrayList<>();
                for (List<String> row : table.data()) {
                    String joined = String.join(" ", row).toUpperCase();
                    if (!joined.contains("SGPA") && !joined.contains("CGPA") && !joined.contains("GPA") && !joined.contains("CREDIT HOURS")) {
                        cleanData.add(new ArrayList<>(row));
                    }
                }
                
                double sgpa = -1.0;
                double cgpa = -1.0;
                String cleanTableTitle = cleanSemesterTitle(table.title());
                if (gpaHistory != null) {
                    for (GpaHistoryData g : gpaHistory) {
                        String cleanHistoryTitle = cleanSemesterTitle(g.semesterTitle());
                        if (cleanHistoryTitle.equalsIgnoreCase(cleanTableTitle)) {
                            sgpa = g.sgpa();
                            cgpa = g.cgpa();
                            break;
                        }
                    }
                }
                if (sgpa <= 0) {
                    sgpa = calculateSemesterGpa(table);
                }
                
                String sgpaStr = sgpa > 0 ? String.format("%.2f", sgpa) : "N/A";
                String cgpaStr = cgpa > 0 ? String.format("%.2f", cgpa) : "N/A";
                
                List<String> gpaRow = new ArrayList<>();
                gpaRow.add("SGPA: " + sgpaStr + "     |     CGPA: " + cgpaStr);
                for (int k = 1; k < table.headers().size(); k++) {
                    gpaRow.add("");
                }
                cleanData.add(gpaRow);
                
                completedTables.add(new SemesterResultTable(table.title(), table.headers(), cleanData));
            }
        }

        if (completedTables.isEmpty()) {
            showExportError("Export Failed", "No completed semester result data available to export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Result Card PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        String reg = context.getSessionRegistration() == null ? "Result_Card" : "Result_Card_" + context.getSessionRegistration();
        chooser.setInitialFileName(reg + ".pdf");
        File file = chooser.showSaveDialog(context.stage());
        if (file == null) {
            return;
        }

        try {
            String studentName = context.portalRepository().getCurrentStudentName();
            String regNo = context.getSessionRegistration();
            new PdfExportService().exportResultCard(studentName, regNo, completedTables, file);
        } catch (Exception ex) {
            showExportError("Export Failed", ex.getMessage());
        }
    }

    private void showExportError(String title, String message) {
        Platform.runLater(() -> {
            context.notificationService().showError(title, message);
        });
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
}
