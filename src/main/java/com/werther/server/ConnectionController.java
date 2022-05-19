package com.werther.server;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ConnectionController {
    @GetMapping("/auth")
    public String auth(@RequestParam(value = "type") String type) {
        try (MongoClient mongoClient = new MongoClient("localhost", 27017)) {
            MongoDatabase db = mongoClient.getDatabase("werther");

            // future client or worker
            Connection connection;

            // depends on what type of connection we want
            // we create new instance of connection object
            if (type.equals("client")) {
                connection = new Client();
            } else if (type.equals("worker")) {
                connection = new Worker();
            } else {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Wrong connection type");
            }

            // get proper collection for this connection type
            String collectionName = connection.getCollectionName();
            MongoCollection<Document> collection = db.getCollection(collectionName);

            // write object to collection, registration
            Document connectionDocument = connection.toDocument();
            collection.insertOne(connectionDocument);

            // return id to user
            return connectionDocument.get("_id", String.class);
        }
    }
}
