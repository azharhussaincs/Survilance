package com.cms.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an NVR/DVR device in the system.
 */
public class NvrDevice {
    private Long id;
    private String locationName;
    private String ipAddress;
    private int port;
    private String username;
    private String password;
    private NvrBrand brand;
    private NvrProtocol protocol;
    private ConnectionStatus connectionStatus;
    private String firmwareVersion;
    private String deviceModel;
    private String macAddress;
    private int totalChannels;
    private LocalDateTime createdAt;
    private LocalDateTime lastConnected;
    private List<Camera> cameras = new ArrayList<>();

    public enum NvrBrand {
        HIKVISION("Hikvision", 8000, 80),
        DAHUA("Dahua", 37777, 80),
        CP_PLUS("CP Plus", 37777, 80),
        ONVIF("ONVIF Generic", 80, 80),
        GENERIC_HTTP("Generic HTTP", 80, 80);

        private final String displayName;
        private final int defaultRtspPort;
        private final int defaultHttpPort;

        NvrBrand(String displayName, int defaultRtspPort, int defaultHttpPort) {
            this.displayName = displayName;
            this.defaultRtspPort = defaultRtspPort;
            this.defaultHttpPort = defaultHttpPort;
        }

        public String getDisplayName() { return displayName; }
        public int getDefaultRtspPort() { return defaultRtspPort; }
        public int getDefaultHttpPort() { return defaultHttpPort; }
    }

    public enum NvrProtocol {
        ONVIF, RTSP, HTTP, HTTPS, SDK
    }

    public enum ConnectionStatus {
        CONNECTED, DISCONNECTED, CONNECTING, ERROR, UNKNOWN
    }

    public NvrDevice() {
        this.connectionStatus = ConnectionStatus.UNKNOWN;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public NvrBrand getBrand() { return brand; }
    public void setBrand(NvrBrand brand) { this.brand = brand; }
    public NvrProtocol getProtocol() { return protocol; }
    public void setProtocol(NvrProtocol protocol) { this.protocol = protocol; }
    public ConnectionStatus getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(ConnectionStatus connectionStatus) { this.connectionStatus = connectionStatus; }
    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }
    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    public int getTotalChannels() { return totalChannels; }
    public void setTotalChannels(int totalChannels) { this.totalChannels = totalChannels; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastConnected() { return lastConnected; }
    public void setLastConnected(LocalDateTime lastConnected) { this.lastConnected = lastConnected; }
    public List<Camera> getCameras() { return cameras; }
    public void setCameras(List<Camera> cameras) { this.cameras = cameras; }

    public String getBaseUrl() {
        return "http://" + ipAddress + (port != 80 ? ":" + port : "");
    }

    @Override
    public String toString() {
        return locationName + " (" + ipAddress + ")";
    }
}
