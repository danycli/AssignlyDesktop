package com.assignly.view;

import com.assignly.util.AppContext;
import com.assignly.service.DataCacheService.NotificationEntry;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Optional;

/**
 * NotificationsTabView lists all saved portal and system notifications
 * stored in the database. Provides options to refresh and clear history.
 */
public class NotificationsTabView {
    private final VBox root = new VBox();
    private final AppContext context;
    private StackPane contentPane;

    public NotificationsTabView(AppContext context) {
        this.context = context;
        buildShell();
        loadNotifications();
    }

    private void buildShell() {
        root.setFillWidth(true);
        
        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(24, 28, 16, 28));
        headerRow.setStyle("-fx-background-color: -color-bg-card;-fx-border-color: -color-border;-fx-border-width:0 0 1 0;");

        Label heading = new Label("Notifications");
        heading.setStyle("-fx-font-size:24px;-fx-font-weight:800;-fx-text-fill: -color-text-main;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearAllBtn = new Button("Clear All");
        clearAllBtn.getStyleClass().add("outline-button");
        clearAllBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6 12;");
        clearAllBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Clear Notifications");
            confirm.setHeaderText("Clear All Notifications");
            confirm.setContentText("Are you sure you want to clear your notification history?");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                context.dataCacheService().clearNotifications();
                context.showToastSuccess("Notification history cleared.");
                loadNotifications();
            }
        });

        Button refreshBtn = new Button("↻");
        refreshBtn.getStyleClass().add("ghost-button");
        refreshBtn.setStyle("-fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 0 0 0 8;");
        refreshBtn.setOnAction(e -> loadNotifications());

        headerRow.getChildren().addAll(heading, spacer, clearAllBtn, refreshBtn);
        root.getChildren().add(headerRow);

        contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);
        root.getChildren().add(contentPane);
    }

    private void buildLoading() {
        contentPane.getChildren().clear();
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(28, 28);
        Label msg = new Label("Loading notifications...");
        msg.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 13px;");
        box.getChildren().addAll(spinner, msg);
        contentPane.getChildren().add(box);
    }

    private void loadNotifications() {
        buildLoading();
        new Thread(() -> {
            List<NotificationEntry> entries = context.dataCacheService().getDetailedNotifications();
            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                buildContent(entries);
            });
        }).start();
    }

    private void buildContent(List<NotificationEntry> entries) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(24, 28, 24, 28));
        content.setFillWidth(true);

        if (entries.isEmpty()) {
            VBox emptyCard = new VBox(12);
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(40, 24, 40, 24));
            emptyCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;");

            Label icon = new Label("🔔");
            icon.setStyle("-fx-font-size:42px;");

            Label label = new Label("No Notifications Found");
            label.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:15px;-fx-font-weight:bold;");

            Label desc = new Label("You don't have any notifications cached in your local database. Trigger a Sync Snapshot inside the SIS Web Portal to retrieve live portal updates.");
            desc.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:12px;-fx-text-alignment:center;");
            desc.setWrapText(true);
            desc.setMaxWidth(400);

            emptyCard.getChildren().addAll(icon, label, desc);
            content.getChildren().add(emptyCard);
        } else {
            for (NotificationEntry entry : entries) {
                HBox card = new HBox(16);
                card.getStyleClass().add("card");
                card.setPadding(new Insets(16));
                card.setAlignment(Pos.TOP_LEFT);

                Label iconLabel = new Label("📣");
                iconLabel.setStyle("-fx-font-size: 20px; -fx-padding: 2 0 0 0;");

                VBox textBox = new VBox(4);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                HBox header = new HBox(8);
                header.setAlignment(Pos.CENTER_LEFT);
                
                Label titleLabel = new Label(entry.title());
                titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: -color-accent;");
                
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // Format timestamp
                String timeStr = entry.createdAt();
                if (timeStr != null && timeStr.contains("T")) {
                    timeStr = timeStr.replace("T", " ").substring(0, Math.min(19, timeStr.length()));
                }
                Label timeLabel = new Label(timeStr != null ? timeStr : "");
                timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

                header.getChildren().addAll(titleLabel, spacer, timeLabel);

                Label msgLabel = new Label(entry.message());
                msgLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-main;");
                msgLabel.setWrapText(true);

                textBox.getChildren().addAll(header, msgLabel);
                card.getChildren().addAll(iconLabel, textBox);
                content.getChildren().add(card);
            }
        }

        // RESIZING FIX: Set a safe minimum width on the notifications content to prevent cards from becoming overly narrow or squished
        content.setMinWidth(600);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        // RESIZING FIX: Allow horizontal scrollbar to appear dynamically to prevent clipping or text overflow on small windows
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        contentPane.getChildren().add(scroll);
    }

    public VBox getRoot() { return root; }
}
