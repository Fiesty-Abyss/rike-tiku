package com.neu.riketiku.xueshengguanli.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class StudentManagementDtos {
    private StudentManagementDtos() {
    }

    public record StudentCreateRequest(
            @NotBlank @Size(max = 64) String studentNumber,
            @NotBlank @Size(max = 64) String name,
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 32) String grade,
            @NotNull Long classId) {
    }

    public record StudentUpdateRequest(
            @NotBlank @Size(max = 64) String name,
            @NotBlank @Size(max = 32) String grade,
            @NotBlank @Pattern(regexp = "ENABLED|DISABLED|LOCKED") String accountStatus,
            @NotBlank @Pattern(regexp = "ACTIVE|DISABLED") String profileStatus) {
    }

    public record StudentTransferRequest(@NotNull Long classId) {
    }

    public record ClassSummaryResponse(Long id, String classCode, String className, String grade) {
    }

    public record StudentSummaryResponse(
            Long id,
            String studentNumber,
            String name,
            String username,
            String grade,
            ClassSummaryResponse currentClass,
            String accountStatus,
            String profileStatus) {
    }

    public record StudentListResponse(
            List<StudentSummaryResponse> records,
            long total,
            long current,
            long size,
            long pages) {
    }

    public record ClassHistoryResponse(
            Long classId,
            String classCode,
            String className,
            LocalDateTime joinedAt,
            LocalDateTime exitedAt,
            boolean current) {
    }

    public record StudentDetailResponse(
            StudentSummaryResponse student,
            List<String> roles,
            List<ClassHistoryResponse> classHistory) {
    }

    public record StudentCreateResponse(StudentDetailResponse student, String initialPassword) {
    }

    public record PasswordResetResponse(String initialPassword) {
    }
}
