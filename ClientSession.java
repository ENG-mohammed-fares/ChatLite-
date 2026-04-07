package server;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientSession {
    private static final AtomicInteger IP_COUNTER = new AtomicInteger(10);

    private String  username;
    private Socket  connectionSocket;
    private String  status;
    private int     sent;
    private int     inbox;
    private String  assignedIp;

    public ClientSession(String username, Socket socket) {
        this.username         = username;
        this.connectionSocket = socket;
        this.status           = "Active";
        this.sent             = 0;
        this.inbox            = 0;
        this.assignedIp       = "192.168.1." + IP_COUNTER.getAndIncrement();
    }

    public String getUsername()         { return username; }
    public Socket getConnectionSocket() { return connectionSocket; }

    public String getStatus()           { return status; }
    public void   setStatus(String s)   { this.status = s; }

    public int  getSent()               { return sent; }
    public void incrementSent()         { this.sent++; }

    public int  getInbox()              { return inbox; }
    public void incrementInbox()        { this.inbox++; }

    public String getAssignedIp()       { return assignedIp; }
}