package com.assignly.view;

import com.assignly.service.PortalRepository;
import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

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

    public DashboardTabView(AppContext context) {
        this.context = context;
        buildLoading();
        fetchDashboard();
    }

    private void buildLoading() {
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
        VBox.setVgrow(loading, Priority.ALWAYS);
        root.getChildren().add(loading);
    }

    private void fetchDashboard() {
        new Thread(() -> {
            PortalRepository.DashboardData data = context.portalRepository().fetchDashboard();
            byte[] photoBytes = null;
            if (data != null && data.photoUrl() != null) {
                photoBytes = context.portalRepository().fetchPhotoBytes(data.photoUrl());
            }
            final byte[] photo = photoBytes;
            Platform.runLater(() -> {
                root.getChildren().clear();
                if (data == null) {
                    Label err = new Label("Could not load dashboard. Session may have expired.");
                    err.setStyle("-fx-text-fill: #999; -fx-padding: 40;");
                    root.getChildren().add(err);
                    return;
                }
                buildDashboard(data, photo);
            });
        }).start();
    }

    private void buildDashboard(PortalRepository.DashboardData data, byte[] photoBytes) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(24, 28, 24, 28));
        content.setFillWidth(true);

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
            // Everything else is ignored (date sheet data, missing docs, thesis, etc.)
        }

        // ---- Academic card: photo + info ----
        HBox academicCard = new HBox(24);
        academicCard.getStyleClass().add("card");
        academicCard.setPadding(new Insets(20));
        academicCard.setAlignment(Pos.TOP_LEFT);

        // Photo
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

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().add(scroll);
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
}
