-- =====================================================
-- CMS - Camera Management System
-- MySQL Database Schema
-- =====================================================

CREATE DATABASE IF NOT EXISTS cms_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE cms_db;

-- =====================================================
-- Users Table
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
                                     id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     username       VARCHAR(50) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL COMMENT 'BCrypt hashed password',
    role           ENUM('ADMIN','MANAGER') NOT NULL DEFAULT 'MANAGER',
    full_name      VARCHAR(100),
    email          VARCHAR(100),
    active         BOOLEAN DEFAULT TRUE,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login     TIMESTAMP NULL,
    INDEX idx_username (username),
    INDEX idx_active (active)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COMMENT='System users';

-- =====================================================
-- NVR/DVR Devices Table
-- =====================================================
CREATE TABLE IF NOT EXISTS nvrs (
                                    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    location_name      VARCHAR(100) NOT NULL COMMENT 'User-defined name',
    ip_address         VARCHAR(255) NOT NULL,
    port               INT DEFAULT 80,
    username           VARCHAR(100),
    password           VARCHAR(255) COMMENT 'Stored encrypted',
    brand              ENUM('HIKVISION','DAHUA','CP_PLUS','ONVIF','GENERIC_HTTP') NOT NULL DEFAULT 'ONVIF',
    protocol           ENUM('ONVIF','RTSP','HTTP','HTTPS','SDK') DEFAULT 'ONVIF',
    connection_status  ENUM('CONNECTED','DISCONNECTED','CONNECTING','ERROR','UNKNOWN') DEFAULT 'UNKNOWN',
    firmware_version   VARCHAR(100),
    device_model       VARCHAR(100),
    mac_address        VARCHAR(17),
    total_channels     INT DEFAULT 0,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_connected     TIMESTAMP NULL,
    INDEX idx_ip (ip_address),
    INDEX idx_brand (brand)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COMMENT='NVR/DVR devices';

-- =====================================================
-- Cameras Table
-- =====================================================
CREATE TABLE IF NOT EXISTS cameras (
                                       id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       nvr_id               BIGINT NOT NULL,
                                       camera_name          VARCHAR(100) NOT NULL,
    channel_number       INT NOT NULL,
    stream_url           VARCHAR(500) COMMENT 'RTSP or HTTP stream URL',
    snapshot_url         VARCHAR(500) COMMENT 'HTTP snapshot URL',
    onvif_profile_token  VARCHAR(100) COMMENT 'ONVIF profile token',
    stream_type          ENUM('RTSP','ONVIF','MJPEG','HTTP_SNAPSHOT','WEBVIEW','UNKNOWN') DEFAULT 'UNKNOWN',
    status               ENUM('ONLINE','OFFLINE','UNKNOWN','ERROR') DEFAULT 'UNKNOWN',
    width                INT DEFAULT 0,
    height               INT DEFAULT 0,
    codec                VARCHAR(50),
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_camera_nvr
    FOREIGN KEY (nvr_id) REFERENCES nvrs(id)
    ON DELETE CASCADE,

    UNIQUE KEY unique_nvr_channel (nvr_id, channel_number),
    INDEX idx_nvr_id (nvr_id),
    INDEX idx_status (status)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COMMENT='Camera channels';

-- =====================================================
-- Grid Layouts Table
-- =====================================================
CREATE TABLE IF NOT EXISTS layouts (
                                       id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       layout_name      VARCHAR(100) NOT NULL,
    `row_count`        INT NOT NULL DEFAULT 2,
    `column_count`     INT NOT NULL DEFAULT 2,
    cell_camera_map  JSON COMMENT 'Map of grid cell index to camera id',
    user_id          BIGINT,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_layout_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE SET NULL,

    INDEX idx_user_id (user_id)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COMMENT='Saved grid layouts';

-- =====================================================
-- Default Users (passwords will be replaced by app)
-- =====================================================
-- Admin: admin / admin123
-- Manager: manager / manager123
-- NOTE: Actual BCrypt hashes are inserted by the application on first run.

-- =====================================================
-- Sample Data (optional for testing)
-- =====================================================
-- INSERT INTO nvrs (location_name, ip_address, port, username, password, brand)
-- VALUES ('Test NVR', '192.168.1.64', 80, 'admin', 'admin123', 'HIKVISION');