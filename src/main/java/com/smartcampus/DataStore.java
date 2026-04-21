package com.smartcampus;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Acts as a simple in-memory store for the API.
 * Static fields are used so data is shared across requests.
 * ConcurrentHashMap allows multiple requests to safely access the data at the same time.
 */
public class DataStore {

    public static final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();

    // each sensor has its own list of readings
    // wrapping the list makes adding readings safer when multiple requests happen close together
    public static final ConcurrentHashMap<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    static {

        // sample rooms
        Room lib = new Room("LIB-301", "Library Quiet Study", 40);
        Room eng = new Room("ENG-101", "Engineering Lab A", 25);

        // sample sensors
        Sensor temp = new Sensor("TEMP-001", "Temperature", "ACTIVE", "LIB-301", 22.5);
        Sensor co2 = new Sensor("CO2-001", "CO2", "ACTIVE", "ENG-101", 400.0);
        Sensor occ = new Sensor("OCC-001", "Occupancy", "MAINTENANCE", "LIB-301", 0.0);

        // link sensors to rooms
        lib.getSensorIds().add("TEMP-001");
        lib.getSensorIds().add("OCC-001");
        eng.getSensorIds().add("CO2-001");

        rooms.put(lib.getId(), lib);
        rooms.put(eng.getId(), eng);

        sensors.put(temp.getId(), temp);
        sensors.put(co2.getId(), co2);
        sensors.put(occ.getId(), occ);

        // start each sensor with an empty history list
        readings.put("TEMP-001", Collections.synchronizedList(new ArrayList<>()));
        readings.put("CO2-001", Collections.synchronizedList(new ArrayList<>()));
        readings.put("OCC-001", Collections.synchronizedList(new ArrayList<>()));
    }

    private DataStore() {
        // stops this class being created accidentally
    }
}
