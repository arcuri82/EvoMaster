package org.evomaster.client.java.instrumentation.mongo;

import com.foo.somedifferentpackage.examples.methodreplacement.mongo.MockedMongoCollection;
import com.mongodb.MongoNamespace;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoLoggerTest {

    @Test
    public void testLogFind() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MongoLogger.getInstance().reset();

        MongoLogger.getInstance().setOutputStream(new PrintStream(byteArrayOutputStream));
        MockedMongoCollection mockedMongoCollection = new MockedMongoCollection();
        mockedMongoCollection.setNamespace(new MongoNamespace("mydb.mycollection"));
        MongoLogger.getInstance().logFind(mockedMongoCollection,new BsonDocument(), true);
        String output = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(output.startsWith(MongoLogger.PREFIX));
    }
}
