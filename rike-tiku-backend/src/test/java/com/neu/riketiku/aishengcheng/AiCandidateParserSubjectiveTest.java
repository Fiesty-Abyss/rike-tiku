package com.neu.riketiku.aishengcheng;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AiCandidateParserSubjectiveTest {
    private final AiCandidateParser parser=new AiCandidateParser();
    @Test void parsesSubjectiveTopicCandidateWithoutObjectiveAnswerOrOptions(){
        var values=parser.parse("""
                {"schemaVersion":2,"candidates":[{"stem":"新的综合材料情境，要求分步骤分析变量关系。","questionType":"SUBJECTIVE","difficulty":3,
                "options":[],"correctAnswer":{"schemaVersion":1,"type":"SUBJECTIVE"},"standardAnalysis":"步骤1：读取材料。步骤2：建立关系。结论：形成待审核解析。",
                "variationMode":"SCENARIO_TRANSFER","variationSummary":"更换情境并重组条件","changedDimensions":["SCENARIO","CONDITION"]}]}
                """,1,"SUBJECTIVE",3,"SCENARIO_TRANSFER");
        assertThat(values).singleElement().satisfies(value->{assertThat(value.options()).isEmpty();assertThat(value.correctAnswer()).contains("SUBJECTIVE");});
    }
    @Test void rejectsSubjectiveAnswerWithExtraFields(){
        assertThatThrownBy(()->parser.parse("""
                {"schemaVersion":2,"candidates":[{"stem":"综合材料","questionType":"SUBJECTIVE","difficulty":3,
                "options":[],"correctAnswer":{"schemaVersion":1,"type":"SUBJECTIVE","answer":"不要保存"},"standardAnalysis":"解析",
                "variationMode":"SCENARIO_TRANSFER","variationSummary":"变化","changedDimensions":["SCENARIO","CONDITION"]}]}
                """,1,"SUBJECTIVE",3,"SCENARIO_TRANSFER")).isInstanceOf(AiCandidateParser.InvalidCandidateException.class);
    }
    @Test void rejectsSubjectiveCandidateThatSmugglesObjectiveOptions(){
        assertThatThrownBy(()->parser.parse("""
                {"schemaVersion":2,"candidates":[{"stem":"综合材料","questionType":"SUBJECTIVE","difficulty":3,
                "options":[{"label":"A","content":"错误选项","correct":true}],"correctAnswer":{"type":"SUBJECTIVE"},"standardAnalysis":"解析",
                "variationMode":"SCENARIO_TRANSFER","variationSummary":"变化","changedDimensions":["SCENARIO","CONDITION"]}]}
                """,1,"SUBJECTIVE",3,"SCENARIO_TRANSFER")).isInstanceOf(AiCandidateParser.InvalidCandidateException.class);
    }
}
