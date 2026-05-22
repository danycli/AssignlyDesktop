package com.assignly.view;

import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class FeeTabView {

    private final VBox root = new VBox();
    private final AppContext context;
    private StackPane contentPane;
    private HBox tabBar;
    private String activeTab = "";

    // Data structures
    private record ChallanInfo(String description, String postbackTarget, String postbackArgument) {}
    private record FeeHistoryTable(String title, List<String> headers, List<List<String>> data) {}

    public FeeTabView(AppContext context) {
        this.context = context;
        buildShell();
        loadTab("fee_challans");
    }

    public VBox getRoot() { return root; }

    private void buildShell() {
        root.setFillWidth(true);
        Label heading = new Label("Fee Information");
        heading.getStyleClass().add("heading-label");
        heading.setPadding(new Insets(24, 28, 0, 28));

        tabBar = new HBox(4);
        tabBar.setPadding(new Insets(12, 28, 0, 28));
        tabBar.getChildren().addAll(
            tabBtn("Fee Challans", "fee_challans"),
            tabBtn("Fee History", "fee_history")
        );

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
        return on ? "-fx-background-color:#004643;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:600;-fx-background-radius:6;-fx-padding:6 12;"
                  : "-fx-background-color:white;-fx-text-fill:#666;-fx-font-size:11px;-fx-font-weight:500;-fx-background-radius:6;-fx-padding:6 12;-fx-border-color:#d5d0ce;-fx-border-radius:6;-fx-border-width:1;";
    }

    private void loadTab(String tabKey) {
        if (activeTab.equals(tabKey)) return;
        activeTab = tabKey;

        for (var n : tabBar.getChildren()) {
            if (n instanceof Button b) {
                b.setStyle(tabStyle(tabKey.equals(b.getUserData())));
            }
        }

        contentPane.getChildren().clear();
        switch (tabKey) {
            case "fee_challans" -> loadFeeChallans();
            case "fee_history" -> loadFeeHistory();
        }
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

    private VBox buildErrorState(String title, String message) {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40, 28, 40, 28));
        box.setStyle("-fx-background-color:#fff5f5;-fx-background-radius:12;-fx-border-color:#fee2e2;-fx-border-width:1;-fx-border-radius:12;-fx-max-width:550;");

        Label icon = new Label("⚠️");
        icon.setStyle("-fx-font-size:32px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill:#991b1b;-fx-font-size:16px;-fx-font-weight:bold;");

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-text-fill:#b91c1c;-fx-font-size:12px;-fx-text-alignment:center;");
        msgLbl.setWrapText(true);

        box.getChildren().addAll(icon, titleLbl, msgLbl);
        
        VBox outer = new VBox(box);
        outer.setAlignment(Pos.CENTER);
        outer.setPadding(new Insets(40));
        return outer;
    }

    // =========================================================================
    // FEE CHALLANS LOGIC
    // =========================================================================

    private void loadFeeChallans() {
        showLoading("Loading Fee Challans...");
        new Thread(() -> {
            try {
                String html = context.portalRepository().fetchPageHtml("FeeChallans.aspx");
                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState("Unable to load Fee Challans", "Failed to connect to the portal."));
                    });
                    return;
                }

                Document doc = Jsoup.parse(html);
                List<ChallanInfo> challans = new ArrayList<>();

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

                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    if (challans.isEmpty()) {
                        contentPane.getChildren().add(buildEmptyChallansView());
                    } else {
                        contentPane.getChildren().add(buildChallansListView(challans));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState("Error", e.getMessage()));
                });
            }
        }).start();
    }

    private VBox buildEmptyChallansView() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24, 28, 24, 28));
        content.setFillWidth(true);

        VBox card = new VBox(12);
        card.setPadding(new Insets(32));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color:white;-fx-background-radius:8;-fx-border-color:#e2e8f0;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size:32px;");

        Label noChallansLabel = new Label("No fee challans are currently available.");
        noChallansLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#475569;");

        card.getChildren().addAll(icon, noChallansLabel);
        content.getChildren().add(card);
        return content;
    }

    private VBox buildChallansListView(List<ChallanInfo> challans) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        VBox listContainer = new VBox(12);
        for (ChallanInfo challan : challans) {
            HBox row = new HBox(16);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(16));
            row.setStyle("-fx-background-color:white;-fx-background-radius:8;-fx-border-color:#e2e8f0;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

            Label desc = new Label(challan.description());
            desc.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#334155;");
            desc.setWrapText(true);
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button downloadBtn = new Button("Download Challan");
            downloadBtn.setStyle("-fx-background-color:#004643;-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:8 16;");
            downloadBtn.setCursor(javafx.scene.Cursor.HAND);
            downloadBtn.setOnAction(e -> downloadChallan(challan.postbackTarget(), challan.postbackArgument(), downloadBtn));

            row.getChildren().addAll(desc, spacer, downloadBtn);
            listContainer.getChildren().add(row);
        }

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        content.getChildren().add(scrollPane);
        return content;
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

    // =========================================================================
    // FEE HISTORY LOGIC
    // =========================================================================

    private void loadFeeHistory() {
        showLoading("Loading Fee History...");
        new Thread(() -> {
            try {
                String html = context.portalRepository().fetchPageHtml("FeeHistorySFMS.aspx");
                if (html == null) {
                    Platform.runLater(() -> {
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(buildErrorState("Unable to load Fee History", "Failed to connect to the portal."));
                    });
                    return;
                }

                Document doc = Jsoup.parse(html);
                List<FeeHistoryTable> historyTables = new ArrayList<>();
                Elements allTables = doc.select("table");

                int tableIndex = 0;
                String[] expectedTitles = {
                    "Semester Fee Current",
                    "Boarding Fee Current",
                    "Other Miscellaneous Fee Current"
                };

                for (Element table : allTables) {
                    Elements rows = table.select("tr");
                    if (rows.size() < 2) continue; // Needs at least header and one data row

                    Elements ths = rows.first().select("th");
                    if (ths.isEmpty()) continue;

                    String titleText = "Fee History";
                    
                    // Look for preceding sibling headings (h2, h3, span)
                    Element prev = table.previousElementSibling();
                    while (prev != null) {
                        String txt = prev.text().trim();
                        if (!txt.isEmpty()) {
                            titleText = txt;
                            break;
                        }
                        prev = prev.previousElementSibling();
                    }
                    
                    // Or if title is embedded in a colspan in the first row
                    if (ths.size() == 1 && ths.first().hasAttr("colspan")) {
                        titleText = ths.first().text().trim();
                        rows.remove(0); // Remove this title row
                        if (rows.isEmpty()) continue;
                        ths = rows.first().select("th");
                        if (ths.isEmpty()) ths = rows.first().select("td");
                    }
                    
                    // Clean up title if it contains things like labels or colons
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
                            // Assign the proper names directly based on their sequential order
                            String finalTitle = titleText;
                            if (tableIndex < expectedTitles.length) {
                                finalTitle = expectedTitles[tableIndex];
                            }
                            historyTables.add(new FeeHistoryTable(finalTitle, headers, data));
                            tableIndex++;
                        }
                    }
                }

                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    if (historyTables.isEmpty()) {
                        contentPane.getChildren().add(buildEmptyHistoryView());
                    } else {
                        contentPane.getChildren().add(buildMultiTableView(historyTables));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    contentPane.getChildren().clear();
                    contentPane.getChildren().add(buildErrorState("Error", e.getMessage()));
                });
            }
        }).start();
    }

    private VBox buildEmptyHistoryView() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24, 28, 24, 28));
        content.setFillWidth(true);

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(32));
        card.setStyle("-fx-background-color:white;-fx-background-radius:8;-fx-border-color:#e2e8f0;-fx-border-width:1;-fx-border-radius:8;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");

        Label icon = new Label("📭");
        icon.setStyle("-fx-font-size:32px;");

        Label noHistoryLabel = new Label("No Fee History Available");
        noHistoryLabel.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#475569;");

        card.getChildren().addAll(icon, noHistoryLabel);
        content.getChildren().add(card);
        return content;
    }

    private ScrollPane buildMultiTableView(List<FeeHistoryTable> tables) {
        VBox content = new VBox(32); // Generous spacing between tables
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        for (FeeHistoryTable table : tables) {
            VBox section = new VBox(12);
            
            Label title = new Label(table.title());
            title.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#004643;");
            
            TableView<List<String>> tableView = new TableView<>();
            tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tableView.setStyle("-fx-font-size:12px;");
            
            // Calculate an appropriate fixed height for the table based on row count
            // Base header height ~30px, each row ~25px
            int estimatedHeight = 35 + (table.data().size() * 25);
            tableView.setPrefHeight(Math.max(100, Math.min(300, estimatedHeight + 15)));

            for (int i = 0; i < table.headers().size(); i++) {
                final int colIndex = i;
                TableColumn<List<String>, String> column = new TableColumn<>(table.headers().get(i));
                column.setCellValueFactory(param -> {
                    List<String> row = param.getValue();
                    if (colIndex < row.size()) return new SimpleStringProperty(row.get(colIndex));
                    else return new SimpleStringProperty("");
                });
                tableView.getColumns().add(column);
            }

            ObservableList<List<String>> items = FXCollections.observableArrayList(table.data());
            tableView.setItems(items);
            
            section.getChildren().addAll(title, tableView);
            content.getChildren().add(section);
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        return scrollPane;
    }

    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
