package com.smarttraffic.model;

import com.smarttraffic.model.enums.ComplaintStatus;
import java.time.LocalDateTime;

/**
 * Citizen Complaint entity.
 */
public class Complaint {
    private int id;
    private String complaintNumber;
    private int citizenId;
    private String citizenName;
    private String citizenPhone;
    private Integer junctionId;
    private String junctionName;
    private String complaintType;
    private String vehicleNumber;
    private String description;
    private LocalDateTime incidentDate;
    private ComplaintStatus status;
    private Integer assignedOfficerId;
    private String assignedOfficerName;
    private String officerActionNotes;
    private String evidenceFilePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Complaint() {
        this.status = ComplaintStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Complaint(int id, String complaintNumber, int citizenId, Integer junctionId,
                     String complaintType, String vehicleNumber, String description,
                     LocalDateTime incidentDate, ComplaintStatus status,
                     Integer assignedOfficerId, String officerActionNotes,
                     String evidenceFilePath, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.complaintNumber = complaintNumber;
        this.citizenId = citizenId;
        this.junctionId = junctionId;
        this.complaintType = complaintType;
        this.vehicleNumber = vehicleNumber;
        this.description = description;
        this.incidentDate = incidentDate != null ? incidentDate : LocalDateTime.now();
        this.status = status != null ? status : ComplaintStatus.PENDING;
        this.assignedOfficerId = assignedOfficerId;
        this.officerActionNotes = officerActionNotes;
        this.evidenceFilePath = evidenceFilePath;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getComplaintNumber() {
        return complaintNumber;
    }

    public void setComplaintNumber(String complaintNumber) {
        this.complaintNumber = complaintNumber;
    }

    public int getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(int citizenId) {
        this.citizenId = citizenId;
    }

    public String getCitizenName() {
        return citizenName;
    }

    public void setCitizenName(String citizenName) {
        this.citizenName = citizenName;
    }

    public String getCitizenPhone() {
        return citizenPhone;
    }

    public void setCitizenPhone(String citizenPhone) {
        this.citizenPhone = citizenPhone;
    }

    public Integer getJunctionId() {
        return junctionId;
    }

    public void setJunctionId(Integer junctionId) {
        this.junctionId = junctionId;
    }

    public String getJunctionName() {
        return junctionName;
    }

    public void setJunctionName(String junctionName) {
        this.junctionName = junctionName;
    }

    public String getComplaintType() {
        return complaintType;
    }

    public void setComplaintType(String complaintType) {
        this.complaintType = complaintType;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(LocalDateTime incidentDate) {
        this.incidentDate = incidentDate;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public Integer getAssignedOfficerId() {
        return assignedOfficerId;
    }

    public void setAssignedOfficerId(Integer assignedOfficerId) {
        this.assignedOfficerId = assignedOfficerId;
    }

    public String getAssignedOfficerName() {
        return assignedOfficerName;
    }

    public void setAssignedOfficerName(String assignedOfficerName) {
        this.assignedOfficerName = assignedOfficerName;
    }

    public String getOfficerActionNotes() {
        return officerActionNotes;
    }

    public void setOfficerActionNotes(String officerActionNotes) {
        this.officerActionNotes = officerActionNotes;
    }

    public String getEvidenceFilePath() {
        return evidenceFilePath;
    }

    public void setEvidenceFilePath(String evidenceFilePath) {
        this.evidenceFilePath = evidenceFilePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
