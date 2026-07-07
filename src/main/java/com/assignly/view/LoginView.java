package com.assignly.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.net.URL;

public class LoginView {
    private final StackPane root = new StackPane();
    private final TextField registrationField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final TextField passwordVisibleField = new TextField();
    private final CheckBox rememberMeCheck = new CheckBox("Remember Me");
    private final Button loginButton = new Button("Sign In");
    private final Label errorLabel = new Label();

    private static final String INPUT_STYLE =
        "-fx-background-color: #181d24;" +
        "-fx-text-fill: #F5F7FA;" +
        "-fx-prompt-text-fill: #4B5563;" +
        "-fx-border-color: #2c323f;" +
        "-fx-border-width: 1.2;" +
        "-fx-border-radius: 8;" +
        "-fx-background-radius: 8;" +
        "-fx-padding: 10 14;" +
        "-fx-font-size: 13px;";

    private static final String INPUT_FOCUS_STYLE =
        "-fx-background-color: #181d24;" +
        "-fx-text-fill: #F5F7FA;" +
        "-fx-prompt-text-fill: #4B5563;" +
        "-fx-border-color: #14b8a6;" +
        "-fx-border-width: 1.2;" +
        "-fx-border-radius: 8;" +
        "-fx-background-radius: 8;" +
        "-fx-padding: 10 14;" +
        "-fx-font-size: 13px;" +
        "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.15), 6, 0, 0, 0);";

    private static final String PASSWORD_INPUT_STYLE = INPUT_STYLE + " -fx-padding: 10 42 10 14;";
    private static final String PASSWORD_INPUT_FOCUS_STYLE = INPUT_FOCUS_STYLE + " -fx-padding: 10 42 10 14;";

    public LoginView() {
        root.setStyle("-fx-background-color: #0b0f13;");

        HBox mainLayout = new HBox(0);
        mainLayout.setAlignment(Pos.CENTER);

        VBox leftPanel = buildLeftPanel();
        VBox rightPanel = buildRightPanel();

        mainLayout.getChildren().addAll(leftPanel, rightPanel);
        root.getChildren().add(mainLayout);
    }

