package com.assignly.service;

import com.assignly.util.AppContext;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class NotificationService {
    private static final int MAX_TOASTS = 3;

    private final AppContext context;
    private VBox toastContainer;

    public enum ToastType {
        INFO, SUCCESS, ERROR, WARNING
    }

    public NotificationService(AppContext context) {
        this.context = context;
    }

    public VBox getToastContainer() {
        return toastContainer;
    }

    public void initToastLayer(StackPane root) {
        toastContainer = new VBox(10);
        toastContainer.setFillWidth(false);
        toastContainer.getStyleClass().add("toast-container");
        toastContainer.setPickOnBounds(false); // Do not consume mouse clicks

        StackPane.setAlignment(toastContainer, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toastContainer, new Insets(0, 24, 24, 0));
        root.getChildren().add(toastContainer);
    }

    public void showSuccess(String message) {
        showSuccess(null, message);
    }

    public void showSuccess(String title, String message) {
        showToast(title, message, ToastType.SUCCESS);
    }

    public void showError(String message) {
        showError(null, message);
    }

    public void showError(String title, String message) {
        showToast(title, message, ToastType.ERROR);
    }

    public void showWarning(String message) {
        showWarning(null, message);
    }

    public void showWarning(String title, String message) {
        showToast(title, message, ToastType.WARNING);
    }

    public void showInfo(String message) {
        showInfo(null, message);
    }

    public void showInfo(String title, String message) {
        showToast(title, message, ToastType.INFO);
    }

    private void showToast(String title, String message, ToastType type) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Toast message cannot be blank.");
        }
        if (type == null) {
            type = ToastType.INFO;
        }

        final ToastType finalType = type;
        Runnable showAction = () -> {
            if (toastContainer == null) {
                System.err.println("Toast container is not initialized.");
                return;
            }
            Node toast = buildToastNode(title, message.trim(), finalType);
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

    private Node buildToastNode(String title, String message, ToastType type) {
        HBox toast = new HBox(12);
        toast.getStyleClass().addAll("toast", "toast-" + type.name().toLowerCase());

        Label icon = new Label(switch (type) {
            case SUCCESS -> "✓";
            case ERROR -> "✗";
            case WARNING -> "⚠";
            default -> "i";
        });
        icon.getStyleClass().add("toast-icon");
        
        VBox textContainer = new VBox(4);
        textContainer.setAlignment(Pos.CENTER_LEFT);
        
        if (title != null && !title.isBlank()) {
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("toast-title");
            titleLabel.setWrapText(true);
            titleLabel.setMaxWidth(300);
            textContainer.getChildren().add(titleLabel);
        }

        Label text = new Label(message);
        text.getStyleClass().add("toast-text");
        text.setWrapText(true);
        text.setMaxWidth(300);
        textContainer.getChildren().add(text);

        toast.getChildren().addAll(icon, textContainer);
        toast.setOpacity(0);
        toast.setTranslateX(60);
        toast.setOnMouseClicked(e -> dismissToast(toast));
        return toast;
    }

    private void playToastIn(Node toast) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(220), toast);
        slideIn.setFromX(60);
        slideIn.setToX(0);

        new ParallelTransition(fadeIn, slideIn).play();
    }

    private void scheduleToastDismiss(Node toast) {
        PauseTransition delay = new PauseTransition(Duration.millis(2000));
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
        slideOut.setToX(60);

        ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);
        exit.setOnFinished(e -> toastContainer.getChildren().remove(toast));
        exit.play();
    }
}
