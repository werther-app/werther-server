package com.werther.server;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static com.mongodb.client.model.Filters.eq;

@RestController
public class OrderController {

    // accepts url for order and returns id of order
    // in future client will ask for order by it's id rather than url
    @PostMapping("/order")
    public String order(@RequestParam(value = "video") String url,
            @RequestParam(value = "id") String client) {
        try (MongoClient mongoClient = new MongoClient("localhost", 27017)) {
            MongoDatabase db = mongoClient.getDatabase("werther");
            // register order in queue
            return registerOrder(db, client, url);
        }
    }

    // accepts order id and tells about order status, or
    // maybe returns result of order computing, if we have it
    @GetMapping("/result")
    public String result(@RequestParam(value = "order") String order,
            @RequestParam(value = "id") String client) {
        try (MongoClient mongoClient = new MongoClient("localhost", 27017)) {
            MongoDatabase db = mongoClient.getDatabase("werther");

            // first, check order in queue, because queue is smaller
            MongoCollection<Document> queue = db.getCollection("ordersQueue");
            Document orderInQueue = queue.find(eq("_id", order)).first();

            if (orderInQueue != null) {
                // if order in queue check it's status
                String status = orderInQueue.get("status").toString();

                if (status.equals("accepted") || status.equals("working")) {
                    // status might be accepted or working,
                    // that means we want for client to wait more time
                    throw new ResponseStatusException(
                            HttpStatus.ACCEPTED, "Working");
                } else {
                    // but also status might be different, that means error, no matter what type
                    throw new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Error");
                }
            } else {
                // if order not in queue, maybe it's completed, let's check
                MongoCollection<Document> completed = db.getCollection("ordersCompleted");
                Document orderCompleted = completed.find(eq("_id", order)).first();

                if (orderCompleted == null) {
                    // if order not completed, that means requested id is wrong
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Wrong order id");
                } else {
                    // else get result
                    String result = orderCompleted.get("result").toString();

                    // but result can be timed out
                    if (result == null) {
                        // if result has timed out, restart job for this request
                        // tell user, that we are still working
                        registerOrder(db, client, orderCompleted.get("url").toString());
                        throw new ResponseStatusException(
                                HttpStatus.ACCEPTED, "Working");
                    } else {
                        // best scenario — return computed result
                        return result;
                    }
                }
            }
        }
    }

    // select no matter what worker with connection
    // useful when we sure that all online workers are free
    private Document selectOnlineWorker(MongoCollection<Document> workers) {
        Document worker = workers
                .find(eq("connection", "true"))
                .first();
        return worker;
    }

    // method to calculate priority for new order
    // just gets biggest (less important) priority and increases by one
    private int calculatePriority(MongoCollection<Document> queue) {
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
    private String registerOrder(MongoDatabase db, String client, String url) {
        MongoCollection<Document> queue = db.getCollection("ordersQueue");
        int priority = calculatePriority(queue);

        // check if queue is empty before we add new order, because…
        Boolean isQueueEmpty = queue.countDocuments() == 0;

        OrderInQueue newOrder = new OrderInQueue(client, url, priority);
        Document orderDocument = newOrder.toDocument();

        queue.insertOne(orderDocument);

        // …if queue was empty we sure that no working in progress
        // because orders in progress also located in queue
        if (isQueueEmpty) {
            MongoCollection<Document> workers = db.getCollection("workers");
            // so we can get random online worker
            Document worker = selectOnlineWorker(workers);

            if (worker != null) {
                // get worker id
                String workerId = worker.get("_id").toString();

                // update order object and convert in to document
                newOrder.setWorker(workerId);
                Document newDocument = newOrder.toDocument();

                // create update query
                Document updateDocument = new Document();
                updateDocument.put("$set", newDocument);

                // update order in db
                queue.updateOne(orderDocument, updateDocument);

                // TODO: sendToWorker();
            }
        }

        return orderDocument.get("_id").toString();
    }
}
