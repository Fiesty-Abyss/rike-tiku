package com.neu.riketiku.ai.search;

public record WebSearchRequest(String query, int limit) {
    public WebSearchRequest {
        if (query == null || query.isBlank() || query.length() > 70) throw new IllegalArgumentException("Search query must be 1-70 characters");
        if (limit < 1 || limit > 5) throw new IllegalArgumentException("Search result limit must be 1-5");
    }
}
