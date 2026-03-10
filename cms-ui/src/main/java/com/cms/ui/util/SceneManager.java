package com.cms.ui.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Manages scene transitions and window configuration.
 */
public class SceneManager {
    private static final Logger logger = LoggerFactory.getLogger(SceneManager.class);
    private static SceneManager instance;
    private Stage primaryStage;

    private SceneManager() {}

    public static synchronized SceneManager getInstance() {
        if (instance == null) instance = new SceneManager();
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void showLogin() {
        loadScene("/com/cms/ui/fxml/login.fxml", "CMS - Login", 900, 600, false);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
    }

    public void showDashboard() {
        loadScene("/com/cms/ui/fxml/dashboard.fxml", "CMS - Camera Management System", 1400, 900, true);
        primaryStage.setResizable(true);
        primaryStage.setMaximized(true);
        primaryStage.centerOnScreen();
    }

    private void loadScene(String fxmlPath, String title, double width, double height, boolean resizable) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/com/cms/ui/css/dark-theme.css").toExternalForm());
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.setResizable(resizable);
            primaryStage.show();
        } catch (IOException e) {
            logger.error("Failed to load scene: {}", fxmlPath, e);
            UIUtils.showError("UI Error", "Failed to load screen: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    public Stage getPrimaryStage() { return primaryStage; }
}
