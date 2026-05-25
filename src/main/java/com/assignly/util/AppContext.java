package com.assignly.util;

import com.assignly.database.DatabaseManager;
import com.assignly.model.UserPreferences;
import com.assignly.service.CredentialManager;
import com.assignly.service.DataCacheService;
import com.assignly.service.PortalRepository;
import com.assignly.service.PortalService;
import com.assignly.service.PreferencesService;
import com.assignly.view.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Optional;

public class AppContext {
    private final Stage stage;
    private final DatabaseManager databaseManager;
    private final CredentialManager credentialManager;
    private final PreferencesService preferencesService;
    private final DataCacheService dataCacheService;
    private final PortalService portalService;
    private final PortalRepository portalRepository;
    private String sessionRegistration;
    private String sessionPassword;

    private BorderPane mainLayout;
    private VBox sidebar;
    private StackPane contentArea;
    private StackPane loadingOverlay;

    public AppContext(Stage stage,
                      DatabaseManager databaseManager,
                      CredentialManager credentialManager,
                      PreferencesService preferencesService,
                      DataCacheService dataCacheService,
                      PortalService portalService) {
        this.stage = stage;
        this.databaseManager = databaseManager;
        this.credentialManager = credentialManager;
        this.preferencesService = preferencesService;
        this.dataCacheService = dataCacheService;
        this.portalService = portalService;
        this.portalRepository = new PortalRepository();
    }

    // ---------- Startup ----------
    public void showSplash() {
        UserPreferences prefs = preferencesService.loadPreferences();
        Optional<com.assignly.model.User> user = credentialManager.getStoredUser();

        if (prefs.isAutoLogin() && user.isPresent() && user.get().isRememberMe()) {
            Optional<String> pass = credentialManager.getDecryptedPassword(user.get());
            if (pass.isPresent()) {
                showSplashScreen(user.get().getRegistrationNo(), pass.get());
                return;
            }
        }
        showLoginScreen();
    }

    private void showSplashScreen(String reg, String password) {
        StackPane splash = new StackPane();
        splash.setStyle("-fx-background-color: #F0EDEC;");

        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        Label brand = new Label("Assignly");
        brand.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #004643;");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(30, 30);
        Label status = new Label("Signing in...");
        status.setStyle("-fx-font-size: 12px; -fx-text-fill: #888888;");
        box.getChildren().addAll(brand, spinner, status);
        splash.getChildren().add(box);

        setScene(splash, "Assignly Desktop");

        new Thread(() -> {
            PortalRepository.LoginResult result = portalRepository.login(reg, password);
            Platform.runLater(() -> {
                if (result instanceof PortalRepository.LoginResult.Success) {
                    setSessionCredentials(reg, password);
                    showMainApp();
                } else {
                    showLoginScreen();
                }
            });
        }).start();
    }

    // ---------- Login ----------
    public void showLoginScreen() {
        LoginView loginView = new LoginView();
        Optional<com.assignly.model.User> user = credentialManager.getStoredUser();
        user.ifPresent(u -> loginView.getRegistrationField().setText(u.getRegistrationNo()));
        loginView.getLoginButton().setOnAction(event -> handleLogin(loginView));
        setScene(loginView.getRoot(), "Assignly Desktop");
    }

    private void handleLogin(LoginView loginView) {
        String reg = loginView.getRegistrationField().getText().trim().toUpperCase();
        String pass = loginView.getPasswordField().getText();
        boolean remember = loginView.getRememberMeCheck().isSelected();

        if (reg.isBlank() || pass.isBlank()) {
            loginView.getErrorLabel().setText("Enter both registration number and password.");
            loginView.getErrorLabel().setVisible(true);
            return;
        }

        loginView.getLoginButton().setDisable(true);
        loginView.getLoginButton().setText("Signing in...");
        loginView.getErrorLabel().setVisible(false);

        new Thread(() -> {
            PortalRepository.LoginResult result = portalRepository.login(reg, pass);
            Platform.runLater(() -> {
                if (result instanceof PortalRepository.LoginResult.Success) {
                    credentialManager.saveCredentials(reg, pass, remember);
                    UserPreferences prefs = preferencesService.loadPreferences();
                    prefs.setAutoLogin(remember);
                    preferencesService.savePreferences(prefs);
                    setSessionCredentials(reg, pass);
                    showMainApp();
                } else if (result instanceof PortalRepository.LoginResult.InvalidCredentials) {
                    showLoginError(loginView, "Invalid registration number or password.");
                } else if (result instanceof PortalRepository.LoginResult.CaptchaRequired) {
                    showLoginError(loginView, "Security check required. Try again later.");
                } else if (result instanceof PortalRepository.LoginResult.Error err) {
                    showLoginError(loginView, err.message());
                }
            });
        }).start();
    }

