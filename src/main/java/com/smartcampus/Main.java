package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    // this is the base path the API should run on
    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static void main(String[] args) throws IOException {

        ResourceConfig rc = new ResourceConfig();

        // JSON support
        rc.register(JacksonFeature.class);

        // resources
        rc.register(com.smartcampus.resource.DiscoveryResource.class);
        rc.register(com.smartcampus.resource.RoomResource.class);
        rc.register(com.smartcampus.resource.SensorResource.class);

        // exception mappers
        rc.register(com.smartcampus.exception.RoomNotEmptyExceptionMapper.class);
        rc.register(com.smartcampus.exception.LinkedResourceNotFoundExceptionMapper.class);
        rc.register(com.smartcampus.exception.SensorUnavailableExceptionMapper.class);
        rc.register(com.smartcampus.exception.GlobalExceptionMapper.class);

        // filters
        rc.register(com.smartcampus.filter.ApiLoggingFilter.class);

        // mount the server directly on the versioned API base path
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);

        LOGGER.info("Smart Campus API running at: " + BASE_URI);
        LOGGER.info("Press ENTER to stop the server");

        System.in.read();
        server.shutdownNow();
    }
}
