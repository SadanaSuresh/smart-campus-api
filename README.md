# Smart Campus Sensor & Room Management API

A RESTful API built with JAX-RS (Jersey) and Grizzly for managing campus rooms and IoT sensors.

**Module:** 5COSC022W — Client-Server Architectures  
**Student ID:** w21162895  
**University of Westminster**

---

## API Overview

This API provides a backend service for the University Smart Campus initiative. It allows facilities managers to manage rooms and the sensors deployed within them, including temperature monitors, CO2 sensors, and occupancy trackers.

### Base URL
```
http://localhost:8080/api/v1
```

### Resources
| Resource | Path |
|----------|------|
| Discovery | `GET /api/v1` |
| Rooms | `/api/v1/rooms` |
| Sensors | `/api/v1/sensors` |
| Sensor Readings | `/api/v1/sensors/{id}/readings` |

---

## Tech Stack

- **Java 11+**
- **JAX-RS** (Jersey 2.39.1)
- **Grizzly** HTTP Server (embedded)
- **Jackson** for JSON serialisation
- **Maven** for build management
- In-memory storage using `ConcurrentHashMap` (no database required)

---

## How to Build and Run

### Prerequisites
- Java 11 or higher
- Apache NetBeans 18

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/sadanasuresh-01/smart-campus-api.git
cd smart-campus-api
```

**2. Open in NetBeans**
- File → Open Project → select the project folder
- Right-click project → Build

**3. Run the server**
- Right-click project → Run

**4. The server starts at:**
```
http://localhost:8080/api/v1
```

Press **ENTER** in the output panel to stop the server.

---

## Sample curl Commands

### 1. Discovery — GET API metadata
```bash
curl http://localhost:8080/api/v1
```

### 2. Get all rooms
```bash
curl http://localhost:8080/api/v1/rooms
```

### 3. Create a new room
```bash
curl -X POST http://localhost:8080/api/v1/rooms -H "Content-Type: application/json" -d "{\"id\":\"CS-101\",\"name\":\"CS Lab\",\"capacity\":30}"
```

### 4. Get sensors filtered by type
```bash
curl http://localhost:8080/api/v1/sensors?type=Temperature
```

### 5. Post a sensor reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings -H "Content-Type: application/json" -d "{\"value\":24.5}"
```

### 6. Attempt to delete a room with sensors (expect 409 Conflict)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 7. Post a reading to a MAINTENANCE sensor (expect 403 Forbidden)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings -H "Content-Type: application/json" -d "{\"value\":10}"
```

---

## Seeded Test Data

The API starts with the following data pre-loaded:

**Rooms:**
| ID | Name | Capacity |
|----|------|----------|
| LIB-301 | Library Quiet Study | 40 |
| ENG-101 | Engineering Lab A | 25 |

**Sensors:**
| ID | Type | Status | Room |
|----|------|--------|------|
| TEMP-001 | Temperature | ACTIVE | LIB-301 |
| CO2-001 | CO2 | ACTIVE | ENG-101 |
| OCC-001 | Occupancy | MAINTENANCE | LIB-301 |

---

## HTTP Status Codes Used

| Code | Meaning | When |
|------|---------|------|
| 200 OK | Success | GET requests |
| 201 Created | Resource created | POST requests |
| 204 No Content | Deleted | DELETE requests |
| 400 Bad Request | Invalid input | Missing required fields |
| 403 Forbidden | Not allowed | POST to MAINTENANCE sensor |
| 404 Not Found | Not found | Resource does not exist |
| 409 Conflict | Conflict | Delete room with sensors |
| 422 Unprocessable Entity | Bad reference | Sensor with non-existent roomId |
| 500 Internal Server Error | Server error | Unexpected errors |

---

## Project Structure

```
src/main/java/com/smartcampus/
├── Main.java                                      # Server bootstrap
├── SmartCampusApplication.java                    # @ApplicationPath("/api/v1")
├── DataStore.java                                 # In-memory data store
├── model/
│   ├── Room.java
│   ├── Sensor.java
│   └── SensorReading.java
├── resource/
│   ├── DiscoveryResource.java                     # GET /api/v1
│   ├── RoomResource.java                          # GET/POST/DELETE /rooms
│   ├── SensorResource.java                        # GET/POST /sensors
│   └── SensorReadingResource.java                 # GET/POST /sensors/{id}/readings
├── exception/
│   ├── RoomNotEmptyException.java
│   ├── RoomNotEmptyExceptionMapper.java           # 409 Conflict
│   ├── LinkedResourceNotFoundException.java
│   ├── LinkedResourceNotFoundExceptionMapper.java # 422 Unprocessable Entity
│   ├── SensorUnavailableException.java
│   ├── SensorUnavailableExceptionMapper.java      # 403 Forbidden
│   └── GlobalExceptionMapper.java                 # 500 catch-all
└── filter/
    └── ApiLoggingFilter.java                      # Request/response logging