    private void showLoginError(LoginView view, String msg) {
        view.getErrorLabel().setText(msg);
        view.getErrorLabel().setVisible(true);
        view.getLoginButton().setDisable(false);
        view.getLoginButton().setText("Sign In");
    }

    // ---------- Main App ----------
    public void showMainApp() {
        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("app-root");

        sidebar = buildSidebar();
        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        loadingOverlay = new StackPane();
        loadingOverlay.getStyleClass().add("loading-overlay");
        ProgressIndicator sp = new ProgressIndicator();
        sp.setMaxSize(30, 30);
        loadingOverlay.getChildren().add(sp);
        loadingOverlay.setVisible(false);

        StackPane main = new StackPane(contentArea, loadingOverlay);
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(main);

        setScene(mainLayout, "Assignly Desktop");
        navigateTo("dashboard");

        // Background Pre-fetcher
        new Thread(() -> {
            fetchAndCacheHtml("Dashboard.aspx");
            fetchAndCacheHtml("CoursePortal.aspx");
            fetchAndCacheHtml("Timetable.aspx");
            fetchAndCacheHtml("FeeChallans.aspx");
            fetchAndCacheHtml("FeeHistorySFMS.aspx");
            fetchAndCacheHtml("StudentResultCard.aspx");
            fetchAndCacheHtml("DateSheet.aspx");
            fetchAndCacheHtml("Summary.aspx");
            fetchAndCacheHtml("classproceedings.aspx");
            fetchAndCacheHtml("QAMarks.aspx");
            fetchAndCacheHtml("EntryCouponSelect.aspx");
            fetchAndCacheHtml("EntryCouponWithQR.aspx");
            fetchAndCacheHtml("AddCellEmailInfo.aspx");
            fetchAndCacheHtml("LoginHistory.aspx");
            fetchAndCacheHtml("scholarship/ViewScholarshipStatuse.aspx");
        }).start();
    }

    private VBox buildSidebar() {
        VBox sb = new VBox(2);
        sb.getStyleClass().add("sidebar");
        sb.setPrefWidth(200);
        sb.setMinWidth(200);
        sb.setPadding(new Insets(20, 10, 16, 10));

        Label brand = new Label("Assignly");
        brand.getStyleClass().add("sidebar-brand");
        Label sub = new Label("Desktop");
        sub.getStyleClass().add("sidebar-brand-sub");
        VBox brandBox = new VBox(2, brand, sub);
        brandBox.setPadding(new Insets(0, 6, 14, 6));

        Button dashboard = navBtn("Dashboard", "dashboard");
        Button courses = navBtn("Courses", "courses");
        Button portal = navBtn("Course Portal", "portal");
        Button examCoupon = navBtn("Exam Entry Coupon", "examcoupon");
        Button mobileApp = navBtn("Mobile App Activation", "mobileapp");
        Button result = navBtn("Result", "result");
        Button timetable = navBtn("Timetable", "timetable");
        Button fee = navBtn("Fee", "fee");
        Button scholarship = navBtn("Scholarship", "scholarship");
        Button dateSheet = navBtn("Date Sheet", "datesheet");
        Button settings = navBtn("Settings", "settings");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        String reg = sessionRegistration != null ? sessionRegistration : "";
        Label userLabel = new Label(reg);
        userLabel.setStyle("-fx-text-fill: rgba(240,237,236,0.4); -fx-font-size: 10px;");
        userLabel.setPadding(new Insets(4, 6, 0, 6));

        Button logout = new Button("Sign Out");
        logout.getStyleClass().add("sidebar-logout");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> {
            credentialManager.clearRememberMe();
            clearSessionCredentials();
            showLoginScreen();
        });

