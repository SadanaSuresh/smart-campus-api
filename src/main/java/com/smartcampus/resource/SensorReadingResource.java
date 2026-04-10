package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sub-resource class for managing sensor readings.
 * Handles GET and POST for /api/v1/sensors/{sensorId}/readings
 * This class is returned by the sub-resource locator in SensorResource.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        if (!DataStore.sensors.containsKey(sensorId))
            return Response.status(404).entity(err("Sensor not found: " + sensorId)).build();
        List<SensorReading> list = DataStore.readings.getOrDefault(sensorId, new ArrayList<>());
        return Response.ok(list).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null)
            return Response.status(404).entity(err("Sensor not found: " + sensorId)).build();
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus()))
            throw new SensorUnavailableException(sensorId);

        String rid = "R-" + System.currentTimeMillis();
        SensorReading r = new SensorReading(rid, sensorId, reading.getValue());
        DataStore.readings.get(sensorId).add(r);

        // Side effect: update parent sensor's currentValue
        sensor.setCurrentValue(reading.getValue());

        URI location = URI.create("http://localhost:8080/api/v1/sensors/" + sensorId + "/readings/" + rid);
        return Response.created(location).entity(r).build();
    }

    private Map<String, String> err(String msg) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("error", msg);
        return m;
    }
}
