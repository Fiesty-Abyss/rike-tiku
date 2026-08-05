package com.neu.riketiku.xueshengdaoru.response;

import java.util.List;

public record StudentImportRowResponse(
        int rowNumber,
        String studentNumber,
        String name,
        String classCode,
        String grade,
        String username,
        String accountStatus,
        boolean passwordProvided,
        boolean passwordWillGenerate,
        String status,
        List<StudentImportError> errors) {
}
