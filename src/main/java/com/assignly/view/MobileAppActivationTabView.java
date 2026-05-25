package com.assignly.view;

import com.assignly.util.AppContext;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MobileAppActivationTabView {
    private final VBox root = new VBox();
    private final AppContext context;

    public MobileAppActivationTabView(AppContext context) {
        this.context = context;
        buildUI();
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

        javafx.scene.control.Button submitBtn = new javafx.scene.control.Button("Submit");
        submitBtn.setStyle("-fx-background-color:#004643;-fx-text-fill:white;-fx-font-weight:bold;-fx-padding:6 20;-fx-background-radius:4;");

        Label statusLbl = new Label("");
        statusLbl.setStyle("-fx-text-fill:#059669;-fx-font-weight:bold;");
        statusLbl.setWrapText(true);

        VBox instructions = new VBox(10);
        instructions.setStyle("-fx-padding:20 0 0 0;");
        
        javafx.scene.text.Text i1Text = new javafx.scene.text.Text("1 - Download the App CUOnline CUI Abbottabad from Google Play Store. ");
        i1Text.setStyle("-fx-fill:#0369a1;-fx-font-weight:bold;-fx-font-size:12px;");
        javafx.scene.control.Hyperlink downloadLink = new javafx.scene.control.Hyperlink("Download");
        downloadLink.setStyle("-fx-text-fill:#2563eb;-fx-font-weight:bold;-fx-padding:0;-fx-font-size:12px;");
        downloadLink.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://play.google.com/store/apps/details?id=edupk.cuiatd.cuonlinestudentportal"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        javafx.scene.text.TextFlow i1 = new javafx.scene.text.TextFlow(i1Text, downloadLink);
        
        Label i2 = new Label("2 - Activate the App from this Page.");
        i2.setStyle("-fx-text-fill:#0369a1;-fx-font-weight:bold;");
        Label i3 = new Label("3 - Use same Password for Portal and App Login.");
        i3.setStyle("-fx-text-fill:#0369a1;-fx-font-weight:bold;");
        
        instructions.getChildren().addAll(i1, i2, i3);

        submitBtn.setOnAction(e -> {
            String enteredPass = passField.getText();
            String actualPass = context.getSessionPassword() != null ? context.getSessionPassword() : "";
            
            if (enteredPass.isEmpty()) {
                statusLbl.setText("Please enter your password.");
                statusLbl.setStyle("-fx-text-fill:#dc2626;-fx-font-weight:bold;");
                return;
            }
            
            if (!enteredPass.equals(actualPass)) {
                statusLbl.setText("Your current password is incorrect");
                statusLbl.setStyle("-fx-text-fill:#dc2626;-fx-font-weight:bold;");
                return;
            }

            submitBtn.setDisable(true);
            submitBtn.setText("Submitting...");
            new Thread(() -> {
                String msg = context.portalRepository().generateAppPassword(enteredPass);
                javafx.application.Platform.runLater(() -> {
                    submitBtn.setDisable(false);
                    submitBtn.setText("Submit");
                    statusLbl.setText(msg);
                    if (msg.toLowerCase().contains("success") || msg.toLowerCase().contains("activated")) {
                        statusLbl.setStyle("-fx-text-fill:#059669;-fx-font-weight:bold;");
                    } else if (msg.contains("completed") || msg.contains("no explicit message")) {
                        // Fallback in case extractPortalMessage misses the success alert
                        statusLbl.setText("Student App is Activated. Use Same Password for Student Portal and App login.");
                        statusLbl.setStyle("-fx-text-fill:#059669;-fx-font-weight:bold;");
                    } else {
                        statusLbl.setStyle("-fx-text-fill:#dc2626;-fx-font-weight:bold;");
                    }
                });
            }).start();
        });

        root.getChildren().addAll(title, inputBox, submitBtn, statusLbl, instructions);
    }

    public VBox getRoot() { return root; }
}
