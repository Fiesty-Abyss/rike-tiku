package com.neu.riketiku.jiaoshi.dto;

import java.util.List;

public record JiaoShiXiangQingXiangYing(JiaoShiXiangYing teacher, List<String> roles, List<RenKeXiangYing> teachingAssignments) {
}
