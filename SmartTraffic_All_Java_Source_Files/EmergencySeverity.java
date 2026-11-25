package com.smarttraffic.model.enums;

public enum EmergencySeverity {
    MINOR("Minor (No injury, minor vehicle scrape)"),
    MAJOR("Major (Injury, significant collision, road obstruction)"),
    CRITICAL("Critical (Fatalities / Multiple casualties / Fire risk)");

    private final String description;

    EmergencySeverity(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name() + " - " + description;
    }
}
