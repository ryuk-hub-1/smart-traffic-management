package com.smarttraffic.service;

import com.smarttraffic.dao.EmergencyDao;
import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.EmergencyReport;
import com.smarttraffic.model.enums.EmergencyStatus;

import java.util.List;
import java.util.Optional;

/**
 * Service managing Accidents, Emergencies, and Rescue Operations.
 */
public class EmergencyService {
    private static EmergencyService instance;
    private final EmergencyDao emergencyDao;

    private EmergencyService() {
        this.emergencyDao = new EmergencyDao();
    }

    public static synchronized EmergencyService getInstance() {
        if (instance == null) {
            instance = new EmergencyService();
        }
        return instance;
    }

    public List<EmergencyReport> getAllEmergencies() throws DatabaseException {
        return emergencyDao.findAll();
    }

    public Optional<EmergencyReport> getEmergencyById(int id) throws DatabaseException {
        return emergencyDao.findById(id);
    }

    public EmergencyReport reportEmergency(EmergencyReport report) throws ValidationException, DatabaseException {
        if (report.getJunctionId() <= 0) {
            throw new ValidationException("Accident Junction location must be specified.");
        }
        if (report.getAccidentType() == null || report.getAccidentType().trim().isEmpty()) {
            throw new ValidationException("Accident incident type is required.");
        }
        if (report.getVehiclesInvolved() < 1) {
            report.setVehiclesInvolved(1);
        }

        report.setStatus(EmergencyStatus.REPORTED);
        return emergencyDao.save(report);
    }

    public boolean updateStatus(int reportId, EmergencyStatus status, String notes) throws ValidationException, DatabaseException {
        if (reportId <= 0) {
            throw new ValidationException("Invalid emergency report identifier.");
        }
        return emergencyDao.updateStatus(reportId, status, notes);
    }

    public boolean deleteEmergency(int reportId) throws DatabaseException {
        return emergencyDao.deleteById(reportId);
    }
}
