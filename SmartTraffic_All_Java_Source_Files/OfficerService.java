package com.smarttraffic.service;

import com.smarttraffic.dao.OfficerDao;
import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.Officer;

import java.util.List;
import java.util.Optional;

/**
 * Service managing Traffic Police Officers.
 */
public class OfficerService {
    private static OfficerService instance;
    private final OfficerDao officerDao;

    private OfficerService() {
        this.officerDao = new OfficerDao();
    }

    public static synchronized OfficerService getInstance() {
        if (instance == null) {
            instance = new OfficerService();
        }
        return instance;
    }

    public List<Officer> getAllOfficers() throws DatabaseException {
        return officerDao.findAll();
    }

    public Optional<Officer> getOfficerById(int id) throws DatabaseException {
        return officerDao.findById(id);
    }

    public Optional<Officer> getOfficerByUserId(int userId) throws DatabaseException {
        return officerDao.findByUserId(userId);
    }

    public Officer registerOfficer(String username, String password, String fullName,
                                   String email, String phone, String badgeNumber,
                                   String rank, String station, Integer assignedJunctionId)
            throws ValidationException, DatabaseException {

        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Officer username is required.");
        }
        if (new com.smarttraffic.dao.UserDao().findByUsername(username.trim()).isPresent()) {
            throw new ValidationException("Username '" + username.trim() + "' is already registered. Choose another.");
        }
        if (password == null || password.length() < 4) {
            throw new ValidationException("Officer password must be at least 4 characters.");
        }
        if (badgeNumber == null || badgeNumber.trim().isEmpty()) {
            throw new ValidationException("Badge number is required.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Officer full name is required.");
        }

        if (officerDao.findByBadge(badgeNumber.trim()).isPresent()) {
            throw new ValidationException("An officer with badge number '" + badgeNumber + "' already exists.");
        }

        return officerDao.createOfficer(username.trim(), password, fullName.trim(),
                email != null ? email.trim() : "",
                phone != null ? phone.trim() : "",
                badgeNumber.trim().toUpperCase(),
                rank != null ? rank : "Officer",
                station != null ? station : "Central Division",
                assignedJunctionId);
    }

    public boolean updateOfficer(Officer officer) throws ValidationException, DatabaseException {
        if (officer == null) throw new ValidationException("Officer object cannot be null.");
        if (officer.getFullName() == null || officer.getFullName().trim().isEmpty()) {
            throw new ValidationException("Officer full name is required.");
        }
        if (officer.getBadgeNumber() == null || officer.getBadgeNumber().trim().isEmpty()) {
            throw new ValidationException("Badge number is required.");
        }

        Optional<Officer> existing = officerDao.findByBadge(officer.getBadgeNumber().trim());
        if (existing.isPresent() && existing.get().getOfficerId() != officer.getOfficerId()) {
            throw new ValidationException("Badge number '" + officer.getBadgeNumber() + "' is already assigned to another officer.");
        }

        return officerDao.update(officer);
    }

    public boolean assignJunction(int officerId, Integer junctionId) throws DatabaseException {
        return officerDao.assignJunction(officerId, junctionId);
    }

    public boolean deleteOfficer(int officerId) throws DatabaseException {
        return officerDao.deleteById(officerId);
    }
}
