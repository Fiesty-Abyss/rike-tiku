package com.neu.riketiku.zhanghao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.zhanghao.entity.YongHu;
import com.neu.riketiku.zhanghao.mapper.YongHuMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class UserTeachingDatabaseModelTest {
    private static final Set<String> BUSINESS_TABLES = Set.of(
        "ke_mu", "zhi_shi_dian", "dao_ru_pi_ci", "ti_mu", "ti_mu_xuan_xiang",
        "ti_mu_jie_xi", "ti_mu_zhi_shi_dian", "ti_mu_fu_jian", "ti_mu_lai_yuan",
        "ti_mu_shen_he_ji_lu", "yong_hu", "jiao_se", "yong_hu_jiao_se",
        "xue_sheng_dang_an", "jiao_shi_dang_an", "ban_ji", "ban_ji_xue_sheng",
        "ren_ke_guan_xi"
    );

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private YongHuMapper yongHuMapper;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private Environment environment;

    @Test
    void migrationsShouldPreserveQuestionModelAndCreateApprovedTables() {
        Set<String> actualTables = Set.copyOf(jdbcTemplate.queryForList("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_type = 'BASE TABLE'
              AND table_name <> 'flyway_schema_history'
            """, String.class));
        assertThat(actualTables).isEqualTo(BUSINESS_TABLES);

        Integer latestVersion = jdbcTemplate.queryForObject(
            "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success=1", Integer.class);
        Integer questionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ti_mu", Integer.class);
        Integer pendingCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ti_mu WHERE zhuang_tai='PENDING'", Integer.class);
        Integer roleCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM jiao_se WHERE jiao_se_dai_ma IN ('STUDENT','TEACHER','ADMIN')", Integer.class);

        assertThat(latestVersion).isEqualTo(6);
        assertThat(questionCount).isEqualTo(3);
        assertThat(pendingCount).isEqualTo(3);
        assertThat(roleCount).isEqualTo(3);
    }

    @Test
    @Transactional
    void accountAndRoleConstraintsShouldSupportMultipleRolesWithoutDuplicates() {
        String suffix = suffix();
        long userId = insertUser("student_" + suffix);
        long otherUserId = insertUser("other_" + suffix);
        long studentRoleId = roleId("STUDENT");
        long adminRoleId = roleId("ADMIN");

        jdbcTemplate.update(
            "INSERT INTO yong_hu_jiao_se(yong_hu_id,jiao_se_id) VALUES (?,?)", userId, studentRoleId);
        jdbcTemplate.update(
            "INSERT INTO yong_hu_jiao_se(yong_hu_id,jiao_se_id) VALUES (?,?)", userId, adminRoleId);
        Integer roleCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM yong_hu_jiao_se WHERE yong_hu_id=?", Integer.class, userId);
        assertThat(roleCount).isEqualTo(2);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO yong_hu_jiao_se(yong_hu_id,jiao_se_id) VALUES (?,?)", userId, studentRoleId))
            .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao) SELECT yong_hu_ming,? FROM yong_hu WHERE id=?",
            encodedPassword(), userId))
            .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO jiao_se(jiao_se_dai_ma,jiao_se_ming_cheng) VALUES ('STUDENT','重复学生角色')"))
            .isInstanceOf(DataAccessException.class);

        assertThat(otherUserId).isPositive();
    }

    @Test
    @Transactional
    void profileConstraintsShouldEnforceOneProfileAndUniqueNumbers() {
        String suffix = suffix();
        long studentUser = insertUser("student_profile_" + suffix);
        long otherStudentUser = insertUser("student_other_" + suffix);
        long teacherUser = insertUser("teacher_profile_" + suffix);
        long otherTeacherUser = insertUser("teacher_other_" + suffix);

        insertStudentProfile(studentUser, "S" + suffix, "学生甲");
        assertThatThrownBy(() -> insertStudentProfile(studentUser, "S2" + suffix, "学生甲"))
            .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertStudentProfile(otherStudentUser, "S" + suffix, "学生甲"))
            .isInstanceOf(DataAccessException.class);

        insertTeacherProfile(teacherUser, "T" + suffix, "教师甲");
        assertThatThrownBy(() -> insertTeacherProfile(teacherUser, "T2" + suffix, "教师甲"))
            .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertTeacherProfile(otherTeacherUser, "T" + suffix, "教师乙"))
            .isInstanceOf(DataAccessException.class);
    }

    @Test
    @Transactional
    void classMembershipShouldRejectDuplicatesAndKeepHistoryWithOneActiveMainClass() {
        String suffix = suffix();
        long studentId = insertStudentProfile(insertUser("class_student_" + suffix), "SC" + suffix, "学生甲");
        long classOne = insertClass("C1" + suffix, "测试一班");
        long classTwo = insertClass("C2" + suffix, "测试二班");

        long firstRelation = insertClassMembership(classOne, studentId, true);
        assertThatThrownBy(() -> insertClassMembership(classOne, studentId, false))
            .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertClassMembership(classTwo, studentId, true))
            .isInstanceOf(DataAccessException.class);

        jdbcTemplate.update("""
            UPDATE ban_ji_xue_sheng
            SET zhuang_tai='EXITED', tui_chu_shi_jian=GREATEST(CURRENT_TIMESTAMP(3), jia_ru_shi_jian)
            WHERE id=?
            """, firstRelation);
        insertClassMembership(classTwo, studentId, true);

        Integer historyCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ban_ji_xue_sheng WHERE xue_sheng_id=?", Integer.class, studentId);
        Integer activeMainCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM ban_ji_xue_sheng
            WHERE xue_sheng_id=? AND shi_fou_zhu_ban_ji=1 AND zhuang_tai='ACTIVE'
            """, Integer.class, studentId);
        assertThat(historyCount).isEqualTo(2);
        assertThat(activeMainCount).isEqualTo(1);

        assertThatThrownBy(() -> insertClass("C1" + suffix, "重复编码班级"))
            .isInstanceOf(DataAccessException.class);
    }

    @Test
    @Transactional
    void teachingRelationshipShouldUseUniqueTeacherClassSubjectTriples() {
        String suffix = suffix();
        long teacherOne = insertTeacherProfile(insertUser("teacher_one_" + suffix), "TA" + suffix, "教师甲");
        long teacherTwo = insertTeacherProfile(insertUser("teacher_two_" + suffix), "TB" + suffix, "教师乙");
        long classOne = insertClass("TC1" + suffix, "任课一班");
        long classTwo = insertClass("TC2" + suffix, "任课二班");

        insertTeaching(teacherOne, classOne, 1L);
        insertTeaching(teacherOne, classTwo, 1L);
        insertTeaching(teacherOne, classOne, 2L);
        assertThatThrownBy(() -> insertTeaching(teacherOne, classOne, 1L))
            .isInstanceOf(DataAccessException.class);

        Integer teacherOneCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ren_ke_guan_xi WHERE jiao_shi_id=?", Integer.class, teacherOne);
        Integer teacherTwoCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ren_ke_guan_xi WHERE jiao_shi_id=?", Integer.class, teacherTwo);
        assertThat(teacherOneCount).isEqualTo(3);
        assertThat(teacherTwoCount).isZero();
    }

    @Test
    @Transactional
    void myBatisPlusShouldAutoFillAndLogicallyDeleteWhileReviewHistoryRemains() {
        YongHu user = new YongHu();
        user.setYongHuMing("mapper_" + suffix());
        user.setMiMaZhaiYao(encodedPassword());
        user.setZhangHaoZhuangTai("ENABLED");
        user.setShiFouShouCiDengLu(true);
        assertThat(yongHuMapper.insert(user)).isEqualTo(1);
        assertThat(user.getId()).isPositive();
        assertThat(user.getChuangJianShiJian()).isNotNull();
        assertThat(user.getGengXinShiJian()).isNotNull();

        int reviewRows = jdbcTemplate.update("""
            INSERT INTO ti_mu_shen_he_ji_lu(
                ti_mu_id,shen_he_dong_zuo,yuan_zhuang_tai,mu_biao_zhuang_tai,shen_he_ren_id,shen_he_yi_jian
            ) VALUES (1,'SUBMITTED','PENDING','PENDING',?,'外键验证')
            """, user.getId());
        assertThat(reviewRows).isEqualTo(1);
        assertThatThrownBy(() -> jdbcTemplate.update("""
            INSERT INTO ti_mu_shen_he_ji_lu(
                ti_mu_id,shen_he_dong_zuo,yuan_zhuang_tai,mu_biao_zhuang_tai,shen_he_ren_id
            ) VALUES (1,'SUBMITTED','PENDING','PENDING',-1)
            """))
            .isInstanceOf(DataAccessException.class);

        assertThat(yongHuMapper.deleteById(user.getId())).isEqualTo(1);
        assertThat(yongHuMapper.selectById(user.getId())).isNull();
        Integer deletedFlag = jdbcTemplate.queryForObject(
            "SELECT yi_shan_chu FROM yong_hu WHERE id=?", Integer.class, user.getId());
        Integer reviewCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM ti_mu_shen_he_ji_lu WHERE shen_he_ren_id=?", Integer.class, user.getId());
        assertThat(deletedFlag).isEqualTo(1);
        assertThat(reviewCount).isEqualTo(1);
    }

    @Test
    void transactionShouldRollBackAllAccountWritesOnFailure() {
        String username = "rollback_" + suffix();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            insertUser(username);
            throw new IllegalStateException("test rollback");
        })).isInstanceOf(IllegalStateException.class);

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming=?", Integer.class, username);
        assertThat(count).isZero();
    }

    @Test
    void emptyDatabaseShouldMigrateFromV1ToV6() throws Exception {
        String configuredUrl = environment.getRequiredProperty("spring.datasource.url");
        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");
        String schema = "rike_tiku_migration_test_" + UUID.randomUUID().toString().replace("-", "");
        assertThat(schema).matches("[a-z0-9_]+");

        int databaseSlash = configuredUrl.indexOf('/', "jdbc:mysql://".length());
        String query = configuredUrl.contains("?") ? configuredUrl.substring(configuredUrl.indexOf('?')) : "";
        String serverPrefix = configuredUrl.substring(0, databaseSlash + 1);
        String adminUrl = serverPrefix + "mysql" + query;
        String testUrl = serverPrefix + schema + query;

        try (Connection admin = DriverManager.getConnection(adminUrl, username, password);
             Statement statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }

        try {
            Flyway flyway = Flyway.configure()
                .dataSource(testUrl, username, password)
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load();
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(6);

            try (Connection connection = DriverManager.getConnection(testUrl, username, password);
                 Statement statement = connection.createStatement()) {
                assertThat(singleInt(statement,
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='" + schema
                        + "' AND table_name <> 'flyway_schema_history'"))
                    .isEqualTo(18);
                assertThat(singleInt(statement, "SELECT COUNT(*) FROM ti_mu")).isEqualTo(3);
                assertThat(singleInt(statement,
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success=1")).isEqualTo(6);
            }
        } finally {
            try (Connection admin = DriverManager.getConnection(adminUrl, username, password);
                 Statement statement = admin.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS `" + schema + "`");
            }
        }
    }

    private long insertUser(String username) {
        jdbcTemplate.update("""
            INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,zhang_hao_zhuang_tai,shi_fou_shou_ci_deng_lu)
            VALUES (?,?,'ENABLED',1)
            """, username, encodedPassword());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long insertStudentProfile(long userId, String studentNumber, String name) {
        jdbcTemplate.update("""
            INSERT INTO xue_sheng_dang_an(yong_hu_id,xue_hao,xing_ming,nian_ji)
            VALUES (?,?,?,'高一')
            """, userId, studentNumber, name);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long insertTeacherProfile(long userId, String teacherNumber, String name) {
        jdbcTemplate.update("""
            INSERT INTO jiao_shi_dang_an(yong_hu_id,gong_hao,xing_ming,xian_shi_zhi_wu)
            VALUES (?,?,?,'任课教师')
            """, userId, teacherNumber, name);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long insertClass(String code, String name) {
        jdbcTemplate.update("""
            INSERT INTO ban_ji(ban_ji_bian_ma,ban_ji_ming_cheng,nian_ji,ru_xue_nian_fen)
            VALUES (?,?, '高一', 2026)
            """, code, name);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long insertClassMembership(long classId, long studentId, boolean mainClass) {
        jdbcTemplate.update("""
            INSERT INTO ban_ji_xue_sheng(ban_ji_id,xue_sheng_id,shi_fou_zhu_ban_ji,zhuang_tai)
            VALUES (?,?,?,'ACTIVE')
            """, classId, studentId, mainClass);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void insertTeaching(long teacherId, long classId, long subjectId) {
        jdbcTemplate.update("""
            INSERT INTO ren_ke_guan_xi(
                jiao_shi_id,ban_ji_id,ke_mu_id,shi_fou_zhu_ren_ke,zhuang_tai,kai_shi_shi_jian
            ) VALUES (?,?,?,0,'ACTIVE',CURRENT_TIMESTAMP(3))
            """, teacherId, classId, subjectId);
    }

    private long roleId(String code) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM jiao_se WHERE jiao_se_dai_ma=?", Long.class, code);
    }

    private String encodedPassword() {
        return passwordEncoder.encode("test-only-" + UUID.randomUUID());
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private int singleInt(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getInt(1);
        }
    }
}
