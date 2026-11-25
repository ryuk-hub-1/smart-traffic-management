package com.smarttraffic.dao;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.AdminUser;
import com.smarttraffic.model.Citizen;
import com.smarttraffic.model.Officer;
import com.smarttraffic.model.User;
import com.smarttraffic.model.enums.Role;
import com.smarttraffic.util.DateUtil;
import com.smarttraffic.util.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for User Authentication and Profiles.
 */
public class UserDao implements BaseDao<User, Integer> {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public Optional<User> authenticate(String username, String password) throws DatabaseException {
        String sql = """
            SELECT u.*, 
                   o.id AS officer_id, o.badge_number, o.rank, o.police_station, o.status AS officer_status,
                   o.assigned_junction_id, j.name AS junction_name,
                   c.dl_number, c.vehicle_number, c.address
            FROM users u
            LEFT JOIN officers o ON u.id = o.user_id
            LEFT JOIN junctions j ON o.assigned_junction_id = j.id
            LEFT JOIN citizen_profiles c ON u.id = c.user_id
            WHERE u.username = ? COLLATE NOCASE;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (PasswordUtil.verifyPassword(password, storedHash)) {
                        return Optional.of(mapResultSetToUser(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error during user authentication: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) throws DatabaseException {
        String sql = """
            SELECT u.*, 
                   o.id AS officer_id, o.badge_number, o.rank, o.police_station, o.status AS officer_status,
                   o.assigned_junction_id, j.name AS junction_name,
                   c.dl_number, c.vehicle_number, c.address
            FROM users u
            LEFT JOIN officers o ON u.id = o.user_id
            LEFT JOIN junctions j ON o.assigned_junction_id = j.id
            LEFT JOIN citizen_profiles c ON u.id = c.user_id
            WHERE u.username = ? COLLATE NOCASE;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by username: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(Integer id) throws DatabaseException {
        String sql = """
            SELECT u.*, 
                   o.id AS officer_id, o.badge_number, o.rank, o.police_station, o.status AS officer_status,
                   o.assigned_junction_id, j.name AS junction_name,
                   c.dl_number, c.vehicle_number, c.address
            FROM users u
            LEFT JOIN officers o ON u.id = o.user_id
            LEFT JOIN junctions j ON o.assigned_junction_id = j.id
            LEFT JOIN citizen_profiles c ON u.id = c.user_id
            WHERE u.id = ?;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() throws DatabaseException {
        List<User> list = new ArrayList<>();
        String sql = """
            SELECT u.*, 
                   o.id AS officer_id, o.badge_number, o.rank, o.police_station, o.status AS officer_status,
                   o.assigned_junction_id, j.name AS junction_name,
                   c.dl_number, c.vehicle_number, c.address
            FROM users u
            LEFT JOIN officers o ON u.id = o.user_id
            LEFT JOIN junctions j ON o.assigned_junction_id = j.id
            LEFT JOIN citizen_profiles c ON u.id = c.user_id
            ORDER BY u.id ASC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching users: " + e.getMessage(), e);
        }
        return list;
    }

    public Citizen registerCitizen(String username, String plainPassword, String fullName,
                                   String email, String phone, String dlNumber,
                                   String vehicleNumber, String address) throws DatabaseException {
        String insertUserSql = "INSERT INTO users (username, password_hash, role, full_name, email, phone, created_at) VALUES (?, ?, ?, ?, ?, ?, ?);";
        String insertProfileSql = "INSERT INTO citizen_profiles (user_id, dl_number, vehicle_number, address) VALUES (?, ?, ?, ?);";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int userId;
                String now = LocalDateTime.now().toString();
                String hash = PasswordUtil.hashPassword(plainPassword);

                try (PreparedStatement pstmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, hash);
                    pstmt.setString(3, "CITIZEN");
                    pstmt.setString(4, fullName);
                    pstmt.setString(5, email);
                    pstmt.setString(6, phone);
                    pstmt.setString(7, now);
                    pstmt.executeUpdate();

                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            userId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Failed to retrieve generated user ID.");
                        }
                    }
                }

                try (PreparedStatement pstmt = conn.prepareStatement(insertProfileSql)) {
                    pstmt.setInt(1, userId);
                    pstmt.setString(2, dlNumber != null ? dlNumber.trim().toUpperCase() : "");
                    pstmt.setString(3, vehicleNumber != null ? vehicleNumber.trim().toUpperCase() : "");
                    pstmt.setString(4, address != null ? address.trim() : "");
                    pstmt.executeUpdate();
                }

                conn.commit();
                return new Citizen(userId, username, hash, fullName, email, phone,
                        LocalDateTime.now(), dlNumber, vehicleNumber, address);
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Failed to register citizen account: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database connection error: " + e.getMessage(), e);
        }
    }

    @Override
    public User save(User entity) throws DatabaseException {
        throw new UnsupportedOperationException("Use specific registration or officer save methods.");
    }

    @Override
    public boolean update(User entity) throws DatabaseException {
        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entity.getFullName());
            pstmt.setString(2, entity.getEmail());
            pstmt.setString(3, entity.getPhone());
            pstmt.setInt(4, entity.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating user: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Integer id) throws DatabaseException {
        String sql = "DELETE FROM users WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting user: " + e.getMessage(), e);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        Role role = Role.valueOf(rs.getString("role"));
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        LocalDateTime createdAt = DateUtil.parseDateTime(rs.getString("created_at"));

        if (role == Role.ADMIN) {
            return new AdminUser(id, username, passwordHash, fullName, email, phone, createdAt,
                    "Traffic Headquarters Operations", "Clearance Level 5 (Chief)");
        } else if (role == Role.OFFICER) {
            int officerId = rs.getInt("officer_id");
            String badgeNumber = rs.getString("badge_number");
            String rank = rs.getString("rank");
            String policeStation = rs.getString("police_station");
            String status = rs.getString("officer_status");
            int jId = rs.getInt("assigned_junction_id");
            Integer junctionId = rs.wasNull() ? null : jId;
            String junctionName = rs.getString("junction_name");
            return new Officer(id, username, passwordHash, fullName, email, phone, createdAt,
                    officerId, badgeNumber, rank, policeStation, status, junctionId, junctionName);
        } else {
            String dl = rs.getString("dl_number");
            String vehicle = rs.getString("vehicle_number");
            String addr = rs.getString("address");
            return new Citizen(id, username, passwordHash, fullName, email, phone, createdAt,
                    dl, vehicle, addr);
        }
    }
}
