package com.werther.server;

import org.bson.Document;

public class Worker implements Connection {
    private Boolean connection;

    public Worker() {
        this.connection = false;
    }

    public Boolean getConnection() {
        return connection;
    }

    public void setConnection(Boolean connection) {
        this.connection = connection;
    }

    public Document toDocument() {
        Document worker = new Document();
        worker.put("connection", this.connection);
        return worker;
    }

    public String getCollectionName() {
        return ("workers");
    }
}
