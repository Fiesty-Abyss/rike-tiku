package com.neu.riketiku.jiaoshi.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RenKeChuangJianQingQiu(
        @NotNull Long classId,
        @NotNull Long subjectId,
        boolean primary,
        @NotNull LocalDateTime startTime) {
}
