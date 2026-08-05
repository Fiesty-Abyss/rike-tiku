package com.neu.riketiku.jiaoshi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RenKeZhuangTaiQingQiu(
        @NotBlank @Pattern(regexp = "ACTIVE|ENDED|DISABLED") String status) {
}
