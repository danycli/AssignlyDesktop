package com.assignly.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LoginView {
    private final StackPane root = new StackPane();
    private final TextField registrationField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final CheckBox rememberMeCheck = new CheckBox("Remember Me");
    private final Button loginButton = new Button("Sign In");
    private final Label errorLabel = new Label();

    public LoginView() {
        root.setStyle("-fx-background-color: #F0EDEC;");

        VBox card = new VBox(16);
        card.getStyleClass().add("login-card");
        card.setPadding(new Insets(36, 40, 36, 40));
        card.setMaxWidth(380);
        card.setAlignment(Pos.CENTER_LEFT);

        Label brand = new Label("Assignly");
        brand.getStyleClass().add("login-title");

        Label subtitle = new Label("Sign in to your COMSATS portal");
        subtitle.getStyleClass().add("login-subtitle");

        VBox brandBox = new VBox(4, brand, subtitle);
        brandBox.setPadding(new Insets(0, 0, 8, 0));

        Label regLabel = new Label("Registration Number");
        regLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666; -fx-font-weight: 500;");
        registrationField.setPromptText("SP25-BCS-001");
        registrationField.setPrefHeight(38);

        Label passLabel = new Label("Password");
        passLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666; -fx-font-weight: 500;");
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefHeight(38);

        rememberMeCheck.setSelected(true);

        errorLabel.getStyleClass().add("error-text");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);

        loginButton.getStyleClass().add("accent-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(40);
        loginButton.setStyle(loginButton.getStyle() + "-fx-font-size: 14px;");

        Label disclaimer = new Label("Unofficial client · Not affiliated with COMSATS");
        disclaimer.setStyle("-fx-font-size: 10px; -fx-text-fill: #bbbbbb;");

        card.getChildren().addAll(
                brandBox,
                regLabel, registrationField,
                passLabel, passwordField,
                rememberMeCheck,
                errorLabel,
                loginButton,
                disclaimer
        );

        root.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);
    }

    public StackPane getRoot() { return root; }
    public TextField getRegistrationField() { return registrationField; }
    public PasswordField getPasswordField() { return passwordField; }
    public CheckBox getRememberMeCheck() { return rememberMeCheck; }
    public Button getLoginButton() { return loginButton; }
    public Label getErrorLabel() { return errorLabel; }
}
