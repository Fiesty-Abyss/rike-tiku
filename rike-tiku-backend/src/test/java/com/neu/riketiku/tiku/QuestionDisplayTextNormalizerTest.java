package com.neu.riketiku.tiku;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuestionDisplayTextNormalizerTest {
    private final QuestionDisplayTextNormalizer normalizer = new QuestionDisplayTextNormalizer();

    @Test
    void removesOnlyProvenInternalDemoPrefixes() {
        assertThat(normalizer.normalize("【演示】覆盖：受力分析题")).isEqualTo("受力分析题");
        assertThat(normalizer.normalize("【演示】变式：条件重组题")).isEqualTo("条件重组题");
        assertThat(normalizer.normalize("【专题演示】覆盖：流程分析题")).isEqualTo("流程分析题");
        assertThat(normalizer.normalize("【专题演示】变式：综合提升题")).isEqualTo("综合提升题");
        assertThat(normalizer.normalize("覆盖：变式：连续前缀题")).isEqualTo("连续前缀题");
    }

    @Test
    void preservesOrdinaryChineseWordsContainingCoverage() {
        assertThat(normalizer.normalize("计算植被覆盖率并说明覆盖范围")).isEqualTo("计算植被覆盖率并说明覆盖范围");
        assertThat(normalizer.normalize("植被覆盖率并说明覆盖范围")).isEqualTo("植被覆盖率并说明覆盖范围");
    }
}
