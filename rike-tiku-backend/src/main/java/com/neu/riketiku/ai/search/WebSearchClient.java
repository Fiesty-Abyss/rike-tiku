package com.neu.riketiku.ai.search;

import java.util.List;

public interface WebSearchClient {
    List<WebSearchResult> search(WebSearchRequest request);
}
