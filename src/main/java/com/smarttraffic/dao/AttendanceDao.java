package com.smarttraffic.dao;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.Attendance;
import com.smarttraffic.model.enums.AttendanceStatus;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Officer Attendance Tracking.
 */
public class AttendanceDao implements BaseDao<Attendance, Integer> {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public Optional<Attendance> findById(Integer id) throws DatabaseException {
        String sql = """
            SELECT a.*, 
                   u.full_name AS officer_name, o.badge_number
            FROM attendance a
            JOIN officers o ON a.officer_id = o.id
            JOIN users u ON o.user_id = u.id
            WHERE a.id = ?;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAttendance(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding attendance by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Attendance> findAll() throws DatabaseException {
        List<Attendance> list = new ArrayList<>();
        String sql = """
            SELECT a.*, 
                   u.full_name AS officer_name, o.badge_number
            FROM attendance a
            JOIN officers o ON a.officer_id = o.id
            JOIN users u ON o.user_id = u.id
            ORDER BY a.attendance_date DESC, a.check_in_time DESC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToAttendance(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching attendance records: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Attendance> findByOfficerId(int officerId) throws DatabaseException {
        List<Attendance> list = new ArrayList<>();
        String sql = """
            SELECT a.*, 
                   u.full_name AS officer_name, o.badge_number
            FROM attendance a
            JOIN officers o ON a.officer_id = o.id
            JOIN users u ON o.user_id = u.id
            WHERE a.officer_id = ?
            ORDER BY a.attendance_date DESC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, officerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAttendance(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching officer attendance: " + e.getMessage(), e);
        }
        return list;
    }

    public Optional<Attendance> findTodayAttendanceForOfficer(int officerId, LocalDate date) throws DatabaseException {
        String sql = """
            SELECT a.*, 
                   u.full_name AS officer_name, o.badge_number
            FROM attendance a
            JOIN officers o ON a.officer_id = o.id
            JOIN users u ON o.user_id = u.id
            WHERE a.officer_id = ? AND a.attendance_date = ?
            ORDER BY a.id DESC LIMIT 1;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, officerId);
            pstmt.setString(2, date.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAttendance(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding today's attendance: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Attendance checkIn(int officerId, Integer dutyId, LocalTime checkInTime, String remarks) throws DatabaseException {
        String sql = """
            INSERT INTO attendance (officer_id, duty_id, attendance_date, check_in_time, status, remarks)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, officerId);
            if (dutyId != null && dutyId > 0) {
                pstmt.setInt(2, dutyId);
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setString(3, LocalDate.now().toString());
            pstmt.setString(4, checkInTime.format(TIME_FMT));
            pstmt.setString(5, AttendanceStatus.ON_DUTY.name());
            pstmt.setString(6, remarks != null ? remarks : "Officer Checked In");
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    return findById(id).orElseThrow(() -> new DatabaseException("Check-in record created but not found."));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to register check-in: " + e.getMessage(), e);
        }
        throw new DatabaseException("Failed to register check-in.");
    }

    public boolean checkOut(int attendanceId, LocalTime checkOutTime) throws DatabaseException {
        String sql = "UPDATE attendance SET check_out_time = ?, status = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, checkOutTime.format(TIME_FMT));
            pstmt.setString(2, AttendanceStatus.PRESENT.name());
            pstmt.setInt(3, attendanceId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to register check-out: " + e.getMessage(), e);
        }
    }

    @Override
    public Attendance save(Attendance entity) throws DatabaseException {
        String sql = """
            INSERT INTO attendance (officer_id, duty_id, attendance_date, check_in_time, check_out_time, status, remarks)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, entity.getOfficerId());
            if (entity.getDutyId() != null && entity.getDutyId() > 0) {
                pstmt.setInt(2, entity.getDutyId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setString(3, entity.getAttendanceDate() != null ? entity.getAttendanceDate().toString() : LocalDate.now().toString());
            pstmt.setString(4, entity.getCheckInTime() != null ? entity.getCheckInTime().format(TIME_FMT) : null);
            pstmt.setString(5, entity.getCheckOutTime() != null ? entity.getCheckOutTime().format(TIME_FMT) : null);
            pstmt.setString(6, entity.getStatus() != null ? entity.getStatus().name() : AttendanceStatus.PRESENT.name());
            pstmt.setString(7, entity.getRemarks());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                    return findById(entity.getId()).orElse(entity);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save attendance record: " + e.getMessage(), e);
        }
        return entity;
    }

    @Override
    public boolean update(Attendance entity) throws DatabaseException {
        String sql = "UPDATE attendance SET status = ?, remarks = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entity.getStatus().name());
            pstmt.setString(2, entity.getRemarks());
            pstmt.setInt(3, entity.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update attendance: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Integer id) throws DatabaseException {
        String sql = "DELETE FROM attendance WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete attendance: " + e.getMessage(), e);
        }
    }

    private Attendance mapResultSetToAttendance(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int officerId = rs.getInt("officer_id");
        int dId = rs.getInt("duty_id");
        Integer dutyId = rs.wasNull() ? null : dId;
        LocalDate attDate = LocalDate.parse(rs.getString("attendance_date"));

        String inStr = rs.getString("check_in_time");
        LocalTime inTime = inStr != null && !inStr.isEmpty() ? LocalTime.parse(inStr) : null;

        String outStr = rs.getString("check_out_time");
        LocalTime outTime = outStr != null && !outStr.isEmpty() ? LocalTime.parse(outStr) : null;

        String statusStr = rs.getString("status");
        AttendanceStatus status = AttendanceStatus.PRESENT;
        if (statusStr != null) {
            try {
                status = AttendanceStatus.valueOf(statusStr);
            } catch (Exception ignored) {
            }
        }
        String remarks = rs.getString("remarks");

        Attendance a = new Attendance(id, officerId, dutyId, attDate, inTime, outTime, status, remarks);
        a.setOfficerName(rs.getString("officer_name"));
        a.setBadgeNumber(rs.getString("badge_number"));
        return a;
    }
}
