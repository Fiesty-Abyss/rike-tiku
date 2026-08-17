package com.neu.riketiku.database;

/** Single source of truth for runtime/demo schema gates and migration tests. */
public final class DatabaseSchemaFacts {
    public static final int LATEST_FLYWAY_VERSION = 30;
    public static final int BUSINESS_TABLE_COUNT = 50;

    private DatabaseSchemaFacts() {
    }
}
