package com.riketiku.jiaoshi;

import com.riketiku.jiaoshi.dto.GaoPinKaoDianDtos.XueShengGaoPinKaoDianXiangYing;
import com.riketiku.renzheng.RenZhengYongHu;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student/high-frequency-points")
public class XueShengGaoPinKaoDianController {
    private final JiaoShiGaoPinKaoDianFuWu service;

    public XueShengGaoPinKaoDianController(JiaoShiGaoPinKaoDianFuWu service) {
        this.service = service;
    }

    @GetMapping
    public List<XueShengGaoPinKaoDianXiangYing> points(@RequestParam long subjectId,
            @AuthenticationPrincipal RenZhengYongHu principal) {
        return service.listForStudent(principal.id(), subjectId);
    }
}
