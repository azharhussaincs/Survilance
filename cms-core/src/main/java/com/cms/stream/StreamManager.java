package com.cms.stream;

import com.cms.model.Camera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active stream sessions for camera feeds.
 */
public class StreamManager {
    private static final Logger logger = LoggerFactory.getLogger(StreamManager.class);
    private static StreamManager instance;
    private final Map<Long, StreamSession> activeSessions = new ConcurrentHashMap<>();

    private StreamManager() {}

    public static synchronized StreamManager getInstance() {
        if (instance == null) instance = new StreamManager();
        return instance;
    }

    public StreamSession getOrCreateSession(Camera camera) {
        return activeSessions.computeIfAbsent(camera.getId(), id -> {
            StreamSession session = new StreamSession(camera);
            logger.info("Created stream session for camera: {}", camera.getCameraName());
            return session;
        });
    }

    public void closeSession(Long cameraId) {
        StreamSession session = activeSessions.remove(cameraId);
        if (session != null) {
            session.close();
            logger.info("Closed stream session for camera id: {}", cameraId);
        }
    }

    public void closeAll() {
        activeSessions.values().forEach(StreamSession::close);
        activeSessions.clear();
        logger.info("All stream sessions closed");
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public boolean hasSession(Long cameraId) {
        return activeSessions.containsKey(cameraId);
    }
}
