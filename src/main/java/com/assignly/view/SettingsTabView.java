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

    public SettingsTabView(AppContext context) {
        this.context = context;
        buildUI();
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

        sidebar.getChildren().addAll(btnGeneral, btnProfile, btnPassword, btnHistory);

        btnGeneral.setOnAction(e -> { setActive(sidebar, btnGeneral); loadGeneral(); });
        btnProfile.setOnAction(e -> { setActive(sidebar, btnProfile); loadProfile(); });
        btnPassword.setOnAction(e -> { setActive(sidebar, btnPassword); loadPassword(); });
        btnHistory.setOnAction(e -> { setActive(sidebar, btnHistory); loadHistory(); });

        root.setLeft(sidebar);
        root.setCenter(contentArea);

        // Load default
        btnGeneral.fire();
    }

    private Button createSubTabBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:transparent;-fx-text-fill: -color-text-muted;-fx-font-size:14px;-fx-font-weight:bold;-fx-alignment:CENTER-LEFT;-fx-padding:10 16;");
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    private void setActive(VBox sidebar, Button activeBtn) {
        for (javafx.scene.Node n : sidebar.getChildren()) {
            if (n instanceof Button b) {
                b.setStyle("-fx-background-color:transparent;-fx-text-fill: -color-text-muted;-fx-font-size:14px;-fx-font-weight:bold;-fx-alignment:CENTER-LEFT;-fx-padding:10 16;");
            }
        }
        activeBtn.setStyle("-fx-background-color:#eff6ff;-fx-text-fill:#2563eb;-fx-font-size:14px;-fx-font-weight:bold;-fx-alignment:CENTER-LEFT;-fx-padding:10 16;-fx-background-radius:6;");
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
        Button syncBtn = new Button("🔄 Force Sync Data");
        syncBtn.setStyle("-fx-background-color:#E8F0FE;-fx-text-fill:#1967D2;-fx-font-weight:bold;-fx-padding:10 20;-fx-background-radius:6;");
        syncBtn.setOnAction(e -> {
            context.dataCacheService().clearAllCaches();
            syncBtn.setText("✅ Synced!");
            syncBtn.setDisable(true);
        });

        // Quick Logout Button
        Button logoutBtn = new Button("🚪 Secure Logout");
        logoutBtn.setStyle("-fx-background-color:#FEF2F2;-fx-text-fill:#DC2626;-fx-font-weight:bold;-fx-padding:10 20;-fx-background-radius:6;");
        logoutBtn.setOnAction(e -> {
            context.credentialManager().clearRememberMe();
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
        
        card.getChildren().addAll(desc, themeRow);
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
        regInfo.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#059669;");

        CheckBox autoLogin = new CheckBox("Auto-login on app startup");
        autoLogin.setStyle("-fx-font-size:13px;-fx-text-fill: -color-text-main;");
        autoLogin.setSelected(prefs.isAutoLogin());
        autoLogin.setOnAction(e -> {
            UserPreferences p = context.preferencesService().loadPreferences();
            p.setAutoLogin(autoLogin.isSelected());
            context.preferencesService().savePreferences(p);
        });

        Button clearBtn = new Button("Wipe Saved Credentials");
        clearBtn.setStyle("-fx-background-color:transparent;-fx-border-color:#ef4444;-fx-border-radius:4;-fx-text-fill:#ef4444;-fx-padding:6 12;");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> {
            context.credentialManager().clearAllCredentials();
            clearBtn.setText("Credentials Wiped");
            clearBtn.setDisable(true);
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
        clearCache.setStyle("-fx-background-color:#f1f5f9;-fx-text-fill: -color-text-muted;-fx-font-weight:bold;-fx-padding:8 16;-fx-background-radius:4;");
        clearCache.setMaxWidth(Double.MAX_VALUE);
        clearCache.setOnAction(e -> {
            context.dataCacheService().clearAllCaches();
            clearCache.setText("Cache Cleared Successfully");
            clearCache.setDisable(true);
        });
        
        card.getChildren().addAll(cacheDesc, clearCache);
        return card;
    }

    // --- PROFILE TAB ---
    private void loadProfile() {
        VBox content = new VBox(16);
        content.setMaxWidth(400);
        
        Label title = new Label("Update Profile Information");
        title.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
        
        Label info = new Label("Loading profile data...");
        info.setStyle("-fx-text-fill: -color-text-muted;");

        HBox cellBox = new HBox(8);
        TextField networkField = new TextField();
        networkField.setPromptText("03XX");
        networkField.setPrefWidth(70);
        
        Label dash = new Label("-");
        
        TextField numberField = new TextField();
        numberField.setPromptText("XXXXXXX");
        numberField.setPrefWidth(150);
        
        cellBox.getChildren().addAll(networkField, dash, numberField);
        cellBox.setAlignment(Pos.CENTER_LEFT);

        TextField emailField = new TextField();
        emailField.setPromptText("Email Address");

        Button saveBtn = new Button("Save Profile");
        saveBtn.setStyle("-fx-background-color:#2563eb;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:10 20;-fx-background-radius:6;");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setDisable(true);

        Label statusLbl = new Label("");
        statusLbl.setWrapText(true);
        statusLbl.setStyle("-fx-text-fill:#059669;");

        content.getChildren().addAll(title, info, 
            new Label("Cell Number:"), cellBox, 
            new Label("Email Address:"), emailField, 
            new Region(), saveBtn, statusLbl);

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
                    saveBtn.setDisable(false);
                } else {
                    info.setText("Failed to load profile data.");
                }
            });
        }).start();

        saveBtn.setOnAction(e -> {
            saveBtn.setText("Saving...");
            saveBtn.setDisable(true);
            new Thread(() -> {
                String msg = context.portalRepository().updateProfile(networkField.getText(), numberField.getText(), emailField.getText());
                context.fetchAndCacheHtml("AddCellEmailInfo.aspx");
                Platform.runLater(() -> {
                    statusLbl.setText(msg);
                    saveBtn.setText("Save Profile");
                    saveBtn.setDisable(false);
                });
            }).start();
        });
    }

    // --- CHANGE PASSWORD TAB ---
    private void loadPassword() {
        VBox content = new VBox(16);
        content.setMaxWidth(400);

        Label title = new Label("Change Password");
        title.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");
        
        Label rules = new Label("Loading password policy...");
        rules.setWrapText(true);
        rules.setStyle("-fx-text-fill: -color-text-muted;-fx-font-size:13px;");

        PasswordField oldPass = new PasswordField();
        oldPass.setPromptText("Current Password");
        PasswordField newPass = new PasswordField();
        newPass.setPromptText("New Password");
        PasswordField confPass = new PasswordField();
        confPass.setPromptText("Confirm New Password");

        Button saveBtn = new Button("Change Password");
        saveBtn.setStyle("-fx-background-color:#2563eb;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:10 20;-fx-background-radius:6;");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        Label statusLbl = new Label("");
        statusLbl.setWrapText(true);
        statusLbl.setStyle("-fx-text-fill:#059669;");

        content.getChildren().addAll(title, rules, 
            new Label("Current Password:"), oldPass, 
            new Label("New Password:"), newPass, 
            new Label("Confirm Password:"), confPass, 
            new Region(), saveBtn, statusLbl);

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

        saveBtn.setOnAction(e -> {
            if (!newPass.getText().equals(confPass.getText())) {
                statusLbl.setText("New passwords do not match.");
                statusLbl.setStyle("-fx-text-fill:#dc2626;");
                return;
            }
            saveBtn.setText("Updating...");
            saveBtn.setDisable(true);
            new Thread(() -> {
                String msg = context.portalRepository().changePassword(oldPass.getText(), newPass.getText(), confPass.getText());
                Platform.runLater(() -> {
                    statusLbl.setText(msg);
                    if (msg.toLowerCase().contains("success") || msg.toLowerCase().contains("changed") || msg.toLowerCase().contains("completed") || msg.toLowerCase().contains("successfully")) {
                        statusLbl.setStyle("-fx-text-fill:#059669;");
                        // Force a logout due to credentials change
                        context.credentialManager().clearRememberMe();
                        context.clearSessionCredentials();
                        
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Password Changed");
                        alert.setHeaderText("Success!");
                        alert.setContentText("Your password has been changed. Please log in again with your new password.");
                        alert.showAndWait();
                        
                        context.showLoginScreen();
                    } else {
                        statusLbl.setStyle("-fx-text-fill:#dc2626;");
                        saveBtn.setText("Change Password");
                        saveBtn.setDisable(false);
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

    public BorderPane getRoot() { return root; }
}
