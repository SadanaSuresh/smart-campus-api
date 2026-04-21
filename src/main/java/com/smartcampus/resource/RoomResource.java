package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.Main;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @GET
    public List<Room> getAllRooms() {

        // return a copy so the internal map values are not exposed directly
        return new ArrayList<>(DataStore.rooms.values());
    }

    @POST
    public Response createRoom(Room room) {

        if (room == null)
            return Response.status(400).entity(errorBody(400, "Bad Request", "Request body is missing")).build();

        if (room.getId() == null || room.getId().isBlank())
            return Response.status(400).entity(errorBody(400, "Bad Request", "Room id is required")).build();

        if (room.getName() == null || room.getName().isBlank())
            return Response.status(400).entity(errorBody(400, "Bad Request", "Room name is required")).build();

        if (room.getCapacity() <= 0)
            return Response.status(400).entity(errorBody(400, "Bad Request", "Capacity must be greater than zero")).build();

        // a new room should start with an empty sensor list
        if (room.getSensorIds() == null)
            room.setSensorIds(new ArrayList<>());

        // putIfAbsent avoids duplicate room creation if the same id is posted again
        if (DataStore.rooms.putIfAbsent(room.getId(), room) != null)
            return Response.status(409).entity(errorBody(409, "Conflict", "Room already exists: " + room.getId())).build();

        URI location = URI.create(Main.BASE_URI + "rooms/" + room.getId());

        return Response.created(location).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {

        Room room = DataStore.rooms.get(roomId);

        if (room == null)
            return Response.status(404).entity(errorBody(404, "Not Found", "Room not found: " + roomId)).build();

        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {

        Room room = DataStore.rooms.get(roomId);

        if (room == null)
            return Response.status(404).entity(errorBody(404, "Not Found", "Room not found: " + roomId)).build();

        // room cannot be removed while sensors are still linked to it
        if (!room.getSensorIds().isEmpty())
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());

        DataStore.rooms.remove(roomId);

        return Response.noContent().build();
    }

    private Map<String, Object> errorBody(int status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        return body;
    }
}
