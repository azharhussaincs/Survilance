package com.cms.service;

import com.cms.model.Camera;
import com.cms.model.NvrDevice;
import com.cms.onvif.OnvifClient;
import com.cms.repository.CameraRepository;
import com.cms.repository.NvrRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for NVR device management and camera discovery.
 */
public class NvrService {
    private static final Logger logger = LoggerFactory.getLogger(NvrService.class);
    private static NvrService instance;
    private final NvrRepository nvrRepository;
    private final CameraRepository cameraRepository;
    private final OnvifClient onvifClient;

    private NvrService() {
        this.nvrRepository = new NvrRepository();
        this.cameraRepository = new CameraRepository();
        this.onvifClient = new OnvifClient();
    }

    public static synchronized NvrService getInstance() {
        if (instance == null) instance = new NvrService();
        return instance;
    }

    /**
     * Connect to an NVR, authenticate, and discover cameras.
     */
    public ConnectionResult connectNvr(NvrDevice nvr) {
        logger.info("Connecting to NVR: {} at {}:{}", nvr.getLocationName(), nvr.getIpAddress(), nvr.getPort());
        nvr.setConnectionStatus(NvrDevice.ConnectionStatus.CONNECTING);

        try {
            List<Camera> discovered = discoverCameras(nvr);
            nvr.setConnectionStatus(NvrDevice.ConnectionStatus.CONNECTED);
            nvr.setLastConnected(java.time.LocalDateTime.now());

            // Persist NVR
            NvrDevice saved = nvrRepository.save(nvr);

            // Replace cameras
            cameraRepository.deleteByNvrId(saved.getId());
            discovered.forEach(c -> c.setNvrId(saved.getId()));
            cameraRepository.saveAll(discovered);
            saved.setCameras(discovered);

            logger.info("Connected to NVR '{}', discovered {} cameras", nvr.getLocationName(), discovered.size());
            return ConnectionResult.success(saved, discovered);

        } catch (Exception e) {
            nvr.setConnectionStatus(NvrDevice.ConnectionStatus.ERROR);
            logger.error("Failed to connect to NVR {}: {}", nvr.getLocationName(), e.getMessage());
            return ConnectionResult.failure(e.getMessage());
        }
    }

    private List<Camera> discoverCameras(NvrDevice nvr) throws Exception {
        return switch (nvr.getBrand()) {
            case HIKVISION -> discoverHikvisionCameras(nvr);
            case DAHUA -> discoverDahuaCameras(nvr);
            case CP_PLUS -> discoverDahuaCameras(nvr); // CP Plus uses Dahua protocol
            case ONVIF -> discoverOnvifCameras(nvr);
            case GENERIC_HTTP -> discoverGenericCameras(nvr);
        };
    }

    private List<Camera> discoverOnvifCameras(NvrDevice nvr) throws Exception {
        logger.info("Discovering ONVIF cameras on {}", nvr.getIpAddress());
        return onvifClient.discoverProfiles(nvr);
    }

    private List<Camera> discoverHikvisionCameras(NvrDevice nvr) {
        logger.info("Discovering Hikvision cameras on {}", nvr.getIpAddress());
        List<Camera> cameras = new ArrayList<>();
        // Hikvision ISAPI - try to get channel list
        // For demo: also try ONVIF fallback
        try {
            cameras = onvifClient.discoverProfiles(nvr);
        } catch (Exception e) {
            logger.warn("ONVIF failed for Hikvision, generating RTSP streams: {}", e.getMessage());
        }
        // If ONVIF failed or returned 0, generate standard Hikvision RTSP URLs
        if (cameras.isEmpty()) {
            cameras = generateHikvisionRtspCameras(nvr);
        }
        return cameras;
    }

