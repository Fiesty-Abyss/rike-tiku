package com.neu.riketiku.jiaoshi.dto;

import java.time.LocalDateTime;

public record RenKeXiangYing(
        Long id, Long classId, String classCode, String className,
        Long subjectId, String subjectCode, String subjectName, boolean primary,
        String status, LocalDateTime startTime, LocalDateTime endTime) {
}
