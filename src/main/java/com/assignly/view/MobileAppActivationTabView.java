package com.assignly.view;

import com.assignly.util.AppContext;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MobileAppActivationTabView {
    private final VBox root = new VBox();
    private final AppContext context;
    private javafx.scene.control.Button submitBtn;
    private final java.util.function.Consumer<Boolean> connectivityListener = this::onConnectivityChanged;

    public MobileAppActivationTabView(AppContext context) {
        this.context = context;
        buildUI();
        context.addConnectivityListener(connectivityListener);
    }

    private void buildUI() {
        root.setFillWidth(true);
        root.setPadding(new Insets(24, 28, 24, 28));

        Label title = new Label("Student App Activation");
        title.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill: -color-text-main;");

        javafx.scene.layout.HBox inputBox = new javafx.scene.layout.HBox(10);
        inputBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label passLbl = new Label("Enter Your Current Password:");
        javafx.scene.control.PasswordField passField = new javafx.scene.control.PasswordField();
        passField.setPrefWidth(200);
        
        inputBox.getChildren().addAll(passLbl, passField);

        submitBtn = new javafx.scene.control.Button("Submit");
        submitBtn.getStyleClass().add("btn-primary");

        Label statusLbl = new Label("");
        statusLbl.getStyleClass().add("status-success");
        statusLbl.setWrapText(true);

        VBox instructions = new VBox(10);
        instructions.setStyle("-fx-padding:20 0 0 0;");
        
        javafx.scene.text.Text i1Text = new javafx.scene.text.Text("1 - Download the App CUOnline CUI Abbottabad from Google Play Store. ");
        i1Text.getStyleClass().add("info-text");
        javafx.scene.control.Hyperlink downloadLink = new javafx.scene.control.Hyperlink("Download");
        downloadLink.getStyleClass().add("hyperlink-custom");
        downloadLink.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://play.google.com/store/apps/details?id=edupk.cuiatd.cuonlinestudentportal"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        javafx.scene.text.TextFlow i1 = new javafx.scene.text.TextFlow(i1Text, downloadLink);
        
        Label i2 = new Label("2 - Activate the App from this Page.");
        i2.getStyleClass().add("info-text");
        Label i3 = new Label("3 - Use same Password for Portal and App Login.");
        i3.getStyleClass().add("info-text");
        
        instructions.getChildren().addAll(i1, i2, i3);

        submitBtn.setOnAction(e -> {
            if (!context.isOnline()) {
                setStatusError(statusLbl, "Cannot activate app in offline mode.");
                return;
            }
            String enteredPass = passField.getText();
            String actualPass = context.getSessionPassword() != null ? context.getSessionPassword() : "";
            
            if (enteredPass.isEmpty()) {
                setStatusError(statusLbl, "Please enter your password.");
                return;
            }
            
            if (!enteredPass.equals(actualPass)) {
                setStatusError(statusLbl, "Your current password is incorrect");
                return;
            }

            submitBtn.setDisable(true);
            submitBtn.setText("Submitting...");
            new Thread(() -> {
                String msg = context.portalRepository().generateAppPassword(enteredPass);
                javafx.application.Platform.runLater(() -> {
                    submitBtn.setDisable(false);
                    submitBtn.setText("Submit");
                    if (msg.toLowerCase().contains("success") || msg.toLowerCase().contains("activated")) {
                        setStatusSuccess(statusLbl, msg);
                    } else if (msg.contains("completed") || msg.contains("no explicit message")) {
                        // Fallback in case extractPortalMessage misses the success alert
                        setStatusSuccess(statusLbl, "Student App is Activated. Use Same Password for Student Portal and App login.");
                    } else {
                        setStatusError(statusLbl, msg);
                    }
                });
            }).start();
        });

        root.getChildren().addAll(title, inputBox, submitBtn, statusLbl, instructions);
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

    private void onConnectivityChanged(boolean isOnline) {
        javafx.application.Platform.runLater(() -> {
            if (submitBtn != null) {
                if (isOnline) {
                    submitBtn.setDisable(false);
                    if (submitBtn.getText().startsWith("🔒 ")) {
                        submitBtn.setText(submitBtn.getText().substring(2));
                    }
                    submitBtn.setTooltip(null);
                } else {
                    submitBtn.setDisable(true);
                    if (!submitBtn.getText().startsWith("🔒 ")) {
                        submitBtn.setText("🔒 " + submitBtn.getText());
                    }
                    submitBtn.setTooltip(new javafx.scene.control.Tooltip("This feature is disabled in offline mode."));
                }
            }
        });
    }

    public javafx.scene.control.ScrollPane getRoot() {
        // RESIZING FIX: Wrap the app activation screen inside a ScrollPane so inputs and lists scroll nicely under low-height views
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        return sp;
    }
}
