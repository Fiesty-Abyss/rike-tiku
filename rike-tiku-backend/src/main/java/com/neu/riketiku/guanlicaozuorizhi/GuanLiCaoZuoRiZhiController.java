package com.neu.riketiku.guanlicaozuorizhi;

import com.neu.riketiku.guanlicaozuorizhi.dto.GuanLiCaoZuoRiZhiDtos;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/operation-logs")
public class GuanLiCaoZuoRiZhiController {
    private final GuanLiCaoZuoRiZhiFuWu service;

    public GuanLiCaoZuoRiZhiController(GuanLiCaoZuoRiZhiFuWu service) {
        this.service = service;
    }

    @GetMapping
    public GuanLiCaoZuoRiZhiDtos.Page page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result) {
        return service.page(page, size, module, action, result);
    }
}
