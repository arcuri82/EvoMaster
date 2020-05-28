package org.evomaster.client.java.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.api.dto.SutRunDto;
import org.evomaster.client.java.controller.db.DatabaseFakeSutController;
import org.evomaster.client.java.controller.db.SqlScriptRunner;
import org.evomaster.client.java.instrumentation.InstrumentingAgent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.DriverManager;

import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.controller.api.ControllerConstants.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class DatabaseTestTemplate {

    /*
        Useful link:
        https://www.tutorialspoint.com/sql/index.htm
     */

    private static Connection connection;

    @BeforeAll
    public static void initClass() throws Exception {

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        InstrumentingAgent.initP6Spy("org.h2.Driver");

        connection = DriverManager.getConnection("jdbc:p6spy:h2:mem:db_test", "sa", "");
    }

    @BeforeEach
    public void initTest() throws Exception {

        /*
            Not supported in H2
            SqlScriptRunner.execCommand(connection, "DROP DATABASE db_test;");
            SqlScriptRunner.execCommand(connection, "CREATE DATABASE db_test;");
        */

        //custom H2 command
        SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;");
    }

    protected Connection getConnection(){
        return connection;
    }


    protected String start(InstrumentedSutStarter starter) {
        boolean started = starter.start();
        assertTrue(started);

        int port = starter.getControllerServerPort();

        startSut(port);

        return "http://localhost:" + port;
    }

    protected void startSut(int port) {
        boolean calculateSqlHeuristics = true;
        boolean enableMongoExtraction = false;
        given().contentType(ContentType.JSON)
                .body(new SutRunDto(true, false, calculateSqlHeuristics, enableMongoExtraction))
                .put("http://localhost:" + port + BASE_PATH + RUN_SUT_PATH)
                .then()
                .statusCode(204);
    }

    protected void startNewActionInSameTest(String url, int index){

        given().accept(ContentType.ANY)
                .contentType(ContentType.JSON)
                .body("{\"index\":" + index + "}")
                .put(url + NEW_ACTION)
                .then()
                .statusCode(204);
    }

    protected void startNewTest(String url){
        boolean calculateSqlHeuristics = true;
        boolean enableMongoExtraction = false;

        given().accept(ContentType.ANY)
                .contentType(ContentType.JSON)
                .body(new SutRunDto(true, true, calculateSqlHeuristics, enableMongoExtraction))
                .put(url + RUN_SUT_PATH)
                .then()
                .statusCode(204);

        startNewActionInSameTest(url, 0);
    }


    protected InstrumentedSutStarter getInstrumentedSutStarter() {
        DatabaseFakeSutController sutController = new DatabaseFakeSutController(connection);
        sutController.setControllerPort(0);
        return new InstrumentedSutStarter(sutController);
    }
}
