package com.neu.riketiku.guanliyibiao;

import static org.assertj.core.api.Assertions.assertThat;

import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class AdminDashboardIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private AdminDashboardService service;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @Transactional
    void returnsRealReadOnlyMetricsAndAtMostFiveRecentOperations() {
        var dashboard = service.dashboard();

        assertThat(dashboard.activeClassCount()).isEqualTo(count("SELECT COUNT(*) FROM ban_ji WHERE zhuang_tai='ACTIVE' AND yi_shan_chu=0"));
        assertThat(dashboard.publishedQuestionCount()).isEqualTo(count("SELECT COUNT(*) FROM ti_mu WHERE zhuang_tai='PUBLISHED' AND yi_shan_chu=0"));
        assertThat(dashboard.pendingQuestionCount()).isEqualTo(count("SELECT COUNT(*) FROM ti_mu WHERE zhuang_tai='PENDING' AND yi_shan_chu=0"));
        assertThat(dashboard.recentOperationLogs()).hasSizeLessThanOrEqualTo(5);
    }

    private long count(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }
}
