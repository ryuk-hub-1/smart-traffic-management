package com.smarttraffic.service;

import com.smarttraffic.dao.UserDao;
import com.smarttraffic.exception.AuthenticationException;
import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.Citizen;
import com.smarttraffic.model.User;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Optional;

/**
 * Service managing user authentication, active session state, and registrations.
 */
public class AuthService {
    private static AuthService instance;
    private final UserDao userDao;
    private final ObjectProperty<User> currentUserProperty = new SimpleObjectProperty<>(null);

    private AuthService() {
        this.userDao = new UserDao();
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public User login(String username, String password) throws AuthenticationException, ValidationException {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Password cannot be empty.");
        }

        try {
            Optional<User> userOpt = userDao.authenticate(username.trim(), password);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                currentUserProperty.set(user);
                return user;
            } else {
                throw new AuthenticationException("Invalid username or password.");
            }
        } catch (DatabaseException e) {
            throw new AuthenticationException("Database error during login: " + e.getMessage());
        }
    }

    public Citizen registerCitizen(String username, String password, String confirmPassword,
                                   String fullName, String email, String phone,
                                   String dlNumber, String vehicleNumber, String address)
            throws ValidationException, DatabaseException {

        if (username == null || username.trim().length() < 3) {
            throw new ValidationException("Username must be at least 3 characters.");
        }
        if (password == null || password.length() < 4) {
            throw new ValidationException("Password must be at least 4 characters.");
        }
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Full Name is required.");
        }

        // Check if username already exists
        if (userDao.findByUsername(username.trim()).isPresent()) {
            throw new ValidationException("Username '" + username + "' is already taken. Please choose another.");
        }

        return userDao.registerCitizen(username.trim(), password, fullName.trim(),
                email != null ? email.trim() : "",
                phone != null ? phone.trim() : "",
                dlNumber, vehicleNumber, address);
    }

    public void logout() {
        currentUserProperty.set(null);
    }

    public User getCurrentUser() {
        return currentUserProperty.get();
    }

    public ObjectProperty<User> currentUserProperty() {
        return currentUserProperty;
    }

    public boolean isAuthenticated() {
        return currentUserProperty.get() != null;
    }
}
