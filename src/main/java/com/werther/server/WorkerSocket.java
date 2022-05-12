package com.werther.server;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WorkerSocket {
    private static final int PORT = 5000;

    public static void main() throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        try {
            while (true) {
                String id;
                Socket client = server.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                try {
                    id = in.readLine();
                    // processes login by checking in db for authed workers
                    String loginResult = ClientListener.processLogin(id);
                    if (loginResult.equals("OK")) {
                        // create new adapter
                        ClientListener clientListener = new ClientListener(client);
                        ClientListener.getClients().put(id, clientListener);
                        // TODO: trigger queue to send order
                        // TODO: move all orders from disconnected
                    } else {
                        client.close();
                    }
                } catch (IOException e) {
                    client.close();
                }
            }
        } finally {
            server.close();
        }
    }
}
