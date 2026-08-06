package com.neu.riketiku.tiku.admin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class AdminQuestionIntegrationTestSupport {
    private static final String PASSWORD = requiredEnvironment("RIKE_TIKU_DB_PASSWORD");
    private static final String USERNAME = environment("RIKE_TIKU_DB_USERNAME", "root");
    private static final String HOST = environment("RIKE_TIKU_DB_HOST", "localhost");
    private static final String PORT = environment("RIKE_TIKU_DB_PORT", "3306");
    private static final String OPTIONS = "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai";
    private static final String SCHEMA = "rike_tiku_question_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String ADMIN_URL = "jdbc:mysql://" + HOST + ":" + PORT + "/mysql" + OPTIONS;
    private static final String TEST_URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + SCHEMA + OPTIONS;

    static {
        try (Connection connection = DriverManager.getConnection(ADMIN_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + SCHEMA + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            Runtime.getRuntime().addShutdownHook(new Thread(AdminQuestionIntegrationTestSupport::dropDatabase));
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TEST_URL);
        registry.add("spring.datasource.username", () -> USERNAME);
        registry.add("spring.datasource.password", () -> PASSWORD);
    }

    private static void dropDatabase() {
        try (Connection connection = DriverManager.getConnection(ADMIN_URL, USERNAME, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + SCHEMA + "`");
        } catch (Exception ignored) {
            // JVM shutdown cannot recover from an unavailable local database.
        }
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required for integration tests");
        return value;
    }
}
