package org.evomaster.client.java.controller.api.dto.mongo;

import java.util.List;

public class FindResultDto {

    public static final String HAS_RETURNED_ANY_DOCUMENT_FIELD_NAME = "hasReturnedAnyDocument";

    public static final String DOCUMENTS_FIELD_NAME = "documents";

    public enum FindResultType {SUMMARY, DETAILED}

    public FindResultType findResultType;

    public boolean hasReturnedAnyDocument;

    public List<DocumentDto> documents;

}
