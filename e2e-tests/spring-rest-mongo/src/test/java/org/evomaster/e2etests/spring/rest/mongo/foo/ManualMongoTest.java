package org.evomaster.e2etests.spring.rest.mongo.foo;

import com.foo.mongo.MyMongoAppEmbeddedController;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.evomaster.client.java.controller.api.Formats;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.internal.db.StandardOutputTracker;
import org.evomaster.e2etests.spring.rest.mongo.SpringRestMongoTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ManualMongoTest extends SpringRestMongoTestBase {

    private static final MyMongoAppEmbeddedController sutController = new MyMongoAppEmbeddedController();

    @BeforeAll
    public static void init() throws Exception {
        SpringRestMongoTestBase.initClass(sutController);
    }

    @BeforeEach
    public void turnOnTracker() {
        StandardOutputTracker.setTracker(true, sutController);

    }

    @AfterEach
    public void turnOffTracker() {
        StandardOutputTracker.setTracker(false, sutController);
    }

    @Test
    public void testSwaggerJSON() {

        SutInfoDto dto = remoteController.getSutInfo();

        String swaggerJson = given().accept(Formats.JSON_V1)
                .get(dto.restProblem.swaggerJsonUrl)
                .then()
                .statusCode(200)
                .extract().asString();

        Swagger swagger = new SwaggerParser().parse(swaggerJson);

        assertEquals("/", swagger.getBasePath());
        assertEquals(15, swagger.getPaths().size());
    }

    @Test
    public void testManualPostAndThenGet() {
        String url = baseUrlOfSut + "/api/mymongoapp/foo";

        given().post(url)
                .then()
                .statusCode(200);

        given().get(url)
                .then()
                .statusCode(200);
    }

    @Test
    public void testManualGetOnEmpty() {
        String url = baseUrlOfSut + "/api/mymongoapp/foo";

        given()
                .get(url)
                .then()
                .statusCode(404);
    }

    @Test
    public void testManualPostGetDeleteGet() {
        String url = baseUrlOfSut + "/api/mymongoapp/foo";

        given().post(url)
                .then()
                .statusCode(200);

        given().get(url)
                .then()
                .statusCode(200);

        given().delete(url)
                .then()
                .statusCode(200);

        given().get(url)
                .then()
                .statusCode(404);

    }

    @Test
    public void testManualDeleteOnEmpty() {
        String url = baseUrlOfSut + "/api/mymongoapp/foo";

        given()
                .delete(url)
                .then()
                .statusCode(404);
    }


    @Test
    public void testManualDuplicatePost() {
        String url = baseUrlOfSut + "/api/mymongoapp/foo";

        given().post(url)
                .then()
                .statusCode(200);

        given().post(url)
                .then()
                .statusCode(400);
    }


}