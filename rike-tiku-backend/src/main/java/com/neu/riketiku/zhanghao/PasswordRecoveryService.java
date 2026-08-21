package com.neu.riketiku.zhanghao;

import com.neu.riketiku.guanlicaozuorizhi.GuanLiCaoZuoRiZhiFuWu;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.renzheng.TuXingYanZhengMaFuWu;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordRecoveryService {
    private static final String ACCEPTED="如账号有效，密码恢复请求已提交给管理员。";
    private final JdbcTemplate jdbc; private final TuXingYanZhengMaFuWu captcha; private final PasswordEncoder encoder;
    private final AdminDefaultPasswordPolicy policy; private final GuanLiCaoZuoRiZhiFuWu audit;
    public PasswordRecoveryService(JdbcTemplate jdbc,TuXingYanZhengMaFuWu captcha,PasswordEncoder encoder,
            AdminDefaultPasswordPolicy policy,GuanLiCaoZuoRiZhiFuWu audit){this.jdbc=jdbc;this.captcha=captcha;this.encoder=encoder;this.policy=policy;this.audit=audit;}

    @Transactional
    public PasswordRecoveryDtos.Accepted request(PasswordRecoveryDtos.Request request){
        captcha.verify(request.challengeId(),request.captchaCode());
        Long id=jdbc.query("SELECT id FROM yong_hu WHERE yong_hu_ming=? AND zhang_hao_zhuang_tai='ENABLED' AND yi_shan_chu=0",
                (rs,row)->rs.getLong(1),request.username().trim()).stream().findFirst().orElse(null);
        if(id!=null) try{jdbc.update("INSERT INTO mi_ma_chong_zhi_shen_qing(yong_hu_id) VALUES (?)",id);}catch(DuplicateKeyException ignored){ }
        return new PasswordRecoveryDtos.Accepted(ACCEPTED);
    }

    @Transactional(readOnly=true)
    public PasswordRecoveryDtos.Page list(){
        var items=jdbc.query("""
          SELECT r.id,u.id,u.yong_hu_ming,COALESCE(xs.xing_ming,j.xing_ming,''),
            COALESCE((SELECT GROUP_CONCAT(js.jiao_se_dai_ma ORDER BY js.jiao_se_dai_ma) FROM yong_hu_jiao_se yj JOIN jiao_se js ON js.id=yj.jiao_se_id WHERE yj.yong_hu_id=u.id),'UNKNOWN'),
            r.zhuang_tai,r.shen_qing_shi_jian,r.chu_li_shi_jian,r.chu_li_jie_guo
          FROM mi_ma_chong_zhi_shen_qing r JOIN yong_hu u ON u.id=r.yong_hu_id
          LEFT JOIN xue_sheng_dang_an xs ON xs.yong_hu_id=u.id AND xs.yi_shan_chu=0
          LEFT JOIN jiao_shi_dang_an j ON j.yong_hu_id=u.id AND j.yi_shan_chu=0
          ORDER BY (r.zhuang_tai='PENDING') DESC,r.shen_qing_shi_jian DESC
          """,(rs,row)->new PasswordRecoveryDtos.Item(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getString(6),rs.getObject(7,LocalDateTime.class),rs.getObject(8,LocalDateTime.class),rs.getString(9)));
        return new PasswordRecoveryDtos.Page(items,items.stream().filter(i->"PENDING".equals(i.status())).count());
    }

    @Transactional
    public PasswordRecoveryDtos.Resolution resolve(long requestId,long adminId){
        return audit.audited("PASSWORD_RECOVERY","RESOLVE",requestId,"管理员恢复账号默认密码",()->{
            RequestRow row=lock(requestId); if(!"PENDING".equals(row.status())) conflict();
            String hash=encoder.encode(policy.password());
            jdbc.update("UPDATE yong_hu SET mi_ma_zhai_yao=?,shi_fou_shou_ci_deng_lu=0,mi_ma_xiu_gai_shi_jian=NULL WHERE id=?",hash,row.userId());
            jdbc.update("UPDATE mi_ma_chong_zhi_shen_qing SET zhuang_tai='RESOLVED',chu_li_ren_id=?,chu_li_shi_jian=CURRENT_TIMESTAMP(3),chu_li_jie_guo='DEFAULT_PASSWORD_RESTORED' WHERE id=?",adminId,requestId);
            return new PasswordRecoveryDtos.Resolution(requestId,"RESOLVED");
        });
    }

    @Transactional
    public PasswordRecoveryDtos.Resolution reject(long requestId,long adminId,String reason){
        return audit.audited("PASSWORD_RECOVERY","REJECT",requestId,"管理员驳回密码恢复请求",()->{
            RequestRow row=lock(requestId); if(!"PENDING".equals(row.status())) conflict();
            jdbc.update("UPDATE mi_ma_chong_zhi_shen_qing SET zhuang_tai='REJECTED',chu_li_ren_id=?,chu_li_shi_jian=CURRENT_TIMESTAMP(3),chu_li_jie_guo=? WHERE id=?",adminId,reason.trim(),requestId);
            return new PasswordRecoveryDtos.Resolution(requestId,"REJECTED");
        });
    }
    @Transactional
    public void delete(long requestId){
        RequestRow row=lock(requestId);
        if("PENDING".equals(row.status())) throw new RenZhengYeWuYiChang("PASSWORD_RECOVERY_PENDING", "请先处理或驳回待处理的密码恢复请求", HttpStatus.CONFLICT);
        jdbc.update("DELETE FROM mi_ma_chong_zhi_shen_qing WHERE id=?",requestId);
    }
    private RequestRow lock(long id){return jdbc.query("SELECT yong_hu_id,zhuang_tai FROM mi_ma_chong_zhi_shen_qing WHERE id=? FOR UPDATE",(rs,row)->new RequestRow(rs.getLong(1),rs.getString(2)),id).stream().findFirst().orElseThrow(()->new RenZhengYeWuYiChang("PASSWORD_RECOVERY_NOT_FOUND","密码恢复请求不存在",HttpStatus.NOT_FOUND));}
    private void conflict(){throw new RenZhengYeWuYiChang("PASSWORD_RECOVERY_ALREADY_HANDLED","密码恢复请求已经处理",HttpStatus.CONFLICT);}
    private record RequestRow(long userId,String status){}
}
