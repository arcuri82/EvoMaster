package org.evomaster.client.java.instrumentation.mongo;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import org.bson.conversions.Bson;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

public class MongoReplacementClass extends ThirdPartyMethodReplacementClass {

    private static final MongoReplacementClass singleton = new MongoReplacementClass();

    public static final String MONGO_COLLECTION_CLASS_NAME = "com.mongodb.client.MongoCollection";
    public static final String ITERATOR_METHOD_NAME = "iterator";
    public static final String FIND_METHOD_NAME = "find";
    public static final String FIND_WITH_CLIENT_SESSION_METHOD_NAME = "findWithClientSession";

    private static boolean invokeHasNext(Object findIterable) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method iteratorMethod = findIterable.getClass().getMethod(ITERATOR_METHOD_NAME);
        Iterator iterator = (Iterator) iteratorMethod.invoke(findIterable);
        return iterator.hasNext();
    }

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return MONGO_COLLECTION_CLASS_NAME;
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "find", usageFilter = UsageFilter.ANY)
    public static FindIterable<?> find(Object mongoCollection, Bson bson, Class documentClass) {
        try {
            Method findMethod = getOriginal(singleton, FIND_METHOD_NAME);
            Object findIterable = findMethod.invoke(mongoCollection, bson, documentClass);
            boolean operationReturnedDocuments = invokeHasNext(findIterable);
            logFind(mongoCollection, bson, operationReturnedDocuments);
            return (FindIterable<?>) findIterable;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findWithClientSession", usageFilter = UsageFilter.ANY)
    public static FindIterable<?> find(Object mongoCollection, ClientSession clientSession, Bson bson, Class documentClass) {

        try {
            Method findWithClientSession = getOriginal(singleton, FIND_WITH_CLIENT_SESSION_METHOD_NAME);
            Object findIterableInstance = findWithClientSession.invoke(mongoCollection, clientSession, bson, documentClass);
            boolean operationReturnedDocuments = invokeHasNext(findIterableInstance);
            logFind(mongoCollection, bson, operationReturnedDocuments);
            return (FindIterable<?>) findIterableInstance;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static void logFind(Object mongoCollection, Bson bson, boolean operationReturnedDocuments) {
        MongoLogger.getInstance().logFind(mongoCollection, bson, operationReturnedDocuments);
    }
}
