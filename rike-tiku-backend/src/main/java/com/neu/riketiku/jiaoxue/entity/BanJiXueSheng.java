package com.neu.riketiku.jiaoxue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ban_ji_xue_sheng")
public class BanJiXueSheng {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long banJiId;
    private Long xueShengId;
    private Boolean shiFouZhuBanJi;
    private LocalDateTime jiaRuShiJian;
    private LocalDateTime tuiChuShiJian;
    private String zhuangTai;
    private LocalDateTime chuangJianShiJian;
    private LocalDateTime gengXinShiJian;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public Long getBanJiId() { return banJiId; }
    public void setBanJiId(Long value) { this.banJiId = value; }
    public Long getXueShengId() { return xueShengId; }
    public void setXueShengId(Long value) { this.xueShengId = value; }
    public Boolean getShiFouZhuBanJi() { return shiFouZhuBanJi; }
    public void setShiFouZhuBanJi(Boolean value) { this.shiFouZhuBanJi = value; }
    public LocalDateTime getJiaRuShiJian() { return jiaRuShiJian; }
    public void setJiaRuShiJian(LocalDateTime value) { this.jiaRuShiJian = value; }
    public LocalDateTime getTuiChuShiJian() { return tuiChuShiJian; }
    public void setTuiChuShiJian(LocalDateTime value) { this.tuiChuShiJian = value; }
    public String getZhuangTai() { return zhuangTai; }
    public void setZhuangTai(String value) { this.zhuangTai = value; }
    public LocalDateTime getChuangJianShiJian() { return chuangJianShiJian; }
    public void setChuangJianShiJian(LocalDateTime value) { this.chuangJianShiJian = value; }
    public LocalDateTime getGengXinShiJian() { return gengXinShiJian; }
    public void setGengXinShiJian(LocalDateTime value) { this.gengXinShiJian = value; }
}
