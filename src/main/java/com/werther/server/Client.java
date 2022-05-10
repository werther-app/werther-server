package com.werther.server;

import org.bson.Document;

public class Client implements Connection {
    public Document toDocument() {
        return new Document();
    }

    public String getCollectionName() {
        return("clients");
    }
}
