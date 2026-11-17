package com.smarttraffic.model.enums;

public enum RiskLevel {
    LOW("Low Congestion / Safe"),
    MEDIUM("Moderate Traffic / Caution"),
    HIGH("High Risk / Bottleneck Zone");

    private final String description;

    RiskLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name();
    }
}
