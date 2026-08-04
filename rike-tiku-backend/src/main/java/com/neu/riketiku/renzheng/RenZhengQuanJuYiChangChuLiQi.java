package com.neu.riketiku.renzheng;

import com.neu.riketiku.renzheng.dto.CuoWuXiangYing;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RenZhengQuanJuYiChangChuLiQi {

    @ExceptionHandler(RenZhengYeWuYiChang.class)
    ResponseEntity<CuoWuXiangYing> handleBusinessException(RenZhengYeWuYiChang exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(error(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<CuoWuXiangYing> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().isEmpty()
                ? "请求参数不正确"
                : exception.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        return ResponseEntity.badRequest().body(error("INVALID_REQUEST", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<CuoWuXiangYing> handleUnreadableRequest() {
        return ResponseEntity.badRequest().body(error("INVALID_REQUEST", "请求格式或枚举值不正确"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<CuoWuXiangYing> handleUnexpectedException() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("INTERNAL_ERROR", "服务器处理请求失败"));
    }

    private CuoWuXiangYing error(String code, String message) {
        return new CuoWuXiangYing(code, message, Instant.now());
    }
}
