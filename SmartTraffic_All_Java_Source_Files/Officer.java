package com.smarttraffic.model;

import com.smarttraffic.model.enums.Role;
import java.time.LocalDateTime;

/**
 * Traffic Police Officer entity.
 * Demonstrates Inheritance and Encapsulation.
 */
public class Officer extends User {
    private int officerId; // PK in officers table
    private String badgeNumber;
    private String rank;
    private String policeStation;
    private String status; // ACTIVE, ON_LEAVE, SUSPENDED, RETIRED
    private Integer assignedJunctionId;
    private String assignedJunctionName;

    public Officer() {
        setRole(Role.OFFICER);
        this.status = "ACTIVE";
    }

    public Officer(int id, String username, String passwordHash,
                   String fullName, String email, String phone, LocalDateTime createdAt,
                   int officerId, String badgeNumber, String rank, String policeStation,
                   String status, Integer assignedJunctionId, String assignedJunctionName) {
        super(id, username, passwordHash, Role.OFFICER, fullName, email, phone, createdAt);
        this.officerId = officerId;
        this.badgeNumber = badgeNumber;
        this.rank = rank;
        this.policeStation = policeStation;
        this.status = status != null ? status : "ACTIVE";
        this.assignedJunctionId = assignedJunctionId;
        this.assignedJunctionName = assignedJunctionName;
    }

    @Override
    public String getRoleDescription() {
        return "Field Traffic Police Officer assigned to junctions and traffic enforcement.";
    }

    @Override
    public boolean hasAdministrativePrivileges() {
        return false;
    }

    @Override
    public boolean hasFieldEnforcementPrivileges() {
        return true;
    }

    public String getRankAndName() {
        return (rank != null ? rank : "Officer") + " " + getFullName() + " [" + badgeNumber + "]";
    }

    // Getters and Setters
    public int getOfficerId() {
        return officerId;
    }

    public void setOfficerId(int officerId) {
        this.officerId = officerId;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getPoliceStation() {
        return policeStation;
    }

    public void setPoliceStation(String policeStation) {
        this.policeStation = policeStation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAssignedJunctionId() {
        return assignedJunctionId;
    }

    public void setAssignedJunctionId(Integer assignedJunctionId) {
        this.assignedJunctionId = assignedJunctionId;
    }

    public String getAssignedJunctionName() {
        return assignedJunctionName;
    }

    public void setAssignedJunctionName(String assignedJunctionName) {
        this.assignedJunctionName = assignedJunctionName;
    }
}
