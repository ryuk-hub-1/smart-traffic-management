package com.smarttraffic.model.enums;

public enum ShiftType {
    MORNING("Morning Shift", "06:00 AM - 02:00 PM"),
    EVENING("Evening Shift", "02:00 PM - 10:00 PM"),
    NIGHT("Night Shift", "10:00 PM - 06:00 AM");

    private final String title;
    private final String timing;

    ShiftType(String title, String timing) {
        this.title = title;
        this.timing = timing;
    }

    public String getTitle() {
        return title;
    }

    public String getTiming() {
        return timing;
    }

    @Override
    public String toString() {
        return title + " (" + timing + ")";
    }
}
