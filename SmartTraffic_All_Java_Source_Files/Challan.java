package com.smarttraffic.model;

import com.smarttraffic.model.enums.ChallanStatus;
import com.smarttraffic.model.enums.VehicleType;
import java.time.LocalDateTime;

/**
 * Digital Traffic Challan entity representing an electronic violation notice.
 */
public class Challan {
    private int id;
    private String challanNumber;
    private String vehicleNumber;
    private VehicleType vehicleType;
    private String violatorName;
    private String violatorPhone;
    private int officerId;
    private String officerName;
    private String officerBadge;
    private int junctionId;
    private String junctionName;
    private int violationId;
    private String violationTitle;
    private String violationCategory;
    private LocalDateTime violationDate;
    private double fineAmount;
    private ChallanStatus paymentStatus;
    private LocalDateTime paymentDate;
    private String paymentReference;
    private String evidenceFilePath;
    private String remarks;

    public Challan() {
        this.paymentStatus = ChallanStatus.PENDING;
        this.violationDate = LocalDateTime.now();
    }

    public Challan(int id, String challanNumber, String vehicleNumber, VehicleType vehicleType,
                   String violatorName, String violatorPhone, int officerId, int junctionId,
                   int violationId, LocalDateTime violationDate, double fineAmount,
                   ChallanStatus paymentStatus, LocalDateTime paymentDate, String paymentReference,
                   String evidenceFilePath, String remarks) {
        this.id = id;
        this.challanNumber = challanNumber;
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.violatorName = violatorName;
        this.violatorPhone = violatorPhone;
        this.officerId = officerId;
        this.junctionId = junctionId;
        this.violationId = violationId;
        this.violationDate = violationDate != null ? violationDate : LocalDateTime.now();
        this.fineAmount = fineAmount;
        this.paymentStatus = paymentStatus != null ? paymentStatus : ChallanStatus.PENDING;
        this.paymentDate = paymentDate;
        this.paymentReference = paymentReference;
        this.evidenceFilePath = evidenceFilePath;
        this.remarks = remarks;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getChallanNumber() {
        return challanNumber;
    }

    public void setChallanNumber(String challanNumber) {
        this.challanNumber = challanNumber;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getViolatorName() {
        return violatorName;
    }

    public void setViolatorName(String violatorName) {
        this.violatorName = violatorName;
    }

    public String getViolatorPhone() {
        return violatorPhone;
    }

    public void setViolatorPhone(String violatorPhone) {
        this.violatorPhone = violatorPhone;
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

    public String getOfficerBadge() {
        return officerBadge;
    }

    public void setOfficerBadge(String officerBadge) {
        this.officerBadge = officerBadge;
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

    public int getViolationId() {
        return violationId;
    }

    public void setViolationId(int violationId) {
        this.violationId = violationId;
    }

    public String getViolationTitle() {
        return violationTitle;
    }

    public void setViolationTitle(String violationTitle) {
        this.violationTitle = violationTitle;
    }

    public String getViolationCategory() {
        return violationCategory;
    }

    public void setViolationCategory(String violationCategory) {
        this.violationCategory = violationCategory;
    }

    public LocalDateTime getViolationDate() {
        return violationDate;
    }

    public void setViolationDate(LocalDateTime violationDate) {
        this.violationDate = violationDate;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }

    public ChallanStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(ChallanStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getEvidenceFilePath() {
        return evidenceFilePath;
    }

    public void setEvidenceFilePath(String evidenceFilePath) {
        this.evidenceFilePath = evidenceFilePath;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getFormattedFine() {
        return String.format("₹ %,.2f", fineAmount);
    }
}
