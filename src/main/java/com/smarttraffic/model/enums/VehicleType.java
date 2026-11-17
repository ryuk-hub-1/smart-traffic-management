package com.smarttraffic.model.enums;

public enum VehicleType {
    TWO_WHEELER("Two-Wheeler (Motorcycle/Scooter)"),
    FOUR_WHEELER_CAR("Four-Wheeler (Car/SUV/Sedan)"),
    COMMERCIAL_AUTO("Commercial Auto Rickshaw / Taxi"),
    HEAVY_VEHICLE("Heavy Vehicle (Truck/Bus/Tanker)"),
    ELECTRIC_VEHICLE("Electric Vehicle (EV)");

    private final String label;

    VehicleType(String label) {
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
