package com.riketiku.jiaoxue.dto;
import com.riketiku.jiaoxue.entity.BanJi;
public record BanJiXiangYing(Long id,String classCode,String className,String grade,Integer enrollmentYear,String status) { public static BanJiXiangYing from(BanJi b){return new BanJiXiangYing(b.getId(),b.getBanJiBianMa(),b.getBanJiMingCheng(),b.getNianJi(),b.getRuXueNianFen(),b.getZhuangTai());}}
