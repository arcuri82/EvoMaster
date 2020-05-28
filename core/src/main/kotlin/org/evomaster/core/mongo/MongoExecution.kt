package org.evomaster.core.mongo

import org.evomaster.client.java.controller.api.dto.mongo.MongoExecutionDto


class MongoExecution(val executedFindOperations: List<ExecutedFindOperation> = mutableListOf()) {

    companion object {

        fun fromDto(dto: MongoExecutionDto?): MongoExecution {
            val mongoExecution = MongoExecution(
                    executedFindOperations = dto!!.executedFindOperationDtos.map { ExecutedFindOperation.fromDto(it) }.toList())
            return mongoExecution
        }

    }

}

