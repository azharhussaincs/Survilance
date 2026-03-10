package com.cms.ui.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * Utility class for common UI operations and alert dialogs.
 */
public class UIUtils {

    public static void showInfo(String title, String message) {
        runOnFX(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            styleAlert(alert);
            alert.showAndWait();
        });
    }

    public static void showError(String title, String message) {
        runOnFX(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText("Error");
            alert.setContentText(message);
            styleAlert(alert);
            alert.showAndWait();
        });
    }

    public static void showWarning(String title, String message) {
        runOnFX(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            styleAlert(alert);
            alert.showAndWait();
        });
    }

    public static void showException(String title, String message, Throwable ex) {
        runOnFX(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(message);

            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            TextArea textArea = new TextArea(sw.toString());
            textArea.setEditable(false);
            textArea.setWrapText(false);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane content = new GridPane();
            content.setMaxWidth(Double.MAX_VALUE);
            content.add(textArea, 0, 0);
            alert.getDialogPane().setExpandableContent(content);
            styleAlert(alert);
            alert.showAndWait();
        });
    }

    public static Optional<ButtonType> showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(null);
        styleAlert(alert);
        return alert.showAndWait();
    }

    private static void styleAlert(Alert alert) {
        try {
            alert.getDialogPane().getStylesheets().add(
                UIUtils.class.getResource("/com/cms/ui/css/dark-theme.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("cms-dialog");
        } catch (Exception ignored) {}
    }

    private static void runOnFX(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}
