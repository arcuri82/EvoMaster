package org.evomaster.e2etests.spring.rest.mongo.foo;

import com.foo.mongo.MyMongoAppEmbeddedController;
import com.foo.mongo.person.AddressDto;
import com.foo.mongo.person.PersonDto;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.evomaster.client.java.controller.internal.db.StandardOutputTracker;
import org.evomaster.e2etests.spring.rest.mongo.SpringRestMongoTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MongoFilterTest extends SpringRestMongoTestBase {

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

    @NotNull
    private static PersonDto buildJohnDoeDto() {
        AddressDto johnDoeAddressDto = new AddressDto();
        johnDoeAddressDto.streetName = "Some street";
        johnDoeAddressDto.streetNumber = 42;

        PersonDto johnDoeDto = new PersonDto();
        johnDoeDto.firstName = "John";
        johnDoeDto.lastName = "Doe";
        johnDoeDto.age = 42;
        johnDoeDto.address = johnDoeAddressDto;
        return johnDoeDto;
    }

    @NotNull
    private static PersonDto buildJackDoeDto() {
        AddressDto jackDoeAddressDto = new AddressDto();
        jackDoeAddressDto.streetName = "Some street";
        jackDoeAddressDto.streetNumber = 42;

        PersonDto jackDoeDto = new PersonDto();
        jackDoeDto.firstName = "Jack";
        jackDoeDto.lastName = "Doe";
        jackDoeDto.address = jackDoeAddressDto;
        jackDoeDto.age = 52;
        return jackDoeDto;
    }

    private static ValidatableResponse add(PersonDto johnDoeDto) {
        return given()
                .contentType(ContentType.JSON)
                .body(johnDoeDto)
                .post(baseUrlOfSut + "/api/mongoperson/add")
                .then()
                .statusCode(200);
    }

    private static PersonDto[] findByLastName(String lastName) {
        return given()
                .accept("*/*")
                .get(baseUrlOfSut + "/api/mongoperson/findByLastName/{lastName}",lastName)
                .then()
                .statusCode(200)
                .extract()
                .as(PersonDto[].class);
    }

    private static PersonDto[] findAll() {
        return given()
                .get(baseUrlOfSut + "/api/mongoperson/findAll")
                .then()
                .statusCode(200)
                .extract()
                .as(PersonDto[].class);
    }

    private static PersonDto[] findByAddressNotNull() {
        return given()
                .get(baseUrlOfSut + "/api/mongoperson/findByAddressNotNull")
                .then()
                .statusCode(200)
                .extract()
                .as(PersonDto[].class);
    }

    private static PersonDto[] findByFirstNameNull() {
        return given()
                .get(baseUrlOfSut + "/api/mongoperson/findByFirstNameNull")
                .then()
                .statusCode(200)
                .extract()
                .as(PersonDto[].class);
    }

    private static PersonDto[] findByAgeGreaterThan(int age) {
        return given()
                .accept("*/*")
                .body(age)
                .get(baseUrlOfSut + "/api/mongoperson/findByAgeGreaterThan/{age}", age)
                .then()
                .statusCode(200)
                .extract()
                .as(PersonDto[].class);
    }

    private static PersonDto[] findByAge(int age) {
        return given()
                .accept("*/*")
                .get(baseUrlOfSut + "/api/mongoperson/findByAge/{age}", age)
                .then()
                .statusCode(200)
                .extract()
                .as(PersonDto[].class);
    }

    private static PersonDto[] findByAgeLessThan(int age) {
        return given()
                .accept("*/*")
                .get(baseUrlOfSut + "/api/mongoperson/findByAgeLessThan/{age}",age)
                .then()
                .statusCode(200)
                .extract()
                .as(PersonDto[].class);
    }

    private static PersonDto[] findByAgeBetween(int from, int to) {
        return given()
                .accept("*/*")
                .get(baseUrlOfSut + "/api/mongoperson/findByAgeBetween/{from}/{to}", from, to)
                .then()
                .statusCode(200)
                .extract()
                .as(PersonDto[].class);
    }

    private static PersonDto[] findByFirstNameLike(String name) {
        return given()
                .accept("*/*")
                .get(baseUrlOfSut + "/api/mongoperson/findByFirstNameLike/{name}",name)
                .then()
                .statusCode(200)
                .extract()
                .as(PersonDto[].class);
    }

    private static PersonDto[] findByFirstNameRegex(String name) {
        return given()
                .accept("*/*")
                .get(baseUrlOfSut + "/api/mongoperson/findByFirstNameRegex/{name}",name)
                .then()
                .statusCode(200)
                .extract()
                .as(PersonDto[].class);
    }


    @Test
    public void testManualFindByLastName() {

        PersonDto[] personDtos = findByLastName("Doe");

        assertNotNull(personDtos);
        assertEquals(0, personDtos.length);

        AddressDto addressDto = new AddressDto();
        addressDto.streetName = "Some street";
        addressDto.streetNumber = 42;

        PersonDto johnDoeDto = new PersonDto();
        johnDoeDto.firstName = "John";
        johnDoeDto.lastName = "Doe";
        johnDoeDto.age = 42;
        johnDoeDto.address = addressDto;

        add(johnDoeDto);


        personDtos = findByLastName("Doe");

        assertNotNull(personDtos);
        assertEquals(1, personDtos.length);
        assertEquals("Doe", personDtos[0].lastName);

        PersonDto jackDoeDto = new PersonDto();
        jackDoeDto.firstName = "Jack";
        jackDoeDto.lastName = "Doe";
        jackDoeDto.address = addressDto;
        jackDoeDto.age = 52;

        add(jackDoeDto);

        personDtos = findByLastName("Doe");

        assertNotNull(personDtos);
        assertEquals(2, personDtos.length);
        assertEquals("Doe", personDtos[0].lastName);
        assertEquals("Doe", personDtos[1].lastName);

    }


    @Test
    public void testManualFindByAgeGreaterThan() {

        PersonDto[] personDtos = findAll();

        assertNotNull(personDtos);
        assertEquals(0, personDtos.length);

        PersonDto johnDoeDto = buildJohnDoeDto();
        PersonDto jackDoeDto = buildJackDoeDto();

        add(johnDoeDto);
        add(jackDoeDto);


        personDtos = findByAgeGreaterThan(42);

        assertNotNull(personDtos);
        assertEquals(1, personDtos.length);
        assertEquals(52, personDtos[0].age);


    }

    @Test
    public void testManualFindByAgeLessThan() {

        PersonDto[] personDtos = findByAgeLessThan(100);

        assertNotNull(personDtos);
        assertEquals(0, personDtos.length);

        PersonDto johnDoeDto = buildJohnDoeDto();
        PersonDto jackDoeDto = buildJackDoeDto();

        add(johnDoeDto);
        add(jackDoeDto);


        personDtos = findByAgeLessThan(52);

        assertNotNull(personDtos);
        assertEquals(1, personDtos.length);
        assertEquals(42, personDtos[0].age);

    }

    @Test
    public void testManualFindByAgeBetween() {

        PersonDto[] personDtos = findByAgeBetween(0, 100);

        assertNotNull(personDtos);
        assertEquals(0, personDtos.length);

        PersonDto johnDoeDto = buildJohnDoeDto();
        PersonDto jackDoeDto = buildJackDoeDto();

        add(johnDoeDto);
        add(jackDoeDto);


        personDtos = findByAgeBetween(0, 50);

        assertNotNull(personDtos);
        assertEquals(1, personDtos.length);
        assertEquals(42, personDtos[0].age);

    }

    @Test
    public void testManualFindByFirstNameNotNull() {

        PersonDto[] personDtos = findByAddressNotNull();

        assertNotNull(personDtos);
        assertEquals(0, personDtos.length);

        PersonDto johnDoeDto = buildJohnDoeDto();

        add(johnDoeDto);


        personDtos = findByAddressNotNull();

        assertNotNull(personDtos);
        assertEquals(1, personDtos.length);
        assertEquals("John", personDtos[0].firstName);

    }


    @Test
    public void testManualFindByFirstNameNull() {

        PersonDto[] personDtos = findByFirstNameNull();

        assertNotNull(personDtos);
        assertEquals(0, personDtos.length);

        PersonDto dto = buildJohnDoeDto();
        dto.firstName = null;
        add(dto);

        add(buildJohnDoeDto());
        add(buildJackDoeDto());

        personDtos = findByFirstNameNull();

        assertNotNull(personDtos);
        assertEquals(1, personDtos.length);
        assertEquals(null, personDtos[0].firstName);

    }

    @Test
    public void testManualFindByFirstNameLike() {

        PersonDto[] personDtos = findAll();
        assertNotNull(personDtos);
        assertEquals(0, personDtos.length);

        add(buildJohnDoeDto());
        add(buildJackDoeDto());

        personDtos = findByFirstNameLike("oh");

        assertNotNull(personDtos);
        assertEquals(1, personDtos.length);
        assertEquals("John", personDtos[0].firstName);

    }

    @Test
    public void testManualFindByFirstNameRegex() {

        PersonDto[] personDtos = findAll();
        assertNotNull(personDtos);
        assertEquals(0, personDtos.length);

        add(buildJohnDoeDto());
        add(buildJackDoeDto());

        personDtos = findByFirstNameRegex("^Jo");

        assertNotNull(personDtos);
        assertEquals(1, personDtos.length);
        assertEquals("John", personDtos[0].firstName);

        personDtos = findByFirstNameRegex("n$");
        assertNotNull(personDtos);
        assertEquals(1, personDtos.length);
        assertEquals("John", personDtos[0].firstName);


    }

    @Test
    public void testManualFindByAge() {

        PersonDto[] personDtos = findAll();

        assertNotNull(personDtos);
        assertEquals(0, personDtos.length);

        PersonDto johnDoeDto = buildJohnDoeDto();
        PersonDto jackDoeDto = buildJackDoeDto();

        add(johnDoeDto);
        add(jackDoeDto);


        personDtos = findByAge(33);

        assertNotNull(personDtos);
        assertEquals(0, personDtos.length);


    }

}