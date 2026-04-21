# Smart Campus Sensor & Room Management API

A RESTful API built with JAX-RS (Jersey) and Grizzly for managing campus rooms and IoT sensors.

Module: 5COSC022W — Client-Server Architectures
Student ID: w21162895
University of Westminster

---

## API Overview

This API provides a backend service for the University Smart Campus initiative. It allows for managing rooms and the sensors deployed within them, including temperature monitors, CO2 sensors, and occupancy trackers.

### Base URL
http://localhost:8080/api/v1

### Resources
| Resource | Path |
|----------|------|
| Discovery | GET /api/v1 |
| Rooms | /api/v1/rooms |
| Sensors | /api/v1/sensors |
| Sensor Readings | /api/v1/sensors/{id}/readings |

---

## Tech Stack

- Java 11+
- JAX-RS (Jersey 2.39.1)
- Grizzly HTTP Server (embedded)
- Jackson for JSON serialisation
- Maven for build management
- In-memory storage using ConcurrentHashMap (no database required)

---

## How to Build and Run

### Prerequisites
- Java 11 or higher
- Apache NetBeans 18

### Steps

1. Clone the repository
git clone https://github.com/sadanasuresh-01/smart-campus-api.git
cd smart-campus-api

2. Open in NetBeans
- File → Open Project → select the project folder
- Right-click project → Build

3. Run the server
- Right-click project → Run

4. The server starts at: http://localhost:8080/api/v1

Press ENTER in the output panel to stop the server.

### Alternative — command line
mvn clean package
java -jar target/smart-campus-api-1.0.0.jar

---

## Sample curl Commands

### 1. Discovery — GET API metadata
curl http://localhost:8080/api/v1

### 2. Get all rooms
curl http://localhost:8080/api/v1/rooms

### 3. Create a new room
curl -X POST http://localhost:8080/api/v1/rooms -H "Content-Type: application/json" -d "{\"id\":\"CS-101\",\"name\":\"CS Lab\",\"capacity\":30}"

### 4. Get sensors filtered by type
curl http://localhost:8080/api/v1/sensors?type=Temperature

### 5. Post a sensor reading
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings -H "Content-Type: application/json" -d "{\"value\":24.5}"

### 6. Attempt to delete a room with sensors (expect 409 Conflict)
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301

### 7. Post a reading to a MAINTENANCE sensor (expect 403 Forbidden)
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings -H "Content-Type: application/json" -d "{\"value\":10}"

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

## Key Design Decisions

Several design decisions were made to keep the API simple, consistent and aligned with REST principles.

The API follows a resource-oriented structure where rooms, sensors and sensor readings are treated as separate resources with clear relationships between them. Rooms represent physical spaces, sensors belong to rooms, and readings belong to sensors. This hierarchical structure reflects the real-world domain model of a smart campus.

An in-memory data store was intentionally used instead of a database to keep the project focused on API design rather than persistence configuration. ConcurrentHashMap ensures thread safety when multiple requests modify shared data at the same time. This simulates behaviour expected in a real multi-user environment.

HTTP status codes were carefully selected to communicate meaning clearly to API clients. For example, 409 Conflict is returned when attempting to delete a room that still contains sensors because the request is valid but violates a business rule. Similarly, 422 Unprocessable Entity is used when a sensor references a room that does not exist, indicating a logical error in the request body rather than a missing endpoint.

Sub-resources were used for sensor readings to reflect containment relationships. A reading cannot exist independently without a sensor. Using the path /sensors/{id}/readings makes this relationship explicit and improves API clarity.

Consistent JSON response structures were applied for both success and error responses to make the API predictable and easier to integrate with client applications.

---
## Conceptual Report

### Part 1.1 - JAX-RS Resource Lifecycle

In JAX-RS, every request gets a brand new instance of the resource class. Once the response is sent, that instance is discarded. This is the request-scoped lifecycle and it is the default behaviour. Resource classes are not singletons. They do not hold onto data between requests.

This created a storage problem. Every resource object disappears after each request, so you cannot store rooms or sensors inside those classes. The data would be gone instantly. To fix this, a separate DataStore class was built to hold everything in static fields. Static fields belong to the class itself, not to any instance. The data stays alive for as long as the server runs.

ConcurrentHashMap was chosen over a regular HashMap because multiple requests arrive at the same time and run on separate threads. A regular HashMap breaks under those conditions. Two threads reading and writing at the same time produces wrong data or crashes. ConcurrentHashMap handles this safely without locking the entire structure.

### Part 1.2 - HATEOAS and the Discovery Endpoint

HATEOAS stands for Hypermedia as the Engine of Application State. The principle is simple. An API should include links in its responses so clients can navigate without knowing every URL in advance. This is widely considered good REST design.

In this project, calling GET /api/v1 returns a JSON object with the URLs for the rooms and sensors collections. A developer with only the base URL makes one request and finds everything else.Hardcoded URLs create tight coupling between client and server. If the API structure changes, every client must be updated. HATEOAS reduces this coupling by allowing clients to dynamically discover valid endpoints directly from the API response. With this approach, clients always get accurate links directly from the response.

