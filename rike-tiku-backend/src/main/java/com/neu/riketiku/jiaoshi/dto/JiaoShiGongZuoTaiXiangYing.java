package com.neu.riketiku.jiaoshi.dto;

import com.neu.riketiku.jiaoshi.dto.GaoPinKaoDianDtos.GaoPinKaoDianXiangYing;
import java.util.List;

public record JiaoShiGongZuoTaiXiangYing(
        Long teachingAssignmentId,
        Long classId,
        String className,
        String grade,
        Long subjectId,
        String subjectName,
        String teacherName,
        int studentCount,
        List<XueShengJiBenXiangYing> students,
        List<GaoPinKaoDianXiangYing> highFrequencyPoints,
        List<KnowledgePointOption> knowledgePoints) {

    public record XueShengJiBenXiangYing(
            Long studentId,
            String studentNumber,
            String name,
            String grade) {
    }

    public record KnowledgePointOption(Long id, String name, String path) {
    }
}
