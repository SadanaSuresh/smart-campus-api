package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @GET
    public List<Sensor> getSensors(@QueryParam("type") String type) {
        List<Sensor> list = new ArrayList<>(DataStore.sensors.values());
        if (type != null && !type.isBlank())
            list = list.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        return list;
    }

    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor.getId() == null || sensor.getId().isBlank())
            return Response.status(400).entity(err("Sensor ID is required")).build();
        if (DataStore.sensors.containsKey(sensor.getId()))
            return Response.status(409).entity(err("Sensor already exists: " + sensor.getId())).build();
        if (sensor.getRoomId() == null || !DataStore.rooms.containsKey(sensor.getRoomId()))
            throw new LinkedResourceNotFoundException("Room", sensor.getRoomId());
        if (sensor.getStatus() == null)
            sensor.setStatus("ACTIVE");

        DataStore.sensors.put(sensor.getId(), sensor);
        DataStore.readings.put(sensor.getId(), new ArrayList<>());

        Room room = DataStore.rooms.get(sensor.getRoomId());
        room.getSensorIds().add(sensor.getId());

        URI location = URI.create("http://localhost:8080/api/v1/sensors/" + sensor.getId());
        return Response.created(location).entity(sensor).build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null)
            return Response.status(404).entity(err("Sensor not found: " + sensorId)).build();
        return Response.ok(sensor).build();
    }

    /**
     * Sub-resource locator for readings.
     * Delegates all /sensors/{sensorId}/readings requests to SensorReadingResource.
     * This implements the Sub-Resource Locator pattern as required by Part 4.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    private Map<String, String> err(String msg) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("error", msg);
        return m;
    }
}
