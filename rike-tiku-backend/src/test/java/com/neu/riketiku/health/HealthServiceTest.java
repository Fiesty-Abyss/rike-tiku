package com.neu.riketiku.health;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

class HealthServiceTest {

    @Test
    void reportsDatabaseDownWhenConnectionFails() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("database unavailable"));

        HealthService healthService = new HealthService(dataSource);

        assertFalse(healthService.isDatabaseUp());
    }
}

