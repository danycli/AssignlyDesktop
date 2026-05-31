package com.assignly.util;

import com.assignly.database.DatabaseManager;
import com.assignly.model.UserPreferences;
import com.assignly.service.CredentialManager;
import com.assignly.service.DataCacheService;
import com.assignly.service.PortalRepository;
import com.assignly.service.PortalService;
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
    private VBox toastContainer;

    // QoL Features
    private TextField searchField;
    private ListView<SearchItem> searchResults;
    private Popup searchPopup;
    private final ObservableList<SearchItem> filteredSearchItems = FXCollections.observableArrayList();
    private List<SearchItem> allSearchItems = List.of();
    private ImageView sidebarAvatar;
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

    private static final int MAX_TOASTS = 3;

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
        this.portalRepository.setOnSessionExpiredCallback(() -> {
            javafx.application.Platform.runLater(() -> {
                showToastError("Session expired. Please log in again.");
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
        stage.setMinWidth(450);
        stage.setMinHeight(600);
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
        mainLayout.setCenter(main);

        setScene(mainLayout, "Assignly Desktop");
        
        // RESIZING FIX: Constrain the minimum window size strictly to the perfect default 1200x750 "normal" size to prevent any info or text from being hidden
        stage.setMinWidth(1200);
        stage.setMinHeight(750);
        
        // RESIZING FIX: Explicitly check and resize stage boundaries if current dimensions are below the 1200x750 safe limits upon login transition
        if (stage.getWidth() < 1200) {
            stage.setWidth(1200);
        }
        if (stage.getHeight() < 750) {
            stage.setHeight(750);
        }
        
        showToastSuccess("Successfully logged in");
        setupSearch();
        
        // Register Ctrl+K
        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.K) {
                if (searchField != null) {
                    searchField.requestFocus();
                    searchField.selectAll();
                }
                e.consume();
            }
        });
        
        // Start Connectivity Checker
        if (statusScheduler != null && !statusScheduler.isShutdown()) statusScheduler.shutdownNow();
        statusScheduler = Executors.newSingleThreadScheduledExecutor();
        statusScheduler.scheduleAtFixedRate(this::updateStatusIndicator, 0, 60, TimeUnit.SECONDS);

        navigateTo("dashboard");
        Platform.runLater(() -> contentArea.requestFocus());

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
            
            // Pre-download COMSATS logo for PDF exports
            try {
                byte[] logoBytes = portalRepository.fetchPhotoBytes("https://sis.cuiatd.edu.pk/resources/images/CIITLogo_Plain.png");
                if (logoBytes != null && logoBytes.length > 0) {
                    java.nio.file.Files.write(java.nio.file.Paths.get("cui_logo.png"), logoBytes);
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
                            sidebarAvatar.setFitWidth(90);
                            sidebarAvatar.setFitHeight(90);
                            sidebarAvatar.setPreserveRatio(false);
                            sidebarAvatar.setImage(img);
                        }
                    });
                }
            }

            showToast("Background sync complete");
            Platform.runLater(this::refreshSearchItems);
        }).start();
    }

    private void setupSearch() {
        if (searchField == null) return;

        searchResults = new ListView<>();
        searchResults.getStyleClass().add("search-results");
        searchResults.setItems(filteredSearchItems);
        searchResults.setMaxHeight(240);
        searchResults.prefHeightProperty().bind(
            javafx.beans.binding.Bindings.createDoubleBinding(
                () -> Math.min(240.0, filteredSearchItems.size() * 38.0 + 8.0),
                filteredSearchItems
            )
        );
        searchResults.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(SearchItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });

        searchResults.setOnMouseClicked(e -> activateSelectedSearchItem());
        searchResults.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                activateSelectedSearchItem();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideSearchPopup();
                e.consume();
            }
        });

        searchPopup = new Popup();
        searchPopup.setAutoHide(true);
        searchPopup.getContent().add(searchResults);
        searchPopup.setOnHiding(e -> searchResults.getSelectionModel().clearSelection());

        searchField.textProperty().addListener((obs, oldValue, newValue) -> updateSearchResults(newValue));
        searchField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                refreshSearchItems();
            } else {
                Platform.runLater(() -> {
                    if (searchResults != null && !searchResults.isFocused()) {
                        hideSearchPopup();
                    }
                });
            }
        });

        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                if (!filteredSearchItems.isEmpty()) {
                    showSearchPopup();
                    searchResults.getSelectionModel().selectFirst();
                    searchResults.requestFocus();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                if (!filteredSearchItems.isEmpty()) {
                    searchResults.getSelectionModel().selectFirst();
                    activateSelectedSearchItem();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideSearchPopup();
                e.consume();
            }
        });

        searchResults.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && searchField != null && !searchField.isFocused()) {
                hideSearchPopup();
            }
        });

        refreshSearchItems();
    }

    private void refreshSearchItems() {
        List<SearchItem> items = new ArrayList<>();
        items.add(navSearchItem("📊", "Dashboard", "dashboard"));
        items.add(navSearchItem("🔔", "Notifications", "notifications"));
        items.add(navSearchItem("📚", "Courses", "courses"));
        items.add(navSearchItem("🌐", "Course Portal", "portal"));
        items.add(navSearchItem("🎟", "Exam Entry Coupon", "examcoupon"));
        items.add(navSearchItem("📱", "Mobile App Activation", "mobileapp"));
        items.add(navSearchItem("🎓", "Result", "result"));
        items.add(navSearchItem("📅", "Timetable", "timetable"));
        items.add(navSearchItem("💰", "Fee", "fee"));
        items.add(navSearchItem("🏅", "Scholarship", "scholarship"));
        items.add(navSearchItem("📆", "Date Sheet", "datesheet"));
        items.add(navSearchItem("⚙", "Settings", "settings"));

        // Index Courses → Marks & Class Proceedings
        for (String course : getCachedCourseNames()) {
            String labelMarks = "📝  " + course + " → Marks";
            String kwMarks = (course + " courses marks sess qa final").toLowerCase();
            items.add(new SearchItem(labelMarks, kwMarks, () -> navigateToCourseMarks(course)));

            String labelProc = "📅  " + course + " → Class Proceedings (Attendance)";
            String kwProc = (course + " courses class proceedings attendance lecture").toLowerCase();
            items.add(new SearchItem(labelProc, kwProc, () -> navigateToCourseProceedings(course)));
        }

        // Index Dynamic Assignments from Cache
        try {
            for (com.assignly.model.Assignment ass : dataCacheService.getAssignments()) {
                String label = "✍  " + ass.getTitle() + " → Assignment Task";
                String keywords = (ass.getTitle() + " " + ass.getDescription() + " assignment task pending due").toLowerCase();
                items.add(new SearchItem(label, keywords, () -> navigateTo("dashboard")));
            }
        } catch (Exception ignored) {}

        // Index Dynamic Announcements from Cache
        try {
            for (com.assignly.model.Announcement ann : dataCacheService.getAnnouncements()) {
                String label = "📢  " + ann.getTitle() + " → Portal Announcement";
                String keywords = (ann.getTitle() + " " + ann.getContent() + " announcement notice").toLowerCase();
                items.add(new SearchItem(label, keywords, () -> navigateTo("dashboard")));
            }
        } catch (Exception ignored) {}

        // Index Dynamic Notifications from Cache
        try {
            for (DataCacheService.NotificationEntry notif : dataCacheService.getDetailedNotifications()) {
                String label = "🔔  " + notif.title() + " → Notification Alert";
                String keywords = (notif.title() + " " + notif.message() + " notification portal update").toLowerCase();
                items.add(new SearchItem(label, keywords, () -> navigateTo("notifications")));
            }
        } catch (Exception ignored) {}

        // Index Date Sheet Exam Schedules from Cache
        try {
            for (String[] entry : getCachedDateSheetEntries()) {
                String course = entry[0];
                String date = entry[1];
                String time = entry[2];
                String label = "📆  " + course + " Exam → " + date + " @ " + time;
                String keywords = (course + " exam date sheet paper schedule room hall " + date + " " + time).toLowerCase();
                items.add(new SearchItem(label, keywords, () -> navigateTo("datesheet")));
            }
        } catch (Exception ignored) {}

        allSearchItems = items;
    }

    private SearchItem navSearchItem(String icon, String name, String tabId) {
        String label = icon + "  " + name + " → Go to " + name;
        return new SearchItem(label, (name + " tab " + icon).toLowerCase(), () -> navigateTo(tabId));
    }

    private void updateSearchResults(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            filteredSearchItems.clear();
            hideSearchPopup();
            return;
        }

        String q = trimmed.toLowerCase();
        List<SearchItem> matches = new ArrayList<>();
        for (SearchItem item : allSearchItems) {
            if (item.keywords().contains(q)) {
                matches.add(item);
            }
        }
        filteredSearchItems.setAll(matches);

        if (matches.isEmpty()) {
            hideSearchPopup();
        } else {
            showSearchPopup();
        }
    }

    private void showSearchPopup() {
        if (searchPopup == null || searchField == null || stage.getScene() == null) return;
        if (!searchPopup.isShowing()) {
            searchResults.getStylesheets().setAll(stage.getScene().getStylesheets());
            Point2D point = searchField.localToScreen(0, searchField.getHeight() + 4);
            if (point != null) {
                searchResults.setPrefWidth(Math.max(220, searchField.getWidth()));
                searchPopup.show(searchField, point.getX(), point.getY());
            }
        }
    }

    private void hideSearchPopup() {
        if (searchPopup != null) {
            searchPopup.hide();
        }
    }

    private void activateSelectedSearchItem() {
        if (searchResults == null) return;
        SearchItem item = searchResults.getSelectionModel().getSelectedItem();
        if (item == null) return;
        hideSearchPopup();
        searchField.clear();
        item.action().run();
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

        searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.getStyleClass().add("search-bar");
        searchField.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(searchField, new Insets(0, 6, 12, 6));

        Button dashboard = navBtn("Dashboard", "dashboard");
        Button notifications = navBtn("Notifications", "notifications");
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

        sidebarAvatar = new ImageView();
        sidebarAvatar.setFitWidth(90);
        sidebarAvatar.setFitHeight(90);
        sidebarAvatar.setPreserveRatio(false);
        sidebarAvatar.setSmooth(true);
        Circle clip = new Circle();
        clip.radiusProperty().bind(sidebarAvatar.fitWidthProperty().divide(2));
        clip.centerXProperty().bind(sidebarAvatar.fitWidthProperty().divide(2));
        clip.centerYProperty().bind(sidebarAvatar.fitHeightProperty().divide(2));
        sidebarAvatar.setClip(clip);

        Label avatarPlaceholder = new Label("👤");
        avatarPlaceholder.visibleProperty().bind(sidebarAvatar.imageProperty().isNull());

        StackPane avatarBox = new StackPane(sidebarAvatar, avatarPlaceholder);
        avatarBox.getStyleClass().add("sidebar-avatar");
        avatarBox.setPrefSize(90, 90);
        avatarBox.setMaxSize(90, 90);
        VBox.setMargin(avatarBox, new Insets(0, 0, 8, 0));

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

        sb.getChildren().addAll(brandBox, searchField, dashboard, notifications, courses, portal, examCoupon, mobileApp,
                result, timetable, fee, scholarship, dateSheet, new Separator(), settings, spacer, avatarBox, userLabel, logout);
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
            case "notifications" -> new NotificationsTabView(this).getRoot();
            case "courses" -> {
                String tab = pendingCoursesTab;
                String query = pendingCourseQuery;
                pendingCoursesTab = null;
                pendingCourseQuery = null;
                yield (tab != null || query != null)
                    ? new CoursesTabView(this, tab, query).getRoot()
                    : new CoursesTabView(this).getRoot();
            }
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


    // ---------- Scene ----------
    public void setScene(Parent contentRoot, String title) {
        contentRoot.getStyleClass().add("app-root");

        StackPane root = new StackPane(contentRoot);
        initToastLayer(root);

        Scene scene = new Scene(root, 1200, 750);
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

    private enum ToastType {
        INFO,
        SUCCESS,
        ERROR
    }

    private void initToastLayer(StackPane root) {
        toastContainer = new VBox(8);
        toastContainer.setFillWidth(false);
        toastContainer.getStyleClass().add("toast-container");
        toastContainer.setPickOnBounds(false); // Do not consume mouse clicks

        StackPane.setAlignment(toastContainer, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toastContainer, new Insets(0, 18, 18, 0));
        root.getChildren().add(toastContainer);
    }

    public void showToast(String message) {
        showToast(message, ToastType.INFO);
    }

    public void showToastSuccess(String message) {
        showToast(message, ToastType.SUCCESS);
    }

    public void showToastError(String message) {
        showToast(message, ToastType.ERROR);
    }

    private void showToast(String message, ToastType type) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Toast message cannot be blank.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Toast type cannot be null.");
        }

        Runnable showAction = () -> {
            if (toastContainer == null) {
                System.err.println("Toast container is not initialized.");
                return;
            }
            Node toast = buildToastNode(message.trim(), type);
            toastContainer.getChildren().add(toast);

            if (toastContainer.getChildren().size() > MAX_TOASTS) {
                toastContainer.getChildren().remove(0);
            }

            playToastIn(toast);
            scheduleToastDismiss(toast);
        };

        if (Platform.isFxApplicationThread()) {
            showAction.run();
        } else {
            Platform.runLater(showAction);
        }
    }

    private Node buildToastNode(String message, ToastType type) {
        HBox toast = new HBox(10);
        toast.getStyleClass().addAll("toast", "toast-" + type.name().toLowerCase());

        Label icon = new Label(switch (type) {
            case SUCCESS -> "✓";
            case ERROR -> "!";
            default -> "i";
        });
        icon.getStyleClass().add("toast-icon");

        Label text = new Label(message);
        text.getStyleClass().add("toast-text");
        text.setWrapText(true);
        text.setMaxWidth(280);

        toast.getChildren().addAll(icon, text);
        toast.setOpacity(0);
        toast.setTranslateX(40);
        toast.setOnMouseClicked(e -> dismissToast(toast));
        return toast;
    }

    private void playToastIn(Node toast) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(220), toast);
        slideIn.setFromX(40);
        slideIn.setToX(0);

        new ParallelTransition(fadeIn, slideIn).play();
    }

    private void scheduleToastDismiss(Node toast) {
        PauseTransition delay = new PauseTransition(Duration.seconds(3.5));
        delay.setOnFinished(e -> dismissToast(toast));
        delay.play();
    }

    private void dismissToast(Node toast) {
        if (toastContainer == null || toast == null) {
            return;
        }
        if (!toastContainer.getChildren().contains(toast)) {
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), toast);
        fadeOut.setFromValue(toast.getOpacity());
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(200), toast);
        slideOut.setToX(40);

        ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);
        exit.setOnFinished(e -> toastContainer.getChildren().remove(toast));
        exit.play();
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

    private record SearchItem(String label, String keywords, Runnable action) {}
}
