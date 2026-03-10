package com.cms.database;

import com.cms.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the database connection pool and schema initialization.
 * Singleton, uses AppConfig defaults if cms.properties is missing.
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    private HikariDataSource dataSource;

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initialize HikariCP connection pool and database schema
     */
    public void initialize() throws SQLException {
        AppConfig config = AppConfig.getInstance();

        String dbUrl = config.get("db.url");
        String dbUser = config.get("db.username");
        String dbPass = config.get("db.password");

        logger.info("Initializing database connection pool...");
        logger.info("DB URL: {}", dbUrl);
        logger.info("DB User: {}", dbUser);
        logger.info("Password Provided: {}", dbPass != null && !dbPass.isEmpty());

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dbUrl);
        hikariConfig.setUsername(dbUser);
        hikariConfig.setPassword(dbPass);
        hikariConfig.setMaximumPoolSize(config.getInt("db.pool.size", 10));
        hikariConfig.setMinimumIdle(config.getInt("db.pool.min.idle", 2));
        hikariConfig.setConnectionTimeout(config.getInt("db.pool.connection.timeout", 30000));
        hikariConfig.setIdleTimeout(config.getInt("db.pool.idle.timeout", 600000));
        hikariConfig.setMaxLifetime(config.getInt("db.pool.max.lifetime", 1800000));
        hikariConfig.setPoolName("CMS-DB-Pool");

        // JDBC optimizations
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(hikariConfig);
        logger.info("Database connection pool initialized successfully");

        initializeSchema();
    }

    /**
     * Creates tables and seeds default users if needed
     */
    private void initializeSchema() throws SQLException {
        logger.info("Initializing database schema...");
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // Users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    role ENUM('ADMIN','MANAGER') NOT NULL DEFAULT 'MANAGER',
                    full_name VARCHAR(100),
                    email VARCHAR(100),
                    active BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_login TIMESTAMP NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            // NVR devices table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS nvrs (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    location_name VARCHAR(100) NOT NULL,
                    ip_address VARCHAR(255) NOT NULL,
                    port INT DEFAULT 80,
                    username VARCHAR(100),
                    password VARCHAR(255),
                    brand ENUM('HIKVISION','DAHUA','CP_PLUS','ONVIF','GENERIC_HTTP') NOT NULL DEFAULT 'ONVIF',
                    protocol ENUM('ONVIF','RTSP','HTTP','HTTPS','SDK') DEFAULT 'ONVIF',
                    connection_status ENUM('CONNECTED','DISCONNECTED','CONNECTING','ERROR','UNKNOWN') DEFAULT 'UNKNOWN',
                    firmware_version VARCHAR(100),
                    device_model VARCHAR(100),
                    mac_address VARCHAR(17),
                    total_channels INT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_connected TIMESTAMP NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            // Cameras table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS cameras (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    nvr_id BIGINT NOT NULL,
                    camera_name VARCHAR(100) NOT NULL,
                    channel_number INT NOT NULL,
                    stream_url VARCHAR(500),
                    snapshot_url VARCHAR(500),
                    onvif_profile_token VARCHAR(100),
                    stream_type ENUM('RTSP','ONVIF','MJPEG','HTTP_SNAPSHOT','WEBVIEW','UNKNOWN') DEFAULT 'UNKNOWN',
                    status ENUM('ONLINE','OFFLINE','UNKNOWN','ERROR') DEFAULT 'UNKNOWN',
                    width INT DEFAULT 0,
                    height INT DEFAULT 0,
                    codec VARCHAR(50),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (nvr_id) REFERENCES nvrs(id) ON DELETE CASCADE,
                    UNIQUE KEY unique_nvr_channel (nvr_id, channel_number)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            // Layouts table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS layouts (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    layout_name VARCHAR(100) NOT NULL,
                    `rows` INT NOT NULL,
                    columns_count INT NOT NULL,
                    cell_camera_map JSON,
                    user_id BIGINT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            // Seed default admin/manager
            seedDefaultUsers(stmt);
            logger.info("Database schema initialized successfully");
        }
    }

    /**
     * Seeds default users if they do not exist
     */
    private void seedDefaultUsers(Statement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username='admin'")) {
            rs.next();
            if (rs.getInt(1) == 0) {
                String adminHash = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
                        .hashToString(12, "admin123".toCharArray());
                String managerHash = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
                        .hashToString(12, "manager123".toCharArray());

                stmt.executeUpdate("""
                    INSERT INTO users (username, password_hash, role, full_name) VALUES
                    ('admin', '%s', 'ADMIN', 'System Administrator'),
                    ('manager', '%s', 'MANAGER', 'Security Manager')
                """.formatted(adminHash, managerHash));

                logger.info("Default users seeded: admin and manager");
            }
        }
    }

    /**
     * Returns a live DB connection
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database not initialized. Call initialize() first.");
        }
        return dataSource.getConnection();
    }

    /**
     * Checks if pool is active
     */
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * Closes the HikariCP pool
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
}