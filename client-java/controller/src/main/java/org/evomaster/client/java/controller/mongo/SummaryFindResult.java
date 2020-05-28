package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.controller.api.dto.mongo.FindResultDto;

public class SummaryFindResult extends FindResult {

    private final boolean hasOperationFoundAnyDocuments;

    public SummaryFindResult(boolean hasOperationFoundAnyDocuments) {
        this.hasOperationFoundAnyDocuments = hasOperationFoundAnyDocuments;
    }

    public boolean hasOperationFoundAnyDocuments() {
        return hasOperationFoundAnyDocuments;
    }

    @Override
    public FindResultDto toDto() {
        FindResultDto dto = new FindResultDto();
        dto.findResultType = FindResultDto.FindResultType.SUMMARY;
        dto.hasReturnedAnyDocument = hasOperationFoundAnyDocuments;
        return dto;
    }

}
