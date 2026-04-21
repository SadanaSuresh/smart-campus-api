package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId){
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings(){

        List<SensorReading> history = DataStore.readings.get(sensorId);

        if (history == null)
            return Response.status(404).entity(error("Sensor not found")).build();

        return Response.ok(history).build();
    }

    @POST
    public Response addReading(SensorReading reading){

        Sensor sensor = DataStore.sensors.get(sensorId);

        if (sensor == null)
            return Response.status(404).entity(error("Sensor not found")).build();

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus()))
            throw new SensorUnavailableException(sensorId);

        reading.setTimestamp(System.currentTimeMillis());

        DataStore.readings.get(sensorId).add(reading);

        // update latest sensor value
        sensor.setCurrentValue(reading.getValue());

        return Response.status(201)
                .entity(reading)
                .build();
    }

    private Map<String,String> error(String message){

        Map<String,String> body = new LinkedHashMap<>();
        body.put("error", message);

        return body;
    }
}
