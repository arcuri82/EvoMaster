package org.evomaster.client.java.controller.internal.db.mongo;

import com.mongodb.MongoClient;
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

import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

public class MongoFakeSutController extends EmbeddedSutController {
    private final MongoClient mongoClient;


    public MongoFakeSutController(MongoClient mongoClient) {
        Objects.requireNonNull(mongoClient);
        this.mongoClient = mongoClient;
    }

    @Override
    public Connection getConnection() {
        return null;
    }

    @Override
    public String getDatabaseDriverName() {
        return null;
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(null, null);
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    @Override
    public String startSut() {
        return "foo";
    }

    @Override
    public void stopSut() {

    }

    @Override
    public void resetStateOfSUT() {
    }

    @Override
    public boolean isSutRunning() {
        return false;
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "none";
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    public MongoHandler getMongoHandler() {
        return this.mongoHandler;
    }


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
