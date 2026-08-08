package com.neu.riketiku.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DatabaseConnectionTest extends AdminQuestionIntegrationTestSupport {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    DatabaseConnectionTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void connectsToExpectedDatabase() {
        String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        assertTrue(databaseName != null && databaseName.startsWith("rike_tiku_question_test_"),
                "自动化测试必须使用随机临时库，禁止连接正式 rike_tiku");
        assertEquals(1, result);
    }
}
