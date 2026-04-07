package server;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler extends Thread {

    private Socket        socket;
    private BufferedReader in;
    private PrintWriter    out;
    private String         username;
    private String         currentRoom;

    private static final int    MAX_HISTORY  = 100;
    private static final String HISTORY_DIR  = "chat_history";
    private static final Map<String, List<String[]>> roomHistory =
            new ConcurrentHashMap<>();

    static {
        new File(HISTORY_DIR).mkdirs();
    }

    private static List<String[]> getHistory(String room) {
        return roomHistory.computeIfAbsent(room, ClientHandler::loadHistoryFromFile);
    }

    private static List<String[]> loadHistoryFromFile(String room) {
        List<String[]> list = new ArrayList<>();
        File f = historyFile(room);
        if (!f.exists()) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", 3);
                if (parts.length == 3) list.add(parts);
            }
        } catch (Exception ignored) {}
        if (list.size() > MAX_HISTORY) {
            list = new ArrayList<>(list.subList(list.size() - MAX_HISTORY, list.size()));
        }
        return list;
    }

    public static synchronized void saveMessage(String room,
                                                String user,
                                                String time,
                                                String msg) {
        String safeUser = user.replace("|", "");
        String safeTime = time.replace("|", "");
        String[] entry  = {safeUser, safeTime, msg};

        List<String[]> history = getHistory(room);
        history.add(entry);

        File f = historyFile(room);
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
            rewriteHistoryFile(f, history);
        } else {
            try (PrintWriter pw = new PrintWriter(new FileWriter(f, true))) {
                pw.println(safeUser + "|" + safeTime + "|" + msg);
            } catch (Exception ignored) {}
        }
    }

    private static void rewriteHistoryFile(File f, List<String[]> history) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
            for (String[] e : history) {
                pw.println(e[0] + "|" + e[1] + "|" + e[2]);
            }
        } catch (Exception ignored) {}
    }

    private static File historyFile(String room) {
        String safeName = room.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return new File(HISTORY_DIR, "history_" + safeName + ".txt");
    }

    public ClientHandler(Socket socket) throws Exception {
        this.socket = socket;
        this.in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out    = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                String[] parts  = msg.split(" ", 3);
                String command  = parts[0].toUpperCase();

                switch (command) {

                    case "HELLO": {
                        String requestedUsername = parts[1];
                        String providedPassword  = parts.length >= 3 ? parts[2] : "";

                        if (!UserManager.isRegistered(requestedUsername)) {
                            out.println("403 NOT REGISTERED");
                            socket.close();
                            return;
                        }
                        if (!UserManager.checkPassword(requestedUsername, providedPassword)) {
                            out.println("401 WRONG PASSWORD");
                            socket.close();
                            return;
                        }
                        boolean added = UserManager.addUser(
                                requestedUsername,
                                new ClientSession(requestedUsername, socket));
                        if (!added) {
                            out.println("409 USERNAME TAKEN");
                            socket.close();
                            return;
                        }
                        username = requestedUsername;
                        out.println("200 WELCOME");

                        if (ServerConsoleGUI.instance != null) {
                            ServerConsoleGUI.instance.logEvent("AUTH",
                                    username + " connected from "
                                            + socket.getInetAddress().getHostAddress());
                        }
                        break;
                    }

                    case "JOIN": {
                        if (currentRoom != null) {
                            RoomManager.leaveRoom(currentRoom, username);
                        }
                        currentRoom = parts[1];
                        RoomManager.joinRoom(currentRoom, username);
                        out.println("210 JOINED " + currentRoom);

                        if (ServerConsoleGUI.instance != null) {
                            ServerConsoleGUI.instance.logEvent("ROOM",
                                    username + " joined room [" + currentRoom + "]");
                        }
                        break;
                    }

                    case "MSG": {
                        if (currentRoom != null && parts.length >= 3) {
                            String roomTarget = parts[1];
                            String text       = parts[2];

                            String stamp = new SimpleDateFormat("hh:mm:ssa").format(new Date());
                            saveMessage(roomTarget, username, stamp, text);

                            RoomManager.broadcast(roomTarget, username, text);
                            out.println("211 SENT");

                            ClientSession senderSession = UserManager.getUser(username);
                            if (senderSession != null) senderSession.incrementSent();

                            if (ServerConsoleGUI.instance != null) {
                                ServerConsoleGUI.instance.logMessage(username, roomTarget, text);
                            }
                        }
                        break;
                    }

                    case "HISTORY": {
                        if (parts.length >= 2) {
                            String room            = parts[1];
                            List<String[]> history = getHistory(room);

                            out.println("216");
                            for (String[] entry : history) {
                                out.println("216H " + entry[0]
                                        + " "       + entry[1]
                                        + " "       + entry[2]);
                            }
                            out.println("216 END");
                        }
                        break;
                    }

                    case "PM": {
                        ClientSession target = UserManager.getUser(parts[1]);
                        if (target != null) {
                            PrintWriter targetOut = new PrintWriter(
                                    target.getConnectionSocket().getOutputStream(), true);
                            targetOut.println("(PM) " + username + ": " + parts[2]);
                            out.println("212 PRIVATE SENT");

                            ClientSession pmSender = UserManager.getUser(username);
                            if (pmSender != null) pmSender.incrementSent();
                            target.incrementInbox();

                            if (ServerConsoleGUI.instance != null) {
                                ServerConsoleGUI.instance.logEvent("PM",
                                        username + " → " + parts[1] + ": " + parts[2]);
                            }
                        } else {
                            out.println("404 USER NOT FOUND");
                        }
                        break;
                    }

                    case "USERS": {
                        out.println("213 " + UserManager.getAllRegistered().size());
                        for (String u : UserManager.getAllRegistered()) {
                            ClientSession cs     = UserManager.getUser(u);
                            String        status = cs != null ? cs.getStatus() : "Offline";
                            out.println("213U " + u + " " + status);
                        }
                        out.println("213 END");
                        break;
                    }

                    case "ROOMS": {
                        for (String r : RoomManager.getRooms().keySet()) {
                            out.println("214 " + r);
                        }
                        break;
                    }

                    case "LEAVE": {
                        if (parts.length >= 2) {
                            String roomToLeave = parts[1];
                            RoomManager.leaveRoom(roomToLeave, username);
                            if (roomToLeave.equals(currentRoom)) {
                                currentRoom = null;
                            }
                            out.println("215 LEFT " + roomToLeave);

                            if (ServerConsoleGUI.instance != null) {
                                ServerConsoleGUI.instance.logEvent("ROOM",
                                        username + " left room [" + roomToLeave + "]");
                            }
                        }
                        break;
                    }

                    case "QUIT": {
                        if (currentRoom != null) {
                            RoomManager.leaveRoom(currentRoom, username);
                            currentRoom = null;
                        }
                        UserManager.removeUser(username);
                        out.println("221 BYE");

                        if (ServerConsoleGUI.instance != null) {
                            ServerConsoleGUI.instance.logEvent("AUTH",
                                    username + " disconnected (QUIT — account preserved)");
                            ServerConsoleGUI.instance.refreshSessions();
                        }
                        socket.close();
                        return;
                    }

                    case "STATUS": {
                        if (parts.length >= 2) {
                            String        newStatus = parts[1];
                            ClientSession session   = UserManager.getUser(username);
                            if (session != null) {
                                session.setStatus(newStatus);
                                out.println("200 STATUS " + newStatus);
                                if (currentRoom != null) {
                                    RoomManager.broadcast(currentRoom, "System",
                                            username + " is now " + newStatus);
                                }
                                if (ServerConsoleGUI.instance != null) {
                                    ServerConsoleGUI.instance.logEvent("INFO",
                                            username + " status → " + newStatus);
                                }
                            }
                        }
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Client disconnected: " + username);
        } finally {
            try {
                if (username != null) {
                    UserManager.removeUser(username);
                    if (ServerConsoleGUI.instance != null) {
                        ServerConsoleGUI.instance.logEvent("AUTH",
                                username + " disconnected (connection lost)");
                        ServerConsoleGUI.instance.refreshSessions();
                    }
                }
                if (currentRoom != null) RoomManager.leaveRoom(currentRoom, username);
                if (!socket.isClosed()) socket.close();
            } catch (Exception ignored) {}
        }
    }
}