package com.neu.riketiku.renzheng.dto;

import com.neu.riketiku.renzheng.JiaoSeDaiMa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DengLuQingQiu(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        @NotNull(message = "登录入口角色不能为空") JiaoSeDaiMa expectedRole) {
}
