package com.smarttraffic.model;

import com.smarttraffic.model.enums.EmergencySeverity;
import com.smarttraffic.model.enums.EmergencyStatus;
import java.time.LocalDateTime;

/**
 * Emergency and Accident Incident Report entity.
 */
public class EmergencyReport {
    private int id;
    private String reportNumber;
    private Integer officerId;
    private String officerName;
    private int junctionId;
    private String junctionName;
    private String accidentType;
    private EmergencySeverity severity;
    private int vehiclesInvolved;
    private int casualtiesCount;
    private boolean ambulanceRequired;
    private boolean towTruckRequired;
    private boolean roadBlocked;
    private LocalDateTime reportTime;
    private EmergencyStatus status;
    private String notes;

    public EmergencyReport() {
        this.severity = EmergencySeverity.MINOR;
        this.status = EmergencyStatus.REPORTED;
        this.reportTime = LocalDateTime.now();
    }

    public EmergencyReport(int id, String reportNumber, Integer officerId, int junctionId,
                           String accidentType, EmergencySeverity severity, int vehiclesInvolved,
                           int casualtiesCount, boolean ambulanceRequired, boolean towTruckRequired,
                           boolean roadBlocked, LocalDateTime reportTime, EmergencyStatus status,
                           String notes) {
        this.id = id;
        this.reportNumber = reportNumber;
        this.officerId = officerId;
        this.junctionId = junctionId;
        this.accidentType = accidentType;
        this.severity = severity != null ? severity : EmergencySeverity.MINOR;
        this.vehiclesInvolved = vehiclesInvolved;
        this.casualtiesCount = casualtiesCount;
        this.ambulanceRequired = ambulanceRequired;
        this.towTruckRequired = towTruckRequired;
        this.roadBlocked = roadBlocked;
        this.reportTime = reportTime != null ? reportTime : LocalDateTime.now();
        this.status = status != null ? status : EmergencyStatus.REPORTED;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getReportNumber() {
        return reportNumber;
    }

    public void setReportNumber(String reportNumber) {
        this.reportNumber = reportNumber;
    }

    public Integer getOfficerId() {
        return officerId;
    }

    public void setOfficerId(Integer officerId) {
        this.officerId = officerId;
    }

    public String getOfficerName() {
        return officerName;
    }

    public void setOfficerName(String officerName) {
        this.officerName = officerName;
    }

    public int getJunctionId() {
        return junctionId;
    }

    public void setJunctionId(int junctionId) {
        this.junctionId = junctionId;
    }

    public String getJunctionName() {
        return junctionName;
    }

    public void setJunctionName(String junctionName) {
        this.junctionName = junctionName;
    }

    public String getAccidentType() {
        return accidentType;
    }

    public void setAccidentType(String accidentType) {
        this.accidentType = accidentType;
    }

    public EmergencySeverity getSeverity() {
        return severity;
    }

    public void setSeverity(EmergencySeverity severity) {
        this.severity = severity;
    }

    public int getVehiclesInvolved() {
        return vehiclesInvolved;
    }

    public void setVehiclesInvolved(int vehiclesInvolved) {
        this.vehiclesInvolved = vehiclesInvolved;
    }

    public int getCasualtiesCount() {
        return casualtiesCount;
    }

    public void setCasualtiesCount(int casualtiesCount) {
        this.casualtiesCount = casualtiesCount;
    }

    public boolean isAmbulanceRequired() {
        return ambulanceRequired;
    }

    public void setAmbulanceRequired(boolean ambulanceRequired) {
        this.ambulanceRequired = ambulanceRequired;
    }

    public boolean isTowTruckRequired() {
        return towTruckRequired;
    }

    public void setTowTruckRequired(boolean towTruckRequired) {
        this.towTruckRequired = towTruckRequired;
    }

    public boolean isRoadBlocked() {
        return roadBlocked;
    }

    public void setRoadBlocked(boolean roadBlocked) {
        this.roadBlocked = roadBlocked;
    }

    public LocalDateTime getReportTime() {
        return reportTime;
    }

    public void setReportTime(LocalDateTime reportTime) {
        this.reportTime = reportTime;
    }

    public EmergencyStatus getStatus() {
        return status;
    }

    public void setStatus(EmergencyStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
