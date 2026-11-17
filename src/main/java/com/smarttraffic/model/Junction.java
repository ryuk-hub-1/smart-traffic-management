package com.smarttraffic.model;

import com.smarttraffic.model.enums.RiskLevel;

/**
 * Traffic Junction / Intersection model.
 */
public class Junction {
    private int id;
    private String junctionCode;
    private String name;
    private String area;
    private String landmark;
    private double latitude;
    private double longitude;
    private RiskLevel riskLevel;
    private int activeOfficersCount;

    public Junction() {
        this.riskLevel = RiskLevel.MEDIUM;
    }

    public Junction(int id, String junctionCode, String name, String area,
                    String landmark, double latitude, double longitude, RiskLevel riskLevel) {
        this.id = id;
        this.junctionCode = junctionCode;
        this.name = name;
        this.area = area;
        this.landmark = landmark;
        this.latitude = latitude;
        this.longitude = longitude;
        this.riskLevel = riskLevel != null ? riskLevel : RiskLevel.MEDIUM;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getJunctionCode() {
        return junctionCode;
    }

    public void setJunctionCode(String junctionCode) {
        this.junctionCode = junctionCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getLandmark() {
        return landmark;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public int getActiveOfficersCount() {
        return activeOfficersCount;
    }

    public void setActiveOfficersCount(int activeOfficersCount) {
        this.activeOfficersCount = activeOfficersCount;
    }

    public String getDisplayName() {
        return junctionCode + " - " + name + " (" + area + ")";
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
