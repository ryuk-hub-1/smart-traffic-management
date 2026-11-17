package com.smarttraffic.dao;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.EmergencyReport;
import com.smarttraffic.model.enums.EmergencySeverity;
import com.smarttraffic.model.enums.EmergencyStatus;
import com.smarttraffic.util.DateUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Accidents and Traffic Emergencies.
 */
public class EmergencyDao implements BaseDao<EmergencyReport, Integer> {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public synchronized String generateNextReportNumber() throws DatabaseException {
        int year = LocalDateTime.now().getYear();
        String prefix = String.format("EMG-%d-", year);
        String sql = "SELECT report_number FROM emergency_reports WHERE report_number LIKE ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, prefix + "%");
            int maxSeq = 0;
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String num = rs.getString(1);
                    if (num != null && num.startsWith(prefix)) {
                        try {
                            int seq = Integer.parseInt(num.substring(prefix.length()));
                            if (seq > maxSeq) {
                                maxSeq = seq;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            return String.format("%s%04d", prefix, maxSeq + 1);
        } catch (SQLException e) {
            throw new DatabaseException("Error generating emergency report number: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<EmergencyReport> findById(Integer id) throws DatabaseException {
        String sql = """
            SELECT e.*, 
                   u.full_name AS officer_name,
                   j.name AS junction_name
            FROM emergency_reports e
            LEFT JOIN officers o ON e.officer_id = o.id
            LEFT JOIN users u ON o.user_id = u.id
            JOIN junctions j ON e.junction_id = j.id
            WHERE e.id = ?;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEmergency(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding emergency report: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<EmergencyReport> findAll() throws DatabaseException {
        List<EmergencyReport> list = new ArrayList<>();
        String sql = """
            SELECT e.*, 
                   u.full_name AS officer_name,
                   j.name AS junction_name
            FROM emergency_reports e
            LEFT JOIN officers o ON e.officer_id = o.id
            LEFT JOIN users u ON o.user_id = u.id
            JOIN junctions j ON e.junction_id = j.id
            ORDER BY e.report_time DESC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToEmergency(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing emergency reports: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public EmergencyReport save(EmergencyReport e) throws DatabaseException {
        if (e.getReportNumber() == null || e.getReportNumber().isEmpty()) {
            e.setReportNumber(generateNextReportNumber());
        }

        String sql = """
            INSERT INTO emergency_reports (report_number, officer_id, junction_id, accident_type,
                                          severity, vehicles_involved, casualties_count,
                                          ambulance_required, tow_truck_required, road_blocked,
                                          report_time, status, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, e.getReportNumber());
            if (e.getOfficerId() != null && e.getOfficerId() > 0) {
                pstmt.setInt(2, e.getOfficerId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setInt(3, e.getJunctionId());
            pstmt.setString(4, e.getAccidentType());
            pstmt.setString(5, e.getSeverity().name());
            pstmt.setInt(6, e.getVehiclesInvolved());
            pstmt.setInt(7, e.getCasualtiesCount());
            pstmt.setInt(8, e.isAmbulanceRequired() ? 1 : 0);
            pstmt.setInt(9, e.isTowTruckRequired() ? 1 : 0);
            pstmt.setInt(10, e.isRoadBlocked() ? 1 : 0);
            pstmt.setString(11, e.getReportTime() != null ? e.getReportTime().toString() : LocalDateTime.now().toString());
            pstmt.setString(12, e.getStatus().name());
            pstmt.setString(13, e.getNotes());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    e.setId(keys.getInt(1));
                    return findById(e.getId()).orElse(e);
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Error saving emergency report: " + ex.getMessage(), ex);
        }
        throw new DatabaseException("Failed to save emergency report.");
    }

    public boolean updateStatus(int reportId, EmergencyStatus status, String notes) throws DatabaseException {
        String sql = "UPDATE emergency_reports SET status = ?, notes = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, notes);
            pstmt.setInt(3, reportId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating emergency status: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(EmergencyReport entity) throws DatabaseException {
        return updateStatus(entity.getId(), entity.getStatus(), entity.getNotes());
    }

    @Override
    public boolean deleteById(Integer id) throws DatabaseException {
        String sql = "DELETE FROM emergency_reports WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting emergency report: " + e.getMessage(), e);
        }
    }

    private EmergencyReport mapResultSetToEmergency(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String rNum = rs.getString("report_number");
        int oId = rs.getInt("officer_id");
        Integer officerId = rs.wasNull() ? null : oId;
        int jId = rs.getInt("junction_id");
        String type = rs.getString("accident_type");
        EmergencySeverity severity = EmergencySeverity.valueOf(rs.getString("severity"));
        int vehicles = rs.getInt("vehicles_involved");
        int casualties = rs.getInt("casualties_count");
        boolean amb = rs.getInt("ambulance_required") == 1;
        boolean tow = rs.getInt("tow_truck_required") == 1;
        boolean roadBlocked = rs.getInt("road_blocked") == 1;
        LocalDateTime time = DateUtil.parseDateTime(rs.getString("report_time"));
        EmergencyStatus status = EmergencyStatus.valueOf(rs.getString("status"));
        String notes = rs.getString("notes");

        EmergencyReport e = new EmergencyReport(id, rNum, officerId, jId, type, severity, vehicles, casualties, amb, tow, roadBlocked, time, status, notes);
        e.setOfficerName(rs.getString("officer_name") != null ? rs.getString("officer_name") : "Central Control Room");
        e.setJunctionName(rs.getString("junction_name"));
        return e;
    }
}
