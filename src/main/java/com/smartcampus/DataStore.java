package com.smartcampus;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory data store.
 * Uses ConcurrentHashMap to handle concurrent requests safely.
 * Static fields ensure data persists across request-scoped resource instances.
 */
public class DataStore {

    public static final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, ArrayList<SensorReading>> readings = new ConcurrentHashMap<>();

    static {
        // Seed rooms
        Room lib = new Room("LIB-301", "Library Quiet Study", 40);
        Room eng = new Room("ENG-101", "Engineering Lab A", 25);

        // Seed sensors
        Sensor temp = new Sensor("TEMP-001", "Temperature", "ACTIVE", "LIB-301", 22.5);
        Sensor co2  = new Sensor("CO2-001",  "CO2",         "ACTIVE", "ENG-101", 400.0);
        Sensor occ  = new Sensor("OCC-001",  "Occupancy",   "MAINTENANCE", "LIB-301", 0.0);

        lib.getSensorIds().add("TEMP-001");
        lib.getSensorIds().add("OCC-001");
        eng.getSensorIds().add("CO2-001");

        rooms.put(lib.getId(), lib);
        rooms.put(eng.getId(), eng);
        sensors.put(temp.getId(), temp);
        sensors.put(co2.getId(), co2);
        sensors.put(occ.getId(), occ);

        readings.put("TEMP-001", new ArrayList<>());
        readings.put("CO2-001",  new ArrayList<>());
        readings.put("OCC-001",  new ArrayList<>());
    }
}
