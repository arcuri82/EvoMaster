package com.foo.somedifferentpackage.examples.methodreplacement.mongo;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.conversions.Bson;
import org.evomaster.client.java.instrumentation.example.methodreplacement.mongo.MongoInstrumented;

public class MongoInstrumentedImpl implements MongoInstrumented {

    @Override
    public FindIterable<?> callFind(MongoCollection<?> mongoCollection, Bson bson, Class<?> documentClass) {
        return mongoCollection.find(bson, documentClass);
    }

    @Override
    public FindIterable<?> callFind(MongoCollection<?> mongoCollection, ClientSession clientSession, Bson bson, Class<?> documentClass) {
        return mongoCollection.find(clientSession, bson, documentClass);
    }
}
