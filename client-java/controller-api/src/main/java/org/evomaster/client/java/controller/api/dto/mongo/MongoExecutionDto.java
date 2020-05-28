package org.evomaster.client.java.controller.api.dto.mongo;

import java.util.ArrayList;
import java.util.List;

/**
 * Each time a monitored MongoDB operation (e.g. find) is executed, we keep track of the
 * arguments (e.g. query) and any other meaningful information.
 *
 *  Note: we keep track of what the SUT tried to execute on the database, but
 *  not the result.
 *
 *
 */
public class MongoExecutionDto {

    public List<ExecutedFindOperationDto> executedFindOperationDtos = new ArrayList<>();
}
