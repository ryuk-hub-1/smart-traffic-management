package com.smarttraffic.model;

import com.smarttraffic.model.enums.DutyStatus;
import com.smarttraffic.model.enums.ShiftType;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Duty Assignment entity mapping Officers to Junctions for specific shifts.
 */
public class DutyAssignment {
    private int id;
    private int officerId;
    private String officerName;
    private String badgeNumber;
    private int junctionId;
    private String junctionName;
    private ShiftType shift;
    private LocalDate dutyDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private DutyStatus status;
    private String notes;

    public DutyAssignment() {
        this.status = DutyStatus.SCHEDULED;
    }

    public DutyAssignment(int id, int officerId, int junctionId, ShiftType shift,
                          LocalDate dutyDate, LocalTime startTime, LocalTime endTime,
                          DutyStatus status, String notes) {
        this.id = id;
        this.officerId = officerId;
        this.junctionId = junctionId;
        this.shift = shift;
        this.dutyDate = dutyDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status != null ? status : DutyStatus.SCHEDULED;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOfficerId() {
        return officerId;
    }

    public void setOfficerId(int officerId) {
        this.officerId = officerId;
    }

    public String getOfficerName() {
        return officerName;
    }

    public void setOfficerName(String officerName) {
        this.officerName = officerName;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
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

    public ShiftType getShift() {
        return shift;
    }

    public void setShift(ShiftType shift) {
        this.shift = shift;
    }

    public LocalDate getDutyDate() {
        return dutyDate;
    }

    public void setDutyDate(LocalDate dutyDate) {
        this.dutyDate = dutyDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public DutyStatus getStatus() {
        return status;
    }

    public void setStatus(DutyStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
