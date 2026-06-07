package com.assignly.view;

import com.assignly.model.UserPreferences;
import com.assignly.model.PortalSnapshot;
import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

/**
 * Embedded WebView for portal ASPX pages with minimal controls.
 */
public class WebPortalTabView {
    private final BorderPane root = new BorderPane();
    private final AppContext context;
    private Button syncSnapshot;

    public WebPortalTabView(AppContext context, String aspxPage, String title) {
        this.context = context;

        // Top bar
        HBox bar = new HBox(8);
        bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("top-bar-title");

        Button back = new Button("←");
        back.getStyleClass().add("ghost-button");
        Button forward = new Button("→");
        forward.getStyleClass().add("ghost-button");
        Button reload = new Button("↻");
        reload.getStyleClass().add("ghost-button");

        Label url = new Label("Loading...");
        url.getStyleClass().add("muted-text");
        url.setMaxWidth(350);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Sync Snapshot Button
        syncSnapshot = new Button("📸 Sync Snapshot");
        syncSnapshot.getStyleClass().add("accent-button");
        syncSnapshot.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");

        bar.getChildren().addAll(titleLabel, back, forward, reload, url, spacer, syncSnapshot);

        // WebView
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        UserPreferences prefs = context.preferencesService().loadPreferences();
        webView.setZoom(prefs.getZoomLevel());

        engine.locationProperty().addListener((o, old, u) -> url.setText(u));

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                UserPreferences currentPrefs = context.preferencesService().loadPreferences();
                context.portalService().applyDarkOverlay(engine, currentPrefs.isDarkOverlay());
            }
        });

        WebHistory history = engine.getHistory();
        back.setOnAction(e -> { if (history.getCurrentIndex() > 0) history.go(-1); });
        forward.setOnAction(e -> { if (history.getCurrentIndex() < history.getEntries().size() - 1) history.go(1); });
        reload.setOnAction(e -> engine.reload());

        syncSnapshot.setOnAction(e -> {
            if (engine.getLocation().toLowerCase().contains("login.aspx")) {
                context.notificationService().showError("Please log in before syncing a snapshot.");
                return;
            }
            try {
                PortalSnapshot snapshot = context.portalService().captureSnapshot(engine);
                context.dataCacheService().saveSnapshot(snapshot);
                context.notificationService().showSuccess("Snapshot captured and cached successfully!");

                UserPreferences currentPrefs = context.preferencesService().loadPreferences();
                if (currentPrefs.isNotificationsEnabled() && snapshot.getNotifications() != null && !snapshot.getNotifications().isEmpty()) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Portal Notifications");
                        alert.setHeaderText("New Notifications Synced");

                        VBox alertContent = new VBox(8);
                        alertContent.setPadding(new Insets(10));
                        for (String notif : snapshot.getNotifications()) {
                            Label lbl = new Label("• " + notif);
                            lbl.setWrapText(true);
                            alertContent.getChildren().add(lbl);
                        }

                        ScrollPane scrollPane = new ScrollPane(alertContent);
                        scrollPane.setFitToWidth(true);
                        scrollPane.setPrefHeight(200);
                        scrollPane.setPrefWidth(450);

                        alert.getDialogPane().setContent(scrollPane);
                        alert.showAndWait();
                    });
                }
            } catch (Exception ex) {
                context.notificationService().showError("Failed to sync snapshot: " + ex.getMessage());
            }
        });

        String reg = context.getSessionRegistration();
        String pass = context.getSessionPassword();
        if (reg != null && pass != null) {
            context.portalService().enableAutoLogin(engine, reg, pass);
        }

        engine.load("https://sis.cuiatd.edu.pk/" + aspxPage);

        VBox content = new VBox(bar, webView);
        VBox.setVgrow(webView, Priority.ALWAYS);
        root.setCenter(content);

        context.addConnectivityListener(this::onConnectivityChanged);
    }

    private void onConnectivityChanged(boolean isOnline) {
        Platform.runLater(() -> {
            if (syncSnapshot != null) {
                if (isOnline) {
                    syncSnapshot.setDisable(false);
                    syncSnapshot.setText("📸 Sync Snapshot");
                    syncSnapshot.setTooltip(null);
                } else {
                    syncSnapshot.setDisable(true);
                    syncSnapshot.setText("🔒 Sync Snapshot");
                    syncSnapshot.setTooltip(new Tooltip("Cannot sync snapshot in offline mode."));
                }
            }
        });
    }

    public BorderPane getRoot() { return root; }
}
