package org.evomaster.client.java.instrumentation.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class MongoLogger {

    public static final String GET_NAMESPACE = "getNamespace";
    public static final String GET_DATABASE_NAME = "getDatabaseName";
    public static final String GET_COLLECTION_NAME = "getCollectionName";

    private static MongoLogger instance = null;

    private PrintStream outputStream = null;

    public synchronized static MongoLogger getInstance() {
        if (instance == null) {
            instance = new MongoLogger();
        }
        return instance;
    }

    public void setOutputStream(PrintStream os) {
        this.outputStream = os;
    }

    private MongoLogger() {

    }

    public void reset() {
        this.outputStream = null;
    }

    public static final String PREFIX = "MONGO_LOGGER";

    private static Object invokeMethod(Class<?> receiverClass, Object receiver, String methodName)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return invokeMethod(receiverClass, receiver, methodName, new Class[]{}, new Object[]{});
    }

    private static Object invokeMethod(Class<?> receiverClass,
                                       Object receiver,
                                       String methodName,
                                       Class<?>[] argumentTypes,
                                       Object[] arguments)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Objects.requireNonNull(receiverClass);
        Objects.requireNonNull(methodName);
        Objects.requireNonNull(argumentTypes);
        Objects.requireNonNull(arguments);
        if (argumentTypes.length != arguments.length) {
            throw new IllegalArgumentException("Mismatch number of argument types and concrete arguments. Argument types "
                    + argumentTypes.length + " while concrete arguments were "
                    + arguments.length);
        }

        Method getNamespaceMethod = receiverClass.getMethod(methodName, argumentTypes);
        boolean getNamespaceMethodIsAccessible = getNamespaceMethod.isAccessible();
        getNamespaceMethod.setAccessible(true);
        Object retVal = getNamespaceMethod.invoke(receiver, arguments);
        getNamespaceMethod.setAccessible(getNamespaceMethodIsAccessible);
        return retVal;
    }


    private static String getDatabaseName(Object mongoCollection) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Object nameSpaceInstance = getNameSpaceInstance(mongoCollection);
        Object databaseNameInstance = invokeMethod(Class.forName("com.mongodb.MongoNamespace"), nameSpaceInstance, GET_DATABASE_NAME);
        return (String) databaseNameInstance;
    }

    private static String getCollectionName(Object mongoCollection) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Object nameSpaceInstance = getNameSpaceInstance(mongoCollection);
        Object collectionNameInstance = invokeMethod(Class.forName("com.mongodb.MongoNamespace"), nameSpaceInstance, GET_COLLECTION_NAME);
        return (String) collectionNameInstance;
    }

    private static Object getNameSpaceInstance(Object mongoCollection) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Class<?> mongoCollectionClass = Class.forName("com.mongodb.client.MongoCollection");
        return invokeMethod(mongoCollectionClass, mongoCollection, GET_NAMESPACE);
    }

    private static String getQueryAsString(Object bson) {
        ObjectMapper mapper = new ObjectMapper();

        String bsonString;
        try {
            bsonString = mapper.writeValueAsString(bson);
        } catch (JsonProcessingException e) {
            bsonString = null;
        }
        return bsonString;
    }

    public void logFind(Object targetCollection, Object findQuery, boolean hasFindExecutionRetrievedAnyDocument) {

        try {
            String databaseNameInstance = getDatabaseName(targetCollection);
            String collectionNameInstance = getCollectionName(targetCollection);
            String bsonString = getQueryAsString(findQuery);

            LoggedExecutedFindOperation log = new LoggedExecutedFindOperation(databaseNameInstance,
                    collectionNameInstance,
                    bsonString,
                    hasFindExecutionRetrievedAnyDocument);

            String jsonString = translateToJSonString(log);

            String mongoOperation = String.format("%s:%s", PREFIX, jsonString);

            if (outputStream == null) {
                // System.out could be changed during execution
                System.out.println(mongoOperation);
            } else {
                outputStream.println(mongoOperation);
            }


        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }


    }

    private static String translateToJSonString(LoggedExecutedFindOperation log) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonString = mapper.writeValueAsString(log);
            return jsonString;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot transform instance of " + LoggedExecutedFindOperation.class.getName() + " to JSON.", e);
        }
    }


}
