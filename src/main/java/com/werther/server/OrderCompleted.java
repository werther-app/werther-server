package com.werther.server;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;

import java.time.LocalDateTime;

public class OrderCompleted implements Order {
    private final ObjectId client;
    private final ObjectId worker;

    private final LocalDateTime createdOn;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    private final String status;
    private final String link;
    private final JSONArray result;

    public OrderCompleted(ObjectId client, ObjectId worker,
            LocalDateTime createdOn, LocalDateTime startTime, LocalDateTime endTime, String status,
            String link, JSONArray result) {
        this.client = client;
        this.worker = worker;

        this.createdOn = createdOn;
        this.startTime = startTime;
        this.endTime = endTime;

        this.status = status;
        this.link = link;
        this.result = result;
    }

    @Override
    public ObjectId getClient() {
        return client;
    }

    @Override
    public ObjectId getWorker() {
        return worker;
    }

    @Override
    public String getLink() {
        return link;
    }

    @Override
    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Document toDocument() {
        Document order = new Document();
        order.put("client", this.client);
        order.put("worker", this.worker);
        order.put("createdOn", this.createdOn);
        order.put("startTime", this.startTime);
        order.put("endTime", this.endTime);
        order.put("link", this.link);
        order.put("result", this.result);
        return order;
    }
}
