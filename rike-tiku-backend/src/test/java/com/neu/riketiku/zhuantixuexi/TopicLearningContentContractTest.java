package com.neu.riketiku.zhuantixuexi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

class TopicLearningContentContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void authoredTopicUnitsStayAsThreeStepSubjectiveBigQuestions() throws Exception {
        Path source = Path.of("..", "docs", "content", "topic-units.v2.json").toAbsolutePath().normalize();
        Map<String, Object> content = mapper.readValue(Files.readString(source), new TypeReference<>() { });
        assertThat(content.get("version")).isEqualTo(3);
        List<Map<String, Object>> units = (List<Map<String, Object>>) content.get("units");
        assertThat(units).hasSize(15);
        assertThat(units).extracting(unit -> unit.get("subjectCode"))
                .containsExactlyInAnyOrder("PHYSICS", "PHYSICS", "PHYSICS", "PHYSICS", "PHYSICS", "PHYSICS",
                        "CHEMISTRY", "CHEMISTRY", "CHEMISTRY", "CHEMISTRY", "CHEMISTRY",
                        "BIOLOGY", "BIOLOGY", "BIOLOGY", "BIOLOGY");
        for (Map<String, Object> unit : units) {
            List<Map<String, Object>> questions = (List<Map<String, Object>>) unit.get("questions");
            assertThat(questions).hasSize(3);
            assertThat(questions).extracting(question -> question.get("stage"))
                    .containsExactly("FOUNDATION", "TRANSFER", "ADVANCED");
            assertThat(questions).allSatisfy(question -> {
                assertThat(question.get("topicType")).isIn("CALCULATION", "EXPERIMENT", "PROCESS", "MATERIAL_ANALYSIS", "COMPREHENSIVE");
                assertThat((String) question.get("stem")).contains("（").contains("\n").doesNotContain("\\n").isNotEmpty();
                assertThat((String) question.get("standardAnalysis")).contains("\n").doesNotContain("\\n").hasSizeGreaterThanOrEqualTo(120);
                assertThat(question.get("knowledgePointId")).isNotNull();
            });
        }
    }
}
