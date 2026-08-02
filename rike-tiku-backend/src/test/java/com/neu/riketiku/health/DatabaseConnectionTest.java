package com.neu.riketiku.health;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DatabaseConnectionTest {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    DatabaseConnectionTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void connectsToExpectedDatabase() {
        String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        assertEquals("rike_tiku", databaseName);
        assertEquals(1, result);
    }
}
