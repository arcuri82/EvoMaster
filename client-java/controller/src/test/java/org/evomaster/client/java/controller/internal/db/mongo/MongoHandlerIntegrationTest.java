package org.evomaster.client.java.controller.internal.db.mongo;

import com.mongodb.client.MongoCollection;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.bson.*;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.WrappedResponseDto;
import org.evomaster.client.java.controller.api.dto.mongo.DocumentDto;
import org.evomaster.client.java.controller.api.dto.mongo.FindOperationDto;
import org.evomaster.client.java.controller.api.dto.mongo.FindResultDto;
import org.evomaster.client.java.instrumentation.mongo.MongoLogger;
import org.junit.jupiter.api.*;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.controller.api.ControllerConstants.BASE_PATH;
import static org.evomaster.client.java.controller.api.ControllerConstants.MONGO_COMMAND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoHandlerIntegrationTest extends MongoTestTemplate {

    public static final String DATABASE_NAME = "testdb";

    private static InstrumentedSutStarter starter;
    private static MongoFakeSutController sutController;
    private static String url;

    @BeforeAll
    public static void startController() {
        sutController = buildSutController();
        starter = new InstrumentedSutStarter(sutController);
        url = start(starter);
        url += BASE_PATH;
    }

    @BeforeEach
    public void dropMongoDatabase() {
        mongoClient.getDatabase(DATABASE_NAME).drop();
        sutController.initMongoClient();
    }

    @AfterEach
    public void resetMongoHandler() {
        sutController.getMongoHandler().reset();
    }

    @AfterAll
    public static void stopController() {
        if (starter != null) {
            starter.stop();
        }
    }


    @Test
    public void testEmptyDistances() {
        startNewTest(url);
        assertTrue(sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.isEmpty());
    }

    @Test
    public void testFindNoResult() {
        startNewTest(url);
        assertTrue(sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.isEmpty());

        MongoCollection<Document> mongoCollection = mongoClient.getDatabase(DATABASE_NAME).getCollection("customers");

        // find({}) returns no element
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument(), true);

        assertEquals(1, sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.size());
    }


    @Test
    public void testTwoFindOperations() {
        startNewTest(url);
        assertTrue(sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.isEmpty());

        MongoCollection<Document> mongoCollection = mongoClient.getDatabase(DATABASE_NAME).getCollection("customers");

        // log find({}) operation
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument(), true);

        assertEquals(1, sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.size());

        // log find({}) operation
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument(), true);

        assertEquals(2, sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.size());
    }


    @Test
    public void testFindSomeResult() {
        startNewTest(url);
        assertTrue(sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.isEmpty());

        MongoCollection<Document> mongoCollection = mongoClient.getDatabase(DATABASE_NAME).getCollection("customers");

        // find({}) returns no element
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument(), true);

        assertEquals(1, sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.size());

        mongoCollection.insertOne(new Document());

        // log find({}) that returns an element
        MongoLogger.getInstance().logFind(mongoCollection, new BsonDocument(), true);
        assertEquals(2, sutController.getMongoHandler().getMongoExecutionDto().executedFindOperationDtos.size());
    }

    @Test
    public void postMongoCommand() throws JsonProcessingException {
        startNewTest(url);

        FindOperationDto requestDto = new FindOperationDto();
        requestDto.databaseName = DATABASE_NAME;
        requestDto.collectionName = "mycollection";
        DocumentDto documentDto = new DocumentDto();
        documentDto.documentAsJsonString = new ObjectMapper().writeValueAsString(new BsonDocument());
        requestDto.queryDocumentDto = documentDto;

        this.mongoClient.getDatabase(DATABASE_NAME).getCollection("mycollection").deleteMany(new BsonDocument());

        Object dataResponse = given().contentType(ContentType.JSON)
                .body(requestDto)
                .post(url + MONGO_COMMAND)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .as(WrappedResponseDto.class).data;

        assertTrue(dataResponse instanceof Map);
        Map responseMap = (Map) dataResponse;


        assertEquals(false, responseMap.get(FindResultDto.HAS_RETURNED_ANY_DOCUMENT_FIELD_NAME));
    }

    @Test
    public void postMongoCommandOnNonEmpty() throws JsonProcessingException {
        startNewTest(url);

        Document anEmptyDocument = new Document();
        this.mongoClient.getDatabase(DATABASE_NAME).getCollection("mycollection").insertOne(anEmptyDocument);

        FindOperationDto requestDto = new FindOperationDto();
        requestDto.databaseName = DATABASE_NAME;
        requestDto.collectionName = "mycollection";
        DocumentDto documentDto = new DocumentDto();
        documentDto.documentAsJsonString = new ObjectMapper().writeValueAsString(new BsonDocument());
        requestDto.queryDocumentDto = documentDto;

        Object responseData = given().contentType(ContentType.JSON)
                .body(requestDto)
                .post(url + MONGO_COMMAND)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .as(WrappedResponseDto.class).data;

        assertTrue(responseData instanceof Map);
        Map responseMap = (Map) responseData;

        assertEquals(true, responseMap.get(FindResultDto.HAS_RETURNED_ANY_DOCUMENT_FIELD_NAME));

        assertTrue(responseMap.get(FindResultDto.DOCUMENTS_FIELD_NAME) instanceof List);
        List documents = (List) responseMap.get(FindResultDto.DOCUMENTS_FIELD_NAME);
        assertEquals(1, documents.size());
    }

    @Test
    public void postMongoCommandOnNonEmptyDocuments() throws IOException {
        startNewTest(url);

        Document aDocument = new Document();
        aDocument.append("firstName", new BsonString("John"));
        aDocument.append("lastName", new BsonString("Doe"));
        aDocument.append("age", new BsonInt32(32));
        aDocument.append("address", new BsonNull());
        this.mongoClient.getDatabase(DATABASE_NAME).getCollection("mycollection").insertOne(aDocument);

        FindOperationDto requestDto = new FindOperationDto();
        requestDto.databaseName = DATABASE_NAME;
        requestDto.collectionName = "mycollection";
        DocumentDto documentDto = new DocumentDto();
        documentDto.documentAsJsonString = new ObjectMapper().writeValueAsString(new BsonDocument());
        requestDto.queryDocumentDto = documentDto;

        WrappedResponseDto response = given().contentType(ContentType.JSON)
                .body(requestDto)
                .post(url + MONGO_COMMAND)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .as(WrappedResponseDto.class);

        assertTrue(response.data instanceof Map);

        BasicBSONObject doc = new BasicBSONObject();
        doc.putAll((Map) response.data);

        assertTrue(doc.getBoolean(FindResultDto.HAS_RETURNED_ANY_DOCUMENT_FIELD_NAME));
        assertTrue(doc.get(FindResultDto.DOCUMENTS_FIELD_NAME) instanceof List);

        List documents = (List) doc.get(FindResultDto.DOCUMENTS_FIELD_NAME);
        assertEquals(1, documents.size());

        assertTrue(documents.get(0) instanceof Map);

        BasicBSONObject storedDoc = new BasicBSONObject();
        storedDoc.putAll((Map) documents.get(0));

        String jsonString = (String) storedDoc.get("documentAsJsonString");
        BsonDocument bson = BsonDocument.parse(jsonString);

        assertEquals("John", bson.get("firstName").asString().getValue());
        assertEquals("Doe", bson.get("lastName").asString().getValue());
        assertEquals(new BsonNull(), bson.get("address"));
        assertEquals(32, bson.get("age").asInt32().getValue());

    }
}
