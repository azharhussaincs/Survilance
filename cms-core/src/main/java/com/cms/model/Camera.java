package com.cms.model;

import java.time.LocalDateTime;

/**
 * Represents an individual camera channel on an NVR/DVR.
 */
public class Camera {
    private Long id;
    private Long nvrId;
    private String cameraName;
    private int channelNumber;
    private String streamUrl;
    private String snapshotUrl;
    private String onvifProfileToken;
    private StreamType streamType;
    private CameraStatus status;
    private int width;
    private int height;
    private String codec;
    private LocalDateTime createdAt;
    private NvrDevice nvr; // parent reference

    public enum StreamType {
        RTSP, ONVIF, MJPEG, HTTP_SNAPSHOT, WEBVIEW, UNKNOWN
    }

    public enum CameraStatus {
        ONLINE, OFFLINE, UNKNOWN, ERROR
    }

    public Camera() {
        this.status = CameraStatus.UNKNOWN;
        this.streamType = StreamType.UNKNOWN;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getNvrId() { return nvrId; }
    public void setNvrId(Long nvrId) { this.nvrId = nvrId; }
    public String getCameraName() { return cameraName; }
    public void setCameraName(String cameraName) { this.cameraName = cameraName; }
    public int getChannelNumber() { return channelNumber; }
    public void setChannelNumber(int channelNumber) { this.channelNumber = channelNumber; }
    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }
    public String getSnapshotUrl() { return snapshotUrl; }
    public void setSnapshotUrl(String snapshotUrl) { this.snapshotUrl = snapshotUrl; }
    public String getOnvifProfileToken() { return onvifProfileToken; }
    public void setOnvifProfileToken(String onvifProfileToken) { this.onvifProfileToken = onvifProfileToken; }
    public StreamType getStreamType() { return streamType; }
    public void setStreamType(StreamType streamType) { this.streamType = streamType; }
    public CameraStatus getStatus() { return status; }
    public void setStatus(CameraStatus status) { this.status = status; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public String getCodec() { return codec; }
    public void setCodec(String codec) { this.codec = codec; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public NvrDevice getNvr() { return nvr; }
    public void setNvr(NvrDevice nvr) { this.nvr = nvr; }

    @Override
    public String toString() {
        return cameraName + " (Ch." + channelNumber + ")";
    }
}
