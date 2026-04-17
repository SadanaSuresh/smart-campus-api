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

---

## Conceptual Report

### Part 1.1 - JAX-RS Resource Lifecycle

In JAX-RS, every time a request arrives at the server, a brand new instance of the resource class is created to handle it. Once the response has been sent back, that instance is discarded. This is referred to as the request-scoped lifecycle, and it is the default behaviour in JAX-RS, meaning resource classes do not function as singletons and do not carry state between requests.

This presented a real challenge when it came to storing data. Since every resource object is thrown away after each request, storing rooms or sensors as instance variables inside those classes would mean the data is lost immediately. To solve this, a dedicated DataStore class was created to hold all application data in static fields. Static fields belong to the class rather than any individual instance, so the data remains available for the entire duration the server runs.

ConcurrentHashMap was selected over a standard HashMap because the server handles multiple requests concurrently across different threads. A regular HashMap is not thread-safe and can produce corrupted or unpredictable results when two threads attempt to read and write simultaneously. ConcurrentHashMap resolves this by supporting safe concurrent access without requiring a full lock on the entire data structure, making it well-suited to this environment.

### Part 1.2 - HATEOAS and the Discovery Endpoint

HATEOAS stands for Hypermedia as the Engine of Application State. The principle is that an API should embed navigational links within its responses, allowing a client to explore the API without needing prior knowledge of every available URL. This is widely regarded as a hallmark of well-designed REST architecture.

In this implementation, a GET request to /api/v1 returns a JSON object containing the URLs for the rooms and sensors collections. A developer who only has the base URL can issue that single request and immediately discover where everything else lives. This is considerably more reliable than static documentation, which can quickly become outdated whenever URLs change. A client reading links directly from the response will always have accurate information, whereas one relying on hardcoded or documented URLs risks breaking whenever the API evolves.

### Part 2.1 - Returning Full Objects vs IDs Only

When a client calls GET /api/v1/rooms, there are two possible approaches to structuring the response. The first is to return only room IDs, and the second is to return the complete room objects with all their fields. Each approach has its trade-offs.

Returning only IDs produces a smaller initial response and reduces bandwidth usage, which sounds efficient. However, it forces the client to issue a separate GET request for each room to retrieve basic details such as name and capacity. For 100 rooms, that becomes 100 additional network calls. This is the N+1 problem, and it significantly increases both latency and server load.

Returning full objects produces a larger initial payload but gives the client everything it needs in a single request. For a campus management system where a facilities manager needs to view all rooms at once, this is clearly the better approach. The additional data size is negligible on modern networks, and the reduction in round-trip times results in a noticeably faster, more efficient experience. This is the design decision adopted in this implementation.

### Part 2.2 - Idempotency of DELETE

Idempotency in HTTP refers to the property where making the same request multiple times produces the same server state as making it once. The important distinction is that idempotency concerns the effect on server state, not the HTTP status code returned in the response.

In this implementation, the first DELETE request to /api/v1/rooms/{id} removes the room from the data store and returns a 204 No Content response. A second identical request returns 404 Not Found because the room no longer exists. The response codes differ, but the server state after both calls is identical: the room is gone. This satisfies the idempotency requirement as defined by the HTTP specification.

The practical benefit is that clients that accidentally repeat a DELETE request, perhaps due to a network timeout or a retry mechanism, will not cause unintended side effects on the server. The outcome is consistent and predictable regardless of how many times the request is sent.

### Part 3.1 - The @Consumes Annotation and Content-Type Mismatches

Applying @Consumes(MediaType.APPLICATION_JSON) to a POST method instructs JAX-RS to only accept requests where the Content-Type header is set to application/json. This establishes a clear contract at the API boundary regarding the expected format of incoming data.

If a client sends a request with an incompatible Content-Type, such as text/plain or application/xml, JAX-RS intercepts it before the resource method is invoked and automatically returns an HTTP 415 Unsupported Media Type response. The method body is never executed. This means there is no need to write manual content validation code inside the resource class, keeping the logic clean and focused purely on the business requirements.

From a design perspective, this ensures that only properly formatted JSON ever reaches the processing layer, improves the API's reliability, and gives clients a clear, descriptive error when they submit data in the wrong format.

