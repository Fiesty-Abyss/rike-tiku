package com.neu.riketiku.jiaoshi.dto;

public record JiaoShiRenKeFanWeiXiangYing(
        Long classId,
        String className,
        String grade,
        Long subjectId,
        String subjectCode,
        String subjectName,
        boolean homeroomSubject,
        String teachingStatus) {
}
