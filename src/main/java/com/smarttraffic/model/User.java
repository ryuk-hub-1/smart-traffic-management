package com.smarttraffic.model;

import com.smarttraffic.model.enums.Role;
import java.time.LocalDateTime;

/**
 * Abstract Base Class representing a User in the Smart Traffic System.
 * Demonstrates Abstraction, Encapsulation, and Polymorphism.
 */
public abstract class User {
    private int id;
    private String username;
    private String passwordHash;
    private Role role;
    private String fullName;
    private String email;
    private String phone;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(int id, String username, String passwordHash, Role role,
                String fullName, String email, String phone, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.createdAt = createdAt;
    }

    // Abstract methods demonstrating polymorphism
    public abstract String getRoleDescription();
    public abstract boolean hasAdministrativePrivileges();
    public abstract boolean hasFieldEnforcementPrivileges();

    public String getFormattedContact() {
        return fullName + " (" + email + " | " + phone + ")";
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return fullName + " [" + (role != null ? role.getDisplayName() : "") + "]";
    }
}
