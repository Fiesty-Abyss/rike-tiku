package com.neu.riketiku.jiaoshi;

import com.neu.riketiku.jiaoshi.dto.JiaoShiRenKeFanWeiXiangYing;
import com.neu.riketiku.renzheng.RenZhengYongHu;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher")
public class JiaoShiGongZuoTaiController {
    private final JiaoShiGongZuoTaiFuWu service;

    public JiaoShiGongZuoTaiController(JiaoShiGongZuoTaiFuWu service) {
        this.service = service;
    }

    @GetMapping("/teaching-scopes")
    List<JiaoShiRenKeFanWeiXiangYing> teachingScopes(@AuthenticationPrincipal RenZhengYongHu principal) {
        return service.myTeachingScopes(principal.id());
    }
}
