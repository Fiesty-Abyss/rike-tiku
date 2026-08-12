package com.neu.riketiku.zhanghao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminDefaultPasswordPolicy {
    private final String defaultPassword;

    public AdminDefaultPasswordPolicy(
            @Value("${app.account.default-reset-password:a1234567}") String defaultPassword) {
        if (defaultPassword == null
                || defaultPassword.length() < 8
                || defaultPassword.length() > 64
                || !defaultPassword.matches(".*[A-Za-z].*")
                || !defaultPassword.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("管理员默认恢复密码必须为8至64位并同时包含字母和数字");
        }
        this.defaultPassword = defaultPassword;
    }

    public String password() {
        return defaultPassword;
    }
}
