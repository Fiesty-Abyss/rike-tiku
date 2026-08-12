package com.neu.riketiku.jiaoshi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import com.neu.riketiku.jiaoshi.dto.JiaoShiChuangJianQingQiu;
import com.neu.riketiku.jiaoshi.dto.JiaoShiChuangJianXiangYing;
import com.neu.riketiku.jiaoshi.dto.JiaoShiXiuGaiQingQiu;
import com.neu.riketiku.jiaoshi.dto.RenKeChuangJianQingQiu;
import com.neu.riketiku.jiaoshi.dto.RenKeZhuangTaiQingQiu;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class JiaoShiGuanLiFuWuTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private JiaoShiGuanLiFuWu service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test @Transactional
    void createTeacherShouldWriteUserRoleProfileAndReturnInitialPasswordOnce() {
        String suffix = suffix();
        JiaoShiChuangJianXiangYing response = service.create(new JiaoShiChuangJianQingQiu("T" + suffix, "教师甲", "teacher_" + suffix, "物理教师", null, "ENABLED"));
        assertThat(response.initialPassword()).matches("(?=.*[A-Za-z])(?=.*[0-9]).{8,64}");
        Long userId = jdbc.queryForObject("SELECT yong_hu_id FROM jiao_shi_dang_an WHERE id=?", Long.class, response.teacher().id());
        String hash = jdbc.queryForObject("SELECT mi_ma_zhai_yao FROM yong_hu WHERE id=?", String.class, userId);
        assertThat(hash).doesNotContain(response.initialPassword());
        assertThat(passwordEncoder.matches(response.initialPassword(), hash)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM yong_hu_jiao_se ur JOIN jiao_se r ON r.id=ur.jiao_se_id WHERE ur.yong_hu_id=? AND r.jiao_se_dai_ma='TEACHER' AND ur.zhuang_tai='ACTIVE'", Integer.class, userId)).isEqualTo(1);
        assertThatThrownBy(() -> service.create(new JiaoShiChuangJianQingQiu("T" + suffix, "教师乙", "other_" + suffix, null, "Password1", "ENABLED"))).isInstanceOf(RenZhengYeWuYiChang.class).hasMessage("工号已存在");
    }

    @Test @Transactional
    void teacherUpdateMustKeepEmployeeNumberUsernameAndRolesOutsideRequestBoundary() {
        String suffix = suffix();
        var created = service.create(new JiaoShiChuangJianQingQiu("T" + suffix, "教师甲", "teacher_" + suffix, null, "Password1", "ENABLED"));
        var updated = service.update(created.teacher().id(), new JiaoShiXiuGaiQingQiu("教师甲改", "教研组长", "DISABLED", "DISABLED"));
        assertThat(updated.employeeNumber()).isEqualTo("T" + suffix);
        assertThat(updated.username()).isEqualTo("teacher_" + suffix);
        assertThat(updated.accountStatus()).isEqualTo("DISABLED");
        assertThat(updated.profileStatus()).isEqualTo("DISABLED");
    }

    @Test @Transactional
    void adminResetTeacherPasswordInvalidatesOldPasswordAndAuditsWithoutPlaintext() {
        String suffix = suffix();
        var created = service.create(new JiaoShiChuangJianQingQiu("T" + suffix, "教师甲", "teacher_" + suffix, null, "OldPassword1", "ENABLED"));
        Long userId = jdbc.queryForObject("SELECT yong_hu_id FROM jiao_shi_dang_an WHERE id=?", Long.class, created.teacher().id());

        var response = service.resetPassword(created.teacher().id());
        String hash = jdbc.queryForObject("SELECT mi_ma_zhai_yao FROM yong_hu WHERE id=?", String.class, userId);

        assertThat(response.initialPassword()).isEqualTo("a1234567");
        assertThat(response.resetCount()).isEqualTo(1);
        assertThat(response.mustChangePassword()).isTrue();
        assertThat(passwordEncoder.matches("OldPassword1", hash)).isFalse();
        assertThat(passwordEncoder.matches(response.initialPassword(), hash)).isTrue();
        assertThat(jdbc.queryForObject("SELECT shi_fou_shou_ci_deng_lu FROM yong_hu WHERE id=?", Boolean.class, userId)).isTrue();
        String audit = jdbc.queryForObject("""
                SELECT CONCAT(cao_zuo_lei_xing,'|',COALESCE(zhai_yao,'')) FROM guan_li_cao_zuo_ri_zhi
                WHERE mo_kuai='TEACHER' AND ye_wu_dui_xiang_id=? ORDER BY id DESC LIMIT 1
                """, String.class, created.teacher().id());
        assertThat(audit).contains("RESET_PASSWORD").doesNotContain(response.initialPassword(), "OldPassword1");
    }

    @Test @Transactional
    void batchResetTeacherPasswordsUsesDistinctBcryptAndAuditsOnlySafeMetadata() {
        String suffix = suffix();
        var first = service.create(new JiaoShiChuangJianQingQiu("TA" + suffix, "教师甲", "teacher_a_" + suffix, null, "OldPassword1", "ENABLED"));
        var second = service.create(new JiaoShiChuangJianQingQiu("TB" + suffix, "教师乙", "teacher_b_" + suffix, null, "OldPassword2", "ENABLED"));

        var response = service.resetPasswords(List.of(first.teacher().id(), second.teacher().id(), first.teacher().id()));
        List<String> hashes = jdbc.queryForList("""
                SELECT u.mi_ma_zhai_yao FROM yong_hu u JOIN jiao_shi_dang_an p ON p.yong_hu_id=u.id
                WHERE p.id IN (?,?) ORDER BY p.id
                """, String.class, first.teacher().id(), second.teacher().id());

        assertThat(response.resetCount()).isEqualTo(2);
        assertThat(response.initialPassword()).isEqualTo("a1234567");
        assertThat(response.mustChangePassword()).isTrue();
        assertThat(hashes).hasSize(2).doesNotHaveDuplicates();
        assertThat(hashes).allMatch(hash -> passwordEncoder.matches(response.initialPassword(), hash));
        String audit = jdbc.queryForObject("""
                SELECT CONCAT(cao_zuo_lei_xing,'|',COALESCE(zhai_yao,'')) FROM guan_li_cao_zuo_ri_zhi
                WHERE mo_kuai='TEACHER' AND cao_zuo_lei_xing='BATCH_RESET_PASSWORD' ORDER BY id DESC LIMIT 1
                """, String.class);
        assertThat(audit).contains("BATCH_RESET_PASSWORD", "数量=2", String.valueOf(first.teacher().id()), String.valueOf(second.teacher().id()))
                .doesNotContain(response.initialPassword(), "OldPassword1", "OldPassword2", first.teacher().name(), second.teacher().name());
    }

    @Test @Transactional
    void batchResetTeacherPasswordsIsAtomicWhenAnyTargetDoesNotExist() {
        String suffix = suffix();
        var teacher = service.create(new JiaoShiChuangJianQingQiu("TC" + suffix, "教师甲", "teacher_c_" + suffix, null, "OldPassword1", "ENABLED"));
        Long userId = jdbc.queryForObject("SELECT yong_hu_id FROM jiao_shi_dang_an WHERE id=?", Long.class, teacher.teacher().id());
        String before = jdbc.queryForObject("SELECT mi_ma_zhai_yao FROM yong_hu WHERE id=?", String.class, userId);

        assertThatThrownBy(() -> service.resetPasswords(List.of(teacher.teacher().id(), 999999999L)))
                .isInstanceOf(RenZhengYeWuYiChang.class).hasMessage("教师不存在");

        assertThat(jdbc.queryForObject("SELECT mi_ma_zhai_yao FROM yong_hu WHERE id=?", String.class, userId)).isEqualTo(before);
        assertThat(passwordEncoder.matches("OldPassword1", before)).isTrue();
    }

    @Test @Transactional
    void assignmentShouldRejectDuplicateAndKeepEndedHistory() {
        String suffix = suffix();
        var teacher = service.create(new JiaoShiChuangJianQingQiu("T" + suffix, "教师甲", "teacher_" + suffix, null, "Password1", "ENABLED")).teacher();
        jdbc.update("INSERT INTO ban_ji(ban_ji_bian_ma,ban_ji_ming_cheng,nian_ji,ru_xue_nian_fen,zhuang_tai) VALUES (?,?, '高一',2026,'ACTIVE')", "C" + suffix, "测试班");
        Long classId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        var relation = service.createAssignment(teacher.id(), new RenKeChuangJianQingQiu(classId, 1L, true, LocalDateTime.now()));
        assertThatThrownBy(() -> service.createAssignment(teacher.id(), new RenKeChuangJianQingQiu(classId, 1L, false, LocalDateTime.now()))).isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("已存在");
        var ended = service.changeAssignmentStatus(relation.id(), new RenKeZhuangTaiQingQiu("ENDED"));
        assertThat(ended.status()).isEqualTo("ENDED"); assertThat(ended.endTime()).isNotNull();
        assertThat(service.assignments(teacher.id())).hasSize(1);
    }

    private String suffix() { return UUID.randomUUID().toString().replace("-", "").substring(0, 10); }
}