    private List<Camera> generateHikvisionRtspCameras(NvrDevice nvr) {
        List<Camera> cameras = new ArrayList<>();
        int channels = Math.max(nvr.getTotalChannels(), 16); // Most NVRs have at least 16 channels
        for (int i = 1; i <= channels; i++) {
            Camera cam = new Camera();
            cam.setNvrId(nvr.getId());
            cam.setCameraName("Camera " + i);
            cam.setChannelNumber(i);
            // Hikvision RTSP format: rtsp://user:pass@ip:554/Streaming/Channels/101
            // 101 = channel 1, main stream; 102 = channel 1, sub stream; 201 = channel 2, main stream
            int rtspPort = 554; // Default RTSP port for Hikvision
            String rtspUrl = String.format("rtsp://%s:%s@%s:%d/Streaming/Channels/%d01",
                nvr.getUsername(), nvr.getPassword(), nvr.getIpAddress(), rtspPort, i);
            cam.setStreamUrl(rtspUrl);
            cam.setStreamType(Camera.StreamType.RTSP);
            cam.setStatus(Camera.CameraStatus.UNKNOWN);
            cameras.add(cam);
        }
        return cameras;
    }

    private List<Camera> discoverDahuaCameras(NvrDevice nvr) {
        logger.info("Discovering Dahua/CP Plus cameras on {}", nvr.getIpAddress());
        List<Camera> cameras = new ArrayList<>();
        try {
            cameras = onvifClient.discoverProfiles(nvr);
        } catch (Exception e) {
            logger.warn("ONVIF failed for Dahua, generating RTSP streams");
        }
        if (cameras.isEmpty()) {
            int channels = Math.max(nvr.getTotalChannels(), 16);
            for (int i = 1; i <= channels; i++) {
                Camera cam = new Camera();
                cam.setCameraName("Channel " + i);
                cam.setChannelNumber(i);
                // Dahua RTSP format: rtsp://user:pass@ip:554/cam/realmonitor?channel=1&subtype=0
                int rtspPort = 554;
                String rtspUrl = String.format("rtsp://%s:%s@%s:%d/cam/realmonitor?channel=%d&subtype=0",
                    nvr.getUsername(), nvr.getPassword(), nvr.getIpAddress(), rtspPort, i);
                cam.setStreamUrl(rtspUrl);
                cam.setStreamType(Camera.StreamType.RTSP);
                cam.setStatus(Camera.CameraStatus.UNKNOWN);
                cameras.add(cam);
            }
        }
        return cameras;
    }

    private List<Camera> discoverGenericCameras(NvrDevice nvr) {
        List<Camera> cameras = new ArrayList<>();
        Camera cam = new Camera();
        cam.setCameraName("Stream 1");
        cam.setChannelNumber(1);
        cam.setStreamUrl("http://" + nvr.getIpAddress() + ":" + nvr.getPort() + "/");
        cam.setStreamType(Camera.StreamType.WEBVIEW);
        cam.setStatus(Camera.CameraStatus.UNKNOWN);
        cameras.add(cam);
        return cameras;
    }

    public List<NvrDevice> getAllNvrsWithCameras() {
        List<NvrDevice> nvrs = nvrRepository.findAll();
        for (NvrDevice nvr : nvrs) {
            List<Camera> cameras = cameraRepository.findByNvrId(nvr.getId());
            cameras.forEach(c -> c.setNvr(nvr));
            nvr.setCameras(cameras);
        }
        return nvrs;
    }

    public void deleteNvr(Long nvrId) {
        nvrRepository.delete(nvrId);
        logger.info("Deleted NVR with id: {}", nvrId);
    }

    public Optional<NvrDevice> findById(Long id) {
        return nvrRepository.findById(id);
    }

    public record ConnectionResult(boolean success, String message, NvrDevice nvr, List<Camera> cameras) {
        public static ConnectionResult success(NvrDevice nvr, List<Camera> cameras) {
            return new ConnectionResult(true, "Connected successfully", nvr, cameras);
        }
        public static ConnectionResult failure(String message) {
            return new ConnectionResult(false, message, null, List.of());
        }
    }
}
