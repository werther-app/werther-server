package com.werther.server;

import org.bson.Document;

public class Worker implements Connection {

    public Document toDocument() {
        Document worker = new Document();
        return worker;
    }

    public String getCollectionName() {
        return ("workers");
    }
}
