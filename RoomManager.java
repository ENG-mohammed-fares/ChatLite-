package server;

import model.Room;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class RoomManager {

    private static Map<String, Room> rooms = new HashMap<>();

    public static synchronized Room getOrCreateRoom(String roomName) {
        return rooms.computeIfAbsent(roomName, Room::new);
    }

    public static synchronized void joinRoom(String roomName, String username) {
        Room room = getOrCreateRoom(roomName);
        room.addUser(username);
    }

    public static synchronized void leaveRoom(String roomName, String username) {
        Room room = rooms.get(roomName);
        if (room != null) {
            room.removeUser(username);
        }
    }

    public static synchronized Map<String, Room> getRooms() {
        return rooms;
    }

    public static synchronized void broadcast(String roomName, String sender, String message) {

        Room room = rooms.get(roomName);
        if (room == null) return;

        for (String user : room.getUsers()) {

            ClientSession session = UserManager.getUser(user);

            if (session != null) {
                try {
                    PrintWriter out = new PrintWriter(
                            session.getConnectionSocket().getOutputStream(), true
                    );

                    out.println(sender + ": " + message);

                } catch (Exception ignored) {
                }
            }
        }
    }

    public static synchronized void reset() {
        rooms.clear();
    }
}