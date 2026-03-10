package com.cms.stream;

import com.cms.model.Camera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Represents an active stream session for a camera.
 */
public class StreamSession {
    private static final Logger logger = LoggerFactory.getLogger(StreamSession.class);

    private final Camera camera;
    private StreamStatus status;
    private LocalDateTime startedAt;
    private String errorMessage;
    private int reconnectAttempts;
    private static final int MAX_RECONNECT = 3;

    public enum StreamStatus {
        INITIALIZING, PLAYING, PAUSED, ERROR, CLOSED, RECONNECTING
    }

    public StreamSession(Camera camera) {
        this.camera = camera;
        this.status = StreamStatus.INITIALIZING;
        this.startedAt = LocalDateTime.now();
        this.reconnectAttempts = 0;
    }

    public void close() {
        this.status = StreamStatus.CLOSED;
        logger.debug("Stream session closed for camera: {}", camera.getCameraName());
    }

    public void setError(String message) {
        this.status = StreamStatus.ERROR;
        this.errorMessage = message;
        logger.warn("Stream error for camera {}: {}", camera.getCameraName(), message);
    }

    public boolean canReconnect() {
        return reconnectAttempts < MAX_RECONNECT;
    }

    public void incrementReconnect() {
        reconnectAttempts++;
        status = StreamStatus.RECONNECTING;
    }

    // Getters
    public Camera getCamera() { return camera; }
    public StreamStatus getStatus() { return status; }
    public void setStatus(StreamStatus status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public String getErrorMessage() { return errorMessage; }
    public int getReconnectAttempts() { return reconnectAttempts; }
}
