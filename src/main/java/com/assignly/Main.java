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
        DatabaseManager databaseManager = new DatabaseManager("jdbc:sqlite:assignly.db");
        databaseManager.initializeSchema();

        context = new AppContext(
            stage,
            databaseManager,
            new CredentialManager(databaseManager),
            new PreferencesService(databaseManager),
            new DataCacheService(databaseManager),
            new PortalService()
        );
        stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        context.showSplash();
        stage.show();
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
