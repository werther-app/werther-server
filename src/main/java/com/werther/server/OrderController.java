package com.werther.server;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;

@RestController
public class OrderController {
    // accepts url for order and returns id of order
    // in future client will ask for order by it's id rather than url
    @PostMapping("/order")
    public String order(@RequestParam(value = "link") String link,
            @RequestParam(value = "id") String client) {
        try (MongoClient mongoClient = new MongoClient("localhost", 27017)) {
            ObjectId clientOid = new ObjectId(client);
            MongoDatabase db = mongoClient.getDatabase("werther");
            // register order in queue
            return registerOrder(db, clientOid, link);
        }
    }

    // accepts order id and tells about order status, or
    // maybe returns result of order computing, if we have it
    @GetMapping("/order")
    public JSONArray result(@RequestParam(value = "order") String order,
            @RequestParam(value = "id") String client) {
        try (MongoClient mongoClient = new MongoClient("localhost", 27017)) {
            ObjectId orderOid = new ObjectId(order);
            ObjectId clientOid = new ObjectId(client);
            MongoDatabase db = mongoClient.getDatabase("werther");

            // first, check order in queue, because queue is smaller
            MongoCollection<Document> queue = db.getCollection("ordersQueue");
            Document orderInQueueQuery = new Document("_id", orderOid).append("client", clientOid);
            Document orderInQueue = queue.find(orderInQueueQuery).first();

            if (orderInQueue != null) {
                throw new ResponseStatusException(
                        HttpStatus.ACCEPTED, "Working");
            } else {
                // if order not in queue, maybe it's completed, let's check
                MongoCollection<Document> completed = db.getCollection("ordersCompleted");
                Document orderCompletedQuery = new Document("_id", orderOid).append("client",
                        clientOid);
                Document orderCompleted = completed.find(orderCompletedQuery).first();

                if (orderCompleted == null) {
                    // if order not completed, that means requested id is wrong
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Wrong order id");
                } else {
                    // else check status, there might be error
                    String status = orderCompleted.get("status", String.class);

                    if (status.equals("completed")) {
                        // if completed, get result
                        @SuppressWarnings("unchecked")
                        ArrayList<String> nullableResult = (ArrayList<String>) orderCompleted.get("result");

                        // but result can be timed out
                        if (nullableResult == null) {
                            // if result has timed out, restart job for this request
                            // tell user, that we are still working
                            registerOrder(db, clientOid, orderCompleted.get("link", String.class));
                            throw new ResponseStatusException(
                                    HttpStatus.ACCEPTED, "Working");
                        } else {
                            // best scenario — return computed result
                            JSONArray result = new JSONArray(nullableResult);
                            return result;
                        }
                    } else {
                        // if status is error, return error
                        throw new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Error");
                    }
                }
            }
        }
    }

    // select no matter what worker with connection
    // useful when we sure that all online workers are free
    private static ObjectId selectOnlineWorker(MongoCollection<Document> workers) {
        ObjectId[] oids = ClientListener.getClients().keySet().toArray(new ObjectId[0]);
        if (oids.length != 0) {
            return oids[0];
        }
        return null;
    }

    // method to calculate priority for new order
    // just gets biggest (less important) priority and increases by one
    private static int calculatePriority(MongoCollection<Document> queue) {
        Document order = queue.find().sort(Sorts.descending("priority")).first();

        if (order != null) {
            int priority = (int) order.get("priority") + 1;
            return priority;
        }
        return 1;
    }

    // register order functionality uses when client submits order
    // and when client tries to get result of timed out order
    // so this functionality needed as separate method
    public static String registerOrder(MongoDatabase db, ObjectId client, String link) {
        MongoCollection<Document> queue = db.getCollection("ordersQueue");
        int priority = calculatePriority(queue);

        // check if queue is empty before we add new order, because…
        Boolean isQueueEmpty = queue.countDocuments() == 0;

        OrderInQueue newOrder = new OrderInQueue(client, link, priority);
        Document orderDocument = newOrder.toDocument();

        queue.insertOne(orderDocument);

        ObjectId orderOid = orderDocument.get("_id", ObjectId.class);

        // …if queue was empty we sure that no working in progress
        // because orders in progress also located in queue
        if (isQueueEmpty) {
            MongoCollection<Document> workers = db.getCollection("workers");
            // so we can get random online worker
            ObjectId workerOid = selectOnlineWorker(workers);

            if (workerOid != null) {
                // get worker id
                // ObjectId workerOid = worker.getObjectId("_id");

                distributeOrder(queue, orderOid, workerOid);
            }
        }

        return orderDocument.getObjectId("_id").toHexString();
    }

    public static void distributeOrder(MongoCollection<Document> queue, ObjectId orderOid, ObjectId workerOid) {
        Document query = new Document("_id", orderOid);

        Bson updates = Updates.combine(
                Updates.set("worker", workerOid),
                Updates.set("status", "working"),
                Updates.set("startTime", LocalDateTime.now()));

        queue.updateOne(query, updates);

        // get data to send order
        Document order = queue.find(query).first();
        ObjectId clientOid = order.get("client", ObjectId.class);
        String link = order.get("link", String.class);

        // send order
        ClientListener.sendOrder(workerOid, clientOid, orderOid, link);
    }

    public static Document selectOrder(MongoCollection<Document> queue) {
        Document query = new Document("status", "accepted");
        Document order = queue.find(query).sort(Sorts.ascending("priority")).first();
        return order;
    }

    public static Bson resetOrder() {
        Bson resetOrder = Updates.combine(
                Updates.set("status", "accepted"),
                Updates.set("startTime", null));
        return resetOrder;
    }

    public static Bson resetWorker() {
        Bson resetWorker = Updates.set("worker", null);
        return resetWorker;
    }

    public static void redistributeOrders(MongoCollection<Document> queue, ObjectId workerOid) {
        Document query = new Document("worker", workerOid);

        Bson resetOrder = resetOrder();
        Bson resetWorker = resetWorker();

        queue.updateMany(query, resetWorker);
        queue.updateMany(query, resetOrder);
    }
}
