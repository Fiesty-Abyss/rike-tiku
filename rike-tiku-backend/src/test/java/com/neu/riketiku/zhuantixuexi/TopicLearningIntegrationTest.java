package com.neu.riketiku.zhuantixuexi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.demo.DemoDataService;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.util.List;
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

    @Test
    @Transactional
    void unitDetailRechecksEveryQuestionStatusSubjectAndTeachingScope() {
        demo.seed();
        long user199 = jdbc.queryForObject("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_199_01'",Long.class);
        long user200 = jdbc.queryForObject("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_200_01'",Long.class);
        long scope199 = jdbc.queryForObject("SELECT r.id FROM ren_ke_guan_xi r JOIN ban_ji b ON b.id=r.ban_ji_id JOIN ke_mu k ON k.id=r.ke_mu_id WHERE b.ban_ji_bian_ma='DEMO_CLASS_199' AND k.ke_mu_dai_ma='PHYSICS'",Long.class);
        long scope200 = jdbc.queryForObject("SELECT r.id FROM ren_ke_guan_xi r JOIN ban_ji b ON b.id=r.ban_ji_id JOIN ke_mu k ON k.id=r.ke_mu_id WHERE b.ban_ji_bian_ma='DEMO_CLASS_200' AND k.ke_mu_dai_ma='PHYSICS'",Long.class);
        long creator199=jdbc.queryForObject("SELECT j.yong_hu_id FROM ren_ke_guan_xi r JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id WHERE r.id=?",Long.class,scope199);
        long creator200=jdbc.queryForObject("SELECT j.yong_hu_id FROM ren_ke_guan_xi r JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id WHERE r.id=?",Long.class,scope200);
        long unit199 = jdbc.queryForObject("SELECT id FROM zhuan_ti_xue_xi_dan_yuan WHERE ke_mu_id=(SELECT id FROM ke_mu WHERE ke_mu_dai_ma='PHYSICS') ORDER BY id LIMIT 1",Long.class);
        List<Long> unit199Questions = jdbc.query("SELECT ti_mu_id FROM zhuan_ti_xue_xi_dan_yuan_ti_mu WHERE dan_yuan_id=? ORDER BY pai_xu",(rs,row)->rs.getLong(1),unit199);
        jdbc.update("UPDATE ti_mu SET ke_jian_fan_wei='TEACHING_SCOPE_PRIVATE',ren_ke_guan_xi_id=?,chuang_jian_ren_id=? WHERE id IN (?,?)",scope199,creator199,unit199Questions.get(0),unit199Questions.get(1));
        jdbc.update("UPDATE ti_mu SET ke_jian_fan_wei='TEACHING_SCOPE_PRIVATE',ren_ke_guan_xi_id=?,chuang_jian_ren_id=? WHERE id=?",scope200,creator200,unit199Questions.get(2));

        assertThat(service.unit(user199,unit199).questions()).hasSize(2)
                .extracting(item -> item.question().id()).containsExactly(unit199Questions.get(0),unit199Questions.get(1));
        assertThatThrownBy(() -> service.unit(user200,unit199))
                .isInstanceOfSatisfying(RenZhengYeWuYiChang.class,error -> assertThat(error.getStatus().value()).isEqualTo(404));

        long unitWithDraft = jdbc.queryForObject("SELECT id FROM zhuan_ti_xue_xi_dan_yuan WHERE id<>? ORDER BY id LIMIT 1",Long.class,unit199);
        List<Long> questions = jdbc.query("SELECT ti_mu_id FROM zhuan_ti_xue_xi_dan_yuan_ti_mu WHERE dan_yuan_id=? ORDER BY pai_xu",(rs,row)->rs.getLong(1),unitWithDraft);
        jdbc.update("UPDATE ti_mu SET zhuang_tai='DRAFT' WHERE id=?",questions.get(2));
        assertThat(service.unit(user199,unitWithDraft).questions()).hasSize(2)
                .allSatisfy(item -> assertThat(item.question().id()).isNotEqualTo(questions.get(2)));
    }
}
