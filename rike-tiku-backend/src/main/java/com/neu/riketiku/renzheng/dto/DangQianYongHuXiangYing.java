package com.neu.riketiku.renzheng.dto;

import java.util.List;

public record DangQianYongHuXiangYing(
        Long id,
        String username,
        List<String> roles,
        boolean mustChangePassword,
        String displayName,
        String studentNumber,
        String teacherNumber) {
}
