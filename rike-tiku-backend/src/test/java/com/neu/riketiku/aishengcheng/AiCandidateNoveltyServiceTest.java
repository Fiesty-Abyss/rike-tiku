package com.neu.riketiku.aishengcheng;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AiCandidateNoveltyServiceTest {
    private final AiCandidateNoveltyService service = new AiCandidateNoveltyService();

    @Test
    void distinguishesAcceptWarnAndRejectWithoutChangingHardGates() {
        AiCandidateParser.Candidate accept = candidate("在全新的实验材料中比较两个独立条件并说明证据链", "全新情境并改变推理路径");
        AiCandidateParser.Candidate warn = candidate("小车在水平面上受到恒力作用求加速度与时间", "保留部分物理结构并增加时间判断");
        AiCandidateParser.Candidate reject = candidate("小车在水平面上受到恒力作用求加速度和位移", "仅替换数字和名称");

        assertThat(service.evaluate("小车在水平面上受到恒力作用求加速度和位移", "[]", "先列式并检查单位", accept).decision())
                .isEqualTo(AiCandidateNoveltyService.Decision.ACCEPT);
        assertThat(service.evaluate("小车在水平面上受到恒力作用求加速度和位移", "[]", "先列式并检查单位", warn).decision())
                .isEqualTo(AiCandidateNoveltyService.Decision.WARN);
        assertThat(service.evaluate("小车在水平面上受到恒力作用求加速度和位移", "[]", "先列式并检查单位", reject).decision())
                .isEqualTo(AiCandidateNoveltyService.Decision.REJECT);
    }

    @Test
    void allowsHigherDifficultyWhenCandidateContainsAComplexityCombination() {
        AiCandidateParser.Candidate candidate = candidate(
                "改变电路条件后比较两组数据并判断结论",
                4,
                "CONDITION_RECOMBINATION",
                List.of("CONDITION", "DATA"),
                "重新组织条件并比较数据证据");

        assertThat(service.evaluate("小车在水平面上受到恒力作用求加速度和位移", "[]", "先列式并检查单位", candidate).rejected())
                .isFalse();
    }

    @Test
    void rejectsCombinedModeWithOnlyTwoChangedDimensions() {
        AiCandidateParser.Candidate candidate = candidate(
                "在新的情境中比较两组测量结果",
                3,
                "COMBINED",
                List.of("SCENARIO", "DATA"),
                "更换情境并重组数据");

        assertThat(service.evaluate("小车在水平面上受到恒力作用求加速度和位移", "[]", "先列式并检查单位", candidate).rejectionReason())
                .isEqualTo("VARIATION_DIMENSIONS_INSUFFICIENT");
    }

    private AiCandidateParser.Candidate candidate(String stem, String summary) {
        return candidate(stem, 3, "SCENARIO_TRANSFER", List.of("SCENARIO", "DATA"), summary);
    }

    private AiCandidateParser.Candidate candidate(String stem, int difficulty, String mode,
                                                   List<String> dimensions, String summary) {
        return new AiCandidateParser.Candidate(stem, "SINGLE_CHOICE", difficulty,
                List.of(new AiCandidateParser.Option("A", "结论一", true),
                        new AiCandidateParser.Option("B", "结论二", false)),
                "{\"schemaVersion\":1,\"type\":\"SINGLE_CHOICE\",\"optionLabels\":[\"A\"]}",
                "先识别条件，再列出关系并检查单位。", mode, summary, dimensions);
    }
}
