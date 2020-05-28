package org.evomaster.client.java.controller.internal.db.mongo;

import org.evomaster.client.java.controller.api.dto.mongo.MongoExecutionDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MongoHandlerTest {

    @Test
    public void testIsExtractMongoExecution() {
        MongoHandler mongoHandler = new MongoHandler();
        assertFalse(mongoHandler.isExtractMongoExecution());
        mongoHandler.setExtractMongoExecution(true);
        assertTrue(mongoHandler.isExtractMongoExecution());
    }

    @Test
    public void testHandle() {
        MongoHandler mongoHandler = new MongoHandler();
        mongoHandler.setExtractMongoExecution(true);
        mongoHandler.handle("{\"databaseName\":\"mydb\",\"collectionName\":\"mycollection\",\"queryDocumentAsJsonString\":\"{}\"}\n");
        MongoExecutionDto dto = mongoHandler.getMongoExecutionDto();
        assertEquals(1, dto.executedFindOperationDtos.size());
        assertEquals("mydb", dto.executedFindOperationDtos.get(0).findOperationDto.databaseName);
        assertEquals("mycollection", dto.executedFindOperationDtos.get(0).findOperationDto.collectionName);
        assertEquals("{}", dto.executedFindOperationDtos.get(0).findOperationDto.queryDocumentDto.documentAsJsonString);
    }

    @Test
    public void testReset() {
        MongoHandler mongoHandler = new MongoHandler();
        mongoHandler.setExtractMongoExecution(true);
        assertEquals(0, mongoHandler.getMongoExecutionDto().executedFindOperationDtos.size());

        mongoHandler.handle("{\"databaseName\":\"mydb\",\"collectionName\":\"mycollection\",\"queryDocumentAsJsonString\":\"{}\"}\n");
        assertEquals(1, mongoHandler.getMongoExecutionDto().executedFindOperationDtos.size());

        mongoHandler.reset();
        assertEquals(0, mongoHandler.getMongoExecutionDto().executedFindOperationDtos.size());
    }
}
