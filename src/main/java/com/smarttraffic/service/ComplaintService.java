package com.smarttraffic.service;

import com.smarttraffic.dao.ComplaintDao;
import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.Complaint;
import com.smarttraffic.model.enums.ComplaintStatus;

import java.util.List;
import java.util.Optional;

/**
 * Service managing Citizen Traffic Complaints and Status Updates.
 */
public class ComplaintService {
    private static ComplaintService instance;
    private final ComplaintDao complaintDao;

    private ComplaintService() {
        this.complaintDao = new ComplaintDao();
    }

    public static synchronized ComplaintService getInstance() {
        if (instance == null) {
            instance = new ComplaintService();
        }
        return instance;
    }

    public List<Complaint> getAllComplaints() throws DatabaseException {
        return complaintDao.findAll();
    }

    public Optional<Complaint> getComplaintById(int id) throws DatabaseException {
        return complaintDao.findById(id);
    }

    public List<Complaint> getComplaintsByCitizen(int citizenId) throws DatabaseException {
        return complaintDao.findByCitizenId(citizenId);
    }

    public List<Complaint> getComplaintsForOfficer(int officerId) throws DatabaseException {
        return complaintDao.findByAssignedOfficerId(officerId);
    }

    public Complaint fileComplaint(Complaint complaint) throws ValidationException, DatabaseException {
        if (complaint.getCitizenId() <= 0) {
            throw new ValidationException("Citizen user is required.");
        }
        if (complaint.getComplaintType() == null || complaint.getComplaintType().trim().isEmpty()) {
            throw new ValidationException("Complaint category/type is required.");
        }
        if (complaint.getDescription() == null || complaint.getDescription().trim().isEmpty()) {
            throw new ValidationException("Detailed incident description is required.");
        }

        complaint.setStatus(ComplaintStatus.PENDING);
        return complaintDao.save(complaint);
    }

    public boolean updateComplaintStatus(int complaintId, ComplaintStatus status,
                                         Integer assignedOfficerId, String resolutionNotes)
            throws ValidationException, DatabaseException {
        if (complaintId <= 0) {
            throw new ValidationException("Invalid complaint identifier.");
        }
        return complaintDao.updateStatusAndAction(complaintId, status, assignedOfficerId, resolutionNotes);
    }

    public boolean deleteComplaint(int complaintId) throws DatabaseException {
        return complaintDao.deleteById(complaintId);
    }
}
