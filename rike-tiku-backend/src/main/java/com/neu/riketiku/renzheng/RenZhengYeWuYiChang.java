package com.neu.riketiku.renzheng;

import org.springframework.http.HttpStatus;

public class RenZhengYeWuYiChang extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public RenZhengYeWuYiChang(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
