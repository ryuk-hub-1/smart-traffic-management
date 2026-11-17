package com.smarttraffic.dao;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.Officer;
import com.smarttraffic.util.DateUtil;
import com.smarttraffic.util.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Officer Management.
 */
public class OfficerDao implements BaseDao<Officer, Integer> {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    @Override
    public Optional<Officer> findById(Integer officerId) throws DatabaseException {
        String sql = """
            SELECT o.id AS officer_id, o.badge_number, o.rank, o.police_station, o.status AS officer_status,
                   o.assigned_junction_id, j.name AS junction_name,
                   u.id AS user_id, u.username, u.password_hash, u.full_name, u.email, u.phone, u.created_at
            FROM officers o
            JOIN users u ON o.user_id = u.id
            LEFT JOIN junctions j ON o.assigned_junction_id = j.id
            WHERE o.id = ?;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, officerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToOfficer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding officer by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<Officer> findByUserId(int userId) throws DatabaseException {
        String sql = """
            SELECT o.id AS officer_id, o.badge_number, o.rank, o.police_station, o.status AS officer_status,
                   o.assigned_junction_id, j.name AS junction_name,
                   u.id AS user_id, u.username, u.password_hash, u.full_name, u.email, u.phone, u.created_at
            FROM officers o
            JOIN users u ON o.user_id = u.id
            LEFT JOIN junctions j ON o.assigned_junction_id = j.id
            WHERE o.user_id = ?;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToOfficer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding officer by User ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<Officer> findByBadge(String badgeNumber) throws DatabaseException {
        String sql = """
            SELECT o.id AS officer_id, o.badge_number, o.rank, o.police_station, o.status AS officer_status,
                   o.assigned_junction_id, j.name AS junction_name,
                   u.id AS user_id, u.username, u.password_hash, u.full_name, u.email, u.phone, u.created_at
            FROM officers o
            JOIN users u ON o.user_id = u.id
            LEFT JOIN junctions j ON o.assigned_junction_id = j.id
            WHERE o.badge_number = ? COLLATE NOCASE;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, badgeNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToOfficer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding officer by Badge: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Officer> findAll() throws DatabaseException {
        List<Officer> list = new ArrayList<>();
        String sql = """
            SELECT o.id AS officer_id, o.badge_number, o.rank, o.police_station, o.status AS officer_status,
                   o.assigned_junction_id, j.name AS junction_name,
                   u.id AS user_id, u.username, u.password_hash, u.full_name, u.email, u.phone, u.created_at
            FROM officers o
            JOIN users u ON o.user_id = u.id
            LEFT JOIN junctions j ON o.assigned_junction_id = j.id
            ORDER BY o.id ASC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToOfficer(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing all officers: " + e.getMessage(), e);
        }
        return list;
    }

    public Officer createOfficer(String username, String rawPassword, String fullName,
                                 String email, String phone, String badgeNumber,
                                 String rank, String policeStation, Integer assignedJunctionId) throws DatabaseException {
        String userSql = "INSERT INTO users (username, password_hash, role, full_name, email, phone, created_at) VALUES (?, ?, ?, ?, ?, ?, ?);";
        String officerSql = "INSERT INTO officers (user_id, badge_number, rank, police_station, status, assigned_junction_id) VALUES (?, ?, ?, ?, ?, ?);";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int userId;
                String now = LocalDateTime.now().toString();
                String hash = PasswordUtil.hashPassword(rawPassword);

                try (PreparedStatement pstmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, hash);
                    pstmt.setString(3, "OFFICER");
                    pstmt.setString(4, fullName);
                    pstmt.setString(5, email);
                    pstmt.setString(6, phone);
                    pstmt.setString(7, now);
                    pstmt.executeUpdate();

                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            userId = keys.getInt(1);
                        } else {
                            throw new SQLException("Could not retrieve generated user id");
                        }
                    }
                }

                int officerId;
                try (PreparedStatement pstmt = conn.prepareStatement(officerSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, userId);
                    pstmt.setString(2, badgeNumber.trim().toUpperCase());
                    pstmt.setString(3, rank);
                    pstmt.setString(4, policeStation);
                    pstmt.setString(5, "ACTIVE");
                    if (assignedJunctionId != null && assignedJunctionId > 0) {
                        pstmt.setInt(6, assignedJunctionId);
                    } else {
                        pstmt.setNull(6, Types.INTEGER);
                    }
                    pstmt.executeUpdate();

                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            officerId = keys.getInt(1);
                        } else {
                            throw new SQLException("Could not retrieve generated officer id");
                        }
                    }
                }

                conn.commit();
                return findById(officerId).orElseThrow(() -> new DatabaseException("Officer created but could not be re-fetched."));
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Failed to create officer: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database connection error: " + e.getMessage(), e);
        }
    }

    @Override
    public Officer save(Officer officer) throws DatabaseException {
        throw new UnsupportedOperationException("Use createOfficer(...) to create officer with user login.");
    }

    @Override
    public boolean update(Officer officer) throws DatabaseException {
        String updateOfficerSql = "UPDATE officers SET badge_number = ?, rank = ?, police_station = ?, status = ?, assigned_junction_id = ? WHERE id = ?;";
        String updateUserSql = "UPDATE users SET full_name = ?, email = ?, phone = ? WHERE id = ?;";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt = conn.prepareStatement(updateOfficerSql)) {
                    pstmt.setString(1, officer.getBadgeNumber().trim().toUpperCase());
                    pstmt.setString(2, officer.getRank());
                    pstmt.setString(3, officer.getPoliceStation());
                    pstmt.setString(4, officer.getStatus());
                    if (officer.getAssignedJunctionId() != null && officer.getAssignedJunctionId() > 0) {
                        pstmt.setInt(5, officer.getAssignedJunctionId());
                    } else {
                        pstmt.setNull(5, Types.INTEGER);
                    }
                    pstmt.setInt(6, officer.getOfficerId());
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement(updateUserSql)) {
                    pstmt.setString(1, officer.getFullName().trim());
                    pstmt.setString(2, officer.getEmail() != null ? officer.getEmail().trim() : "");
                    pstmt.setString(3, officer.getPhone() != null ? officer.getPhone().trim() : "");
                    pstmt.setInt(4, officer.getId());
                    pstmt.executeUpdate();
                }

                if (officer.getPasswordHash() != null && !officer.getPasswordHash().isEmpty()) {
                    String pwdSql = "UPDATE users SET password_hash = ? WHERE id = ?;";
                    try (PreparedStatement pstmt = conn.prepareStatement(pwdSql)) {
                        pstmt.setString(1, officer.getPasswordHash());
                        pstmt.setInt(2, officer.getId());
                        pstmt.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Failed to update officer details: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage(), e);
        }
    }

    public boolean assignJunction(int officerId, Integer junctionId) throws DatabaseException {
        String sql = "UPDATE officers SET assigned_junction_id = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (junctionId != null && junctionId > 0) {
                pstmt.setInt(1, junctionId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setInt(2, officerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error assigning junction: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Integer officerId) throws DatabaseException {
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int userId = -1;
                String getUserIdSql = "SELECT user_id FROM officers WHERE id = ?;";
                try (PreparedStatement pstmt = conn.prepareStatement(getUserIdSql)) {
                    pstmt.setInt(1, officerId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            userId = rs.getInt("user_id");
                        }
                    }
                }

                if (userId == -1) {
                    conn.rollback();
                    return false;
                }

                // 1. Delete challans issued by this officer so foreign key restrict doesn't block
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM challans WHERE officer_id = ?;")) {
                    pstmt.setInt(1, officerId);
                    pstmt.executeUpdate();
                }

                // 2. Delete attendance records of this officer
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM attendance WHERE officer_id = ?;")) {
                    pstmt.setInt(1, officerId);
                    pstmt.executeUpdate();
                }

                // 3. Delete duty assignments of this officer
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM duty_assignments WHERE officer_id = ?;")) {
                    pstmt.setInt(1, officerId);
                    pstmt.executeUpdate();
                }

                // 4. Detach complaints assigned to this officer
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE complaints SET assigned_officer_id = NULL WHERE assigned_officer_id = ?;")) {
                    pstmt.setInt(1, officerId);
                    pstmt.executeUpdate();
                }

                // 5. Detach emergency reports assigned to this officer
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE emergency_reports SET officer_id = NULL WHERE officer_id = ?;")) {
                    pstmt.setInt(1, officerId);
                    pstmt.executeUpdate();
                }

                // 6. Delete officer record
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM officers WHERE id = ?;")) {
                    pstmt.setInt(1, officerId);
                    pstmt.executeUpdate();
                }

                // 7. Delete user account
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE id = ?;")) {
                    pstmt.setInt(1, userId);
                    pstmt.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Failed to delete officer: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage(), e);
        }
    }

    private Officer mapResultSetToOfficer(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        LocalDateTime createdAt = DateUtil.parseDateTime(rs.getString("created_at"));

        int officerId = rs.getInt("officer_id");
        String badgeNumber = rs.getString("badge_number");
        String rank = rs.getString("rank");
        String policeStation = rs.getString("police_station");
        String status = rs.getString("officer_status");
        int jId = rs.getInt("assigned_junction_id");
        Integer assignedJunctionId = rs.wasNull() ? null : jId;
        String assignedJunctionName = rs.getString("junction_name");

        return new Officer(userId, username, passwordHash, fullName, email, phone, createdAt,
                officerId, badgeNumber, rank, policeStation, status, assignedJunctionId, assignedJunctionName);
    }
}
