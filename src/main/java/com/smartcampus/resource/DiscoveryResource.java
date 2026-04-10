package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discovery endpoint at GET /api/v1
 * Returns API metadata including versioning, contact details,
 * and links to primary resource collections (HATEOAS).
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Map<String, Object> discover() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", "Smart Campus Sensor & Room Management API");
        info.put("version", "1.0.0");
        info.put("description", "A RESTful API for managing campus rooms and IoT sensors.");
        info.put("contact", "admin@smartcampus.ac.uk");
        info.put("baseUrl", "http://localhost:8080/api/v1");

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms",    "http://localhost:8080/api/v1/rooms");
        resources.put("sensors",  "http://localhost:8080/api/v1/sensors");
        resources.put("discovery","http://localhost:8080/api/v1");
        info.put("resources", resources);

        return info;
    }
}
