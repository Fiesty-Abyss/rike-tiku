package com.neu.riketiku.xueshengdaoru.response;

import java.util.List;

public record StudentImportPreviewResponse(
        String fileName,
        int totalCount,
        int validCount,
        int invalidCount,
        List<StudentImportRowResponse> rows) {
}
