package com.neu.riketiku.jiaoxue.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BanJiXiuGaiQingQiu(
        @NotBlank @Size(max = 128) String className,
        @NotBlank @Size(max = 32) String grade,
        @NotNull @Min(2000) @Max(2100) Integer enrollmentYear) {
}
