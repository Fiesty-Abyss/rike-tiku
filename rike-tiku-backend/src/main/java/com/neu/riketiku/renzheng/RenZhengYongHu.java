package com.neu.riketiku.renzheng;

import java.util.List;

public record RenZhengYongHu(
        Long id,
        String yongHuMing,
        List<String> jiaoSe,
        boolean biXuXiuGaiMiMa) {
}
