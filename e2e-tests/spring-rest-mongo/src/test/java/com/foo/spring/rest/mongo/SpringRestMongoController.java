package com.foo.spring.rest.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.mongo.FindOperationDto;
import org.evomaster.client.java.controller.mongo.DetailedFindResult;
import org.evomaster.client.java.controller.mongo.FindOperation;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

public abstract class SpringRestMongoController extends EmbeddedSutController {

    private final Class<?> applicationClass;

    private ConfigurableApplicationContext ctx;
    private MongoClient mongoClient;

    public SpringRestMongoController(Class<?> applicationClass) {
        this.applicationClass = applicationClass;
    }

    private final static int MONGO_PORT = 27017; //https://docs.mongodb.com/manual/reference/default-mongodb-port/

    private final GenericContainer mongodb = new GenericContainer("mongo:3.2")
            .withExposedPorts(MONGO_PORT);

    @Override
    public boolean isSutRunning() {
        return ctx != null && ctx.isRunning();
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    @Override
    public Connection getConnection() {
        return null;
    }

    @Override
    public String getDatabaseDriverName() {
        return "org.h2.Driver";
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v2/api-docs",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_4;
    }

    @Override
    public String startSut() {

        mongodb.start();
        ServerAddress serverAddress = new ServerAddress(mongodb.getContainerIpAddress(), mongodb.getMappedPort(MONGO_PORT));
        MongoClientOptions mongoClientOptions = MongoClientOptions.builder()
                .build();
        mongoClient = new MongoClient(serverAddress, mongoClientOptions);

        String host = mongodb.getContainerIpAddress();
        int port = mongodb.getMappedPort(MONGO_PORT);

        ctx = SpringApplication.run(applicationClass,
                "--server.port=0",
                //               "--spring.mongodb.embedded.version=3.2",
                "--spring.data.mongodb.host=" + host,
                "--spring.data.mongodb.port=" + port,
                "--spring.data.mongodb.database=testdb",
                "--spring.main.allow-bean-definition-overriding=true",
                "--logging.level.org.springframework.data.mongodb.core.MongoTemplate=DEBUG"); // start app with mongodb connected

        return "http://localhost:" + getSutPort();

    }

    private Integer getSutPort() {
        return (Integer) ((Map) ctx.getEnvironment()
                .getPropertySources().get("server.ports").getSource())
                .get("local.server.port");
    }

    @Override
    public void stopSut() {
        ctx.stop();
        ctx.close();
        mongodb.stop();
    }

    @Override
    public void resetStateOfSUT() {
        mongoClient.getDatabase("testdb").drop();
    }

    @Override
    public DetailedFindResult executeMongoFindOperation(FindOperationDto dto) {
        FindOperation findOperation = FindOperation.fromDto(dto);
        MongoDatabase database = this.mongoClient.getDatabase(findOperation.getDatabaseName());
        MongoCollection collection = database.getCollection(findOperation.getCollectionName());
        FindIterable<Document> findIterable = collection.find(findOperation.getQuery());
        DetailedFindResult findResult = new DetailedFindResult();
        StreamSupport.stream(findIterable.spliterator(), false).forEach(findResult::addDocument);
        return findResult;
    }
}
