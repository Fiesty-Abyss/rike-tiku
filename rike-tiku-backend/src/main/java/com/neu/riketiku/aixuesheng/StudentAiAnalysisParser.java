package com.neu.riketiku.aixuesheng;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class StudentAiAnalysisParser {
    private static final Set<String> FIELDS = Set.of(
            "errorType", "errorReason", "correctThinking", "commonMistakes", "reviewSuggestions");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParsedAnalysis parse(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            if (!root.isObject() || root.size() != FIELDS.size() || FIELDS.stream().anyMatch(field -> !root.has(field))) {
                throw invalid();
            }
            StudentAiErrorType type = StudentAiErrorType.valueOf(requiredText(root, "errorType", 32));
            String reason = requiredText(root, "errorReason", 1200);
            String thinking = requiredText(root, "correctThinking", 1600);
            List<String> mistakes = requiredArray(root, "commonMistakes");
            List<String> suggestions = requiredArray(root, "reviewSuggestions");
            return new ParsedAnalysis(type, reason, thinking, mistakes, suggestions);
        } catch (InvalidAnalysisException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private String requiredText(JsonNode root, String field, int max) {
        JsonNode node = root.path(field);
        if (!node.isTextual()) throw invalid();
        String value = node.asText().trim();
        if (value.isEmpty() || value.length() > max) throw invalid();
        return value;
    }

    private List<String> requiredArray(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (!node.isArray() || node.isEmpty() || node.size() > 5) throw invalid();
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual()) throw invalid();
            String value = item.asText().trim();
            if (value.isEmpty() || value.length() > 300) throw invalid();
            values.add(value);
        }
        return List.copyOf(values);
    }

    private InvalidAnalysisException invalid() { return new InvalidAnalysisException(); }

    public record ParsedAnalysis(StudentAiErrorType errorType, String errorReason, String correctThinking,
                                 List<String> commonMistakes, List<String> reviewSuggestions) { }

    public static final class InvalidAnalysisException extends RuntimeException { }
}
