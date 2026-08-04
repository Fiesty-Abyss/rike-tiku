package com.neu.riketiku.jiaoxue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ren_ke_guan_xi")
public class RenKeGuanXi {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jiaoShiId;
    private Long banJiId;
    private Long keMuId;
    private Boolean shiFouZhuRenKe;
    private String zhuangTai;
    private LocalDateTime kaiShiShiJian;
    private LocalDateTime jieShuShiJian;
    private LocalDateTime chuangJianShiJian;
    private LocalDateTime gengXinShiJian;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public Long getJiaoShiId() { return jiaoShiId; }
    public void setJiaoShiId(Long value) { this.jiaoShiId = value; }
    public Long getBanJiId() { return banJiId; }
    public void setBanJiId(Long value) { this.banJiId = value; }
    public Long getKeMuId() { return keMuId; }
    public void setKeMuId(Long value) { this.keMuId = value; }
    public Boolean getShiFouZhuRenKe() { return shiFouZhuRenKe; }
    public void setShiFouZhuRenKe(Boolean value) { this.shiFouZhuRenKe = value; }
    public String getZhuangTai() { return zhuangTai; }
    public void setZhuangTai(String value) { this.zhuangTai = value; }
    public LocalDateTime getKaiShiShiJian() { return kaiShiShiJian; }
    public void setKaiShiShiJian(LocalDateTime value) { this.kaiShiShiJian = value; }
    public LocalDateTime getJieShuShiJian() { return jieShuShiJian; }
    public void setJieShuShiJian(LocalDateTime value) { this.jieShuShiJian = value; }
    public LocalDateTime getChuangJianShiJian() { return chuangJianShiJian; }
    public void setChuangJianShiJian(LocalDateTime value) { this.chuangJianShiJian = value; }
    public LocalDateTime getGengXinShiJian() { return gengXinShiJian; }
    public void setGengXinShiJian(LocalDateTime value) { this.gengXinShiJian = value; }
}
