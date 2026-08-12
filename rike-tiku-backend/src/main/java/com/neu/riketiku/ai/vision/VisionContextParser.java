package com.neu.riketiku.ai.vision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisionContextParser {
    private static final Set<String> FIELDS = Set.of("diagramType", "summary", "visibleText", "relations", "uncertainty");
    private static final Pattern JSON_FENCE = Pattern.compile("(?s)^```(?:json)?\\s*(\\{.*})\\s*```$");
    private final ObjectMapper mapper;
    public VisionContextParser(ObjectMapper mapper) { this.mapper = mapper; }

    public AiVisionContext parse(String content) {
        try {
            JsonNode root = mapper.readTree(normalize(content));
            if (!root.isObject()) throw invalid();
            Set<String> names = new HashSet<>(); root.fieldNames().forEachRemaining(names::add);
            if (!names.equals(FIELDS)) throw invalid();
            String type = text(root, "diagramType", 64);
            String summary = text(root, "summary", 800);
            List<String> visible = strings(root, "visibleText", 20, 120);
            List<String> relations = strings(root, "relations", 20, 160);
            List<String> uncertainty = strings(root, "uncertainty", 10, 160);
            int total = type.length() + summary.length() + visible.stream().mapToInt(String::length).sum()
                    + relations.stream().mapToInt(String::length).sum() + uncertainty.stream().mapToInt(String::length).sum();
            if (total > 1500) throw invalid();
            return new AiVisionContext(type, summary, visible, relations, uncertainty);
        } catch (AiVisionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiVisionException(com.neu.riketiku.ai.provider.AiProviderErrorType.INVALID_RESPONSE,
                    "Vision provider returned invalid JSON", exception);
        }
    }

    private String normalize(String content) {
        if (content == null || content.isBlank()) throw invalid();
        String value = content.trim();
        if (!value.startsWith("```")) return value;
        Matcher matcher = JSON_FENCE.matcher(value);
        if (!matcher.matches()) throw invalid();
        return matcher.group(1).trim();
    }

    private String text(JsonNode root, String field, int max) {
        JsonNode value = root.path(field);
        if (!value.isTextual() || value.asText().isBlank() || value.asText().length() > max) throw invalid();
        return value.asText().trim();
    }
    private List<String> strings(JsonNode root, String field, int maxItems, int maxChars) {
        JsonNode value = root.path(field);
        if (!value.isArray() || value.size() > maxItems) throw invalid();
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isTextual() || item.asText().isBlank() || item.asText().length() > maxChars) throw invalid();
            result.add(item.asText().trim());
        }
        return List.copyOf(result);
    }
    private AiVisionException invalid() {
        return new AiVisionException(com.neu.riketiku.ai.provider.AiProviderErrorType.INVALID_RESPONSE,
                "Vision provider returned invalid JSON");
    }
}
