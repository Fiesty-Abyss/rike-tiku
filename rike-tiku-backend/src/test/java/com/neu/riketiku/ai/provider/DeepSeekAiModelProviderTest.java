package com.neu.riketiku.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.riketiku.ai.config.AiProviderProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class DeepSeekAiModelProviderTest {
    private static final String SECRET = "test-secret-that-must-not-leak";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private final AtomicReference<JsonNode> lastRequest = new AtomicReference<>();

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void mapsSuccessMessagesJsonModeAndTokenUsage() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = server(attempt -> new Reply(200, successJson()), calls, 0);
        DeepSeekAiModelProvider provider = provider(properties(1, Duration.ofSeconds(1)));

        AiModelResult result = provider.generate(new AiModelRequest(
                List.of(new AiMessage("system", "return json"), new AiMessage("user", "hello")),
                "unit-test", null, true, 1200, AiThinkingMode.DISABLED));

        assertThat(calls).hasValue(1);
        assertThat(result.providerCode()).isEqualTo("deepseek");
        assertThat(result.modelCode()).isEqualTo("deepseek-v4-flash");
        assertThat(result.content()).isEqualTo("mock answer");
        assertThat(result.usage()).isEqualTo(new AiTokenUsage(12, 8, 20));
        assertThat(lastRequest.get().path("response_format").path("type").asText()).isEqualTo("json_object");
        assertThat(lastRequest.get().path("thinking").path("type").asText()).isEqualTo("disabled");
        assertThat(lastRequest.get().path("max_tokens").asInt()).isEqualTo(1200);
    }

    @Test
    void retries429And5xxExactlyOnce() throws Exception {
        assertRetriesThenSucceeds(429, AiProviderErrorType.RATE_LIMITED);
        stopServer();
        assertRetriesThenSucceeds(503, AiProviderErrorType.PROVIDER_UNAVAILABLE);
    }

    @Test
    void configuredRetryCountCanNeverExceedOne() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = server(attempt -> new Reply(503, "{}"), calls, 0);
        AiProviderProperties properties = properties(99, Duration.ofSeconds(1));

        assertThatThrownBy(() -> provider(properties).generate(request()))
                .isInstanceOfSatisfying(AiProviderException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(AiProviderErrorType.PROVIDER_UNAVAILABLE));
        assertThat(calls).hasValue(2);
    }

    @Test
    void doesNotRetry400OrAuthenticationFailures() throws Exception {
        assertNoRetry(400, AiProviderErrorType.UNKNOWN);
        stopServer();
        assertNoRetry(401, AiProviderErrorType.AUTHENTICATION_ERROR);
        stopServer();
        assertNoRetry(403, AiProviderErrorType.AUTHENTICATION_ERROR);
    }

    @Test
    void timesOutAndRetriesOnlyOnce() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = server(attempt -> new Reply(200, successJson()), calls, 250);
        DeepSeekAiModelProvider provider = provider(properties(1, Duration.ofMillis(60)));

        assertThatThrownBy(() -> provider.generate(request()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(AiProviderErrorType.TIMEOUT);
                    assertThat(exception.getMessage()).doesNotContain(SECRET);
                });
        assertThat(calls).hasValue(2);
    }

    @Test
    void rejectsInvalidResponseWithoutLeakingSecret() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = server(attempt -> new Reply(200, "{not-json"), calls, 0);

        assertThatThrownBy(() -> provider(properties(1, Duration.ofSeconds(1))).generate(request()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(AiProviderErrorType.INVALID_RESPONSE);
                    assertThat(exception.getMessage()).doesNotContain(SECRET).doesNotContain("{not-json");
                });
        assertThat(calls).hasValue(1);
    }

    @Test
    void apiKeyNeverAppearsInProviderLogs() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = server(attempt -> new Reply(401, "{\"error\":\"authentication failed\"}"), calls, 0);
        Logger logger = (Logger) LoggerFactory.getLogger(DeepSeekAiModelProvider.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatThrownBy(() -> provider(properties(1, Duration.ofSeconds(1))).generate(request()))
                    .isInstanceOf(AiProviderException.class);
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                    .allSatisfy(message -> assertThat(message).doesNotContain(SECRET));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void disabledAndMissingKeyAreControlledWithoutNetwork() {
        AiProviderProperties disabled = properties(1, Duration.ofSeconds(1));
        disabled.setEnabled(false);
        DeepSeekAiModelProvider disabledProvider = provider(disabled);
        assertThat(disabledProvider.status().errorType()).isEqualTo(AiProviderErrorType.DISABLED);
        assertThatThrownBy(() -> disabledProvider.generate(request()))
                .isInstanceOfSatisfying(AiProviderException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(AiProviderErrorType.DISABLED));

        AiProviderProperties missing = properties(1, Duration.ofSeconds(1));
        missing.setApiKey("");
        DeepSeekAiModelProvider missingProvider = provider(missing);
        assertThat(missingProvider.status().errorType()).isEqualTo(AiProviderErrorType.CONFIGURATION_ERROR);
        assertThatThrownBy(() -> missingProvider.generate(request()))
                .isInstanceOfSatisfying(AiProviderException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(AiProviderErrorType.CONFIGURATION_ERROR));
    }

    private void assertRetriesThenSucceeds(int firstStatus, AiProviderErrorType expectedFirstType) throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = server(attempt -> attempt == 1 ? new Reply(firstStatus, "{}") : new Reply(200, successJson()), calls, 0);
        assertThat(provider(properties(1, Duration.ofSeconds(1))).generate(request()).content()).isEqualTo("mock answer");
        assertThat(calls).hasValue(2);
        assertThat(expectedFirstType).isNotNull();
    }

    private void assertNoRetry(int status, AiProviderErrorType type) throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = server(attempt -> new Reply(status, "{}"), calls, 0);
        assertThatThrownBy(() -> provider(properties(1, Duration.ofSeconds(1))).generate(request()))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.errorType()).isEqualTo(type);
                    assertThat(exception.getMessage()).doesNotContain(SECRET);
                });
        assertThat(calls).hasValue(1);
    }

    private HttpServer server(IntFunction<Reply> replies, AtomicInteger calls, long delayMillis) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.createContext("/chat/completions", exchange -> handle(exchange, replies, calls, delayMillis));
        httpServer.start();
        return httpServer;
    }

    private void handle(HttpExchange exchange, IntFunction<Reply> replies, AtomicInteger calls, long delayMillis)
            throws IOException {
        int attempt = calls.incrementAndGet();
        try {
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer " + SECRET);
            JsonNode requestBody = objectMapper.readTree(exchange.getRequestBody());
            lastRequest.set(requestBody);
            assertThat(requestBody.path("stream").asBoolean()).isFalse();
            if (delayMillis > 0) Thread.sleep(delayMillis);
            Reply reply = replies.apply(attempt);
            byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status(), bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            exchange.close();
        }
    }

    private DeepSeekAiModelProvider provider(AiProviderProperties properties) {
        return new DeepSeekAiModelProvider(properties,
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(200)).build(), objectMapper);
    }

    private AiProviderProperties properties(int retries, Duration requestTimeout) {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://127.0.0.1:" + (server == null ? 1 : server.getAddress().getPort()));
        properties.setModel("deepseek-v4-flash");
        properties.setApiKey(SECRET);
        properties.setConnectTimeout(Duration.ofMillis(200));
        properties.setRequestTimeout(requestTimeout);
        properties.setRetryCount(retries);
        return properties;
    }

    private AiModelRequest request() { return AiModelRequest.text("unit-test", "hello"); }
    private String successJson() {
        return """
                {"model":"deepseek-v4-flash","choices":[{"message":{"content":"mock answer"},"finish_reason":"stop"}],
                 "usage":{"prompt_tokens":12,"completion_tokens":8,"total_tokens":20}}
                """;
    }
    private record Reply(int status, String body) { }
}
