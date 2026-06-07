package com.assignly.view;

import com.assignly.util.AppContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

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

    public ScholarshipTabView(AppContext context, String initialTab) {
        this.context = context;
        buildUI();
        loadTab(initialTab, false);
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

        headerRow.getChildren().addAll(heading, spacer);

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
            "-fx-background-color: -color-accent;-fx-text-fill:white;-fx-font-weight:600;-fx-padding:8 16;-fx-background-radius:6;-fx-font-size:13px;" : 
            "-fx-background-color: -color-bg-card;-fx-text-fill: -color-text-muted;-fx-font-weight:500;-fx-padding:8 16;-fx-background-radius:6;-fx-font-size:13px;-fx-border-color: -color-border;-fx-border-radius:6;";
    }

    private javafx.scene.control.ScrollPane buildStatusTab(boolean forceRefresh) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24, 28, 24, 28));
        content.setFillWidth(true);

        Label title = new Label("Scholarship Status");
        title.setStyle("-fx-font-size:24px;-fx-font-weight:800;-fx-text-fill: -color-text-main;");

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10, title);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.control.ScrollPane spLoader = new javafx.scene.control.ScrollPane(com.assignly.util.ShimmerBuilder.buildScholarshipShimmer());
        spLoader.setFitToWidth(true);
        spLoader.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox dataBox = new VBox(10);
        dataBox.setFillWidth(true);

        // Load initially from cache or fetch
        dataBox.getChildren().setAll(spLoader);
        new Thread(() -> {
            String html = null;
            if (!forceRefresh) html = context.dataCacheService().getCachedHtml("scholarship/ViewScholarshipStatuse.aspx").orElse(null);
            if (html == null) html = context.fetchAndCacheHtml("scholarship/ViewScholarshipStatuse.aspx");
            java.util.List<com.assignly.service.PortalRepository.ScholarshipTable> data = context.portalRepository().parseScholarships(html);
            javafx.application.Platform.runLater(() -> populateStatusTable(dataBox, data));
        }).start();

        content.getChildren().addAll(header, dataBox);

        // RESIZING FIX: Set safe minimum width boundaries on the scholarship cards list
        content.setMinWidth(600);

        // RESIZING FIX: Wrap the entire VBox panel inside a responsive ScrollPane so vertical and horizontal scrollbars appear under small window sizes
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        return sp;
    }

    private void populateStatusTable(VBox container, java.util.List<com.assignly.service.PortalRepository.ScholarshipTable> tables) {
        container.getChildren().clear();
        if (tables == null || tables.isEmpty()) {
            Label noData = new Label("No scholarship records found.");
            noData.setStyle("-fx-text-fill: -color-text-muted;");
            container.getChildren().add(noData);
            return;
        }

        for (com.assignly.service.PortalRepository.ScholarshipTable tableData : tables) {
            Label groupTitle = new Label(tableData.title());
            groupTitle.setStyle("-fx-text-fill: -color-accent; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 8 0 4 0;");
            container.getChildren().add(groupTitle);

            java.util.List<String> headers = tableData.headers();
            
            int scholarshipIdx = findHeaderIndex(headers, "scholarship", "name", "type");
            int dateIdx = findHeaderIndex(headers, "date", "award");
            int sessionIdx = findHeaderIndex(headers, "session", "academic");
            int feeIdx = findHeaderIndex(headers, "fee");
            int mealIdx = findHeaderIndex(headers, "meal");
            int boardingIdx = findHeaderIndex(headers, "boarding");
            int stipendIdx = findHeaderIndex(headers, "stipend");
            int bookIdx = findHeaderIndex(headers, "book", "bookallowance");
            int totalIdx = findHeaderIndex(headers, "total", "amount");
            int chequeIdx = findHeaderIndex(headers, "cheque", "transaction", "chequeno");
            int remarksIdx = findHeaderIndex(headers, "remarks", "remark");

            VBox cardsList = new VBox(16);
            cardsList.setFillWidth(true);

            for (java.util.List<String> row : tableData.data()) {
                String scholarshipName = getValueSafe(row, scholarshipIdx, "Scholarship");
                String academicSession = getValueSafe(row, sessionIdx, "N/A");
                String awardDate = getValueSafe(row, dateIdx, "N/A");
                String totalAmount = getValueSafe(row, totalIdx, "0");
                String feeContribution = getValueSafe(row, feeIdx, "0");
                String mealAllowance = getValueSafe(row, mealIdx, "0");
                String boardingAllowance = getValueSafe(row, boardingIdx, "0");
                String stipend = getValueSafe(row, stipendIdx, "0");
                String bookAllowance = getValueSafe(row, bookIdx, "0");
                String transactionNo = getValueSafe(row, chequeIdx, "N/A");
                String remarks = getValueSafe(row, remarksIdx, "");

                // Build Card VBox
                VBox card = new VBox(0);
                card.setStyle(
                    "-fx-background-color: -color-bg-card;" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-color: -color-border;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 12;" +
                    "-fx-padding: 0;" +
                    "-fx-max-width: Infinity;"
                );
                
                DropShadow shadow = new DropShadow();
                shadow.setColor(Color.rgb(0, 0, 0, 0.04));
                shadow.setRadius(10);
                shadow.setOffsetY(4);
                card.setEffect(shadow);

                // Summary Block
                VBox summaryPane = new VBox(12);
                summaryPane.setPadding(new Insets(20, 24, 20, 24));
                summaryPane.setCursor(Cursor.HAND);

                Label nameLabel = new Label(scholarshipName);
                nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");

                Label sessionLabel = new Label("📅 " + academicSession);
                sessionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted;");
                Label dot = new Label(" • ");
                dot.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted;");
                Label dateLabel = new Label("🗓️ " + awardDate);
                dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted;");
                
                HBox metaBox = new HBox(6, sessionLabel, dot, dateLabel);
                metaBox.setAlignment(Pos.CENTER_LEFT);

                // Status logic
                String status = "Approved";
                if (!transactionNo.equalsIgnoreCase("N/A") && !transactionNo.equalsIgnoreCase("na") && !transactionNo.isBlank()) {
                    status = "Disbursed";
                }
                
                Label statusBadge = new Label(status);
                if (status.equals("Disbursed")) {
                    statusBadge.setStyle("-fx-background-color: #ccfbf1; -fx-text-fill: #0f766e; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 12;");
                } else {
                    statusBadge.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 12;");
                }

                Label amtLabel = new Label(formatCurrency(totalAmount));
                amtLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: -color-accent;");

                Label toggleBtn = new Label("View Details ▼");
                toggleBtn.setStyle("-fx-text-fill: -color-accent; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 4 8; -fx-background-color: rgba(0, 70, 67, 0.05); -fx-background-radius: 6;");

                HBox.setHgrow(metaBox, Priority.ALWAYS);
                
                VBox leftMeta = new VBox(4, nameLabel, metaBox);
                HBox.setHgrow(leftMeta, Priority.ALWAYS);

                HBox rightActions = new HBox(12);
                rightActions.setAlignment(Pos.CENTER_RIGHT);
                rightActions.getChildren().addAll(statusBadge, amtLabel, toggleBtn);

                HBox topRow = new HBox(12);
                topRow.setAlignment(Pos.CENTER_LEFT);
                topRow.getChildren().addAll(leftMeta, rightActions);

                summaryPane.getChildren().add(topRow);

                // Divider line
                Separator div = new Separator();
                div.setStyle("-fx-background-color: -color-border; -fx-opacity: 0.5;");

                // Details Area
                VBox detailBox = new VBox(16);
                detailBox.setPadding(new Insets(20, 24, 20, 24));
                detailBox.setStyle("-fx-background-color: rgba(0, 70, 67, 0.01);");

                GridPane grid = new GridPane();
                grid.setHgap(32);
                grid.setVgap(16);
                
                javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
                col1.setPercentWidth(50);
                javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
                col2.setPercentWidth(50);
                grid.getColumnConstraints().addAll(col1, col2);

                grid.add(createDetailCell("🏦", "Fee Contribution", formatCurrency(feeContribution)), 0, 0);
                grid.add(createDetailCell("🍽️", "Meal Allowance", formatCurrency(mealAllowance)), 0, 1);
                grid.add(createDetailCell("🏠", "Boarding Allowance", formatCurrency(boardingAllowance)), 0, 2);
                
                grid.add(createDetailCell("🪙", "Stipend", formatCurrency(stipend)), 1, 0);
                grid.add(createDetailCell("📚", "Book Allowance", formatCurrency(bookAllowance)), 1, 1);
                grid.add(createDetailCell("🔑", "Transaction / Cheque No", transactionNo), 1, 2);

                if (remarks != null && !remarks.isBlank() && !remarks.equalsIgnoreCase("N/A")) {
                    Label remarksIcon = new Label("📝");
                    remarksIcon.setStyle("-fx-font-size: 18px; -fx-min-width: 24px;");
                    
                    Label remarksTitle = new Label("Remarks");
                    remarksTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500;");
                    
                    Label remarksContent = new Label(remarks);
                    remarksContent.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-main; -fx-font-style: italic;");
                    remarksContent.setWrapText(true);
                    
                    VBox remarksTextGroup = new VBox(2, remarksTitle, remarksContent);
                    HBox remarksCell = new HBox(8, remarksIcon, remarksTextGroup);
                    remarksCell.setAlignment(Pos.TOP_LEFT);
                    remarksCell.setPadding(new Insets(8, 0, 0, 0));
                    
                    detailBox.getChildren().addAll(grid, new Separator(), remarksCell);
                } else {
                    detailBox.getChildren().add(grid);
                }

                // Setup collapsed state
                detailBox.setVisible(false);
                detailBox.setManaged(false);

                String highlight = context.getPendingSearchHighlight();
                if (highlight != null && !highlight.isBlank() && scholarshipName.toLowerCase().contains(highlight.toLowerCase())) {
                    detailBox.setVisible(true);
                    detailBox.setManaged(true);
                    detailBox.setOpacity(1.0);
                    toggleBtn.setText("Hide Details ▲");
                    card.setStyle(
                        "-fx-background-color: rgba(20, 184, 166, 0.18);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: -color-accent;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 0;" +
                        "-fx-max-width: Infinity;"
                    );
                    javafx.application.Platform.runLater(card::requestFocus);
                    context.clearPendingSearchHighlight();
                }

                // Bind divider to detail visibility
                div.visibleProperty().bind(detailBox.visibleProperty());
                div.managedProperty().bind(detailBox.managedProperty());

                // Interactive hover state
                summaryPane.setOnMouseEntered(e -> {
                    card.setStyle(
                        "-fx-background-color: -color-bg-card;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: -color-accent;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 0;" +
                        "-fx-max-width: Infinity;"
                    );
                    shadow.setColor(Color.rgb(0, 0, 0, 0.08));
                    shadow.setRadius(15);
                });
                summaryPane.setOnMouseExited(e -> {
                    card.setStyle(
                        "-fx-background-color: -color-bg-card;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: -color-border;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 0;" +
                        "-fx-max-width: Infinity;"
                    );
                    shadow.setColor(Color.rgb(0, 0, 0, 0.04));
                    shadow.setRadius(10);
                });

                // Toggle click listener
                summaryPane.setOnMouseClicked(e -> {
                    boolean isExpanded = detailBox.isManaged();
                    if (isExpanded) {
                        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), detailBox);
                        ft.setFromValue(1.0);
                        ft.setToValue(0.0);
                        ft.setOnFinished(ev -> {
                            detailBox.setVisible(false);
                            detailBox.setManaged(false);
                        });
                        ft.play();
                        toggleBtn.setText("View Details ▼");
                    } else {
                        detailBox.setManaged(true);
                        detailBox.setVisible(true);
                        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), detailBox);
                        ft.setFromValue(0.0);
                        ft.setToValue(1.0);
                        ft.play();
                        toggleBtn.setText("Hide Details ▲");
                    }
                });

                card.getChildren().addAll(summaryPane, div, detailBox);
                cardsList.getChildren().add(card);
            }
            container.getChildren().add(cardsList);
        }
    }

    private int findHeaderIndex(java.util.List<String> headers, String... keys) {
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).toLowerCase().trim();
            for (String key : keys) {
                if (h.contains(key.toLowerCase())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String getValueSafe(java.util.List<String> row, int index, String fallback) {
        if (index >= 0 && index < row.size()) {
            String val = row.get(index);
            return (val == null || val.isBlank()) ? fallback : val;
        }
        return fallback;
    }

    private String formatCurrency(String amt) {
        if (amt == null || amt.isBlank() || amt.equalsIgnoreCase("n/a") || amt.equals("0") || amt.equals("0.0")) return "Rs. 0";
        try {
            String clean = amt.replaceAll("[^0-9.]", "");
            if (clean.isEmpty()) return "Rs. " + amt;
            double val = Double.parseDouble(clean);
            return String.format("Rs. %,.0f", val);
        } catch (Exception e) {
            return "Rs. " + amt;
        }
    }

    private javafx.scene.Node createDetailCell(String icon, String labelText, String valueText) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 20px; -fx-min-width: 30px;");
        
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500;");
        
        Label val = new Label(valueText);
        val.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-text-main; -fx-font-weight: bold;");
        
        VBox textGroup = new VBox(2, lbl, val);
        HBox cell = new HBox(8, iconLbl, textGroup);
        cell.setAlignment(Pos.CENTER_LEFT);
        return cell;
    }

    private VBox buildConditionsTab(boolean forceRefresh) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24, 28, 24, 28));

        Label title = new Label("General Scholarship Conditions");
        title.setStyle("-fx-font-size:24px;-fx-font-weight:800;-fx-text-fill: -color-text-main;");

        javafx.scene.control.ScrollPane spLoader = new javafx.scene.control.ScrollPane(com.assignly.util.ShimmerBuilder.buildScholarshipShimmer());
        spLoader.setFitToWidth(true);
        spLoader.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        
        javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
        webView.setZoom(1.75 * context.preferencesService().loadPreferences().getZoomLevel());
        webView.setVisible(false); // hide until loaded
        javafx.scene.layout.VBox.setVgrow(webView, javafx.scene.layout.Priority.ALWAYS);
        
        // Ensure web view styling matches app theme
        webView.setStyle("-fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8;");

        content.getChildren().addAll(title, spLoader, webView);

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
                "body { font-family: 'Segoe UI', system-ui, sans-serif; font-size: 26px; color: #1e293b; padding: 40px; background-color: #ffffff; line-height: 1.8; }" +
                "h2 { color: #0f172a; margin-top: 0; font-size: 32px; font-weight: 800; border-bottom: 2px solid #e2e8f0; padding-bottom: 12px; margin-bottom: 24px; }" +
                "ul { padding-left: 24px; margin: 0; }" +
                "li { margin-bottom: 18px; }" +
                "</style></head><body>" +
                "<h2>General Scholarship Conditions</h2>" +
                "<ul>" + (formattedList.contains("</li>") ? formattedList + "</li>" : "<li style='margin-bottom: 18px; line-height: 1.8;'>" + formattedList + "</li>") + "</ul>" +
                "</body></html>";

            javafx.application.Platform.runLater(() -> {
                content.getChildren().remove(spLoader);
                webView.setVisible(true);
                webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        context.portalService().applyDarkOverlay(webView.getEngine(), context.preferencesService().loadPreferences().isDarkOverlay());
                    }
                });
                webView.getEngine().loadContent(htmlContent);
            });
        }).start();

        return content;
    }

    public VBox getRoot() { return root; }
}
