package model;

public class User {

    private String username;
    private String status;

    public User(String username) {
        this.username = username;
        this.status = "Active";
    }

    public String getUsername() {
        return username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}