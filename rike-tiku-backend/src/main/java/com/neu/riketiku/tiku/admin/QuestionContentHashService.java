package com.neu.riketiku.tiku.admin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.stereotype.Service;

/** Keeps duplicate detection and attachment-driven question hash updates on one exact rule. */
@Service
public class QuestionContentHashService {
    public String calculate(String stem, List<OptionContent> options) {
        try {
            StringBuilder value = new StringBuilder(stem == null ? "" : stem.replaceAll("\\s+", ""));
            for (OptionContent option : options == null ? List.<OptionContent>of() : options) {
                value.append('|').append(option.label().trim()).append(':').append(option.content().replaceAll("\\s+", ""));
            }
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算内容哈希", exception);
        }
    }

    public record OptionContent(String label, String content) { }
}
