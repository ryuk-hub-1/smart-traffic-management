package com.smarttraffic.model.enums;

public enum ComplaintStatus {
    PENDING("Pending Review"),
    INVESTIGATING("Investigating / In Progress"),
    RESOLVED("Resolved"),
    REJECTED("Rejected / Closed");

    private final String label;

    ComplaintStatus(String label) {
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
