package com.neu.riketiku.database;

/** Single source of truth for runtime/demo schema gates and migration tests. */
public final class DatabaseSchemaFacts {
    public static final int LATEST_FLYWAY_VERSION = 28;
    public static final int BUSINESS_TABLE_COUNT = 49;

    private DatabaseSchemaFacts() {
    }
}
