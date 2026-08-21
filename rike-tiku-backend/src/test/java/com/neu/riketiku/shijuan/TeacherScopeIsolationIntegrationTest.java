package com.neu.riketiku.shijuan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Direct service-level guard for the final 203 teaching-scope isolation scenario. */
@SpringBootTest
class TeacherScopeIsolationIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private PaperService papers;
    @Autowired private PaperAssignmentService assignments;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @Transactional
    void teacher203CannotQueryOrPublishThrough199ScopeAndPrivateQuestionsStaySeparated() {
        long zhang203 = teacher("张生康203", "T203");
        long teacher199 = teacher("199范围教师", "T199");
        long class203 = classRoom("CLASS_203_SCOPE", "203班");
        long class199 = classRoom("CLASS_199_SCOPE", "199班");
        long physics = jdbc.queryForObject("SELECT id FROM ke_mu WHERE ke_mu_dai_ma='PHYSICS'", Long.class);
        long scope203 = scope(zhang203, class203, physics);
        long scope199 = scope(teacher199, class199, physics);
        long global = question(physics, "GLOBAL", null, null, "203范围全局题");
        long private203 = question(physics, "TEACHING_SCOPE_PRIVATE", scope203, zhang203, "203私有题");
        long private199 = question(physics, "TEACHING_SCOPE_PRIVATE", scope199, teacher199, "199私有题");

        assertThat(papers.questions(zhang203, physics, scope203, null, "SINGLE_CHOICE", null, null))
                .extracting(PaperDtos.QuestionOption::id).contains(global, private203).doesNotContain(private199);
        assertThatThrownBy(() -> papers.questions(zhang203, physics, scope199, null, "SINGLE_CHOICE", null, null))
                .isInstanceOfSatisfying(RenZhengYeWuYiChang.class,
                        error -> assertThat(error.getCode()).isEqualTo("PAPER_SCOPE_FORBIDDEN"));

        PaperDtos.Paper paper = papers.save(zhang203, new PaperDtos.Save(physics, "203隔离试卷", "MANUAL",
                List.of(new PaperDtos.ItemInput(private203, new BigDecimal("10")))));
        assertThatThrownBy(() -> assignments.publish(zhang203, paper.id(),
                new PaperAssignmentDtos.Publish(scope199, LocalDateTime.now().plusHours(1))))
                .isInstanceOfSatisfying(RenZhengYeWuYiChang.class,
                        error -> assertThat(error.getCode()).isEqualTo("PAPER_SCOPE_FORBIDDEN"));
    }

    private long teacher(String name, String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao) VALUES (?,?)", prefix.toLowerCase() + suffix, "x".repeat(60));
        long userId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO jiao_shi_dang_an(yong_hu_id,gong_hao,xing_ming) VALUES (?,?,?)", userId, prefix + suffix, name);
        return userId;
    }

    private long classRoom(String code, String name) {
        jdbc.update("INSERT INTO ban_ji(ban_ji_bian_ma,ban_ji_ming_cheng,nian_ji,ru_xue_nian_fen) VALUES (?,?, '高三',2023)", code, name);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long scope(long teacherUserId, long classId, long subjectId) {
        long teacherId = jdbc.queryForObject("SELECT id FROM jiao_shi_dang_an WHERE yong_hu_id=?", Long.class, teacherUserId);
        jdbc.update("INSERT INTO ren_ke_guan_xi(jiao_shi_id,ban_ji_id,ke_mu_id,zhuang_tai,kai_shi_shi_jian) VALUES (?,?,?,'ACTIVE',CURRENT_TIMESTAMP(3))", teacherId, classId, subjectId);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long question(long subjectId, String visibility, Long scopeId, Long creatorId, String stem) {
        String hash = UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO ti_mu(ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi,ke_jian_fan_wei,ren_ke_guan_xi_id,chuang_jian_ren_id) VALUES (?,'SINGLE_CHOICE','ONLINE_PRACTICE',?,JSON_OBJECT('schemaVersion',1,'type','SINGLE_CHOICE','optionLabels',JSON_ARRAY('A')),1,1,'PUBLISHED',?,?,?,?)",
                subjectId, stem, hash, visibility, scopeId, creatorId);
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO ti_mu_xuan_xiang(ti_mu_id,xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que,pai_xu) VALUES (?,'A','正确',1,1),(?,'B','错误',0,2)", id, id);
        jdbc.update("INSERT INTO ti_mu_jie_xi(ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (?,'STANDARD','STANDARD',1,'PUBLISHED')", id);
        return id;
    }
}
