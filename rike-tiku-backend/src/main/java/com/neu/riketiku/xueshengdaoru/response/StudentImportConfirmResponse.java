package com.neu.riketiku.xueshengdaoru.response;
import java.util.List;
public record StudentImportConfirmResponse(int totalCount, int importedCount, List<StudentImportAccountResponse> accounts) {}
