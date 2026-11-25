package com.smarttraffic.model.enums;

public enum Role {
    ADMIN("Administrator"),
    OFFICER("Traffic Police Officer"),
    CITIZEN("Citizen");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
