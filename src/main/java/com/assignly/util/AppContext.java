package com.assignly.util;

import com.assignly.database.DatabaseManager;
import com.assignly.model.UserPreferences;
import com.assignly.service.CredentialManager;
import com.assignly.service.DataCacheService;
import com.assignly.service.PortalRepository;
import com.assignly.service.PortalService;
import com.assignly.service.NotificationService;
import com.assignly.service.PreferencesService;
import com.assignly.view.*;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import org.jsoup.Jsoup;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.geometry.Point2D;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.shape.Circle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.SnapshotParameters;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;

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
    private final NotificationService notificationService;

    // QoL Features
    private TextField searchField;
    private ListView<SearchItem> searchResults;
    private final ObservableList<SearchItem> filteredSearchItems = FXCollections.observableArrayList();
    private List<SearchItem> allSearchItems = List.of();
    private TextField paletteSearchField;
    private VBox paletteCard;
    private StackPane overlay;
    private ImageView blurBackground;
    private Region dimPane;
    private String pendingPortalTab;
    private String pendingFeeTab;
    private String pendingScholarshipTab;
    private String pendingSettingsSubTab;
    private String pendingSearchHighlight;
    private ImageView sidebarAvatar;
    private Label sidebarNameLabel;
    private Label sidebarProgramLabel;
    private Label headerTitleLabel;
    private Button syncPortalBtn;
    private boolean isSyncing = false;
    private Label statusLabel;
    private Label syncTimeLabel;
    private Circle statusIndicator;
    private ScheduledExecutorService statusScheduler;
    private String pendingCoursesTab;
    private String pendingCourseQuery;
    private final java.util.concurrent.ConcurrentMap<String, java.util.concurrent.CompletableFuture<String>> inFlightCaches = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean isOnline = true;
    private final java.util.List<java.lang.ref.WeakReference<java.util.function.Consumer<Boolean>>> connectivityListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void addConnectivityListener(java.util.function.Consumer<Boolean> listener) {
        connectivityListeners.removeIf(ref -> ref.get() == null);
        connectivityListeners.add(new java.lang.ref.WeakReference<>(listener));
        listener.accept(isOnline);
    }
    
    public boolean isOnline() {
        return isOnline;
    }

    
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
        this.notificationService = new NotificationService(this);
        this.portalRepository = new PortalRepository();
        this.portalRepository.setOnSessionExpiredCallback(() -> {
            javafx.application.Platform.runLater(() -> {
                notificationService.showError("Session expired. Please log in again.");
                credentialManager.clearRememberMe();
                clearSessionCredentials();
                showLoginScreen();
            });
        });
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
        splash.setStyle("-fx-background-color: #111315;");

        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        Label brand = new Label("Assignly");
        brand.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #14b8a6;");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(30, 30);
        Label status = new Label("Signing in...");
        status.setStyle("-fx-font-size: 12px; -fx-text-fill: #B8C0CC;");
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

        // Fixed-size login window: disable resizing and maximize
        stage.setMaximized(false);
        stage.setResizable(false);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setWidth(1000);
        stage.setHeight(650);

        // Center on screen
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
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
        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setFitToHeight(true);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sidebarScroll.getStyleClass().add("sidebar-scroll");
        sidebarScroll.setMinWidth(200);
        sidebarScroll.setPrefWidth(200);
        sidebarScroll.setMaxWidth(200);

        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        loadingOverlay = new StackPane();
        loadingOverlay.getStyleClass().add("loading-overlay");
        ProgressIndicator sp = new ProgressIndicator();
        sp.setMaxSize(30, 30);
        loadingOverlay.getChildren().add(sp);
        loadingOverlay.setVisible(false);

        StackPane main = new StackPane(contentArea, loadingOverlay);

        mainLayout.setLeft(sidebarScroll);
        
        // Setup Status Bar
        HBox statusBar = new HBox(8);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(4, 16, 4, 16));
        
        statusIndicator = new Circle(4);
        statusLabel = new Label("Checking connection...");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        
        syncTimeLabel = new Label("");
        syncTimeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        
        Region statusSpacer = new Region();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);
        
        statusBar.getChildren().addAll(statusIndicator, statusLabel, statusSpacer, syncTimeLabel);

        // UI RESTORATION: Restore the original layout structure where the status bar spans the full bottom width of the screen, and the main StackPane sits in the center region
        mainLayout.setBottom(statusBar);

        HBox topHeader = buildTopHeader();
        VBox centerLayout = new VBox(topHeader, main);
        VBox.setVgrow(main, Priority.ALWAYS);
        centerLayout.setStyle("-fx-background-color: transparent;");
        mainLayout.setCenter(centerLayout);

        setScene(mainLayout, "Assignly Desktop");

        // Re-enable window resizing for the main dashboard
        stage.setResizable(true);

        // Constrain minimum size and expand to dashboard dimensions
        stage.setMinWidth(1200);
        stage.setMinHeight(750);
        if (stage.getWidth() < 1200) {
            stage.setWidth(1200);
        }
        if (stage.getHeight() < 750) {
            stage.setHeight(750);
        }

        // Center on screen after resize
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
        
        notificationService.showSuccess("Successfully logged in");
        setupSearch();
        
        // Register Ctrl+K
        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.K) {
                showSearchPopup();
                e.consume();
            }
        });
        
        // Start Connectivity Checker
        if (statusScheduler != null && !statusScheduler.isShutdown()) statusScheduler.shutdownNow();
        statusScheduler = Executors.newSingleThreadScheduledExecutor();
        statusScheduler.scheduleAtFixedRate(this::updateStatusIndicator, 0, 60, TimeUnit.SECONDS);

        // Initialize sidebar profile from local database cache if exists
        try {
            String cachedDash = dataCacheService.getCachedHtml("Dashboard.aspx").orElse(null);
            if (cachedDash != null) {
                PortalRepository.DashboardData cachedData = portalRepository.parseDashboard(cachedDash);
                if (cachedData != null) {
                    String name = portalRepository.getCurrentStudentName();
                    String program = cachedData.studentInfo().get("Program");
                    updateSidebarProfile(name, program);
                }
            }
        } catch (Exception ignored) {}

        navigateTo("dashboard");
        Platform.runLater(() -> contentArea.requestFocus());

        // Background Pre-fetcher
        new Thread(() -> {
            fetchAndCacheHtml("Dashboard.aspx");
            try {
                String liveDash = dataCacheService.getCachedHtml("Dashboard.aspx").orElse(null);
                if (liveDash != null) {
                    PortalRepository.DashboardData liveData = portalRepository.parseDashboard(liveDash);
                    if (liveData != null) {
                        String name = portalRepository.getCurrentStudentName();
                        String program = liveData.studentInfo().get("Program");
                        updateSidebarProfile(name, program);
                    }
                }
            } catch (Exception ignored) {}
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
            fetchAndCacheHtml("CTS/CTSdashboard.aspx");
            fetchAndCacheHtml("CoursePortal.aspx?isTest=1");
            fetchAndCacheHtml("CoursePortalContentsSummary.aspx");
            fetchAndCacheHtml("CoursePortalPendingAssignments.aspx");
            try {
                syncCourseSpecificData();
            } catch (Exception ignored) {}
            
            // Pre-download COMSATS logo for PDF exports
            try {
                byte[] logoBytes = portalRepository.fetchPhotoBytes("https://sis.cuiatd.edu.pk/resources/images/CIITLogo_Plain.png");
                if (logoBytes != null && logoBytes.length > 0) {
                    java.nio.file.Files.write(java.nio.file.Path.of(com.assignly.util.AppDirectoryHelper.getAppDataDir(), "cui_logo.png"), logoBytes);
                }
            } catch (Exception ignored) {}
            
            // Fetch profile picture if not already fetched
            String photoUrl = portalRepository.getCurrentStudentPhotoUrl();
            if (photoUrl != null && !photoUrl.isBlank()) {
                byte[] photoBytes = portalRepository.fetchPhotoBytes(photoUrl);
                if (photoBytes != null && photoBytes.length > 0) {
                    Platform.runLater(() -> {
                        if (sidebarAvatar != null) {
                            Image img = new Image(new ByteArrayInputStream(photoBytes));
                            double iw = img.getWidth();
                            double ih = img.getHeight();
                            if (iw > 0 && ih > 0) {
                                double s = Math.min(iw, ih);
                                double x = (iw - s) / 2;
                                double y = (ih - s) / 2;
                                sidebarAvatar.setViewport(new javafx.geometry.Rectangle2D(x, y, s, s));
                            }
                            sidebarAvatar.setFitWidth(30);
                            sidebarAvatar.setFitHeight(30);
                            sidebarAvatar.setPreserveRatio(false);
                            sidebarAvatar.setImage(img);
                        }
                    });
                }
            }

            notificationService.showInfo("Background sync complete");
            Platform.runLater(this::refreshSearchItems);
        }).start();
    }

    public void updateSidebarProfile(String name, String program) {
        Platform.runLater(() -> {
            if (sidebarNameLabel != null && name != null && !name.isBlank()) {
                sidebarNameLabel.setText(name);
            }
            if (sidebarProgramLabel != null && program != null && !program.isBlank()) {
                sidebarProgramLabel.setText(program);
            }
        });
    }

    private void setupSearch() {
        // Initialize search palette text field
        paletteSearchField = new TextField();
        paletteSearchField.setPromptText("Search anything...");
        paletteSearchField.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-width: 0;" +
            "-fx-font-size: 16px;" +
            "-fx-text-fill: -color-text-main;" +
            "-fx-padding: 16 12 16 4;"
        );

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 18px; -fx-text-fill: -color-text-muted; -fx-padding: 0 0 0 16;");

        Button clearBtn = new Button("❌");
        clearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-text-muted; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 16 0 0;");
        clearBtn.visibleProperty().bind(paletteSearchField.textProperty().isNotEmpty());
        clearBtn.setOnAction(e -> {
            paletteSearchField.clear();
            paletteSearchField.requestFocus();
        });

        HBox topBar = new HBox(8, searchIcon, paletteSearchField, clearBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(paletteSearchField, Priority.ALWAYS);

        Separator divider = new Separator();
        divider.setStyle("-fx-background-color: -color-border; -fx-opacity: 0.3;");

        // Initialize Results ListView
        searchResults = new ListView<>();
        searchResults.getStyleClass().add("search-results");
        searchResults.setItems(filteredSearchItems);
        searchResults.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
        searchResults.setPrefHeight(380);
        VBox.setVgrow(searchResults, Priority.ALWAYS);

        Label guideLabel = new Label("↑↓ Navigate  •  Double-Click Open  •  Esc Close");
        guideLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-padding: 8 16;");
        
        HBox footerBar = new HBox(guideLabel);
        footerBar.setAlignment(Pos.CENTER_RIGHT);
        footerBar.setStyle("-fx-background-color: -color-bg-main; -fx-background-radius: 0 0 16 16; -fx-border-color: -color-border; -fx-border-width: 1 0 0 0;");

        // Centered glassmorphic palette card VBox
        paletteCard = new VBox(0, topBar, divider, searchResults, footerBar);
        paletteCard.setMaxSize(700, 500);
        paletteCard.setMinSize(700, 500);
        paletteCard.getStyleClass().add("search-palette-card");

        DropShadow cardShadow = new DropShadow();
        cardShadow.setColor(Color.rgb(0, 0, 0, 0.22));
        cardShadow.setRadius(24);
        cardShadow.setOffsetY(12);
        paletteCard.setEffect(cardShadow);

        // Backdrop blurred snapshot view
        blurBackground = new ImageView();
        blurBackground.setPreserveRatio(false);
        blurBackground.setEffect(new javafx.scene.effect.GaussianBlur(15)); // Premium soft glass blur

        // Darken overlay tint to dim the blurred layout
        dimPane = new Region();
        dimPane.getStyleClass().add("overlay-dim-pane");

        // Full-screen overlay containing the blurred image, dim pane, and centered card
        overlay = new StackPane(blurBackground, dimPane, paletteCard);
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay || e.getTarget() == dimPane) {
                hideSearchPopup();
            }
        });
        
        // Handle global Escape key press inside overlay
        overlay.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hideSearchPopup();
                e.consume();
            }
        });

        // Selection model skipping headers
        searchResults.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.action() == null) {
                int curIdx = searchResults.getSelectionModel().getSelectedIndex();
                int itemsSize = filteredSearchItems.size();
                int oldIdx = oldVal == null ? -1 : filteredSearchItems.indexOf(oldVal);
                
                if (oldIdx < curIdx) {
                    // Moving DOWN
                    int nextIdx = curIdx + 1;
                    while (nextIdx < itemsSize && filteredSearchItems.get(nextIdx).action() == null) {
                        nextIdx++;
                    }
                    if (nextIdx < itemsSize) {
                        searchResults.getSelectionModel().select(nextIdx);
                    } else {
                        searchResults.getSelectionModel().select(oldVal);
                    }
                } else {
                    // Moving UP
                    int prevIdx = curIdx - 1;
                    while (prevIdx >= 0 && filteredSearchItems.get(prevIdx).action() == null) {
                        prevIdx--;
                    }
                    if (prevIdx >= 0) {
                        searchResults.getSelectionModel().select(prevIdx);
                    } else {
                        searchResults.getSelectionModel().select(oldVal);
                    }
                }
            }
        });

        // Search cell factory with high-contrast text color selection overrides
        searchResults.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SearchItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else if (item.title().equals("No results found")) {
                    Label emptyLabel = new Label("No results found matching \"" + paletteSearchField.getText() + "\"");
                    emptyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted; -fx-padding: 24;");
                    HBox box = new HBox(emptyLabel);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                    setText(null);
                    setMouseTransparent(true);
                    setFocusTraversable(false);
                    setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
                } else if (item.action() == null) {
                    // Category Header
                    Label headerLabel = new Label(item.title().toUpperCase());
                    headerLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted; -fx-padding: 8 12 4 12;");
                    setGraphic(headerLabel);
                    setText(null);
                    setMouseTransparent(true);
                    setFocusTraversable(false);
                    setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
                } else {
                    setMouseTransparent(false);
                    setFocusTraversable(true);
                    
                    Label iconLabel = new Label(item.icon());
                    iconLabel.setStyle("-fx-font-size: 16px; -fx-min-width: 24px;");
                    
                    Label titleLabel = new Label(item.title());
                    
                    String sub = item.category();
                    if (item.parentPath() != null && !item.parentPath().isBlank()) {
                        sub += " • " + item.parentPath();
                    }
                    Label subLabel = new Label(sub);
                    
                    VBox textGroup = new VBox(2, titleLabel, subLabel);
                    
                    HBox itemRow = new HBox(12, iconLabel, textGroup);
                    itemRow.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(textGroup, Priority.ALWAYS);
                    
                    if (item.statusBadge() != null && !item.statusBadge().isBlank()) {
                        Label badge = new Label(item.statusBadge());
                        if (item.statusColor() != null) {
                            badge.setStyle(item.statusColor());
                        } else {
                            badge.setStyle("-fx-background-color: rgba(0, 70, 67, 0.08); -fx-text-fill: -color-accent; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10;");
                        }
                        itemRow.getChildren().add(badge);
                    }
                    
                    if (isSelected()) {
                        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: white;");
                        subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255, 255, 255, 0.7);");
                        iconLabel.setStyle("-fx-font-size: 16px; -fx-min-width: 24px; -fx-text-fill: white;");
                    } else {
                        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -color-text-main;");
                        subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
                        iconLabel.setStyle("-fx-font-size: 16px; -fx-min-width: 24px; -fx-text-fill: -color-text-main;");
                    }
                    
                    setGraphic(itemRow);
                    setText(null);
                    setStyle("");
                }
            }
        });

        searchResults.setOnMouseClicked(e -> {
            activateSelectedSearchItem();
        });
        searchResults.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                activateSelectedSearchItem();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideSearchPopup();
                e.consume();
            } else if (e.getCode() == KeyCode.UP) {
                int selectedIndex = searchResults.getSelectionModel().getSelectedIndex();
                int prevIdx = selectedIndex - 1;
                while (prevIdx >= 0 && filteredSearchItems.get(prevIdx).action() == null) {
                    prevIdx--;
                }
                if (prevIdx < 0) {
                    paletteSearchField.requestFocus();
                    paletteSearchField.selectEnd();
                    e.consume();
                }
            }
        });

        paletteSearchField.textProperty().addListener((obs, oldValue, newValue) -> updateSearchResults(newValue));
        paletteSearchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                if (!filteredSearchItems.isEmpty()) {
                    searchResults.requestFocus();
                    int startIdx = 0;
                    while (startIdx < filteredSearchItems.size() && filteredSearchItems.get(startIdx).action() == null) {
                        startIdx++;
                    }
                    if (startIdx < filteredSearchItems.size()) {
                        searchResults.getSelectionModel().select(startIdx);
                    }
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                if (!filteredSearchItems.isEmpty()) {
                    int startIdx = 0;
                    while (startIdx < filteredSearchItems.size() && filteredSearchItems.get(startIdx).action() == null) {
                        startIdx++;
                    }
                    if (startIdx < filteredSearchItems.size()) {
                        searchResults.getSelectionModel().select(startIdx);
                        activateSelectedSearchItem();
                    }
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideSearchPopup();
                e.consume();
            }
        });

        refreshSearchItems();
    }

    private void showSearchPopup() {
        if (stage == null || stage.getScene() == null || mainLayout == null || overlay == null) return;
        
        StackPane sceneRoot = (StackPane) stage.getScene().getRoot();
        if (sceneRoot.getChildren().contains(overlay)) return;
        
        refreshSearchItems();
        paletteSearchField.clear();
        filteredSearchItems.clear();
        updateSearchResults(""); // Trigger Empty State populate initially
        
        // Take a static snapshot of the live layout to avoid WebView live blur rendering glitches
        WritableImage snapshot = mainLayout.snapshot(null, null);
        blurBackground.setImage(snapshot);
        
        // Match search overlay size exactly to the scene client area
        double width = stage.getScene().getWidth();
        double height = stage.getScene().getHeight();
        
        overlay.setPrefSize(width, height);
        overlay.setMinSize(width, height);
        overlay.setMaxSize(width, height);
        
        blurBackground.setFitWidth(width);
        blurBackground.setFitHeight(height);
        
        // Add overlay to scene root StackPane on top of app layout but below toast alert layer
        if (notificationService.getToastContainer() != null && sceneRoot.getChildren().contains(notificationService.getToastContainer())) {
            int toastIdx = sceneRoot.getChildren().indexOf(notificationService.getToastContainer());
            sceneRoot.getChildren().add(toastIdx, overlay);
        } else {
            sceneRoot.getChildren().add(overlay);
        }
        
        // Premium opening scale and fade parallel transition
        paletteCard.setOpacity(0.0);
        paletteCard.setScaleX(0.95);
        paletteCard.setScaleY(0.95);
        
        FadeTransition ft = new FadeTransition(Duration.millis(200), paletteCard);
        ft.setToValue(1.0);
        
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(200), paletteCard);
        st.setToX(1.0);
        st.setToY(1.0);
        
        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.play();
        
        Platform.runLater(() -> paletteSearchField.requestFocus());
    }

    private void hideSearchPopup() {
        if (stage == null || stage.getScene() == null) return;
        StackPane sceneRoot = (StackPane) stage.getScene().getRoot();
        if (!sceneRoot.getChildren().contains(overlay)) return;
        
        // Closing exit transition
        FadeTransition ft = new FadeTransition(Duration.millis(150), paletteCard);
        ft.setToValue(0.0);
        
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(150), paletteCard);
        st.setToX(0.95);
        st.setToY(0.95);
        
        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.setOnFinished(e -> {
            sceneRoot.getChildren().remove(overlay);
            blurBackground.setImage(null); // Release snapshot memory
        });
        pt.play();
    }

    private void activateSelectedSearchItem() {
        if (searchResults == null) return;
        SearchItem item = searchResults.getSelectionModel().getSelectedItem();
        if (item == null || item.action() == null) return;
        hideSearchPopup();
        paletteSearchField.clear();
        item.action().run();
    }

    private void updateSearchResults(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            // Display empty state: Recent Searches & Suggested Shortcuts
            List<SearchItem> emptyState = new ArrayList<>();
            
            // Recent Searches
            emptyState.add(new SearchItem(null, "Recent Searches", "Header", null, null, null, "", null));
            emptyState.add(new SearchItem("🕒", "Data Structures Marks", "Recent Search", "Courses • Data Structures", null, null, "", () -> navigateToCourseMarks("Data Structures")));
            emptyState.add(new SearchItem("🕒", "OOP Assignment", "Recent Search", "Course Portal • Assignments", null, null, "", () -> { setPendingPortalTab("portal_assignments"); navigateTo("portal"); }));
            emptyState.add(new SearchItem("🕒", "Shining Star Scholarship", "Recent Search", "Scholarships • Status", "Approved", "-fx-background-color: #def7ec; -fx-text-fill: #03543f; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10;", "", () -> { setPendingScholarshipTab("status"); setPendingSearchHighlight("Shining"); navigateTo("scholarship"); }));
            
            // Suggested Shortcuts
            emptyState.add(new SearchItem(null, "Suggested", "Header", null, null, null, "", null));
            emptyState.add(new SearchItem("📊", "Academic Results", "Suggested Shortcut", "Result Portal", null, null, "", () -> navigateTo("result")));
            emptyState.add(new SearchItem("📘", "Course Portal", "Suggested Shortcut", "Course Portal", null, null, "", () -> navigateTo("portal")));
            emptyState.add(new SearchItem("🎓", "Scholarships", "Suggested Shortcut", "Scholarship Portal", null, null, "", () -> navigateTo("scholarship")));
            emptyState.add(new SearchItem("📅", "Attendance", "Suggested Shortcut", "Timetable & Attendance", null, null, "", () -> navigateTo("timetable")));
            
            filteredSearchItems.setAll(emptyState);
            return;
        }

        String q = trimmed.toLowerCase();
        java.util.Map<String, java.util.List<SearchItem>> groupedMatches = new java.util.LinkedHashMap<>();
        
        List<String> categoriesOrder = List.of(
            "Module",
            "Course",
            "Sub Module",
            "Assignment",
            "MCQ Test",
            "Subjective Test",
            "Academic Result",
            "Exam Schedule",
            "Fee Challan",
            "Payment Record",
            "Scholarship",
            "Announcement"
        );
        
        for (String cat : categoriesOrder) {
            groupedMatches.put(cat, new ArrayList<>());
        }
        List<SearchItem> otherMatches = new ArrayList<>();

        for (SearchItem item : allSearchItems) {
            boolean matches = item.title().toLowerCase().contains(q) || 
                              item.category().toLowerCase().contains(q) || 
                              item.keywords().contains(q);
            if (matches) {
                List<SearchItem> catList = groupedMatches.get(item.category());
                if (catList != null) {
                    catList.add(item);
                } else {
                    otherMatches.add(item);
                }
            }
        }

        List<SearchItem> flatList = new ArrayList<>();
        for (String cat : categoriesOrder) {
            List<SearchItem> list = groupedMatches.get(cat);
            if (list != null && !list.isEmpty()) {
                flatList.add(new SearchItem(null, cat + "s", cat, null, null, null, "", null));
                flatList.addAll(list);
            }
        }
        
        if (!otherMatches.isEmpty()) {
            flatList.add(new SearchItem(null, "OTHER RESULTS", "Other", null, null, null, "", null));
            flatList.addAll(otherMatches);
        }

        filteredSearchItems.setAll(flatList);

        if (flatList.isEmpty()) {
        }
    }

    private void refreshSearchItems() {
        List<SearchItem> items = new ArrayList<>();
        
        // 1. Modules
        items.add(new SearchItem("📊", "Dashboard", "Module", "Assignly", null, null, "dashboard panel main home", () -> navigateTo("dashboard")));
        items.add(new SearchItem("📚", "Courses", "Module", "Assignly", null, null, "courses registered syllabus grade", () -> navigateTo("courses")));
        items.add(new SearchItem("🌐", "Course Portal", "Module", "Assignly", null, null, "course portal online assignments tests", () -> navigateTo("portal")));
        items.add(new SearchItem("💰", "Fee", "Module", "Assignly", null, null, "fee challenger challans payments history", () -> navigateTo("fee")));
        items.add(new SearchItem("🏅", "Scholarship", "Module", "Assignly", null, null, "scholarship status general conditions", () -> navigateTo("scholarship")));
        items.add(new SearchItem("📆", "Date Sheet", "Module", "Assignly", null, null, "datesheet exam paper schedules timetable", () -> navigateTo("datesheet")));
        items.add(new SearchItem("⚙", "Settings", "Module", "Assignly", null, null, "settings profile password configuration", () -> navigateTo("settings")));

        // 2. Tab Submodules (Flat)
        items.add(new SearchItem("🎓", "Result Card", "Sub Module", "Result Portal", null, null, "result transcript semester cgpa sgpa", () -> navigateTo("result")));
        items.add(new SearchItem("📅", "Timetable Schedule", "Sub Module", "Timetable", null, null, "timetable weekly schedule lectures room", () -> navigateTo("timetable")));
        items.add(new SearchItem("🎟", "Exam Entry Coupon", "Sub Module", "Exam Portal", null, null, "exam coupon entrance pass ticket", () -> navigateTo("examcoupon")));
        items.add(new SearchItem("📱", "Mobile App Activation", "Sub Module", "Settings", null, null, "mobile app activation qr code scanner key", () -> navigateTo("mobileapp")));

        // 3. Sub-Modules for each course
        List<String> cachedCourses = getCachedCourseNames();
        for (String course : cachedCourses) {
            String cleanCourse = course.replaceAll("\\s+", " ").trim();
            items.add(new SearchItem("📊", cleanCourse + " Marks", "Sub Module", "Courses Portal", null, null, 
                (cleanCourse + " marks midterm final sessional score grade").toLowerCase(), 
                () -> navigateToCourseMarks(cleanCourse)));
            
            items.add(new SearchItem("📅", cleanCourse + " Class Proceedings", "Sub Module", "Courses Portal", null, null, 
                (cleanCourse + " class proceedings attendance lecture timeline").toLowerCase(), 
                () -> navigateToCourseProceedings(cleanCourse)));
        }

        // 4. Specific Assignments (from DB cache)
        try {
            for (com.assignly.model.Assignment ass : dataCacheService.getAssignments()) {
                String title = ass.getTitle().trim();
                String desc = ass.getDescription().trim();
                String kw = (title + " " + desc + " assignment task course portal pending due").toLowerCase();
                String status = ass.getStatus();
                String badgeColor = status.equalsIgnoreCase("submitted")
                    ? "-fx-background-color: #ccfbf1; -fx-text-fill: #0f766e; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10;"
                    : "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10;";
                
                items.add(new SearchItem("✍", title, "Assignment", "Course Portal", status, badgeColor, kw, () -> {
                    setPendingPortalTab("portal_assign_summ");
                    setPendingSearchHighlight(title);
                    navigateTo("portal");
                }));
            }
        } catch (Exception ignored) {}

        // 5. MCQ Tests (from cached HTML)
        dataCacheService.getCachedHtml("CTS/CTSdashboard.aspx").ifPresent(html -> {
            try {
                org.jsoup.nodes.Document doc = Jsoup.parse(html);
                org.jsoup.nodes.Element table = doc.getElementById("DataContent_gvCTSdashboard");
                if (table != null) {
                    org.jsoup.select.Elements rows = table.select("tr");
                    for (int i = 1; i < rows.size(); i++) {
                        org.jsoup.select.Elements tds = rows.get(i).select("td");
                        if (tds.size() > 4) {
                            String subject = tds.get(1).text().trim();
                            String testTitle = tds.get(2).text().trim();
                            String status = tds.get(4).text().trim();
                            
                            String kw = (testTitle + " mcq quiz test " + subject + " portal " + status).toLowerCase();
                            items.add(new SearchItem("📝", testTitle, "MCQ Test", subject, status, null, kw, () -> {
                                setPendingPortalTab("portal_mcq");
                                setPendingSearchHighlight(testTitle);
                                navigateTo("portal");
                            }));
                        }
                    }
                }
            } catch (Exception ignored) {}
        });

        // 6. Subjective Tests (from cached HTML)
        dataCacheService.getCachedHtml("CoursePortal.aspx?isTest=1").ifPresent(html -> {
            try {
                org.jsoup.nodes.Document doc = Jsoup.parse(html);
                org.jsoup.nodes.Element table = doc.getElementById("DataContent_gvPortalSummary");
                if (table != null) {
                    org.jsoup.select.Elements rows = table.select("tr");
                    for (int i = 1; i < rows.size(); i++) {
                        org.jsoup.select.Elements tds = rows.get(i).select("td");
                        if (tds.size() > 4) {
                            String subject = tds.get(1).text().trim();
                            String testTitle = tds.get(2).text().trim();
                            String status = tds.get(4).text().trim();
                            
                            if (!testTitle.contains("No Subjective Test") && !testTitle.contains("No Assignment")) {
                                String kw = (testTitle + " subjective quiz test assignment " + subject + " portal " + status).toLowerCase();
                                items.add(new SearchItem("📝", testTitle, "Subjective Test", subject, status, null, kw, () -> {
                                    setPendingPortalTab("portal_subjective");
                                    setPendingSearchHighlight(testTitle);
                                    navigateTo("portal");
                                }));
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        });

        // 7. Fee Challans (from cached HTML)
        dataCacheService.getCachedHtml("FeeChallans.aspx").ifPresent(html -> {
            try {
                org.jsoup.nodes.Document doc = Jsoup.parse(html);
                org.jsoup.select.Elements links = doc.select("a[id*=LinkButton], a[id*=btnDownload], a[id*=gvChallans]");
                for (org.jsoup.nodes.Element link : links) {
                    String desc = link.text().trim();
                    if (!desc.isEmpty() && !desc.toLowerCase().contains("download")) {
                        String kw = (desc + " fee challan challans portal").toLowerCase();
                        items.add(new SearchItem("💵", desc, "Fee Challan", "Fee Portal", "Download", null, kw, () -> {
                            setPendingFeeTab("fee_challans");
                            setPendingSearchHighlight(desc);
                            navigateTo("fee");
                        }));
                    }
                }
            } catch (Exception ignored) {}
        });

        // 8. Payment Records (Fee History)
        dataCacheService.getCachedHtml("FeeHistorySFMS.aspx").ifPresent(html -> {
            try {
                org.jsoup.nodes.Document doc = Jsoup.parse(html);
                org.jsoup.select.Elements tables = doc.select("table");
                for (org.jsoup.nodes.Element table : tables) {
                    org.jsoup.select.Elements rows = table.select("tr");
                    if (rows.size() < 2) continue;
                    
                    String text = table.text().toLowerCase();
                    if (!text.contains("receipt") && !text.contains("challan") && !text.contains("amount")) continue;
                    
                    for (int i = 1; i < rows.size(); i++) {
                        org.jsoup.select.Elements tds = rows.get(i).select("td");
                        if (tds.size() > 4) {
                            String receiptNo = tds.get(0).text().trim();
                            String semester = tds.get(1).text().trim();
                            String amount = tds.get(3).text().trim();
                            String date = tds.get(4).text().trim();
                            
                            if (!receiptNo.isEmpty()) {
                                String title = "Receipt #" + receiptNo + " (" + semester + ")";
                                String kw = (receiptNo + " fee history payment record receipt " + semester + " " + amount + " " + date).toLowerCase();
                                items.add(new SearchItem("💰", title, "Payment Record", "Fee Portal • " + date, "Rs. " + amount, null, kw, () -> {
                                    setPendingFeeTab("fee_history");
                                    setPendingSearchHighlight(receiptNo);
                                    navigateTo("fee");
                                }));
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        });

        // 9. Scholarship Records
        dataCacheService.getCachedHtml("scholarship/ViewScholarshipStatuse.aspx").ifPresent(html -> {
            try {
                org.jsoup.nodes.Document doc = Jsoup.parse(html);
                org.jsoup.select.Elements tables = doc.select("table");
                for (org.jsoup.nodes.Element table : tables) {
                    org.jsoup.select.Elements rows = table.select("tr");
                    if (rows.size() < 2) continue;
                    
                    String text = table.text().toLowerCase();
                    if (!text.contains("scholarship") && !text.contains("chequeno")) continue;
                    
                    for (int i = 1; i < rows.size(); i++) {
                        org.jsoup.select.Elements tds = rows.get(i).select("td");
                        if (tds.size() > 9) {
                            String name = tds.get(1).text().trim();
                            String date = tds.get(2).text().trim();
                            String session = tds.get(3).text().trim();
                            String total = tds.get(9).text().trim();
                            String cheque = tds.get(10).text().trim();
                            
                            if (!name.isEmpty()) {
                                String status = "Approved";
                                if (!cheque.equalsIgnoreCase("N/A") && !cheque.isBlank()) {
                                    status = "Disbursed";
                                }
                                String statusColor = status.equals("Disbursed") 
                                    ? "-fx-background-color: #ccfbf1; -fx-text-fill: #0f766e; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10;"
                                    : "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10;";
                                    
                                String kw = (name + " scholarship award " + session + " total " + total + " cheque " + cheque + " disbursed approved").toLowerCase();
                                items.add(new SearchItem("🎓", name, "Scholarship", session + " • " + date, status, statusColor, kw, () -> {
                                    setPendingScholarshipTab("status");
                                    setPendingSearchHighlight(name);
                                    navigateTo("scholarship");
                                }));
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        });

        // 10. Date Sheet / Exams
        try {
            for (String[] entry : getCachedDateSheetEntries()) {
                String course = entry[0].trim();
                String date = entry[1].trim();
                String time = entry[2].trim();
                String label = course + " Exam";
                String kw = (course + " exam date sheet paper schedule room hall " + date + " " + time).toLowerCase();
                items.add(new SearchItem("📆", label, "Exam Schedule", "Date Sheet • " + date + " @ " + time, "Exam", null, kw, () -> {
                    setPendingSearchHighlight(course);
                    navigateTo("examcoupon");
                }));
            }
        } catch (Exception ignored) {}

        // 11. Academic Results
        dataCacheService.getCachedHtml("StudentResultCard.aspx").ifPresent(html -> {
            try {
                org.jsoup.nodes.Document doc = Jsoup.parse(html);
                org.jsoup.select.Elements tables = doc.select("table");
                for (org.jsoup.nodes.Element table : tables) {
                    org.jsoup.select.Elements rows = table.select("tr");
                    if (rows.size() < 2) continue;
                    
                    org.jsoup.select.Elements firstRowCells = rows.first().select("th, td");
                    boolean isTranscript = false;
                    for (org.jsoup.nodes.Element th : firstRowCells) {
                        String lower = th.text().toLowerCase();
                        if (lower.contains("course") || lower.contains("credit") || lower.contains("marks") || lower.contains("grade")) {
                            isTranscript = true;
                            break;
                        }
                    }
                    if (!isTranscript) continue;
                    
                    String semesterTitle = "Semester Result";
                    org.jsoup.nodes.Element prev = table.previousElementSibling();
                    while (prev != null) {
                        String txt = prev.text().trim();
                        if (!txt.isEmpty()) {
                            semesterTitle = txt;
                            break;
                        }
                        prev = prev.previousElementSibling();
                    }
                    
                    for (int i = 1; i < rows.size(); i++) {
                        org.jsoup.select.Elements tds = rows.get(i).select("td");
                        if (tds.size() > 4) {
                            String code = tds.get(0).text().trim();
                            String subject = tds.get(1).text().trim();
                            String grade = tds.get(3).text().trim();
                            String marks = tds.get(4).text().trim();
                            
                            if (!subject.isEmpty() && !code.isEmpty()) {
                                String label = subject + " (" + code + ")";
                                String status = "Grade " + grade + " • " + marks + " Marks";
                                String statusColor = grade.startsWith("A") ? "-fx-background-color: #ccfbf1; -fx-text-fill: #0f766e; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 8; -fx-background-radius: 10;" : null;
                                String kw = (subject + " " + code + " academic result transcript grade " + grade + " marks " + marks + " " + semesterTitle).toLowerCase();
                                items.add(new SearchItem("📊", label, "Academic Result", semesterTitle, status, statusColor, kw, () -> {
                                    setPendingSearchHighlight(subject);
                                    navigateTo("result");
                                }));
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        });

        // 12. Announcements
        try {
            for (com.assignly.model.Announcement ann : dataCacheService.getAnnouncements()) {
                String title = ann.getTitle().trim();
                String content = ann.getContent().trim();
                String kw = (title + " " + content + " announcement notice portal board").toLowerCase();
                items.add(new SearchItem("📢", title, "Announcement", "Dashboard Portal", "Notice", null, kw, () -> {
                    setPendingSearchHighlight(title);
                    navigateTo("dashboard");
                }));
            }
        } catch (Exception ignored) {}



        allSearchItems = items;
    }



    private void navigateToCourseMarks(String courseName) {
        pendingCoursesTab = "marks";
        pendingCourseQuery = courseName;
        navigateTo("courses");
    }

    private void navigateToCourseProceedings(String courseName) {
        pendingCoursesTab = "proceedings";
        pendingCourseQuery = courseName;
        navigateTo("courses");
    }

    private List<String[]> getCachedDateSheetEntries() {
        List<String[]> entries = new ArrayList<>();
        String[] pages = {"DateSheet.aspx", "Datesheet.aspx", "ExamDateSheet.aspx", "ExamSchedule.aspx", "Dashboard.aspx"};
        for (String page : pages) {
            dataCacheService.getCachedHtml(page).ifPresent(html -> {
                org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
                for (org.jsoup.nodes.Element table : doc.select("table")) {
                    org.jsoup.select.Elements rows = table.select("tr");
                    if (rows.size() < 2) continue;

                    // Skip profile table
                    boolean isProfile = false;
                    for (org.jsoup.nodes.Element row : rows) {
                        String rt = row.text().toLowerCase();
                        if (rt.contains("father name") || rt.contains("roll no") || rt.contains("cnic")) {
                            isProfile = true;
                            break;
                        }
                    }
                    if (isProfile) continue;

                    org.jsoup.nodes.Element headerRow = rows.first();
                    if (headerRow == null) continue;
                    org.jsoup.select.Elements headerCells = headerRow.select("th, td");
                    String headerText = headerRow.text().toLowerCase();

                    boolean looksLikeDateSheet = headerText.contains("course") ||
                            headerText.contains("date") || headerText.contains("exam") ||
                            headerText.contains("time") || headerText.contains("paper");

                    if (!looksLikeDateSheet) continue;

                    int courseIdx = -1, dateIdx = -1, timeIdx = -1;
                    for (int i = 0; i < headerCells.size(); i++) {
                        String h = headerCells.get(i).text().toLowerCase().trim();
                        if (h.contains("course") || h.contains("subject") || h.contains("paper")) courseIdx = i;
                        else if (h.contains("date")) dateIdx = i;
                        else if (h.contains("time")) timeIdx = i;
                    }

                    if (courseIdx >= 0 || dateIdx >= 0) {
                        for (int r = 1; r < rows.size(); r++) {
                            org.jsoup.select.Elements cells = rows.get(r).select("td");
                            if (cells.isEmpty()) continue;

                            String course = (courseIdx >= 0 && courseIdx < cells.size()) ? cells.get(courseIdx).text().trim() : "";
                            String date = (dateIdx >= 0 && dateIdx < cells.size()) ? cells.get(dateIdx).text().trim() : "";
                            String time = (timeIdx >= 0 && timeIdx < cells.size()) ? cells.get(timeIdx).text().trim() : "";

                            if (!course.isBlank() && !date.isBlank()) {
                                entries.add(new String[]{course, date, time});
                            }
                        }
                        break;
                    }
                }
            });
            if (!entries.isEmpty()) break;
        }
        return entries;
    }

    private List<String> getCachedCourseNames() {
        Set<String> courses = new LinkedHashSet<>();
        String[] pages = {
            "classproceedings.aspx",
            "QAMarks.aspx",
            "QASessMarks.aspx",
            "QASessionMarks.aspx",
            "Marks.aspx",
            "CoursePortalContentsSummary.aspx",
            "CoursePortal.aspx"
        };

        for (String page : pages) {
            dataCacheService.getCachedHtml(page).ifPresent(html -> {
                List<String[]> options = portalRepository.parseDropdownOptions(html, "course");
                if (options.isEmpty()) {
                    options = portalRepository.parseDropdownOptions(html, "ddl");
                }
                for (String[] opt : options) {
                    if (opt.length > 1 && opt[1] != null && !opt[1].isBlank()) {
                        courses.add(opt[1].trim());
                    }
                }
            });
        }

        // 100% UNIVERSAL DISCOVERY: Harvest course names from cached datesheet entries to index newly synced semesters instantly
        try {
            for (String[] entry : getCachedDateSheetEntries()) {
                courses.add(entry[0].trim());
            }
        } catch (Exception ignored) {}

        return new ArrayList<>(courses);
    }
    
    private void updateStatusIndicator() {
        boolean currentOnline = checkConnectivity();
        boolean changed = (this.isOnline != currentOnline);
        this.isOnline = currentOnline;
        Optional<LocalDateTime> lastSync = dataCacheService.getCacheTimestamp("Dashboard.aspx");
        
        Platform.runLater(() -> {
            if (currentOnline) {
                statusIndicator.setFill(javafx.scene.paint.Color.web("#10b981")); // Green
                statusLabel.setText("Online");
            } else {
                statusIndicator.setFill(javafx.scene.paint.Color.web("#ef4444")); // Red
                statusLabel.setText("Offline — Cached Data");
            }
            
            if (lastSync.isPresent()) {
                java.time.Duration duration = java.time.Duration.between(lastSync.get(), LocalDateTime.now());
                long minutes = duration.toMinutes();
                if (minutes < 1) {
                    syncTimeLabel.setText("Last synced: Just now");
                } else if (minutes < 60) {
                    syncTimeLabel.setText("Last synced: " + minutes + " mins ago");
                } else {
                    syncTimeLabel.setText("Last synced: " + (minutes / 60) + " hours ago");
                }
            } else {
                syncTimeLabel.setText("Last synced: Never");
            }

            if (changed) {
                for (java.lang.ref.WeakReference<java.util.function.Consumer<Boolean>> ref : connectivityListeners) {
                    java.util.function.Consumer<Boolean> listener = ref.get();
                    if (listener != null) {
                        try {
                            listener.accept(currentOnline);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                connectivityListeners.removeIf(ref -> ref.get() == null);
            }
        });
    }

    private boolean checkConnectivity() {
        return portalRepository.checkConnectivity();
    }

    private VBox sidebarGroupHeader(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: rgba(240, 237, 236, 0.4); -fx-font-size: 9px; -fx-font-weight: 800; -fx-padding: 14 6 4 6; -fx-letter-spacing: 0.5px;");
        return new VBox(label);
    }

    private VBox buildSidebar() {
        VBox sb = new VBox(0);
        sb.getStyleClass().add("sidebar");
        sb.setPrefWidth(180);
        sb.setMinWidth(180);
        sb.setPadding(new Insets(20, 8, 16, 8));

        Label brand = new Label("Assignly");
        brand.getStyleClass().add("sidebar-brand");
        brand.setStyle("-fx-text-fill: #f0edec;");
        Label sub = new Label("Desktop Client");
        sub.getStyleClass().add("sidebar-brand-sub");
        sub.setStyle("-fx-text-fill: rgba(240, 237, 236, 0.5);");
        VBox brandBox = new VBox(2, brand, sub);
        brandBox.setPadding(new Insets(0, 6, 8, 6));

        // Group Navigation Buttons
        Button dashboard = navBtn("📊  Dashboard", "dashboard");
        Button courses = navBtn("📚  Courses", "courses");
        Button portal = navBtn("🌐  Course Portal", "portal");
        Button examCoupon = navBtn("🎟  Exam Entry", "examcoupon");
        Button mobileApp = navBtn("📱  App Activation", "mobileapp");
        Button result = navBtn("🎓  Results", "result");
        Button timetable = navBtn("📅  Timetable", "timetable");
        Button fee = navBtn("💰  Fee Portal", "fee");
        Button scholarship = navBtn("🏅  Scholarships", "scholarship");
        Button settings = navBtn("⚙️  Settings", "settings");

        VBox academicsGroup = new VBox(2);
        academicsGroup.getChildren().addAll(
            sidebarGroupHeader("ACADEMICS"),
            dashboard,
            courses,
            result,
            timetable
        );

        VBox financialGroup = new VBox(2);
        financialGroup.getChildren().addAll(
            sidebarGroupHeader("FINANCIAL"),
            fee,
            scholarship
        );

        VBox servicesGroup = new VBox(2);
        servicesGroup.getChildren().addAll(
            sidebarGroupHeader("SERVICES"),
            portal,
            examCoupon,
            mobileApp
        );

        VBox systemGroup = new VBox(2);
        systemGroup.getChildren().addAll(
            sidebarGroupHeader("SYSTEM"),
            settings
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Compact Discord-style sidebar profile section
        sidebarAvatar = new ImageView();
        sidebarAvatar.setFitWidth(30);
        sidebarAvatar.setFitHeight(30);
        sidebarAvatar.setPreserveRatio(false);
        sidebarAvatar.setSmooth(true);
        Circle clip = new Circle(15, 15, 15);
        sidebarAvatar.setClip(clip);

        Label avatarPlaceholder = new Label("👤");
        avatarPlaceholder.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.4);");
        avatarPlaceholder.visibleProperty().bind(sidebarAvatar.imageProperty().isNull());

        StackPane avatarBox = new StackPane(sidebarAvatar, avatarPlaceholder);
        avatarBox.setPrefSize(30, 30);
        avatarBox.setMaxSize(30, 30);

        sidebarNameLabel = new Label("Loading...");
        sidebarNameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
        sidebarNameLabel.setMaxWidth(130);

        sidebarProgramLabel = new Label("");
        sidebarProgramLabel.setStyle("-fx-text-fill: rgba(240, 237, 236, 0.5); -fx-font-size: 10px;");

        VBox profileInfo = new VBox(2, sidebarNameLabel, sidebarProgramLabel);
        profileInfo.setAlignment(Pos.CENTER_LEFT);

        HBox userRow = new HBox(8, avatarBox, profileInfo);
        userRow.setAlignment(Pos.CENTER_LEFT);
        userRow.setPadding(new Insets(4, 0, 4, 0));

        Button accountDropdownBtn = new Button("Account  ▼");
        accountDropdownBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-text-fill: rgba(255,255,255,0.8);" +
            "-fx-cursor: hand;" +
            "-fx-padding: 6 12;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 6;" +
            "-fx-max-width: infinity;"
        );
        accountDropdownBtn.setOnMouseEntered(e -> accountDropdownBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.12);" +
            "-fx-text-fill: white;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 6 12;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 6;" +
            "-fx-max-width: infinity;"
        ));
        accountDropdownBtn.setOnMouseExited(e -> accountDropdownBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-text-fill: rgba(255,255,255,0.8);" +
            "-fx-cursor: hand;" +
            "-fx-padding: 6 12;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 6;" +
            "-fx-max-width: infinity;"
        ));

        VBox accountDropdownContent = new VBox(4);
        accountDropdownContent.setVisible(false);
        accountDropdownContent.setManaged(false);
        accountDropdownContent.setPadding(new Insets(4, 0, 4, 10));

        Button subProfileBtn = new Button("👤   Profile");
        subProfileBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.7); -fx-cursor: hand; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-max-width: infinity;");
        subProfileBtn.setOnMouseEntered(e -> subProfileBtn.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-max-width: infinity; -fx-background-radius: 4;"));
        subProfileBtn.setOnMouseExited(e -> subProfileBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.7); -fx-cursor: hand; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-max-width: infinity;"));
        subProfileBtn.setOnAction(e -> {
            setPendingSettingsSubTab("profile");
            navigateTo("settings");
        });

        Button subSettingsBtn = new Button("⚙   Settings");
        subSettingsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.7); -fx-cursor: hand; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-max-width: infinity;");
        subSettingsBtn.setOnMouseEntered(e -> subSettingsBtn.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-max-width: infinity; -fx-background-radius: 4;"));
        subSettingsBtn.setOnMouseExited(e -> subSettingsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.7); -fx-cursor: hand; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-max-width: infinity;"));
        subSettingsBtn.setOnAction(e -> navigateTo("settings"));

        Button subSignOutBtn = new Button("🚪   Sign Out");
        subSignOutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.7); -fx-cursor: hand; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-max-width: infinity;");
        subSignOutBtn.setOnMouseEntered(e -> subSignOutBtn.setStyle("-fx-background-color: rgba(255,100,100,0.12); -fx-text-fill: #ff6b6b; -fx-cursor: hand; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-max-width: infinity; -fx-background-radius: 4;"));
        subSignOutBtn.setOnMouseExited(e -> subSignOutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.7); -fx-cursor: hand; -fx-padding: 6 12; -fx-font-size: 11px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-max-width: infinity;"));
        subSignOutBtn.setOnAction(e -> {
            credentialManager.clearRememberMe();
            clearSessionCredentials();
            showLoginScreen();
        });

        accountDropdownContent.getChildren().addAll(subProfileBtn, subSettingsBtn, subSignOutBtn);

        accountDropdownBtn.setOnAction(e -> {
            boolean isExpanded = accountDropdownContent.isManaged();
            if (isExpanded) {
                accountDropdownContent.setVisible(false);
                accountDropdownContent.setManaged(false);
                accountDropdownBtn.setText("Account  ▼");
            } else {
                accountDropdownContent.setVisible(true);
                accountDropdownContent.setManaged(true);
                accountDropdownBtn.setText("Account  ▲");
                
                // Dropdown fade transition
                javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), accountDropdownContent);
                ft.setFromValue(0.0);
                ft.setToValue(1.0);
                
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), accountDropdownContent);
                tt.setFromY(-5);
                tt.setToY(0);
                
                javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(ft, tt);
                pt.play();
            }
        });

        VBox profileFooter = new VBox(8, userRow, accountDropdownBtn, accountDropdownContent);
        profileFooter.getStyleClass().add("sidebar-profile-footer");
        profileFooter.setPadding(new Insets(12, 10, 12, 10));
        profileFooter.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.15);" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: rgba(255,255,255,0.08);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;"
        );
        VBox.setMargin(profileFooter, new Insets(12, 4, 0, 4));

        sb.getChildren().addAll(brandBox, academicsGroup, financialGroup, servicesGroup, systemGroup, spacer, new Separator(), profileFooter);
        return sb;
    }

    private String activeTabId = "dashboard";
    private boolean forceRefreshActiveTab = false;

    private String formatTabTitle(String tabId) {
        return switch (tabId) {
            case "dashboard" -> "Dashboard";
            case "courses" -> "My Courses";
            case "portal" -> "Course Portal";
            case "examcoupon" -> "Exam Entry Coupon";
            case "mobileapp" -> "Mobile App Activation";
            case "result" -> "Transcript & Results";
            case "timetable" -> "Class Timetable";
            case "fee" -> "Fee Portal";
            case "scholarship" -> "Scholarship Portal";
            case "settings" -> "Settings";
            default -> Character.toUpperCase(tabId.charAt(0)) + tabId.substring(1);
        };
    }

    public void syncPortalData() {
        if (isSyncing) return;
        isSyncing = true;
        
        Platform.runLater(() -> {
            if (syncPortalBtn != null) {
                syncPortalBtn.setDisable(true);
                syncPortalBtn.setText("🔄 Syncing (0%)...");
            }
            notificationService.showInfo("Starting portal sync...");
        });

        new Thread(() -> {
            String[] urlsToSync = {
                "Dashboard.aspx",
                "CoursePortal.aspx",
                "Timetable.aspx",
                "FeeChallans.aspx",
                "FeeHistorySFMS.aspx",
                "StudentResultCard.aspx",
                "DateSheet.aspx",
                "Summary.aspx",
                "classproceedings.aspx",
                "QAMarks.aspx",
                "EntryCouponSelect.aspx",
                "EntryCouponWithQR.aspx",
                "AddCellEmailInfo.aspx",
                "LoginHistory.aspx",
                "scholarship/ViewScholarshipStatuse.aspx",
                "CTS/CTSdashboard.aspx",
                "CoursePortal.aspx?isTest=1",
                "CoursePortalContentsSummary.aspx",
                "CoursePortalPendingAssignments.aspx"
            };

            int total = urlsToSync.length;
            for (int i = 0; i < total; i++) {
                final int currentProgress = (int) (((double) (i + 1) / total) * 100);
                String url = urlsToSync[i];
                try {
                    fetchAndCacheHtml(url);
                } catch (Exception ignored) {}
                
                Platform.runLater(() -> {
                    if (syncPortalBtn != null) {
                        syncPortalBtn.setText("🔄 Syncing (" + currentProgress + "%)...");
                    }
                });
            }

            try {
                String liveDash = dataCacheService.getCachedHtml("Dashboard.aspx").orElse(null);
                if (liveDash != null) {
                    PortalRepository.DashboardData liveData = portalRepository.parseDashboard(liveDash);
                    if (liveData != null) {
                        String name = portalRepository.getCurrentStudentName();
                        String program = liveData.studentInfo().get("Program");
                        updateSidebarProfile(name, program);
                    }
                }
            } catch (Exception ignored) {}

            try {
                byte[] logoBytes = portalRepository.fetchPhotoBytes("https://sis.cuiatd.edu.pk/resources/images/CIITLogo_Plain.png");
                if (logoBytes != null && logoBytes.length > 0) {
                    java.nio.file.Files.write(java.nio.file.Path.of(com.assignly.util.AppDirectoryHelper.getAppDataDir(), "cui_logo.png"), logoBytes);
                }
            } catch (Exception ignored) {}

            String photoUrl = portalRepository.getCurrentStudentPhotoUrl();
            if (photoUrl != null && !photoUrl.isBlank()) {
                byte[] photoBytes = portalRepository.fetchPhotoBytes(photoUrl);
                if (photoBytes != null && photoBytes.length > 0) {
                    Platform.runLater(() -> {
                        if (sidebarAvatar != null) {
                            Image img = new Image(new ByteArrayInputStream(photoBytes));
                            double iw = img.getWidth();
                            double ih = img.getHeight();
                            if (iw > 0 && ih > 0) {
                                double s = Math.min(iw, ih);
                                double x = (iw - s) / 2;
                                double y = (ih - s) / 2;
                                sidebarAvatar.setViewport(new javafx.geometry.Rectangle2D(x, y, s, s));
                            }
                            sidebarAvatar.setFitWidth(30);
                            sidebarAvatar.setFitHeight(30);
                            sidebarAvatar.setPreserveRatio(false);
                            sidebarAvatar.setImage(img);
                        }
                    });
                }
            }
            try {
                syncCourseSpecificData();
            } catch (Exception ignored) {}

            isSyncing = false;

            Platform.runLater(() -> {
                if (syncPortalBtn != null) {
                    syncPortalBtn.setDisable(false);
                    syncPortalBtn.setText("🔄 Sync Portal");
                }
                notificationService.showSuccess("Portal sync complete!");
                updateStatusIndicator();
                refreshSearchItems();
                navigateTo(activeTabId);
            });
        }).start();
    }

    private void triggerActiveTabRefresh() {
        if (activeTabId != null) {
            forceRefreshActiveTab = true;
            navigateTo(activeTabId);
            forceRefreshActiveTab = false;
        }
    }

    private HBox buildTopHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("top-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 24, 10, 24)); // Reduced vertical padding from 14 to 10 for sleek alignment

        headerTitleLabel = new Label("Dashboard");
        headerTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button searchBtn = new Button("🔍 Search... (Ctrl + K)");
        searchBtn.getStyleClass().add("header-search-button");
        searchBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        searchBtn.setPrefWidth(200);
        searchBtn.setMinWidth(160);
        searchBtn.setMaxWidth(220);
        searchBtn.setOnAction(e -> showSearchPopup());

        String iconBtnNormal = "-fx-background-color: transparent; -fx-text-fill: -color-text-muted; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 6; -fx-background-radius: 6;";
        String iconBtnHover = "-fx-background-color: rgba(20, 184, 166, 0.08); -fx-text-fill: -color-accent; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 6; -fx-background-radius: 6;";

        Button notificationBtn = new Button("🔔");
        notificationBtn.setStyle(iconBtnNormal);
        notificationBtn.setOnMouseEntered(e -> notificationBtn.setStyle(iconBtnHover));
        notificationBtn.setOnMouseExited(e -> notificationBtn.setStyle(iconBtnNormal));
        notificationBtn.setOnAction(e -> notificationService.showInfo("No new notifications."));

        syncPortalBtn = new Button("🔄 Sync Portal");
        syncPortalBtn.setStyle(
            "-fx-background-color: -color-accent;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 11px;" +
            "-fx-padding: 6 14;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;"
        );
        syncPortalBtn.setOnMouseEntered(e -> syncPortalBtn.setOpacity(0.9));
        syncPortalBtn.setOnMouseExited(e -> syncPortalBtn.setOpacity(1.0));
        syncPortalBtn.setOnAction(e -> syncPortalData());

        Button profileGearBtn = new Button("👤");
        profileGearBtn.setStyle(iconBtnNormal);
        profileGearBtn.setOnMouseEntered(e -> profileGearBtn.setStyle(iconBtnHover));
        profileGearBtn.setOnMouseExited(e -> profileGearBtn.setStyle(iconBtnNormal));
        profileGearBtn.setOnAction(e -> navigateTo("settings"));

        header.getChildren().addAll(headerTitleLabel, spacer, searchBtn, syncPortalBtn, notificationBtn, profileGearBtn);
        return header;
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
        activeTabId = tabId;
        if (headerTitleLabel != null) {
            headerTitleLabel.setText(formatTabTitle(tabId));
        }
        updateActiveNavState(sidebar, tabId);

        javafx.scene.Node newTab = switch (tabId) {
            case "dashboard" -> new DashboardTabView(this, forceRefreshActiveTab).getRoot();
            case "courses" -> {
                String tab = pendingCoursesTab;
                String query = pendingCourseQuery;
                pendingCoursesTab = null;
                pendingCourseQuery = null;
                yield (tab != null || query != null)
                    ? new CoursesTabView(this, tab, query).getRoot()
                    : new CoursesTabView(this).getRoot();
            }
            case "portal" -> {
                String initialTab = getPendingPortalTab();
                yield (initialTab != null && !initialTab.isBlank())
                    ? new CoursePortalTabView(this, initialTab).getRoot()
                    : new CoursePortalTabView(this).getRoot();
            }
            case "examcoupon" -> new ExamCouponTabView(this).getRoot();
            case "mobileapp" -> new MobileAppActivationTabView(this).getRoot();
            case "result" -> new ResultTabView(this).getRoot();
            case "timetable" -> new TimetableTabView(this).getRoot();
            case "fee" -> {
                String initialTab = getPendingFeeTab();
                yield (initialTab != null && !initialTab.isBlank())
                    ? new FeeTabView(this, initialTab).getRoot()
                    : new FeeTabView(this).getRoot();
            }
            case "scholarship" -> {
                String initialTab = getPendingScholarshipTab();
                yield (initialTab != null && !initialTab.isBlank())
                    ? new ScholarshipTabView(this, initialTab).getRoot()
                    : new ScholarshipTabView(this).getRoot();
            }
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

    public String getPendingPortalTab() {
        String t = pendingPortalTab;
        pendingPortalTab = null;
        return t;
    }
    public void setPendingPortalTab(String t) { this.pendingPortalTab = t; }

    public String getPendingFeeTab() {
        String t = pendingFeeTab;
        pendingFeeTab = null;
        return t;
    }
    public void setPendingFeeTab(String t) { this.pendingFeeTab = t; }

    public String getPendingScholarshipTab() {
        String t = pendingScholarshipTab;
        pendingScholarshipTab = null;
        return t;
    }
    public void setPendingScholarshipTab(String t) { this.pendingScholarshipTab = t; }

    public String getPendingSearchHighlight() {
        return pendingSearchHighlight;
    }
    public void setPendingSearchHighlight(String q) { this.pendingSearchHighlight = q; }
    public void clearPendingSearchHighlight() { this.pendingSearchHighlight = null; }

    public String getPendingSettingsSubTab() { return pendingSettingsSubTab; }
    public void setPendingSettingsSubTab(String val) { this.pendingSettingsSubTab = val; }
    public void clearPendingSettingsSubTab() { this.pendingSettingsSubTab = null; }



    // ---------- Scene ----------
    public void setScene(Parent contentRoot, String title) {
        contentRoot.getStyleClass().add("app-root");

        HBox titleBar = buildCustomTitleBar();
        VBox wrapper = new VBox(titleBar, contentRoot);
        VBox.setVgrow(contentRoot, Priority.ALWAYS);
        wrapper.setStyle("-fx-background-color: -color-bg-main;");

        StackPane root = new StackPane(wrapper);
        notificationService.initToastLayer(root);

        Scene scene = new Scene(root, 1200, 750);
        URL css = getClass().getResource("/com/assignly/styles/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        
        // Dark mode hook (we will implement this fully later)
        applyTheme(scene);

        stage.setTitle(title);
        stage.setScene(scene);

        ResizeHelper.addResizeListener(stage);
    }

    private HBox buildCustomTitleBar() {
        HBox titleBar = new HBox(12);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(0, 0, 0, 16));
        titleBar.setPrefHeight(32);
        titleBar.setMinHeight(32);
        titleBar.setMaxHeight(32);
        titleBar.setStyle("-fx-background-color: -color-bg-sidebar; -fx-border-color: -color-border; -fx-border-width: 0 0 1 0;");

        // 1. Logo
        ImageView logoView = new ImageView();
        URL logoUrl = getClass().getResource("/com/assignly/images/assignly_logo.png");
        if (logoUrl != null) {
            Image logoImg = new Image(logoUrl.toExternalForm(), 16, 16, true, true);
            logoView.setImage(logoImg);
            logoView.setFitWidth(16);
            logoView.setFitHeight(16);
            logoView.setSmooth(true);
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(16, 16);
            clip.setArcWidth(4);
            clip.setArcHeight(4);
            logoView.setClip(clip);
        }

        // 2. Title Text - Increased size & font weight
        Label appTitleLabel = new Label("Assignly Desktop");
        appTitleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: rgba(240, 237, 236, 0.9);");

        // HBox with spacing 8 for logo & text
        HBox brandingBox = new HBox(8, logoView, appTitleLabel);
        brandingBox.setAlignment(Pos.CENTER_LEFT);

        // 3. Center Spacer / Drag Area
        Region dragSpacer = new Region();
        HBox.setHgrow(dragSpacer, Priority.ALWAYS);

        // Styling tokens for controls
        String controlNormalStyle = "-fx-background-color: transparent; -fx-background-radius: 0; -fx-padding: 0;";
        String controlHoverStyle = "-fx-background-color: rgba(240, 237, 236, 0.1); -fx-background-radius: 0; -fx-padding: 0;";
        String controlPressedStyle = "-fx-background-color: rgba(240, 237, 236, 0.18); -fx-background-radius: 0; -fx-padding: 0;";

        // 4. Minimize button
        Button minBtn = new Button();
        minBtn.setPrefSize(45, 32);
        minBtn.setMinSize(45, 32);
        minBtn.setMaxSize(45, 32);
        minBtn.setCursor(javafx.scene.Cursor.HAND);
        minBtn.setFocusTraversable(false);
        
        Label minIcon = new Label("🗕");
        minIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: rgba(240, 237, 236, 0.7); -fx-font-family: 'Segoe UI Symbol';");
        minBtn.setGraphic(minIcon);

        minBtn.setStyle(controlNormalStyle);
        minBtn.setOnMouseEntered(e -> minBtn.setStyle(controlHoverStyle));
        minBtn.setOnMouseExited(e -> minBtn.setStyle(controlNormalStyle));
        minBtn.setOnMousePressed(e -> {
            minBtn.setStyle(controlPressedStyle);
            e.consume();
        });
        minBtn.setOnMouseReleased(e -> {
            minBtn.setStyle(controlNormalStyle);
            e.consume();
        });
        minBtn.setOnAction(e -> stage.setIconified(true));

        // 5. Maximize / Restore button
        Button maxBtn = new Button();
        maxBtn.setPrefSize(45, 32);
        maxBtn.setMinSize(45, 32);
        maxBtn.setMaxSize(45, 32);
        maxBtn.setCursor(javafx.scene.Cursor.HAND);
        maxBtn.setFocusTraversable(false);
        
        Label maxIcon = new Label("🗖");
        maxIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: rgba(240, 237, 236, 0.7); -fx-font-family: 'Segoe UI Symbol';");
        maxBtn.setGraphic(maxIcon);

        maxBtn.setStyle(controlNormalStyle);
        maxBtn.setOnMouseEntered(e -> {
            if (!maxBtn.isDisable()) maxBtn.setStyle(controlHoverStyle);
        });
        maxBtn.setOnMouseExited(e -> {
            if (!maxBtn.isDisable()) maxBtn.setStyle(controlNormalStyle);
        });
        maxBtn.setOnMousePressed(e -> {
            if (!maxBtn.isDisable()) maxBtn.setStyle(controlPressedStyle);
            e.consume();
        });
        maxBtn.setOnMouseReleased(e -> {
            if (!maxBtn.isDisable()) maxBtn.setStyle(controlNormalStyle);
            e.consume();
        });
        maxBtn.setOnAction(e -> {
            if (stage.isResizable()) {
                stage.setMaximized(!stage.isMaximized());
            }
        });

        // Bind disable status to Stage.resizableProperty().not()
        maxBtn.disableProperty().bind(stage.resizableProperty().not());
        maxBtn.disableProperty().addListener((obs, oldVal, newVal) -> {
            maxBtn.setOpacity(newVal ? 0.3 : 1.0);
        });

        // Update Maximize/Restore icon based on stage state
        stage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                maxIcon.setText("🗗");
            } else {
                maxIcon.setText("🗖");
            }
        });

        // 6. Close button
        Button closeBtn = new Button();
        closeBtn.setPrefSize(45, 32);
        closeBtn.setMinSize(45, 32);
        closeBtn.setMaxSize(45, 32);
        closeBtn.setCursor(javafx.scene.Cursor.HAND);
        closeBtn.setFocusTraversable(false);

        Label closeIcon = new Label("✕");
        closeIcon.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: rgba(240, 237, 236, 0.7); -fx-font-family: 'Segoe UI Symbol';");
        closeBtn.setGraphic(closeIcon);

        String closeNormalStyle = "-fx-background-color: transparent; -fx-background-radius: 0; -fx-padding: 0;";
        String closeHoverStyle = "-fx-background-color: #dc2626; -fx-background-radius: 0; -fx-padding: 0;";
        String closePressedStyle = "-fx-background-color: #b91c1c; -fx-background-radius: 0; -fx-padding: 0;";

        closeBtn.setStyle(closeNormalStyle);
        closeBtn.setOnMouseEntered(e -> {
            closeBtn.setStyle(closeHoverStyle);
            closeIcon.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: 'Segoe UI Symbol';");
        });
        closeBtn.setOnMouseExited(e -> {
            closeBtn.setStyle(closeNormalStyle);
            closeIcon.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: rgba(240, 237, 236, 0.7); -fx-font-family: 'Segoe UI Symbol';");
        });
        closeBtn.setOnMousePressed(e -> {
            closeBtn.setStyle(closePressedStyle);
            e.consume();
        });
        closeBtn.setOnMouseReleased(e -> {
            closeBtn.setStyle(closeNormalStyle);
            e.consume();
        });
        closeBtn.setOnAction(e -> stage.close());

        // Assembly
        titleBar.getChildren().addAll(brandingBox, dragSpacer, minBtn, maxBtn, closeBtn);

        // Draggable window handlers
        final double[] xOffset = new double[1];
        final double[] yOffset = new double[1];

        titleBar.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            if (!stage.isMaximized()) {
                stage.setX(event.getScreenX() - xOffset[0]);
                stage.setY(event.getScreenY() - yOffset[0]);
            }
        });

        titleBar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                if (stage.isResizable()) {
                    stage.setMaximized(!stage.isMaximized());
                }
            }
        });

        return titleBar;
    }
    
    public void applyTheme(Scene scene) {
        if ("DARK".equalsIgnoreCase(preferencesService.loadPreferences().getTheme())) {
            scene.getRoot().getStyleClass().add("dark-theme");
        } else {
            scene.getRoot().getStyleClass().remove("dark-theme");
        }
        applyDarkOverlayToAllWebViews();
        applyZoomLevelToAllWebViews();
    }

    public void applyDarkOverlayToAllWebViews() {
        if (stage.getScene() == null) return;
        boolean enabled = preferencesService.loadPreferences().isDarkOverlay();
        applyDarkOverlayRecursively(stage.getScene().getRoot(), enabled);
    }

    private void applyDarkOverlayRecursively(Node node, boolean enabled) {
        if (node instanceof WebView webView) {
            portalService.applyDarkOverlay(webView.getEngine(), enabled);
        } else if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyDarkOverlayRecursively(child, enabled);
            }
        }
    }

    public void applyZoomLevelToAllWebViews() {
        if (stage.getScene() == null) return;
        double zoom = preferencesService.loadPreferences().getZoomLevel();
        applyZoomLevelRecursively(stage.getScene().getRoot(), zoom);
    }

    private void applyZoomLevelRecursively(Node node, double zoom) {
        if (node instanceof WebView webView) {
            webView.setZoom(zoom);
        } else if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyZoomLevelRecursively(child, zoom);
            }
        }
    }

    // ---------- Accessors ----------
    public Stage stage() { return stage; }
    public DatabaseManager databaseManager() { return databaseManager; }
    public CredentialManager credentialManager() { return credentialManager; }
    public PreferencesService preferencesService() { return preferencesService; }
    public DataCacheService dataCacheService() { return dataCacheService; }
    public PortalService portalService() { return portalService; }
    public NotificationService notificationService() { return notificationService; }
    public PortalRepository portalRepository() { return portalRepository; }

    public void setSessionCredentials(String reg, String pass) {
        this.sessionRegistration = reg;
        this.sessionPassword = pass;
    }
    public String getSessionRegistration() { return sessionRegistration; }
    public String getSessionPassword() { return sessionPassword; }
    public String fetchAndCacheHtml(String relativeUrl) {
        boolean[] isCreator = {false};
        java.util.concurrent.CompletableFuture<String> future = inFlightCaches.computeIfAbsent(relativeUrl, k -> {
            isCreator[0] = true;
            return new java.util.concurrent.CompletableFuture<>();
        });

        if (isCreator[0]) {
            try {
                String html = portalRepository.fetchPageHtml(relativeUrl);
                if (html != null) {
                    dataCacheService.cacheHtml(relativeUrl, html);
                }
                future.complete(html);
                return html;
            } catch (Throwable t) {
                future.completeExceptionally(t);
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                return null;
            } finally {
                inFlightCaches.remove(relativeUrl);
            }
        } else {
            try {
                return future.join();
            } catch (java.util.concurrent.CompletionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                return null;
            }
        }
    }

    public void clearSessionCredentials() {
        this.sessionRegistration = null;
        this.sessionPassword = null;
        if (this.dataCacheService != null) {
            try {
                this.dataCacheService.clearAllCaches();
            } catch (Exception ignored) {}
        }
        if (this.portalRepository != null) {
            try {
                this.portalRepository.clearSessionState();
            } catch (Exception ignored) {}
        }
        if (this.sidebarAvatar != null) {
            try {
                this.sidebarAvatar.setImage(null);
            } catch (Exception ignored) {}
        }
    }

    public void syncCourseSpecificData() {
        try {
            String resultHtml = dataCacheService.getCachedHtml("StudentResultCard.aspx").orElse(null);
            java.util.Map<String, String> courseNames = portalRepository.parseCourseNames(resultHtml);
            String summaryHtml = dataCacheService.getCachedHtml("Summary.aspx").orElse(null);
            if (summaryHtml != null) {
                List<com.assignly.view.CoursesTabView.CourseSummary> courses = com.assignly.view.CoursesTabView.parseSummaryCourses(summaryHtml, courseNames);
                
                for (com.assignly.view.CoursesTabView.CourseSummary c : courses) {
                    String cleanCode = c.code.isEmpty() ? c.title : c.code;
                    if (cleanCode.isEmpty()) continue;
                    
                    if (c.postbackTarget != null && !c.postbackTarget.isEmpty()) {
                        try {
                            // 1. Select the course context on Summary.aspx
                            portalRepository.postbackEventStandard("Summary.aspx", c.postbackTarget);
                            
                            // 2. Fetch Class Proceedings for Course
                            String postResult = portalRepository.fetchPageHtml("classproceedings.aspx");
                            if (postResult != null && !postResult.contains("Please select a course")) {
                                dataCacheService.cacheHtml("classproceedings.aspx_" + cleanCode, postResult);
                                if (!c.code.isEmpty() && !c.code.equals(cleanCode)) {
                                    dataCacheService.cacheHtml("classproceedings.aspx_" + c.code, postResult);
                                }
                            }
                            
                            // 3. Fetch Marks for Course
                            String marksResult = portalRepository.fetchPageHtml("QAMarks.aspx");
                            if (marksResult != null && !marksResult.contains("Please select a course")) {
                                dataCacheService.cacheHtml("QAMarks.aspx_" + cleanCode, marksResult);
                                if (!c.code.isEmpty() && !c.code.equals(cleanCode)) {
                                    dataCacheService.cacheHtml("QAMarks.aspx_" + c.code, marksResult);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Failed syncing course: " + cleanCode + ", error: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Failed course-specific sync: " + ex.getMessage());
        }
    }

    private record SearchItem(
        String icon,
        String title,
        String category,
        String parentPath,
        String statusBadge,
        String statusColor,
        String keywords,
        Runnable action
    ) {}

    // ---------- Window Frame Resize Helper ----------
    private static class ResizeHelper {
        public static void addResizeListener(javafx.stage.Stage stage) {
            javafx.scene.Scene scene = stage.getScene();
            if (scene == null) return;

            ResizeListener resizeListener = new ResizeListener(stage);
            scene.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_MOVED, resizeListener);
            scene.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, resizeListener);
            scene.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, resizeListener);
            scene.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_EXITED, resizeListener);
        }

        private static class ResizeListener implements javafx.event.EventHandler<javafx.scene.input.MouseEvent> {
            private final javafx.stage.Stage stage;
            private double startX = 0;
            private double startY = 0;
            private double startScreenX = 0;
            private double startScreenY = 0;
            private double startWidth = 0;
            private double startHeight = 0;
            private boolean resizeX = false;
            private boolean resizeY = false;
            private boolean resizeXReverse = false;
            private boolean resizeYReverse = false;
            private final int border = 6;

            public ResizeListener(javafx.stage.Stage stage) {
                this.stage = stage;
            }

            @Override
            public void handle(javafx.scene.input.MouseEvent event) {
                javafx.event.EventType<?> type = event.getEventType();
                javafx.scene.Scene scene = stage.getScene();
                if (scene == null) return;

                double mouseX = event.getSceneX();
                double mouseY = event.getSceneY();

                if (javafx.scene.input.MouseEvent.MOUSE_MOVED.equals(type)) {
                    if (!stage.isResizable() || stage.isMaximized()) {
                        scene.setCursor(javafx.scene.Cursor.DEFAULT);
                        return;
                    }
                    
                    double width = stage.getWidth();
                    double height = stage.getHeight();
                    
                    boolean top = mouseY < border;
                    boolean bottom = mouseY > height - border;
                    boolean left = mouseX < border;
                    boolean right = mouseX > width - border;

                    if (top && left) {
                        scene.setCursor(javafx.scene.Cursor.NW_RESIZE);
                    } else if (top && right) {
                        scene.setCursor(javafx.scene.Cursor.NE_RESIZE);
                    } else if (bottom && left) {
                        scene.setCursor(javafx.scene.Cursor.SW_RESIZE);
                    } else if (bottom && right) {
                        scene.setCursor(javafx.scene.Cursor.SE_RESIZE);
                    } else if (top) {
                        scene.setCursor(javafx.scene.Cursor.N_RESIZE);
                    } else if (bottom) {
                        scene.setCursor(javafx.scene.Cursor.S_RESIZE);
                    } else if (left) {
                        scene.setCursor(javafx.scene.Cursor.W_RESIZE);
                    } else if (right) {
                        scene.setCursor(javafx.scene.Cursor.E_RESIZE);
                    } else {
                        scene.setCursor(javafx.scene.Cursor.DEFAULT);
                    }
                } else if (javafx.scene.input.MouseEvent.MOUSE_EXITED.equals(type)) {
                    scene.setCursor(javafx.scene.Cursor.DEFAULT);
                } else if (javafx.scene.input.MouseEvent.MOUSE_PRESSED.equals(type)) {
                    if (!stage.isResizable() || stage.isMaximized()) return;

                    double width = stage.getWidth();
                    double height = stage.getHeight();

                    resizeX = false;
                    resizeY = false;
                    resizeXReverse = false;
                    resizeYReverse = false;

                    if (mouseX < border) {
                        resizeX = true;
                        resizeXReverse = true;
                    } else if (mouseX > width - border) {
                        resizeX = true;
                    }

                    if (mouseY < border) {
                        resizeY = true;
                        resizeYReverse = true;
                    } else if (mouseY > height - border) {
                        resizeY = true;
                    }

                    if (resizeX || resizeY) {
                        startX = mouseX;
                        startY = mouseY;
                        startScreenX = event.getScreenX();
                        startScreenY = event.getScreenY();
                        startWidth = stage.getWidth();
                        startHeight = stage.getHeight();
                        event.consume();
                    }
                } else if (javafx.scene.input.MouseEvent.MOUSE_DRAGGED.equals(type)) {
                    if (resizeX || resizeY) {
                        double deltaX = event.getScreenX() - startScreenX;
                        double deltaY = event.getScreenY() - startScreenY;

                        if (resizeX) {
                            if (resizeXReverse) {
                                double newWidth = startWidth - deltaX;
                                if (newWidth > stage.getMinWidth()) {
                                    stage.setWidth(newWidth);
                                    stage.setX(event.getScreenX() - startX);
                                }
                            } else {
                                double newWidth = startWidth + deltaX;
                                if (newWidth > stage.getMinWidth()) {
                                    stage.setWidth(newWidth);
                                }
                            }
                        }

                        if (resizeY) {
                            if (resizeYReverse) {
                                double newHeight = startHeight - deltaY;
                                if (newHeight > stage.getMinHeight()) {
                                    stage.setHeight(newHeight);
                                    stage.setY(event.getScreenY() - startY);
                                }
                            } else {
                                double newHeight = startHeight + deltaY;
                                if (newHeight > stage.getMinHeight()) {
                                    stage.setHeight(newHeight);
                                }
                            }
                        }
                        event.consume();
                    }
                }
            }
        }
    }
}
