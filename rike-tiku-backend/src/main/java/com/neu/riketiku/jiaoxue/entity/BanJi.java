package com.neu.riketiku.jiaoxue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ban_ji")
public class BanJi {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String banJiBianMa;
    private String banJiMingCheng;
    private String nianJi;
    private Integer ruXueNianFen;
    private String zhuangTai;
    private LocalDateTime chuangJianShiJian;
    private LocalDateTime gengXinShiJian;
    @TableLogic
    private Integer yiShanChu;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public String getBanJiBianMa() { return banJiBianMa; }
    public void setBanJiBianMa(String value) { this.banJiBianMa = value; }
    public String getBanJiMingCheng() { return banJiMingCheng; }
    public void setBanJiMingCheng(String value) { this.banJiMingCheng = value; }
    public String getNianJi() { return nianJi; }
    public void setNianJi(String value) { this.nianJi = value; }
    public Integer getRuXueNianFen() { return ruXueNianFen; }
    public void setRuXueNianFen(Integer value) { this.ruXueNianFen = value; }
    public String getZhuangTai() { return zhuangTai; }
    public void setZhuangTai(String value) { this.zhuangTai = value; }
    public LocalDateTime getChuangJianShiJian() { return chuangJianShiJian; }
    public void setChuangJianShiJian(LocalDateTime value) { this.chuangJianShiJian = value; }
    public LocalDateTime getGengXinShiJian() { return gengXinShiJian; }
    public void setGengXinShiJian(LocalDateTime value) { this.gengXinShiJian = value; }
    public Integer getYiShanChu() { return yiShanChu; }
    public void setYiShanChu(Integer value) { this.yiShanChu = value; }
}
