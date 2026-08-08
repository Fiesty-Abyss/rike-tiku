package com.neu.riketiku.jiaoshi;

import com.neu.riketiku.jiaoshi.dto.GaoPinKaoDianDtos.GaoPinKaoDianChuangJianQingQiu;
import com.neu.riketiku.jiaoshi.dto.GaoPinKaoDianDtos.GaoPinKaoDianXiangYing;
import com.neu.riketiku.jiaoshi.dto.GaoPinKaoDianDtos.GaoPinKaoDianXiuGaiQingQiu;
import com.neu.riketiku.jiaoshi.dto.GaoPinKaoDianDtos.GaoPinKaoDianZhuangTaiQingQiu;
import com.neu.riketiku.jiaoshi.dto.JiaoShiGongZuoTaiXiangYing;
import com.neu.riketiku.renzheng.RenZhengYongHu;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher")
public class JiaoShiGaoPinKaoDianController {
    private final JiaoShiGaoPinKaoDianFuWu service;

    public JiaoShiGaoPinKaoDianController(JiaoShiGaoPinKaoDianFuWu service) {
        this.service = service;
    }

    @GetMapping("/scopes/{scopeId}")
    public JiaoShiGongZuoTaiXiangYing workspace(@PathVariable long scopeId,
            @AuthenticationPrincipal RenZhengYongHu principal) {
        return service.getWorkspace(principal.id(), scopeId);
    }

    @GetMapping("/scopes/{scopeId}/high-frequency-points")
    public List<GaoPinKaoDianXiangYing> points(@PathVariable long scopeId,
            @AuthenticationPrincipal RenZhengYongHu principal) {
        return service.listForTeacher(principal.id(), scopeId);
    }

    @PostMapping("/scopes/{scopeId}/high-frequency-points")
    public GaoPinKaoDianXiangYing create(@PathVariable long scopeId, @Valid @RequestBody GaoPinKaoDianChuangJianQingQiu request,
            @AuthenticationPrincipal RenZhengYongHu principal) {
        return service.create(principal.id(), scopeId, request);
    }

    @PutMapping("/high-frequency-points/{id}")
    public GaoPinKaoDianXiangYing update(@PathVariable long id, @Valid @RequestBody GaoPinKaoDianXiuGaiQingQiu request,
            @AuthenticationPrincipal RenZhengYongHu principal) {
        return service.update(principal.id(), id, request);
    }

    @PostMapping("/high-frequency-points/{id}/status")
    public GaoPinKaoDianXiangYing updateStatus(@PathVariable long id, @Valid @RequestBody GaoPinKaoDianZhuangTaiQingQiu request,
            @AuthenticationPrincipal RenZhengYongHu principal) {
        return service.updateStatus(principal.id(), id, request);
    }
}
