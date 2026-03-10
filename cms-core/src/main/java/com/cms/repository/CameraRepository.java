package com.cms.repository;

import com.cms.database.DatabaseManager;
import com.cms.model.Camera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access object for Camera entities.
 */
public class CameraRepository {
    private static final Logger logger = LoggerFactory.getLogger(CameraRepository.class);

    public Camera save(Camera camera) {
        if (camera.getId() == null) {
            return insert(camera);
        }
        return update(camera);
    }

    private Camera insert(Camera camera) {
        String sql = """
            INSERT INTO cameras (nvr_id, camera_name, channel_number, stream_url, snapshot_url,
                onvif_profile_token, stream_type, status, width, height, codec)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, camera.getNvrId());
            ps.setString(2, camera.getCameraName());
            ps.setInt(3, camera.getChannelNumber());
            ps.setString(4, camera.getStreamUrl());
            ps.setString(5, camera.getSnapshotUrl());
            ps.setString(6, camera.getOnvifProfileToken());
            ps.setString(7, camera.getStreamType().name());
            ps.setString(8, camera.getStatus().name());
            ps.setInt(9, camera.getWidth());
            ps.setInt(10, camera.getHeight());
            ps.setString(11, camera.getCodec());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) camera.setId(rs.getLong(1));
            }
        } catch (SQLException e) {
            logger.error("Error inserting camera: {}", e.getMessage());
            throw new RuntimeException("Failed to save camera", e);
        }
        return camera;
    }

    private Camera update(Camera camera) {
        String sql = """
            UPDATE cameras SET camera_name=?, channel_number=?, stream_url=?, snapshot_url=?,
                onvif_profile_token=?, stream_type=?, status=?, width=?, height=?, codec=?
            WHERE id=?
        """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, camera.getCameraName());
            ps.setInt(2, camera.getChannelNumber());
            ps.setString(3, camera.getStreamUrl());
            ps.setString(4, camera.getSnapshotUrl());
            ps.setString(5, camera.getOnvifProfileToken());
            ps.setString(6, camera.getStreamType().name());
            ps.setString(7, camera.getStatus().name());
            ps.setInt(8, camera.getWidth());
            ps.setInt(9, camera.getHeight());
            ps.setString(10, camera.getCodec());
            ps.setLong(11, camera.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating camera: {}", e.getMessage());
        }
        return camera;
    }

    public List<Camera> findByNvrId(Long nvrId) {
        List<Camera> list = new ArrayList<>();
        String sql = "SELECT * FROM cameras WHERE nvr_id = ? ORDER BY channel_number";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, nvrId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding cameras by NVR id: {}", e.getMessage());
        }
        return list;
    }

    public Optional<Camera> findById(Long id) {
        String sql = "SELECT * FROM cameras WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding camera by id: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public void deleteByNvrId(Long nvrId) {
        String sql = "DELETE FROM cameras WHERE nvr_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, nvrId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting cameras for NVR: {}", e.getMessage());
        }
    }

    public void saveAll(List<Camera> cameras) {
        for (Camera c : cameras) save(c);
    }

    private Camera mapRow(ResultSet rs) throws SQLException {
        Camera c = new Camera();
        c.setId(rs.getLong("id"));
        c.setNvrId(rs.getLong("nvr_id"));
        c.setCameraName(rs.getString("camera_name"));
        c.setChannelNumber(rs.getInt("channel_number"));
        c.setStreamUrl(rs.getString("stream_url"));
        c.setSnapshotUrl(rs.getString("snapshot_url"));
        c.setOnvifProfileToken(rs.getString("onvif_profile_token"));
        try { c.setStreamType(Camera.StreamType.valueOf(rs.getString("stream_type"))); } catch (Exception e) { c.setStreamType(Camera.StreamType.UNKNOWN); }
        try { c.setStatus(Camera.CameraStatus.valueOf(rs.getString("status"))); } catch (Exception e) { c.setStatus(Camera.CameraStatus.UNKNOWN); }
        c.setWidth(rs.getInt("width"));
        c.setHeight(rs.getInt("height"));
        c.setCodec(rs.getString("codec"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) c.setCreatedAt(createdAt.toLocalDateTime());
        return c;
    }
}
