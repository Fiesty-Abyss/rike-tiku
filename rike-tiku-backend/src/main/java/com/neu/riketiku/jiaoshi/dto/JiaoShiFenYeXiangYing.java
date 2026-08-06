package com.neu.riketiku.jiaoshi.dto;

import java.util.List;

public record JiaoShiFenYeXiangYing(List<JiaoShiXiangYing> records, long total, long current, long size, long pages) {
}
