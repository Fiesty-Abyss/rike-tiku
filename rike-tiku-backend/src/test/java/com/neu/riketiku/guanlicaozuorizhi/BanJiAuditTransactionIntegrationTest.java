package com.neu.riketiku.guanlicaozuorizhi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.jiaoxue.BanJiFuWu;
import com.neu.riketiku.jiaoxue.dto.BanJiChuangJianQingQiu;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Primary;

@SpringBootTest
@Import(BanJiAuditTransactionIntegrationTest.FailingAuditConfiguration.class)
class BanJiAuditTransactionIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private BanJiFuWu classes;
    @Autowired private JdbcTemplate jdbc;
    private String classCode;

    @Test
    void successAuditFailureRollsBackClassWrite() {
        classCode = "AUDIT-" + UUID.randomUUID().toString().substring(0, 8);

        assertThatThrownBy(() -> classes.create(new BanJiChuangJianQingQiu(classCode, "审计回滚班", "高一", 2026)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("故意模拟成功审计失败");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ban_ji WHERE ban_ji_bian_ma=?", Integer.class, classCode)).isZero();
    }

    @AfterEach
    void cleanup() {
        if (classCode != null) jdbc.update("DELETE FROM ban_ji WHERE ban_ji_bian_ma=?", classCode);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingAuditConfiguration {
        @Bean
        @Primary
        GuanLiCaoZuoRiZhiJiLuFuWu failingAuditRecordService(JdbcTemplate jdbc) {
            return new GuanLiCaoZuoRiZhiJiLuFuWu(jdbc) {
                @Override
                public void success(String module, String action, Long objectId, String summary) {
                    throw new IllegalStateException("故意模拟成功审计失败");
                }

                @Override
                public void failure(String module, String action, Long objectId, String errorCode) {
                    // The business transaction must still roll back; the real failure path is tested separately.
                }
            };
        }
    }
}
