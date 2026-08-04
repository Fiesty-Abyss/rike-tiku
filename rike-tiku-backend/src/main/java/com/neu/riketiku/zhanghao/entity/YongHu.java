package com.neu.riketiku.zhanghao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("yong_hu")
public class YongHu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String yongHuMing;
    private String miMaZhaiYao;
    private String zhangHaoZhuangTai;
    private Boolean shiFouShouCiDengLu;
    private LocalDateTime miMaXiuGaiShiJian;
    private LocalDateTime zuiHouDengLuShiJian;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime chuangJianShiJian;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gengXinShiJian;
    @TableLogic
    private Integer yiShanChu;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public String getYongHuMing() { return yongHuMing; }
    public void setYongHuMing(String value) { this.yongHuMing = value; }
    public String getMiMaZhaiYao() { return miMaZhaiYao; }
    public void setMiMaZhaiYao(String value) { this.miMaZhaiYao = value; }
    public String getZhangHaoZhuangTai() { return zhangHaoZhuangTai; }
    public void setZhangHaoZhuangTai(String value) { this.zhangHaoZhuangTai = value; }
    public Boolean getShiFouShouCiDengLu() { return shiFouShouCiDengLu; }
    public void setShiFouShouCiDengLu(Boolean value) { this.shiFouShouCiDengLu = value; }
    public LocalDateTime getMiMaXiuGaiShiJian() { return miMaXiuGaiShiJian; }
    public void setMiMaXiuGaiShiJian(LocalDateTime value) { this.miMaXiuGaiShiJian = value; }
    public LocalDateTime getZuiHouDengLuShiJian() { return zuiHouDengLuShiJian; }
    public void setZuiHouDengLuShiJian(LocalDateTime value) { this.zuiHouDengLuShiJian = value; }
    public LocalDateTime getChuangJianShiJian() { return chuangJianShiJian; }
    public void setChuangJianShiJian(LocalDateTime value) { this.chuangJianShiJian = value; }
    public LocalDateTime getGengXinShiJian() { return gengXinShiJian; }
    public void setGengXinShiJian(LocalDateTime value) { this.gengXinShiJian = value; }
    public Integer getYiShanChu() { return yiShanChu; }
    public void setYiShanChu(Integer value) { this.yiShanChu = value; }
}
