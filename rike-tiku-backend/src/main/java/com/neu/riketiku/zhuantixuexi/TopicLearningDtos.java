package com.neu.riketiku.zhuantixuexi;

import java.util.List;

public final class TopicLearningDtos {
    private TopicLearningDtos() {
    }

    public record KnowledgePoint(Long id, String name, String path) {
    }

    public record TopicItem(Long id, Long subjectId, String subjectCode, String subjectName,
                            String title, int difficulty, List<KnowledgePoint> knowledgePoints) {
    }

    public record TopicDetail(Long id, Long subjectId, String subjectCode, String subjectName,
                              String title, String material, int difficulty, String standardAnalysis,
                              List<KnowledgePoint> knowledgePoints) {
    }
}
