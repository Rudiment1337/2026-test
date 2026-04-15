package com.busmonitor.model;

public enum SensorType {
    temp_lever("temp_level", 70.0, 100.0, 115.0),
    pressure("pressure", 2.2, 3.5, 4.0),
    fuel_level("fuel_level", 40.0, 99.0, 99.0),
    speed("speed", 0.0, 90.0, 110.0);

    private final String type;
    private final double minNormal;
    private final double warningThreshold;
    private final double errorThreshold;

    SensorType(String type, double minNormal, double warningThreshold, double errorThreshold) {
        this.type = type;
        this.minNormal = minNormal;
        this.warningThreshold = warningThreshold;
        this.errorThreshold = errorThreshold;
    }

    public String getType() { return type; }
    public double getMinNormal() { return minNormal; }
    public double getWarningThreshold() { return warningThreshold; }
    public double getErrorThreshold() { return errorThreshold; }
}
