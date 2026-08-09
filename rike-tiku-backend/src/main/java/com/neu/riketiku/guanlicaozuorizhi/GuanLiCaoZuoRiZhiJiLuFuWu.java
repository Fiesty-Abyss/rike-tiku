package com.neu.riketiku.guanlicaozuorizhi;

import com.neu.riketiku.renzheng.RenZhengYongHu;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuanLiCaoZuoRiZhiJiLuFuWu {
    private final JdbcTemplate jdbc;

    public GuanLiCaoZuoRiZhiJiLuFuWu(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void success(String module, String action, Long objectId, String summary) {
        insert(module, action, objectId, "SUCCESS", summary, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failure(String module, String action, Long objectId, String errorCode) {
        insert(module, action, objectId, "FAILURE", "操作失败", errorCode);
    }

    private void insert(String module, String action, Long objectId, String result, String summary, String errorCode) {
        jdbc.update("""
                INSERT INTO guan_li_cao_zuo_ri_zhi
                    (cao_zuo_ren_yong_hu_id,mo_kuai,cao_zuo_lei_xing,ye_wu_dui_xiang_id,cao_zuo_jie_guo,zhai_yao,cuo_wu_dai_ma)
                VALUES (?,?,?,?,?,?,?)
                """, operatorId(), safe(module, 64), safe(action, 96), objectId, result,
                safe(summary, 1000), safe(errorCode, 96));
    }

    private Long operatorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof RenZhengYongHu principal) {
            return principal.id();
        }
        return null;
    }

    private String safe(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
