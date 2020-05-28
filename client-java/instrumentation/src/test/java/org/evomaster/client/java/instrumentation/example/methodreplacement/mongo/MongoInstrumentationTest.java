package org.evomaster.client.java.instrumentation.example.methodreplacement.mongo;

import com.foo.somedifferentpackage.examples.methodreplacement.mongo.MockedFindIterable;
import com.foo.somedifferentpackage.examples.methodreplacement.mongo.MockedMongoCollection;
import com.foo.somedifferentpackage.examples.methodreplacement.mongo.MongoInstrumentedImpl;
import com.mongodb.MongoNamespace;
import org.bson.BsonDocument;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.mongo.MongoLogger;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by jgaleotti
 */
public class MongoInstrumentationTest {


    protected MongoInstrumented getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (MongoInstrumented)
                cl.loadClass(MongoInstrumentedImpl.class.getName()).newInstance();
    }


    @Test
    public void testFindBsonDocument() throws Exception {

        MongoInstrumented mongoInstrumented = getInstance();

        MockedMongoCollection mockedMongoCollection = new MockedMongoCollection();
        MockedFindIterable mockedFindIterable = new MockedFindIterable();
        mockedMongoCollection.setFindIterable(mockedFindIterable);
        mockedMongoCollection.setNamespace(new MongoNamespace("mydb.mycollection"));

        MongoLogger.getInstance().reset();
        String output;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            MongoLogger.getInstance().setOutputStream(new PrintStream(byteArrayOutputStream));

            mongoInstrumented.callFind(mockedMongoCollection, new BsonDocument(), null);

            output = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
        }
        assertNotNull(output);
        assertTrue(output.startsWith(MongoLogger.PREFIX));
    }

    @Test
    public void testFindClientSession() throws Exception {

        MongoInstrumented mongoInstrumented = getInstance();

        MockedMongoCollection mockedMongoCollection = new MockedMongoCollection();
        MockedFindIterable mockedFindIterable = new MockedFindIterable();
        mockedMongoCollection.setFindIterable(mockedFindIterable);

        mockedMongoCollection.setNamespace(new MongoNamespace("mydb.mycollection"));

        MongoLogger.getInstance().reset();
        String output;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            MongoLogger.getInstance().setOutputStream(new PrintStream(byteArrayOutputStream));

            mongoInstrumented.callFind(mockedMongoCollection, null, new BsonDocument(), null);

            output = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
        }
        assertNotNull(output);
        assertTrue(output.startsWith(MongoLogger.PREFIX));
    }


}