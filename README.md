# Smart Campus Sensor & Room Management API

A RESTful API built with JAX-RS (Jersey) and Grizzly for managing campus rooms and IoT sensors.

Module: 5COSC022W — Client-Server Architectures
Student ID: w21162895
University of Westminster
---

# Smart Campus Sensor & Room Management API

A RESTful API built with JAX-RS (Jersey) and Grizzly for managing campus rooms and IoT sensors.

Module: 5COSC022W — Client-Server Architectures
Student ID: w21162895
University of Westminster

---

## API Overview

This API provides a backend service for the University Smart Campus initiative. It allows facilities managers to manage rooms and the sensors deployed within them, including temperature monitors, CO2 sensors, and occupancy trackers.

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

## Conceptual Report

### Part 1.1 - JAX-RS Resource Lifecycle

In JAX-RS, every time an HTTP request comes in, the framework creates a brand new instance of the resource class to handle it. Once the request is complete and the response is sent back, that instance is discarded. This is called the request-scoped lifecycle, and it is the default behaviour in JAX-RS. It means the resource class is not a singleton and does not retain any state between requests.

This created a challenge for storing data in the application. Since each request gets its own fresh resource object, rooms or sensors cannot be stored as instance variables in the resource classes, as the data would be lost after each request. To address this, a separate DataStore class was created that holds all the data in static fields. Because the fields are static, they belong to the class itself rather than any specific instance, so the data persists for as long as the server is running.

ConcurrentHashMap was used instead of a regular HashMap because multiple requests can arrive at the same time and be processed by different threads simultaneously. A regular HashMap is not designed to handle this and can produce unpredictable results or corrupt data when two threads try to read and write simultaneously. ConcurrentHashMap handles concurrent access safely without requiring manual locking of the entire map, making it the correct choice for a multi-threaded server environment.

### Part 1.2 - HATEOAS and the Discovery Endpoint

HATEOAS stands for Hypermedia as the Engine of Application State. The principle behind it is that an API should include links in its responses so that a client can navigate the API by following them, without needing prior knowledge of all available URLs. This is widely considered a characteristic of a well-designed REST API because it makes the API self-describing and discoverable.

In this implementation, the GET /api/v1 endpoint returns a JSON response that includes the URLs for the rooms and sensors collections. A developer who only knows the base URL can discover the full API surface with a single request. Compared to static documentation, this approach is considerably more reliable because if URLs change in future, the response will reflect those changes automatically. A client that reads URLs from the response will always receive correct information, whereas a client with hardcoded URLs from outdated documentation would fail.

### Part 2.1 - Returning Full Objects vs IDs Only

When a client calls GET /api/v1/rooms, there are two possible approaches to structuring the response. The first is to return only the room IDs, and the second is to return the full room objects, including all their fields. Both approaches carry trade-offs that are worth examining.

Returning only IDs produces a smaller response and uses less bandwidth, which may appear efficient on the surface. However, the client would need to make a separate GET request for each room to retrieve basic information such as the name and capacity. If there are 100 rooms in the system, that results in 100 additional network requests. This pattern, known as the N+1 problem, significantly increases both latency and server load.

Returning full objects produces a larger initial response but allows the client to retrieve everything it needs in a single request. For a campus management system where a facilities manager needs to view all rooms simultaneously, this is clearly the more appropriate approach. The payload size of a reasonable number of room objects is negligible on any modern network, and the reduction in round-trip time delivers a far better overall experience. This is the approach adopted in the implementation.

### Part 2.2 - Idempotency of DELETE

Idempotency in the context of HTTP methods refers to the property where sending the same request multiple times results in the same server state as sending it just once. The key distinction here is that idempotency is measured by the effect on the server state, not by the HTTP response code the client receives.

In this implementation, sending a DELETE request to /api/v1/rooms/{id} for the first time removes the room from the data store and returns a 204 No Content response. Sending the same request a second time returns a 404 Not Found response because the room no longer exists. Although the response codes differ between calls, the server state is identical after both. The room is absent from the system in either case, and this behaviour is fully consistent with the idempotency contract defined by the HTTP specification.

The practical benefit of this design is that clients which accidentally repeat a DELETE request due to network issues or retry logic will not cause any unintended changes to the server state. The outcome remains predictable and safe regardless of how many times the request is sent.

### Part 3.1 - The @Consumes Annotation and Content-Type Mismatches

Applying the @Consumes(MediaType.APPLICATION_JSON) annotation to a POST method instructs the JAX-RS runtime to only accept requests where the Content-Type header is set to application/json. This establishes a formal contract at the API boundary, clearly defining the expected format for all incoming request payloads.

When a client submits a request with an incompatible Content-Type, such as text/plain or application/xml, the JAX-RS framework intercepts it before the resource method is invoked and returns an HTTP 415 Unsupported Media Type response automatically. The method body is never invoked. This framework-level enforcement eliminates the need to write manual content validation logic within the resource class, keeping the code focused solely on business logic.

