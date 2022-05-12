package com.werther.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.types.ObjectId;
import org.json.JSONObject;

public class ClientListener {
    private Socket socket;

    private BufferedReader in;
    private PrintWriter out;

    private ObjectId workerOid;

    private static HashMap<String, ClientListener> clients = new HashMap<String, ClientListener>();

    public ClientListener(Socket socket) throws IOException {
        this.socket = socket;

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
    }

    public static void sendOrder(String workerId, String clientId, String url) {
        JSONObject order = new JSONObject();
        order.put("id", clientId);
        order.put("url", url);

        ClientListener workerListener = getClients().get(workerId);
        PrintWriter output = workerListener.getOutput();
        BufferedReader input = workerListener.getInput();

        output.write(order.toString());
        output.flush();

        try {
            String result = input.readLine();
            parseResultAndUpdateDB(result);
        } catch (IOException e) {
            try {
                workerListener.getSocket().close();
            } catch (IOException p) {
                System.out.println(p);
            }
            clients.remove(workerId);
        }
    }

    public void waitResult() {
        try {
            String result = in.readLine();
            parseResultAndUpdateDB(result);
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException p) {
                System.out.println(p);
            }
            clients.remove(workerOid.toString());
        }
    }

    private static void parseResultAndUpdateDB(String result) {
        JSONObject jsonResult = new JSONObject(result);
        String orderId = jsonResult.get("id").toString();
        String code = jsonResult.getString("result").toString();
        String endTime = jsonResult.get("endTime").toString();
        updateDBOrder(new ObjectId(orderId), code, endTime);
    }

    public static String processLogin(String id) {
        String sync = updateDBState(new ObjectId(id), "connected");
        return sync;
    }

    public static HashMap<String, ClientListener> getClients() {
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

    public static void updateDBOrder(ObjectId orderId, String code, String endTime) {
        try (MongoClient mongoClient = new MongoClient("localhost", 27017)) {
            MongoDatabase db = mongoClient.getDatabase("werther");
            MongoCollection<Document> queue = db.getCollection("ordersQueue");

            Document query = new Document("_id", orderId);
            Document order = queue.find(query).first();

            if (order != null) {
                ObjectId client = order.getObjectId("client");
                ObjectId worker = order.getObjectId("worker");
                String createdOn = order.get("createdOn").toString();
                String startTime = order.get("startTime").toString();
                String link = order.get("link").toString();

                OrderCompleted completed = new OrderCompleted(client, worker, createdOn, startTime, link, code);

                Document completedDocument = completed.toDocument();
                MongoCollection<Document> ordersCompleted = db.getCollection("ordersQueue");
                ordersCompleted.insertOne(completedDocument);

                queue.deleteOne(query);
            }
        }
    }

    public static String updateDBState(ObjectId oid, String state) {
        try (MongoClient mongoClient = new MongoClient("localhost", 27017)) {
            MongoDatabase db = mongoClient.getDatabase("werther");
            MongoCollection<Document> workers = db.getCollection("workers");

            // find worker to update
            Document query = new Document();
            query.put("_id", oid);
            Document workerDocument = workers.find(query).first();

            if (workerDocument != null) {
                // create new proper worker
                Worker updateWorker = new Worker();
                switch (state) {
                    case "connected":
                        updateWorker.setConnection(true);
                        break;
                    case "disconnected":
                        updateWorker.setConnection(false);
                        break;
                }
                Document newWorkerDocument = updateWorker.toDocument();
                newWorkerDocument.put("_id", oid);

                // create update query
                Document updateWorkerDocument = new Document();
                updateWorkerDocument.put("$set", newWorkerDocument);

                // update worker
                workers.updateOne(workerDocument, updateWorkerDocument);

                return "OK";
            } else {
                return "BAD";
            }
        }
    }
}