### Part 3.2 - @QueryParam vs Path Parameter for Filtering

When implementing type filtering on the sensors endpoint, a design decision was needed between embedding the filter in the URL path such as /api/v1/sensors/type/CO2, or expressing it as a query parameter such as /api/v1/sensors?type=CO2. The query parameter approach was chosen, and the reasoning is rooted in REST conventions and practical usability.

Path parameters are semantically intended to identify a specific resource within a collection. For example, /sensors/TEMP-001 uniquely identifies one sensor by its ID. A type filter does not identify a specific resource; it simply narrows down a collection based on a shared property value. Placing that in the URL path misrepresents its purpose and conflicts with standard REST design principles.

Query parameters are the correct tool for this job. They are designed for optional filtering, searching, and sorting operations on collections, and they do not alter the identity of the resource being accessed. The endpoint works correctly with or without the filter, and multiple conditions can be combined in a single request, such as ?type=CO2&status=ACTIVE, without any changes to the URL structure. This produces an API that is semantically accurate, more flexible, and straightforward to extend.

### Part 4.1 - The Sub-Resource Locator Pattern

The Sub-Resource Locator pattern in JAX-RS provides a clean way to organise nested resources. Rather than placing all the logic for sensor readings inside SensorResource and making it one large, difficult-to-maintain class, a dedicated SensorReadingResource class was created to handle everything related to reading history. Inside SensorResource, a locator method annotated with @Path delegates incoming requests for the readings path to an instance of SensorReadingResource, which JAX-RS then uses to handle the request.

The main benefit is a clear separation of concerns. SensorResource is responsible for sensor registration and retrieval, while SensorReadingResource is responsible for reading history. If the reading logic ever needs to change, only SensorReadingResource needs to be modified, without risking accidental bugs in the sensor handling code. Each class can also be tested independently, and the overall codebase stays manageable as the API grows.

### Part 5.2 - Why HTTP 422 is More Appropriate Than 404

When a client tries to register a new sensor with a roomId that does not exist in the system, returning HTTP 404 Not Found would be misleading. A 404 typically indicates that the requested URL could not be found on the server. In this case, however, the endpoint /api/v1/sensors is fully operational. The problem is not with the URL at all, but with the request body.

HTTP 422 Unprocessable Entity is semantically more appropriate. It indicates that the server received a well-formed request containing valid JSON, but could not process it due to a logical error in the payload. In this instance, the error refers to a nonexistent room. Returning a 422 code gives the client precise, actionable information about what needs to be corrected. A client receiving a 404 might assume it is calling the wrong endpoint, whereas a 422 makes it immediately clear that the problem lies in the submitted data.

### Part 5.4 - Cybersecurity Risks of Exposing Stack Traces

Returning raw Java stack traces to external API clients is a significant security risk. Stack traces contain detailed information about the application's internal workings that should never be accessible to anyone outside the system.

They expose full package and class names, which gives an attacker a detailed map of the application architecture and helps them identify specific targets. They also reveal the exact versions of frameworks and libraries in use, which can be cross-referenced against public vulnerability databases to find known exploits. In some cases, stack traces even include absolute file paths from the server, disclosing information about the deployment environment.

To prevent any of this reaching the client, a GlobalExceptionMapper was implemented that intercepts all unhandled exceptions and returns a simple, generic HTTP 500 response. No internal details are ever sent to the client. All meaningful error information is logged server-side and accessible only by developers.

### Part 5.5 - Why JAX-RS Filters are Better for Logging

One approach to logging is to add Logger.info() calls manually inside every resource method. While this achieves the goal, it scatters logging code across the entire codebase. If the logging format needs to change, every method in every resource class has to be updated. If a new endpoint is added and the logging statement is forgotten, that endpoint operates with no observability at all.

JAX-RS filters provide a much cleaner solution. By implementing both ContainerRequestFilter and ContainerResponseFilter in a single ApiLoggingFilter class, logging is applied automatically to every request and response that passes through the server. No changes are needed to any resource class, and new endpoints are automatically covered without extra effort. This is a practical example of handling a cross-cutting concern: a single piece of infrastructure applies uniformly across the entire application. The result is complete, consistent observability with significantly less code.
