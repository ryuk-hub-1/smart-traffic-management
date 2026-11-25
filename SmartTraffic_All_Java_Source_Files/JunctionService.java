package com.smarttraffic.service;

import com.smarttraffic.dao.JunctionDao;
import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.Junction;

import java.util.List;
import java.util.Optional;

/**
 * Service managing Traffic Junctions and Points of Interest.
 */
public class JunctionService {
    private static JunctionService instance;
    private final JunctionDao junctionDao;

    private JunctionService() {
        this.junctionDao = new JunctionDao();
    }

    public static synchronized JunctionService getInstance() {
        if (instance == null) {
            instance = new JunctionService();
        }
        return instance;
    }

    public List<Junction> getAllJunctions() throws DatabaseException {
        return junctionDao.findAll();
    }

    public Optional<Junction> getJunctionById(int id) throws DatabaseException {
        return junctionDao.findById(id);
    }

    public Junction addJunction(Junction junction) throws ValidationException, DatabaseException {
        if (junction.getJunctionCode() == null || junction.getJunctionCode().trim().isEmpty()) {
            throw new ValidationException("Junction code is required.");
        }
        if (junction.getName() == null || junction.getName().trim().isEmpty()) {
            throw new ValidationException("Junction name is required.");
        }
        return junctionDao.save(junction);
    }

    public Junction createJunction(Junction junction) throws ValidationException, DatabaseException {
        return addJunction(junction);
    }

    public boolean updateJunction(Junction junction) throws ValidationException, DatabaseException {
        if (junction == null || junction.getId() <= 0) {
            throw new ValidationException("Invalid junction record.");
        }
        return junctionDao.update(junction);
    }

    public boolean deleteJunction(int id) throws DatabaseException {
        return junctionDao.deleteById(id);
    }
}
