package com.neu.riketiku.tiku.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AdminQuestionQueryIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private QuestionAdminService service;

    @Test
    void pagesAndCombinesKnownSampleFilters() {
        var all = service.page(1, 2, null, null, null, null, "PENDING", null, null);
        assertThat(all.records()).hasSizeLessThanOrEqualTo(2);
        assertThat(all.total()).isGreaterThanOrEqualTo(3);
        assertThat(service.page(1, 10, "PHYSICS", "SINGLE_CHOICE", "ONLINE_PRACTICE", 1, "PENDING", "声波", "COPYRIGHT_UNKNOWN").records()).hasSize(1);
    }

    @Test
    void detailContainsRelationsAndNoAttachmentPath() {
        var detail = service.detail(1L);
        assertThat(detail.stem()).contains("声波");
        assertThat(detail.options()).hasSize(4);
        assertThat(detail.correctAnswer()).contains("optionLabels");
        assertThat(detail.standardAnalysis()).isNotBlank();
        assertThat(detail.knowledgePoints()).isNotEmpty();
        assertThat(detail.sources()).hasSize(3);
        assertThat(detail.attachments()).allSatisfy(attachment -> assertThat(attachment.fileName()).doesNotContain(":"));
    }

    @Test
    void missingQuestionReturnsBusinessNotFound() {
        assertThatThrownBy(() -> service.detail(999999L)).isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("不存在");
    }
}
