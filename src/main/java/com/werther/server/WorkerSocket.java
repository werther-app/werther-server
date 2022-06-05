package com.werther.server;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.bson.types.ObjectId;

public class WorkerSocket {
    private static final int PORT = 5554;

    public static void run() {
        try (ServerSocket server = new ServerSocket(PORT)) {
            try {
                while (true) {
                    String id;
                    ObjectId oid;
                    Socket client = server.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    PrintWriter out = new PrintWriter(
                            new BufferedWriter(new OutputStreamWriter(client.getOutputStream())));
                    try {
                        id = in.readLine();
                        oid = new ObjectId(id);
                        // processes login by checking in db for authed workers
                        Boolean loginResult = ClientListener.processLogin(oid);

                        if (loginResult) {
                            out.write("Success login.");
                            out.flush();
                            // create new adapter
                            ClientListener clientListener = new ClientListener(client, oid);
                            ClientListener.getClients().put(oid, clientListener);

                            // distribute to new worker order from queue if we have one
                            try (MongoClient mongoClient = new MongoClient("localhost", 27017)) {
                                MongoDatabase db = mongoClient.getDatabase("werther");
                                MongoCollection<Document> queue = db.getCollection("ordersQueue");

                                Document order = OrderController.selectOrder(queue);

                                if (order != null) {
                                    ObjectId orderOid = order.get("_id", ObjectId.class);
                                    OrderController.distributeOrder(queue, orderOid, oid);
                                }
                            }
                        } else {
                            out.write("Cannot login, wrong id.");
                            out.flush();
                            client.close();
                        }
                    } catch (IOException e) {
                        client.close();
                    }

                }
            } finally {
                server.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
