package com.werther.server;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

public class OrderInQueue implements Order {
    private final ObjectId client;
    private ObjectId worker;

    private final LocalDateTime createdOn;
    private LocalDateTime startTime;

    private int priority;
    private String status;

    private final String link;

    public OrderInQueue(ObjectId client, String link, int priority) {
        this.client = client;
        this.link = link;
        this.priority = priority;
        this.createdOn = LocalDateTime.now();
        this.status = "accepted";
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

    public void setWorker(ObjectId worker) {
        this.worker = worker;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public Document toDocument() {
        Document order = new Document();
        order.put("client", this.client);
        order.put("worker", this.worker);
        order.put("createdOn", this.createdOn);
        order.put("startTime", this.startTime);
        order.put("status", this.status);
        order.put("priority", this.priority);
        order.put("link", this.link);
        return order;
    }
}
