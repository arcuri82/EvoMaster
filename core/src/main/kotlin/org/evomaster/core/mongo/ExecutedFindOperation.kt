package org.evomaster.core.mongo

import org.evomaster.client.java.controller.api.dto.mongo.ExecutedFindOperationDto

class ExecutedFindOperation(val findOperation: FindOperation, val isResultNotEmpty: Boolean) {

    companion object {

        fun fromDto(dto: ExecutedFindOperationDto): ExecutedFindOperation {
            return ExecutedFindOperation(findOperation = FindOperation.fromDto(dto.findOperationDto),
                    isResultNotEmpty = dto.findResultDto.hasReturnedAnyDocument)
        }
    }

}