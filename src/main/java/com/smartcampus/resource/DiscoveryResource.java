package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * This is the base discovery endpoint for the API.
 * It gives clients the main links they need so they can navigate from the root.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Map<String, Object> discover() {

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", "Smart Campus Sensor & Room Management API");
        info.put("version", "1.0.0");
        info.put("description", "A RESTful API for managing campus rooms, sensors, and sensor readings.");
        info.put("contact", "admin@smartcampus.ac.uk");
        info.put("baseUrl", "http://localhost:8080/api/v1");

        Map<String, String> resources = new LinkedHashMap<>();

        // these are the main entry points a client would usually look for first
        resources.put("discovery", "http://localhost:8080/api/v1");
        resources.put("rooms", "http://localhost:8080/api/v1/rooms");
        resources.put("sensors", "http://localhost:8080/api/v1/sensors");
        resources.put("sensorReadings", "http://localhost:8080/api/v1/sensors/{sensorId}/readings");

        info.put("resources", resources);

        return info;
    }
}
