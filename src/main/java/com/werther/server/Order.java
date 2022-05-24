package com.werther.server;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

public interface Order {
    ObjectId getClient();

    ObjectId getWorker();

    String getLink();

    LocalDateTime getCreatedOn();

    String getStatus();

    Document toDocument();
}
