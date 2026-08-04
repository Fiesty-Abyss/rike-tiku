package com.neu.riketiku.renzheng.dto;

import java.util.List;

public record YongHuZhaiYaoXiangYing(
        Long id,
        String username,
        List<String> roles) {
}
