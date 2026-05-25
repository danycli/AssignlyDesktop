package com.assignly.view;

import com.assignly.util.AppContext;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class ScholarshipTabView {
    private final VBox root = new VBox();
    private final AppContext context;
    private javafx.scene.layout.HBox tabBar;
    private javafx.scene.layout.StackPane contentPane;
    private String activeTab = "";

    public ScholarshipTabView(AppContext context) {
        this.context = context;
        buildUI();
        loadTab("status", false);
    }

    private void buildUI() {
        root.setFillWidth(true);
        root.getStyleClass().add("app-root");

        javafx.scene.layout.HBox headerRow = new javafx.scene.layout.HBox();
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(24, 28, 0, 28));

        Label heading = new Label("Scholarship");
        heading.setStyle("-fx-font-size:24px;-fx-font-weight:800;-fx-text-fill: -color-text-main;"); // or heading-label class

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.Button refreshBtn = new javafx.scene.control.Button("🔄");
        refreshBtn.setStyle("-fx-background-color:transparent;-fx-font-size:18px;-fx-cursor:hand;");
        refreshBtn.setOnAction(e -> loadTab(activeTab, true));

        headerRow.getChildren().addAll(heading, spacer, refreshBtn);

        tabBar = new javafx.scene.layout.HBox(4);
        tabBar.setPadding(new Insets(12, 28, 0, 28));
        tabBar.getChildren().addAll(
            tabBtn("Scholarship Status", "status"),
            tabBtn("General Conditions", "conditions")
        );

        contentPane = new javafx.scene.layout.StackPane();
        javafx.scene.layout.VBox.setVgrow(contentPane, javafx.scene.layout.Priority.ALWAYS);
        root.getChildren().addAll(headerRow, tabBar, contentPane);
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

    private void loadTab(String tabKey, boolean forceRefresh) {
        if (!forceRefresh && activeTab.equals(tabKey)) return;
        activeTab = tabKey;

        for (javafx.scene.Node n : tabBar.getChildren()) {
            if (n instanceof javafx.scene.control.Button b) {
                b.setStyle(tabStyle(tabKey.equals(b.getUserData())));
            }
        }

        if (tabKey.equals("status")) {
            setContentAnimated(buildStatusTab(forceRefresh));
        } else if (tabKey.equals("conditions")) {
            setContentAnimated(buildConditionsTab(forceRefresh));
        }
    }

    private javafx.scene.control.Button tabBtn(String label, String id) {
        javafx.scene.control.Button b = new javafx.scene.control.Button(label);
        b.setUserData(id);
        b.setCursor(javafx.scene.Cursor.HAND);
        b.setStyle(tabStyle(false));
        b.setOnAction(e -> loadTab(id, false));
        return b;
    }

    private String tabStyle(boolean active) {
        return active ? 
            "-fx-background-color:#004643;-fx-text-fill:white;-fx-font-weight:600;-fx-padding:8 16;-fx-background-radius:6;-fx-font-size:13px;" : 
            "-fx-background-color: -color-bg-card;-fx-text-fill: -color-text-muted;-fx-font-weight:500;-fx-padding:8 16;-fx-background-radius:6;-fx-font-size:13px;-fx-border-color: -color-border;-fx-border-radius:6;";
    }

    private VBox buildStatusTab(boolean forceRefresh) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24, 28, 24, 28));

        Label title = new Label("Scholarship Status");
        title.setStyle("-fx-font-size:24px;-fx-font-weight:800;-fx-text-fill: -color-text-main;");

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10, title);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setMaxSize(30, 30);
        VBox loader = new VBox(spinner);
        loader.setAlignment(javafx.geometry.Pos.CENTER);

        VBox dataBox = new VBox(10);

        // Load initially from cache or fetch
        dataBox.getChildren().setAll(loader);
        new Thread(() -> {
            String html = null;
            if (!forceRefresh) html = context.dataCacheService().getCachedHtml("scholarship/ViewScholarshipStatuse.aspx").orElse(null);
            if (html == null) html = context.fetchAndCacheHtml("scholarship/ViewScholarshipStatuse.aspx");
            java.util.List<java.util.List<String>> data = context.portalRepository().parseScholarships(html);
            javafx.application.Platform.runLater(() -> populateStatusTable(dataBox, data));
        }).start();

        content.getChildren().addAll(header, dataBox);
        return content;
    }

    private void populateStatusTable(VBox container, java.util.List<java.util.List<String>> data) {
        container.getChildren().clear();
        if (data == null || data.isEmpty()) {
            Label noData = new Label("No scholarship records found.");
            noData.setStyle("-fx-text-fill: -color-text-muted;");
            container.getChildren().add(noData);
            return;
        }

        javafx.scene.control.TableView<java.util.List<String>> table = new javafx.scene.control.TableView<>();
        table.setStyle("-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-radius:8;");
        
        java.util.List<String> headers = data.get(0);
        for (int i = 0; i < headers.size(); i++) {
            final int colIdx = i;
            javafx.scene.control.TableColumn<java.util.List<String>, String> col = new javafx.scene.control.TableColumn<>(headers.get(i));
            col.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().size() > colIdx ? cellData.getValue().get(colIdx) : ""
            ));
            // Provide a decent min width to avoid squashing
            col.setMinWidth(150);
            table.getColumns().add(col);
        }
        
        table.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        
        for (int r = 1; r < data.size(); r++) {
            table.getItems().add(data.get(r));
        }
        
        javafx.scene.layout.VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        container.getChildren().add(table);
    }

    private VBox buildConditionsTab(boolean forceRefresh) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24, 28, 24, 28));

        Label title = new Label("General Scholarship Conditions");
        title.setStyle("-fx-font-size:24px;-fx-font-weight:800;-fx-text-fill: -color-text-main;");

        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setMaxSize(30, 30);
        VBox loader = new VBox(10, spinner, new Label("Downloading & Extracting PDF..."));
        loader.setAlignment(javafx.geometry.Pos.CENTER);
        
        javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
        webView.setVisible(false); // hide until loaded
        javafx.scene.layout.VBox.setVgrow(webView, javafx.scene.layout.Priority.ALWAYS);
        
        // Ensure web view styling matches app theme
        webView.setStyle("-fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8;");

        content.getChildren().addAll(title, loader, webView);

        new Thread(() -> {
            String extractedText = null;
            if (!forceRefresh) {
                extractedText = context.dataCacheService().getCachedHtml("scholarship_conditions_text.txt").orElse(null);
            }
            if (extractedText == null) {
                extractedText = context.portalRepository().downloadAndExtractPdf("scholarship/Genral%20Scholarships%20Conditions%20April%202026.pdf");
                if (extractedText != null) {
                    context.dataCacheService().cacheHtml("scholarship_conditions_text.txt", extractedText);
                }
            }
            
            // Reformat the text to be civilized
            // Replace weird bullet characters (squares, diamonds, empty squares, standard bullets) with an HTML list item trigger
            String formattedList = extractedText
                .replaceAll("[\uF0B7\uF0A7\u25A1\u25A0\u25AA\u25AB\uFFFD\u2022•]+", "</li><li style='margin-bottom: 12px; line-height: 1.6;'>")
                .replaceAll("\\n", " ");
            
            // Clean up the start if it begins with a closing tag
            if (formattedList.startsWith("</li>")) {
                formattedList = formattedList.substring(5);
            }
            
            String htmlContent = "<!DOCTYPE html><html><head><style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; font-size: 15px; color: #334155; padding: 30px; background-color: #ffffff; }" +
                "h2 { color: #0f172a; margin-top: 0; }" +
                "ul { padding-left: 20px; }" +
                "</style></head><body>" +
                "<h2>General Scholarship Conditions</h2>" +
                "<ul>" + (formattedList.contains("</li>") ? formattedList + "</li>" : "<li style='margin-bottom: 12px; line-height: 1.6;'>" + formattedList + "</li>") + "</ul>" +
                "</body></html>";

            javafx.application.Platform.runLater(() -> {
                content.getChildren().remove(loader);
                webView.setVisible(true);
                webView.getEngine().loadContent(htmlContent);
            });
        }).start();

        return content;
    }

    public VBox getRoot() { return root; }
}