```

---

## Conceptual Report

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a new instance of each resource class for every incoming HTTP request (request-scoped lifecycle). This means resource classes are NOT singletons. Each time a request arrives, Jersey instantiates the relevant resource class, processes the request, returns the response, and discards the instance.

This has a direct implication for in-memory data management. Because resource instances are not shared between requests, shared application state must be stored in a separate singleton class — in this implementation, the DataStore class, which uses static ConcurrentHashMap fields. ConcurrentHashMap was chosen because multiple threads may handle simultaneous requests, providing thread-safe access without explicit synchronisation blocks.

### Part 1.2 — HATEOAS and the Discovery Endpoint

HATEOAS (Hypermedia as the Engine of Application State) is considered a hallmark of advanced RESTful design because it makes an API self-describing and navigable. Rather than requiring a client to have prior knowledge of all available URLs, a HATEOAS-compliant response includes links that guide the client to related resources. The GET /api/v1 Discovery endpoint returns links to the /rooms and /sensors collections, allowing clients to explore the entire API from the base URL without consulting external documentation.

### Part 2.1 — Returning Full Objects vs. IDs Only

Returning only IDs forces the client to make N additional GET requests to retrieve meaningful data (the N+1 problem), increasing latency and server load. Returning full objects requires a larger initial payload but eliminates follow-up requests. For a campus management system where administrators need to view all rooms at a glance, returning full Room objects is the superior choice.

### Part 2.2 — Idempotency of DELETE

The first DELETE /api/v1/rooms/{id} request removes the room and returns 204 No Content. Subsequent identical requests return 404 Not Found because the room no longer exists. This satisfies idempotency because the outcome — the room does not exist — is identical after every call. Idempotency refers to the effect on the resource, not the response code.

### Part 3.1 — @Consumes and Content-Type Mismatches

The @Consumes(MediaType.APPLICATION_JSON) annotation declares that POST endpoints only accept requests with Content-Type: application/json. If a client sends text/plain or application/xml, JAX-RS automatically returns HTTP 415 Unsupported Media Type before the method is even invoked, enforcing strict contract at the API boundary.

### Part 3.2 — @QueryParam vs. Path Parameter for Filtering

Path parameters are semantically reserved for identifying a specific resource (e.g., /sensors/TEMP-001). A type filter does not identify a single resource — it restricts a collection. Query parameters are optional by nature, allowing GET /sensors to work with or without the filter. They also compose naturally for multiple filters (e.g., ?type=CO2&status=ACTIVE) without changing the path structure.

### Part 4.1 — Benefits of the Sub-Resource Locator Pattern

The Sub-Resource Locator pattern delegates /sensors/{id}/readings to a dedicated SensorReadingResource class. This enforces separation of concerns — each class has a single, well-defined responsibility. It improves testability, since each resource class can be unit-tested independently, and makes the codebase easier to maintain as the API grows.

### Part 5.2 — Why HTTP 422 is More Accurate than 404

HTTP 404 implies the endpoint was not found, but /api/v1/sensors exists and functions correctly. HTTP 422 Unprocessable Entity communicates that the server understood the request format but the payload contains a logical error — a reference to a non-existent room. This gives client developers a clearer signal to correct their payload data rather than their URL.

### Part 5.4 — Cybersecurity Risks of Exposing Stack Traces

Exposing raw Java stack traces reveals internal class names and package structure, framework and library versions (enabling CVE lookups), server file paths, and business logic details. The GlobalExceptionMapper intercepts all unhandled Throwable instances and returns a generic HTTP 500 response, logging full details server-side only.

### Part 5.5 — Advantages of JAX-RS Filters for Logging

Filters implement the cross-cutting concerns principle — logging applies to every request regardless of which resource handles it. Manual Logger.info() statements inside resource methods are error-prone (a developer might forget to add them to new endpoints) and hard to maintain. Filters guarantee complete, consistent observability across the entire API surface automatically.
