package com.werther.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

public class ClientListener extends Thread {
    private Socket socket;

    private BufferedReader in;
    private PrintWriter out;

    private ObjectId workerOid;

    private static HashMap<ObjectId, ClientListener> clients = new HashMap<ObjectId, ClientListener>();

    public ClientListener(Socket socket, ObjectId oid) throws IOException {
        this.socket = socket;
        this.workerOid = oid;

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
        start();
    }

    @Override
    public void run() {
        while (!socket.isClosed()) {
            try {
                // checks if server socket contains zero client sockets
                // that means we client disconnected,
                // because we use server-side socket only for one client
                if (socket.getInputStream().read() == -1) {
                    closeConnection(workerOid, socket);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void closeConnection(ObjectId workerOid, Socket socket) {
        // remove client from connected map
        clients.remove(workerOid);
        // move all orders on this client to other clients
        redistributeOrders(workerOid);
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendOrder(ObjectId workerOid, ObjectId clientOid, ObjectId orderOid, String link) {
        JSONObject order = new JSONObject()
                .put("id", orderOid)
                .put("client", clientOid)
                .put("link", link);

        ClientListener workerListener = getClients().get(workerOid);
        PrintWriter output = workerListener.getOutput();
        BufferedReader input = workerListener.getInput();

        output.write(order.toString());
        output.flush();

        try {
            String result = input.readLine();
            parseResultAndUpdateDB(result);
        } catch (IOException e) {
            closeConnection(workerOid, workerListener.getSocket());
        }
    }

    private static void redistributeOrders(ObjectId workerOid) {
        try (MongoClient mongoClient = new MongoClient("localhost", 27017)) {
            MongoDatabase db = mongoClient.getDatabase("werther");
            MongoCollection<Document> queue = db.getCollection("ordersQueue");

            OrderController.redistributeOrders(queue, workerOid);
        }
    }

    private static void parseResultAndUpdateDB(String result) {
        String status;
        JSONObject jsonResult = new JSONObject(result);
        ObjectId orderOid = new ObjectId(jsonResult.get("id").toString());
        JSONArray code = jsonResult.getJSONArray("result");
        LocalDateTime endTime = LocalDateTime.parse(jsonResult.get("endTime").toString());
        if (code.length() == 0) {
            status = "error";
        } else {
            status = "completed";
        }
        updateDBOrder(orderOid, code, endTime, status);
    }

    // maybe in future we will need to do more actions in login processing
    // rather than just checking in db, so we need this method
    public static Boolean processLogin(ObjectId oid) {
        return checkIfRegistered(oid);
    }

    public static HashMap<ObjectId, ClientListener> getClients() {
        return clients;
    }

    public PrintWriter getOutput() {
        return out;
    }

    public BufferedReader getInput() {
        return in;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public static void updateDBOrder(ObjectId orderId, JSONArray code, LocalDateTime endTime, String status) {
        try (MongoClient mongoClient = new MongoClient("localhost", 27017)) {
            MongoDatabase db = mongoClient.getDatabase("werther");
            MongoCollection<Document> queue = db.getCollection("ordersQueue");

            Document query = new Document("_id", orderId);
            Document order = queue.find(query).first();

            if (order != null) {
                ObjectId client = order.get("client", ObjectId.class);
                ObjectId worker = order.get("worker", ObjectId.class);

                LocalDateTime createdOn = order.get("createdOn", Date.class)
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                LocalDateTime startTime = order.get("startTime", Date.class)
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                String link = order.get("link", String.class);

                OrderCompleted completed = new OrderCompleted(client, worker, createdOn, startTime, endTime, status,
                        link,
                        code);

                Document completedDocument = completed.toDocument();
                completedDocument.put("_id", orderId);
                MongoCollection<Document> ordersCompleted = db.getCollection("ordersCompleted");
                ordersCompleted.insertOne(completedDocument);

                queue.deleteOne(query);
            }
        }
    }

    public static Boolean checkIfRegistered(ObjectId oid) {
        try (MongoClient mongoClient = new MongoClient("localhost", 27017)) {
            MongoDatabase db = mongoClient.getDatabase("werther");
            MongoCollection<Document> workers = db.getCollection("workers");

            // find worker
            Document query = new Document();
            query.put("_id", oid);
            Document workerDocument = workers.find(query).first();

            if (workerDocument != null) {
                return true;
            } else {
                return false;
            }
        }
    }
}
