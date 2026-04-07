package server;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {

    private static ConcurrentHashMap<String, ClientSession> users      = new ConcurrentHashMap<>();
    private static Set<String>                              registered = ConcurrentHashMap.newKeySet();
    private static ConcurrentHashMap<String, String>        passwords  = new ConcurrentHashMap<>();

    public static void reset() {
        users.clear();
        registered.clear();
        passwords.clear();
    }

    public static boolean addUser(String username, ClientSession session) {
        if (users.containsKey(username)) return false;
        users.put(username, session);
        registered.add(username);
        return true;
    }

    public static void removeUser(String username) {
        users.remove(username);
    }

    public static ClientSession getUser(String username) {
        return users.get(username);
    }

    public static int count() { return users.size(); }

    public static Collection<ClientSession> getAllUsers() { return users.values(); }

    public static Iterable<String> getAllUsernames() { return users.keySet(); }

    public static void registerUsername(String username) {
        registered.add(username);
    }

    public static void registerUsername(String username, String password) {
        registered.add(username);
        passwords.put(username, password);
    }

    public static void unregisterUsername(String username) {
        registered.remove(username);
        passwords.remove(username);
        users.remove(username);
    }

    public static boolean isRegistered(String username) {
        return registered.contains(username);
    }

    public static boolean checkPassword(String username, String password) {
        String stored = passwords.get(username);
        if (stored == null || stored.isEmpty()) return true;
        return stored.equals(password);
    }

    public static Set<String> getAllRegistered() { return registered; }
}