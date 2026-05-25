package com.assignly.view;

import com.assignly.service.PortalRepository;
import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.chart.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * Dashboard – photo + academic info + personal info. Nothing else.
 */
public class DashboardTabView {
    private final VBox root = new VBox();
    private final AppContext context;

    // Only these keys belong on the dashboard (from the profile table)
    private static final Set<String> ACADEMIC_KEYS = Set.of(
            "name", "roll no", "program", "current section",
            "total registered courses", "registered courses", "current advisor"
    );
    private static final Set<String> PERSONAL_KEYS = Set.of(
            "father name", "date of birth", "cnic"
    );

    private StackPane contentPane;

    public DashboardTabView(AppContext context) {
        this.context = context;
        buildShell();
        loadDashboard(false);
    }

    private void buildShell() {
        root.setFillWidth(true);

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(24, 28, 0, 28));

        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size:24px;-fx-font-weight:800;-fx-text-fill: -color-text-main;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄");
        refreshBtn.setStyle("-fx-background-color:transparent;-fx-font-size:18px;-fx-cursor:hand;");
        refreshBtn.setOnAction(e -> {
            contentPane.getChildren().clear();
            loadDashboard(true);
        });

        headerRow.getChildren().addAll(title, spacer, refreshBtn);
        root.getChildren().add(headerRow);

        contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        root.getChildren().add(contentPane);
    }

    private void buildLoading() {
        contentPane.getChildren().clear();
        StackPane loading = new StackPane();
        loading.setStyle("-fx-background-color: #F0EDEC;");
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(28, 28);
        Label msg = new Label("Loading dashboard...");
        msg.setStyle("-fx-text-fill: #888888; -fx-font-size: 13px;");
        box.getChildren().addAll(spinner, msg);
        loading.getChildren().add(box);
        contentPane.getChildren().add(loading);
    }

    private void loadDashboard(boolean forceRefresh) {
        buildLoading();
        new Thread(() -> {
            String html = null;
            boolean isOffline = false;

            if (!forceRefresh) {
                html = context.dataCacheService().getCachedHtml("Dashboard.aspx").orElse(null);
            }

            if (html == null) {
                html = context.fetchAndCacheHtml("Dashboard.aspx");
                if (html == null) {
                    html = context.dataCacheService().getCachedHtml("Dashboard.aspx").orElse(null);
                    isOffline = true;
                }
            }

            PortalRepository.DashboardData data = null;
            if (html != null) {
                data = context.portalRepository().parseDashboard(html);
            }
            
            // --- Fetch GPA History ---
            String resultHtml = null;
            if (!forceRefresh) {
                resultHtml = context.dataCacheService().getCachedHtml("StudentResultCard.aspx").orElse(null);
            }
            if (resultHtml == null && !isOffline) {
                resultHtml = context.fetchAndCacheHtml("StudentResultCard.aspx");
            }
            if (resultHtml == null && isOffline) {
                resultHtml = context.dataCacheService().getCachedHtml("StudentResultCard.aspx").orElse(null);
            }
            List<PortalRepository.GpaHistoryData> gpaHistory = context.portalRepository().parseGpaHistory(resultHtml);
            final List<PortalRepository.GpaHistoryData> finalGpaHistory = gpaHistory;

            byte[] photoBytes = null;
            if (data != null && data.photoUrl() != null) {
                photoBytes = context.portalRepository().fetchPhotoBytes(data.photoUrl());
            }
            final byte[] photo = photoBytes;
            final boolean finalOffline = isOffline;
            final PortalRepository.DashboardData finalData = data;
            
            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                if (finalData == null) {
                    Label err = new Label("Could not load dashboard. No offline data available.");
                    err.setStyle("-fx-text-fill: #999; -fx-padding: 40;");
                    contentPane.getChildren().add(err);
                    return;
                }
                buildDashboard(finalData, photo, finalOffline, finalGpaHistory);
            });
        }).start();
    }

    private void buildDashboard(PortalRepository.DashboardData data, byte[] photoBytes, boolean isOffline, List<PortalRepository.GpaHistoryData> gpaHistory) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(16, 28, 24, 28));
        content.setFillWidth(true);

        if (isOffline) {
            content.getChildren().add(buildOfflineBanner());
        }

        // Split into academic / personal using whitelist
        Map<String, String> academic = new LinkedHashMap<>();
        Map<String, String> personal = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : data.studentInfo().entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            String keyLower = key.toLowerCase().trim();

            if (val.isBlank() || val.equalsIgnoreCase("NA")) continue;

            if (ACADEMIC_KEYS.contains(keyLower)) {
                academic.put(key, val);
            } else if (PERSONAL_KEYS.contains(keyLower)) {
                personal.put(key, val);
            }
        }

        // ---- Academic card: photo + info ----
        HBox academicCard = new HBox(24);
        academicCard.getStyleClass().add("card");
        academicCard.setPadding(new Insets(20));
        academicCard.setAlignment(Pos.TOP_LEFT);

        VBox photoBox = new VBox();
        photoBox.setAlignment(Pos.TOP_CENTER);
        photoBox.setMinWidth(100);
        if (photoBytes != null && photoBytes.length > 0) {
            Image img = new Image(new ByteArrayInputStream(photoBytes), 90, 90, true, true);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(90);
            iv.setFitHeight(90);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            Circle clip = new Circle(45, 45, 45);
            iv.setClip(clip);
            photoBox.getChildren().add(iv);
        } else {
            Label placeholder = new Label("👤");
            placeholder.setStyle("-fx-font-size: 40px; -fx-text-fill: #ccc;");
            placeholder.setMinSize(90, 90);
            placeholder.setAlignment(Pos.CENTER);
            photoBox.getChildren().add(placeholder);
        }

        VBox academicSection = new VBox(4);
        Label academicTitle = new Label("Academic Information");
        academicTitle.getStyleClass().add("card-title");
        GridPane academicGrid = buildInfoGrid(academic);
        academicSection.getChildren().addAll(academicTitle, academicGrid);
        HBox.setHgrow(academicSection, Priority.ALWAYS);

        academicCard.getChildren().addAll(photoBox, academicSection);
        content.getChildren().add(academicCard);

        // ---- Personal card ----
        if (!personal.isEmpty()) {
            VBox personalCard = new VBox(8);
            personalCard.getStyleClass().add("card");
            personalCard.setPadding(new Insets(16, 20, 16, 20));

            Label personalTitle = new Label("Personal Information");
            personalTitle.getStyleClass().add("card-title");
            GridPane personalGrid = buildInfoGrid(personal);
            personalCard.getChildren().addAll(personalTitle, personalGrid);
            content.getChildren().add(personalCard);
        }

        // ---- Analytics Row ----
        HBox analyticsRow = new HBox(20);
        analyticsRow.setFillHeight(true);
        
        VBox gpaCard = buildGpaChartCard(gpaHistory);
        HBox.setHgrow(gpaCard, Priority.ALWAYS);
        
        VBox attendanceCard = buildAttendanceDonutCard(data.attendanceOverall());
        HBox.setHgrow(attendanceCard, Priority.ALWAYS);
        
        analyticsRow.getChildren().addAll(gpaCard, attendanceCard);
        content.getChildren().add(analyticsRow);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        contentPane.getChildren().add(scroll);
    }

    private GridPane buildInfoGrid(Map<String, String> data) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        grid.setPadding(new Insets(6, 0, 0, 0));
        int row = 0;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            Label keyLabel = new Label(entry.getKey());
            keyLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888888; -fx-font-weight: 500;");
            keyLabel.setMinWidth(160);

            Label valLabel = new Label(entry.getValue());
            valLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1a1a1a; -fx-font-weight: 600;");
            valLabel.setWrapText(true);

            grid.add(keyLabel, 0, row);
            grid.add(valLabel, 1, row);
            row++;
        }
        return grid;
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

    // ==================== ANALYTICS CHARTS ====================

    private VBox buildGpaChartCard(List<PortalRepository.GpaHistoryData> gpaHistory) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        card.setMinHeight(320);

        Label title = new Label("📈  GPA Progression");
        title.getStyleClass().add("card-title");
        title.setStyle("-fx-font-size:15px;-fx-font-weight:800;");

        if (gpaHistory == null || gpaHistory.isEmpty()) {
            Label empty = new Label("No GPA history available yet.\nResult data will appear here once published.");
            empty.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12px;-fx-wrap-text:true;-fx-padding:30 0 0 0;");
            empty.setAlignment(Pos.CENTER);
            empty.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().addAll(title, empty);
            VBox.setVgrow(empty, Priority.ALWAYS);
            return card;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Semester");
        xAxis.setStyle("-fx-tick-label-fill:#64748b;-fx-font-size:10px;");

        NumberAxis yAxis = new NumberAxis(0, 4.0, 0.5);
        yAxis.setLabel("GPA");
        yAxis.setStyle("-fx-tick-label-fill:#64748b;-fx-font-size:10px;");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setLegendVisible(true);
        lineChart.setAnimated(true);
        lineChart.setCreateSymbols(true);
        lineChart.setMinHeight(240);
        lineChart.setStyle("-fx-background-color:transparent;");

        XYChart.Series<String, Number> sgpaSeries = new XYChart.Series<>();
        sgpaSeries.setName("SGPA");

        XYChart.Series<String, Number> cgpaSeries = new XYChart.Series<>();
        cgpaSeries.setName("CGPA");

        int semNum = 1;
        for (PortalRepository.GpaHistoryData entry : gpaHistory) {
            // Use a short label: "Sem 1", "Sem 2", etc.
            String label = "Sem " + semNum++;
            sgpaSeries.getData().add(new XYChart.Data<>(label, entry.sgpa()));
            cgpaSeries.getData().add(new XYChart.Data<>(label, entry.cgpa()));
        }

        lineChart.getData().add(sgpaSeries);
        lineChart.getData().add(cgpaSeries);

        // Style the chart lines with Cyprus Green tones
        lineChart.setStyle("-fx-background-color:transparent;");
        lineChart.lookup(".chart-plot-background").setStyle("-fx-background-color:transparent;");

        VBox.setVgrow(lineChart, Priority.ALWAYS);
        card.getChildren().addAll(title, lineChart);

        // Apply custom colors after chart is laid out
        lineChart.applyCss();
        lineChart.layout();
        styleChartSeries(lineChart);

        return card;
    }

    private void styleChartSeries(LineChart<String, Number> chart) {
        // SGPA = Cyprus Green (#004643), CGPA = Teal (#14b8a6)
        try {
            javafx.scene.Node sgpaLine = chart.lookup(".default-color0.chart-series-line");
            if (sgpaLine != null) sgpaLine.setStyle("-fx-stroke:#004643;-fx-stroke-width:3px;");
            javafx.scene.Node cgpaLine = chart.lookup(".default-color1.chart-series-line");
            if (cgpaLine != null) cgpaLine.setStyle("-fx-stroke:#14b8a6;-fx-stroke-width:3px;");

            for (javafx.scene.Node node : chart.lookupAll(".default-color0.chart-line-symbol")) {
                node.setStyle("-fx-background-color:#004643,white;-fx-background-insets:0,2;-fx-background-radius:5px;-fx-padding:5px;");
            }
            for (javafx.scene.Node node : chart.lookupAll(".default-color1.chart-line-symbol")) {
                node.setStyle("-fx-background-color:#14b8a6,white;-fx-background-insets:0,2;-fx-background-radius:5px;-fx-padding:5px;");
            }
        } catch (Exception ignored) {
            // Styling is cosmetic; don't crash if CSS lookup fails
        }
    }

    private VBox buildAttendanceDonutCard(Map<String, Double> attendanceData) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        card.setMinHeight(320);

        Label title = new Label("🎯  Attendance Overview");
        title.getStyleClass().add("card-title");
        title.setStyle("-fx-font-size:15px;-fx-font-weight:800;");

        if (attendanceData == null || attendanceData.isEmpty()) {
            Label empty = new Label("No attendance data available.\nAttendance stats will appear here once available.");
            empty.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12px;-fx-wrap-text:true;-fx-padding:30 0 0 0;");
            empty.setAlignment(Pos.CENTER);
            empty.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().addAll(title, empty);
            VBox.setVgrow(empty, Priority.ALWAYS);
            return card;
        }

        // Calculate overall average
        double totalPct = 0;
        for (double v : attendanceData.values()) totalPct += v;
        double avgPct = totalPct / attendanceData.size();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : attendanceData.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        PieChart pieChart = new PieChart(pieData);
        pieChart.setLabelsVisible(false);
        pieChart.setLegendVisible(true);
        pieChart.setAnimated(true);
        pieChart.setMinHeight(220);
        pieChart.setMaxHeight(260);
        pieChart.setStyle("-fx-background-color:transparent;");

        // Donut center overlay
        StackPane donutPane = new StackPane();

        VBox centerLabel = new VBox(2);
        centerLabel.setAlignment(Pos.CENTER);
        centerLabel.setMouseTransparent(true);

        Label avgValue = new Label(String.format("%.0f%%", avgPct));
        avgValue.setStyle("-fx-font-size:26px;-fx-font-weight:800;-fx-text-fill:#004643;");

        Label avgDesc = new Label("Average");
        avgDesc.setStyle("-fx-font-size:11px;-fx-font-weight:600;-fx-text-fill:#94a3b8;");

        centerLabel.getChildren().addAll(avgValue, avgDesc);

        // The background circle for the donut hole
        javafx.scene.shape.Circle donutHole = new javafx.scene.shape.Circle(55);
        donutHole.setStyle("-fx-fill:white;-fx-stroke:#e2e8f0;-fx-stroke-width:1;");
        // Use CSS class for dark mode compatibility
        donutHole.getStyleClass().add("card");

        donutPane.getChildren().addAll(pieChart, donutHole, centerLabel);

        // Add hover tooltips to each slice
        for (PieChart.Data d : pieData) {
            javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(
                    d.getName() + ": " + String.format("%.1f%%", d.getPieValue()));
            tooltip.setStyle("-fx-font-size:12px;");
            javafx.scene.control.Tooltip.install(d.getNode(), tooltip);

            // Hover effect: slight scale on the slice
            d.getNode().setOnMouseEntered(e -> d.getNode().setStyle("-fx-opacity:0.85;-fx-cursor:hand;"));
            d.getNode().setOnMouseExited(e -> d.getNode().setStyle("-fx-opacity:1;"));
        }

        VBox.setVgrow(donutPane, Priority.ALWAYS);
        card.getChildren().addAll(title, donutPane);
        return card;
    }
}
