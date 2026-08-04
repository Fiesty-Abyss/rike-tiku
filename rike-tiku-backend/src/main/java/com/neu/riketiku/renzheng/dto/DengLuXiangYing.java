package com.neu.riketiku.renzheng.dto;

public record DengLuXiangYing(
        String accessToken,
        String tokenType,
        long expiresIn,
        boolean mustChangePassword,
        YongHuZhaiYaoXiangYing user) {
}
