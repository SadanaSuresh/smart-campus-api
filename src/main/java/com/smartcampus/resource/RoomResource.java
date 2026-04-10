package com.smartcampus.resource;

import com.smartcampus.DataStore;
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
        return new ArrayList<>(DataStore.rooms.values());
    }

    @POST
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().isBlank())
            return Response.status(400).entity(err("Room ID is required")).build();
        if (DataStore.rooms.containsKey(room.getId()))
            return Response.status(409).entity(err("Room already exists: " + room.getId())).build();
        if (room.getSensorIds() == null)
            room.setSensorIds(new ArrayList<>());
        DataStore.rooms.put(room.getId(), room);
        URI location = URI.create("http://localhost:8080/api/v1/rooms/" + room.getId());
        return Response.created(location).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = DataStore.rooms.get(roomId);
        if (room == null)
            return Response.status(404).entity(err("Room not found: " + roomId)).build();
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.rooms.get(roomId);
        if (room == null)
            return Response.status(404).entity(err("Room not found: " + roomId)).build();
        if (!room.getSensorIds().isEmpty())
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        DataStore.rooms.remove(roomId);
        return Response.noContent().build();
    }

    private Map<String, String> err(String msg) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("error", msg);
        return m;
    }
}
