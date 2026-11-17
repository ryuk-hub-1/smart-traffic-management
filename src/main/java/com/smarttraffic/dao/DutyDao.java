package com.smarttraffic.dao;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.DutyAssignment;
import com.smarttraffic.model.enums.DutyStatus;
import com.smarttraffic.model.enums.ShiftType;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Officer Duty Rosters and Shift Allocations.
 */
public class DutyDao implements BaseDao<DutyAssignment, Integer> {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    @Override
    public Optional<DutyAssignment> findById(Integer id) throws DatabaseException {
        String sql = """
            SELECT d.*, 
                   u.full_name AS officer_name, o.badge_number,
                   j.name AS junction_name
            FROM duty_assignments d
            JOIN officers o ON d.officer_id = o.id
            JOIN users u ON o.user_id = u.id
            JOIN junctions j ON d.junction_id = j.id
            WHERE d.id = ?;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDuty(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding duty by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<DutyAssignment> findAll() throws DatabaseException {
        List<DutyAssignment> list = new ArrayList<>();
        String sql = """
            SELECT d.*, 
                   u.full_name AS officer_name, o.badge_number,
                   j.name AS junction_name
            FROM duty_assignments d
            JOIN officers o ON d.officer_id = o.id
            JOIN users u ON o.user_id = u.id
            JOIN junctions j ON d.junction_id = j.id
            ORDER BY d.duty_date DESC, d.start_time ASC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToDuty(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing duties: " + e.getMessage(), e);
        }
        return list;
    }

    public List<DutyAssignment> findByOfficerId(int officerId) throws DatabaseException {
        List<DutyAssignment> list = new ArrayList<>();
        String sql = """
            SELECT d.*, 
                   u.full_name AS officer_name, o.badge_number,
                   j.name AS junction_name
            FROM duty_assignments d
            JOIN officers o ON d.officer_id = o.id
            JOIN users u ON o.user_id = u.id
            JOIN junctions j ON d.junction_id = j.id
            WHERE d.officer_id = ?
            ORDER BY d.duty_date DESC, d.start_time ASC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, officerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDuty(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding officer duties: " + e.getMessage(), e);
        }
        return list;
    }

    public List<DutyAssignment> findByDate(LocalDate date) throws DatabaseException {
        List<DutyAssignment> list = new ArrayList<>();
        String sql = """
            SELECT d.*, 
                   u.full_name AS officer_name, o.badge_number,
                   j.name AS junction_name
            FROM duty_assignments d
            JOIN officers o ON d.officer_id = o.id
            JOIN users u ON o.user_id = u.id
            JOIN junctions j ON d.junction_id = j.id
            WHERE d.duty_date = ?
            ORDER BY d.start_time ASC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDuty(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding duties by date: " + e.getMessage(), e);
        }
        return list;
    }

    public Optional<DutyAssignment> findActiveDutyForOfficer(int officerId, LocalDate date) throws DatabaseException {
        String sql = """
            SELECT d.*, 
                   u.full_name AS officer_name, o.badge_number,
                   j.name AS junction_name
            FROM duty_assignments d
            JOIN officers o ON d.officer_id = o.id
            JOIN users u ON o.user_id = u.id
            JOIN junctions j ON d.junction_id = j.id
            WHERE d.officer_id = ? AND d.duty_date = ?
            ORDER BY d.id DESC LIMIT 1;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, officerId);
            pstmt.setString(2, date.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDuty(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding active duty: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public DutyAssignment save(DutyAssignment duty) throws DatabaseException {
        String sql = """
            INSERT INTO duty_assignments (officer_id, junction_id, shift, duty_date, start_time, end_time, status, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, duty.getOfficerId());
            pstmt.setInt(2, duty.getJunctionId());
            pstmt.setString(3, duty.getShift().name());
            pstmt.setString(4, duty.getDutyDate().toString());
            pstmt.setString(5, duty.getStartTime().toString());
            pstmt.setString(6, duty.getEndTime().toString());
            pstmt.setString(7, duty.getStatus().name());
            pstmt.setString(8, duty.getNotes());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    duty.setId(keys.getInt(1));
                    return duty;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving duty assignment: " + e.getMessage(), e);
        }
        throw new DatabaseException("Failed to save duty, no key generated.");
    }

    @Override
    public boolean update(DutyAssignment duty) throws DatabaseException {
        String sql = """
            UPDATE duty_assignments 
            SET officer_id = ?, junction_id = ?, shift = ?, duty_date = ?, 
                start_time = ?, end_time = ?, status = ?, notes = ?
            WHERE id = ?;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, duty.getOfficerId());
            pstmt.setInt(2, duty.getJunctionId());
            pstmt.setString(3, duty.getShift().name());
            pstmt.setString(4, duty.getDutyDate().toString());
            pstmt.setString(5, duty.getStartTime().toString());
            pstmt.setString(6, duty.getEndTime().toString());
            pstmt.setString(7, duty.getStatus().name());
            pstmt.setString(8, duty.getNotes());
            pstmt.setInt(9, duty.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating duty assignment: " + e.getMessage(), e);
        }
    }

    public boolean updateStatus(int dutyId, DutyStatus status) throws DatabaseException {
        String sql = "UPDATE duty_assignments SET status = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setInt(2, dutyId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating duty status: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Integer id) throws DatabaseException {
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE attendance SET duty_id = NULL WHERE duty_id = ?;")) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM duty_assignments WHERE id = ?;")) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Error deleting duty assignment: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage(), e);
        }
    }

    private DutyAssignment mapResultSetToDuty(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int officerId = rs.getInt("officer_id");
        int junctionId = rs.getInt("junction_id");
        ShiftType shift = ShiftType.valueOf(rs.getString("shift"));
        LocalDate dutyDate = LocalDate.parse(rs.getString("duty_date"));
        LocalTime startTime = LocalTime.parse(rs.getString("start_time"));
        LocalTime endTime = LocalTime.parse(rs.getString("end_time"));
        DutyStatus status = DutyStatus.valueOf(rs.getString("status"));
        String notes = rs.getString("notes");

        DutyAssignment d = new DutyAssignment(id, officerId, junctionId, shift, dutyDate, startTime, endTime, status, notes);
        d.setOfficerName(rs.getString("officer_name"));
        d.setBadgeNumber(rs.getString("badge_number"));
        d.setJunctionName(rs.getString("junction_name"));
        return d;
    }
}
