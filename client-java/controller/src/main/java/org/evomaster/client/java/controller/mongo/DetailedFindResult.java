package org.evomaster.client.java.controller.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.evomaster.client.java.controller.api.dto.mongo.DocumentDto;
import org.evomaster.client.java.controller.api.dto.mongo.FindResultDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DetailedFindResult extends FindResult {

    private final List<Document> documentList = new ArrayList<>();

    public void addDocument(Document document) {
        Objects.requireNonNull(document);
        documentList.add(document);
    }

    public boolean hasOperationFoundAnyDocuments() {
        return !documentList.isEmpty();
    }

    @Override
    public FindResultDto toDto() {
        FindResultDto dto = new FindResultDto();
        dto.findResultType = FindResultDto.FindResultType.DETAILED;
        dto.documents = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        for (Document document : this.documentList) {
            DocumentDto documentDto = new DocumentDto();
            try {
                String documentStr = mapper.writeValueAsString(document);
                documentDto.documentAsJsonString = documentStr;

            } catch (JsonProcessingException e) {
                documentDto.documentAsJsonString = null;
            }
            dto.documents.add(documentDto);
        }
        dto.hasReturnedAnyDocument = !this.documentList.isEmpty();
        return dto;
    }

}
