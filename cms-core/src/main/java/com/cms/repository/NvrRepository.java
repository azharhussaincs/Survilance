package com.cms.repository;

import com.cms.database.DatabaseManager;
import com.cms.model.NvrDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access object for NvrDevice entities.
 */
public class NvrRepository {
    private static final Logger logger = LoggerFactory.getLogger(NvrRepository.class);

    public NvrDevice save(NvrDevice nvr) {
        if (nvr.getId() == null) {
            return insert(nvr);
        }
        return update(nvr);
    }

    private NvrDevice insert(NvrDevice nvr) {
        String sql = """
            INSERT INTO nvrs (location_name, ip_address, port, username, password, brand, protocol,
                connection_status, firmware_version, device_model, mac_address, total_channels)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nvr.getLocationName());
            ps.setString(2, nvr.getIpAddress());
            ps.setInt(3, nvr.getPort());
            ps.setString(4, nvr.getUsername());
            ps.setString(5, nvr.getPassword());
            ps.setString(6, nvr.getBrand() != null ? nvr.getBrand().name() : NvrDevice.NvrBrand.ONVIF.name());
            ps.setString(7, nvr.getProtocol() != null ? nvr.getProtocol().name() : NvrDevice.NvrProtocol.ONVIF.name());
            ps.setString(8, nvr.getConnectionStatus().name());
            ps.setString(9, nvr.getFirmwareVersion());
            ps.setString(10, nvr.getDeviceModel());
            ps.setString(11, nvr.getMacAddress());
            ps.setInt(12, nvr.getTotalChannels());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) nvr.setId(rs.getLong(1));
            }
        } catch (SQLException e) {
            logger.error("Error inserting NVR: {}", e.getMessage());
            throw new RuntimeException("Failed to save NVR", e);
        }
        return nvr;
    }

    private NvrDevice update(NvrDevice nvr) {
        String sql = """
            UPDATE nvrs SET location_name=?, ip_address=?, port=?, username=?, password=?, brand=?,
                protocol=?, connection_status=?, firmware_version=?, device_model=?, mac_address=?,
                total_channels=?, last_connected=?
            WHERE id=?
        """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nvr.getLocationName());
            ps.setString(2, nvr.getIpAddress());
            ps.setInt(3, nvr.getPort());
            ps.setString(4, nvr.getUsername());
            ps.setString(5, nvr.getPassword());
            ps.setString(6, nvr.getBrand().name());
            ps.setString(7, nvr.getProtocol().name());
            ps.setString(8, nvr.getConnectionStatus().name());
            ps.setString(9, nvr.getFirmwareVersion());
            ps.setString(10, nvr.getDeviceModel());
            ps.setString(11, nvr.getMacAddress());
            ps.setInt(12, nvr.getTotalChannels());
            ps.setTimestamp(13, nvr.getLastConnected() != null ? Timestamp.valueOf(nvr.getLastConnected()) : null);
            ps.setLong(14, nvr.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating NVR: {}", e.getMessage());
            throw new RuntimeException("Failed to update NVR", e);
        }
        return nvr;
    }

    public List<NvrDevice> findAll() {
        List<NvrDevice> list = new ArrayList<>();
        String sql = "SELECT * FROM nvrs ORDER BY location_name";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all NVRs: {}", e.getMessage());
        }
        return list;
    }

    public Optional<NvrDevice> findById(Long id) {
        String sql = "SELECT * FROM nvrs WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding NVR by id: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public void delete(Long id) {
        String sql = "DELETE FROM nvrs WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting NVR: {}", e.getMessage());
            throw new RuntimeException("Failed to delete NVR", e);
        }
    }

    private NvrDevice mapRow(ResultSet rs) throws SQLException {
        NvrDevice nvr = new NvrDevice();
        nvr.setId(rs.getLong("id"));
        nvr.setLocationName(rs.getString("location_name"));
        nvr.setIpAddress(rs.getString("ip_address"));
        nvr.setPort(rs.getInt("port"));
        nvr.setUsername(rs.getString("username"));
        nvr.setPassword(rs.getString("password"));
        try { nvr.setBrand(NvrDevice.NvrBrand.valueOf(rs.getString("brand"))); } catch (Exception e) { nvr.setBrand(NvrDevice.NvrBrand.ONVIF); }
        try { nvr.setProtocol(NvrDevice.NvrProtocol.valueOf(rs.getString("protocol"))); } catch (Exception e) { nvr.setProtocol(NvrDevice.NvrProtocol.HTTP); }
        try { nvr.setConnectionStatus(NvrDevice.ConnectionStatus.valueOf(rs.getString("connection_status"))); } catch (Exception e) { nvr.setConnectionStatus(NvrDevice.ConnectionStatus.UNKNOWN); }
        nvr.setFirmwareVersion(rs.getString("firmware_version"));
        nvr.setDeviceModel(rs.getString("device_model"));
        nvr.setMacAddress(rs.getString("mac_address"));
        nvr.setTotalChannels(rs.getInt("total_channels"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) nvr.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp lastConn = rs.getTimestamp("last_connected");
        if (lastConn != null) nvr.setLastConnected(lastConn.toLocalDateTime());
        return nvr;
    }
}
