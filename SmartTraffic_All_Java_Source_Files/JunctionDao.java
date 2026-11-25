package com.smarttraffic.dao;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.Junction;
import com.smarttraffic.model.enums.RiskLevel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Traffic Junctions / Intersections.
 */
public class JunctionDao implements BaseDao<Junction, Integer> {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    @Override
    public Optional<Junction> findById(Integer id) throws DatabaseException {
        String sql = """
            SELECT j.*, 
                   (SELECT COUNT(*) FROM officers o WHERE o.assigned_junction_id = j.id AND o.status = 'ACTIVE') AS officer_count
            FROM junctions j
            WHERE j.id = ?;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToJunction(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding junction by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Junction> findAll() throws DatabaseException {
        List<Junction> list = new ArrayList<>();
        String sql = """
            SELECT j.*, 
                   (SELECT COUNT(*) FROM officers o WHERE o.assigned_junction_id = j.id AND o.status = 'ACTIVE') AS officer_count
            FROM junctions j
            ORDER BY j.name ASC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToJunction(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving junctions: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Junction save(Junction junction) throws DatabaseException {
        String sql = "INSERT INTO junctions (junction_code, name, area, landmark, latitude, longitude, risk_level) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, junction.getJunctionCode().trim().toUpperCase());
            pstmt.setString(2, junction.getName());
            pstmt.setString(3, junction.getArea());
            pstmt.setString(4, junction.getLandmark());
            pstmt.setDouble(5, junction.getLatitude());
            pstmt.setDouble(6, junction.getLongitude());
            pstmt.setString(7, junction.getRiskLevel() != null ? junction.getRiskLevel().name() : "MEDIUM");
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    junction.setId(keys.getInt(1));
                    return junction;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving new junction: " + e.getMessage(), e);
        }
        throw new DatabaseException("Failed to save junction, no generated key.");
    }

    @Override
    public boolean update(Junction junction) throws DatabaseException {
        String sql = "UPDATE junctions SET name = ?, area = ?, landmark = ?, latitude = ?, longitude = ?, risk_level = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, junction.getName());
            pstmt.setString(2, junction.getArea());
            pstmt.setString(3, junction.getLandmark());
            pstmt.setDouble(4, junction.getLatitude());
            pstmt.setDouble(5, junction.getLongitude());
            pstmt.setString(6, junction.getRiskLevel() != null ? junction.getRiskLevel().name() : "MEDIUM");
            pstmt.setInt(7, junction.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating junction: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Integer junctionId) throws DatabaseException {
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Unassign officers assigned to this junction beat
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE officers SET assigned_junction_id = NULL WHERE assigned_junction_id = ?;")) {
                    pstmt.setInt(1, junctionId);
                    pstmt.executeUpdate();
                }

                // 2. Unassign complaints filed at this junction
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE complaints SET junction_id = NULL WHERE junction_id = ?;")) {
                    pstmt.setInt(1, junctionId);
                    pstmt.executeUpdate();
                }

                // 3. Delete emergency reports at this junction
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM emergency_reports WHERE junction_id = ?;")) {
                    pstmt.setInt(1, junctionId);
                    pstmt.executeUpdate();
                }

                // 4. Delete duties scheduled at this junction
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM duty_assignments WHERE junction_id = ?;")) {
                    pstmt.setInt(1, junctionId);
                    pstmt.executeUpdate();
                }

                // 5. Delete challans issued at this junction so RESTRICT does not block
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM challans WHERE junction_id = ?;")) {
                    pstmt.setInt(1, junctionId);
                    pstmt.executeUpdate();
                }

                // 6. Delete junction
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM junctions WHERE id = ?;")) {
                    pstmt.setInt(1, junctionId);
                    pstmt.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Failed to delete junction: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage(), e);
        }
    }

    private Junction mapResultSetToJunction(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String code = rs.getString("junction_code");
        String name = rs.getString("name");
        String area = rs.getString("area");
        String landmark = rs.getString("landmark");
        double lat = rs.getDouble("latitude");
        double lon = rs.getDouble("longitude");
        String riskStr = rs.getString("risk_level");
        RiskLevel risk = RiskLevel.MEDIUM;
        if (riskStr != null) {
            try {
                risk = RiskLevel.valueOf(riskStr);
            } catch (Exception ignored) {
            }
        }
        Junction j = new Junction(id, code, name, area, landmark, lat, lon, risk);
        j.setActiveOfficersCount(rs.getInt("officer_count"));
        return j;
    }
}
