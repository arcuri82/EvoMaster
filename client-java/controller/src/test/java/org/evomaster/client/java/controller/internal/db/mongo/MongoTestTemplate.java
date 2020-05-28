package org.evomaster.client.java.controller.internal.db.mongo;

import com.mongodb.MongoClient;
import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutRunDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;

import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.controller.api.ControllerConstants.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class MongoTestTemplate {

    protected static final int MONGO_PORT = 27017;
    protected static GenericContainer mongo;
    protected static MongoClient mongoClient;

    @BeforeAll
    public static void startMongoContainer() {
        mongo = new GenericContainer("mongo:3.2")
                .withExposedPorts(MONGO_PORT);
        mongo.start();
        mongoClient = new MongoClient(mongo.getContainerIpAddress(), mongo.getMappedPort(MONGO_PORT));
    }

    @AfterAll
    public static void stopMongoContainer() {
        if (mongo != null) {
            mongo.stop();
        }
    }

    @BeforeEach
    public void dropAllDatabases() {
        for (String databaseName : mongoClient.listDatabaseNames()) {
            mongoClient.getDatabase(databaseName).drop();
        }
    }

    protected static MongoFakeSutController buildSutController() {
        MongoFakeSutController sutController = new MongoFakeSutController(mongoClient);
        sutController.setControllerPort(0);
        return sutController;
    }


    protected static String start(InstrumentedSutStarter starter) {
        boolean started = starter.start();
        assertTrue(started);

        int port = starter.getControllerServerPort();

        startSut(port);

        return "http://localhost:" + port;
    }

    protected static void startSut(int port) {
        boolean calculateSqlHeuristics = false;
        boolean enableMongoExtraction = true;

        given().contentType(ContentType.JSON)
                .body(new SutRunDto(true, false, calculateSqlHeuristics, enableMongoExtraction))
                .put("http://localhost:" + port + BASE_PATH + RUN_SUT_PATH)
                .then()
                .statusCode(204);
    }

    protected void startNewActionInSameTest(String url, int index) {

        given().accept(ContentType.ANY)
                .contentType(ContentType.JSON)
                .body("{\"index\":" + index + "}")
                .put(url + NEW_ACTION)
                .then()
                .statusCode(204);
    }

    protected void startNewTest(String url) {

        given().accept(ContentType.ANY)
                .contentType(ContentType.JSON)
                .body(new SutRunDto(true, true, true, true, true))
                .put(url + RUN_SUT_PATH)
                .then()
                .statusCode(204);

        startNewActionInSameTest(url, 0);
    }
}
