package com.assignly.view;

import com.assignly.model.UserPreferences;
import com.assignly.service.PortalRepository;
import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

public class SettingsTabView {
    private final BorderPane root = new BorderPane();
    private final AppContext context;
    private StackPane contentArea;
    private Button syncBtn;
    private Button saveProfileBtn;
    private Button changePassBtn;
    private final java.util.function.Consumer<Boolean> connectivityListener = this::onConnectivityChanged;

    public SettingsTabView(AppContext context) {
        this.context = context;
        buildUI();
        context.addConnectivityListener(connectivityListener);
    }

    private void buildUI() {
        root.setStyle("-fx-background-color: -color-bg-main;");

        // Sidebar for sub-tabs
        VBox sidebar = new VBox(4);
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: -color-bg-card;-fx-border-color: -color-border;-fx-border-width:0 1 0 0;");
        sidebar.setPadding(new Insets(24, 12, 24, 12));

        Label title = new Label("Settings");
        title.setStyle("-fx-font-size:20px;-fx-font-weight:800;-fx-text-fill: -color-text-main;");
        title.setPadding(new Insets(0, 0, 16, 8));
        sidebar.getChildren().add(title);

        contentArea = new StackPane();
        contentArea.setPadding(new Insets(24));

        Button btnGeneral = createSubTabBtn("⚙ General");
        Button btnProfile = createSubTabBtn("👤 Profile");
        Button btnPassword = createSubTabBtn("🔑 Change Password");
        Button btnHistory = createSubTabBtn("📜 Login History");
        Button btnUpdates = createSubTabBtn("☁ Updates");

        sidebar.getChildren().addAll(btnGeneral, btnProfile, btnPassword, btnHistory, btnUpdates);

        btnGeneral.setOnAction(e -> { setActive(sidebar, btnGeneral); loadGeneral(); });
        btnProfile.setOnAction(e -> { setActive(sidebar, btnProfile); loadProfile(); });
        btnPassword.setOnAction(e -> { setActive(sidebar, btnPassword); loadPassword(); });
        btnHistory.setOnAction(e -> { setActive(sidebar, btnHistory); loadHistory(); });
        btnUpdates.setOnAction(e -> { setActive(sidebar, btnUpdates); loadUpdates(); });

        root.setLeft(sidebar);
        root.setCenter(contentArea);

        // Load default or pending subtab
        if ("profile".equals(context.getPendingSettingsSubTab())) {
            context.clearPendingSettingsSubTab();
            btnProfile.fire();
        } else {
            btnGeneral.fire();
        }
    }

    private Button createSubTabBtn(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("settings-subtab-button");
        return btn;
    }

    private void setActive(VBox sidebar, Button activeBtn) {
        for (javafx.scene.Node n : sidebar.getChildren()) {
            if (n instanceof Button b) {
                b.getStyleClass().remove("settings-subtab-button-active");
            }
        }
        activeBtn.getStyleClass().add("settings-subtab-button-active");
    }

    // --- GENERAL TAB ---
    private void loadGeneral() {
        VBox content = new VBox(24);
        content.setFillWidth(true);

        Label subTitle = new Label("Manage your account, appearance, and application data");
        subTitle.setStyle("-fx-font-size:13px;-fx-text-fill: -color-text-muted;-fx-font-weight:600;");

        FlowPane grid = new FlowPane();
        grid.setHgap(24);
        grid.setVgap(24);
        grid.getChildren().addAll(buildAppearanceCard(), buildQuickActionsHub(), buildSecurityCard(), buildDataCard());

        content.getChildren().addAll(subTitle, grid);
        
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        contentArea.getChildren().setAll(sp);
    }

