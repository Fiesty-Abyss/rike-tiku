package com.neu.riketiku.portal;

import static org.assertj.core.api.Assertions.assertThat;

import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class PortalStatsServiceIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private PortalStatsService service;
    @Autowired private JdbcTemplate jdbc;
    private Long privateAssignmentId;
    private Long privateCreatorId;

    @Test
    @Transactional
    void countsOnlyPublicPublishedQuestionsThatMatchTheirActualLearningModes() {
        PortalStats before = service.current();

        insert("SINGLE_CHOICE", "ONLINE_PRACTICE", 1, "PUBLISHED", "GLOBAL", 0);
        insert("SINGLE_CHOICE", "ONLINE_PRACTICE", 1, "PENDING", "GLOBAL", 0);
        insert("SINGLE_CHOICE", "ONLINE_PRACTICE", 1, "PUBLISHED", "TEACHING_SCOPE_PRIVATE", 0);
        insert("SUBJECTIVE", "TOPIC_LEARNING", 0, "PUBLISHED", "GLOBAL", 0);
        insert("SUBJECTIVE", "TOPIC_LEARNING", 0, "PENDING", "GLOBAL", 0);
        insert("SUBJECTIVE", "TOPIC_LEARNING", 0, "PUBLISHED", "GLOBAL", 1);

        PortalStats after = service.current();
        assertThat(after.subjectCount()).isEqualTo(before.subjectCount());
        assertThat(after.automaticPracticeQuestionCount()).isEqualTo(before.automaticPracticeQuestionCount() + 1);
        assertThat(after.topicQuestionCount()).isEqualTo(before.topicQuestionCount() + 1);
    }

    private void insert(String type, String mode, int autoGradable, String status, String visibility, int deleted) {
        String suffix = UUID.randomUUID().toString();
        Long assignmentId = "TEACHING_SCOPE_PRIVATE".equals(visibility) ? privateAssignment() : null;
        Long creatorId = assignmentId == null ? null : privateCreatorId;
        jdbc.update("""
                INSERT INTO ti_mu(ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,
                                  shi_fou_ke_zi_dong_pan_fen,zhuang_tai,ke_jian_fan_wei,ren_ke_guan_xi_id,chuang_jian_ren_id,
                                  nei_rong_ha_xi,yi_shan_chu)
                VALUES (1,?,?,?,CAST(? AS JSON),1,?,?,?,?,?,?,?)
                """, type, mode, "门户统计隔离题 " + suffix, "{\"type\":\"" + type + "\"}", autoGradable, status,
                visibility, assignmentId, creatorId, hash(suffix), deleted);
    }

    private String hash(String value) {
        return (value.replace("-", "") + "0".repeat(64)).substring(0, 64);
    }

    private long privateAssignment() {
        if (privateAssignmentId != null) return privateAssignmentId;
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,shi_fou_shou_ci_deng_lu) VALUES (?,?,0)",
                "portal_scope_" + suffix, "x".repeat(60));
        privateCreatorId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO jiao_shi_dang_an(yong_hu_id,gong_hao,xing_ming) VALUES (?,?,?)", privateCreatorId,
                "PORTAL_" + suffix, "统计范围教师");
        long teacherId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO ban_ji(ban_ji_bian_ma,ban_ji_ming_cheng,nian_ji,ru_xue_nian_fen,zhuang_tai) VALUES (?,?, '高三',2023,'ACTIVE')",
                "PORTAL_" + suffix, "统计范围班级");
        long classId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO ren_ke_guan_xi(jiao_shi_id,ban_ji_id,ke_mu_id,zhuang_tai,kai_shi_shi_jian) VALUES (?,?,1,'ACTIVE',CURRENT_TIMESTAMP)",
                teacherId, classId);
        privateAssignmentId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return privateAssignmentId;
    }
}
