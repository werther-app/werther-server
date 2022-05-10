package com.werther.server;

import org.bson.Document;

public interface Connection {
    Document toDocument();

    String getCollectionName();
}
