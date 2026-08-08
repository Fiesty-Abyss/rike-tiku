package com.neu.riketiku.jiaoshi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class GaoPinKaoDianDtos {
    private GaoPinKaoDianDtos() {
    }

    public record GaoPinKaoDianChuangJianQingQiu(
            @NotNull Long knowledgePointId,
            @NotBlank @Size(max = 200) String title,
            @NotBlank String content,
            @Size(max = 500) String memoryTrick,
            String commonMistake,
            @Min(0) @Max(100000) int sortOrder) {
    }

    public record GaoPinKaoDianXiuGaiQingQiu(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String content,
            @Size(max = 500) String memoryTrick,
            String commonMistake,
            @Min(0) @Max(100000) int sortOrder) {
    }

    public record GaoPinKaoDianZhuangTaiQingQiu(
            @NotBlank String status) {
    }

    public record GaoPinKaoDianXiangYing(
            Long id,
            Long teachingAssignmentId,
            Long knowledgePointId,
            String knowledgePointName,
            String title,
            String content,
            String memoryTrick,
            String commonMistake,
            int sortOrder,
            String status,
            String teacherName) {
    }

    public record XueShengGaoPinKaoDianXiangYing(
            Long id,
            Long knowledgePointId,
            String knowledgePointName,
            String title,
            String content,
            String memoryTrick,
            String commonMistake,
            int sortOrder,
            String teacherName) {
    }
}
