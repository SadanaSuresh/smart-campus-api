package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @GET
    public List<Sensor> getSensors(@QueryParam("type") String type) {

        Collection<Sensor> sensors = DataStore.sensors.values();

        if (type == null)
            return new ArrayList<>(sensors);

        // allows filtering by type eg Temperature or CO2
        return sensors.stream()
                .filter(s -> type.equalsIgnoreCase(s.getType()))
                .collect(Collectors.toList());
    }

    @POST
    public Response createSensor(Sensor sensor) {

        if (sensor == null)
            return Response.status(400).entity(error("Request body missing")).build();

        if (sensor.getId() == null || sensor.getId().isBlank())
            return Response.status(400).entity(error("Sensor id required")).build();

        if (!DataStore.rooms.containsKey(sensor.getRoomId()))
            throw new LinkedResourceNotFoundException("Room", sensor.getRoomId());

        if (DataStore.sensors.putIfAbsent(sensor.getId(), sensor) != null)
            return Response.status(409).entity(error("Sensor already exists")).build();

        if (sensor.getStatus() == null)
            sensor.setStatus("ACTIVE");

        // link sensor to room
        DataStore.rooms.get(sensor.getRoomId())
                .getSensorIds()
                .add(sensor.getId());

        // create empty list to store future readings
        DataStore.readings.put(sensor.getId(),
                Collections.synchronizedList(new ArrayList<>()));

        URI location = URI.create("http://localhost:8080/api/v1/sensors/" + sensor.getId());

        return Response.created(location)
                .entity(sensor)
                .build();
    }

    private Map<String,String> error(String message){

        Map<String,String> body = new LinkedHashMap<>();
        body.put("error", message);

        return body;
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(
            @PathParam("sensorId") String sensorId){

        return new SensorReadingResource(sensorId);
    }
}
