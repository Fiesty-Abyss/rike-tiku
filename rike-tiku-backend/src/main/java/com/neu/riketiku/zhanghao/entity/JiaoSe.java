package com.neu.riketiku.zhanghao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("jiao_se")
public class JiaoSe {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String jiaoSeDaiMa;
    private String jiaoSeMingCheng;
    private String zhuangTai;
    private LocalDateTime chuangJianShiJian;
    private LocalDateTime gengXinShiJian;
    @TableLogic
    private Integer yiShanChu;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public String getJiaoSeDaiMa() { return jiaoSeDaiMa; }
    public void setJiaoSeDaiMa(String value) { this.jiaoSeDaiMa = value; }
    public String getJiaoSeMingCheng() { return jiaoSeMingCheng; }
    public void setJiaoSeMingCheng(String value) { this.jiaoSeMingCheng = value; }
    public String getZhuangTai() { return zhuangTai; }
    public void setZhuangTai(String value) { this.zhuangTai = value; }
    public LocalDateTime getChuangJianShiJian() { return chuangJianShiJian; }
    public void setChuangJianShiJian(LocalDateTime value) { this.chuangJianShiJian = value; }
    public LocalDateTime getGengXinShiJian() { return gengXinShiJian; }
    public void setGengXinShiJian(LocalDateTime value) { this.gengXinShiJian = value; }
    public Integer getYiShanChu() { return yiShanChu; }
    public void setYiShanChu(Integer value) { this.yiShanChu = value; }
}