    private VBox buildQuickActionsHub() {
        VBox box = new VBox(12);
        
        Label sectionTitle = new Label("Quick Actions");
        sectionTitle.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill: -color-text-muted;");
        
        HBox actionsRow = new HBox(16);
        
        // Force Sync Button
        syncBtn = new Button("🔄 Force Sync Data");
        syncBtn.getStyleClass().add("btn-secondary");
        applyOfflineStateIfOffline(syncBtn, "Force Sync Data", "🔄 ");
        syncBtn.setOnAction(e -> {
            if (!context.isOnline()) {
                context.notificationService().showError("Cannot force sync in offline mode.");
                return;
            }
            context.dataCacheService().clearAllCaches();
            syncBtn.setText("✅ Synced!");
            syncBtn.setDisable(true);
            context.notificationService().showSuccess("Cache cleared. Data will refresh on next load.");
        });

        // Quick Logout Button
        Button logoutBtn = new Button("🚪 Secure Logout");
        logoutBtn.getStyleClass().add("btn-danger");
        logoutBtn.setOnAction(e -> {
            context.credentialManager().clearRememberMe();
            try {
                UserPreferences p = context.preferencesService().loadPreferences();
                p.setAutoLogin(false);
                context.preferencesService().savePreferences(p);
            } catch (Exception ignored) {}
            context.clearSessionCredentials();
            context.showLoginScreen();
        });

        actionsRow.getChildren().addAll(syncBtn, logoutBtn);
        box.getChildren().addAll(sectionTitle, actionsRow);
        return box;
    }

