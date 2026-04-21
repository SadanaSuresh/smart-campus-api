package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @GET
    public List<Room> getAllRooms() {

        // return a copy so internal storage is not exposed directly
        return new ArrayList<>(DataStore.rooms.values());
    }

    @POST
    public Response createRoom(Room room) {

        if (room == null)
            return Response.status(400).entity(error("Request body is missing")).build();

        if (room.getId() == null || room.getId().isBlank())
            return Response.status(400).entity(error("Room id is required")).build();

        if (room.getName() == null || room.getName().isBlank())
            return Response.status(400).entity(error("Room name is required")).build();

        if (room.getCapacity() <= 0)
            return Response.status(400).entity(error("Capacity must be greater than zero")).build();

        // prevents duplicate rooms being created
        if (DataStore.rooms.putIfAbsent(room.getId(), room) != null)
            return Response.status(409).entity(error("Room already exists")).build();

        // sensor list should start empty
        if (room.getSensorIds() == null)
            room.setSensorIds(new ArrayList<>());

        URI location = URI.create("http://localhost:8080/api/v1/rooms/" + room.getId());

        return Response
                .created(location)
                .entity(room)
                .build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {

        Room room = DataStore.rooms.get(roomId);

        if (room == null)
            return Response.status(404).entity(error("Room not found")).build();

        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {

        Room room = DataStore.rooms.get(roomId);

        if (room == null)
            return Response.status(404).entity(error("Room not found")).build();

        // business rule: room cannot be deleted if sensors still exist inside it
        if (!room.getSensorIds().isEmpty())
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());

        DataStore.rooms.remove(roomId);

        return Response.noContent().build();
    }

    private Map<String,String> error(String message){

        Map<String,String> body = new LinkedHashMap<>();
        body.put("error", message);

        return body;
    }
}
