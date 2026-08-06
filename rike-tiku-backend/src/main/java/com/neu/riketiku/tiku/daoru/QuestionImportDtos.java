package com.neu.riketiku.tiku.daoru;

import java.util.List;

public final class QuestionImportDtos {
    private QuestionImportDtos() {
    }

    public record Error(String field, String code, String message) {
    }

    public record Attachment(String position, String type, String objectMarker, String fileName, int characterPosition) {
    }

    public record Row(int rowNumber, String subjectCode, String questionType, String usageMode,
                      String stemSummary, List<String> knowledgePointPaths, int attachmentCount,
                      String contentHash, String status, List<Error> errors, List<String> warnings) {
    }

    public record Preview(String fileName, String fileHash, String subjectCode, int totalCount, int validCount,
                          int invalidCount, int duplicateCount, boolean alreadyImported, List<Row> rows) {
    }

    public record Confirm(String batchCode, int totalCount, int importedCount) {
    }
}
