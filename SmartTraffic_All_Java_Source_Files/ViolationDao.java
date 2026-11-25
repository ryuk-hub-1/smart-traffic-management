package com.smarttraffic.dao;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.ViolationType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Violation Types catalog.
 */
public class ViolationDao implements BaseDao<ViolationType, Integer> {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    @Override
    public Optional<ViolationType> findById(Integer id) throws DatabaseException {
        String sql = "SELECT * FROM violations WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToViolation(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding violation type: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<ViolationType> findAll() throws DatabaseException {
        List<ViolationType> list = new ArrayList<>();
        String sql = "SELECT * FROM violations ORDER BY title ASC;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToViolation(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing violation types: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public ViolationType save(ViolationType v) throws DatabaseException {
        String sql = "INSERT INTO violations (violation_code, title, category, description, standard_fine_amount) VALUES (?, ?, ?, ?, ?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, v.getViolationCode());
            pstmt.setString(2, v.getTitle());
            pstmt.setString(3, v.getCategory());
            pstmt.setString(4, v.getDescription());
            pstmt.setDouble(5, v.getStandardFineAmount());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    v.setId(keys.getInt(1));
                    return v;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving violation type: " + e.getMessage(), e);
        }
        throw new DatabaseException("Failed to save violation type.");
    }

    @Override
    public boolean update(ViolationType v) throws DatabaseException {
        String sql = "UPDATE violations SET title = ?, category = ?, description = ?, standard_fine_amount = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, v.getTitle());
            pstmt.setString(2, v.getCategory());
            pstmt.setString(3, v.getDescription());
            pstmt.setDouble(4, v.getStandardFineAmount());
            pstmt.setInt(5, v.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating violation type: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Integer id) throws DatabaseException {
        String sql = "DELETE FROM violations WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting violation type: " + e.getMessage(), e);
        }
    }

    private ViolationType mapResultSetToViolation(ResultSet rs) throws SQLException {
        return new ViolationType(
                rs.getInt("id"),
                rs.getString("violation_code"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getDouble("standard_fine_amount")
        );
    }
}
