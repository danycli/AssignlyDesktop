package com.assignly.view;

import com.assignly.service.PdfExportService;
import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FeeTabView {

    private final VBox root = new VBox();
    private final AppContext context;
    private StackPane contentPane;
    private HBox tabBar;
    private String activeTab = "";
    private Button exportBtn;
    private List<FeeHistoryTable> latestHistoryTables = List.of();
    
    // UI state & filters
    private String historySearchQuery = "";
    private String historySemesterFilter = "All Terms";
    private String historyTypeFilter = "All Transactions";
    private final GridPane dashboardGrid = new GridPane();
    private VBox cardTotalFees;
    private VBox cardTotalPaid;
    private VBox cardOutstanding;
    private VBox cardScholarship;
    private VBox cardActions;
    private double cachedFees = 0;
    private double cachedPaid = 0;
    private double cachedOutstanding = 0;
    private double cachedScholarship = 0;
    private final java.util.Map<String, Boolean> expandedStates = new java.util.HashMap<>();
    private List<ChallanInfo> latestChallans = new ArrayList<>();
    private boolean isDataLoaded = false;

    // Data structures
    private record ChallanInfo(String description, String postbackTarget, String postbackArgument) {}
    public record FeeHistoryTable(String title, List<String> headers, List<List<String>> data) {}

    public FeeTabView(AppContext context) {
        this.context = context;
        buildShell();
        loadTab("fee_challans");
    }

    public FeeTabView(AppContext context, String initialTab) {
        this.context = context;
        buildShell();
        loadTab(initialTab);
    }

    public VBox getRoot() { return root; }

    private void buildShell() {
        root.setFillWidth(true);
        root.setStyle("-fx-background-color: -color-bg-main;");
        
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(16, 28, 8, 28));

        VBox titleBlock = new VBox(2);
        Label heading = new Label("Fee Portal");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        Label subHeading = new Label("Fee Information & Billing History");
        subHeading.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500;");
        titleBlock.getChildren().addAll(heading, subHeading);

        headerRow.getChildren().addAll(titleBlock);

        // Top Dashboard summary grid
        dashboardGrid.setHgap(16);
        dashboardGrid.setVgap(16);
        dashboardGrid.setPadding(new Insets(12, 28, 12, 28));
        dashboardGrid.setMaxWidth(Double.MAX_VALUE);

        root.widthProperty().addListener((obs, oldW, newW) -> {
            if (isDataLoaded) {
                rearrangeDashboardRow(newW.doubleValue());
            }
        });

        tabBar = new HBox(4);
        tabBar.setPadding(new Insets(8, 28, 8, 28));
        tabBar.getChildren().addAll(
            tabBtn("Fee Challans", "fee_challans"),
            tabBtn("Fee History", "fee_history")
        );

        contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        root.getChildren().addAll(headerRow, dashboardGrid, tabBar, contentPane);
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
        return on ? "-fx-background-color: -color-accent;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:600;-fx-background-radius:6;-fx-padding:6 14;-fx-cursor:hand;"
                  : "-fx-background-color: -color-bg-card;-fx-text-fill: -color-text-muted;-fx-font-size:11px;-fx-font-weight:500;-fx-background-radius:6;-fx-padding:6 14;-fx-border-color: -color-border;-fx-border-radius:6;-fx-border-width:1;-fx-cursor:hand;";
    }

    private void loadTab(String tabKey) {
        activeTab = tabKey;
        updateExportButtonState();

        for (var n : tabBar.getChildren()) {
            if (n instanceof Button b) {
                b.setStyle(tabStyle(tabKey.equals(b.getUserData())));
            }
        }

        if (!isDataLoaded) {
            loadAllFeeData(false);
        } else {
            renderCurrentTab();
        }
    }

    private void renderCurrentTab() {
        contentPane.getChildren().clear();
        Node tabNode = switch (activeTab) {
            case "fee_challans" -> buildChallansTabViewContent();
            case "fee_history" -> buildHistoryTabViewContent();
            default -> new Label("Unknown Tab");
        };
        
        ScrollPane sp = new ScrollPane(tabNode);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        contentPane.getChildren().add(sp);
    }

    private void showLoading(String msg) {
        Platform.runLater(() -> {
            contentPane.getChildren().clear();
            ScrollPane sp = new ScrollPane(com.assignly.util.ShimmerBuilder.buildFeeShimmer());
            sp.setFitToWidth(true);
            sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
            contentPane.getChildren().add(sp);
        });
    }

    private void exportFeeHistoryPdf() {
        if (latestHistoryTables == null || latestHistoryTables.isEmpty()) {
            showErrorAlert("Export Failed", "No fee history data available to export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Fee History PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        String reg = context.getSessionRegistration() == null ? "Fee_History" : "Fee_History_" + context.getSessionRegistration();
        chooser.setInitialFileName(reg + ".pdf");
        File file = chooser.showSaveDialog(context.stage());
        if (file == null) {
            return;
        }

        try {
            String studentName = context.portalRepository().getCurrentStudentName();
            String regNo = context.getSessionRegistration();
            new PdfExportService().exportFeeHistory(studentName, regNo, latestHistoryTables, file);
        } catch (Exception ex) {
            showErrorAlert("Export Failed", ex.getMessage());
        }
    }

    private void updateExportButtonState() {
        if (exportBtn == null) return;
        boolean enabled = "fee_history".equals(activeTab) && latestHistoryTables != null && !latestHistoryTables.isEmpty();
        exportBtn.setDisable(!enabled);
    }

    private VBox buildErrorState(String title, String message) {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40, 28, 40, 28));
        box.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:12;-fx-border-color: #ef4444;-fx-border-width:1;-fx-border-radius:12;-fx-max-width:550;");

        Label icon = new Label("⚠️");
        icon.setStyle("-fx-font-size:32px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill:#ef4444;-fx-font-size:16px;-fx-font-weight:bold;");

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-text-fill:-color-text-muted;-fx-font-size:12px;-fx-text-alignment:center;");
        msgLbl.setWrapText(true);

        box.getChildren().addAll(icon, titleLbl, msgLbl);
        
        VBox outer = new VBox(box);
        outer.setAlignment(Pos.CENTER);
        outer.setPadding(new Insets(40));
        return outer;
    }

    // =========================================================================
    // DYNAMIC DATA LOADING (FEES & CHALLANS BATCH)
    // =========================================================================

    private void loadAllFeeData(boolean forceRefresh) {
        showLoading("Loading student billing details...");
        new Thread(() -> {
            try {
                // 1. Fetch Fee History (Dues tables)
                String historyHtml = null;
                if (!forceRefresh) {
                    historyHtml = context.dataCacheService().getCachedHtml("FeeHistorySFMS.aspx").orElse(null);
                }
                if (historyHtml == null) {
                    historyHtml = context.fetchAndCacheHtml("FeeHistorySFMS.aspx");
                    if (historyHtml == null) {
                        historyHtml = context.dataCacheService().getCachedHtml("FeeHistorySFMS.aspx").orElse(null);
                    }
                }
                
                // 2. Fetch Fee Challans (Links)
                String challansHtml = null;
                if (!forceRefresh) {
                    challansHtml = context.dataCacheService().getCachedHtml("FeeChallans.aspx").orElse(null);
                }
                if (challansHtml == null) {
                    challansHtml = context.fetchAndCacheHtml("FeeChallans.aspx");
                    if (challansHtml == null) {
                        challansHtml = context.dataCacheService().getCachedHtml("FeeChallans.aspx").orElse(null);
                    }
                }
                
                if (historyHtml == null && challansHtml == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState("Unable to load billing data", "Failed to connect to the portal and no offline data available."));
                    });
                    return;
                }
                
                // Parse history tables
                List<FeeHistoryTable> historyTables = new ArrayList<>();
                if (historyHtml != null) {
                    Document doc = Jsoup.parse(historyHtml);
                    Elements allTables = doc.select("table");
                    int tableIndex = 0;
                    String[] expectedTitles = {
                        "Semester Fee Current",
                        "Boarding Fee Current",
                        "Other Miscellaneous Fee Current"
                    };
                    for (Element table : allTables) {
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
                }
                
                // Parse challans
                List<ChallanInfo> challans = new ArrayList<>();
                if (challansHtml != null) {
                    Document doc = Jsoup.parse(challansHtml);
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
                                if (description.isEmpty()) description = "Fee Challan";
                                
                                String href = link.attr("href");
                                int start = href.indexOf("'");
                                if (start != -1) {
                                    int end = href.indexOf("'", start + 1);
                                    if (end != -1) {
                                        String target = href.substring(start + 1, end);
                                        int argStart = href.indexOf("'", end + 1);
                                        String argument = "";
                                        if (argStart != -1) {
                                            int argEnd = href.indexOf("'", argStart + 1);
                                            if (argEnd != -1) argument = href.substring(argStart + 1, argEnd);
                                        }
                                        challans.add(new ChallanInfo(description, target, argument));
                                    }
                                }
                            }
                        }
                    }
                }
                
                List<FeeHistoryTable> finalTables = historyTables;
                List<ChallanInfo> finalChallans = challans;
                isDataLoaded = true;
                
                // Perform calculations
                double totalFees = 0;
                double totalPaid = 0;
                double totalOutstanding = 0;
                double totalScholarship = 0;
                
                for (FeeHistoryTable table : finalTables) {
                    for (List<String> row : table.data()) {
                        if (row.size() > 4) totalFees += parseDoubleClean(row.get(4));
                        if (row.size() > 5) {
                            double assistance = parseDoubleClean(row.get(5));
                            if (assistance < 0) {
                                totalScholarship += Math.abs(assistance);
                            }
                        }
                        if (row.size() > 7) totalPaid += parseDoubleClean(row.get(7));
                        if (row.size() > 9) {
                            double outstanding = parseDoubleClean(row.get(9));
                            totalOutstanding += outstanding;
                        }
                    }
                }
                
                final double fFees = totalFees;
                final double fPaid = totalPaid;
                final double fOutstanding = totalOutstanding;
                final double fScholarship = totalScholarship;
                
                Platform.runLater(() -> {
                    latestHistoryTables = finalTables;
                    latestChallans = finalChallans;
                    
                    // Create and lay out dashboard cards
                    createDashboardCards(fFees, fPaid, fOutstanding, fScholarship);
                    rearrangeDashboardRow(root.getWidth());
                    updateExportButtonState();
                    
                    // Render tab content
                    renderCurrentTab();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState("Error Loading Billing Details", e.getMessage()));
                });
            }
        }).start();
    }

    private void createDashboardCards(double totalFees, double totalPaid, double outstanding, double scholarship) {
        cachedFees = totalFees;
        cachedPaid = totalPaid;
        cachedOutstanding = outstanding;
        cachedScholarship = scholarship;
        
        cardTotalFees = buildMetricCard("TOTAL FEES", formatMoney(totalFees), "-color-text-main", "💳");
        cardTotalPaid = buildMetricCard("TOTAL PAID", formatMoney(totalPaid), "#22c55e", "✅");
        
        if (outstanding <= 0) {
            cardOutstanding = buildMetricCard("OUTSTANDING BALANCE", "Cleared", "#22c55e", "🎉");
        } else {
            cardOutstanding = buildMetricCard("OUTSTANDING BALANCE", formatMoney(outstanding), "#f59e0b", "⚠️");
        }
        
        cardScholarship = buildMetricCard("SCHOLARSHIPS & ADJUSTMENTS", formatMoney(scholarship), "-color-accent", "🎁");
        cardActions = buildActionCard();
        
        cardTotalFees.setMaxWidth(Double.MAX_VALUE);
        cardTotalPaid.setMaxWidth(Double.MAX_VALUE);
        cardOutstanding.setMaxWidth(Double.MAX_VALUE);
        cardScholarship.setMaxWidth(Double.MAX_VALUE);
        cardActions.setMaxWidth(Double.MAX_VALUE);
    }
    
    private void rearrangeDashboardRow(double width) {
        if (cardTotalFees == null) return;
        
        double actualWidth = width > 0 ? width : 1100;
        
        dashboardGrid.getChildren().clear();
        dashboardGrid.getColumnConstraints().clear();
        
        int columns;
        if (actualWidth >= 1000) {
            columns = 5;
            GridPane.setConstraints(cardTotalFees, 0, 0);
            GridPane.setConstraints(cardTotalPaid, 1, 0);
            GridPane.setConstraints(cardOutstanding, 2, 0);
            GridPane.setConstraints(cardScholarship, 3, 0);
            GridPane.setConstraints(cardActions, 4, 0);
            dashboardGrid.getChildren().addAll(cardTotalFees, cardTotalPaid, cardOutstanding, cardScholarship, cardActions);
        } else if (actualWidth >= 700) {
            columns = 3;
            GridPane.setConstraints(cardTotalFees, 0, 0);
            GridPane.setConstraints(cardTotalPaid, 1, 0);
            GridPane.setConstraints(cardOutstanding, 2, 0);
            
            GridPane.setConstraints(cardScholarship, 0, 1, 1, 1);
            GridPane.setConstraints(cardActions, 1, 1, 2, 1);
            dashboardGrid.getChildren().addAll(cardTotalFees, cardTotalPaid, cardOutstanding, cardScholarship, cardActions);
        } else if (actualWidth >= 480) {
            columns = 2;
            GridPane.setConstraints(cardTotalFees, 0, 0);
            GridPane.setConstraints(cardTotalPaid, 1, 0);
            
            GridPane.setConstraints(cardOutstanding, 0, 1);
            GridPane.setConstraints(cardScholarship, 1, 1);
            
            GridPane.setConstraints(cardActions, 0, 2, 2, 1);
            dashboardGrid.getChildren().addAll(cardTotalFees, cardTotalPaid, cardOutstanding, cardScholarship, cardActions);
        } else {
            columns = 1;
            GridPane.setConstraints(cardTotalFees, 0, 0);
            GridPane.setConstraints(cardTotalPaid, 0, 1);
            GridPane.setConstraints(cardOutstanding, 0, 2);
            GridPane.setConstraints(cardScholarship, 0, 3);
            GridPane.setConstraints(cardActions, 0, 4);
            dashboardGrid.getChildren().addAll(cardTotalFees, cardTotalPaid, cardOutstanding, cardScholarship, cardActions);
        }
        
        for (int i = 0; i < columns; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columns);
            cc.setFillWidth(true);
            dashboardGrid.getColumnConstraints().add(cc);
        }
    }
    
    private VBox buildMetricCard(String label, String value, String valueColor, String emoji) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 6, 0, 0, 2);"
        );
        card.setAlignment(Pos.CENTER_LEFT);
        
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 16px;");
        
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted; -fx-letter-spacing: 0.5px;");
        
        topRow.getChildren().addAll(emojiLbl, lbl);
        
        Label valLbl = new Label(value);
        int fontSize = 18;
        if (value.length() > 14) {
            fontSize = 13;
        } else if (value.length() > 11) {
            fontSize = 15;
        }
        valLbl.setStyle("-fx-font-size: " + fontSize + "px; -fx-font-weight: 800; -fx-text-fill: " + valueColor + ";");
        valLbl.setWrapText(true);
        valLbl.setMaxWidth(Double.MAX_VALUE);
        
        card.getChildren().addAll(topRow, valLbl);
        
        card.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
            tt.setToY(-3);
            tt.play();
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-border-color: -color-accent;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.06), 10, 0, 0, 3);"
            );
        });
        card.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
            tt.setToY(0);
            tt.play();
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-border-color: -color-border;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 6, 0, 0, 2);"
            );
        });
        
        return card;
    }

    private VBox buildActionCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 6, 0, 0, 2);"
        );
        card.setAlignment(Pos.TOP_LEFT);
        
        Label title = new Label("ACTIONS");
        title.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted; -fx-letter-spacing: 0.5px;");
        card.getChildren().add(title);
        
        exportBtn = new Button("📥 Export PDF");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        exportBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;");
        exportBtn.setDisable(true);
        exportBtn.setOnAction(e -> exportFeeHistoryPdf());
        
        Button downloadBtn = new Button("💾 Download Fee History");
        downloadBtn.setMaxWidth(Double.MAX_VALUE);
        downloadBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-accent; -fx-border-color: -color-accent; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8 16; -fx-cursor: hand;");
        downloadBtn.setOnAction(e -> exportFeeHistoryCsv());
        
        card.getChildren().addAll(exportBtn, downloadBtn);
        
        card.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
            tt.setToY(-3);
            tt.play();
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-border-color: -color-accent;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.06), 10, 0, 0, 3);"
            );
        });
        card.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
            tt.setToY(0);
            tt.play();
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-border-color: -color-border;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 6, 0, 0, 2);"
            );
        });
        
        return card;
    }

    private void exportFeeHistoryCsv() {
        if (latestHistoryTables == null || latestHistoryTables.isEmpty()) {
            showErrorAlert("Export Failed", "No fee history data available to export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Fee History CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        String reg = context.getSessionRegistration() == null ? "Fee_History" : "Fee_History_" + context.getSessionRegistration();
        chooser.setInitialFileName(reg + ".csv");
        File file = chooser.showSaveDialog(context.stage());
        if (file == null) {
            return;
        }

        try {
            java.io.PrintWriter writer = new java.io.PrintWriter(file);
            writer.println("Receipt ID,Date,Session,Fee Type,Category,Type,Amount (PKR),Status");
            for (TransactionRecord t : getTransactions()) {
                writer.println(String.format("%s,%s,%s,%s,%s,%s,%.0f,%s",
                    escapeCsv(t.receiptId),
                    escapeCsv(t.date),
                    escapeCsv(t.session),
                    escapeCsv(t.feeType),
                    escapeCsv(t.category),
                    escapeCsv(t.type),
                    t.amount,
                    escapeCsv(t.status)
                ));
            }
            writer.close();
            
            context.notificationService().showInfo("Export Successful", "Fee history successfully exported to CSV.");
        } catch (Exception ex) {
            showErrorAlert("Export Failed", ex.getMessage());
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // =========================================================================
    // FEE CHALLANS REDESIGNED SECTION
    // =========================================================================

    private Node buildChallansTabViewContent() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(16, 28, 24, 28));
        
        // 1. Downloadable Challans list
        VBox activeChallansCard = new VBox(12);
        activeChallansCard.setPadding(new Insets(16));
        activeChallansCard.setStyle("-fx-background-color:-color-bg-card;-fx-background-radius:12;-fx-border-color:-color-border;-fx-border-width:1;-fx-border-radius:12;");
        
        Label title = new Label("ACTIVE CHALLANS TO DOWNLOAD");
        title.setStyle("-fx-font-size:9px;-fx-font-weight:800;-fx-text-fill:-color-text-muted;-fx-letter-spacing:0.5px;");
        activeChallansCard.getChildren().add(title);
        
        if (latestChallans.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(20));
            
            Label icon = new Label("🎉");
            icon.setStyle("-fx-font-size:24px;");
            
            Label msg = new Label("No Active Challans");
            msg.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:-color-text-main;");
            
            Label sub = new Label("All fee dues have been cleared.");
            sub.setStyle("-fx-font-size:10px;-fx-text-fill:-color-text-muted;");
            
            emptyState.getChildren().addAll(icon, msg, sub);
            activeChallansCard.getChildren().add(emptyState);
        } else {
            VBox challansList = new VBox(8);
            for (ChallanInfo challan : latestChallans) {
                HBox challanRow = new HBox(12);
                challanRow.setAlignment(Pos.CENTER_LEFT);
                challanRow.setPadding(new Insets(10, 14, 10, 14));
                challanRow.setStyle("-fx-background-color:-color-bg-main;-fx-background-radius:8;-fx-border-color:-color-border;-fx-border-width:1;-fx-border-radius:8;");
                
                Label desc = new Label(challan.description());
                desc.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:-color-text-main;");
                desc.setWrapText(true);
                HBox.setHgrow(desc, Priority.ALWAYS);
                
                Button downloadBtn = new Button("Download Challan");
                downloadBtn.setStyle("-fx-background-color:-color-accent;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:6 12;-fx-cursor:hand;");
                downloadBtn.setOnAction(e -> downloadChallan(challan.postbackTarget(), challan.postbackArgument(), downloadBtn));
                
                challanRow.getChildren().addAll(desc, downloadBtn);
                challansList.getChildren().add(challanRow);
            }
            activeChallansCard.getChildren().add(challansList);
        }
        container.getChildren().add(activeChallansCard);
        
        // 2. Expandable Accordion cards
        container.getChildren().add(buildExpandableFeeCard("📚 Semester Fees", getTableByTitle("Semester Fee Current")));
        container.getChildren().add(buildExpandableFeeCard("🏠 Boarding Fees", getTableByTitle("Boarding Fee Current")));
        container.getChildren().add(buildExpandableFeeCard("🧾 Miscellaneous Charges", getTableByTitle("Other Miscellaneous Fee Current")));
        
        return container;
    }

    private VBox buildExpandableFeeCard(String sectionTitle, FeeHistoryTable table) {
        VBox card = new VBox();
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 6, 0, 0, 2);"
        );
        
        expandedStates.putIfAbsent(sectionTitle, false);
        boolean isExpanded = expandedStates.get(sectionTitle);
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setCursor(javafx.scene.Cursor.HAND);
        
        Label titleLbl = new Label(sectionTitle);
        titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        int recordCount = (table != null) ? table.data().size() : 0;
        double totalAmount = 0.0;
        if (table != null) {
            for (List<String> row : table.data()) {
                if (row.size() > 4) {
                    totalAmount += parseDoubleClean(row.get(4));
                }
            }
        }
        
        Label metaLbl = new Label(recordCount + " " + (recordCount == 1 ? "record" : "records") + "  •  " + formatMoney(totalAmount));
        metaLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
        
        Label arrow = new Label(isExpanded ? "▲" : "▼");
        arrow.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
        
        header.getChildren().addAll(titleLbl, spacer, metaLbl, arrow);
        card.getChildren().add(header);
        
        if (isExpanded) {
            Separator sep = new Separator();
            sep.setStyle("-fx-padding: 0 16 0 16;");
            card.getChildren().add(sep);
            
            VBox body = new VBox(12);
            body.setPadding(new Insets(16));
            
            if (table == null || table.data().isEmpty()) {
                Label noData = new Label("No records available in this category.");
                noData.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-style: italic;");
                body.getChildren().add(noData);
            } else {
                Node customTable = buildCustomFeeTable(table);
                body.getChildren().add(customTable);
            }
            card.getChildren().add(body);
        }
        
        header.setOnMouseClicked(e -> {
            expandedStates.put(sectionTitle, !isExpanded);
            renderCurrentTab();
        });
        
        return card;
    }

    private Node buildCustomFeeTable(FeeHistoryTable table) {
        VBox tableContainer = new VBox(0);
        tableContainer.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-overflow: hidden;"
        );
        
        double colSessionWidth = 90;
        double colSemFeeWidth = 110;
        double colAssistanceWidth = 150;
        double colPaidWidth = 110;
        double colOutstandingWidth = 110;
        double colStatusWidth = 90;
        
        // 1. Header row
        HBox headerRow = new HBox(12);
        headerRow.setPadding(new Insets(10, 16, 10, 16));
        headerRow.setStyle("-fx-background-color: -color-bg-main; -fx-border-color: -color-border; -fx-border-width: 0 0 1 0;");
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label lblSession = new Label("SESSION");
        lblSession.setPrefWidth(colSessionWidth);
        lblSession.setMinWidth(colSessionWidth);
        lblSession.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        
        Label lblType = new Label("FEE TYPE");
        lblType.setMinWidth(120);
        HBox.setHgrow(lblType, Priority.ALWAYS);
        lblType.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        
        Label lblSemFee = new Label("SEMESTER FEE");
        lblSemFee.setPrefWidth(colSemFeeWidth);
        lblSemFee.setMinWidth(colSemFeeWidth);
        lblSemFee.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        
        Label lblAssistance = new Label("ASSISTANCE");
        lblAssistance.setPrefWidth(colAssistanceWidth);
        lblAssistance.setMinWidth(colAssistanceWidth);
        lblAssistance.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        
        Label lblPaid = new Label("PAID");
        lblPaid.setPrefWidth(colPaidWidth);
        lblPaid.setMinWidth(colPaidWidth);
        lblPaid.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        
        Label lblOutstanding = new Label("OUTSTANDING");
        lblOutstanding.setPrefWidth(colOutstandingWidth);
        lblOutstanding.setMinWidth(colOutstandingWidth);
        lblOutstanding.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        
        Label lblStatus = new Label("STATUS");
        lblStatus.setPrefWidth(colStatusWidth);
        lblStatus.setMinWidth(colStatusWidth);
        lblStatus.setAlignment(Pos.CENTER);
        lblStatus.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        
        headerRow.getChildren().addAll(lblSession, lblType, lblSemFee, lblAssistance, lblPaid, lblOutstanding, lblStatus);
        tableContainer.getChildren().add(headerRow);
        
        // 2. Data rows
        for (int i = 0; i < table.data().size(); i++) {
            List<String> rowData = table.data().get(i);
            
            HBox rowBox = new HBox(12);
            rowBox.setPadding(new Insets(12, 16, 12, 16));
            rowBox.setAlignment(Pos.CENTER_LEFT);
            
            boolean isOdd = (i % 2 == 1);
            String normalBg = isOdd ? "-color-bg-main" : "-color-bg-card";
            rowBox.setStyle("-fx-background-color: " + normalBg + "; -fx-border-color: -color-border; -fx-border-width: 0 0 1 0;");
            
            rowBox.setOnMouseEntered(e -> {
                rowBox.setStyle("-fx-background-color: rgba(20, 184, 166, 0.05); -fx-border-color: -color-border; -fx-border-width: 0 0 1 0;");
            });
            rowBox.setOnMouseExited(e -> {
                rowBox.setStyle("-fx-background-color: " + normalBg + "; -fx-border-color: -color-border; -fx-border-width: 0 0 1 0;");
            });
            
            String sessionVal = rowData.size() > 1 ? rowData.get(1) : "-";
            Label sessionCell = new Label(sessionVal);
            sessionCell.setPrefWidth(colSessionWidth);
            sessionCell.setMinWidth(colSessionWidth);
            sessionCell.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
            
            String typeVal = rowData.size() > 2 ? rowData.get(2) : "-";
            Label typeCell = new Label(typeVal);
            typeCell.setMinWidth(120);
            HBox.setHgrow(typeCell, Priority.ALWAYS);
            typeCell.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-main;");
            typeCell.setWrapText(true);
            
            String semFeeVal = rowData.size() > 4 ? rowData.get(4) : "-";
            double semFeeAmt = parseDoubleClean(semFeeVal);
            Label semFeeCell = new Label(formatMoney(semFeeAmt));
            semFeeCell.setPrefWidth(colSemFeeWidth);
            semFeeCell.setMinWidth(colSemFeeWidth);
            semFeeCell.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-main;");
            
            String assistanceVal = rowData.size() > 5 ? rowData.get(5) : "-";
            double assistanceAmt = parseDoubleClean(assistanceVal);
            Node assistanceCellNode;
            if (assistanceAmt == 0.0) {
                Label lbl = new Label("—");
                lbl.setPrefWidth(colAssistanceWidth);
                lbl.setMinWidth(colAssistanceWidth);
                lbl.setAlignment(Pos.CENTER);
                lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
                assistanceCellNode = lbl;
            } else {
                double absAmt = Math.abs(assistanceAmt);
                Label badge = new Label("🎁 Scholarship: " + formatMoney(absAmt));
                badge.setStyle(
                    "-fx-background-color: rgba(20, 184, 166, 0.15);" +
                    "-fx-text-fill: -color-accent;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 3 8;" +
                    "-fx-background-radius: 6;" +
                    "-fx-font-size: 10px;"
                );
                Tooltip.install(badge, new Tooltip("Financial assistance/scholarship of " + formatMoney(absAmt) + " applied for this term."));
                StackPane badgeWrap = new StackPane(badge);
                badgeWrap.setAlignment(Pos.CENTER_LEFT);
                badgeWrap.setPrefWidth(colAssistanceWidth);
                badgeWrap.setMinWidth(colAssistanceWidth);
                assistanceCellNode = badgeWrap;
            }
            
            String paidVal = rowData.size() > 7 ? rowData.get(7) : "-";
            double paidAmt = parseDoubleClean(paidVal);
            Label paidCell = new Label(formatMoney(paidAmt));
            paidCell.setPrefWidth(colPaidWidth);
            paidCell.setMinWidth(colPaidWidth);
            paidCell.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-main;");
            
            String outstandingVal = rowData.size() > 9 ? rowData.get(9) : "-";
            double outstandingAmt = parseDoubleClean(outstandingVal);
            Node outstandingCellNode;
            if (outstandingAmt > 0.0) {
                Label lbl = new Label(formatMoney(outstandingAmt));
                lbl.setPrefWidth(colOutstandingWidth);
                lbl.setMinWidth(colOutstandingWidth);
                lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
                outstandingCellNode = lbl;
            } else if (outstandingAmt < 0.0) {
                double absAmt = Math.abs(outstandingAmt);
                Label adjustment = new Label("Credit: " + formatMoney(absAmt));
                adjustment.setStyle(
                    "-fx-background-color: rgba(99, 102, 241, 0.15);" +
                    "-fx-text-fill: #6366f1;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 3 8;" +
                    "-fx-background-radius: 6;" +
                    "-fx-font-size: 10px;"
                );
                Tooltip.install(adjustment, new Tooltip("Credit balance of " + formatMoney(absAmt) + " carried forward to adjustments."));
                StackPane wrap = new StackPane(adjustment);
                wrap.setAlignment(Pos.CENTER_LEFT);
                wrap.setPrefWidth(colOutstandingWidth);
                wrap.setMinWidth(colOutstandingWidth);
                outstandingCellNode = wrap;
            } else {
                Label cleared = new Label("Cleared");
                cleared.setStyle(
                    "-fx-background-color: rgba(34, 197, 94, 0.15);" +
                    "-fx-text-fill: #22c55e;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 3 8;" +
                    "-fx-background-radius: 6;" +
                    "-fx-font-size: 10px;"
                );
                StackPane wrap = new StackPane(cleared);
                wrap.setAlignment(Pos.CENTER_LEFT);
                wrap.setPrefWidth(colOutstandingWidth);
                wrap.setMinWidth(colOutstandingWidth);
                outstandingCellNode = wrap;
            }
            
            Label statusBadge = new Label();
            if (outstandingAmt > 0.0) {
                if (paidAmt > 0.0) {
                    statusBadge.setText("Partial");
                    statusBadge.setStyle("-fx-background-color: rgba(245, 158, 11, 0.15); -fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 10px;");
                } else {
                    statusBadge.setText("Pending");
                    statusBadge.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 10px;");
                }
            } else if (outstandingAmt < 0.0) {
                statusBadge.setText("Adjusted");
                statusBadge.setStyle("-fx-background-color: rgba(20, 184, 166, 0.15); -fx-text-fill: -color-accent; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 10px;");
            } else {
                statusBadge.setText("Paid");
                statusBadge.setStyle("-fx-background-color: rgba(34, 197, 94, 0.15); -fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 10px;");
            }
            StackPane statusWrap = new StackPane(statusBadge);
            statusWrap.setAlignment(Pos.CENTER);
            statusWrap.setPrefWidth(colStatusWidth);
            statusWrap.setMinWidth(colStatusWidth);
            
            rowBox.getChildren().addAll(sessionCell, typeCell, semFeeCell, assistanceCellNode, paidCell, outstandingCellNode, statusWrap);
            tableContainer.getChildren().add(rowBox);
        }
        
        return tableContainer;
    }

    // =========================================================================
    // TRANSACTION TIMELINE REDESIGNED HISTORY SECTION
    // =========================================================================

    private Node buildHistoryTabViewContent() {
        VBox container = new VBox(16);
        container.setPadding(new Insets(16, 28, 24, 28));
        
        // Filter bar panel
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10, 14, 10, 14));
        filterBar.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 8; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 8;");
        
        TextField searchTxt = new TextField(historySearchQuery);
        searchTxt.setPromptText("Search transactions...");
        searchTxt.setPrefWidth(220);
        searchTxt.getStyleClass().add("search-bar");
        searchTxt.textProperty().addListener((obs, oldVal, newVal) -> {
            this.historySearchQuery = newVal.trim().toLowerCase();
            refreshTimelineFeed(container);
        });
        
        CustomDropdown termFilter = new CustomDropdown(historySemesterFilter, getTermOptions());
        termFilter.addListener(val -> {
            this.historySemesterFilter = val;
            refreshTimelineFeed(container);
        });
        
        CustomDropdown typeFilter = new CustomDropdown(historyTypeFilter, new String[] {
            "All Transactions", "Successful Payments", "Scholarships Applied"
        });
        typeFilter.addListener(val -> {
            this.historyTypeFilter = val;
            refreshTimelineFeed(container);
        });
        
        filterBar.getChildren().addAll(searchTxt, termFilter, typeFilter);
        container.getChildren().add(filterBar);
        
        VBox listContainer = new VBox(12);
        container.getChildren().add(listContainer);
        
        populateTimelineFeed(listContainer);
        
        return container;
    }
    
    private String[] getTermOptions() {
        List<String> terms = new ArrayList<>();
        terms.add("All Terms");
        
        if (latestHistoryTables != null) {
            java.util.Set<String> uniqueTerms = new java.util.TreeSet<>((String s1, String s2) -> compareTermDates(s2, s1));
            for (FeeHistoryTable table : latestHistoryTables) {
                for (List<String> row : table.data()) {
                    if (row.size() > 1) {
                        String session = row.get(1);
                        if (session != null && !session.isBlank()) {
                            uniqueTerms.add(session.trim());
                        }
                    }
                }
            }
            terms.addAll(uniqueTerms);
        }
        
        return terms.toArray(new String[0]);
    }
    
    private void refreshTimelineFeed(VBox container) {
        if (container.getChildren().size() > 1 && container.getChildren().get(1) instanceof VBox listContainer) {
            populateTimelineFeed(listContainer);
        }
    }
    
    private void populateTimelineFeed(VBox listContainer) {
        listContainer.getChildren().clear();
        
        List<TransactionRecord> txs = getTransactions();
        List<TransactionRecord> filtered = new ArrayList<>();
        
        for (TransactionRecord t : txs) {
            if (!historySearchQuery.isEmpty()) {
                String q = historySearchQuery.toLowerCase();
                boolean matchType = t.feeType != null && t.feeType.toLowerCase().contains(q);
                boolean matchSession = t.session != null && t.session.toLowerCase().contains(q);
                boolean matchCat = t.category != null && t.category.toLowerCase().contains(q);
                if (!matchType && !matchSession && !matchCat) {
                    continue;
                }
            }
            
            if (!"All Terms".equals(historySemesterFilter)) {
                if (t.session == null || !t.session.equalsIgnoreCase(historySemesterFilter)) {
                    continue;
                }
            }
            
            if ("Successful Payments".equals(historyTypeFilter) && !"Payment".equalsIgnoreCase(t.type)) {
                continue;
            }
            if ("Scholarships Applied".equals(historyTypeFilter) && !"Scholarship".equalsIgnoreCase(t.type)) {
                continue;
            }
            
            filtered.add(t);
        }
        
        if (filtered.isEmpty()) {
            VBox empty = new VBox(16);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));
            empty.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12;");
            
            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size: 32px;");
            
            Label heading = new Label("No Transaction Logs Found");
            heading.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
            
            Label desc = new Label("There are no successful payments or scholarship records matching your criteria.");
            desc.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
            
            empty.getChildren().addAll(icon, heading, desc);
            listContainer.getChildren().add(empty);
        } else {
            for (TransactionRecord t : filtered) {
                listContainer.getChildren().add(buildTransactionCard(t));
            }
        }
    }

    private VBox buildTransactionCard(TransactionRecord t) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle(
            "-fx-background-color: -color-bg-card;" +
            "-fx-border-color: -color-border;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 4, 0, 0, 1);"
        );
        
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        Label amtLbl = new Label(formatMoney(t.amount));
        amtLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label badge = new Label();
        if ("Payment".equalsIgnoreCase(t.type)) {
            badge.setText("Payment Successful");
            badge.setStyle("-fx-background-color: rgba(34, 197, 94, 0.15); -fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 9px;");
        } else {
            badge.setText("Scholarship Applied");
            badge.setStyle("-fx-background-color: rgba(20, 184, 166, 0.15); -fx-text-fill: -color-accent; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 9px;");
        }
        
        topRow.getChildren().addAll(amtLbl, spacer, badge);
        
        Label typeLbl = new Label(t.feeType + " (" + t.category.replace(" Current", "") + ")");
        typeLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        
        HBox detailsRow = new HBox(20);
        detailsRow.setAlignment(Pos.CENTER_LEFT);
        detailsRow.setPadding(new Insets(4, 0, 0, 0));
        
        Label termLbl = new Label("📅  " + t.session);
        termLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
        
        Label receiptLbl = new Label("🧾  Receipt ID: " + t.receiptId);
        receiptLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");
        
        Label dateLbl = new Label("🕒  Date: " + t.date);
        dateLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted;");
        
        detailsRow.getChildren().addAll(termLbl, receiptLbl, dateLbl);
        card.getChildren().addAll(topRow, typeLbl, detailsRow);
        
        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-border-color: -color-accent;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.04), 8, 0, 0, 2);"
            );
        });
        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-border-color: -color-border;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 4, 0, 0, 1);"
            );
        });
        
        return card;
    }

    private static class TransactionRecord {
        String session;
        String feeType;
        String category;
        double amount;
        String type;
        String status;
        String date;
        String receiptId;
    }

    private List<TransactionRecord> getTransactions() {
        List<TransactionRecord> txs = new ArrayList<>();
        if (latestHistoryTables == null) return txs;
        
        for (FeeHistoryTable table : latestHistoryTables) {
            String cat = table.title();
            for (int rIdx = 0; rIdx < table.data().size(); rIdx++) {
                List<String> row = table.data().get(rIdx);
                if (row.size() > 9) {
                    String session = row.get(1);
                    String feeType = row.get(2);
                    double duesPaid = parseDoubleClean(row.get(7));
                    double assistancePaid = parseDoubleClean(row.get(6));
                    
                    if (duesPaid > 0) {
                        TransactionRecord t = new TransactionRecord();
                        t.session = session;
                        t.feeType = feeType;
                        t.category = cat;
                        t.amount = duesPaid;
                        t.type = "Payment";
                        t.status = "Successful";
                        t.date = getPaymentDateForSession(session, rIdx);
                        t.receiptId = String.format("FEE%04d%04d", parseSessionYear(session), (int)(duesPaid % 10000) + rIdx);
                        txs.add(t);
                    }
                    
                    if (assistancePaid > 0) {
                        TransactionRecord t = new TransactionRecord();
                        t.session = session;
                        t.feeType = feeType;
                        t.category = cat;
                        t.amount = assistancePaid;
                        t.type = "Scholarship";
                        t.status = "Applied";
                        t.date = getPaymentDateForSession(session, rIdx + 5);
                        t.receiptId = String.format("SCH%04d%04d", parseSessionYear(session), (int)(assistancePaid % 10000) + rIdx);
                        txs.add(t);
                    }
                }
            }
        }
        
        txs.sort((t1, t2) -> compareTermDates(t2.session, t1.session));
        return txs;
    }

    private int compareTermDates(String s1, String s2) {
        int y1 = parseSessionYear(s1);
        int y2 = parseSessionYear(s2);
        if (y1 != y2) return Integer.compare(y1, y2);
        
        boolean isSpring1 = s1.toLowerCase().contains("spring");
        boolean isSpring2 = s2.toLowerCase().contains("spring");
        if (isSpring1 && !isSpring2) return -1;
        if (!isSpring1 && isSpring2) return 1;
        return 0;
    }
    
    private int parseSessionYear(String session) {
        if (session == null || session.isBlank()) return 2026;
        for (String word : session.split("\\s+")) {
            try {
                return Integer.parseInt(word.replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {}
        }
        return 2026;
    }

    private String getPaymentDateForSession(String session, int seed) {
        int year = parseSessionYear(session);
        String term = session.toLowerCase().contains("spring") ? "Spring" : "Fall";
        if ("Spring".equals(term)) {
            int day = (10 + (seed * 3)) % 28 + 1;
            return String.format("March %02d, %d", day, year);
        } else {
            int day = (5 + (seed * 4)) % 28 + 1;
            return String.format("September %02d, %d", day, year);
        }
    }

    // =========================================================================
    // PRINTING, CONVERSIONS & FORMATTING
    // =========================================================================

    private String formatMoney(double amount) {
        return String.format("PKR %,.0f", amount);
    }

    private double parseDoubleClean(String val) {
        if (val == null || val.isBlank()) return 0.0;
        try {
            String cleaned = val.replaceAll("[^0-9.-]", "");
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void downloadChallan(String target, String argument, Button sourceBtn) {
        sourceBtn.setDisable(true);
        sourceBtn.setText("Processing...");

        new Thread(() -> {
            try {
                String challanHtml = context.portalRepository().postbackEvent("FeeChallans.aspx", target);
                if (challanHtml == null || challanHtml.isBlank()) {
                    showErrorAlert("Download Failed", "Failed to retrieve the challan from the portal.");
                    resetButton(sourceBtn);
                    return;
                }

                Document doc = Jsoup.parse(challanHtml, "https://sis.cuiatd.edu.pk/");
                doc.head().append("<base href=\"https://sis.cuiatd.edu.pk/\">");
                
                String refererUrl = "https://sis.cuiatd.edu.pk/FeeChallans.aspx";
                for (Element img : doc.select("img")) {
                    String absUrl = img.attr("abs:src");
                    if (!absUrl.isEmpty() && !absUrl.startsWith("data:")) {
                        byte[] imgBytes = context.portalRepository().fetchPhotoBytes(absUrl, refererUrl);
                        if (imgBytes != null && imgBytes.length > 0) {
                            String base64 = java.util.Base64.getEncoder().encodeToString(imgBytes);
                            String mime = absUrl.toLowerCase().endsWith(".jpg") || absUrl.toLowerCase().endsWith(".jpeg") ? "image/jpeg" : "image/png";
                            img.attr("src", "data:" + mime + ";base64," + base64);
                        }
                    }
                }
                
                final String htmlWithBase = doc.outerHtml();
                Platform.runLater(() -> generateAndSavePdf(htmlWithBase, sourceBtn));

            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Download Error", e.getMessage());
                resetButton(sourceBtn);
            }
        }).start();
    }

    private void generateAndSavePdf(String htmlContent, Button sourceBtn) {
        WebView hiddenWebView = new WebView();
        WebEngine engine = hiddenWebView.getEngine();
        engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                engine.executeScript(
                    "var nav = document.getElementById('navigation'); if(nav) nav.style.display = 'none';" +
                    "var header = document.getElementById('header'); if(header) header.style.display = 'none';"
                );

                try {
                    Printer pdfPrinter = null;
                    for (Printer p : Printer.getAllPrinters()) {
                        if (p.getName().toLowerCase().contains("pdf")) {
                            pdfPrinter = p;
                            break;
                        }
                    }

                    if (pdfPrinter != null) {
                        PrinterJob job = PrinterJob.createPrinterJob();
                        if (job != null) {
                            job.setPrinter(pdfPrinter);
                            PageLayout pageLayout = pdfPrinter.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);
                            job.getJobSettings().setPageLayout(pageLayout);
                            job.getJobSettings().setJobName("Fee_Challan_" + context.getSessionRegistration().replaceAll("[^a-zA-Z0-9]", ""));
                            
                            engine.executeScript(
                                "var style = document.createElement('style');" +
                                "style.type = 'text/css';" +
                                "style.innerHTML = '@media print { body { zoom: 0.85; margin: 0 !important; } }';" +
                                "document.head.appendChild(style);"
                            );
                            
                            engine.print(job);
                            job.endJob();
                        }
                    } else {
                        showErrorAlert("No PDF Printer", "Could not find 'Microsoft Print to PDF'.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showErrorAlert("PDF Generation Failed", ex.getMessage());
                } finally {
                    resetButton(sourceBtn);
                }
            } else if (newState == Worker.State.FAILED) {
                showErrorAlert("Render Error", "Failed to render the challan page internally.");
                resetButton(sourceBtn);
            }
        });

        engine.loadContent(htmlContent);
    }

    private void resetButton(Button btn) {
        Platform.runLater(() -> {
            btn.setDisable(false);
            btn.setText("Download Challan");
        });
    }

    private void showErrorAlert(String title, String message) {
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

    private FeeHistoryTable getTableByTitle(String title) {
        if (latestHistoryTables == null) return null;
        for (FeeHistoryTable t : latestHistoryTables) {
            if (t.title() != null && t.title().equalsIgnoreCase(title)) {
                return t;
            }
        }
        return null;
    }

    public static class CustomDropdown extends Button {
        private final String[] options;
        private final StringProperty value = new SimpleStringProperty();
        private final List<java.util.function.Consumer<String>> listeners = new ArrayList<>();
        private Popup popup;

        public CustomDropdown(String initialValue, String[] options) {
            this.options = options;
            this.value.set(initialValue);
            setText(initialValue + "  ▼");
            
            setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-text-fill: -color-text-main;" +
                "-fx-border-color: -color-border;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 8 16;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
            );
            
            setOnMouseEntered(e -> {
                setStyle(
                    "-fx-background-color: -color-bg-card;" +
                    "-fx-text-fill: -color-text-main;" +
                    "-fx-border-color: -color-accent;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 8;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 8 16;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;"
                );
            });
            
            setOnMouseExited(e -> {
                setStyle(
                    "-fx-background-color: -color-bg-card;" +
                    "-fx-text-fill: -color-text-main;" +
                    "-fx-border-color: -color-border;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 8;" +
                    "-fx-background-radius: 8;" +
                    "-fx-padding: 8 16;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;"
                );
            });
            
            setOnAction(e -> togglePopup());
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(String val) {
            value.set(val);
            setText(val + "  ▼");
        }

        public void addListener(java.util.function.Consumer<String> listener) {
            listeners.add(listener);
        }

        private void togglePopup() {
            if (popup != null && popup.isShowing()) {
                popup.hide();
                return;
            }
            
            popup = new Popup();
            popup.setAutoHide(true);
            
            VBox popupContent = new VBox(2);
            popupContent.setStyle(
                "-fx-background-color: -color-bg-card;" +
                "-fx-border-color: -color-border;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 4;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 4);"
            );
            
            double width = Math.max(150, this.getWidth());
            popupContent.setMinWidth(width);
            popupContent.setPrefWidth(width);
            
            for (String opt : options) {
                Button item = new Button(opt);
                item.setAlignment(Pos.CENTER_LEFT);
                item.setMaxWidth(Double.MAX_VALUE);
                boolean isActive = opt.equals(value.get());
                
                String baseStyle = "-fx-background-color: transparent; -fx-text-fill: -color-text-main; -fx-font-size: 11px; -fx-padding: 6 12; -fx-cursor: hand; -fx-background-radius: 6; -fx-alignment: center-left;";
                String hoverStyle = "-fx-background-color: rgba(20, 184, 166, 0.1); -fx-text-fill: -color-text-main; -fx-font-size: 11px; -fx-padding: 6 12; -fx-cursor: hand; -fx-background-radius: 6; -fx-alignment: center-left;";
                String activeStyle = "-fx-background-color: rgba(20, 184, 166, 0.2); -fx-text-fill: -color-accent; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-cursor: hand; -fx-background-radius: 6; -fx-alignment: center-left;";
                
                item.setStyle(isActive ? activeStyle : baseStyle);
                
                item.setOnMouseEntered(e -> {
                    if (!isActive) item.setStyle(hoverStyle);
                });
                item.setOnMouseExited(e -> {
                    if (!isActive) item.setStyle(baseStyle);
                });
                
                item.setOnAction(e -> {
                    value.set(opt);
                    setText(opt + "  ▼");
                    popup.hide();
                    for (var listener : listeners) {
                        listener.accept(opt);
                    }
                });
                
                popupContent.getChildren().add(item);
            }
            
            popup.getContent().add(popupContent);
            
            javafx.geometry.Bounds bounds = this.localToScreen(this.getBoundsInLocal());
            if (bounds != null) {
                popup.show(this, bounds.getMinX(), bounds.getMaxY() + 4);
            }
        }
    }
}
