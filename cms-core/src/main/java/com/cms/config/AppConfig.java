package com.cms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Central application configuration manager.
 * Loads sensible defaults and enforces correct DB credentials.
 */
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static AppConfig instance;
    private final Properties properties = new Properties();

    private AppConfig() {
        loadDefaults();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    /**
     * Load all default properties directly
     */
    private void loadDefaults() {
        // Application info
        properties.put("app.name", "CMS - Camera Management System");
        properties.put("app.version", "1.0.0");

        // Database defaults (force working credentials)
        properties.put("db.url", "jdbc:mysql://localhost:3306/cms_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        properties.put("db.username", "root");
        properties.put("db.password", "admin");
        properties.put("db.pool.size", "10");
        properties.put("db.pool.min.idle", "2");
        properties.put("db.pool.connection.timeout", "30000");
        properties.put("db.pool.idle.timeout", "600000");
        properties.put("db.pool.max.lifetime", "1800000");

        // Stream / ONVIF defaults
        properties.put("stream.connection.timeout", "10000");
        properties.put("stream.read.timeout", "30000");
        properties.put("onvif.discovery.timeout", "5000");
        properties.put("nvr.connect.timeout", "15000");

        // UI
        properties.put("ui.theme", "dark");

        logger.info("AppConfig loaded with defaults (DB user=root, password=admin)");
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = properties.getProperty(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val);
    }

    public void set(String key, String value) {
        properties.setProperty(key, value);
    }

    // No need to save config file anymore; all defaults are in-memory
}