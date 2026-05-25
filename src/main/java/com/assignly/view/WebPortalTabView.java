package com.assignly.view;

import com.assignly.model.UserPreferences;
import com.assignly.util.AppContext;
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

        bar.getChildren().addAll(titleLabel, back, forward, reload, url, spacer);

        // WebView
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        UserPreferences prefs = context.preferencesService().loadPreferences();

        engine.locationProperty().addListener((o, old, u) -> url.setText(u));

        WebHistory history = engine.getHistory();
        back.setOnAction(e -> { if (history.getCurrentIndex() > 0) history.go(-1); });
        forward.setOnAction(e -> { if (history.getCurrentIndex() < history.getEntries().size() - 1) history.go(1); });
        reload.setOnAction(e -> engine.reload());

        String reg = context.getSessionRegistration();
        String pass = context.getSessionPassword();
        if (reg != null && pass != null) {
            context.portalService().enableAutoLogin(engine, reg, pass);
        }

        engine.load("https://sis.cuiatd.edu.pk/" + aspxPage);

        VBox content = new VBox(bar, webView);
        VBox.setVgrow(webView, Priority.ALWAYS);
        root.setCenter(content);
    }

    public BorderPane getRoot() { return root; }
}
