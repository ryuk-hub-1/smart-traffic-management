package com.smarttraffic.model;

import com.smarttraffic.model.enums.Role;
import java.time.LocalDateTime;

/**
 * Citizen entity representing public users who view challans and file complaints.
 * Demonstrates Inheritance from User.
 */
public class Citizen extends User {
    private String drivingLicenseNumber;
    private String registeredVehicleNumber;
    private String address;

    public Citizen() {
        setRole(Role.CITIZEN);
    }

    public Citizen(int id, String username, String passwordHash,
                   String fullName, String email, String phone, LocalDateTime createdAt,
                   String drivingLicenseNumber, String registeredVehicleNumber, String address) {
        super(id, username, passwordHash, Role.CITIZEN, fullName, email, phone, createdAt);
        this.drivingLicenseNumber = drivingLicenseNumber;
        this.registeredVehicleNumber = registeredVehicleNumber;
        this.address = address;
    }

    @Override
    public String getRoleDescription() {
        return "Citizen account to monitor vehicle challans and report traffic anomalies.";
    }

    @Override
    public boolean hasAdministrativePrivileges() {
        return false;
    }

    @Override
    public boolean hasFieldEnforcementPrivileges() {
        return false;
    }

    public String getDrivingLicenseNumber() {
        return drivingLicenseNumber;
    }

    public void setDrivingLicenseNumber(String drivingLicenseNumber) {
        this.drivingLicenseNumber = drivingLicenseNumber;
    }

    public String getRegisteredVehicleNumber() {
        return registeredVehicleNumber;
    }

    public void setRegisteredVehicleNumber(String registeredVehicleNumber) {
        this.registeredVehicleNumber = registeredVehicleNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
