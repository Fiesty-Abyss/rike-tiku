package com.neu.riketiku.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.stereotype.Component;

/**
 * Converts the single V14 legacy variation-mode name before V25 tightens its check constraint.
 * The callback is deliberately version-gated and idempotent; it is not a general data repair path.
 */
@Component
public final class LegacyVariantModeFlywayCallback implements Callback {

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.BEFORE_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        Connection connection = context.getConnection();
        try {
            if (!tableExists(connection, "flyway_schema_history")
                || !tableExists(connection, "ai_sheng_cheng_ren_wu")
                || installedVersion(connection) != 24) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE ai_sheng_cheng_ren_wu DROP CHECK ck_ai_sheng_cheng_mode");
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE ai_sheng_cheng_ren_wu
                SET bian_shi_fang_shi = 'CONDITION_RECOMBINATION'
                WHERE bian_shi_fang_shi = 'NUMERIC_CONDITION'
                """)) {
                statement.executeUpdate();
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                    ALTER TABLE ai_sheng_cheng_ren_wu
                    ADD CONSTRAINT ck_ai_sheng_cheng_mode CHECK (
                        bian_shi_fang_shi IN (
                            'SCENARIO_TRANSFER','CONDITION_RECOMBINATION','REPRESENTATION_SWITCH',
                            'MULTI_STEP_EXTENSION','DISTRACTOR_REDESIGN','COMBINED'
                        )
                    )
                    """);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to normalize the V24 legacy variation mode", exception);
        }
    }

    @Override
    public String getCallbackName() {
        return "normalize-v24-legacy-variant-mode";
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), null, table, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private int installedVersion(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT COALESCE(MAX(CAST(version AS UNSIGNED)), 0)
            FROM flyway_schema_history
            WHERE success = 1
            """); ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }
}
