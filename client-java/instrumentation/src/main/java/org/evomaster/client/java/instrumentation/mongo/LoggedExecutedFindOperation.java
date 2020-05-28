package org.evomaster.client.java.instrumentation.mongo;


public class LoggedExecutedFindOperation {

    private String databaseName;

    private String collectionName;

    private String queryDocumentAsJsonString;

    private boolean hasReturnedAnyDocument;

    public LoggedExecutedFindOperation() {

    }

    public LoggedExecutedFindOperation(String databaseName, String collectionName, String queryDocumentAsJsonString, boolean hasReturnedAnyDocument) {
        this.databaseName = databaseName;
        this.collectionName = collectionName;
        this.queryDocumentAsJsonString = queryDocumentAsJsonString;
        this.hasReturnedAnyDocument = hasReturnedAnyDocument;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getQueryDocumentAsJsonString() {
        return queryDocumentAsJsonString;
    }

    public boolean getHasReturnedAnyDocument() {
        return hasReturnedAnyDocument;
    }

}
