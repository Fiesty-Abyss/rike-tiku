package com.neu.riketiku.renzheng;

import java.time.LocalDateTime;

public record YongHuRenZhengShuJu(
        Long id,
        String yongHuMing,
        String miMaZhaiYao,
        String zhangHaoZhuangTai,
        boolean shiFouShouCiDengLu,
        LocalDateTime zuiHouDengLuShiJian) {
}
