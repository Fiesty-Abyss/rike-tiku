package com.neu.riketiku.ai.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.riketiku.ai.config.AiRuntimeConfig;
import com.neu.riketiku.ai.config.AiRuntimeConfigurationService;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class OfficialWebSearchClient implements WebSearchClient {
    private final AiRuntimeConfigurationService runtimes;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client;

    @Autowired public OfficialWebSearchClient(AiRuntimeConfigurationService runtimes) { this(runtimes,null); }
    OfficialWebSearchClient(AiRuntimeConfigurationService runtimes,HttpClient client) { this.runtimes = runtimes; this.client=client; }

    @Override
    public List<WebSearchResult> search(WebSearchRequest request) {
        return search(runtimes.search(), request);
    }

    public List<WebSearchResult> search(AiRuntimeConfig config, WebSearchRequest request) {
        if (!config.enabled() || config.apiKey() == null || config.apiKey().isBlank()) throw new WebSearchException("Search is unavailable");
        try {
            Map<String,Object> body = new LinkedHashMap<>();
            body.put("search_query", request.query());
            body.put("search_engine", config.model());
            body.put("search_intent", false);
            body.put("count", request.limit());
            body.put("search_recency_filter", "noLimit");
            body.put("content_size", "medium");
            String base = config.baseUrl().replaceAll("/+$", "");
            URI endpoint = URI.create(base.endsWith("/web_search") ? base : base + "/web_search");
            if (!"https".equalsIgnoreCase(endpoint.getScheme())) throw new WebSearchException("Search endpoint is invalid");
            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint).timeout(config.timeout())
                    .header("Authorization", "Bearer " + config.apiKey()).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpClient activeClient = client == null ? HttpClient.newBuilder().connectTimeout(config.timeout()).build() : client;
            for (int attempt=0; attempt<=config.retryCount(); attempt++) {
                try {
                    HttpResponse<String> response = activeClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() >= 200 && response.statusCode() < 300) return parse(response.body(), request.limit());
                    if (attempt < config.retryCount() && (response.statusCode()==429 || response.statusCode()>=500)) continue;
                    throw new WebSearchException("Search provider rejected the request");
                } catch (HttpTimeoutException exception) {
                    if (attempt < config.retryCount()) continue;
                    throw new WebSearchException("Search timed out", exception);
                }
            }
        } catch (WebSearchException exception) { throw exception; }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new WebSearchException("Search interrupted", exception); }
        catch (Exception exception) { throw new WebSearchException("Search unavailable", exception); }
        throw new WebSearchException("Search unavailable");
    }

    List<WebSearchResult> parse(String body, int limit) throws Exception {
        JsonNode results = mapper.readTree(body).path("search_result");
        if (!results.isArray()) throw new WebSearchException("Search response is invalid");
        List<WebSearchResult> safe = new ArrayList<>();
        for (JsonNode item : results) {
            if (safe.size() >= limit) break;
            String link = text(item,"link",500); URI uri;
            try { uri=URI.create(link); } catch (Exception ignored) { continue; }
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost()==null || !publicHost(uri.getHost())) continue;
            safe.add(new WebSearchResult(text(item,"title",300), uri.toString(), text(item,"media",120),
                    text(item,"publish_date",32), text(item,"content",800)));
        }
        return List.copyOf(safe);
    }

    private boolean publicHost(String host) {
        try {
            if (host.equalsIgnoreCase("localhost")) return false;
            InetAddress address=InetAddress.getByName(host);
            return !(address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress());
        } catch (Exception ignored) { return false; }
    }
    private String text(JsonNode node,String field,int max){String value=node.path(field).isTextual()?node.path(field).asText().trim():"";return value.substring(0,Math.min(max,value.length()));}
}
