package com.smartcampus.model;

public class SensorReading {
    private String id;
    private String sensorId;
    private double value;
    private long timestamp;

    public SensorReading() {}

    public SensorReading(String id, String sensorId, double value) {
        this.id = id;
        this.sensorId = sensorId;
        this.value = value;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
