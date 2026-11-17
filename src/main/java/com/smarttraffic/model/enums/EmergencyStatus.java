package com.smarttraffic.model.enums;

public enum EmergencyStatus {
    REPORTED("Reported"),
    ASSISTANCE_DISPATCHED("Emergency Units Dispatched"),
    INVESTIGATING("Under Investigation"),
    CLEARED("Cleared / Resolved");

    private final String label;

    EmergencyStatus(String label) {
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
