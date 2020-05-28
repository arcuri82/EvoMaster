package org.evomaster.e2etests.spring.rest.mongo;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MongoContainterTest {

    private static final int MONGO_PORT = 27017;
    private GenericContainer mongo;

    @BeforeEach
    public void startMongo() {
        mongo = new GenericContainer("mongo:3.2")
                .withExposedPorts(MONGO_PORT);
        mongo.start();
    }

    @AfterEach
    public void stopMongo() {
        if (mongo != null) {
            mongo.stop();
        }
    }

    @Test
    public void testOnlyStartAndStop() {
        assertTrue(mongo.isRunning());
    }

    @Test
    public void testInsertOne() {
        Assumptions.assumeTrue(mongo.isRunning());

        MongoClient mongoClient = new MongoClient(mongo.getContainerIpAddress(), mongo.getMappedPort(MONGO_PORT));
        MongoDatabase database = mongoClient.getDatabase("test");
        MongoCollection<Document> collection = database.getCollection("testCollection");

        Document doc = new Document("name", "foo")
                .append("value", 1);
        collection.insertOne(doc);

        Document doc2 = collection.find(new Document("name", "foo")).first();
        assertEquals("A record can be inserted into and retrieved from MongoDB", 1, doc2.get("value"));

    }


    @Test
    public void testListDatabases() {
        Assumptions.assumeTrue(mongo.isRunning());

        MongoClient mongoClient = new MongoClient(mongo.getContainerIpAddress(), mongo.getMappedPort(MONGO_PORT));
        List<String> databaseList = StreamSupport.stream(mongoClient.listDatabaseNames().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, databaseList.size());

        String dbName = databaseList.iterator().next();
        assertEquals("local",dbName);

        MongoDatabase db = mongoClient.getDatabase(dbName);

        List<String> collectionList = StreamSupport.stream(db.listCollectionNames().spliterator(), false).collect(Collectors.toList());
        assertEquals(1, collectionList.size());
        String collectionName = collectionList.iterator().next();

        long numberOfDocumentsInCollection = db.getCollection(collectionName).countDocuments();

        assertEquals(1, numberOfDocumentsInCollection);
        List<Document> documentList = StreamSupport.stream(db.getCollection(collectionName).find().spliterator(), false).collect(Collectors.toList());

        assertEquals(1, documentList.size());
    }


}