From a design perspective, this approach ensures that only correctly formatted JSON data reaches the processing layer, improves the API's overall reliability, and provides clients with a clear, descriptive error response when they submit data in an unsupported format.

### Part 3.2 - @QueryParam vs Path Parameter for Filtering

When adding type filtering to the sensors endpoint, a design decision was required between two approaches: embedding the filter value in the URL path, such as /api/v1/sensors/type/CO2, or expressing it as a query parameter, such as /api/v1/sensors?type=CO2. The query parameter approach was selected, and the decision is grounded in both REST semantics and practical API design.

Path parameters carry a specific semantic meaning in REST architecture. They are intended to identify a distinct resource within a collection, as in /sensors/TEMP-001, which points to one specific sensor by its identifier. A type filter does not identify any single resource. It narrows down the results of a collection based on a shared attribute. Placing such a value in the URL path would misrepresent its purpose and conflict with the established conventions of resource-oriented design.

Query parameters are the appropriate mechanism for this use case. They are designed specifically for optional operations such as filtering, searching, and sorting on collections, and they do not alter the identity of the resource being addressed. Using a query parameter also means the endpoint functions correctly with or without the filter present, and supports combining multiple filter conditions simultaneously, such as ?type=CO2&status=ACTIVE, without requiring any structural changes to the URL. This results in an API that is more semantically accurate, more flexible, and significantly easier to extend as the application grows.

### Part 4.1 - The Sub-Resource Locator Pattern

The Sub-Resource Locator pattern is a JAX-RS technique for organising nested resources cleanly. Rather than placing all the logic for sensor readings inside the SensorResource class, a dedicated SensorReadingResource class was created to handle everything related to reading history. Within SensorResource, a locator method annotated with @Path delegates incoming requests for the readings path to an instance of SensorReadingResource. JAX-RS then passes the request to that class for processing.

The primary benefit of this approach is a clear separation of concerns. Each class carries a single well-defined responsibility. SensorResource manages sensor registration and retrieval, while SensorReadingResource manages reading history. If the reading logic needs to change, only SensorReadingResource is affected, and there is no risk of introducing bugs into the sensor handling code. The pattern also improves testability since each class can be tested independently, and it keeps the codebase maintainable as the API scales.

### Part 5.2 - Why HTTP 422 is More Appropriate Than 404

When a client attempts to register a new sensor and provides a roomId that does not exist in the system, returning HTTP 404 Not Found would be semantically misleading. A 404 response typically communicates that the URL the client requested could not be located on the server. In this scenario, however, the endpoint /api/v1/sensors is fully operational and accessible. The problem lies not in the URL but in the request body.

HTTP 422 Unprocessable Entity is the more appropriate status code in this context. It indicates that the server received a well-formed request with valid JSON but was unable to process it because of a semantic error in the payload. In this case, the error refers to a room that does not exist in the data store. Returning a 422 gives the client precise, actionable feedback on what needs to be corrected. A client that receives a 404 may incorrectly assume it is targeting the wrong endpoint, whereas a 422 makes it clear that the issue is with the submitted data.

### Part 5.4 - Cybersecurity Risks of Exposing Stack Traces

Returning raw Java stack traces to external API clients represents a significant cybersecurity risk. A stack trace contains detailed information about the application's internal workings that should never be visible to users outside the system.

Stack traces expose the full package and class names used within the application, allowing an attacker to map out the internal architecture and identify specific components to target. They also reveal the exact versions of frameworks and libraries in use, which can be cross-referenced against public vulnerability databases to identify known security flaws in those specific versions. In some cases, stack traces include absolute file paths from the server filesystem, disclosing information about the deployment environment and operating system configuration.

In this implementation, the GlobalExceptionMapper intercepts all unhandled exceptions before any details reach the client and returns a generic HTTP 500 Internal Server Error response with a safe, non-revealing message. Full exception details are logged on the server side, where developers can access them for debugging, while nothing sensitive is ever transmitted to the client.

### Part 5.5 - Why JAX-RS Filters are Better for Logging

One approach to logging in an API is to manually insert Logger.info() calls inside every resource method. While this technically achieves logging, it introduces significant maintainability concerns. If a new endpoint is added and the developer omits a logging statement, that endpoint operates without any observability. If the logging format needs to change, every method across every resource class must be updated individually. This level of repetition is both error-prone and difficult to manage as the codebase grows.

JAX-RS filters address this problem in a much cleaner way. By implementing both ContainerRequestFilter and ContainerResponseFilter within a single ApiLoggingFilter class, logging is applied automatically to every request and response that passes through the server. No changes are required in any resource class, and no logging code needs to be added when new endpoints are introduced. This is an example of handling a cross-cutting concern, where a single piece of infrastructure applies uniformly across the entire application without being coupled to any specific resource. The result is consistent, complete observability with minimal code and no risk of coverage gaps.
