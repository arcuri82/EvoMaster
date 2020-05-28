package org.evomaster.core.mongo

import org.bson.BsonDocument
import org.bson.Document
import org.bson.codecs.DecoderContext
import org.evomaster.client.java.controller.api.dto.mongo.FindOperationDto
import org.bson.codecs.DocumentCodec


class FindOperation(val databaseName: String,
                    val collectionName: String,
                    val queryDocument: Document) {

    companion object {

        fun fromDto(dto: FindOperationDto): FindOperation {
            val documentAsJsonString = dto.queryDocumentDto!!.documentAsJsonString
            val bsonDocument = BsonDocument.parse(documentAsJsonString)
            val queryDocument = DocumentCodec().decode(bsonDocument.asBsonReader(), DecoderContext.builder().build());

            return FindOperation(databaseName = dto.databaseName,
                    collectionName = dto.collectionName,
                    queryDocument = queryDocument)
        }
    }

}