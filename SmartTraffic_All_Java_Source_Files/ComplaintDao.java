package com.smarttraffic.dao;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.Complaint;
import com.smarttraffic.model.enums.ComplaintStatus;
import com.smarttraffic.util.DateUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Citizen Complaints and Road Reports.
 */
public class ComplaintDao implements BaseDao<Complaint, Integer> {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public synchronized String generateNextComplaintNumber() throws DatabaseException {
        int year = LocalDateTime.now().getYear();
        String prefix = String.format("CMP-%d-", year);
        String sql = "SELECT complaint_number FROM complaints WHERE complaint_number LIKE ?;";
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
            throw new DatabaseException("Error generating complaint number: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Complaint> findById(Integer id) throws DatabaseException {
        String sql = """
            SELECT c.*, 
                   cu.full_name AS citizen_name, cu.phone AS citizen_phone,
                   j.name AS junction_name,
                   ou.full_name AS officer_name
            FROM complaints c
            JOIN users cu ON c.citizen_id = cu.id
            LEFT JOIN junctions j ON c.junction_id = j.id
            LEFT JOIN officers o ON c.assigned_officer_id = o.id
            LEFT JOIN users ou ON o.user_id = ou.id
            WHERE c.id = ?;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToComplaint(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding complaint by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Complaint> findAll() throws DatabaseException {
        List<Complaint> list = new ArrayList<>();
        String sql = """
            SELECT c.*, 
                   cu.full_name AS citizen_name, cu.phone AS citizen_phone,
                   j.name AS junction_name,
                   ou.full_name AS officer_name
            FROM complaints c
            JOIN users cu ON c.citizen_id = cu.id
            LEFT JOIN junctions j ON c.junction_id = j.id
            LEFT JOIN officers o ON c.assigned_officer_id = o.id
            LEFT JOIN users ou ON o.user_id = ou.id
            ORDER BY c.created_at DESC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToComplaint(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing complaints: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Complaint> findByCitizenId(int citizenId) throws DatabaseException {
        List<Complaint> list = new ArrayList<>();
        String sql = """
            SELECT c.*, 
                   cu.full_name AS citizen_name, cu.phone AS citizen_phone,
                   j.name AS junction_name,
                   ou.full_name AS officer_name
            FROM complaints c
            JOIN users cu ON c.citizen_id = cu.id
            LEFT JOIN junctions j ON c.junction_id = j.id
            LEFT JOIN officers o ON c.assigned_officer_id = o.id
            LEFT JOIN users ou ON o.user_id = ou.id
            WHERE c.citizen_id = ?
            ORDER BY c.created_at DESC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, citizenId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToComplaint(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding citizen complaints: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Complaint> findByAssignedOfficerId(int officerId) throws DatabaseException {
        List<Complaint> list = new ArrayList<>();
        String sql = """
            SELECT c.*, 
                   cu.full_name AS citizen_name, cu.phone AS citizen_phone,
                   j.name AS junction_name,
                   ou.full_name AS officer_name
            FROM complaints c
            JOIN users cu ON c.citizen_id = cu.id
            LEFT JOIN junctions j ON c.junction_id = j.id
            LEFT JOIN officers o ON c.assigned_officer_id = o.id
            LEFT JOIN users ou ON o.user_id = ou.id
            WHERE c.assigned_officer_id = ?
            ORDER BY c.created_at DESC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, officerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToComplaint(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding officer complaints: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Complaint save(Complaint comp) throws DatabaseException {
        if (comp.getComplaintNumber() == null || comp.getComplaintNumber().isEmpty()) {
            comp.setComplaintNumber(generateNextComplaintNumber());
        }

        String sql = """
            INSERT INTO complaints (complaint_number, citizen_id, junction_id, complaint_type,
                                    vehicle_number, description, incident_date, status,
                                    assigned_officer_id, officer_action_notes, evidence_file_path,
                                    created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, comp.getComplaintNumber());
            pstmt.setInt(2, comp.getCitizenId());
            if (comp.getJunctionId() != null && comp.getJunctionId() > 0) {
                pstmt.setInt(3, comp.getJunctionId());
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            pstmt.setString(4, comp.getComplaintType());
            pstmt.setString(5, comp.getVehicleNumber());
            pstmt.setString(6, comp.getDescription());
            pstmt.setString(7, comp.getIncidentDate() != null ? comp.getIncidentDate().toString() : LocalDateTime.now().toString());
            pstmt.setString(8, comp.getStatus() != null ? comp.getStatus().name() : ComplaintStatus.PENDING.name());
            if (comp.getAssignedOfficerId() != null && comp.getAssignedOfficerId() > 0) {
                pstmt.setInt(9, comp.getAssignedOfficerId());
            } else {
                pstmt.setNull(9, Types.INTEGER);
            }
            pstmt.setString(10, comp.getOfficerActionNotes());
            pstmt.setString(11, comp.getEvidenceFilePath());
            pstmt.setString(12, comp.getCreatedAt() != null ? comp.getCreatedAt().toString() : LocalDateTime.now().toString());
            pstmt.setString(13, LocalDateTime.now().toString());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    comp.setId(keys.getInt(1));
                    return findById(comp.getId()).orElse(comp);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving complaint: " + e.getMessage(), e);
        }
        throw new DatabaseException("Failed to save complaint.");
    }

    public boolean updateStatusAndAction(int complaintId, ComplaintStatus status,
                                         Integer assignedOfficerId, String notes) throws DatabaseException {
        String sql = """
            UPDATE complaints 
            SET status = ?, assigned_officer_id = ?, officer_action_notes = ?, updated_at = ?
            WHERE id = ?;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            if (assignedOfficerId != null && assignedOfficerId > 0) {
                pstmt.setInt(2, assignedOfficerId);
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setString(3, notes);
            pstmt.setString(4, LocalDateTime.now().toString());
            pstmt.setInt(5, complaintId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating complaint resolution: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Complaint entity) throws DatabaseException {
        return updateStatusAndAction(entity.getId(), entity.getStatus(), entity.getAssignedOfficerId(), entity.getOfficerActionNotes());
    }

    @Override
    public boolean deleteById(Integer id) throws DatabaseException {
        String sql = "DELETE FROM complaints WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting complaint: " + e.getMessage(), e);
        }
    }

    private Complaint mapResultSetToComplaint(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String cNum = rs.getString("complaint_number");
        int citizenId = rs.getInt("citizen_id");
        int jId = rs.getInt("junction_id");
        Integer junctionId = rs.wasNull() ? null : jId;
        String type = rs.getString("complaint_type");
        String vNum = rs.getString("vehicle_number");
        String desc = rs.getString("description");
        LocalDateTime incDate = DateUtil.parseDateTime(rs.getString("incident_date"));
        ComplaintStatus status = ComplaintStatus.valueOf(rs.getString("status"));
        int oId = rs.getInt("assigned_officer_id");
        Integer officerId = rs.wasNull() ? null : oId;
        String notes = rs.getString("officer_action_notes");
        String evPath = rs.getString("evidence_file_path");
        LocalDateTime created = DateUtil.parseDateTime(rs.getString("created_at"));
        LocalDateTime updated = DateUtil.parseDateTime(rs.getString("updated_at"));

        Complaint c = new Complaint(id, cNum, citizenId, junctionId, type, vNum, desc, incDate, status, officerId, notes, evPath, created, updated);
        c.setCitizenName(rs.getString("citizen_name"));
        c.setCitizenPhone(rs.getString("citizen_phone"));
        c.setJunctionName(rs.getString("junction_name"));
        c.setAssignedOfficerName(rs.getString("officer_name"));
        return c;
    }
}
