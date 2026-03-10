package com.cms;

import com.cms.config.AppConfig;
import com.cms.database.DatabaseManager;
import com.cms.stream.StreamManager;
import com.cms.ui.util.SceneManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main JavaFX application entry point.
 * Initializes all services and launches the login screen.
 */
public class CmsApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(CmsApplication.class);

    @Override
    public void start(Stage primaryStage) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logger.error("Uncaught exception in thread {}: {}", thread.getName(), throwable.getMessage(), throwable);
        });

        SceneManager.getInstance().setPrimaryStage(primaryStage);

        primaryStage.setTitle("CMS - Camera Management System");
        primaryStage.setOnCloseRequest(event -> {
            logger.info("Application closing...");
            StreamManager.getInstance().closeAll();
            DatabaseManager.getInstance().shutdown();
            Platform.exit();
            System.exit(0);
        });

        // Show login
        SceneManager.getInstance().showLogin();
        logger.info("CMS Application started");
    }

    @Override
    public void stop() {
        logger.info("Application stopping, cleaning up resources...");
        StreamManager.getInstance().closeAll();
        DatabaseManager.getInstance().shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
