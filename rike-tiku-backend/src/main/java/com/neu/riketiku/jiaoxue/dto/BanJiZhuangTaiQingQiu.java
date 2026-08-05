package com.neu.riketiku.jiaoxue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BanJiZhuangTaiQingQiu(@NotBlank @Size(max = 16) String status) {
}
