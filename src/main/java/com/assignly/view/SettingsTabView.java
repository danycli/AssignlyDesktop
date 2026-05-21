package com.assignly.view;

import com.assignly.model.UserPreferences;
import com.assignly.util.AppContext;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsTabView {
    private final VBox root = new VBox(20);
    private final AppContext context;

    public SettingsTabView(AppContext context) {
        this.context = context;
        buildUI();
    }

    private void buildUI() {
        root.setPadding(new Insets(24, 28, 24, 28));
        root.setFillWidth(true);

        Label heading = new Label("Settings");
        heading.getStyleClass().add("heading-label");

        UserPreferences prefs = context.preferencesService().loadPreferences();

        // Portal
        VBox portalCard = section("Portal");
        CheckBox darkOverlay = new CheckBox("Dark overlay on portal pages");
        darkOverlay.setSelected(prefs.isDarkOverlay());
        darkOverlay.setOnAction(e -> {
            UserPreferences p = context.preferencesService().loadPreferences();
            p.setDarkOverlay(darkOverlay.isSelected());
            context.preferencesService().savePreferences(p);
        });
        Label zoomLabel = new Label("Zoom: " + Math.round(prefs.getZoomLevel() * 100) + "%");
        zoomLabel.getStyleClass().add("label");
        Slider zoom = new Slider(0.5, 2.0, prefs.getZoomLevel());
        zoom.setBlockIncrement(0.1);
        zoom.valueProperty().addListener((o, old, val) -> {
            zoomLabel.setText("Zoom: " + Math.round(val.doubleValue() * 100) + "%");
            UserPreferences p = context.preferencesService().loadPreferences();
            p.setZoomLevel(val.doubleValue());
            context.preferencesService().savePreferences(p);
        });
        portalCard.getChildren().addAll(darkOverlay, zoomLabel, zoom);

        // Account
        VBox accountCard = section("Account");
        CheckBox autoLogin = new CheckBox("Auto-login on startup");
        autoLogin.setSelected(prefs.isAutoLogin());
        autoLogin.setOnAction(e -> {
            UserPreferences p = context.preferencesService().loadPreferences();
            p.setAutoLogin(autoLogin.isSelected());
            context.preferencesService().savePreferences(p);
        });
        Label regInfo = new Label(context.getSessionRegistration() != null
                ? "Signed in as " + context.getSessionRegistration() : "");
        regInfo.getStyleClass().add("muted-text");

        Button clearBtn = new Button("Clear Saved Credentials");
        clearBtn.getStyleClass().add("danger-button");
        clearBtn.setOnAction(e -> {
            context.credentialManager().clearAllCredentials();
            clearBtn.setText("Cleared");
            clearBtn.setDisable(true);
        });
        accountCard.getChildren().addAll(autoLogin, regInfo, clearBtn);

        // Data
        VBox dataCard = section("Data");
        Button clearCache = new Button("Clear Cached Data");
        clearCache.getStyleClass().add("outline-button");
        clearCache.setOnAction(e -> {
            context.dataCacheService().clearAllCaches();
            clearCache.setText("Cleared");
            clearCache.setDisable(true);
        });
        dataCard.getChildren().add(clearCache);

        // About
        VBox aboutCard = section("About");
        Label ver = new Label("Assignly Desktop v1.0.0");
        ver.getStyleClass().add("label");
        Label disc = new Label("Unofficial client · Not affiliated with COMSATS University");
        disc.getStyleClass().add("muted-text");
        aboutCard.getChildren().addAll(ver, disc);

        root.getChildren().addAll(heading, portalCard, accountCard, dataCard, aboutCard);
    }

    private VBox section(String title) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        Label t = new Label(title);
        t.getStyleClass().add("card-title");
        card.getChildren().add(t);
        return card;
    }

    public VBox getRoot() { return root; }
}
