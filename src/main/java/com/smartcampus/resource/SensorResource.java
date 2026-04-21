package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.Main;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
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

        // query parameter is used here because this is filtering a collection, not identifying one resource
        if (type != null && !type.isBlank()) {
            list = list.stream()
                    .filter(sensor -> sensor.getType() != null && sensor.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        return list;
    }

    @POST
    public Response createSensor(Sensor sensor) {

        if (sensor == null)
            return Response.status(400).entity(errorBody(400, "Bad Request", "Request body is missing")).build();

        if (sensor.getId() == null || sensor.getId().isBlank())
            return Response.status(400).entity(errorBody(400, "Bad Request", "Sensor id is required")).build();

        if (sensor.getType() == null || sensor.getType().isBlank())
            return Response.status(400).entity(errorBody(400, "Bad Request", "Sensor type is required")).build();

        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank())
            return Response.status(400).entity(errorBody(400, "Bad Request", "roomId is required")).build();

        if (DataStore.sensors.containsKey(sensor.getId()))
            return Response.status(409).entity(errorBody(409, "Conflict", "Sensor already exists: " + sensor.getId())).build();

        if (!DataStore.rooms.containsKey(sensor.getRoomId()))
            throw new LinkedResourceNotFoundException("Room", sensor.getRoomId());

        // if no status is given, ACTIVE is the default
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        } else {
            String status = sensor.getStatus().toUpperCase();

            if (!status.equals("ACTIVE") && !status.equals("MAINTENANCE") && !status.equals("OFFLINE")) {
                return Response.status(400)
                        .entity(errorBody(400, "Bad Request", "Sensor status must be ACTIVE, MAINTENANCE, or OFFLINE"))
                        .build();
            }

            sensor.setStatus(status);
        }

        DataStore.sensors.put(sensor.getId(), sensor);
        DataStore.readings.put(sensor.getId(), Collections.synchronizedList(new ArrayList<>()));

        // once the sensor is created, also link it back to the room
        Room room = DataStore.rooms.get(sensor.getRoomId());
        room.getSensorIds().add(sensor.getId());

        URI location = URI.create(Main.BASE_URI + "sensors/" + sensor.getId());

        return Response.created(location).entity(sensor).build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {

        Sensor sensor = DataStore.sensors.get(sensorId);

        if (sensor == null)
            return Response.status(404).entity(errorBody(404, "Not Found", "Sensor not found: " + sensorId)).build();

        return Response.ok(sensor).build();
    }

    /*
     * This sub resource locator passes nested reading requests to a separate class.
     * It keeps SensorResource focused only on sensor level operations.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    private Map<String, Object> errorBody(int status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        return body;
    }
}
