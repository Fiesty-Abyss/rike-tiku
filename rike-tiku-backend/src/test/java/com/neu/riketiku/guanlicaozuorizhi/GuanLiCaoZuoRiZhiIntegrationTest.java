package com.neu.riketiku.guanlicaozuorizhi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.renzheng.RenZhengYongHu;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootTest
class GuanLiCaoZuoRiZhiIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private GuanLiCaoZuoRiZhiFuWu service;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void recordsSuccessAndFailureWithOperatorAndSafeSummary() {
        String username = "audit_test_" + UUID.randomUUID();
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao) VALUES (?,?)", username, "x".repeat(60));
        Long operatorId = jdbc.queryForObject("SELECT id FROM yong_hu WHERE yong_hu_ming=?", Long.class, username);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new RenZhengYongHu(operatorId, username, List.of("ADMIN"), false), null, List.of()));
        try {
            service.audited("TEST", "SUCCESS_ACTION", 42L, "只记录业务摘要", () -> "ok");
            assertThatThrownBy(() -> service.audited("TEST", "FAILURE_ACTION", 43L, "不应写入异常详情", () -> {
                throw new RenZhengYeWuYiChang("TEST_FAILURE", "包含不应进入日志的详情", org.springframework.http.HttpStatus.CONFLICT);
            })).isInstanceOf(RenZhengYeWuYiChang.class);

            var page = service.page(1, 10, "TEST", null, null);
            assertThat(page.records()).extracting(item -> item.action())
                    .contains("SUCCESS_ACTION", "FAILURE_ACTION");
            var success = page.records().stream().filter(item -> "SUCCESS_ACTION".equals(item.action())).findFirst().orElseThrow();
            var failure = page.records().stream().filter(item -> "FAILURE_ACTION".equals(item.action())).findFirst().orElseThrow();
            assertThat(success.operatorId()).isEqualTo(operatorId);
            assertThat(success.operatorUsername()).isEqualTo(username);
            assertThat(success.businessObjectId()).isEqualTo(42L);
            assertThat(success.summary()).isEqualTo("只记录业务摘要");
            assertThat(failure.result()).isEqualTo("FAILURE");
            assertThat(failure.errorCode()).isEqualTo("TEST_FAILURE");
            assertThat(failure.summary()).doesNotContain("不应写入日志");
        } finally {
            SecurityContextHolder.clearContext();
            jdbc.update("DELETE FROM guan_li_cao_zuo_ri_zhi WHERE cao_zuo_ren_yong_hu_id=?", operatorId);
            jdbc.update("DELETE FROM yong_hu WHERE id=?", operatorId);
        }
    }
}
