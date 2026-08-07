package com.neu.riketiku.renzheng;

import com.neu.riketiku.renzheng.dto.ChuShiMiMaXiuGaiQingQiu;
import com.neu.riketiku.renzheng.dto.DangQianYongHuXiangYing;
import com.neu.riketiku.renzheng.dto.DengLuQingQiu;
import com.neu.riketiku.renzheng.dto.DengLuXiangYing;
import com.neu.riketiku.renzheng.dto.HuaKuaiTiaoZhanXiangYing;
import com.neu.riketiku.renzheng.dto.ZhuDongMiMaXiuGaiQingQiu;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class RenZhengController {
    private final RenZhengFuWu service;

    public RenZhengController(RenZhengFuWu service) {
        this.service = service;
    }

    @PostMapping("/login")
    DengLuXiangYing login(@Valid @RequestBody DengLuQingQiu request) {
        return service.dengLu(request);
    }

    @GetMapping("/slider-challenge")
    HuaKuaiTiaoZhanXiangYing sliderChallenge() {
        return service.huaKuaiTiaoZhan();
    }

    @GetMapping("/me")
    DangQianYongHuXiangYing me(@AuthenticationPrincipal RenZhengYongHu principal) {
        return service.dangQianYongHu(principal);
    }

    @PostMapping("/change-initial-password")
    DengLuXiangYing changeInitialPassword(
            @AuthenticationPrincipal RenZhengYongHu principal,
            @Valid @RequestBody ChuShiMiMaXiuGaiQingQiu request) {
        return service.xiuGaiChuShiMiMa(principal, request);
    }

    @PostMapping("/change-password")
    DengLuXiangYing changePassword(
            @AuthenticationPrincipal RenZhengYongHu principal,
            @Valid @RequestBody ZhuDongMiMaXiuGaiQingQiu request) {
        return service.zhuDongXiuGaiMiMa(principal, request);
    }
}
