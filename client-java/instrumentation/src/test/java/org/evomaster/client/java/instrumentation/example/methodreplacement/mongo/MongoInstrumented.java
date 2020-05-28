package org.evomaster.client.java.instrumentation.example.methodreplacement.mongo;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.conversions.Bson;

public interface MongoInstrumented {

    FindIterable<?> callFind(MongoCollection<?> mongoCollection, Bson bson, Class<?> documentClass);

    FindIterable<?> callFind(MongoCollection<?> mongoCollection, ClientSession clientSession, Bson bson, Class<?> documentClass);

}
