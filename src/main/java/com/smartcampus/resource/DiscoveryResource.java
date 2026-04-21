package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Simple discovery endpoint for the base API path.
 * It gives clients a quick summary of the API and the main resource links.
 * This supports the idea of HATEOAS because the client can navigate using links in the response.
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

        // these are the main entry points a client would usually need first
        resources.put("discovery", "http://localhost:8080/api/v1");
        resources.put("rooms", "http://localhost:8080/api/v1/rooms");
        resources.put("sensors", "http://localhost:8080/api/v1/sensors");

        // readings are nested under a specific sensor, so this is shown as a template
        resources.put("sensorReadings", "http://localhost:8080/api/v1/sensors/{sensorId}/readings");

        info.put("resources", resources);

        return info;
    }
}
