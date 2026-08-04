package com.neu.riketiku.tiku.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ti_mu_fu_jian")
public class TiMuFuJian {
    @TableId
    private Long id;
    private Long tiMuId;
    private Long tiMuXuanXiangId;
    private Long tiMuJieXiId;
    private String guanLianWeiZhi;
    private String fuJianLeiXing;
    private String yuanShiWenJianMing;
    private String xiangDuiLuJing;
    private String neiRongHaXi;
    private String duiXiangBiaoShi;
    private Integer zhengWenZiFuWeiZhi;
    private String yuanShiYeMa;
    private String fuJianShuoMing;
    private Integer paiXu;
    private String zhuangTai;
    private LocalDateTime chuangJianShiJian;
    private LocalDateTime gengXinShiJian;
    @TableLogic
    private Integer yiShanChu;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public Long getTiMuId() { return tiMuId; }
    public void setTiMuId(Long value) { this.tiMuId = value; }
    public Long getTiMuXuanXiangId() { return tiMuXuanXiangId; }
    public void setTiMuXuanXiangId(Long value) { this.tiMuXuanXiangId = value; }
    public Long getTiMuJieXiId() { return tiMuJieXiId; }
    public void setTiMuJieXiId(Long value) { this.tiMuJieXiId = value; }
    public String getGuanLianWeiZhi() { return guanLianWeiZhi; }
    public void setGuanLianWeiZhi(String value) { this.guanLianWeiZhi = value; }
    public String getFuJianLeiXing() { return fuJianLeiXing; }
    public void setFuJianLeiXing(String value) { this.fuJianLeiXing = value; }
    public String getYuanShiWenJianMing() { return yuanShiWenJianMing; }
    public void setYuanShiWenJianMing(String value) { this.yuanShiWenJianMing = value; }
    public String getXiangDuiLuJing() { return xiangDuiLuJing; }
    public void setXiangDuiLuJing(String value) { this.xiangDuiLuJing = value; }
    public String getNeiRongHaXi() { return neiRongHaXi; }
    public void setNeiRongHaXi(String value) { this.neiRongHaXi = value; }
    public String getDuiXiangBiaoShi() { return duiXiangBiaoShi; }
    public void setDuiXiangBiaoShi(String value) { this.duiXiangBiaoShi = value; }
    public Integer getZhengWenZiFuWeiZhi() { return zhengWenZiFuWeiZhi; }
    public void setZhengWenZiFuWeiZhi(Integer value) { this.zhengWenZiFuWeiZhi = value; }
    public String getYuanShiYeMa() { return yuanShiYeMa; }
    public void setYuanShiYeMa(String value) { this.yuanShiYeMa = value; }
    public String getFuJianShuoMing() { return fuJianShuoMing; }
    public void setFuJianShuoMing(String value) { this.fuJianShuoMing = value; }
    public Integer getPaiXu() { return paiXu; }
    public void setPaiXu(Integer value) { this.paiXu = value; }
    public String getZhuangTai() { return zhuangTai; }
    public void setZhuangTai(String value) { this.zhuangTai = value; }
    public LocalDateTime getChuangJianShiJian() { return chuangJianShiJian; }
    public void setChuangJianShiJian(LocalDateTime value) { this.chuangJianShiJian = value; }
    public LocalDateTime getGengXinShiJian() { return gengXinShiJian; }
    public void setGengXinShiJian(LocalDateTime value) { this.gengXinShiJian = value; }
    public Integer getYiShanChu() { return yiShanChu; }
    public void setYiShanChu(Integer value) { this.yiShanChu = value; }
}
