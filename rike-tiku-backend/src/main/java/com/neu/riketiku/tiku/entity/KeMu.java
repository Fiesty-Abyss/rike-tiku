package com.neu.riketiku.tiku.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ke_mu")
public class KeMu {
    @TableId
    private Long id;
    private String keMuDaiMa;
    private String keMuMingCheng;
    private Integer paiXu;
    private String zhuangTai;
    private LocalDateTime chuangJianShiJian;
    private LocalDateTime gengXinShiJian;
    @TableLogic
    private Integer yiShanChu;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKeMuDaiMa() { return keMuDaiMa; }
    public void setKeMuDaiMa(String keMuDaiMa) { this.keMuDaiMa = keMuDaiMa; }
    public String getKeMuMingCheng() { return keMuMingCheng; }
    public void setKeMuMingCheng(String keMuMingCheng) { this.keMuMingCheng = keMuMingCheng; }
    public Integer getPaiXu() { return paiXu; }
    public void setPaiXu(Integer paiXu) { this.paiXu = paiXu; }
    public String getZhuangTai() { return zhuangTai; }
    public void setZhuangTai(String zhuangTai) { this.zhuangTai = zhuangTai; }
    public LocalDateTime getChuangJianShiJian() { return chuangJianShiJian; }
    public void setChuangJianShiJian(LocalDateTime value) { this.chuangJianShiJian = value; }
    public LocalDateTime getGengXinShiJian() { return gengXinShiJian; }
    public void setGengXinShiJian(LocalDateTime value) { this.gengXinShiJian = value; }
    public Integer getYiShanChu() { return yiShanChu; }
    public void setYiShanChu(Integer yiShanChu) { this.yiShanChu = yiShanChu; }
}
