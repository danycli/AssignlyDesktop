package com.assignly.view;

import com.assignly.service.PdfExportService;
import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        StackPane loading = new StackPane();
        loading.setStyle("-fx-background-color: -color-bg-main;");
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(28, 28);
        Label msg = new Label("Loading Result Card...");
        msg.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 13px;");
        box.getChildren().addAll(spinner, msg);
        loading.getChildren().add(box);
        VBox.setVgrow(loading, Priority.ALWAYS);
        root.getChildren().add(loading);
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

                    // Validate that this is actually a transcript table (and not a personal info table)
                    boolean isTranscript = false;
                    for (String header : headers) {
                        String lower = header.toLowerCase();
                        if (lower.contains("course") || lower.contains("credit") || lower.contains("marks") || lower.contains("grade")) {
                            isTranscript = true;
                            break;
                        }
                    }
                    if (!isTranscript) continue;

                    // Extract the data rows
                    List<List<String>> data = new ArrayList<>();
                    for (int i = headerRowIndex + 1; i < rows.size(); i++) {
                        Elements cells = rows.get(i).select("td, th"); // Footer might use th
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

                boolean finalOffline = isOffline;
                Platform.runLater(() -> {
                    root.getChildren().clear();
                    if (finalOffline) root.getChildren().add(buildOfflineBanner());
                    if (resultTables.isEmpty()) {
                        root.getChildren().add(buildEmptyView());
                    } else {
                        root.getChildren().add(buildMultiTableView(resultTables));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
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
        card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;"
                + "-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;"
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

    private VBox buildMultiTableView(List<SemesterResultTable> tables) {
        VBox wrapper = new VBox(0);
        wrapper.setFillWidth(true);
        VBox.setVgrow(wrapper, Priority.ALWAYS);

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(24, 28, 16, 28));
        headerRow.setStyle("-fx-background-color: -color-bg-card;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");
        
        VBox titleBox = new VBox(4);
        Label title = new Label("Academic Result Card");
        title.setStyle("-fx-font-size:24px;-fx-font-weight:800;-fx-text-fill: -color-text-main;");
        
        Label subTitle = new Label("Semester Transcripts");
        subTitle.setStyle("-fx-font-size:13px;-fx-text-fill: -color-text-muted;-fx-font-weight:600;");
        titleBox.getChildren().addAll(title, subTitle);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button refreshBtn = new Button("🔄");
        refreshBtn.setStyle("-fx-background-color:transparent;-fx-font-size:18px;-fx-cursor:hand;");
        refreshBtn.setOnAction(e -> {
            root.getChildren().clear();
            buildLoading();
            loadData(true);
        });

        Button exportBtn = new Button("📥 Export PDF");
        exportBtn.getStyleClass().add("accent-button");
        exportBtn.setOnAction(e -> exportResultPdf(tables));
        HBox.setMargin(exportBtn, new Insets(0, 8, 0, 0));

        headerRow.getChildren().addAll(titleBox, spacer, exportBtn, refreshBtn);
        wrapper.getChildren().add(headerRow);

        // Content section
        VBox content = new VBox(32); // Generous spacing between semester tables
        content.setPadding(new Insets(24, 28, 40, 28));
        content.setFillWidth(true);

        for (SemesterResultTable table : tables) {
            VBox section = new VBox(12);
            
            // Semester Title Block
            HBox titleBar = new HBox(12);
            titleBar.setAlignment(Pos.CENTER_LEFT);
            titleBar.setPadding(new Insets(10, 16, 10, 16));
            titleBar.setStyle("-fx-background-color:#E8F0FE;-fx-background-radius:6;-fx-border-radius:6;-fx-border-color:#D2E3FC;-fx-border-width:1;");
            
            Label semIcon = new Label("📅");
            semIcon.setStyle("-fx-font-size:16px;");
            
            Label semTitle = new Label(table.title());
            semTitle.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#1967D2;");
            
            titleBar.getChildren().addAll(semIcon, semTitle);
            
            // Data Table
            TableView<List<String>> tableView = new TableView<>();
            tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tableView.setStyle("-fx-font-size:12px;");
            
            // Calculate height to avoid internal scrolling if possible
            int estimatedHeight = 35 + (table.data().size() * 25);
            tableView.setPrefHeight(Math.max(100, estimatedHeight + 15));

            for (int i = 0; i < table.headers().size(); i++) {
                final int colIndex = i;
                TableColumn<List<String>, String> column = new TableColumn<>(table.headers().get(i));
                column.setCellValueFactory(param -> {
                    List<String> row = param.getValue();
                    if (colIndex < row.size()) return new SimpleStringProperty(row.get(colIndex));
                    else return new SimpleStringProperty("");
                });
                
                // RESIZING FIX: Set safe preferred AND minimum widths so that the CONSTRAINED_RESIZE_POLICY does not compress columns to unreadable sizes on narrow screens
                String header = table.headers().get(i).toLowerCase();
                if (header.contains("course title") || header.contains("subject")) {
                    column.setPrefWidth(250);
                    column.setMinWidth(200);
                } else if (header.contains("code") || header.contains("credit") || header.contains("grade") || header.contains("marks")) {
                    column.setPrefWidth(80);
                    column.setMinWidth(70);
                } else {
                    column.setMinWidth(70);
                }
                
                tableView.getColumns().add(column);
            }

            ObservableList<List<String>> items = FXCollections.observableArrayList(table.data());
            tableView.setItems(items);
            
            section.getChildren().addAll(titleBar, tableView);
            content.getChildren().add(section);
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        wrapper.getChildren().add(scrollPane);
        return wrapper;
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

    private void exportResultPdf(List<SemesterResultTable> tables) {
        if (tables == null || tables.isEmpty()) {
            showExportError("Export Failed", "No result data available to export.");
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
            new PdfExportService().exportResultCard(studentName, regNo, tables, file);
        } catch (Exception ex) {
            showExportError("Export Failed", ex.getMessage());
        }
    }

    private void showExportError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
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
