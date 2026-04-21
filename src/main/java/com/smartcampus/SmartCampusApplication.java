package com.smartcampus;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application configuration.
 * The @ApplicationPath annotation defines the base URI for all REST endpoints.
 * JAX-RS creates a NEW instance of each resource class per HTTP request
 * (request-scoped). This means resource classes are NOT singletons.
 * Shared in-memory data is stored in the DataStore singleton class using
 * ConcurrentHashMap to prevent race conditions.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // Resource registration is handled in Main.java via ResourceConfig
}
