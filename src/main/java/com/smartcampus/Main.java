package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static void main(String[] args) throws IOException {
        ResourceConfig rc = ResourceConfig.forApplicationClass(SmartCampusApplication.class);
        rc.register(JacksonFeature.class);
        rc.register(com.smartcampus.resource.DiscoveryResource.class);
        rc.register(com.smartcampus.resource.RoomResource.class);
        rc.register(com.smartcampus.resource.SensorResource.class);
        rc.register(com.smartcampus.exception.RoomNotEmptyExceptionMapper.class);
        rc.register(com.smartcampus.exception.LinkedResourceNotFoundExceptionMapper.class);
        rc.register(com.smartcampus.exception.SensorUnavailableExceptionMapper.class);
        rc.register(com.smartcampus.exception.GlobalExceptionMapper.class);
        rc.register(com.smartcampus.filter.ApiLoggingFilter.class);

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create("http://localhost:8080/"), rc);
        LOGGER.info("Smart Campus API running at: " + BASE_URI);
        LOGGER.info("Press ENTER to stop...");
        System.in.read();
        server.shutdownNow();
    }
}
