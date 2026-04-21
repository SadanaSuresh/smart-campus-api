package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.Main;
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
            return Response.status(404).entity(errorBody(404, "Not Found", "Sensor not found: " + sensorId)).build();

        List<SensorReading> history = DataStore.readings.getOrDefault(sensorId, new ArrayList<>());

        return Response.ok(history).build();
    }

    @POST
    public Response addReading(SensorReading reading) {

        if (reading == null)
            return Response.status(400).entity(errorBody(400, "Bad Request", "Request body is missing")).build();

        Sensor sensor = DataStore.sensors.get(sensorId);

        if (sensor == null)
            return Response.status(404).entity(errorBody(404, "Not Found", "Sensor not found: " + sensorId)).build();

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus()))
            throw new SensorUnavailableException(sensorId);

        String readingId = "R-" + System.currentTimeMillis();
        SensorReading newReading = new SensorReading(readingId, sensorId, reading.getValue());

        DataStore.readings.get(sensorId).add(newReading);

        // keep the latest sensor value in sync with the newest reading
        sensor.setCurrentValue(newReading.getValue());

        URI location = URI.create(Main.BASE_URI + "sensors/" + sensorId + "/readings/" + readingId);

        return Response.created(location).entity(newReading).build();
    }

    private Map<String, Object> errorBody(int status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        return body;
    }
}
