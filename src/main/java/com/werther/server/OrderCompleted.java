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

    private final String link;
    private final JSONArray result;

    public OrderCompleted(ObjectId client, ObjectId worker,
            String createdOn, String startTime,
            String link, String result) {
        this.client = client;
        this.worker = worker;

        this.createdOn = LocalDateTime.parse(createdOn);
        this.startTime = LocalDateTime.parse(startTime);
        this.endTime = LocalDateTime.now();

        this.link = link;
        this.result = new JSONArray(result);
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