        sb.getChildren().addAll(brandBox, dashboard, courses, portal, examCoupon, mobileApp,
                result, timetable, fee, scholarship, dateSheet, new Separator(), settings, spacer, userLabel, logout);
        return sb;
    }

    private Button navBtn(String text, String tabId) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setUserData(tabId);
        btn.setOnAction(e -> navigateTo(tabId));
        return btn;
    }

    private void updateActiveNavState(Parent parent, String tabId) {
        if (parent == null) return;
        for (javafx.scene.Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Button btn && btn.getStyleClass().contains("sidebar-button")) {
                btn.getStyleClass().remove("sidebar-button-active");
                if (tabId.equals(btn.getUserData())) {
                    btn.getStyleClass().add("sidebar-button-active");
                }
            } else if (node instanceof Parent p) {
                updateActiveNavState(p, tabId);
            }
        }
    }

    public void navigateTo(String tabId) {
        updateActiveNavState(sidebar, tabId);

        javafx.scene.Node newTab = switch (tabId) {
            case "dashboard" -> new DashboardTabView(this).getRoot();
            case "courses" -> new CoursesTabView(this).getRoot();
            case "portal" -> new CoursePortalTabView(this).getRoot();
            case "examcoupon" -> new ExamCouponTabView(this).getRoot();
            case "mobileapp" -> new MobileAppActivationTabView(this).getRoot();
            case "result" -> new ResultTabView(this).getRoot();
            case "timetable" -> new TimetableTabView(this).getRoot();
            case "fee" -> new FeeTabView(this).getRoot();
            case "scholarship" -> new ScholarshipTabView(this).getRoot();
            case "datesheet" -> new DateSheetTabView(this).getRoot();
            case "settings" -> new SettingsTabView(this).getRoot();
            default -> null;
        };

        if (newTab != null) {
            contentArea.getChildren().setAll(newTab);
            
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), newTab);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(250), newTab);
            tt.setFromY(15);
            tt.setToY(0);
            
            javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(ft, tt);
            pt.play();
        }
    }

    private double xOffset = 0;
    private double yOffset = 0;

    // ---------- Scene ----------
    public void setScene(Parent contentRoot, String title) {
        contentRoot.getStyleClass().add("app-root");

        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_RIGHT);
        titleBar.setMinHeight(30);
        titleBar.setStyle("-fx-background-color: #004643;");
        
        Label windowTitle = new Label(title);
        windowTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 0 0 0 10;");
        HBox.setHgrow(windowTitle, Priority.ALWAYS);
        windowTitle.setMaxWidth(Double.MAX_VALUE);

        Button minBtn = new Button("—");
        minBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        minBtn.setOnAction(e -> stage.setIconified(true));

        Button maxBtn = new Button("☐");
        maxBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        maxBtn.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;"));
        closeBtn.setOnAction(e -> Platform.exit());

        titleBar.getChildren().addAll(windowTitle, minBtn, maxBtn, closeBtn);

        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        BorderPane windowContainer = new BorderPane();
        windowContainer.setTop(titleBar);
        windowContainer.setCenter(contentRoot);
        // Ensure root styling is applied to the wrapper to prevent unstyled borders
        windowContainer.setStyle("-fx-border-color: #004643; -fx-border-width: 1;");

        Scene scene = new Scene(windowContainer, 1200, 750);
        URL css = getClass().getResource("/com/assignly/styles/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        
        // Dark mode hook (we will implement this fully later)
        applyTheme(scene);

        stage.setTitle(title);
        stage.setScene(scene);
    }
    
    public void applyTheme(Scene scene) {
        if ("DARK".equalsIgnoreCase(preferencesService.loadPreferences().getTheme())) {
            scene.getRoot().getStyleClass().add("dark-theme");
        } else {
            scene.getRoot().getStyleClass().remove("dark-theme");
        }
    }

    // ---------- Accessors ----------
    public Stage stage() { return stage; }
    public DatabaseManager databaseManager() { return databaseManager; }
    public CredentialManager credentialManager() { return credentialManager; }
    public PreferencesService preferencesService() { return preferencesService; }
    public DataCacheService dataCacheService() { return dataCacheService; }
    public PortalService portalService() { return portalService; }
    public PortalRepository portalRepository() { return portalRepository; }

    public void setSessionCredentials(String reg, String pass) {
        this.sessionRegistration = reg;
        this.sessionPassword = pass;
    }
    public String getSessionRegistration() { return sessionRegistration; }
    public String getSessionPassword() { return sessionPassword; }
    public String fetchAndCacheHtml(String relativeUrl) {
        String html = portalRepository.fetchPageHtml(relativeUrl);
        if (html != null) {
            dataCacheService.cacheHtml(relativeUrl, html);
        }
        return html;
    }

    public void clearSessionCredentials() {
        this.sessionRegistration = null;
        this.sessionPassword = null;
    }
}
