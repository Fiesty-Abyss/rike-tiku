package com.neu.riketiku.zhuantixuexi;

import static org.assertj.core.api.Assertions.assertThat;

import com.neu.riketiku.demo.DemoDataService;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class TopicLearningIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private DemoDataService demo;
    @Autowired private TopicLearningService service;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @Transactional
    void exposesTopic18ForReadingWithoutAddingItToAutomaticPracticePool() {
        demo.seed();
        long userId = jdbc.queryForObject("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_199_01'", Long.class);

        var all = service.list(userId, null);
        assertThat(all).hasSize(18);
        assertThat(all.stream().filter(item -> "PHYSICS".equals(item.subjectCode()))).hasSize(6);
        assertThat(all.stream().filter(item -> "CHEMISTRY".equals(item.subjectCode()))).hasSize(6);
        assertThat(all.stream().filter(item -> "BIOLOGY".equals(item.subjectCode()))).hasSize(6);

        var detail = service.detail(userId, all.getFirst().id());
        assertThat(detail.material()).isNotBlank();
        assertThat(detail.standardAnalysis()).contains("步骤");
        assertThat(detail.knowledgePoints()).isNotEmpty();
        var units=service.units(userId,null);
        assertThat(units).hasSize(3).allSatisfy(unit->assertThat(unit.questionCount()).isEqualTo(3));
        var unit=service.unit(userId,units.getFirst().id());
        assertThat(unit.questions()).extracting(TopicLearningDtos.UnitQuestion::stage)
                .containsExactly("FOUNDATION","TRANSFER","ADVANCED");
        var illustrated=all.stream().filter(item->item.title().equals("力学综合计算")).findFirst().orElseThrow();
        var illustratedDetail=service.detail(userId,illustrated.id());
        assertThat(illustratedDetail.stemAttachments()).singleElement().satisfies(item->assertThat(item.contentUrl()).contains("/topic-learning/"));
        assertThat(illustratedDetail.analysisAttachments()).hasSize(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM ti_mu WHERE id=? AND ti_mu_lei_xing='SUBJECTIVE'
                  AND shi_yong_mo_shi='TOPIC_LEARNING' AND shi_fou_ke_zi_dong_pan_fen=0
                """, Integer.class, detail.id())).isOne();
    }
}
