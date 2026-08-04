package com.neu.riketiku.tiku.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ti_mu")
public class TiMu {
    @TableId
    private Long id;
    private Long keMuId;
    private Long fuTiMuId;
    private Long daoRuPiCiId;
    private String tiMuLeiXing;
    private String shiYongMoShi;
    private String tiGan;
    private String zhengQueDaAn;
    private Integer nanDu;
    private String nanDuShuoMing;
    private Boolean shiFouKeZiDongPanFen;
    private String zhuangTai;
    private String neiRongHaXi;
    private LocalDateTime chuangJianShiJian;
    private LocalDateTime gengXinShiJian;
    @TableLogic
    private Integer yiShanChu;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getKeMuId() { return keMuId; }
    public void setKeMuId(Long keMuId) { this.keMuId = keMuId; }
    public Long getFuTiMuId() { return fuTiMuId; }
    public void setFuTiMuId(Long fuTiMuId) { this.fuTiMuId = fuTiMuId; }
    public Long getDaoRuPiCiId() { return daoRuPiCiId; }
    public void setDaoRuPiCiId(Long value) { this.daoRuPiCiId = value; }
    public String getTiMuLeiXing() { return tiMuLeiXing; }
    public void setTiMuLeiXing(String value) { this.tiMuLeiXing = value; }
    public String getShiYongMoShi() { return shiYongMoShi; }
    public void setShiYongMoShi(String value) { this.shiYongMoShi = value; }
    public String getTiGan() { return tiGan; }
    public void setTiGan(String value) { this.tiGan = value; }
    public String getZhengQueDaAn() { return zhengQueDaAn; }
    public void setZhengQueDaAn(String value) { this.zhengQueDaAn = value; }
    public Integer getNanDu() { return nanDu; }
    public void setNanDu(Integer value) { this.nanDu = value; }
    public String getNanDuShuoMing() { return nanDuShuoMing; }
    public void setNanDuShuoMing(String value) { this.nanDuShuoMing = value; }
    public Boolean getShiFouKeZiDongPanFen() { return shiFouKeZiDongPanFen; }
    public void setShiFouKeZiDongPanFen(Boolean value) { this.shiFouKeZiDongPanFen = value; }
    public String getZhuangTai() { return zhuangTai; }
    public void setZhuangTai(String value) { this.zhuangTai = value; }
    public String getNeiRongHaXi() { return neiRongHaXi; }
    public void setNeiRongHaXi(String value) { this.neiRongHaXi = value; }
    public LocalDateTime getChuangJianShiJian() { return chuangJianShiJian; }
    public void setChuangJianShiJian(LocalDateTime value) { this.chuangJianShiJian = value; }
    public LocalDateTime getGengXinShiJian() { return gengXinShiJian; }
    public void setGengXinShiJian(LocalDateTime value) { this.gengXinShiJian = value; }
    public Integer getYiShanChu() { return yiShanChu; }
    public void setYiShanChu(Integer value) { this.yiShanChu = value; }
}
