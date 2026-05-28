package org.carey.travelgadget.service;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.web_search.WebSearchRequest;
import ai.z.openapi.service.web_search.WebSearchResponse;
import ai.z.openapi.service.web_search.WebSearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZhipuWebSearchService {

    private static final int MAX_RESULTS = 5;
    private static final int MAX_ITEM_CONTENT_CHARS = 600;
    private static final int MAX_TOTAL_CHARS = 12000;

    private final ZhipuClientHolder zhipuClientHolder;
    private final ObjectMapper objectMapper;

    public String search(String searchQuery) {
        ZhipuAiClient client = zhipuClientHolder.getClient();
        if (client == null) {
            return null;
        }
        WebSearchRequest request = WebSearchRequest.builder()
                .searchEngine("search_pro")
                .searchQuery(searchQuery)
                .count(MAX_RESULTS)
                .searchRecencyFilter("noLimit")
                .contentSize("medium")
                .build();
        log.info("调用智谱 WebSearch，query={}", searchQuery);
        WebSearchResponse response = client.webSearch().createWebSearch(request);
        return compactSearchResult(response);
    }

    private String compactSearchResult(WebSearchResponse response) {
        try {
            JsonNode root = objectMapper.valueToTree(response);
            JsonNode results = findResultsArray(root);
            if (results == null || !results.isArray() || results.isEmpty()) {
                return truncate(objectMapper.writeValueAsString(response), MAX_TOTAL_CHARS);
            }
            StringBuilder sb = new StringBuilder();
            int index = 0;
            for (JsonNode item : results) {
                if (index >= MAX_RESULTS || sb.length() >= MAX_TOTAL_CHARS) {
                    break;
                }
                String title = firstText(item, "title", "name");
                String content = truncate(firstText(item, "content", "snippet", "summary"), MAX_ITEM_CONTENT_CHARS);
                if (!title.isBlank() || !content.isBlank()) {
                    sb.append(index + 1).append(". ").append(title).append('\n');
                    if (!content.isBlank()) {
                        sb.append(content).append('\n');
                    }
                    sb.append('\n');
                    index++;
                }
            }
            String compact = sb.toString().trim();
            return compact.isBlank() ? truncate(objectMapper.writeValueAsString(response), MAX_TOTAL_CHARS) : compact;
        } catch (JsonProcessingException e) {
            return truncate(String.valueOf(response), MAX_TOTAL_CHARS);
        }
    }

    private JsonNode findResultsArray(JsonNode root) {
        for (String key : new String[]{"search_result", "searchResult", "results", "data"}) {
            JsonNode node = root.path(key);
            if (node.isArray()) {
                return node;
            }
        }
        return null;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isTextual() && !value.asText().isBlank()) {
                return value.asText().trim();
            }
        }
        return "";
    }

    private String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "\n...(truncated)";
    }
}
