package com.smarttraffic.service;

import com.smarttraffic.dao.ChallanDao;
import com.smarttraffic.dao.ViolationDao;
import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.Challan;
import com.smarttraffic.model.ViolationType;
import com.smarttraffic.model.enums.ChallanStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service managing Traffic Violations, Challan Issuance, and Payments.
 */
public class ChallanService {
    private static ChallanService instance;
    private final ChallanDao challanDao;
    private final ViolationDao violationDao;

    private ChallanService() {
        this.challanDao = new ChallanDao();
        this.violationDao = new ViolationDao();
    }

    public static synchronized ChallanService getInstance() {
        if (instance == null) {
            instance = new ChallanService();
        }
        return instance;
    }

    public List<ViolationType> getAllViolationTypes() throws DatabaseException {
        return violationDao.findAll();
    }

    public Optional<ViolationType> getViolationTypeById(int id) throws DatabaseException {
        return violationDao.findById(id);
    }

    public List<Challan> getAllChallans() throws DatabaseException {
        return challanDao.findAll();
    }

    public Optional<Challan> getChallanById(int id) throws DatabaseException {
        return challanDao.findById(id);
    }

    public Optional<Challan> getChallanByNumber(String challanNumber) throws DatabaseException {
        return challanDao.findByChallanNumber(challanNumber);
    }

    public List<Challan> getChallansForVehicle(String vehicleNumber) throws DatabaseException {
        if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
            return List.of();
        }
        return challanDao.findByVehicleNumber(vehicleNumber.trim());
    }

    public List<Challan> getChallansByOfficer(int officerId) throws DatabaseException {
        return challanDao.findByOfficerId(officerId);
    }

    public Challan issueChallan(Challan challan) throws ValidationException, DatabaseException {
        if (challan.getVehicleNumber() == null || challan.getVehicleNumber().trim().isEmpty()) {
            throw new ValidationException("Vehicle Registration Number is mandatory.");
        }
        if (challan.getVehicleType() == null) {
            throw new ValidationException("Vehicle Type must be specified.");
        }
        if (challan.getViolationId() <= 0) {
            throw new ValidationException("Violation type must be selected.");
        }
        if (challan.getJunctionId() <= 0) {
            throw new ValidationException("Violation junction/location must be selected.");
        }
        if (challan.getFineAmount() <= 0) {
            throw new ValidationException("Fine amount must be greater than zero.");
        }

        challan.setPaymentStatus(ChallanStatus.PENDING);
        return challanDao.save(challan);
    }

    public boolean payChallan(int challanId, String paymentReference) throws ValidationException, DatabaseException {
        Optional<Challan> challanOpt = challanDao.findById(challanId);
        if (challanOpt.isEmpty()) {
            throw new ValidationException("Challan not found.");
        }
        Challan challan = challanOpt.get();
        if (challan.getPaymentStatus() == ChallanStatus.PAID) {
            throw new ValidationException("Challan " + challan.getChallanNumber() + " is already marked as PAID.");
        }

        String ref = paymentReference != null && !paymentReference.trim().isEmpty()
                ? paymentReference.trim().toUpperCase()
                : "TXN-" + System.currentTimeMillis();

        return challanDao.markAsPaid(challanId, ref);
    }

    public boolean deleteChallan(int challanId) throws DatabaseException {
        return challanDao.deleteById(challanId);
    }

    public boolean updateChallan(Challan challan) throws DatabaseException {
        return challanDao.update(challan);
    }

    public Map<String, Integer> getViolationsByCategoryStats() throws DatabaseException {
        return challanDao.getViolationsByCategoryStats();
    }

    public Map<String, Double> getPaymentSummaryStats() throws DatabaseException {
        return challanDao.getPaymentSummaryStats();
    }
}
