package com.smarttraffic.model;

import com.smarttraffic.model.enums.Role;
import java.time.LocalDateTime;

/**
 * Admin entity representing department administrators.
 * Demonstrates Inheritance from User.
 */
public class AdminUser extends User {
    private String department;
    private String clearanceLevel;

    public AdminUser() {
        setRole(Role.ADMIN);
    }

    public AdminUser(int id, String username, String passwordHash,
                     String fullName, String email, String phone, LocalDateTime createdAt,
                     String department, String clearanceLevel) {
        super(id, username, passwordHash, Role.ADMIN, fullName, email, phone, createdAt);
        this.department = department;
        this.clearanceLevel = clearanceLevel;
    }

    @Override
    public String getRoleDescription() {
        return "System Administrator with full management and reporting authority.";
    }

    @Override
    public boolean hasAdministrativePrivileges() {
        return true;
    }

    @Override
    public boolean hasFieldEnforcementPrivileges() {
        return true;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getClearanceLevel() {
        return clearanceLevel;
    }

    public void setClearanceLevel(String clearanceLevel) {
        this.clearanceLevel = clearanceLevel;
    }
}
