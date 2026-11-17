package com.smarttraffic.dao;

import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.Challan;
import com.smarttraffic.model.enums.ChallanStatus;
import com.smarttraffic.model.enums.VehicleType;
import com.smarttraffic.util.DateUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Access Object for Digital Challans and Traffic Violations.
 */
public class ChallanDao implements BaseDao<Challan, Integer> {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public synchronized String generateNextChallanNumber() throws DatabaseException {
        int year = LocalDateTime.now().getYear();
        String prefix = String.format("CH-%d-", year);
        String sql = "SELECT challan_number FROM challans WHERE challan_number LIKE ?;";
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
            return String.format("%s%05d", prefix, maxSeq + 1);
        } catch (SQLException e) {
            throw new DatabaseException("Error generating challan number: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Challan> findById(Integer id) throws DatabaseException {
        String sql = """
            SELECT c.*, 
                   u.full_name AS officer_name, o.badge_number AS officer_badge,
                   j.name AS junction_name,
                   v.title AS violation_title, v.category AS violation_category
            FROM challans c
            JOIN officers o ON c.officer_id = o.id
            JOIN users u ON o.user_id = u.id
            JOIN junctions j ON c.junction_id = j.id
            JOIN violations v ON c.violation_id = v.id
            WHERE c.id = ?;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToChallan(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding challan by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<Challan> findByChallanNumber(String challanNumber) throws DatabaseException {
        String sql = """
            SELECT c.*, 
                   u.full_name AS officer_name, o.badge_number AS officer_badge,
                   j.name AS junction_name,
                   v.title AS violation_title, v.category AS violation_category
            FROM challans c
            JOIN officers o ON c.officer_id = o.id
            JOIN users u ON o.user_id = u.id
            JOIN junctions j ON c.junction_id = j.id
            JOIN violations v ON c.violation_id = v.id
            WHERE c.challan_number = ? COLLATE NOCASE;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, challanNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToChallan(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding challan: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Challan> findAll() throws DatabaseException {
        List<Challan> list = new ArrayList<>();
        String sql = """
            SELECT c.*, 
                   u.full_name AS officer_name, o.badge_number AS officer_badge,
                   j.name AS junction_name,
                   v.title AS violation_title, v.category AS violation_category
            FROM challans c
            JOIN officers o ON c.officer_id = o.id
            JOIN users u ON o.user_id = u.id
            JOIN junctions j ON c.junction_id = j.id
            JOIN violations v ON c.violation_id = v.id
            ORDER BY c.violation_date DESC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToChallan(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing challans: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Challan> findByVehicleNumber(String vehicleNumber) throws DatabaseException {
        List<Challan> list = new ArrayList<>();
        String sql = """
            SELECT c.*, 
                   u.full_name AS officer_name, o.badge_number AS officer_badge,
                   j.name AS junction_name,
                   v.title AS violation_title, v.category AS violation_category
            FROM challans c
            JOIN officers o ON c.officer_id = o.id
            JOIN users u ON o.user_id = u.id
            JOIN junctions j ON c.junction_id = j.id
            JOIN violations v ON c.violation_id = v.id
            WHERE REPLACE(REPLACE(c.vehicle_number, ' ', ''), '-', '') = REPLACE(REPLACE(?, ' ', ''), '-', '') COLLATE NOCASE
            ORDER BY c.violation_date DESC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, vehicleNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToChallan(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding challans by vehicle: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Challan> findByOfficerId(int officerId) throws DatabaseException {
        List<Challan> list = new ArrayList<>();
        String sql = """
            SELECT c.*, 
                   u.full_name AS officer_name, o.badge_number AS officer_badge,
                   j.name AS junction_name,
                   v.title AS violation_title, v.category AS violation_category
            FROM challans c
            JOIN officers o ON c.officer_id = o.id
            JOIN users u ON o.user_id = u.id
            JOIN junctions j ON c.junction_id = j.id
            JOIN violations v ON c.violation_id = v.id
            WHERE c.officer_id = ?
            ORDER BY c.violation_date DESC;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, officerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToChallan(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding officer challans: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Challan save(Challan challan) throws DatabaseException {
        if (challan.getChallanNumber() == null || challan.getChallanNumber().isEmpty()) {
            challan.setChallanNumber(generateNextChallanNumber());
        }

        String sql = """
            INSERT INTO challans (challan_number, vehicle_number, vehicle_type, violator_name, 
                                  violator_phone, officer_id, junction_id, violation_id, 
                                  violation_date, fine_amount, payment_status, payment_date, 
                                  payment_reference, evidence_file_path, remarks)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, challan.getChallanNumber());
            pstmt.setString(2, challan.getVehicleNumber().trim().toUpperCase());
            pstmt.setString(3, challan.getVehicleType().name());
            pstmt.setString(4, challan.getViolatorName());
            pstmt.setString(5, challan.getViolatorPhone());
            pstmt.setInt(6, challan.getOfficerId());
            pstmt.setInt(7, challan.getJunctionId());
            pstmt.setInt(8, challan.getViolationId());
            pstmt.setString(9, challan.getViolationDate() != null ? challan.getViolationDate().toString() : LocalDateTime.now().toString());
            pstmt.setDouble(10, challan.getFineAmount());
            pstmt.setString(11, challan.getPaymentStatus() != null ? challan.getPaymentStatus().name() : ChallanStatus.PENDING.name());
            pstmt.setString(12, challan.getPaymentDate() != null ? challan.getPaymentDate().toString() : null);
            pstmt.setString(13, challan.getPaymentReference());
            pstmt.setString(14, challan.getEvidenceFilePath());
            pstmt.setString(15, challan.getRemarks());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    challan.setId(keys.getInt(1));
                    return findById(challan.getId()).orElse(challan);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving challan: " + e.getMessage(), e);
        }
        throw new DatabaseException("Failed to save challan.");
    }

    public boolean markAsPaid(int challanId, String paymentReference) throws DatabaseException {
        String sql = """
            UPDATE challans 
            SET payment_status = ?, payment_date = ?, payment_reference = ?
            WHERE id = ?;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ChallanStatus.PAID.name());
            pstmt.setString(2, LocalDateTime.now().toString());
            pstmt.setString(3, paymentReference != null ? paymentReference : "TXN-" + System.currentTimeMillis());
            pstmt.setInt(4, challanId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to record challan payment: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Challan challan) throws DatabaseException {
        String sql = """
            UPDATE challans 
            SET vehicle_number = ?, vehicle_type = ?, violator_name = ?, violator_phone = ?,
                fine_amount = ?, payment_status = ?, remarks = ?
            WHERE id = ?;
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, challan.getVehicleNumber().trim().toUpperCase());
            pstmt.setString(2, challan.getVehicleType().name());
            pstmt.setString(3, challan.getViolatorName());
            pstmt.setString(4, challan.getViolatorPhone());
            pstmt.setDouble(5, challan.getFineAmount());
            pstmt.setString(6, challan.getPaymentStatus().name());
            pstmt.setString(7, challan.getRemarks());
            pstmt.setInt(8, challan.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating challan: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Integer id) throws DatabaseException {
        String sql = "DELETE FROM challans WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting challan: " + e.getMessage(), e);
        }
    }

    public Map<String, Integer> getViolationsByCategoryStats() throws DatabaseException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = """
            SELECT v.category, COUNT(c.id) AS count
            FROM violations v
            LEFT JOIN challans c ON v.id = c.violation_id
            GROUP BY v.category;
        """;

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                stats.put(rs.getString("category"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error calculating category stats: " + e.getMessage(), e);
        }
        return stats;
    }

    public Map<String, Double> getPaymentSummaryStats() throws DatabaseException {
        Map<String, Double> map = new HashMap<>();
        String sql = """
            SELECT payment_status, SUM(fine_amount) as total_fine, COUNT(*) as count
            FROM challans
            GROUP BY payment_status;
        """;

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("payment_status") + "_AMOUNT", rs.getDouble("total_fine"));
                map.put(rs.getString("payment_status") + "_COUNT", (double) rs.getInt("count"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error calculating payment stats: " + e.getMessage(), e);
        }
        return map;
    }

    private Challan mapResultSetToChallan(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String challanNum = rs.getString("challan_number");
        String vehNo = rs.getString("vehicle_number");
        VehicleType vType = VehicleType.valueOf(rs.getString("vehicle_type"));
        String violName = rs.getString("violator_name");
        String violPhone = rs.getString("violator_phone");
        int offId = rs.getInt("officer_id");
        int jId = rs.getInt("junction_id");
        int vId = rs.getInt("violation_id");
        LocalDateTime vDate = DateUtil.parseDateTime(rs.getString("violation_date"));
        double fine = rs.getDouble("fine_amount");
        ChallanStatus pStatus = ChallanStatus.valueOf(rs.getString("payment_status"));
        LocalDateTime pDate = DateUtil.parseDateTime(rs.getString("payment_date"));
        String pRef = rs.getString("payment_reference");
        String evPath = rs.getString("evidence_file_path");
        String remarks = rs.getString("remarks");

        Challan c = new Challan(id, challanNum, vehNo, vType, violName, violPhone, offId, jId, vId, vDate, fine, pStatus, pDate, pRef, evPath, remarks);
        c.setOfficerName(rs.getString("officer_name"));
        c.setOfficerBadge(rs.getString("officer_badge"));
        c.setJunctionName(rs.getString("junction_name"));
        c.setViolationTitle(rs.getString("violation_title"));
        c.setViolationCategory(rs.getString("violation_category"));
        return c;
    }
}
