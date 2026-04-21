package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException e) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("message", "Sensor '" + e.getSensorId() + "' is under maintenance and cannot accept new readings.");

        return Response.status(403)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