    private VBox buildAppearanceCard() {
        VBox card = createBaseCard("Appearance", "🎨");
        
        Label desc = new Label("Customize the application's look and feel.");
        desc.setStyle("-fx-text-fill: -color-text-muted;-fx-wrap-text:true;");

        HBox themeRow = new HBox(12);
        themeRow.setAlignment(Pos.CENTER_LEFT);
        
        Label themeLbl = new Label("Dark Mode");
        themeLbl.setStyle("-fx-font-weight:bold;-fx-text-fill: -color-text-main;");

        CheckBox darkModeToggle = new CheckBox();
        darkModeToggle.setStyle("-fx-cursor:hand;");
        UserPreferences prefs = context.preferencesService().loadPreferences();
        darkModeToggle.setSelected("DARK".equalsIgnoreCase(prefs.getTheme()));

        darkModeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            prefs.setTheme(newVal ? "DARK" : "LIGHT");
            context.preferencesService().savePreferences(prefs);
            context.applyTheme(context.stage().getScene());
        });

        themeRow.getChildren().addAll(themeLbl, darkModeToggle);

        HBox notifRow = new HBox(12);
        notifRow.setAlignment(Pos.CENTER_LEFT);
        
        Label notifLbl = new Label("Enable Notifications");
        notifLbl.setStyle("-fx-font-weight:bold;-fx-text-fill: -color-text-main;");

        CheckBox notifToggle = new CheckBox();
        notifToggle.setStyle("-fx-cursor:hand;");
        notifToggle.setSelected(prefs.isNotificationsEnabled());

        notifToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            prefs.setNotificationsEnabled(newVal);
            context.preferencesService().savePreferences(prefs);
            context.notificationService().showSuccess(newVal ? "Notifications enabled" : "Notifications disabled");
        });

        notifRow.getChildren().addAll(notifLbl, notifToggle);

        card.getChildren().addAll(desc, themeRow, notifRow);
        return card;
    }

    private VBox createBaseCard(String title, String icon) {
        VBox card = new VBox(16);
        card.setPrefWidth(350);
        card.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius:8;-fx-border-color: -color-border;-fx-border-width:1;-fx-border-radius:8;-fx-padding:20;-fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.02),10,0,0,2);");
        
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size:16px;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
        
        header.getChildren().addAll(iconLabel, titleLabel);
        card.getChildren().add(header);
        
        return card;
    }

    private VBox buildSecurityCard() {
        VBox card = createBaseCard("Account Security", "🛡");
        UserPreferences prefs = context.preferencesService().loadPreferences();

        Label regInfo = new Label(context.getSessionRegistration() != null
                ? "Signed in as: " + context.getSessionRegistration() : "Not signed in");
        regInfo.getStyleClass().add("status-success");
        regInfo.setStyle("-fx-font-size:13px;");

        CheckBox autoLogin = new CheckBox("Auto-login on app startup");
        autoLogin.setStyle("-fx-font-size:13px;-fx-text-fill: -color-text-main;");
        autoLogin.setSelected(prefs.isAutoLogin());
        autoLogin.setOnAction(e -> {
            UserPreferences p = context.preferencesService().loadPreferences();
            if (autoLogin.isSelected() && !context.credentialManager().hasRememberedCredentials()) {
                context.notificationService().showError("Auto-login requires saved credentials. Please log in with 'Remember Me' enabled.");
                autoLogin.setSelected(false);
                return;
            }
            p.setAutoLogin(autoLogin.isSelected());
            context.preferencesService().savePreferences(p);
            context.notificationService().showSuccess("Auto-login preference saved.");
        });

        Button clearBtn = new Button("Wipe Saved Credentials");
        clearBtn.getStyleClass().add("btn-danger-outline");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> {
            context.credentialManager().clearAllCredentials();
            
            // Also update preferences
            try {
                UserPreferences p = context.preferencesService().loadPreferences();
                p.setAutoLogin(false);
                context.preferencesService().savePreferences(p);
            } catch (Exception ignored) {}
            
            // Update UI components
            autoLogin.setSelected(false);
            
            clearBtn.setText("Credentials Wiped");
            clearBtn.setDisable(true);
            context.notificationService().showSuccess("Saved credentials wiped from database.");
        });

        card.getChildren().addAll(regInfo, autoLogin, clearBtn);
        return card;
    }

    private VBox buildDataCard() {
        VBox card = createBaseCard("Data Management", "💾");

        Label cacheDesc = new Label("Clear your local cache to resolve sync issues or free up disk space.");
        cacheDesc.setStyle("-fx-font-size:12px;-fx-text-fill: -color-text-muted;");
        cacheDesc.setWrapText(true);

        Button clearCache = new Button("Clear Local Cache");
        clearCache.getStyleClass().add("btn-muted");
        clearCache.setMaxWidth(Double.MAX_VALUE);
        clearCache.setOnAction(e -> {
            context.dataCacheService().clearAllCaches();
            clearCache.setText("Cache Cleared Successfully");
            clearCache.setDisable(true);
            context.notificationService().showSuccess("Local cache cleared.");
        });
        
        card.getChildren().addAll(cacheDesc, clearCache);
        return card;
    }

    // --- PROFILE TAB ---
    private void loadProfile() {
        VBox content = new VBox(20);
        content.setMaxWidth(450);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12;");
        
        Label title = new Label("Update Profile Information");
        title.setStyle("-fx-font-size:18px;-fx-font-weight:800;-fx-text-fill: -color-text-main;");
        
        Label info = new Label("Loading profile data...");
        info.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size:13px; -fx-font-weight:500;");

        HBox cellBox = new HBox(8);
        cellBox.setAlignment(Pos.CENTER_LEFT);
        
        TextField networkField = new TextField();
        networkField.setPromptText("03XX");
        networkField.setPrefWidth(75);
        networkField.setStyle("-fx-background-color: -color-bg-main; -fx-border-color: -color-border; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: -color-text-main; -fx-padding: 8;");
        
        Label dash = new Label("-");
        dash.setStyle("-fx-text-fill: -color-text-muted; -fx-font-weight: bold;");
        
        TextField numberField = new TextField();
        numberField.setPromptText("XXXXXXX");
        HBox.setHgrow(numberField, Priority.ALWAYS);
        numberField.setMaxWidth(Double.MAX_VALUE);
        numberField.setStyle("-fx-background-color: -color-bg-main; -fx-border-color: -color-border; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: -color-text-main; -fx-padding: 8;");
        
        cellBox.getChildren().addAll(networkField, dash, numberField);

        TextField emailField = new TextField();
        emailField.setPromptText("Email Address");
        emailField.setMaxWidth(Double.MAX_VALUE);
        emailField.setStyle("-fx-background-color: -color-bg-main; -fx-border-color: -color-border; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: -color-text-main; -fx-padding: 8;");

        saveProfileBtn = new Button("Save Profile");
        saveProfileBtn.getStyleClass().add("btn-primary");
        saveProfileBtn.setMaxWidth(Double.MAX_VALUE);
        saveProfileBtn.setDisable(true);
        saveProfileBtn.setCursor(javafx.scene.Cursor.HAND);
        saveProfileBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 16; -fx-background-radius: 6;");
        applyOfflineStateIfOffline(saveProfileBtn, "Save Profile", "");

        Label statusLbl = new Label("");
        statusLbl.setWrapText(true);
        statusLbl.getStyleClass().add("status-success");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPadding(new Insets(8, 0, 8, 0));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(110);
        col1.setPrefWidth(110);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        Label cellLabel = new Label("Cell Number:");
        cellLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -color-text-main;");
        grid.add(cellLabel, 0, 0);
        grid.add(cellBox, 1, 0);

        Label emailLabel = new Label("Email Address:");
        emailLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -color-text-main;");
        grid.add(emailLabel, 0, 1);
        grid.add(emailField, 1, 1);

        content.getChildren().addAll(title, info, grid, saveProfileBtn, statusLbl);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        contentArea.getChildren().setAll(sp);

        new Thread(() -> {
            String html = context.dataCacheService().getCachedHtml("AddCellEmailInfo.aspx").orElse(null);
            if (html == null) html = context.fetchAndCacheHtml("AddCellEmailInfo.aspx");
            PortalRepository.ProfileInfo prof = context.portalRepository().parseProfileInfo(html);
            Platform.runLater(() -> {
                if (prof != null) {
                    networkField.setText(prof.cellNetwork());
                    numberField.setText(prof.cellNumber());
                    emailField.setText(prof.email());
                    info.setText("Update your contact details below:");
                    saveProfileBtn.setDisable(!context.isOnline());
                } else {
                    info.setText("Failed to load profile data.");
                }
            });
        }).start();

        saveProfileBtn.setOnAction(e -> {
            if (!context.isOnline()) {
                setStatusError(statusLbl, "Cannot save profile in offline mode.");
                return;
            }
            saveProfileBtn.setText("Saving...");
            saveProfileBtn.setDisable(true);
            new Thread(() -> {
                String msg = context.portalRepository().updateProfile(networkField.getText(), numberField.getText(), emailField.getText());
                context.fetchAndCacheHtml("AddCellEmailInfo.aspx");
                Platform.runLater(() -> {
                    if (msg.toLowerCase().contains("success") || msg.toLowerCase().contains("updated") || msg.toLowerCase().contains("saved")) {
                        setStatusSuccess(statusLbl, msg);
                    } else {
                        setStatusError(statusLbl, msg);
                    }
                    saveProfileBtn.setText("Save Profile");
                    saveProfileBtn.setDisable(!context.isOnline());
                });
            }).start();
        });
    }

    // --- CHANGE PASSWORD TAB ---
    private void loadPassword() {
        VBox content = new VBox(20);
        content.setMaxWidth(450);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12;");

        Label title = new Label("Change Password");
        title.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
        
        Label rules = new Label("Loading password policy...");
        rules.setWrapText(true);
        rules.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:13px;");

        PasswordField oldPass = new PasswordField();
        oldPass.setPromptText("Current Password");
        oldPass.setMaxWidth(Double.MAX_VALUE);
        oldPass.setStyle("-fx-background-color: -color-bg-main; -fx-border-color: -color-border; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: -color-text-main; -fx-padding: 8;");

        PasswordField newPass = new PasswordField();
        newPass.setPromptText("New Password");
        newPass.setMaxWidth(Double.MAX_VALUE);
        newPass.setStyle("-fx-background-color: -color-bg-main; -fx-border-color: -color-border; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: -color-text-main; -fx-padding: 8;");

        PasswordField confPass = new PasswordField();
        confPass.setPromptText("Confirm New Password");
        confPass.setMaxWidth(Double.MAX_VALUE);
        confPass.setStyle("-fx-background-color: -color-bg-main; -fx-border-color: -color-border; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: -color-text-main; -fx-padding: 8;");

        changePassBtn = new Button("Change Password");
        changePassBtn.getStyleClass().add("btn-primary");
        changePassBtn.setMaxWidth(Double.MAX_VALUE);
        changePassBtn.setCursor(javafx.scene.Cursor.HAND);
        changePassBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 16; -fx-background-radius: 6;");
        applyOfflineStateIfOffline(changePassBtn, "Change Password", "");

        Label statusLbl = new Label("");
        statusLbl.setWrapText(true);
        statusLbl.getStyleClass().add("status-success");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setPadding(new Insets(8, 0, 8, 0));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(120);
        col1.setPrefWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        Label oldLabel = new Label("Current Password:");
        oldLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -color-text-main;");
        grid.add(oldLabel, 0, 0);
        grid.add(oldPass, 1, 0);

        Label newLabel = new Label("New Password:");
        newLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -color-text-main;");
        grid.add(newLabel, 0, 1);
        grid.add(newPass, 1, 1);

        Label confLabel = new Label("Confirm Password:");
        confLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -color-text-main;");
        grid.add(confLabel, 0, 2);
        grid.add(confPass, 1, 2);

        content.getChildren().addAll(title, rules, grid, changePassBtn, statusLbl);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        contentArea.getChildren().setAll(sp);

        new Thread(() -> {
            String html = context.dataCacheService().getCachedHtml("changepassword.aspx").orElse(null);
            if (html == null) html = context.fetchAndCacheHtml("changepassword.aspx");
            String policy = context.portalRepository().extractPasswordRules(html);
            Platform.runLater(() -> rules.setText(policy));
        }).start();

        changePassBtn.setOnAction(e -> {
            if (!context.isOnline()) {
                setStatusError(statusLbl, "Cannot change password in offline mode.");
                return;
            }
            if (!newPass.getText().equals(confPass.getText())) {
                setStatusError(statusLbl, "New passwords do not match.");
                return;
            }
            changePassBtn.setText("Updating...");
            changePassBtn.setDisable(true);
            new Thread(() -> {
                String msg = context.portalRepository().changePassword(oldPass.getText(), newPass.getText(), confPass.getText());
                Platform.runLater(() -> {
                    if (msg.toLowerCase().contains("success") || msg.toLowerCase().contains("changed") || msg.toLowerCase().contains("completed") || msg.toLowerCase().contains("successfully")) {
                        setStatusSuccess(statusLbl, msg);
                        // Force a logout due to credentials change
                        context.credentialManager().clearRememberMe();
                        context.clearSessionCredentials();
                        
                        context.notificationService().showInfo("Password Changed", "Your password has been changed. Please log in again with your new password.");
                        
                        context.showLoginScreen();
                    } else {
                        setStatusError(statusLbl, msg);
                        changePassBtn.setText("Change Password");
                        changePassBtn.setDisable(!context.isOnline());
                    }
                    oldPass.clear(); newPass.clear(); confPass.clear();
                });
            }).start();
        });
    }

    // --- LOGIN HISTORY TAB ---
    private void loadHistory() {
        VBox content = new VBox(20);
        content.setFillWidth(true);

        Label title = new Label("Login History");
        title.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
        
        VBox historyList = new VBox(10);
        historyList.getChildren().add(new Label("Loading history..."));

        content.getChildren().addAll(title, historyList);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        contentArea.getChildren().setAll(sp);

        new Thread(() -> {
            String html = context.dataCacheService().getCachedHtml("LoginHistory.aspx").orElse(null);
            if (html == null) html = context.fetchAndCacheHtml("LoginHistory.aspx");
            List<PortalRepository.LoginHistoryEntry> history = context.portalRepository().parseLoginHistory(html);
            
            Platform.runLater(() -> {
                historyList.getChildren().clear();
                if (history == null || history.isEmpty()) {
                    historyList.getChildren().add(new Label("No login history found."));
                    return;
                }
                for (PortalRepository.LoginHistoryEntry entry : history) {
                    VBox card = new VBox(4);
                    card.setStyle("-fx-background-color: -color-bg-card;-fx-border-color: -color-border;-fx-border-radius:6;-fx-padding:12;");
                    
                    Label date = new Label(entry.date() + " " + entry.time());
                    date.setStyle("-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
                    
                    Label details = new Label("IP: " + entry.ip());
                    details.setStyle("-fx-text-fill: -color-text-muted;");
                    
                    card.getChildren().addAll(date, details);
                    historyList.getChildren().add(card);
                }
            });
        }).start();
    }

    // --- UPDATES TAB ---
    private void loadUpdates() {
        VBox content = new VBox(24);
        content.setFillWidth(true);

        Label subTitle = new Label("Manage application updates");
        subTitle.setStyle("-fx-font-size:13px;-fx-text-fill: -color-text-muted;-fx-font-weight:600;");

        VBox card = createBaseCard("Update Preferences", "🚀");
        UserPreferences prefs = context.preferencesService().loadPreferences();

        CheckBox autoCheck = new CheckBox("Automatically check for updates on startup");
        autoCheck.setStyle("-fx-font-size:13px;-fx-text-fill: -color-text-main;");
        autoCheck.setSelected(prefs.isAutoCheckUpdates());
        autoCheck.setOnAction(e -> {
            UserPreferences p = context.preferencesService().loadPreferences();
            p.setAutoCheckUpdates(autoCheck.isSelected());
            context.preferencesService().savePreferences(p);
        });

        CheckBox preReleases = new CheckBox("Notify me about pre-releases (Beta versions)");
        preReleases.setStyle("-fx-font-size:13px;-fx-text-fill: -color-text-main;");
        preReleases.setSelected(prefs.isNotifyPreReleases());
        preReleases.setOnAction(e -> {
            UserPreferences p = context.preferencesService().loadPreferences();
            p.setNotifyPreReleases(preReleases.isSelected());
            context.preferencesService().savePreferences(p);
        });

        CheckBox openWeb = new CheckBox("Prioritize website download link over GitHub releases");
        openWeb.setStyle("-fx-font-size:13px;-fx-text-fill: -color-text-main;");
        openWeb.setSelected(prefs.isOpenWebsiteFirst());
        openWeb.setOnAction(e -> {
            UserPreferences p = context.preferencesService().loadPreferences();
            p.setOpenWebsiteFirst(openWeb.isSelected());
            context.preferencesService().savePreferences(p);
        });

        Button checkNow = new Button("Check For Updates Now");
        checkNow.getStyleClass().add("btn-primary");
        checkNow.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        checkNow.setOnAction(e -> {
            StackPane root = (StackPane) context.stage().getScene().getRoot();
            context.updateService().checkForUpdatesManually(root);
        });

        card.getChildren().addAll(autoCheck, preReleases, openWeb, checkNow);

        content.getChildren().addAll(subTitle, card);
        
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        contentArea.getChildren().setAll(sp);
    }

    private void applyOfflineStateIfOffline(Button btn, String originalText, String prefix) {
        if (!context.isOnline()) {
            btn.setDisable(true);
            String cleanText = originalText;
            if (cleanText.startsWith("🔄 ")) {
                cleanText = cleanText.substring(2);
            }
            btn.setText("🔒 " + cleanText);
            btn.setTooltip(new Tooltip("This feature is disabled in offline mode."));
        }
    }

    private void onConnectivityChanged(boolean isOnline) {
        Platform.runLater(() -> {
            updateButtonState(syncBtn, "Force Sync Data", isOnline, "🔄 ");
            updateButtonState(saveProfileBtn, "Save Profile", isOnline, "");
            updateButtonState(changePassBtn, "Change Password", isOnline, "");
        });
    }

    private void updateButtonState(Button btn, String originalText, boolean isOnline, String prefix) {
        if (btn == null) return;
        if (isOnline) {
            btn.setDisable(false);
            if (btn.getText().startsWith("🔒 ")) {
                btn.setText(prefix + originalText);
            }
            btn.setTooltip(null);
        } else {
            btn.setDisable(true);
            String cleanText = btn.getText();
            if (cleanText.startsWith("🔄 ")) {
                cleanText = cleanText.substring(2);
            }
            if (!cleanText.startsWith("🔒 ")) {
                btn.setText("🔒 " + cleanText);
            }
            btn.setTooltip(new Tooltip("This feature is disabled in offline mode."));
        }
    }

    private void setStatusSuccess(Label lbl, String text) {
        lbl.setText(text);
        lbl.getStyleClass().removeAll("status-error", "status-success");
        lbl.getStyleClass().add("status-success");
    }

    private void setStatusError(Label lbl, String text) {
        lbl.setText(text);
        lbl.getStyleClass().removeAll("status-error", "status-success");
        lbl.getStyleClass().add("status-error");
    }

    public BorderPane getRoot() { return root; }
}
