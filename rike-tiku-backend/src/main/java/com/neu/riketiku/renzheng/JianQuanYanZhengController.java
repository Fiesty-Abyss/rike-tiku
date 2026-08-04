package com.neu.riketiku.renzheng;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class JianQuanYanZhengController {

    @GetMapping("/student")
    Map<String, String> student() {
        return Map.of("role", "STUDENT", "status", "OK");
    }

    @GetMapping("/teacher")
    Map<String, String> teacher() {
        return Map.of("role", "TEACHER", "status", "OK");
    }

    @GetMapping("/admin")
    Map<String, String> admin() {
        return Map.of("role", "ADMIN", "status", "OK");
    }
}
