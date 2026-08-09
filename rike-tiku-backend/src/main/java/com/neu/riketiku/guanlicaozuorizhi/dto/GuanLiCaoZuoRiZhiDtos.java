package com.neu.riketiku.guanlicaozuorizhi.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class GuanLiCaoZuoRiZhiDtos {
    private GuanLiCaoZuoRiZhiDtos() {
    }

    public record Item(Long id, Long operatorId, String operatorUsername, String module,
                       String action, Long businessObjectId, String result, String summary,
                       String errorCode, LocalDateTime createdAt) {
    }

    public record Page(List<Item> records, long total, long page, long size, long pages) {
    }
}
