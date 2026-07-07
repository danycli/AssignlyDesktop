package com.assignly;

import com.assignly.database.DatabaseManager;
import com.assignly.service.CredentialManager;
import com.assignly.service.DataCacheService;
import com.assignly.service.PortalService;
import com.assignly.service.PreferencesService;
import com.assignly.util.AppContext;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    private AppContext context;

    @Override
    public void start(Stage stage) {
        DatabaseManager databaseManager = new DatabaseManager(com.assignly.util.AppDirectoryHelper.getDatabaseUrl());
        databaseManager.initializeSchema();

        context = new AppContext(
            stage,
            databaseManager,
            new CredentialManager(databaseManager),
            new PreferencesService(databaseManager),
            new DataCacheService(databaseManager),
            new PortalService()
        );
        stage.initStyle(javafx.stage.StageStyle.DECORATED);
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/com/assignly/images/favicon.png");
            if (iconStream != null) {
                stage.getIcons().add(new javafx.scene.image.Image(iconStream));
            }
        } catch (Exception e) {
            System.err.println("Failed to load stage icon: " + e.getMessage());
        }
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        context.showSplash();
        stage.show();
        com.assignly.util.NativeWindowHelper.applyNativeBorderless(stage, "Assignly Desktop", 32);

        // Pre-warm WebView in background after startup to avoid tab-switch lag
        javafx.animation.PauseTransition prewarm = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        prewarm.setOnFinished(ev -> {
            try {
                new javafx.scene.web.WebView();
            } catch (Throwable ignored) {}
        });
        prewarm.play();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.databaseManager().shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
