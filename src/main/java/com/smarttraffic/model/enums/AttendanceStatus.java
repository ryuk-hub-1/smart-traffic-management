package com.smarttraffic.model.enums;

public enum AttendanceStatus {
    PRESENT("Present"),
    LATE("Late"),
    ON_DUTY("On Active Duty"),
    ABSENT("Absent"),
    ON_LEAVE("On Leave");

    private final String label;

    AttendanceStatus(String label) {
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
