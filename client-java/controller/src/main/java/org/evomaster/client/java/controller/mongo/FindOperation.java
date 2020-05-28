package org.evomaster.client.java.controller.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.evomaster.client.java.controller.api.dto.mongo.DocumentDto;
import org.evomaster.client.java.controller.api.dto.mongo.FindOperationDto;

import java.io.IOException;
import java.util.Objects;

/**
 * A find(...) operation for a given database and collection over a MongoDB instance.
 */
public class FindOperation {

    public FindOperation(String databaseName, String collectionName, Document query) {
        Objects.requireNonNull(databaseName);
        Objects.requireNonNull(collectionName);
        Objects.requireNonNull(query);

        this.databaseName = databaseName;
        this.collectionName = collectionName;
        this.query = query;
    }

    private final String databaseName;

    private final String collectionName;

    private final Document query;

    public static FindOperation fromDto(FindOperationDto dto) {
        ObjectMapper mapper = new ObjectMapper();
        try {

            return new FindOperation(dto.databaseName,
                    dto.collectionName,
                    mapper.readValue(dto.queryDocumentDto.documentAsJsonString, Document.class));
        } catch (IOException e) {
            throw new RuntimeException("Could not read from " + Document.class.getName() + " from string " + dto.queryDocumentDto);
        }
    }

    public Document getQuery() {
        return query;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public FindOperationDto toDto() {
        ObjectMapper mapper = new ObjectMapper();
        FindOperationDto dto = new FindOperationDto();
        dto.databaseName = this.databaseName;
        dto.collectionName = this.collectionName;
        try {
            DocumentDto documentdto = new DocumentDto();
            documentdto.documentAsJsonString = mapper.writeValueAsString(this.query);
            dto.queryDocumentDto = documentdto;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not write query into JSON string: " + this.query);
        }
        return dto;
    }
}
