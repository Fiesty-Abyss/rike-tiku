package com.neu.riketiku.jiaoxue;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neu.riketiku.jiaoxue.dto.BanJiChuangJianQingQiu;
import com.neu.riketiku.jiaoxue.dto.BanJiXiangYing;
import com.neu.riketiku.jiaoxue.dto.BanJiXiuGaiQingQiu;
import com.neu.riketiku.jiaoxue.dto.BanJiZhuangTaiQingQiu;
import com.neu.riketiku.jiaoxue.entity.BanJi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/classes")
public class BanJiController {
    private final BanJiFuWu banJiFuWu;

    public BanJiController(BanJiFuWu banJiFuWu) {
        this.banJiFuWu = banJiFuWu;
    }

    @GetMapping
    public IPage<BanJiXiangYing> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String status) {
        Page<BanJi> result = banJiFuWu.page(page, size, code, name, grade, status);
        return result.convert(BanJiXiangYing::from);
    }

    @GetMapping("/{id}")
    public BanJiXiangYing get(@PathVariable Long id) {
        return banJiFuWu.get(id);
    }

    @PostMapping
    public BanJiXiangYing create(@Valid @RequestBody BanJiChuangJianQingQiu request) {
        return banJiFuWu.create(request);
    }

    @PutMapping("/{id}")
    public BanJiXiangYing update(@PathVariable Long id, @Valid @RequestBody BanJiXiuGaiQingQiu request) {
        return banJiFuWu.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public BanJiXiangYing changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody BanJiZhuangTaiQingQiu request) {
        return banJiFuWu.changeStatus(id, request);
    }
}
