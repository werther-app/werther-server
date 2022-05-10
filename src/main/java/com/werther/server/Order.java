package com.werther.server;

import org.bson.Document;

import java.time.LocalDateTime;

public interface Order {
    String getClient();

    String getWorker();

    String getLink();

    LocalDateTime getCreatedOn();

    Document toDocument();
}
