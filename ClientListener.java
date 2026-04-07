package client;

import java.io.BufferedReader;

public class ClientListener extends Thread {

    BufferedReader inFromServer;

    public ClientListener(BufferedReader inFromServer) {
        this.inFromServer = inFromServer;
    }

    public void run() {
        try {
            String response;
            while ((response = inFromServer.readLine()) != null) {
                System.out.println(response);
            }
        } catch (Exception e) {
        }
    }
}