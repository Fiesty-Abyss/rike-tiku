package com.neu.riketiku.jiaoshi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.jiaoshi.dto.JiaoShiChuangJianQingQiu;
import com.neu.riketiku.jiaoshi.dto.JiaoShiChuangJianXiangYing;
import com.neu.riketiku.jiaoshi.dto.JiaoShiXiuGaiQingQiu;
import com.neu.riketiku.jiaoshi.dto.RenKeChuangJianQingQiu;
import com.neu.riketiku.jiaoshi.dto.RenKeZhuangTaiQingQiu;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class JiaoShiGuanLiFuWuTest {
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
