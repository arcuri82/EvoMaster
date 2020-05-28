package org.evomaster.client.java.controller.internal.db.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.client.java.controller.api.dto.mongo.*;
import org.evomaster.client.java.instrumentation.mongo.LoggedExecutedFindOperation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The client-side MongoHandler basically records interactions
 * between the client and the MongoDB database
 */
public class MongoHandler {

    private final List<String> mongoOperations;

    private volatile boolean extractMongoExecution = false;

    public MongoHandler() {
        mongoOperations = new ArrayList<>();
    }

    public void reset() {
        mongoOperations.clear();
    }

    public void handle(String mongoOperation) {
        Objects.requireNonNull(mongoOperation);

        if (!extractMongoExecution) {
            return;
        }

        this.mongoOperations.add(mongoOperation);
    }


    public boolean isExtractMongoExecution() {
        return extractMongoExecution;
    }


    public void setExtractMongoExecution(boolean extractMongoExecution) {
        this.extractMongoExecution = extractMongoExecution;
    }

    public MongoExecutionDto getMongoExecutionDto() {
        if (!extractMongoExecution) {
            return null;
        }

        MongoExecutionDto dto = new MongoExecutionDto();
        dto.executedFindOperationDtos.addAll(mongoOperations.stream().map(MongoHandler::toDto).collect(Collectors.toList()));
        return dto;
    }

    private static ExecutedFindOperationDto toDto(String loggedExecutedFindOperationJsonStr) {
        ObjectMapper jacksonMapper = new ObjectMapper();

        LoggedExecutedFindOperation loggedExecutedFindOperation = null;
        try {
            loggedExecutedFindOperation = jacksonMapper.readValue(loggedExecutedFindOperationJsonStr, LoggedExecutedFindOperation.class);
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + LoggedExecutedFindOperation.class.getName() + " instance from string " + loggedExecutedFindOperationJsonStr, e);
        }

        FindOperationDto findOperationDto = new FindOperationDto();
        findOperationDto.databaseName = loggedExecutedFindOperation.getDatabaseName();
        findOperationDto.collectionName = loggedExecutedFindOperation.getCollectionName();
        DocumentDto documentDto = new DocumentDto();
        documentDto.documentAsJsonString = loggedExecutedFindOperation.getQueryDocumentAsJsonString();
        findOperationDto.queryDocumentDto = documentDto;

        FindResultDto findResultDto = new FindResultDto();
        findResultDto.findResultType = FindResultDto.FindResultType.SUMMARY;
        findResultDto.hasReturnedAnyDocument = loggedExecutedFindOperation.getHasReturnedAnyDocument();

        ExecutedFindOperationDto dto = new ExecutedFindOperationDto();
        dto.findOperationDto = findOperationDto;
        dto.findResultDto = findResultDto;
        return dto;
    }
}