### Part 2.1 - Returning Full Objects vs IDs Only

When a client calls GET /api/v1/rooms, there are two options. Send back only the room IDs, or send back the full room objects. Both have trade-offs.

Sending only IDs keeps the response small. But the client then needs a separate GET request for each room to get the name and capacity. For 100 rooms that is 100 extra requests. This is the N+1 problem. It slows everything down and puts unnecessary load on the server.

Sending full objects produces a larger first response. But the client gets everything in one call. For a system where a manager needs to see all rooms at once, this is the right approach. The increase in payload size is minimal compared to the performance cost of repeated HTTP round trips. Reducing the number of requests improves responsiveness and reduces server overhead. Cutting out all those extra round trips makes the experience faster.

### Part 2.2 - Idempotency of DELETE

Idempotency means sending the same request multiple times produces the same server state as sending it once. This is about server state, not response codes.

The first DELETE to /api/v1/rooms/{id} removes the room and returns 204 No Content. Sending the same request again returns 404 Not Found because the room is already gone. The response codes differ but the server state is identical after both calls. The room does not exist either way. This satisfies the idempotency requirement in the HTTP specification.

If a client accidentally sends the same DELETE twice due to a timeout or retry, nothing breaks on the server. The outcome is consistent no matter how many times the request is sent.

### Part 3.1 - The @Consumes Annotation and Content-Type Mismatches

Adding @Consumes(MediaType.APPLICATION_JSON) to a POST method tells JAX-RS to only accept requests where the Content-Type header is application/json. This sets a firm rule at the API level about what format incoming data must be in.

If a client sends data with the wrong Content-Type like text/plain or application/xml, JAX-RS blocks the request before the method runs. It sends back a 415 Unsupported Media Type response automatically. The method body never executes. There is no need to write manual format checking inside the method. JAX-RS takes care of it.

From a design point of view, this keeps the resource code focused on business logic, improves reliability, and gives clients a clear error when they send the wrong format.

### Part 3.2 - @QueryParam vs Path Parameter for Filtering

When adding type filtering to the sensors endpoint, a choice had to be made. Put the filter in the URL path like /api/v1/sensors/type/CO2, or use a query parameter like /api/v1/sensors?type=CO2. The query parameter approach was chosen.

Path parameters point to a specific resource. Something like /sensors/TEMP-001 identifies one sensor by its ID. A type filter does not identify a specific resource. It narrows down a list. Putting a filter in the path gives it a meaning it was not built for and breaks standard REST conventions.

Query parameters are designed for optional filtering. They do not change what resource is being accessed. The endpoint works with or without the filter. And combining multiple filters like ?type=CO2&status=ACTIVE requires no changes to the URL structure at all. The API stays clean and easy to extend.

### Part 4.1 - The Sub-Resource Locator Pattern

All reading logic was not placed inside SensorResource. A separate SensorReadingResource class was created to handle reading history instead. Inside SensorResource, a locator method with a @Path annotation hands off requests for the readings path to an instance of SensorReadingResource. JAX-RS uses that class to process the request.

Each class has one job. SensorResource handles sensors. SensorReadingResource handles reading history. If the reading logic changes, only SensorReadingResource gets updated. There is no risk of breaking anything in SensorResource. Each class can be tested independently. The codebase stays manageable as the project grows.

This separation improves maintainability because future changes to reading logic can be implemented without affecting sensor endpoints.

### Part 4.2 - Why HTTP 422 is More Appropriate Than 404

When a client tries to register a sensor with a roomId that does not exist, returning 404 Not Found would be wrong. A 404 tells the client the URL does not exist. But /api/v1/sensors is working fine. The problem is not the URL. The problem is the data inside the request body.

HTTP 422 Unprocessable Entity is the correct response. It tells the client the request arrived fine and the JSON was valid, but the content had a logic error. In this case the error is a reference to a room that does not exist. A 404 makes a developer think they called the wrong URL. A 422 makes it clear the problem is in the data they sent.

### Part 5.1 - Cybersecurity Risks of Exposing Stack Traces

Sending raw Java stack traces to API clients is a serious security risk. Stack traces contain internal application detail that should never leave the server.

They expose full package and class names. An attacker gets a map of the application structure and knows where to target. They also reveal the exact versions of every framework and library in use. An attacker cross-references those versions against public vulnerability databases and finds known exploits. Stack traces sometimes include server file paths too, which reveals deployment details.

A GlobalExceptionMapper was built to stop this. It catches all unhandled exceptions and returns a plain generic 500 message. Nothing internal reaches the client. All useful error detail is logged server-side where only developers see it.

### Part 5.2 - Why JAX-RS Filters are Better for Logging

Adding Logger.info() calls inside every resource method works but creates a mess. If the logging format changes, every single method across every resource class needs updating. If someone adds a new endpoint and forgets the logging line, that endpoint runs with no visibility at all.

JAX-RS filters solve this cleanly. By building ContainerRequestFilter and ContainerResponseFilter into one ApiLoggingFilter class, logging covers every request and response automatically. No resource class needs touching. New endpoints get logged without any extra work. One class handles logging for the whole application. The result is full coverage with far less code.
