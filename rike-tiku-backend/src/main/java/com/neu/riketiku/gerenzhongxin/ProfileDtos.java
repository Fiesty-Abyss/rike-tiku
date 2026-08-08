package com.neu.riketiku.gerenzhongxin;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class ProfileDtos {
    private ProfileDtos() {
    }

    public record ProfileUpdateRequest(
            @Size(max = 500, message = "个人简介不能超过500字") String introduction) {
    }

    public record ProfileResponse(
            String displayName,
            AccountResponse account,
            StudentProfileResponse studentProfile,
            TeacherProfileResponse teacherProfile,
            PersonalResponse personal) {
    }

    public record AccountResponse(
            String username,
            String accountStatus,
            List<String> roles,
            boolean firstLogin,
            LocalDateTime passwordChangedAt,
            LocalDateTime lastLoginAt) {
    }

    public record StudentProfileResponse(
            String studentNumber,
            String name,
            String grade,
            String currentClass) {
    }

    public record TeacherProfileResponse(
            String teacherNumber,
            String name,
            String title,
            List<TeachingScopeResponse> teachingScopes) {
    }

    public record TeachingScopeResponse(
            long teachingAssignmentId,
            String className,
            String grade,
            String subjectName) {
    }

    public record PersonalResponse(
            String introduction,
            String avatarDataUrl,
            String avatarMime,
            LocalDateTime avatarUpdatedAt) {
    }

    public record AvatarResponse(
            String avatarDataUrl,
            String avatarMime,
            LocalDateTime avatarUpdatedAt) {
    }
}
