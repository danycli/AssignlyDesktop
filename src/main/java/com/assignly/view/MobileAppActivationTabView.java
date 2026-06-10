package com.assignly.view;

import com.assignly.util.AppContext;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Mobile App Activation tab view – redesigned to match the modern dark theme of Assignly.
 * Guides the user through app setup using badges, steps timeline, a dynamically resolved
 * activation status layout, and dynamic Play Store download links.
 */
public class MobileAppActivationTabView {
    private final VBox root = new VBox(20);
    private final AppContext context;
    
    // UI Elements
    private Button submitBtn;
    private Button playStoreLinkBtn;
    private ProgressBar progressBar;
    private Label statusLbl;
    private final VBox stepsContainer = new VBox();
    private final List<VBox> stepCards = new ArrayList<>();
    private boolean isLayoutNarrow = false;
    
    private final java.util.function.Consumer<Boolean> connectivityListener = this::onConnectivityChanged;

    public MobileAppActivationTabView(AppContext context) {
        this.context = context;
        buildUI();
        context.addConnectivityListener(connectivityListener);
        fetchPlayStoreLink();
    }

    private void buildUI() {
        root.setFillWidth(true);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: -color-bg-main;");

        // 1. Premium Sleek Hero Card
        VBox heroCard = new VBox(8);
        heroCard.setPadding(new Insets(16, 20, 16, 20));
        heroCard.setStyle("-fx-background-color: -color-bg-card;"
                + "-fx-background-radius: 12;-fx-border-color: -color-border;-fx-border-width: 1;-fx-border-radius: 12;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 8, 0, 0, 3);");
        
        Label heroTitle = new Label("Mobile App Activation");
        heroTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
        
        Label heroDesc = new Label("Connect your student account with the official COMSATS mobile application for quick access to academic information.");
        heroDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500;");
        heroDesc.setWrapText(true);

        FlowPane badgesFlow = new FlowPane(8, 6);
        badgesFlow.setPadding(new Insets(4, 0, 0, 0));
        badgesFlow.getChildren().addAll(
            createFeatureBadge("Attendance Tracking"),
            createFeatureBadge("Results Access"),
            createFeatureBadge("Timetable Viewing"),
            createFeatureBadge("Notifications"),
            createFeatureBadge("Course Updates")
        );

        heroCard.getChildren().addAll(heroTitle, heroDesc, badgesFlow);
        root.getChildren().add(heroCard);

        // 2. Main Content Split
        HBox bodySplit = new HBox(20);
        VBox.setVgrow(bodySplit, Priority.ALWAYS);

        // Left Column (Activation Input & Notice)
        VBox leftCol = new VBox(15);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        VBox activationCard = new VBox(15);
        activationCard.setPadding(new Insets(20));
        activationCard.setStyle("-fx-background-color: -color-bg-card;-fx-background-radius: 12;"
                + "-fx-border-color: -color-border;-fx-border-width: 1;-fx-border-radius: 12;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 2);");

        Label cardTitle = new Label("Portal Verification");
        cardTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");

        VBox passGroup = new VBox(8);
        Label passLbl = new Label("Current Portal Password");
        passLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted; -fx-font-weight: bold;");

        StackPane passwordStack = new StackPane();
        passwordStack.setAlignment(Pos.CENTER_LEFT);

        PasswordField pf = new PasswordField();
        pf.setPromptText("Enter portal password");
        
        TextField tf = new TextField();
        tf.setPromptText("Enter portal password");
        tf.setManaged(false);
        tf.setVisible(false);

        String fieldStyleNormal = "-fx-background-color: -color-bg-elevated; -fx-text-fill: -color-text-main; -fx-prompt-text-fill: -color-text-muted-extra; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 36 10 12; -fx-font-size: 12px;";
        String fieldStyleFocused = "-fx-background-color: -color-bg-elevated; -fx-text-fill: -color-text-main; -fx-prompt-text-fill: -color-text-muted-extra; -fx-border-color: -color-accent; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 36 10 12; -fx-font-size: 12px; -fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.1), 6, 0, 0, 0);";

        pf.setStyle(fieldStyleNormal);
        tf.setStyle(fieldStyleNormal);

        pf.focusedProperty().addListener((obs, oldVal, newVal) -> {
            pf.setStyle(newVal ? fieldStyleFocused : fieldStyleNormal);
        });
        tf.focusedProperty().addListener((obs, oldVal, newVal) -> {
            tf.setStyle(newVal ? fieldStyleFocused : fieldStyleNormal);
        });

        Button toggleBtn = new Button("👁");
        toggleBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-text-muted; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 0;");
        StackPane.setAlignment(toggleBtn, Pos.CENTER_RIGHT);
        StackPane.setMargin(toggleBtn, new Insets(0, 12, 0, 0));

        toggleBtn.setOnAction(e -> {
            if (pf.isVisible()) {
                tf.setText(pf.getText());
                pf.setVisible(false);
                pf.setManaged(false);
                tf.setVisible(true);
                tf.setManaged(true);
                toggleBtn.setText("🙈");
            } else {
                pf.setText(tf.getText());
                tf.setVisible(false);
                tf.setManaged(false);
                pf.setVisible(true);
                pf.setManaged(true);
                toggleBtn.setText("👁");
            }
        });
        
        toggleBtn.setOnMouseEntered(e -> toggleBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-accent; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 0;"));
        toggleBtn.setOnMouseExited(e -> toggleBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-text-muted; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 0;"));

        passwordStack.getChildren().addAll(pf, tf, toggleBtn);
        passGroup.getChildren().addAll(passLbl, passwordStack);

        submitBtn = new Button("Activate Mobile Access");
        submitBtn.setCursor(javafx.scene.Cursor.HAND);
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        
        String activeBtnStyle = "-fx-background-color: -color-accent; -fx-text-fill: white;"
                + "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;";
        String hoverBtnStyle = "-fx-background-color: #0d9488; -fx-text-fill: white;"
                + "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.25), 8, 0, 0, 2);";
        
        submitBtn.setStyle(activeBtnStyle);
        submitBtn.setOnMouseEntered(e -> {
            if (!submitBtn.isDisable()) submitBtn.setStyle(hoverBtnStyle);
        });
        submitBtn.setOnMouseExited(e -> {
            if (!submitBtn.isDisable()) submitBtn.setStyle(activeBtnStyle);
        });

        progressBar = new ProgressBar();
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setStyle("-fx-accent: -color-accent;");

        statusLbl = new Label("");
        statusLbl.setWrapText(true);
        statusLbl.setVisible(false);
        statusLbl.setManaged(false);

        activationCard.getChildren().addAll(cardTitle, passGroup, submitBtn, progressBar, statusLbl);
        
        activationCard.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), activationCard);
            tt.setToY(-2);
            tt.play();
            activationCard.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12;"
                    + "-fx-border-color: -color-accent; -fx-border-width: 1; -fx-border-radius: 12;"
                    + "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.05), 10, 0, 0, 3);");
        });
        activationCard.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), activationCard);
            tt.setToY(0);
            tt.play();
            activationCard.setStyle("-fx-background-color: -color-bg-card; -fx-background-radius: 12;"
                    + "-fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 12;"
                    + "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.02), 8, 0, 0, 2);");
        });

        HBox securityNotice = new HBox(8);
        securityNotice.setAlignment(Pos.CENTER_LEFT);
        securityNotice.setPadding(new Insets(12));
        securityNotice.setStyle("-fx-background-color: rgba(20, 184, 166, 0.03); -fx-border-color: rgba(20, 184, 166, 0.12);"
                + "-fx-border-width: 1;-fx-border-radius: 8;-fx-background-radius: 8;");
        
        Label lockIcon = new Label("🔒");
        lockIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-accent;");
        Label noticeText = new Label("Your password is verified securely through the university portal and is not stored locally.");
        noticeText.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500;");
        noticeText.setWrapText(true);
        HBox.setHgrow(noticeText, Priority.ALWAYS);
        securityNotice.getChildren().addAll(lockIcon, noticeText);

        leftCol.getChildren().addAll(activationCard, securityNotice);

        // Right Column (Status & Download Card)
        VBox rightCol = new VBox(20);
        rightCol.setPrefWidth(320);
        rightCol.setMinWidth(280);
        rightCol.setMaxWidth(350);

        // Download App Card

        // Download App Card
        VBox downloadCard = new VBox(12);
        downloadCard.setPadding(new Insets(16));
        downloadCard.setStyle("-fx-background-color: -color-bg-card;-fx-border-color: -color-border;"
                + "-fx-border-width: 1;-fx-border-radius: 12;-fx-background-radius: 12;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 2);");

        Label appName = new Label("CUOnline CUI Abbottabad");
        appName.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        Label appSub = new Label("Official Student Mobile Application");
        appSub.setStyle("-fx-font-size: 10px; -fx-text-fill: -color-text-muted; -fx-font-weight: 500;");

        HBox appHeader = new HBox(12);
        appHeader.setAlignment(Pos.CENTER_LEFT);
        Label phoneIcon = new Label("📱");
        phoneIcon.setStyle("-fx-font-size: 24px;");
        VBox titleBox = new VBox(2, appName, appSub);
        appHeader.getChildren().addAll(phoneIcon, titleBox);

        HBox downloadActions = new HBox(8);
        downloadActions.setAlignment(Pos.CENTER_LEFT);

        Button downloadBtn = new Button("Download APK");
        downloadBtn.setCursor(javafx.scene.Cursor.HAND);
        
        String dlBtnStyle = "-fx-background-color: -color-accent; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8 14; -fx-background-radius: 6; -fx-cursor: hand;";
        String dlBtnHover = "-fx-background-color: #0d9488; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8 14; -fx-background-radius: 6; -fx-cursor: hand;";
        
        downloadBtn.setStyle(dlBtnStyle);
        downloadBtn.setOnMouseEntered(e -> downloadBtn.setStyle(dlBtnHover));
        downloadBtn.setOnMouseExited(e -> downloadBtn.setStyle(dlBtnStyle));
        downloadBtn.setOnAction(e -> launchUrl("https://play.google.com/store/apps/details?id=edupk.cuiatd.cuonlinestudentportal"));

        playStoreLinkBtn = new Button("Open Play Store");
        playStoreLinkBtn.setCursor(javafx.scene.Cursor.HAND);
        
        String playStyle = "-fx-background-color: -color-bg-elevated; -fx-text-fill: -color-text-main; -fx-border-color: -color-border; -fx-border-width: 1; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 7 13; -fx-background-radius: 6; -fx-border-radius: 6; -fx-cursor: hand;";
        String playHover = "-fx-background-color: -color-bg-card; -fx-text-fill: -color-accent; -fx-border-color: -color-accent; -fx-border-width: 1; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 7 13; -fx-background-radius: 6; -fx-border-radius: 6; -fx-cursor: hand;";
        
        playStoreLinkBtn.setStyle(playStyle);
        playStoreLinkBtn.setOnMouseEntered(e -> playStoreLinkBtn.setStyle(playHover));
        playStoreLinkBtn.setOnMouseExited(e -> playStoreLinkBtn.setStyle(playStyle));
        playStoreLinkBtn.setOnAction(e -> launchUrl("https://play.google.com/store/apps/details?id=edupk.cuiatd.cuonlinestudentportal"));

        downloadActions.getChildren().addAll(downloadBtn, playStoreLinkBtn);
        
        Separator cardSep = new Separator();
        cardSep.setStyle("-fx-background-color: -color-border; -fx-opacity: 0.25;");
        
        downloadCard.getChildren().addAll(appHeader, cardSep, downloadActions);
        
        downloadCard.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), downloadCard);
            tt.setToY(-2);
            tt.play();
            downloadCard.setStyle("-fx-background-color: -color-bg-card; -fx-border-color: -color-accent;"
                    + "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;"
                    + "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.05), 10, 0, 0, 3);");
        });
        downloadCard.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), downloadCard);
            tt.setToY(0);
            tt.play();
            downloadCard.setStyle("-fx-background-color: -color-bg-card; -fx-border-color: -color-border;"
                    + "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;"
                    + "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.02), 8, 0, 0, 2);");
        });

        rightCol.getChildren().addAll(downloadCard);
        bodySplit.getChildren().addAll(leftCol, rightCol);
        root.getChildren().add(bodySplit);

        // 3. Visual Timeline Steps Container
        VBox stepsTimeline = new VBox(12);
        stepsTimeline.setPadding(new Insets(16));
        stepsTimeline.setStyle("-fx-background-color: -color-bg-card;-fx-border-color: -color-border;"
                + "-fx-border-width: 1;-fx-border-radius: 12;-fx-background-radius: 12;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 2);");

        Label timelineTitle = new Label("Activation & Setup Steps");
        timelineTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted; -fx-letter-spacing: 0.5px;");
        stepsTimeline.getChildren().add(timelineTitle);

        stepsTimeline.getChildren().add(stepsContainer);
        root.getChildren().add(stepsTimeline);

        // Populate steps
        rebuildStepCards();

        // Submit Button Action
        submitBtn.setOnAction(e -> {
            clearStatus(statusLbl);
            if (!context.isOnline()) {
                setStatusError(statusLbl, "Cannot activate app in offline mode.");
                return;
            }
            String enteredPass = pf.isVisible() ? pf.getText() : tf.getText();
            String actualPass = context.getSessionPassword() != null ? context.getSessionPassword() : "";
            
            if (enteredPass.isEmpty()) {
                setStatusError(statusLbl, "Please enter your password.");
                return;
            }
            
            if (!enteredPass.equals(actualPass)) {
                setStatusError(statusLbl, "Your current password is incorrect.");
                return;
            }

            submitBtn.setDisable(true);
            submitBtn.setText("Activating Access...");
            submitBtn.setStyle("-fx-background-color: -color-border; -fx-text-fill: -color-text-muted; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8;");
            progressBar.setVisible(true);
            
            new Thread(() -> {
                String msg = context.portalRepository().generateAppPassword(enteredPass);
                Platform.runLater(() -> {
                    submitBtn.setDisable(false);
                    submitBtn.setText("Activate Mobile Access");
                    submitBtn.setStyle(activeBtnStyle);
                    progressBar.setVisible(false);
                    
                    if (msg.toLowerCase().contains("success") || msg.toLowerCase().contains("activated") 
                            || msg.contains("completed") || msg.contains("no explicit message")) {
                        
                        String successMsg = (msg.contains("completed") || msg.contains("no explicit message")) 
                                ? "Student App is Activated. Use Same Password for Student Portal and App login." 
                                : msg;
                        setStatusSuccess(statusLbl, successMsg);
                    } else {
                        setStatusError(statusLbl, msg);
                    }
                });
            }).start();
        });

        // Responsive width triggers
        bodySplit.widthProperty().addListener((obs, oldVal, newVal) -> {
            boolean isNarrow = newVal.doubleValue() < 750;
            boolean isSideBySide = bodySplit.getChildren().contains(rightCol);
            
            if (isNarrow && isSideBySide) {
                Platform.runLater(() -> {
                    if (bodySplit.getChildren().contains(rightCol)) {
                        bodySplit.getChildren().remove(rightCol);
                        if (!root.getChildren().contains(rightCol)) {
                            // Insert rightCol right after bodySplit in the root VBox layout
                            int index = root.getChildren().indexOf(bodySplit);
                            root.getChildren().add(index + 1, rightCol);
                        }
                        rightCol.setMaxWidth(Double.MAX_VALUE);
                        updateStepsLayout(true);
                    }
                });
            } else if (!isNarrow && !isSideBySide) {
                Platform.runLater(() -> {
                    if (root.getChildren().contains(rightCol)) {
                        root.getChildren().remove(rightCol);
                        if (!bodySplit.getChildren().contains(rightCol)) {
                            bodySplit.getChildren().add(rightCol);
                        }
                        rightCol.setMaxWidth(350);
                        updateStepsLayout(false);
                    }
                });
            }
        });
    }

    private HBox createFeatureBadge(String text) {
        HBox badge = new HBox(6);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setPadding(new Insets(3, 8, 3, 8));
        badge.setStyle("-fx-background-color: rgba(20, 184, 166, 0.04);"
                + "-fx-border-color: rgba(20, 184, 166, 0.15);-fx-border-width: 1;"
                + "-fx-border-radius: 10;-fx-background-radius: 10;");
        
        Label check = new Label("✓");
        check.setStyle("-fx-text-fill: -color-accent; -fx-font-weight: bold; -fx-font-size: 10px;");
        
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: -color-text-muted; -fx-font-weight: 600; -fx-font-size: 9.5px;");
        
        badge.getChildren().addAll(check, lbl);
        return badge;
    }

    private VBox createStepCardStyle(String stepNum, String title, String emoji, String state) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        
        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        
        Label step = new Label(stepNum.toUpperCase());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 14px;");
        
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        lbl.setWrapText(true);
        
        top.getChildren().addAll(step, spacer, icon);
        card.getChildren().addAll(top, lbl);
        
        if ("completed".equals(state)) {
            card.setStyle("-fx-background-color: rgba(16, 185, 129, 0.03); -fx-border-color: rgba(16, 185, 129, 0.25); -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
            step.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: #10B981; -fx-letter-spacing: 0.5px;");
            lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
            step.setText("✓ " + stepNum.toUpperCase());
        } else if ("active_highlight".equals(state)) {
            card.setStyle("-fx-background-color: -color-bg-elevated; -fx-border-color: -color-accent; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8;"
                    + "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.15), 10, 0, 0, 3);");
            step.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: -color-accent; -fx-letter-spacing: 0.5px;");
            lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: -color-text-main;");
            step.setText("▶ " + stepNum.toUpperCase());
        } else if ("active".equals(state)) {
            card.setStyle("-fx-background-color: -color-bg-elevated; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
            step.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: -color-accent; -fx-letter-spacing: 0.5px;");
            lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-main;");
        } else { // muted
            card.setStyle("-fx-background-color: -color-bg-card; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-opacity: 0.55;");
            step.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: -color-text-muted; -fx-letter-spacing: 0.5px;");
            lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: -color-text-muted;");
        }

        card.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
            tt.setToY(-2);
            tt.play();
            if ("active_highlight".equals(state)) {
                card.setStyle("-fx-background-color: -color-bg-elevated; -fx-border-color: -color-accent; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8;"
                        + "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.25), 12, 0, 0, 4);");
            } else if ("completed".equals(state)) {
                card.setStyle("-fx-background-color: rgba(16, 185, 129, 0.05); -fx-border-color: rgba(16, 185, 129, 0.35); -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;"
                        + "-fx-effect: dropshadow(three-pass-box, rgba(16, 185, 129, 0.08), 8, 0, 0, 2);");
            } else if ("active".equals(state)) {
                card.setStyle("-fx-background-color: -color-bg-elevated; -fx-border-color: -color-accent; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;"
                        + "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.08), 8, 0, 0, 2);");
            }
        });
        card.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(150), card);
            tt.setToY(0);
            tt.play();
            if ("completed".equals(state)) {
                card.setStyle("-fx-background-color: rgba(16, 185, 129, 0.03); -fx-border-color: rgba(16, 185, 129, 0.25); -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
            } else if ("active_highlight".equals(state)) {
                card.setStyle("-fx-background-color: -color-bg-elevated; -fx-border-color: -color-accent; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-background-radius: 8;"
                        + "-fx-effect: dropshadow(three-pass-box, rgba(20, 184, 166, 0.15), 10, 0, 0, 3);");
            } else if ("active".equals(state)) {
                card.setStyle("-fx-background-color: -color-bg-elevated; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
            }
        });
        
        return card;
    }

    private void rebuildStepCards() {
        stepCards.clear();
        
        // Step 1: Download mobile app
        stepCards.add(createStepCardStyle("Step 1", "Download mobile app", "📥", "active"));
            
        // Step 2: Activate app access
        stepCards.add(createStepCardStyle("Step 2", "Activate app access", "⚡", "active_highlight"));
            
        // Step 3: Login via credentials
        stepCards.add(createStepCardStyle("Step 3", "Login via credentials", "🔑", "muted"));
            
        // Step 4: Access portal services
        stepCards.add(createStepCardStyle("Step 4", "Access portal services", "✨", "muted"));
            
        updateStepsLayout(this.isLayoutNarrow);
    }

    private void updateStepsLayout(boolean isNarrow) {
        this.isLayoutNarrow = isNarrow;
        stepsContainer.getChildren().clear();
        if (isNarrow) {
            VBox vbox = new VBox(10);
            for (VBox card : stepCards) {
                vbox.getChildren().add(card);
                VBox.setVgrow(card, Priority.ALWAYS);
            }
            stepsContainer.getChildren().add(vbox);
        } else {
            HBox hbox = new HBox(12);
            for (VBox card : stepCards) {
                hbox.getChildren().add(card);
                HBox.setHgrow(card, Priority.ALWAYS);
            }
            stepsContainer.getChildren().add(hbox);
        }
    }

    // ==================== Utilities ====================

    private void fetchPlayStoreLink() {
        new Thread(() -> {
            try {
                String html = context.dataCacheService().getCachedHtml("GenerateAppPassword.aspx").orElse(null);
                if (html == null && context.isOnline()) {
                    html = context.fetchAndCacheHtml("GenerateAppPassword.aspx");
                }
                
                String playStoreUrl = "https://play.google.com/store/apps/details?id=edupk.cuiatd.cuonlinestudentportal";
                if (html != null && !html.isBlank()) {
                    Document doc = Jsoup.parse(html);
                    for (Element a : doc.select("a[href*=play.google.com]")) {
                        String href = a.attr("href");
                        if (!href.isEmpty()) {
                            playStoreUrl = href;
                            break;
                        }
                    }
                }
                
                final String finalPlayStoreUrl = playStoreUrl;
                Platform.runLater(() -> {
                    if (playStoreLinkBtn != null) {
                        playStoreLinkBtn.setOnAction(e -> launchUrl(finalPlayStoreUrl));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void launchUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setStatusSuccess(Label lbl, String text) {
        lbl.setText(text);
        lbl.setVisible(true);
        lbl.setManaged(true);
        lbl.setStyle("-fx-background-color: rgba(16, 185, 129, 0.08); -fx-border-color: #10B981; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12; -fx-text-fill: #10B981; -fx-font-size: 11px; -fx-font-weight: bold;");
    }

    private void setStatusError(Label lbl, String text) {
        lbl.setText(text);
        lbl.setVisible(true);
        lbl.setManaged(true);
        lbl.setStyle("-fx-background-color: rgba(239, 68, 68, 0.08); -fx-border-color: #EF4444; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12; -fx-text-fill: #EF4444; -fx-font-size: 11px; -fx-font-weight: bold;");
    }
    
    private void clearStatus(Label lbl) {
        lbl.setText("");
        lbl.setVisible(false);
        lbl.setManaged(false);
    }

    private void onConnectivityChanged(boolean isOnline) {
        Platform.runLater(() -> {
            if (submitBtn != null) {
                if (isOnline) {
                    submitBtn.setDisable(false);
                    submitBtn.setText("Activate Mobile Access");
                    submitBtn.setStyle("-fx-background-color: -color-accent; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
                    submitBtn.setTooltip(null);
                } else {
                    submitBtn.setDisable(true);
                    submitBtn.setText("🔒 Offline Mode");
                    submitBtn.setStyle("-fx-background-color: -color-border; -fx-text-fill: -color-text-muted; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 8;");
                    submitBtn.setTooltip(new Tooltip("This feature is disabled in offline mode."));
                }
            }
        });
    }

    public ScrollPane getRoot() {
        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        return sp;
    }
}
