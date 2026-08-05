package com.neu.riketiku.xueshengdaoru.response;
public record StudentImportAccountResponse(String studentNumber, String name, String classCode, String username,
        String initialPassword, String accountStatus, boolean mustChangePassword) {}
