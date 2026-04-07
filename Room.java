package model;

import java.util.HashSet;
import java.util.Set;

public class Room {

    private String name;
    private Set<String> users = new HashSet<>();

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<String> getUsers() {
        return users;
    }

    public void addUser(String username) {
        users.add(username);
    }

    public void removeUser(String username) {
        users.remove(username);
    }
}