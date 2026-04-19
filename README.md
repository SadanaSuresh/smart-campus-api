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

In JAX-RS, every time a request comes in, the framework creates a fresh instance of the resource class to deal with it. Once the response is sent, that instance is gone. This is called the request-scoped lifecycle and it is the default in JAX-RS. So resource classes are not singletons and they do not remember anything between requests.

This caused a problem for storing data. Since every resource object gets thrown away after each request, you cannot store rooms or sensors inside those classes because the data would vanish. To fix this, a separate DataStore class was created that holds everything in static fields. Static fields belong to the class itself, not to any particular instance, so the data sticks around for as long as the server is running.

ConcurrentHashMap was used instead of a regular HashMap because multiple requests can come in at the same time and run on different threads. A normal HashMap is not safe in that situation and can give back wrong data or crash when two threads hit it at the same time. ConcurrentHashMap handles this properly without locking the whole structure, which makes it the right choice here.

### Part 1.2 - HATEOAS and the Discovery Endpoint

HATEOAS stands for Hypermedia as the Engine of Application State. The idea is that an API should include links in its responses so a client can find its way around without already knowing every URL. This is generally seen as good REST design.

In this project, calling GET /api/v1 returns a JSON object that includes the URLs for the rooms and sensors collections. A developer who only knows the base URL can make that one request and find everything else from there. Static documentation is not as reliable because if URLs change the docs go out of date and any client using old URLs will break. With this approach the client always gets the right links straight from the response.

### Part 2.1 - Returning Full Objects vs IDs Only

When a client calls GET /api/v1/rooms there are two ways to structure the response. You can send back only the room IDs or you can send back the full room objects. Both have trade-offs.

Sending only IDs keeps the response small, which sounds efficient. But then the client has to make a separate GET request for each room just to get basic info like the name and capacity. For 100 rooms that is 100 extra requests. This is the N+1 problem and it makes things much slower and harder on the server.

Sending full objects means a bigger first response but the client gets everything in one go. For a system where a manager needs to see all rooms at once, this is the better call. The extra data is tiny on any modern network and avoiding all those extra round trips makes the whole thing noticeably faster. This is what the implementation does.

### Part 2.2 - Idempotency of DELETE

Idempotency means that sending the same request multiple times ends up with the same result as sending it once. The key point is that this is about what happens to the server state, not about what response code comes back.

In this implementation, the first DELETE to /api/v1/rooms/{id} removes the room and returns 204 No Content. Sending the same request again returns 404 Not Found because the room is already gone. The response codes are different but the server state is the same after both calls. The room does not exist either way. That meets the idempotency requirement from the HTTP specification.

The benefit is that if a client accidentally sends the same DELETE twice because of a timeout or retry, nothing bad happens on the server. The outcome stays the same no matter how many times it is sent.

### Part 3.1 - The @Consumes Annotation and Content-Type Mismatches

Adding @Consumes(MediaType.APPLICATION_JSON) to a POST method tells JAX-RS to only accept requests where the Content-Type header is application/json. This sets a clear rule at the API level about what format incoming data needs to be in.

If a client sends something with the wrong Content-Type like text/plain or application/xml, JAX-RS blocks it before the method even runs and sends back a 415 Unsupported Media Type response. The method body never executes. This means there is no need to write extra checking code inside the method to validate the format. JAX-RS handles it automatically.

From a design point of view this keeps the resource code focused on business logic, makes the API more reliable, and gives the client a clear error message when they send the wrong format.

### Part 3.2 - @QueryParam vs Path Parameter for Filtering

When adding type filtering to the sensors endpoint, a choice had to be made between putting the filter in the URL path like /api/v1/sensors/type/CO2, or using a query parameter like /api/v1/sensors?type=CO2. The query parameter approach was chosen and the reasoning is straightforward.

Path parameters are meant to point to a specific resource. Something like /sensors/TEMP-001 identifies one particular sensor by its ID. A type filter is not identifying a specific resource. It is just narrowing down a list. Putting it in the path gives it a meaning it was not designed for and goes against standard REST conventions.

Query parameters are built for exactly this kind of optional filtering. They do not change what resource is being accessed, they just refine the results. The endpoint works fine with or without the filter. And if you want to filter by multiple things at once like ?type=CO2&status=ACTIVE you just add more parameters without touching the URL structure at all. That makes the API much easier to extend.

### Part 4.1 - The Sub-Resource Locator Pattern

Rather than putting all the reading logic inside SensorResource and making it one huge class that does everything, a separate SensorReadingResource class was created to handle reading history. Inside SensorResource there is a locator method with a @Path annotation that hands off incoming requests for the readings path to an instance of SensorReadingResource. JAX-RS then uses that class to process the request.

The main benefit is that each class has one clear job. SensorResource deals with sensors. SensorReadingResource deals with reading history. If the reading logic needs to change, only SensorReadingResource gets touched and there is no risk of breaking anything in SensorResource. Each class can also be tested on its own, which keeps things manageable as the project grows.

### Part 5.2 - Why HTTP 422 is More Appropriate Than 404

When a client tries to register a sensor with a roomId that does not exist, sending back 404 Not Found would be misleading. A 404 normally means the URL the client called does not exist on the server. But in this case /api/v1/sensors is working fine. The problem is not with the URL. It is with the data inside the request body.

HTTP 422 Unprocessable Entity is the better response here. It tells the client that the request came through fine and the JSON was valid, but the content could not be processed because of a logic error in the data. In this case that error is a reference to a room that does not exist. A 404 might make a developer think they have the wrong URL. A 422 makes it clear the issue is in the data they sent.

### Part 5.4 - Cybersecurity Risks of Exposing Stack Traces

Sending raw Java stack traces back to API clients is a real security risk. Stack traces contain a lot of internal detail about the application that should never leave the server.

They show the full package and class names used in the codebase, which gives an attacker a map of how the application is structured. They also show the exact versions of every framework and library being used. An attacker can look those up in public vulnerability databases and find known exploits for those specific versions. Sometimes stack traces even include file paths from the server which reveals details about the deployment setup.

To stop this, a GlobalExceptionMapper was implemented that catches all unhandled exceptions and returns a simple generic 500 message. Nothing internal ever reaches the client. All the useful error detail gets logged on the server side where only developers can see it.

### Part 5.5 - Why JAX-RS Filters are Better for Logging

One way to do logging is to add Logger.info() calls inside every resource method. That works but it spreads logging code all over the place. If the format needs to change, every single method has to be updated. And if someone adds a new endpoint and forgets to add the logging line, that endpoint has no coverage at all.

JAX-RS filters are a much cleaner way to handle this. By implementing ContainerRequestFilter and ContainerResponseFilter in one ApiLoggingFilter class, logging happens automatically for every request and response that goes through the server. No resource class needs to be touched and new endpoints get covered automatically. This is what is meant by a cross-cutting concern. One class handles it for the whole application. The result is consistent logging everywhere with far less code.
