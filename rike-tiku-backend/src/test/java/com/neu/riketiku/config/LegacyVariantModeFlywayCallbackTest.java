package com.neu.riketiku.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.junit.jupiter.api.Test;

class LegacyVariantModeFlywayCallbackTest {

    @Test
    void shouldNormalizeOnlyTheV24LegacyNumericMode() throws Exception {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        ResultSet historyTable = mock(ResultSet.class);
        ResultSet taskTable = mock(ResultSet.class);
        PreparedStatement versionStatement = mock(PreparedStatement.class);
        ResultSet versionResult = mock(ResultSet.class);
        PreparedStatement updateStatement = mock(PreparedStatement.class);
        Statement ddlStatement = mock(Statement.class);

        when(context.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("rike_tiku");
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getTables("rike_tiku", null, "flyway_schema_history", new String[]{"TABLE"}))
            .thenReturn(historyTable);
        when(metadata.getTables("rike_tiku", null, "ai_sheng_cheng_ren_wu", new String[]{"TABLE"}))
            .thenReturn(taskTable);
        when(historyTable.next()).thenReturn(true);
        when(taskTable.next()).thenReturn(true);
        when(connection.prepareStatement(contains("MAX(CAST(version"))).thenReturn(versionStatement);
        when(versionStatement.executeQuery()).thenReturn(versionResult);
        when(versionResult.next()).thenReturn(true);
        when(versionResult.getInt(1)).thenReturn(24);
        when(connection.prepareStatement(contains("SET bian_shi_fang_shi"))).thenReturn(updateStatement);
        when(updateStatement.executeUpdate()).thenReturn(1);
        when(connection.createStatement()).thenReturn(ddlStatement);

        LegacyVariantModeFlywayCallback callback = new LegacyVariantModeFlywayCallback();
        assertThat(callback.supports(Event.BEFORE_MIGRATE, context)).isTrue();
        callback.handle(Event.BEFORE_MIGRATE, context);

        verify(updateStatement).executeUpdate();
        verify(ddlStatement).execute("ALTER TABLE ai_sheng_cheng_ren_wu DROP CHECK ck_ai_sheng_cheng_mode");
    }
}