    // ================================================================
    // LEFT PANEL (60% split - Branding & Features)
    // ================================================================
    private VBox buildLeftPanel() {
        StackPane container = new StackPane();
        container.setPrefWidth(600);
        container.setMinWidth(600);
        container.setMaxWidth(600);
        container.setStyle("-fx-background-color: linear-gradient(to bottom right, #001f1d, #004643, #022b29);");

        // Subtle ambient glowing highlights
        Circle glow1 = new Circle(220, Color.web("#14b8a6", 0.04));
        glow1.setMouseTransparent(true);
        StackPane.setAlignment(glow1, Pos.TOP_LEFT);
        glow1.setTranslateX(-60);
        glow1.setTranslateY(-60);
        glow1.setEffect(new javafx.scene.effect.BoxBlur(120, 120, 3));

        Circle glow2 = new Circle(180, Color.web("#0d9488", 0.03));
        glow2.setMouseTransparent(true);
        StackPane.setAlignment(glow2, Pos.BOTTOM_RIGHT);
        glow2.setTranslateX(60);
        glow2.setTranslateY(60);
        glow2.setEffect(new javafx.scene.effect.BoxBlur(100, 100, 3));

        // Glassmorphic panel holding main content
        VBox glassPanel = new VBox(20);
        glassPanel.setAlignment(Pos.CENTER_LEFT);
        glassPanel.setPadding(new Insets(32, 40, 32, 40));
        glassPanel.setMaxWidth(480);
        glassPanel.setPrefWidth(480);
        glassPanel.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.02);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(255, 255, 255, 0.07);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 20;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.15), 20, 0, 0, 6);"
        );

        // Logo Container
        StackPane logoContainer = new StackPane();
        logoContainer.setAlignment(Pos.CENTER);
        logoContainer.setPrefSize(72, 72);
        logoContainer.setMinSize(72, 72);
        logoContainer.setMaxSize(72, 72);
        logoContainer.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.03);" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(255, 255, 255, 0.12);" +
            "-fx-border-width: 1.2;" +
            "-fx-border-radius: 18;"
        );

        ImageView logoView = new ImageView();
        URL logoUrl = getClass().getResource("/com/assignly/images/assignly_logo.png");
        if (logoUrl != null) {
            Image logoImg = new Image(logoUrl.toExternalForm(), 48, 48, true, true);
            logoView.setImage(logoImg);
            logoView.setFitWidth(48);
            logoView.setFitHeight(48);
            logoView.setSmooth(true);
            Rectangle clip = new Rectangle(48, 48);
            clip.setArcWidth(12);
            clip.setArcHeight(12);
            logoView.setClip(clip);
        }
        logoContainer.getChildren().add(logoView);

        // Branding titles
        Label title = new Label("Assignly");
        title.setStyle(
            "-fx-font-size: 32px;" +
            "-fx-font-weight: 900;" +
            "-fx-text-fill: #F5F7FA;" +
            "-fx-letter-spacing: 0.5;"
        );

        Label subtitle = new Label("ACADEMIC PORTAL CLIENT");
        subtitle.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: #14b8a6;" +
            "-fx-letter-spacing: 1.5;"
        );

        VBox titleBox = new VBox(2, title, subtitle);

        // Divider
        Region divider = new Region();
        divider.setPrefHeight(1.2);
        divider.setMaxWidth(380);
        divider.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08);");

        // Description
        Label desc = new Label("Access your academic journey through one unified desktop portal.");
        desc.setWrapText(true);
        desc.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #9CA3AF;" +
            "-fx-line-spacing: 1.4;"
        );

        // Features rows
        VBox featuresList = new VBox(12);
        featuresList.getChildren().addAll(
            createFeatureRow("📊", "Results & GPA", "Grades tracker, credit calculator, and PDF report cards"),
            createFeatureRow("⏰", "Attendance Tracking", "Real-time class attendance percentages and alerts"),
            createFeatureRow("📅", "Timetable Management", "Daily class countdown schedules and weekly calendar grids"),
            createFeatureRow("📚", "Course Materials", "Centralized list for pending assignments and marks lists"),
            createFeatureRow("💳", "Fee Management", "Review tuition installments and export official billing slips"),
            createFeatureRow("📝", "Examination Services", "Verify date sheet details, checks list, and exam entry coupons")
        );

        glassPanel.getChildren().addAll(logoContainer, titleBox, divider, desc, featuresList);

        // Disclaimer Footer
        VBox footer = new VBox(2);
        footer.setAlignment(Pos.CENTER);
        footer.setMaxWidth(480);
        Label verLabel = new Label("v1.1.0");
        verLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255, 255, 255, 0.25); -fx-font-weight: bold;");
        Label discLabel = new Label("Unofficial Client  •  Not Affiliated With COMSATS");
        discLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: rgba(255, 255, 255, 0.2);");
        footer.getChildren().addAll(verLabel, discLabel);

        VBox contentBox = new VBox(20, glassPanel, footer);
        contentBox.setAlignment(Pos.CENTER);

        container.getChildren().addAll(glow1, glow2, contentBox);

        VBox panel = new VBox(container);
        panel.setAlignment(Pos.CENTER);
        VBox.setVgrow(container, Priority.ALWAYS);
        return panel;
    }

    private HBox createFeatureRow(String icon, String title, String subtitle) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(28, 28);
        iconPane.setMinSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setStyle(
            "-fx-background-color: rgba(20, 184, 166, 0.08);" +
            "-fx-background-radius: 50%;" +
            "-fx-border-color: rgba(20, 184, 166, 0.15);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 50%;"
        );
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #14b8a6;");
        iconPane.getChildren().add(iconLabel);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #E5E7EB;");

        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        VBox textGroup = new VBox(0, titleLabel, subLabel);
        row.getChildren().addAll(iconPane, textGroup);
        return row;
    }

    // ================================================================
    // RIGHT PANEL (40% split - Floating Auth Card)
    // ================================================================
    private VBox buildRightPanel() {
        StackPane container = new StackPane();
        container.setPrefWidth(400);
        container.setMinWidth(400);
        container.setMaxWidth(400);
        container.setStyle("-fx-background-color: #0b0f13;");

        // Subtle backing glow behind the card
        Circle glow = new Circle(140, Color.web("#14b8a6", 0.02));
        glow.setMouseTransparent(true);
        glow.setEffect(new javafx.scene.effect.BoxBlur(80, 80, 3));
        container.getChildren().add(glow);

        // Floating authentication card
        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(32, 28, 32, 28));
        card.setMaxWidth(330);
        card.setPrefWidth(330);
        card.setStyle(
            "-fx-background-color: #14181c;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: #282e38;" +
            "-fx-border-width: 1.2;" +
            "-fx-border-radius: 16;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.45), 24, 0, 0, 10);"
        );

        // Header
        Label welcomeTitle = new Label("Welcome Back");
        welcomeTitle.setStyle(
            "-fx-font-size: 22px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: #F5F7FA;"
        );
        Label welcomeSub = new Label("Enter your credentials to access your portal.");
        welcomeSub.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #9CA3AF;"
        );
        welcomeSub.setWrapText(true);
        VBox headerBox = new VBox(4, welcomeTitle, welcomeSub);

        // Registration Input
        Label regLabel = new Label("REGISTRATION NUMBER");
        regLabel.setStyle(
            "-fx-font-size: 9px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: #9CA3AF;" +
            "-fx-letter-spacing: 0.8;"
        );
        registrationField.setPromptText("SP25-BCS-001");
        registrationField.setPrefHeight(40);
        registrationField.setStyle(INPUT_STYLE);
        addFocusStyle(registrationField);
        VBox regGroup = new VBox(6, regLabel, registrationField);

        // Password Input
        Label passLabel = new Label("PASSWORD");
        passLabel.setStyle(
            "-fx-font-size: 9px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: #9CA3AF;" +
            "-fx-letter-spacing: 0.8;"
        );
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefHeight(40);
        passwordField.setStyle(PASSWORD_INPUT_STYLE);
        addFocusStyle(passwordField);

        passwordVisibleField.setPromptText("Enter your password");
        passwordVisibleField.setPrefHeight(40);
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);
        passwordVisibleField.setStyle(PASSWORD_INPUT_STYLE);
        addFocusStyle(passwordVisibleField);
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        Button toggleBtn = new Button("👁");
        toggleBtn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #4B5563;" +
            "-fx-font-size: 12px;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 0 12 0 0;" +
            "-fx-max-width: 32;"
        );
        toggleBtn.setOnMouseEntered(e -> toggleBtn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #14b8a6;" +
            "-fx-font-size: 12px;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 0 12 0 0;" +
            "-fx-max-width: 32;"
        ));
        toggleBtn.setOnMouseExited(e -> toggleBtn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #4B5563;" +
            "-fx-font-size: 12px;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 0 12 0 0;" +
            "-fx-max-width: 32;"
        ));
        toggleBtn.setOnAction(e -> {
            boolean showing = passwordVisibleField.isVisible();
            passwordField.setVisible(showing);
            passwordField.setManaged(showing);
            passwordVisibleField.setVisible(!showing);
            passwordVisibleField.setManaged(!showing);
            toggleBtn.setText(showing ? "👁" : "🙈");
        });

        StackPane passStack = new StackPane(passwordField, passwordVisibleField);
        StackPane.setAlignment(toggleBtn, Pos.CENTER_RIGHT);
        StackPane passContainer = new StackPane(passStack, toggleBtn);
        VBox passGroup = new VBox(6, passLabel, passContainer);

        // Remember Me
        rememberMeCheck.setSelected(true);
        rememberMeCheck.setStyle(
            "-fx-text-fill: #9CA3AF;" +
            "-fx-font-size: 12px;" +
            "-fx-cursor: hand;"
        );

        // Error label
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setStyle(
            "-fx-text-fill: #fca5a5;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: 600;" +
            "-fx-padding: 10 14;" +
            "-fx-background-color: rgba(239, 68, 68, 0.08);" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: rgba(239, 68, 68, 0.2);" +
            "-fx-border-radius: 8;" +
            "-fx-border-width: 1;"
        );
        errorLabel.visibleProperty().addListener((obs, o, n) -> errorLabel.setManaged(n));

        // Sign In button
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(42);
        String btnNormal =
            "-fx-background-color: #14b8a6;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: 700;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;";
        loginButton.setStyle(btnNormal);
        loginButton.setOnMouseEntered(e -> {
            if (!loginButton.isDisabled()) {
                loginButton.setStyle(
                    "-fx-background-color: #0d9488;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: 700;" +
                    "-fx-font-size: 13px;" +
                    "-fx-background-radius: 8;" +
                    "-fx-cursor: hand;" +
                    "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.2), 8, 0, 0, 2);"
                );
            }
        });
        loginButton.setOnMouseExited(e -> {
            if (!loginButton.isDisabled()) {
                loginButton.setStyle(btnNormal);
            }
        });

        // Trigger login on Enter key press
        registrationField.setOnAction(e -> loginButton.fire());
        passwordField.setOnAction(e -> loginButton.fire());
        passwordVisibleField.setOnAction(e -> loginButton.fire());

        card.getChildren().addAll(
            headerBox,
            regGroup,
            passGroup,
            rememberMeCheck,
            errorLabel,
            loginButton
        );

        container.getChildren().add(card);

        VBox panel = new VBox(container);
        panel.setAlignment(Pos.CENTER);
        VBox.setVgrow(container, Priority.ALWAYS);
        return panel;
    }

    private void addFocusStyle(TextField field) {
        field.focusedProperty().addListener((obs, old, focused) -> {
            boolean isPassword = field == passwordField || field == passwordVisibleField;
            if (focused) {
                field.setStyle(isPassword ? PASSWORD_INPUT_FOCUS_STYLE : INPUT_FOCUS_STYLE);
            } else {
                field.setStyle(isPassword ? PASSWORD_INPUT_STYLE : INPUT_STYLE);
            }
        });
    }

    // ================================================================
    // PUBLIC API
    // ================================================================
    public StackPane getRoot() { return root; }
    public TextField getRegistrationField() { return registrationField; }
    public PasswordField getPasswordField() { return passwordField; }
    public CheckBox getRememberMeCheck() { return rememberMeCheck; }
    public Button getLoginButton() { return loginButton; }
    public Label getErrorLabel() { return errorLabel; }
}
