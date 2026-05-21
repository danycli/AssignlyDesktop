package com.assignly.util;

import javafx.scene.control.Alert;

public final class UiUtil {
    private UiUtil() {
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Assignly Desktop");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
