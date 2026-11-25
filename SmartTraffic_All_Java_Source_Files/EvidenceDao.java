package com.smarttraffic.dao;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.Evidence;
import com.smarttraffic.util.DateUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Photographic and Document Evidence.
 */
public class EvidenceDao implements BaseDao<Evidence, Integer> {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    @Override
    public Optional<Evidence> findById(Integer id) throws DatabaseException {
        String sql = "SELECT * FROM evidence WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEvidence(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding evidence: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Evidence> findByEntity(String entityType, int entityId) throws DatabaseException {
        List<Evidence> list = new ArrayList<>();
        String sql = "SELECT * FROM evidence WHERE entity_type = ? AND entity_id = ? ORDER BY uploaded_at DESC;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entityType);
            pstmt.setInt(2, entityId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToEvidence(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding evidence for entity: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Evidence> findAll() throws DatabaseException {
        List<Evidence> list = new ArrayList<>();
        String sql = "SELECT * FROM evidence ORDER BY uploaded_at DESC;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToEvidence(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing evidence: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Evidence save(Evidence ev) throws DatabaseException {
        String sql = """
            INSERT INTO evidence (entity_type, entity_id, file_path, file_name, file_size, description, uploaded_at)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, ev.getEntityType());
            pstmt.setInt(2, ev.getEntityId());
            pstmt.setString(3, ev.getFilePath());
            pstmt.setString(4, ev.getFileName());
            pstmt.setLong(5, ev.getFileSize());
            pstmt.setString(6, ev.getDescription());
            pstmt.setString(7, ev.getUploadedAt() != null ? ev.getUploadedAt().toString() : LocalDateTime.now().toString());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    ev.setId(keys.getInt(1));
                    return ev;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving evidence record: " + e.getMessage(), e);
        }
        throw new DatabaseException("Failed to save evidence.");
    }

    @Override
    public boolean update(Evidence ev) throws DatabaseException {
        String sql = "UPDATE evidence SET description = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ev.getDescription());
            pstmt.setInt(2, ev.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating evidence: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Integer id) throws DatabaseException {
        String sql = "DELETE FROM evidence WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting evidence: " + e.getMessage(), e);
        }
    }

    private Evidence mapResultSetToEvidence(ResultSet rs) throws SQLException {
        return new Evidence(
                rs.getInt("id"),
                rs.getString("entity_type"),
                rs.getInt("entity_id"),
                rs.getString("file_path"),
                rs.getString("file_name"),
                rs.getLong("file_size"),
                rs.getString("description"),
                DateUtil.parseDateTime(rs.getString("uploaded_at"))
        );
    }
}
