package client;

import java.io.*;
import java.net.Socket;

public class ChatClient {

    public static void main(String[] args) {

        String serverIP = "localhost";
        int port = 5000;

        try {
            Socket socket = new Socket(serverIP, port);

            BufferedReader userInput = new BufferedReader(
                    new InputStreamReader(System.in)
            );

            BufferedReader inFromServer = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            DataOutputStream outToServer = new DataOutputStream(
                    socket.getOutputStream()
            );

            ClientListener listener = new ClientListener(inFromServer);
            listener.start();

            String sentence;

            while ((sentence = userInput.readLine()) != null) {
                outToServer.writeBytes(sentence + "\n");
            }

        } catch (Exception e) {
            System.out.println("Connection error");
        }
    }
}