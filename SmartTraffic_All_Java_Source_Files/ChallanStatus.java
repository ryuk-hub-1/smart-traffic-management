package com.smarttraffic.model.enums;

public enum ChallanStatus {
    PENDING("Pending Payment"),
    PAID("Paid"),
    OVERDUE("Overdue"),
    DISPUTED("Disputed / Under Review"),
    CANCELLED("Cancelled");

    private final String label;

    ChallanStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
