package com.smarttraffic.model;

/**
 * Traffic violation definition and standard legal penalties.
 */
public class ViolationType {
    private int id;
    private String violationCode;
    private String title;
    private String category;
    private String description;
    private double standardFineAmount;

    public ViolationType() {
    }

    public ViolationType(int id, String violationCode, String title, String category,
                         String description, double standardFineAmount) {
        this.id = id;
        this.violationCode = violationCode;
        this.title = title;
        this.category = category;
        this.description = description;
        this.standardFineAmount = standardFineAmount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getViolationCode() {
        return violationCode;
    }

    public void setViolationCode(String violationCode) {
        this.violationCode = violationCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStandardFineAmount() {
        return standardFineAmount;
    }

    public void setStandardFineAmount(double standardFineAmount) {
        this.standardFineAmount = standardFineAmount;
    }

    public String getFormattedFine() {
        return String.format("₹ %,.2f", standardFineAmount);
    }

    @Override
    public String toString() {
        return title + " (" + getFormattedFine() + ")";
    }
}
