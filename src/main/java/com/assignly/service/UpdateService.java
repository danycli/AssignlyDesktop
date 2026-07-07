package com.assignly.service;

import java.awt.Desktop;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.assignly.model.UserPreferences;
import com.assignly.util.AppContext;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateService {
    public static final String CURRENT_VERSION = "v1.1.0";
    private static final String GITHUB_API_LATEST = "https://api.github.com/repos/danycli/AssignlyDesktop/releases/latest";
    private static final String GITHUB_API_ALL = "https://api.github.com/repos/danycli/AssignlyDesktop/releases";
    private static final String WEBSITE_URL = "https://assignly-web.vercel.app/";

    private final AppContext context;
    private final ExecutorService executor;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private Node currentNotification;

    public UpdateService(AppContext context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public void checkForUpdatesSilently(StackPane sceneRoot) {
        UserPreferences prefs = context.preferencesService().loadPreferences();
        if (!prefs.isAutoCheckUpdates()) return;

        executor.submit(() -> performUpdateCheck(sceneRoot, true, prefs));
    }

    public void checkForUpdatesManually(StackPane sceneRoot) {
        UserPreferences prefs = context.preferencesService().loadPreferences();
        Platform.runLater(() -> context.notificationService().showInfo("Checking for updates..."));
        executor.submit(() -> performUpdateCheck(sceneRoot, false, prefs));
    }

    private void performUpdateCheck(StackPane sceneRoot, boolean silent, UserPreferences prefs) {
        try {
            String url = prefs.isNotifyPreReleases() ? GITHUB_API_ALL : GITHUB_API_LATEST;
            
            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    if (!silent) Platform.runLater(() -> context.notificationService().showError("Failed to check for updates. API returned " + response.code()));
                    return;
                }

                String json = response.body().string();
                JsonObject releaseInfo = null;

                if (prefs.isNotifyPreReleases()) {
                    JsonArray releases = gson.fromJson(json, JsonArray.class);
                    if (releases.size() > 0) {
                        releaseInfo = releases.get(0).getAsJsonObject();
                    }
                } else {
                    releaseInfo = gson.fromJson(json, JsonObject.class);
                }

                if (releaseInfo == null) {
                    if (!silent) Platform.runLater(() -> context.notificationService().showError("No release data found."));
                    return;
                }

                String fetchedVersion = releaseInfo.has("tag_name") ? releaseInfo.get("tag_name").getAsString() : null;
                String htmlUrl = releaseInfo.has("html_url") ? releaseInfo.get("html_url").getAsString() : null;
                String body = releaseInfo.has("body") ? releaseInfo.get("body").getAsString() : "A new version of Assignly Desktop is available!";

                if (fetchedVersion == null || htmlUrl == null) {
                    if (!silent) Platform.runLater(() -> context.notificationService().showError("Invalid release data from GitHub."));
                    return;
                }

                if (isNewerVersion(CURRENT_VERSION, fetchedVersion)) {
                    if (silent && fetchedVersion.equals(prefs.getDismissedVersion())) {
                        return; // User already dismissed this version
                    }
                    Platform.runLater(() -> showUpdateNotification(sceneRoot, fetchedVersion, body, htmlUrl, prefs.isOpenWebsiteFirst()));
                } else {
                    if (!silent) Platform.runLater(() -> context.notificationService().showSuccess("Assignly Desktop is up to date (" + CURRENT_VERSION + ")."));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!silent) Platform.runLater(() -> context.notificationService().showError("Network error while checking for updates."));
        }
    }

    private boolean isNewerVersion(String current, String fetched) {
        try {
            String c = current.replaceAll("[^0-9.]", "");
            String f = fetched.replaceAll("[^0-9.]", "");
            String[] cParts = c.split("\\.");
            String[] fParts = f.split("\\.");
            int length = Math.max(cParts.length, fParts.length);
            for (int i = 0; i < length; i++) {
                int cPart = i < cParts.length ? Integer.parseInt(cParts[i]) : 0;
                int fPart = i < fParts.length ? Integer.parseInt(fParts[i]) : 0;
                if (cPart < fPart) return true;
                if (cPart > fPart) return false;
            }
            return false;
        } catch (Exception e) {
            // Fallback to simple string comparison if parsing fails
            return fetched.compareTo(current) > 0;
        }
    }

    private void showUpdateNotification(StackPane sceneRoot, String version, String description, String githubUrl, boolean openWebsiteFirst) {
        if (currentNotification != null && sceneRoot.getChildren().contains(currentNotification)) {
            return; // Already showing
        }

        VBox card = new VBox(12);
        card.setMaxWidth(380);
        card.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 15, 0, 0, 5);");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("✨");
        icon.setStyle("-fx-font-size: 18px;");
        Label title = new Label("Update Available");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label versionBadge = new Label(version);
        versionBadge.setStyle("-fx-background-color: rgba(20, 184, 166, 0.15); -fx-text-fill: -color-accent; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 12;");
        header.getChildren().addAll(icon, title, spacer, versionBadge);

        String cleanDesc = description.split("\\r?\\n")[0];
        if (cleanDesc.length() > 100) cleanDesc = cleanDesc.substring(0, 97) + "...";
        Label descLabel = new Label(cleanDesc);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 13px;");

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(8, 0, 0, 0));

        Button laterBtn = new Button("Later");
        laterBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-text-muted; -fx-cursor: hand; -fx-font-weight: bold;");
        laterBtn.setOnAction(e -> {
            dismissUpdate(version);
            closeNotification(sceneRoot, card);
        });

        Button updateBtn = new Button("Update Now");
        updateBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 8 16;");
        updateBtn.setOnAction(e -> {
            closeNotification(sceneRoot, card);
            executor.submit(() -> launchUpdateUrl(githubUrl, openWebsiteFirst));
        });

        actions.getChildren().addAll(laterBtn, updateBtn);
        card.getChildren().addAll(header, descLabel, actions);

        StackPane.setAlignment(card, Pos.TOP_RIGHT);
        StackPane.setMargin(card, new Insets(24, 24, 0, 0));
        
        card.setTranslateY(-20);
        card.setOpacity(0);
        
        sceneRoot.getChildren().add(card);
        currentNotification = card;

        FadeTransition ft = new FadeTransition(Duration.millis(300), card);
        ft.setToValue(1.0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), card);
        tt.setToY(0);
        
        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.play();
    }

    private void closeNotification(StackPane sceneRoot, Node card) {
        FadeTransition ft = new FadeTransition(Duration.millis(200), card);
        ft.setToValue(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), card);
        tt.setToY(-20);
        
        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setOnFinished(e -> sceneRoot.getChildren().remove(card));
        pt.play();
        currentNotification = null;
    }

    private void dismissUpdate(String version) {
        UserPreferences prefs = context.preferencesService().loadPreferences();
        prefs.setDismissedVersion(version);
        context.preferencesService().savePreferences(prefs);
    }

    private void launchUpdateUrl(String githubUrl, boolean preferWebsite) {
        try {
            String targetUrl = githubUrl;
            if (preferWebsite && isWebsiteHealthy()) {
                targetUrl = WEBSITE_URL;
            }
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(targetUrl));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isWebsiteHealthy() {
        try {
            URL url = new URL(WEBSITE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            int code = conn.getResponseCode();
            return code >= 200 && code < 400;
        } catch (Exception e) {
            return false;
        }
    }
}
