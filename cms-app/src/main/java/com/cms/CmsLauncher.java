package com.cms;

import com.cms.config.AppConfig;
import com.cms.database.DatabaseManager;
import com.cms.ui.util.UIUtils;
import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Launcher class to avoid JavaFX classpath issues.
 * Initializes database before launching the JavaFX application.
 */
public class CmsLauncher {
    private static final Logger logger = LoggerFactory.getLogger(CmsLauncher.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("  CMS - Camera Management System");
        logger.info("  Version: {}", AppConfig.getInstance().get("app.version"));
        logger.info("========================================");

        // Initialize database
        try {
            logger.info("Connecting to database...");
            DatabaseManager.getInstance().initialize();
            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.error("FATAL: Could not initialize database: {}", e.getMessage(), e);
            System.err.println("\n[FATAL ERROR] Could not connect to database!");
            System.err.println("Please ensure MySQL is running and configure connection in:");
            System.err.println("  ~/.cms/cms.properties");
            System.err.println("\nError: " + e.getMessage());
            System.err.println("\nDefault config:");
            System.err.println("  db.url=jdbc:mysql://localhost:3306/cms_db?useSSL=false&allowPublicKeyRetrieval=true");
            System.err.println("  db.username=root");
            System.err.println("  db.password=");
            System.exit(1);
        }

        // Launch JavaFX
        Application.launch(CmsApplication.class, args);
    }
}
