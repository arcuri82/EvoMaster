package org.evomaster.client.java.controller.mongo;


import org.evomaster.client.java.controller.api.dto.mongo.ExecutedFindOperationDto;

import java.util.Objects;

/**
 * A mongo operation that was executed and contains a
 * result of the mongo operation in the database.
 */
public class ExecutedFindOperation {

    private final FindOperation operation;

    private final boolean hasOperationFoundAnyDocuments;

    private final FindResult result;

    public ExecutedFindOperation(FindOperation operation, SummaryFindResult result) {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(result);

        this.operation = operation;
        this.result = result;
        this.hasOperationFoundAnyDocuments = result.hasOperationFoundAnyDocuments();
    }

    public boolean hasOperationFoundAnyDocuments() {
        return hasOperationFoundAnyDocuments;
    }

    public FindOperation getOperation() {
        return operation;
    }

    public FindResult getResult() {
        return result;
    }

    public ExecutedFindOperationDto toDto() {
        ExecutedFindOperationDto dto = new ExecutedFindOperationDto();
        dto.findOperationDto =this.operation.toDto();
        dto.findResultDto = this.result.toDto();
        return dto;
    }

}
