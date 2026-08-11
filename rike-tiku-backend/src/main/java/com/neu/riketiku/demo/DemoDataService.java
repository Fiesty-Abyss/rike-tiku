package com.neu.riketiku.demo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import com.neu.riketiku.tiku.admin.QuestionContentHashService;
import com.neu.riketiku.tiku.admin.QuestionContentHashService.OptionContent;
import com.neu.riketiku.tiku.fujian.QuestionAttachmentStorage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoDataService {
    public static final String DEMO_PASSWORD = "a1234567";
    public static final String RIGHTS_BASIS = "本科毕业设计项目原创自编演示题，仅用于本地验收环境。";
    public static final int BASE_DEMO_QUESTION_COUNT = 90;
    public static final int FINAL_DEMO_QUESTION_COUNT = BASE_DEMO_QUESTION_COUNT
            + DemoVariantQuestionBank.ACCEPTED_COUNT + DemoCurriculumQuestionBank.TOTAL_COUNT;
    public static final int TOTAL_DEMO_QUESTION_COUNT = FINAL_DEMO_QUESTION_COUNT + DemoTopicQuestionBank.TOTAL_COUNT;
    private static final Set<String> FORBIDDEN_DATABASES = Set.of(
            "rike_tiku", "mysql", "information_schema", "performance_schema", "sys");
    private static final String DEMO_STEM_PREFIX = "【演示】";
    private static final String TOPIC_STEM_PREFIX = "【专题演示】";
    private static final List<String> DEMO_USERS = List.of(
            "demo_admin", "demo_teacher", "demo_student",
            "demo_physics_admin", "demo_biology_teacher", "demo_chemistry_teacher",
            "demo_199_01", "demo_199_02", "demo_199_03", "demo_199_04", "demo_199_05",
            "demo_200_01", "demo_200_02", "demo_200_03");

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final QuestionAttachmentStorage attachmentStorage;
    private final QuestionContentHashService contentHashService;

    public DemoDataService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder,
            QuestionAttachmentStorage attachmentStorage, QuestionContentHashService contentHashService) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.attachmentStorage = attachmentStorage;
        this.contentHashService = contentHashService;
    }

    public void validateSchema() {
        String database = currentDatabase();
        guardDatabaseName(database);
        int version = jdbc.queryForObject("SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success=1", Integer.class);
        int tableCount = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name<>'flyway_schema_history'", Integer.class);
        if (version != 14 || tableCount != 35) {
            throw new IllegalStateException("演示库必须完整执行V1-V14且包含35张业务表，当前V" + version + "，" + tableCount + "张");
        }
        expect("管理员操作日志表", 1, count("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='guan_li_cao_zuo_ri_zhi'"));
        System.out.println("演示数据库结构校验通过: " + database + "，V1-V14，35张业务表");
    }

    @Transactional
    public void seed() {
        validateSchema();
        cleanInternal();
        Map<String, Long> users = seedUsers();
        long teacherId = seedTeacher(users.get("demo_teacher"), "DEMO_T001", "演示教师", "理综演示教师");
        long studentId = seedStudent(users.get("demo_student"), "DEMO_S001", "演示学生");
        long classId = seedClass("DEMO_CLASS_01", "高三理综演示班");
        jdbc.update("INSERT INTO ban_ji_xue_sheng (ban_ji_id,xue_sheng_id,shi_fou_zhu_ban_ji,zhuang_tai) VALUES (?,?,1,'ACTIVE')", classId, studentId);

        long class199 = seedClass("DEMO_CLASS_199", "199班");
        long class200 = seedClass("DEMO_CLASS_200", "200班");
        long physicsTeacher = seedTeacher(users.get("demo_physics_admin"), "DEMO_T_PHYSICS", "物理管理员教师", "物理教师");
        long biologyTeacher = seedTeacher(users.get("demo_biology_teacher"), "DEMO_T_BIOLOGY", "生物教师", "生物教师");
        long chemistryTeacher = seedTeacher(users.get("demo_chemistry_teacher"), "DEMO_T_CHEMISTRY", "化学教师", "化学教师");
        for (int index = 1; index <= 5; index++) {
            long fixedStudent = seedStudent(users.get("demo_199_0" + index), "DEMO_199_0" + index, "199班学生0" + index);
            jdbc.update("INSERT INTO ban_ji_xue_sheng (ban_ji_id,xue_sheng_id,shi_fou_zhu_ban_ji,zhuang_tai) VALUES (?,?,1,'ACTIVE')", class199, fixedStudent);
        }
        for (int index = 1; index <= 3; index++) {
            long fixedStudent = seedStudent(users.get("demo_200_0" + index), "DEMO_200_0" + index, "200班学生0" + index);
            jdbc.update("INSERT INTO ban_ji_xue_sheng (ban_ji_id,xue_sheng_id,shi_fou_zhu_ban_ji,zhuang_tai) VALUES (?,?,1,'ACTIVE')", class200, fixedStudent);
        }

        Map<String, Long> subjects = jdbc.query("SELECT ke_mu_dai_ma,id FROM ke_mu WHERE yi_shan_chu=0", rs -> {
            var result = new java.util.HashMap<String, Long>();
            while (rs.next()) result.put(rs.getString(1), rs.getLong(2));
            return result;
        });
        for (String code : List.of("PHYSICS", "CHEMISTRY", "BIOLOGY")) {
            jdbc.update("INSERT INTO ren_ke_guan_xi (jiao_shi_id,ban_ji_id,ke_mu_id,shi_fou_zhu_ren_ke,zhuang_tai,kai_shi_shi_jian) VALUES (?,?,?,1,'ACTIVE',CURRENT_TIMESTAMP(3))",
                    teacherId, classId, required(subjects, code));
        }
        Map<String, Long> scopes = new java.util.HashMap<>();
        scopes.put("PHYSICS_199", seedTeachingScope(physicsTeacher, class199, required(subjects, "PHYSICS")));
        scopes.put("PHYSICS_200", seedTeachingScope(physicsTeacher, class200, required(subjects, "PHYSICS")));
        scopes.put("BIOLOGY_199", seedTeachingScope(biologyTeacher, class199, required(subjects, "BIOLOGY")));
        scopes.put("BIOLOGY_200", seedTeachingScope(biologyTeacher, class200, required(subjects, "BIOLOGY")));
        scopes.put("CHEMISTRY_199", seedTeachingScope(chemistryTeacher, class199, required(subjects, "CHEMISTRY")));
        scopes.put("CHEMISTRY_200", seedTeachingScope(chemistryTeacher, class200, required(subjects, "CHEMISTRY")));
        Map<String, Long> points = seedKnowledgePoints(subjects);
        for (Question question : questions()) seedQuestion(question, required(subjects, question.subject()), required(points, question.knowledgePath()), users.get("demo_admin"));
        for (DemoTopicQuestionBank.TopicQuestion question : DemoTopicQuestionBank.questions()) {
            seedTopicQuestion(question, required(subjects, question.subject()), required(points, question.knowledgePath()), users.get("demo_admin"));
        }
        seedHighFrequencyPoints(scopes, points);
        validateSeed();
        System.out.println("演示数据写入完成。14个固定演示账号、3个班级、4位教师、9名学生；固定密码: " + DEMO_PASSWORD + "（仅限本地演示库）");
    }

    @Transactional
    public void clean() {
        validateSchema();
        cleanInternal();
        System.out.println("演示数据已清理，Flyway基础数据及非演示数据保持不变: " + currentDatabase());
    }

    public void validateSeed() {
        validateSchema();
        expect("三个学科", 3, count("SELECT COUNT(*) FROM ke_mu WHERE ke_mu_dai_ma IN ('PHYSICS','CHEMISTRY','BIOLOGY') AND zhuang_tai='ACTIVE' AND yi_shan_chu=0"));
        expect("三个演示账号", 3, count("SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming IN ('demo_admin','demo_teacher','demo_student') AND yi_shan_chu=0"));
        expect("最终验收四账号", 4, count("SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming IN ('demo_admin','demo_199_01','demo_teacher','demo_physics_admin') AND zhang_hao_zhuang_tai='ENABLED' AND yi_shan_chu=0"));
        expect("三个角色", 3, count("SELECT COUNT(DISTINCT r.jiao_se_dai_ma) FROM yong_hu u JOIN yong_hu_jiao_se ur ON ur.yong_hu_id=u.id AND ur.zhuang_tai='ACTIVE' JOIN jiao_se r ON r.id=ur.jiao_se_id WHERE u.yong_hu_ming IN ('demo_admin','demo_teacher','demo_student')"));
        for (String username : DEMO_USERS) {
            String digest = jdbc.queryForObject("SELECT mi_ma_zhai_yao FROM yong_hu WHERE yong_hu_ming=?", String.class, username);
            if (digest == null || digest.contains(DEMO_PASSWORD) || !passwordEncoder.matches(DEMO_PASSWORD, digest)) throw new IllegalStateException(username + " 的BCrypt密码校验失败");
        }
        expect("固定演示账号", 14, count("SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming IN ('demo_admin','demo_teacher','demo_student','demo_physics_admin','demo_biology_teacher','demo_chemistry_teacher','demo_199_01','demo_199_02','demo_199_03','demo_199_04','demo_199_05','demo_200_01','demo_200_02','demo_200_03') AND yi_shan_chu=0"));
        expect("物理管理员双角色", 2, count("SELECT COUNT(*) FROM yong_hu u JOIN yong_hu_jiao_se ur ON ur.yong_hu_id=u.id AND ur.zhuang_tai='ACTIVE' JOIN jiao_se r ON r.id=ur.jiao_se_id AND r.zhuang_tai='ACTIVE' WHERE u.yong_hu_ming='demo_physics_admin' AND r.jiao_se_dai_ma IN ('ADMIN','TEACHER')"));
        expect("单角色场景教师", 2, count("""
                SELECT COUNT(*) FROM (
                    SELECT u.id FROM yong_hu u JOIN yong_hu_jiao_se ur ON ur.yong_hu_id=u.id AND ur.zhuang_tai='ACTIVE'
                    JOIN jiao_se r ON r.id=ur.jiao_se_id AND r.zhuang_tai='ACTIVE'
                    WHERE u.yong_hu_ming IN ('demo_biology_teacher','demo_chemistry_teacher')
                    GROUP BY u.id HAVING COUNT(*)=1 AND MAX(r.jiao_se_dai_ma)='TEACHER'
                ) scenario_teachers
                """));
        expect("场景学生单角色", 8, count("""
                SELECT COUNT(*) FROM (
                    SELECT u.id FROM yong_hu u JOIN yong_hu_jiao_se ur ON ur.yong_hu_id=u.id AND ur.zhuang_tai='ACTIVE'
                    JOIN jiao_se r ON r.id=ur.jiao_se_id AND r.zhuang_tai='ACTIVE'
                    WHERE u.yong_hu_ming LIKE 'demo_199_%' OR u.yong_hu_ming LIKE 'demo_200_%'
                    GROUP BY u.id HAVING COUNT(*)=1 AND MAX(r.jiao_se_dai_ma)='STUDENT'
                ) scenario_students
                """));
        expect("教师档案", 1, count("SELECT COUNT(*) FROM jiao_shi_dang_an WHERE gong_hao='DEMO_T001' AND zhuang_tai='ACTIVE' AND yi_shan_chu=0"));
        expect("学生档案", 1, count("SELECT COUNT(*) FROM xue_sheng_dang_an WHERE xue_hao='DEMO_S001' AND zhuang_tai='ACTIVE' AND yi_shan_chu=0"));
        expect("演示班级", 1, count("SELECT COUNT(*) FROM ban_ji WHERE ban_ji_bian_ma='DEMO_CLASS_01' AND zhuang_tai='ACTIVE' AND yi_shan_chu=0"));
        expect("三个固定演示班级", 3, count("SELECT COUNT(*) FROM ban_ji WHERE ban_ji_bian_ma IN ('DEMO_CLASS_01','DEMO_CLASS_199','DEMO_CLASS_200') AND zhuang_tai='ACTIVE' AND yi_shan_chu=0"));
        expect("四位演示教师", 4, count("SELECT COUNT(*) FROM jiao_shi_dang_an WHERE gong_hao IN ('DEMO_T001','DEMO_T_PHYSICS','DEMO_T_BIOLOGY','DEMO_T_CHEMISTRY') AND zhuang_tai='ACTIVE' AND yi_shan_chu=0"));
        expect("九名演示学生", 9, count("SELECT COUNT(*) FROM xue_sheng_dang_an WHERE (xue_hao='DEMO_S001' OR xue_hao LIKE 'DEMO_199_%' OR xue_hao LIKE 'DEMO_200_%') AND zhuang_tai='ACTIVE' AND yi_shan_chu=0"));
        expect("199班五名学生", 5, count("SELECT COUNT(*) FROM ban_ji_xue_sheng bx JOIN ban_ji b ON b.id=bx.ban_ji_id WHERE b.ban_ji_bian_ma='DEMO_CLASS_199' AND bx.zhuang_tai='ACTIVE' AND bx.shi_fou_zhu_ban_ji=1"));
        expect("200班三名学生", 3, count("SELECT COUNT(*) FROM ban_ji_xue_sheng bx JOIN ban_ji b ON b.id=bx.ban_ji_id WHERE b.ban_ji_bian_ma='DEMO_CLASS_200' AND bx.zhuang_tai='ACTIVE' AND bx.shi_fou_zhu_ban_ji=1"));
        expect("每名学生一个主班级", 9, count("""
                SELECT COUNT(*) FROM (
                    SELECT s.id FROM xue_sheng_dang_an s JOIN ban_ji_xue_sheng bx ON bx.xue_sheng_id=s.id
                    WHERE (s.xue_hao='DEMO_S001' OR s.xue_hao LIKE 'DEMO_199_%' OR s.xue_hao LIKE 'DEMO_200_%')
                      AND bx.shi_fou_zhu_ban_ji=1 AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL
                    GROUP BY s.id HAVING COUNT(*)=1
                ) fixed_students
                """));
        expect("主班级关系", 1, count("SELECT COUNT(*) FROM ban_ji_xue_sheng bx JOIN ban_ji b ON b.id=bx.ban_ji_id WHERE b.ban_ji_bian_ma='DEMO_CLASS_01' AND bx.zhuang_tai='ACTIVE' AND bx.shi_fou_zhu_ban_ji=1"));
        expect("三元任课关系", 3, count("SELECT COUNT(*) FROM ren_ke_guan_xi r JOIN ban_ji b ON b.id=r.ban_ji_id JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id WHERE b.ban_ji_bian_ma='DEMO_CLASS_01' AND t.gong_hao='DEMO_T001' AND r.zhuang_tai='ACTIVE'"));
        expect("六条场景任课关系", 6, count("SELECT COUNT(*) FROM ren_ke_guan_xi r JOIN ban_ji b ON b.id=r.ban_ji_id JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id WHERE b.ban_ji_bian_ma IN ('DEMO_CLASS_199','DEMO_CLASS_200') AND t.gong_hao IN ('DEMO_T_PHYSICS','DEMO_T_BIOLOGY','DEMO_T_CHEMISTRY') AND r.zhuang_tai='ACTIVE'"));
        expect("九条ACTIVE任课关系", 9, count("SELECT COUNT(*) FROM ren_ke_guan_xi r JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id WHERE t.gong_hao LIKE 'DEMO_T%' AND r.zhuang_tai='ACTIVE'"));
        expect("十二条高频考点", 12, count("SELECT COUNT(*) FROM gao_pin_kao_dian h JOIN ren_ke_guan_xi r ON r.id=h.ren_ke_guan_xi_id JOIN ban_ji b ON b.id=r.ban_ji_id WHERE b.ban_ji_bian_ma IN ('DEMO_CLASS_199','DEMO_CLASS_200') AND h.zhuang_tai='ACTIVE' AND h.yi_shan_chu=0"));
        expect("每条场景任课关系两个高频考点", 6, count("""
                SELECT COUNT(*) FROM (
                    SELECT r.id FROM ren_ke_guan_xi r JOIN ban_ji b ON b.id=r.ban_ji_id
                    LEFT JOIN gao_pin_kao_dian h ON h.ren_ke_guan_xi_id=r.id AND h.zhuang_tai='ACTIVE' AND h.yi_shan_chu=0
                    WHERE b.ban_ji_bian_ma IN ('DEMO_CLASS_199','DEMO_CLASS_200') AND r.zhuang_tai='ACTIVE'
                    GROUP BY r.id HAVING COUNT(h.id)=2
                ) scopes_with_points
                """));
        expect("高频考点知识点同科", 0, count("""
                SELECT COUNT(*) FROM gao_pin_kao_dian h
                JOIN ren_ke_guan_xi r ON r.id=h.ren_ke_guan_xi_id
                JOIN zhi_shi_dian k ON k.id=h.zhi_shi_dian_id
                WHERE k.ke_mu_id<>r.ke_mu_id OR k.yi_shan_chu=1
                """));
        Map<String, Integer> expectedLeafCoverage = Map.of("PHYSICS", 18, "CHEMISTRY", 16, "BIOLOGY", 21);
        expect("Demo90稳定基线", BASE_DEMO_QUESTION_COUNT,
                count("SELECT COUNT(*) FROM ti_mu WHERE ti_gan LIKE '【演示】%' AND ti_gan NOT LIKE '【演示】变式：%' AND ti_gan NOT LIKE '【演示】覆盖：%'"));
        expect("审核通过变式题", DemoVariantQuestionBank.ACCEPTED_COUNT,
                count("SELECT COUNT(*) FROM ti_mu WHERE ti_gan LIKE '【演示】变式：%'"));
        expect("课程覆盖扩充题", DemoCurriculumQuestionBank.TOTAL_COUNT,
                count("SELECT COUNT(*) FROM ti_mu WHERE ti_gan LIKE '【演示】覆盖：%'"));
        expect("最终演示题总数", TOTAL_DEMO_QUESTION_COUNT, demoQuestionCount());
        Map<String, Integer> subjectCounts = Map.of("PHYSICS", 120, "CHEMISTRY", 120, "BIOLOGY", 120);
        for (String subject : List.of("PHYSICS", "CHEMISTRY", "BIOLOGY")) {
            expect(subject + "题目", subjectCounts.get(subject), count("SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id WHERE q.ti_gan LIKE '【演示】%' AND s.ke_mu_dai_ma=?", subject));
            expect(subject + "叶子知识点覆盖", expectedLeafCoverage.get(subject), count("""
                    SELECT COUNT(DISTINCT k.id) FROM ti_mu q
                    JOIN ke_mu s ON s.id=q.ke_mu_id
                    JOIN ti_mu_zhi_shi_dian qk ON qk.ti_mu_id=q.id AND qk.yi_shan_chu=0
                    JOIN zhi_shi_dian k ON k.id=qk.zhi_shi_dian_id AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                    WHERE q.ti_gan LIKE '【演示】%' AND s.ke_mu_dai_ma=?
                    """, subject));
            int fiveQuestionPoints = count("""
                    SELECT COUNT(*) FROM (
                        SELECT k.id FROM zhi_shi_dian k
                        JOIN ti_mu_zhi_shi_dian qk ON qk.zhi_shi_dian_id=k.id AND qk.yi_shan_chu=0
                        JOIN ti_mu q ON q.id=qk.ti_mu_id AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0
                        JOIN ke_mu s ON s.id=q.ke_mu_id
                        WHERE q.ti_gan LIKE '【演示】%' AND s.ke_mu_dai_ma=?
                          AND q.shi_yong_mo_shi='ONLINE_PRACTICE' AND q.shi_fou_ke_zi_dong_pan_fen=1
                        GROUP BY k.id HAVING COUNT(DISTINCT q.id)>=5
                    ) eligible_points
                    """, subject);
            if (fiveQuestionPoints < 3) {
                throw new IllegalStateException(subject + "至少需要3个可稳定创建五题练习的知识点，实际" + fiveQuestionPoints);
            }
            expect(subject + "难度覆盖", 3, count("SELECT COUNT(DISTINCT q.nan_du) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id WHERE q.ti_gan LIKE '【演示】%' AND s.ke_mu_dai_ma=?", subject));
            for (String type : List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "FILL_BLANK")) {
                int expected = "SINGLE_CHOICE".equals(type) ? 44 : 38;
                expect(subject + type, expected, count("SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id WHERE q.ti_gan LIKE '【演示】%' AND s.ke_mu_dai_ma=? AND q.ti_mu_lei_xing=?", subject, type));
            }
            for (int difficulty : List.of(1, 2, 3)) {
                int expected = difficulty == 2 ? 48 : 36;
                expect(subject + "难度" + difficulty, expected, count("SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id WHERE q.ti_gan LIKE '【演示】%' AND s.ke_mu_dai_ma=? AND q.nan_du=?", subject, difficulty));
            }
            for (String type : List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "FILL_BLANK")) {
                for (int difficulty : List.of(1, 2, 3)) {
                    int combinationCount = count("""
                            SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                            WHERE q.ti_gan LIKE '【演示】%' AND s.ke_mu_dai_ma=? AND q.ti_mu_lei_xing=? AND q.nan_du=?
                              AND q.zhuang_tai='PUBLISHED' AND q.shi_yong_mo_shi='ONLINE_PRACTICE'
                              AND q.shi_fou_ke_zi_dong_pan_fen=1 AND q.yi_shan_chu=0
                            """, subject, type, difficulty);
                    if (combinationCount < 5) {
                        throw new IllegalStateException(subject + " " + type + " 难度" + difficulty
                                + "至少需要5道可练习题，实际" + combinationCount);
                    }
                }
            }
        }
        expect("全部覆盖叶子至少三题", 55, count("""
                SELECT COUNT(*) FROM (
                    SELECT k.id FROM zhi_shi_dian k
                    JOIN ti_mu_zhi_shi_dian qk ON qk.zhi_shi_dian_id=k.id AND qk.yi_shan_chu=0
                    JOIN ti_mu q ON q.id=qk.ti_mu_id AND q.yi_shan_chu=0
                    WHERE q.ti_gan LIKE '【演示】%' AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                    GROUP BY k.id HAVING COUNT(DISTINCT q.id)>=3
                ) covered_points
                """));
        expect("可练习题", FINAL_DEMO_QUESTION_COUNT, count("SELECT COUNT(*) FROM ti_mu q WHERE q.ti_gan LIKE '【演示】%' AND q.zhuang_tai='PUBLISHED' AND q.shi_yong_mo_shi='ONLINE_PRACTICE' AND q.shi_fou_ke_zi_dong_pan_fen=1 AND q.yi_shan_chu=0"));
        expect("专题学习题", DemoTopicQuestionBank.TOTAL_COUNT, count("""
                SELECT COUNT(*) FROM ti_mu q WHERE q.ti_gan LIKE '【专题演示】%'
                  AND q.ti_mu_lei_xing='SUBJECTIVE' AND q.shi_yong_mo_shi='TOPIC_LEARNING'
                  AND q.shi_fou_ke_zi_dong_pan_fen=0 AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0
                """));
        for (String subject : List.of("PHYSICS", "CHEMISTRY", "BIOLOGY")) {
            expect(subject + "专题学习题", DemoTopicQuestionBank.COUNT_PER_SUBJECT, count("""
                    SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                    WHERE q.ti_gan LIKE '【专题演示】%' AND s.ke_mu_dai_ma=?
                      AND q.ti_mu_lei_xing='SUBJECTIVE' AND q.shi_yong_mo_shi='TOPIC_LEARNING'
                      AND q.shi_fou_ke_zi_dong_pan_fen=0 AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0
                    """, subject));
        }
        expect("专题标准解析", DemoTopicQuestionBank.TOTAL_COUNT, count("""
                SELECT COUNT(*) FROM ti_mu_jie_xi a JOIN ti_mu q ON q.id=a.ti_mu_id
                WHERE q.ti_gan LIKE '【专题演示】%' AND a.jie_xi_lei_xing='STANDARD'
                  AND a.zhuang_tai='PUBLISHED' AND a.yi_shan_chu=0 AND CHAR_LENGTH(TRIM(a.jie_xi_nei_rong))>=80
                """));
        validateStructuredTopicAnalyses();
        expect("完整可冻结题", FINAL_DEMO_QUESTION_COUNT, count("""
                SELECT COUNT(*) FROM ti_mu q
                WHERE q.ti_gan LIKE '【演示】%' AND q.zhuang_tai='PUBLISHED'
                  AND q.shi_yong_mo_shi='ONLINE_PRACTICE' AND q.shi_fou_ke_zi_dong_pan_fen=1 AND q.yi_shan_chu=0
                  AND EXISTS (SELECT 1 FROM ti_mu_jie_xi a WHERE a.ti_mu_id=q.id AND a.jie_xi_lei_xing='STANDARD' AND a.ban_ben_hao=1 AND a.zhuang_tai='PUBLISHED' AND a.yi_shan_chu=0)
                  AND EXISTS (SELECT 1 FROM ti_mu_zhi_shi_dian qk JOIN zhi_shi_dian k ON k.id=qk.zhi_shi_dian_id WHERE qk.ti_mu_id=q.id AND qk.yi_shan_chu=0 AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0)
                  AND ((q.ti_mu_lei_xing='FILL_BLANK' AND JSON_LENGTH(JSON_EXTRACT(q.zheng_que_da_an,'$.blanks'))>=1)
                    OR (q.ti_mu_lei_xing IN ('SINGLE_CHOICE','MULTIPLE_CHOICE')
                      AND (SELECT COUNT(*) FROM ti_mu_xuan_xiang o WHERE o.ti_mu_id=q.id AND o.yi_shan_chu=0)>=2
                      AND JSON_LENGTH(JSON_EXTRACT(q.zheng_que_da_an,'$.optionLabels'))>=1))
                """));
        expect("单选答案结构", 0, count("""
                SELECT COUNT(*) FROM ti_mu q
                WHERE q.ti_gan LIKE '【演示】%' AND q.ti_mu_lei_xing='SINGLE_CHOICE'
                  AND (JSON_UNQUOTE(JSON_EXTRACT(q.zheng_que_da_an,'$.type'))<>'SINGLE_CHOICE'
                    OR JSON_LENGTH(JSON_EXTRACT(q.zheng_que_da_an,'$.optionLabels'))<>1
                    OR (SELECT COUNT(*) FROM ti_mu_xuan_xiang o WHERE o.ti_mu_id=q.id AND o.shi_fou_zheng_que=1 AND o.yi_shan_chu=0)<>1)
                """));
        expect("多选答案结构", 0, count("""
                SELECT COUNT(*) FROM ti_mu q
                WHERE q.ti_gan LIKE '【演示】%' AND q.ti_mu_lei_xing='MULTIPLE_CHOICE'
                  AND (JSON_UNQUOTE(JSON_EXTRACT(q.zheng_que_da_an,'$.type'))<>'MULTIPLE_CHOICE'
                    OR JSON_LENGTH(JSON_EXTRACT(q.zheng_que_da_an,'$.optionLabels'))<2
                    OR (SELECT COUNT(*) FROM ti_mu_xuan_xiang o WHERE o.ti_mu_id=q.id AND o.shi_fou_zheng_que=1 AND o.yi_shan_chu=0)<2)
                """));
        expect("填空答案结构", 0, count("""
                SELECT COUNT(*) FROM ti_mu q
                WHERE q.ti_gan LIKE '【演示】%' AND q.ti_mu_lei_xing='FILL_BLANK'
                  AND (JSON_UNQUOTE(JSON_EXTRACT(q.zheng_que_da_an,'$.type'))<>'FILL_BLANK'
                    OR JSON_LENGTH(JSON_EXTRACT(q.zheng_que_da_an,'$.blanks'))<1
                    OR JSON_LENGTH(JSON_EXTRACT(q.zheng_que_da_an,'$.blanks[0].acceptedAnswers'))<1)
                """));
        expect("PUBLISHED标准解析", FINAL_DEMO_QUESTION_COUNT, count("SELECT COUNT(*) FROM ti_mu_jie_xi a JOIN ti_mu q ON q.id=a.ti_mu_id WHERE q.ti_gan LIKE '【演示】%' AND a.jie_xi_lei_xing='STANDARD' AND a.ban_ben_hao=1 AND a.zhuang_tai='PUBLISHED' AND a.yi_shan_chu=0"));
        validateChoiceAnalyses();
        List<String> lowQualityAnalyses = jdbc.queryForList("""
                SELECT CONCAT(q.id, ':', a.jie_xi_nei_rong) FROM ti_mu_jie_xi a JOIN ti_mu q ON q.id=a.ti_mu_id
                WHERE q.ti_gan LIKE '【演示】%' AND a.jie_xi_lei_xing='STANDARD'
                  AND (TRIM(a.jie_xi_nei_rong)='' OR CHAR_LENGTH(TRIM(a.jie_xi_nei_rong))<18)
                """, String.class);
        if (!lowQualityAnalyses.isEmpty()) {
            throw new IllegalStateException("普通题解析为空或明显过短: " + lowQualityAnalyses);
        }
        expect("普通题解析同义反复", 0, count("""
                SELECT COUNT(*) FROM ti_mu_jie_xi a JOIN ti_mu q ON q.id=a.ti_mu_id
                WHERE q.ti_gan LIKE '【演示】%' AND a.jie_xi_lei_xing='STANDARD'
                  AND (a.jie_xi_nei_rong LIKE '%答案是%A%因为%A%正确%'
                    OR a.jie_xi_nei_rong LIKE '%答案是%B%因为%B%正确%'
                    OR a.jie_xi_nei_rong LIKE '%答案是%C%因为%C%正确%'
                    OR a.jie_xi_nei_rong LIKE '%答案是%D%因为%D%正确%'
                    OR a.jie_xi_nei_rong LIKE '%正确答案正确%'
                    OR a.jie_xi_nei_rong LIKE '根据相关基本概念或计算%'
                    OR a.jie_xi_nei_rong LIKE '%应先依据题干给出的条件建立对应概念或数量关系%')
                """));
        expect("STANDARD解析无演示说明", 0, count("""
                SELECT COUNT(*) FROM ti_mu_jie_xi a JOIN ti_mu q ON q.id=a.ti_mu_id
                WHERE q.ti_gan LIKE '【演示】%' AND a.jie_xi_lei_xing='STANDARD'
                  AND (a.jie_xi_nei_rong LIKE '%演示时可用其他选项构造错题%'
                       OR a.jie_xi_nei_rong LIKE '%正确答案为由%')
                """));
        expect("活动图片附件", 2, count("SELECT COUNT(*) FROM ti_mu_fu_jian f JOIN ti_mu q ON q.id=f.ti_mu_id WHERE q.ti_gan LIKE '【演示】%' AND f.fu_jian_lei_xing='IMAGE' AND f.zhuang_tai='ACTIVE' AND f.yi_shan_chu=0"));
        List<DemoAttachment> physicsS1Attachments = jdbc.query("""
                SELECT f.guan_lian_wei_zhi,f.dui_xiang_biao_shi,f.xiang_dui_lu_jing,f.nei_rong_ha_xi
                FROM ti_mu_fu_jian f
                JOIN ti_mu q ON q.id=f.ti_mu_id
                JOIN ke_mu s ON s.id=q.ke_mu_id
                WHERE s.ke_mu_dai_ma='PHYSICS'
                  AND q.ti_gan='【演示】物体保持静止时，所受合力应为多少？〔图片对象 I001〕'
                  AND f.fu_jian_lei_xing='IMAGE' AND f.zhuang_tai='ACTIVE' AND f.yi_shan_chu=0
                ORDER BY f.guan_lian_wei_zhi
                """, (rs, row) -> new DemoAttachment(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
        if (physicsS1Attachments.size() != 2
                || physicsS1Attachments.stream().noneMatch(item -> "QUESTION".equals(item.position()) && "I001".equals(item.marker()))
                || physicsS1Attachments.stream().noneMatch(item -> "STANDARD_ANALYSIS".equals(item.position()) && "I002".equals(item.marker()))) {
            throw new IllegalStateException("PHYSICS-S1必须包含I001题干图片和I002标准解析图片");
        }
        for (DemoAttachment item : physicsS1Attachments) {
            QuestionAttachmentStorage.StoredImage image = attachmentStorage.read(item.relativePath(), item.hash());
            if (!"image/png".equals(image.mime()) || image.bytes().length == 0) {
                throw new IllegalStateException("PHYSICS-S1图片文件必须是可回读的PNG: " + item.marker());
            }
        }
        expect("对象标记", 0, count("""
                SELECT COUNT(*) FROM ti_mu q
                WHERE q.ti_gan LIKE '【演示】%'
                  AND (q.ti_gan LIKE '%[[I%' OR q.ti_gan LIKE '%[[F%'
                    OR CAST(q.zheng_que_da_an AS CHAR) LIKE '%[[I%' OR CAST(q.zheng_que_da_an AS CHAR) LIKE '%[[F%'
                    OR EXISTS (SELECT 1 FROM ti_mu_xuan_xiang o WHERE o.ti_mu_id=q.id AND (o.xuan_xiang_nei_rong LIKE '%[[I%' OR o.xuan_xiang_nei_rong LIKE '%[[F%'))
                    OR EXISTS (SELECT 1 FROM ti_mu_jie_xi a WHERE a.ti_mu_id=q.id AND (a.jie_xi_nei_rong LIKE '%[[I%' OR a.jie_xi_nei_rong LIKE '%[[F%')))
                """));
        expect("重复内容哈希", 0, count("SELECT COUNT(*)-COUNT(DISTINCT q.nei_rong_ha_xi) FROM ti_mu q WHERE (q.ti_gan LIKE '【演示】%' OR q.ti_gan LIKE '【专题演示】%') AND q.yi_shan_chu=0"));
        expect("重复标准化题干", 0, count("""
                SELECT COUNT(*) FROM (
                    SELECT REPLACE(REPLACE(REPLACE(LOWER(TRIM(q.ti_gan)),' ',''),'【演示】',''),'变式：','') normalized
                    FROM ti_mu q WHERE q.ti_gan LIKE '【演示】%' AND q.yi_shan_chu=0
                    GROUP BY normalized HAVING COUNT(*)>1
                ) duplicate_stems
                """));
        expect("三项来源", FINAL_DEMO_QUESTION_COUNT * 3, count("""
                SELECT COUNT(*) FROM ti_mu_lai_yuan s JOIN ti_mu q ON q.id=s.ti_mu_id
                WHERE q.ti_gan LIKE '【演示】%' AND s.lai_yuan_lei_xing='TEACHER_CREATED'
                  AND s.lai_yuan_ming_cheng='本科毕业设计自编演示题'
                  AND s.quan_li_zhuang_tai='USER_PROVIDED' AND s.quan_li_yi_ju IS NOT NULL
                  AND TRIM(s.quan_li_yi_ju)<>'' AND s.yi_shan_chu=0
                """));
        expect("审核轨迹", FINAL_DEMO_QUESTION_COUNT * 2, count("SELECT COUNT(*) FROM ti_mu_shen_he_ji_lu r JOIN ti_mu q ON q.id=r.ti_mu_id WHERE q.ti_gan LIKE '【演示】%' AND r.shen_he_dong_zuo IN ('SUBMITTED','APPROVED')"));
        for (String table : List.of("lian_xi_hui_hua", "lian_xi_ti_mu", "xue_sheng_da_ti", "xue_xi_jie_guo", "cuo_ti_ji_lu", "si_xin_hui_hua", "si_xin_xiao_xi")) expect(table + "初始记录", 0, count("SELECT COUNT(*) FROM " + table));
        System.out.println("演示数据校验通过: 14账号、3班级、4教师、9学生、9任课关系、12高频考点、55个叶子知识点、"
                + FINAL_DEMO_QUESTION_COUNT + "道普通练习题（物理/化学/生物各120）+ Topic18，合计"
                + TOTAL_DEMO_QUESTION_COUNT + "题；学习与私信记录为0");
    }

    public static void guardDatabaseName(String database) {
        if (database == null || database.isBlank() || FORBIDDEN_DATABASES.contains(database.toLowerCase()) || !database.matches("[A-Za-z0-9_]+")) {
            throw new IllegalStateException("拒绝对受保护或非法数据库执行演示数据操作: " + database);
        }
    }

    private Map<String, Long> seedUsers() {
        var result = new java.util.HashMap<String, Long>();
        for (String username : DEMO_USERS) {
            long id = insert("INSERT INTO yong_hu (yong_hu_ming,mi_ma_zhai_yao,zhang_hao_zhuang_tai,shi_fou_shou_ci_deng_lu,mi_ma_xiu_gai_shi_jian) VALUES (?,?,'ENABLED',0,CURRENT_TIMESTAMP(3))",
                    username, passwordEncoder.encode(DEMO_PASSWORD));
            List<String> roles = switch (username) {
                case "demo_admin" -> List.of("ADMIN");
                case "demo_teacher", "demo_biology_teacher", "demo_chemistry_teacher" -> List.of("TEACHER");
                case "demo_physics_admin" -> List.of("ADMIN", "TEACHER");
                default -> List.of("STUDENT");
            };
            for (String role : roles) {
                jdbc.update("INSERT INTO yong_hu_jiao_se (yong_hu_id,jiao_se_id,zhuang_tai) SELECT ?,id,'ACTIVE' FROM jiao_se WHERE jiao_se_dai_ma=? AND yi_shan_chu=0", id, role);
            }
            result.put(username, id);
        }
        return result;
    }

    private long seedTeacher(long userId, String employeeNumber, String name, String position) {
        return insert("INSERT INTO jiao_shi_dang_an (yong_hu_id,gong_hao,xing_ming,xian_shi_zhi_wu,zhuang_tai) VALUES (?,?,?,?,'ACTIVE')", userId, employeeNumber, name, position);
    }

    private long seedStudent(long userId, String studentNumber, String name) {
        return insert("INSERT INTO xue_sheng_dang_an (yong_hu_id,xue_hao,xing_ming,nian_ji,zhuang_tai) VALUES (?,?,?,'高三','ACTIVE')", userId, studentNumber, name);
    }

    private long seedClass(String code, String name) {
        return insert("INSERT INTO ban_ji (ban_ji_bian_ma,ban_ji_ming_cheng,nian_ji,ru_xue_nian_fen,zhuang_tai) VALUES (?,?,'高三',?,'ACTIVE')", code, name, Math.max(2000, Year.now().getValue() - 3));
    }

    private long seedTeachingScope(long teacherId, long classId, long subjectId) {
        return insert("INSERT INTO ren_ke_guan_xi (jiao_shi_id,ban_ji_id,ke_mu_id,shi_fou_zhu_ren_ke,zhuang_tai,kai_shi_shi_jian) VALUES (?,?,?,1,'ACTIVE',CURRENT_TIMESTAMP(3))",
                teacherId, classId, subjectId);
    }

    private void seedHighFrequencyPoints(Map<String, Long> scopes, Map<String, Long> points) {
        seedHighFrequencyPoint(required(scopes, "PHYSICS_199"), required(points, "力学>运动和力>牛顿运动定律"),
                "牛顿第二定律受力分析", "先画出研究对象受到的力，再沿选定方向列出合力与加速度的关系。", "合力方向决定加速度方向", "把某个单独的力误当成合力。", 1);
        seedHighFrequencyPoint(required(scopes, "PHYSICS_199"), required(points, "电磁学>电场>电场强度"),
                "电场强度方向判断", "正试探电荷所受电场力方向就是电场方向，负试探电荷受力方向与电场方向相反。", "正电荷顺场，负电荷逆场", "用负试探电荷受力方向直接代替电场方向。", 2);
        seedHighFrequencyPoint(required(scopes, "PHYSICS_200"), required(points, "力学>运动和力>牛顿运动定律"),
                "牛顿第二定律的单位检查", "使用F=ma时，质量用kg、加速度用m/s²，合力单位自然得到N。", "先统一单位再代入", "把g或质量写成带有其他单位的数值。", 1);
        seedHighFrequencyPoint(required(scopes, "PHYSICS_200"), required(points, "电磁学>电场>电场强度"),
                "电场线疏密与场强", "同一幅电场线图中，电场线越密的区域表示电场强度越大。", "线密场强大", "把电场线条数当成带电粒子运动轨迹。", 2);
        seedHighFrequencyPoint(required(scopes, "CHEMISTRY_199"), required(points, "化学基本概念>物质的量>摩尔计算"),
                "摩尔计算单位换算", "n=m/M，计算物质的量时质量m用g、摩尔质量M用g/mol，结果单位为mol。", "质量除以摩尔质量", "质量和摩尔质量单位没有统一。", 1);
        seedHighFrequencyPoint(required(scopes, "CHEMISTRY_199"), required(points, "无机化学>元素化合物>氧化还原反应"),
                "氧化还原电子变化", "失电子的物质被氧化，是还原剂；得电子的物质被还原，是氧化剂。", "失氧化、得还原", "把氧化剂和还原剂的名称对调。", 2);
        seedHighFrequencyPoint(required(scopes, "CHEMISTRY_200"), required(points, "化学基本概念>物质的量>摩尔计算"),
                "阿伏加德罗常数使用", "1 mol微粒约含6.02×10²³个微粒，换算时先确认题目统计的是原子、分子还是离子。", "一摩尔对应NA", "忽略化学式中的下标导致微粒数计算错误。", 1);
        seedHighFrequencyPoint(required(scopes, "CHEMISTRY_200"), required(points, "无机化学>元素化合物>氧化还原反应"),
                "化合价升降判断", "同一元素化合价升高表示失电子、被氧化；化合价降低表示得电子、被还原。", "升失氧、降得还", "只看反应物或生成物一侧，不比较化合价变化。", 2);
        seedHighFrequencyPoint(required(scopes, "BIOLOGY_199"), required(points, "分子与细胞>细胞结构>细胞膜"),
                "细胞膜运输方式", "小分子顺浓度梯度通过膜可属于自由扩散；借助载体但不消耗能量属于协助扩散。", "顺梯度不耗能", "把所有需要载体的运输都判断为主动运输。", 1);
        seedHighFrequencyPoint(required(scopes, "BIOLOGY_199"), required(points, "遗传与进化>遗传规律>分离定律"),
                "分离定律比例判断", "杂合子自交时，等位基因在形成配子时分离，理想完全显性条件下后代表型常见3∶1。", "配子分离再组合", "不说明显性关系就直接套用3∶1。", 2);
        seedHighFrequencyPoint(required(scopes, "BIOLOGY_200"), required(points, "分子与细胞>细胞结构>细胞膜"),
                "膜的选择透过性", "细胞膜允许水和部分小分子通过，同时限制其他物质，体现了选择透过性。", "选择性进出", "把选择透过性误写成完全不透过。", 1);
        seedHighFrequencyPoint(required(scopes, "BIOLOGY_200"), required(points, "遗传与进化>遗传规律>分离定律"),
                "等位基因分离时机", "等位基因随同源染色体在减数分裂形成配子时彼此分离，每个配子通常只含其中一个。", "减数分裂形成配子", "把等位基因分离误认为受精时发生。", 2);
    }

    private void seedHighFrequencyPoint(long scopeId, long knowledgePointId, String title, String content,
            String memoryTrick, String commonMistake, int sortOrder) {
        jdbc.update("""
                INSERT INTO gao_pin_kao_dian(ren_ke_guan_xi_id,zhi_shi_dian_id,biao_ti,nei_rong,ji_yi_kou_jue,chang_jian_wu_qu,pai_xu,zhuang_tai)
                VALUES (?,?,?,?,?,?,?,'ACTIVE')
                """, scopeId, knowledgePointId, title, content, memoryTrick, commonMistake, sortOrder);
    }

    private Map<String, Long> seedKnowledgePoints(Map<String, Long> subjects) {
        Map<String, LinkedHashSet<String>> leafPaths = new HashMap<>();
        for (Question question : questions()) {
            leafPaths.computeIfAbsent(question.subject(), ignored -> new LinkedHashSet<>()).add(question.knowledgePath());
        }
        var result = new HashMap<String, Long>();
        for (String subject : List.of("PHYSICS", "CHEMISTRY", "BIOLOGY")) {
            List<String> sortedLeaves = leafPaths.get(subject).stream().sorted().toList();
            Map<String, Long> ids = new HashMap<>();
            int order = 1;
            for (String leaf : sortedLeaves) {
                String[] segments = leaf.split(">");
                StringBuilder path = new StringBuilder();
                Long parentId = null;
                for (int level = 0; level < segments.length; level++) {
                    if (level > 0) path.append('>');
                    path.append(segments[level]);
                    String fullPath = path.toString();
                    Long existing = jdbc.query("SELECT id FROM zhi_shi_dian WHERE ke_mu_id=? AND wan_zheng_lu_jing=?",
                            rs -> rs.next() ? rs.getLong(1) : null, required(subjects, subject), fullPath);
                    long id = existing == null
                            ? insert("INSERT INTO zhi_shi_dian (ke_mu_id,fu_zhi_shi_dian_id,zhi_shi_dian_ming_cheng,wan_zheng_lu_jing,ceng_ji,pai_xu,zhuang_tai) VALUES (?,?,?,?,?,?,'ACTIVE')",
                                    required(subjects, subject), parentId, segments[level], fullPath, level + 1, order++)
                            : existing;
                    if (existing != null) {
                        jdbc.update("UPDATE zhi_shi_dian SET zhuang_tai='ACTIVE',yi_shan_chu=0 WHERE id=?", id);
                    }
                    ids.put(fullPath, id);
                    parentId = id;
                }
                result.put(leaf, required(ids, leaf));
            }
        }
        return result;
    }

    private void validateChoiceAnalyses() {
        List<ChoiceAnalysis> choices = jdbc.query("""
                SELECT q.id,a.jie_xi_nei_rong FROM ti_mu q
                JOIN ti_mu_jie_xi a ON a.ti_mu_id=q.id AND a.jie_xi_lei_xing='STANDARD' AND a.yi_shan_chu=0
                WHERE q.ti_gan LIKE '【演示】%' AND q.ti_mu_lei_xing IN ('SINGLE_CHOICE','MULTIPLE_CHOICE')
                  AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0
                ORDER BY q.id
                """, (rs, row) -> new ChoiceAnalysis(rs.getLong(1), rs.getString(2)));
        if (choices.size() != 246) {
            throw new IllegalStateException("Demo360选择题数量不符: " + choices.size());
        }
        Set<String> unique = new HashSet<>();
        for (ChoiceAnalysis choice : choices) {
            if (choice.analysis().length() < 160
                    || !choice.analysis().contains("结论：选择 ")
                    || !choice.analysis().contains("关键依据：")
                    || !choice.analysis().contains("易错点：")
                    || choice.analysis().contains("根据基本概念可知答案为")
                    || choice.analysis().contains("A 正确，其他错误")) {
                throw new IllegalStateException("Demo选择题解析结构不完整: " + choice.questionId());
            }
            List<OptionText> options = jdbc.query("""
                    SELECT xuan_xiang_biao_shi,xuan_xiang_nei_rong FROM ti_mu_xuan_xiang
                    WHERE ti_mu_id=? AND yi_shan_chu=0 ORDER BY pai_xu,id
                    """, (rs, row) -> new OptionText(rs.getString(1), rs.getString(2)), choice.questionId());
            if (options.isEmpty() || options.stream().anyMatch(option ->
                    !choice.analysis().contains(option.label() + ". " + option.content() + "："))) {
                throw new IllegalStateException("Demo选择题解析未逐项覆盖活动选项: " + choice.questionId());
            }
            unique.add(choice.analysis());
        }
        if (unique.size() != choices.size()) {
            throw new IllegalStateException("Demo选择题存在完全重复解析");
        }
    }

    private void validateStructuredTopicAnalyses() {
        List<String> analyses = jdbc.queryForList("""
                SELECT a.jie_xi_nei_rong FROM ti_mu q
                JOIN ti_mu_jie_xi a ON a.ti_mu_id=q.id AND a.jie_xi_lei_xing='STANDARD' AND a.yi_shan_chu=0
                WHERE q.ti_gan LIKE '【专题演示】%' AND q.zhuang_tai='PUBLISHED' AND q.yi_shan_chu=0
                ORDER BY q.id
                """, String.class);
        if (analyses.size() != DemoTopicQuestionBank.TOTAL_COUNT || analyses.stream().anyMatch(analysis -> {
            long nonEmptyLines = analysis.lines().map(String::trim).filter(line -> !line.isBlank()).count();
            return nonEmptyLines < 7 || !analysis.contains("解题思路\n") || !analysis.contains("步骤 1：")
                    || !analysis.contains("\n\n结论\n") || !analysis.contains("\n\n易错点\n");
        })) {
            throw new IllegalStateException("Topic18 标准解析没有全部采用多段结构");
        }
    }

    private void seedQuestion(Question q, long subjectId, long pointId, long adminId) {
        String stem = DEMO_STEM_PREFIX + q.stem() + ("PHYSICS-S1".equals(q.key()) ? "〔图片对象 I001〕" : "");
        List<OptionContent> hashOptions = q.options().stream()
                .map(option -> new OptionContent(option.label(), option.content())).toList();
        String contentHash = contentHashService.calculate(stem, hashOptions);
        String existingStem = jdbc.query("SELECT ti_gan FROM ti_mu WHERE ke_mu_id=? AND nei_rong_ha_xi=? AND yi_shan_chu=0",
                rs -> rs.next() ? rs.getString(1) : null, subjectId, contentHash);
        if (existingStem != null) {
            throw new IllegalStateException("Demo题内容重复: " + q.key() + " 与题面“" + existingStem + "”冲突");
        }
        long questionId = insert("""
                INSERT INTO ti_mu (ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,nan_du_shuo_ming,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi)
                VALUES (?,?,'ONLINE_PRACTICE',?,?,?,'本地演示数据难度分级',1,'PUBLISHED',?)
                """, subjectId, q.type(), stem, q.answer(), q.difficulty(), contentHash);
        int optionOrder = 1;
        for (Option option : q.options()) jdbc.update("INSERT INTO ti_mu_xuan_xiang (ti_mu_id,xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que,pai_xu) VALUES (?,?,?,?,?)",
                questionId, option.label(), option.content(), option.correct() ? 1 : 0, optionOrder++);
        String analysis = q.analysis() + ("PHYSICS-S1".equals(q.key()) ? "〔图片对象 I002〕" : "");
        jdbc.update("INSERT INTO ti_mu_jie_xi (ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (?,'STANDARD',?,1,'PUBLISHED')", questionId, analysis);
        if ("PHYSICS-S1".equals(q.key())) {
            QuestionAttachmentStorage.StoredImage image = attachmentStorage.store("demo-net-force.png", demoDiagram());
            long analysisId = jdbc.queryForObject("SELECT id FROM ti_mu_jie_xi WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD'", Long.class, questionId);
            for (String position : List.of("QUESTION", "STANDARD_ANALYSIS")) jdbc.update("INSERT INTO ti_mu_fu_jian(ti_mu_id,ti_mu_jie_xi_id,guan_lian_wei_zhi,fu_jian_lei_xing,yuan_shi_wen_jian_ming,xiang_dui_lu_jing,nei_rong_ha_xi,dui_xiang_biao_shi,zheng_wen_zi_fu_wei_zhi,pai_xu,zhuang_tai) VALUES (?,?,?,?,?,?,?,?,?,?, 'ACTIVE')", questionId, "STANDARD_ANALYSIS".equals(position) ? analysisId : null, position, "IMAGE", "demo-net-force.png", image.relativePath(), image.hash(), "QUESTION".equals(position) ? "I001" : "I002", 1, 1);
        }
        jdbc.update("INSERT INTO ti_mu_zhi_shi_dian (ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu) VALUES (?,?,1,1)", questionId, pointId);
        for (String contentType : List.of("QUESTION", "ANSWER", "STANDARD_ANALYSIS")) jdbc.update("""
                INSERT INTO ti_mu_lai_yuan (ti_mu_id,nei_rong_lei_xing,lai_yuan_lei_xing,lai_yuan_ming_cheng,lai_yuan_di_zhi,quan_li_zhuang_tai,quan_li_yi_ju)
                VALUES (?,?,'TEACHER_CREATED','本科毕业设计自编演示题',NULL,'USER_PROVIDED',?)
                """, questionId, contentType, RIGHTS_BASIS);
        jdbc.update("INSERT INTO ti_mu_shen_he_ji_lu (ti_mu_id,shen_he_dong_zuo,yuan_zhuang_tai,mu_biao_zhuang_tai,shen_he_ren_id,shen_he_yi_jian) VALUES (?,'SUBMITTED','DRAFT','PENDING',?,'演示题提交审核')", questionId, adminId);
        jdbc.update("INSERT INTO ti_mu_shen_he_ji_lu (ti_mu_id,shen_he_dong_zuo,yuan_zhuang_tai,mu_biao_zhuang_tai,shen_he_ren_id,shen_he_yi_jian) VALUES (?,'APPROVED','PENDING','PUBLISHED',?,'演示题审核通过')", questionId, adminId);
    }

    private void seedTopicQuestion(DemoTopicQuestionBank.TopicQuestion q, long subjectId, long pointId, long adminId) {
        String stem = TOPIC_STEM_PREFIX + q.title() + "｜" + q.stem();
        String contentHash = contentHashService.calculate(stem, List.of());
        String existingStem = jdbc.query("SELECT ti_gan FROM ti_mu WHERE ke_mu_id=? AND nei_rong_ha_xi=? AND yi_shan_chu=0",
                rs -> rs.next() ? rs.getString(1) : null, subjectId, contentHash);
        if (existingStem != null) {
            throw new IllegalStateException("Topic18题内容重复: " + q.key() + " 与题面“" + existingStem + "”冲突");
        }
        long questionId = insert("""
                INSERT INTO ti_mu (ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,nan_du_shuo_ming,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi)
                VALUES (?,'SUBJECTIVE','TOPIC_LEARNING',?,JSON_OBJECT('type','SUBJECTIVE'),?,'综合材料阅读与分步推理',0,'PUBLISHED',?)
                """, subjectId, stem, q.difficulty(), contentHash);
        jdbc.update("INSERT INTO ti_mu_jie_xi (ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (?,'STANDARD',?,1,'PUBLISHED')",
                questionId, q.analysis());
        jdbc.update("INSERT INTO ti_mu_zhi_shi_dian (ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu) VALUES (?,?,1,1)", questionId, pointId);
        for (String contentType : List.of("QUESTION", "ANSWER", "STANDARD_ANALYSIS")) jdbc.update("""
                INSERT INTO ti_mu_lai_yuan (ti_mu_id,nei_rong_lei_xing,lai_yuan_lei_xing,lai_yuan_ming_cheng,lai_yuan_di_zhi,quan_li_zhuang_tai,quan_li_yi_ju)
                VALUES (?,?,'TEACHER_CREATED','本科毕业设计自编演示题',NULL,'USER_PROVIDED',?)
                """, questionId, contentType, RIGHTS_BASIS);
        jdbc.update("INSERT INTO ti_mu_shen_he_ji_lu (ti_mu_id,shen_he_dong_zuo,yuan_zhuang_tai,mu_biao_zhuang_tai,shen_he_ren_id,shen_he_yi_jian) VALUES (?,'SUBMITTED','DRAFT','PENDING',?,'专题演示题提交审核')", questionId, adminId);
        jdbc.update("INSERT INTO ti_mu_shen_he_ji_lu (ti_mu_id,shen_he_dong_zuo,yuan_zhuang_tai,mu_biao_zhuang_tai,shen_he_ren_id,shen_he_yi_jian) VALUES (?,'APPROVED','PENDING','PUBLISHED',?,'专题演示题审核通过')", questionId, adminId);
    }

    private void cleanInternal() {
        guardDatabaseName(currentDatabase());
        jdbc.query("SELECT f.xiang_dui_lu_jing FROM ti_mu_fu_jian f JOIN ti_mu q ON q.id=f.ti_mu_id WHERE q.ti_gan LIKE '【演示】%' OR q.ti_gan LIKE '【专题演示】%'", (rs, row) -> rs.getString(1)).forEach(attachmentStorage::delete);
        jdbc.update("DELETE m FROM si_xin_xiao_xi m JOIN si_xin_hui_hua h ON h.id=m.hui_hua_id JOIN xue_sheng_dang_an s ON s.id=h.xue_sheng_id WHERE s.xue_hao LIKE 'DEMO_%'");
        jdbc.update("DELETE h FROM si_xin_hui_hua h JOIN xue_sheng_dang_an s ON s.id=h.xue_sheng_id WHERE s.xue_hao LIKE 'DEMO_%'");
        jdbc.update("DELETE w FROM cuo_ti_ji_lu w LEFT JOIN xue_sheng_dang_an s ON s.id=w.xue_sheng_id LEFT JOIN ti_mu q ON q.id=w.ti_mu_id WHERE s.xue_hao LIKE 'DEMO_%' OR q.ti_gan LIKE '【演示】%'");
        jdbc.update("DELETE r FROM xue_xi_jie_guo r JOIN lian_xi_hui_hua h ON h.id=r.lian_xi_hui_hua_id JOIN xue_sheng_dang_an s ON s.id=h.xue_sheng_id WHERE s.xue_hao LIKE 'DEMO_%'");
        jdbc.update("DELETE a FROM xue_sheng_da_ti a JOIN lian_xi_ti_mu pq ON pq.id=a.lian_xi_ti_mu_id JOIN lian_xi_hui_hua h ON h.id=pq.lian_xi_hui_hua_id JOIN xue_sheng_dang_an s ON s.id=h.xue_sheng_id WHERE s.xue_hao LIKE 'DEMO_%'");
        jdbc.update("DELETE pq FROM lian_xi_ti_mu pq JOIN lian_xi_hui_hua h ON h.id=pq.lian_xi_hui_hua_id JOIN xue_sheng_dang_an s ON s.id=h.xue_sheng_id WHERE s.xue_hao LIKE 'DEMO_%'");
        jdbc.update("DELETE h FROM lian_xi_hui_hua h JOIN xue_sheng_dang_an s ON s.id=h.xue_sheng_id WHERE s.xue_hao LIKE 'DEMO_%'");
        for (String table : List.of("ti_mu_fu_jian", "ti_mu_shen_he_ji_lu", "ti_mu_lai_yuan", "ti_mu_zhi_shi_dian", "ti_mu_jie_xi", "ti_mu_xuan_xiang")) jdbc.update("DELETE child FROM " + table + " child JOIN ti_mu q ON q.id=child.ti_mu_id WHERE q.ti_gan LIKE '【演示】%' OR q.ti_gan LIKE '【专题演示】%'");
        jdbc.update("DELETE FROM ti_mu WHERE ti_gan LIKE '【演示】%' OR ti_gan LIKE '【专题演示】%'");
        jdbc.update("DELETE h FROM gao_pin_kao_dian h JOIN ren_ke_guan_xi r ON r.id=h.ren_ke_guan_xi_id JOIN ban_ji b ON b.id=r.ban_ji_id WHERE b.ban_ji_bian_ma LIKE 'DEMO_CLASS_%'");
        jdbc.update("DELETE r FROM ren_ke_guan_xi r JOIN ban_ji b ON b.id=r.ban_ji_id WHERE b.ban_ji_bian_ma LIKE 'DEMO_CLASS_%'");
        jdbc.update("DELETE bx FROM ban_ji_xue_sheng bx JOIN ban_ji b ON b.id=bx.ban_ji_id WHERE b.ban_ji_bian_ma LIKE 'DEMO_CLASS_%'");
        jdbc.update("DELETE FROM ban_ji WHERE ban_ji_bian_ma LIKE 'DEMO_CLASS_%'");
        jdbc.update("DELETE FROM jiao_shi_dang_an WHERE gong_hao LIKE 'DEMO_T%'");
        jdbc.update("DELETE FROM xue_sheng_dang_an WHERE xue_hao LIKE 'DEMO_%'");
        jdbc.update("DELETE l FROM guan_li_cao_zuo_ri_zhi l JOIN yong_hu u ON u.id=l.cao_zuo_ren_yong_hu_id WHERE u.yong_hu_ming LIKE 'demo_%'");
        jdbc.update("DELETE ur FROM yong_hu_jiao_se ur JOIN yong_hu u ON u.id=ur.yong_hu_id WHERE u.yong_hu_ming LIKE 'demo_%'");
        jdbc.update("DELETE FROM yong_hu WHERE yong_hu_ming LIKE 'demo_%'");
        demoKnowledgePaths().stream()
                .sorted(Comparator.comparingInt(DemoDataService::pathDepth).reversed())
                .forEach(path -> jdbc.update("DELETE FROM zhi_shi_dian WHERE wan_zheng_lu_jing=? AND id>9", path));
    }

    private String currentDatabase() {
        return jdbc.queryForObject("SELECT DATABASE()", String.class);
    }

    private int demoQuestionCount() {
        return count("SELECT COUNT(*) FROM ti_mu WHERE (ti_gan LIKE '【演示】%' OR ti_gan LIKE '【专题演示】%') AND yi_shan_chu=0");
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private void expect(String label, int expected, int actual) {
        if (actual != expected) throw new IllegalStateException(label + "校验失败，期望" + expected + "，实际" + actual);
    }

    private long insert(String sql, Object... args) {
        GeneratedKeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < args.length; index++) statement.setObject(index + 1, args[index]);
            return statement;
        }, holder);
        if (holder.getKey() == null) throw new IllegalStateException("演示数据插入未返回主键");
        return holder.getKey().longValue();
    }

    private static long required(Map<String, Long> values, String key) {
        Long value = values.get(key);
        if (value == null) throw new IllegalStateException("缺少基础数据: " + key);
        return value;
    }

    private static Set<String> demoKnowledgePaths() {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        for (Question question : questions()) {
            StringBuilder current = new StringBuilder();
            for (String segment : question.knowledgePath().split(">")) {
                if (!current.isEmpty()) current.append('>');
                current.append(segment);
                paths.add(current.toString());
            }
        }
        for (DemoTopicQuestionBank.TopicQuestion question : DemoTopicQuestionBank.questions()) {
            StringBuilder current = new StringBuilder();
            for (String segment : question.knowledgePath().split(">")) {
                if (!current.isEmpty()) current.append('>');
                current.append(segment);
                paths.add(current.toString());
            }
        }
        return paths;
    }

    private static int pathDepth(String path) {
        return path.split(">").length;
    }

    private byte[] demoDiagram() {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BufferedImage image = new BufferedImage(360, 180, BufferedImage.TYPE_INT_RGB); Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE); g.fillRect(0, 0, 360, 180); g.setColor(new Color(37, 99, 235)); g.fillRect(135, 70, 90, 45); g.drawString("静止物体", 151, 96); g.setColor(Color.DARK_GRAY); g.drawLine(180, 45, 180, 65); g.drawLine(180, 65, 174, 57); g.drawLine(180, 65, 186, 57); g.drawString("合力 = 0", 145, 32); g.dispose(); ImageIO.write(image, "png", output); return output.toByteArray();
        } catch (Exception exception) { throw new IllegalStateException("无法生成演示图片", exception); }
    }

    private static List<Question> questions() {
        List<Question> items = new ArrayList<>();
        items.add(choice("PHYSICS-S1", "PHYSICS", "SINGLE_CHOICE", "物体保持静止时，所受合力应为多少？", "力学>运动和力>牛顿运动定律", 1,
                List.of("0", "1 N", "2 N", "无法确定"), Set.of("A"), "物体保持静止说明加速度为零；由牛顿第二定律F=ma可知，所受合力必须为零"));
        items.add(choice("PHYSICS-S2", "PHYSICS", "SINGLE_CHOICE", "在同一电场点，检验电荷量加倍时，该点电场强度如何变化？", "电磁学>电场>电场强度", 2,
                List.of("不变", "变为2倍", "变为一半", "变为4倍"), Set.of("A"), "电场中某点的场强由场源和位置共同决定；检验电荷量加倍时受力同比加倍，但E=F/q仍不变"));
        items.add(choice("PHYSICS-M1", "PHYSICS", "MULTIPLE_CHOICE", "关于惯性，下列说法正确的是哪些？", "力学>运动和力>牛顿运动定律", 3,
                List.of("质量越大惯性越大", "惯性是物体的固有属性", "速度越大惯性越大", "只有静止物体有惯性"), Set.of("A", "B"), "惯性是物体保持原有运动状态的固有属性，质量是其量度；速度大小和是否静止都不决定惯性有无"));
        items.add(choice("PHYSICS-M2", "PHYSICS", "MULTIPLE_CHOICE", "下列哪些过程通常会使物体内能增加？", "热学>分子动理论>温度和内能", 1,
                List.of("外界对物体做功", "物体吸收热量", "物体对外做功且不吸热", "物体向外放热"), Set.of("A", "B"), "改变内能有做功和热传递两条途径；外界做功或物体吸热通常使内能增加，向外做功、放热则相反"));
        items.add(fill("PHYSICS-F1", "PHYSICS", "质量为2 kg的物体获得3 m/s²加速度，合力为____N。", "力学>运动和力>牛顿运动定律", 2, "6"));
        items.add(fill("PHYSICS-F2", "PHYSICS", "理想情况下，物体不受外力时将保持静止或做____运动。", "力学>运动和力>牛顿运动定律", 3, "匀速直线"));

        items.add(choice("CHEMISTRY-S1", "CHEMISTRY", "SINGLE_CHOICE", "1 mol任意微粒所含微粒数约为多少？", "化学基本概念>物质的量>摩尔计算", 1,
                List.of("6.02×10²³", "3.01×10²³", "1.00×10²³", "6.02×10²²"), Set.of("A"), "物质的量与微粒数满足N=nNA；当n=1 mol时，N约为阿伏加德罗常数6.02×10²³"));
        items.add(choice("CHEMISTRY-S2", "CHEMISTRY", "SINGLE_CHOICE", "氧化还原反应中，还原剂发生什么变化？", "无机化学>元素化合物>氧化还原反应", 2,
                List.of("失去电子并被氧化", "得到电子并被还原", "只发生物理变化", "化合价一定降低"), Set.of("A"), "还原剂把电子提供给氧化剂，因此自身失去电子、所含元素化合价升高，发生氧化反应"));
        items.add(choice("CHEMISTRY-M1", "CHEMISTRY", "MULTIPLE_CHOICE", "关于物质的量和摩尔质量，下列说法正确的是哪些？", "化学基本概念>物质的量>摩尔计算", 3,
                List.of("物质的量单位是mol", "摩尔质量常用单位是g/mol", "1 mol任何物质质量都相同", "物质的量就是物质质量"), Set.of("A", "B"), "物质的量单位是mol，摩尔质量常用g/mol；由m=nM可知，不同物质的摩尔质量不同，1 mol时质量也不同"));
        items.add(choice("CHEMISTRY-M2", "CHEMISTRY", "MULTIPLE_CHOICE", "改变可逆反应条件后，下列哪些量可能立即发生变化？", "化学反应原理>化学平衡>平衡移动", 1,
                List.of("反应速率", "平衡移动方向", "元素种类", "原子总数"), Set.of("A", "B"), "浓度、温度或压强变化可立即改变反应速率，并可能使平衡向新状态移动；体系中的元素种类和原子总数仍守恒"));
        items.add(fill("CHEMISTRY-F1", "CHEMISTRY", "2 mol水分子含有____mol氢原子。", "化学基本概念>物质的量>摩尔计算", 2, "4"));
        items.add(fill("CHEMISTRY-F2", "CHEMISTRY", "可逆反应达到平衡时，正反应速率与逆反应速率____。", "化学反应原理>化学平衡>平衡移动", 3, "相等"));

        items.add(choice("BIOLOGY-S1", "BIOLOGY", "SINGLE_CHOICE", "细胞膜的基本支架主要由什么构成？", "分子与细胞>细胞结构>细胞膜", 1,
                List.of("磷脂双分子层", "纤维素单分子层", "核酸双链", "糖原颗粒"), Set.of("A"), "磷脂分子的亲水头部朝向两侧水环境、疏水尾部相对排列，形成细胞膜的磷脂双分子层基本支架"));
        items.add(choice("BIOLOGY-S2", "BIOLOGY", "SINGLE_CHOICE", "孟德尔分离定律描述的是哪类遗传因子的行为？", "遗传与进化>遗传规律>分离定律", 2,
                List.of("成对遗传因子", "细胞膜蛋白", "环境因子", "所有染色体整体"), Set.of("A"), "分离定律指出，杂合子中的成对遗传因子在形成配子时彼此分离，分别进入不同配子"));
        items.add(choice("BIOLOGY-M1", "BIOLOGY", "MULTIPLE_CHOICE", "细胞膜具有的功能包括哪些？", "分子与细胞>细胞结构>细胞膜", 3,
                List.of("控制物质进出", "参与细胞间信息交流", "储存全部遗传信息", "合成全部蛋白质"), Set.of("A", "B"), "细胞膜的选择透过性参与物质运输，膜受体等结构参与信息交流；遗传信息主要由DNA储存，蛋白质合成依赖核糖体"));
        items.add(choice("BIOLOGY-M2", "BIOLOGY", "MULTIPLE_CHOICE", "下列属于激素调节特点的有哪些？", "稳态与调节>生命活动调节>激素调节", 1,
                List.of("微量高效", "通过体液运输", "只在分泌部位起作用", "作用时间都极短"), Set.of("A", "B"), "激素通常具有微量高效、经体液运输并作用于特定靶细胞等特点，不局限于分泌部位，作用时间也并非都极短"));
        items.add(fill("BIOLOGY-F1", "BIOLOGY", "细胞膜控制物质进出体现了膜的____功能。", "分子与细胞>细胞结构>细胞膜", 2, "选择透过"));
        items.add(fill("BIOLOGY-F2", "BIOLOGY", "成对遗传因子在形成配子时彼此____。", "遗传与进化>遗传规律>分离定律", 3, "分离"));
        items.addAll(DemoQuestionBank.additionalQuestions());
        items.addAll(DemoVariantQuestionBank.acceptedQuestions());
        items.addAll(DemoCurriculumQuestionBank.questions());
        return rebalanceDifficulties(items);
    }

    private static List<Question> rebalanceDifficulties(List<Question> items) {
        Map<String, int[]> finalTargets = Map.of(
                "SINGLE_CHOICE", new int[] {14, 16, 14},
                "MULTIPLE_CHOICE", new int[] {11, 16, 11},
                "FILL_BLANK", new int[] {11, 16, 11});
        Map<String, Integer> reassigned = new HashMap<>();
        for (String subject : List.of("PHYSICS", "CHEMISTRY", "BIOLOGY")) {
            for (String type : List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "FILL_BLANK")) {
                List<Question> fixed = items.stream()
                        .filter(item -> subject.equals(item.subject()) && type.equals(item.type()) && !item.stem().startsWith("覆盖："))
                        .toList();
                int[] target = finalTargets.get(type);
                int[] remaining = new int[] {target[0], target[1], target[2]};
                for (Question item : fixed) remaining[item.difficulty() - 1]--;
                if (java.util.Arrays.stream(remaining).anyMatch(value -> value < 0)) {
                    throw new IllegalStateException(subject + " " + type + "固定题难度分布超过重平衡目标");
                }
                List<Question> ranked = items.stream()
                        .filter(item -> subject.equals(item.subject()) && type.equals(item.type()) && item.stem().startsWith("覆盖："))
                        .sorted(Comparator.comparingInt(Question::difficulty).thenComparing(Question::key))
                        .toList();
                if (ranked.size() != remaining[0] + remaining[1] + remaining[2]) {
                    throw new IllegalStateException(subject + " " + type + "题型数量无法执行确定性难度重平衡");
                }
                for (int index = 0; index < ranked.size(); index++) {
                    reassigned.put(ranked.get(index).key(), index < remaining[0] ? 1 : index < remaining[0] + remaining[1] ? 2 : 3);
                }
            }
        }
        return items.stream().map(item -> reassigned.containsKey(item.key()) ? item.withDifficulty(reassigned.get(item.key())) : item).toList();
    }

    static Question choice(String key, String subject, String type, String stem, String point, int difficulty,
            List<String> contents, Set<String> correct, String explanation) {
        List<Option> options = new ArrayList<>();
        List<String> labels = List.of("A", "B", "C", "D");
        for (int index = 0; index < labels.size(); index++) options.add(new Option(labels.get(index), contents.get(index), correct.contains(labels.get(index))));
        String answer = "{\"schemaVersion\":1,\"type\":\"" + type + "\",\"optionLabels\":[" + correct.stream().sorted().map(label -> "\"" + label + "\"").reduce((a, b) -> a + "," + b).orElse("") + "]}";
        return new Question(key, subject, type, stem, point, difficulty, answer, options,
                choiceExplanation(subject, contents, correct, explanation));
    }

    private static String choiceExplanation(String subject, List<String> contents, Set<String> correct, String explanation) {
        List<String> labels = List.of("A", "B", "C", "D");
        String conclusion = correct.stream().sorted().collect(java.util.stream.Collectors.joining("、"));
        String correctStatements = java.util.stream.IntStream.range(0, labels.size())
                .filter(index -> correct.contains(labels.get(index)))
                .mapToObj(contents::get)
                .collect(java.util.stream.Collectors.joining("；"));
        String basis = terminal(explanation);
        StringBuilder result = new StringBuilder("结论：选择 ").append(conclusion).append("。\n\n");
        for (int index = 0; index < labels.size(); index++) {
            String label = labels.get(index);
            String content = contents.get(index);
            result.append(label).append(". ").append(content).append("：");
            if (correct.contains(label)) {
                result.append("正确。").append(basis).append("因此该项给出的“")
                        .append(content).append("”符合题干条件。");
            } else if (content.matches(".*(全部|一定|始终|停止|完全|只需要|只能|不会|无关|无限).*")) {
                result.append("错误。").append(basis).append("题干只能支持“")
                        .append(correctStatements).append("”，不能推出该项的绝对判断“")
                        .append(content).append("”。");
            } else if (content.matches(".*[0-9０-９].*")) {
                result.append("错误。").append(basis).append("按题干数据和上述关系应得到“")
                        .append(correctStatements).append("”，不是该项的“").append(content).append("”。");
            } else {
                result.append("错误。").append(basis).append("由此可得“")
                        .append(correctStatements).append("”，而该项所述“").append(content)
                        .append("”颠倒、遗漏或超出了题干所给的条件。");
            }
            result.append("\n");
        }
        result.append("\n关键依据：").append(basis).append("\n\n易错点：")
                .append(switch (subject) {
                    case "PHYSICS" -> "先确定研究对象、方向和适用规律，再核对量纲与条件，不能只凭关键词选项。";
                    case "CHEMISTRY" -> "先核对物质、条件、反应或实验边界，再判断每个选项，不能把相近概念互换。";
                    case "BIOLOGY" -> "先限定材料、对象和生理或遗传条件，再逐项判断，不能把可能性写成必然性。";
                    default -> "逐项核对题干条件与结论，避免只记选项字母。";
                });
        return result.toString();
    }

    private static String terminal(String value) {
        String normalized = value.trim();
        return normalized.endsWith("。") || normalized.endsWith("；") ? normalized : normalized + "。";
    }

    static Question fill(String key, String subject, String stem, String point, int difficulty, String accepted) {
        return fill(key, subject, stem, point, difficulty, List.of(accepted));
    }

    static Question fill(String key, String subject, String stem, String point, int difficulty, List<String> accepted) {
        if (accepted.isEmpty()) throw new IllegalArgumentException("填空题至少需要一个可接受答案");
        String acceptedJson = accepted.stream().map(DemoDataService::jsonString)
                .collect(java.util.stream.Collectors.joining(","));
        String answer = "{\"schemaVersion\":1,\"type\":\"FILL_BLANK\",\"blanks\":[{\"index\":1,\"acceptedAnswers\":[" + acceptedJson + "],\"caseSensitive\":false}]}";
        return new Question(key, subject, "FILL_BLANK", stem, point, difficulty, answer, List.of(),
                fillExplanation(subject, stem, point, accepted.getFirst()));
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String fillExplanation(String subject, String stem, String point, String accepted) {
        if ("PHYSICS".equals(subject)) {
            if (stem.contains("不受外力")) return "本题考查牛顿第一定律。合力为零时物体的运动状态保持不变，所以原来运动的物体继续做匀速直线运动。";
            if (stem.contains("末速度")) return "本题考查牛顿第二定律和匀变速运动。先由a=F/m求加速度2 m/s²，再由v=at计算3 s后的速度为6 m/s。";
            if (stem.contains("合力为____")) return "本题考查牛顿第二定律。由F=ma，将质量2 kg和加速度3 m/s²代入，得到合力为6 N。";
            if (stem.contains("受到10 N合力")) return "本题考查牛顿第二定律。由a=F/m，将合力10 N除以质量5 kg，得到加速度2 m/s²。";
            if (stem.contains("所受电场力")) return "本题考查匀强电场中的受力关系。由F=qE，将0.02 C与500 N/C相乘，得到电场力10 N。";
            if (stem.contains("电场力做功")) return "本题考查匀强电场做功。电荷沿场强方向移动时W=qEd，代入0.01 C、200 N/C和0.5 m，得到1 J。";
            if (stem.contains("受到3 N电场力")) return "本题考查电场强度定义。由E=F/q，将3 N除以1 C，得到该点电场强度3 N/C。";
            if (stem.contains("27℃")) return "本题考查摄氏温标与热力学温标换算。按T=t+273计算，27℃约等于300 K。";
            if (stem.contains("0℃")) return "本题考查摄氏温标与热力学温标换算。按T=t+273计算，0℃约等于273 K。";
            if (stem.contains("吸收500 J")) return "本题考查热力学第一定律。气体吸热500 J并对外做功200 J，故内能增量ΔU=Q-W=300 J。";
        }
        if ("CHEMISTRY".equals(subject)) {
            if (stem.contains("2 mol水分子")) return "本题考查化学式与微粒计量。每个H₂O含2个氢原子，所以2 mol水分子含4 mol氢原子。";
            if (stem.contains("正反应速率")) return "本题考查化学平衡的动态特征。达到平衡时反应仍在进行，但正、逆反应速率相等，宏观组成保持稳定。";
            if (stem.contains("11.2 L")) return "本题考查标准状况气体摩尔体积。按22.4 L/mol计算，11.2 L除以22.4 L/mol得到0.5 mol。";
            if (stem.contains("2 mol CO₂")) return "本题考查化学式中的原子计量。每个CO₂含2个氧原子，所以2 mol CO₂含4 mol氧原子。";
            if (stem.contains("Fe³⁺转化为Fe²⁺")) return "本题考查氧化还原中的电子守恒。铁元素化合价由+3降至+2，每个Fe³⁺需要得到1个电子。";
            if (stem.contains("KMnO₄")) return "本题考查化合物中化合价代数和为零。K为+1、4个O合计-8，因此Mn应为+7价。";
            if (stem.contains("H₂O中O")) return "本题考查常见元素化合价。水分子呈电中性，两个H合计+2，因此O的化合价为-2。";
            if (stem.contains("平衡常数")) return "本题考查平衡常数的影响因素。对确定的可逆反应，平衡常数由温度决定，浓度或压强变化不改变其数值。";
            if (stem.contains("增大压强")) return "本题考查勒夏特列原理。加压时平衡向气体物质的量较小的一侧移动，合成氨反应右侧气体系数较小，所以向右移动。";
            if (stem.contains("各组分浓度")) return "本题考查动态平衡。外界条件不变时正、逆反应速率相等，各组分浓度保持不变，但反应并未停止。";
        }
        if ("BIOLOGY".equals(subject)) {
            if (stem.contains("控制物质进出")) return "本题考查细胞膜功能。膜对不同物质的通透能力不同，能够有选择地控制物质进出，体现选择透过功能。";
            if (stem.contains("形成配子时")) return "本题考查分离定律。杂合子形成配子时，成对遗传因子随同源染色体分开而彼此分离。";
            if (stem.contains("基本支架")) return "本题考查流动镶嵌模型。磷脂分子的亲水头朝向水环境、疏水尾相对排列，磷脂双分子层因此构成膜的基本支架。";
            if (stem.contains("限制另一些物质")) return "本题考查细胞膜的功能特性。膜对不同物质具有不同通透能力，这种有选择地允许物质通过的特性称为选择透过性。";
            if (stem.contains("Aa与Aa")) return "本题考查分离定律。Aa双方各产生A、a两类等比例配子，随机结合得到AA:Aa:aa=1:2:1，因此aa概率为1/4。";
            if (stem.contains("AA与aa")) return "本题考查配子结合。AA个体只产生A配子，aa个体只产生a配子，受精后F1全部为Aa。";
            if (stem.contains("显性纯合子")) return "本题考查基因型表示。纯合子含两个相同等位基因，显性等位基因用A表示，因此显性纯合子的基因型为AA。";
            if (stem.contains("血糖升高")) return "本题考查血糖的反馈调节。血糖升高会促进胰岛B细胞分泌胰岛素，增强葡萄糖摄取、利用和储存，所以分泌量增加。";
            if (stem.contains("持续高血糖")) return "本题考查胰岛素的生理作用。胰岛素不足时组织摄取和利用葡萄糖受限，血糖难以下降，因而可出现持续高血糖。";
            if (stem.contains("运输到全身")) return "本题考查激素调节特点。内分泌腺没有导管，激素进入血液等体液后随循环运输到相应靶细胞。";
        }
        return "本题考查“" + point + "”。应先依据题干给出的条件建立对应概念或数量关系，再得到结论“" + accepted + "”。";
    }

    record Question(String key, String subject, String type, String stem, String knowledgePath,
                    int difficulty, String answer, List<Option> options, String analysis) {
        Question withStemPrefix(String prefix) {
            return new Question(key, subject, type, prefix + stem, knowledgePath, difficulty, answer, options, analysis);
        }

        Question withDifficulty(int value) {
            return new Question(key, subject, type, stem, knowledgePath, value, answer, options, analysis);
        }
    }

    private record Option(String label, String content, boolean correct) {
    }

    static Question fill(String key, String subject, String stem, String point, int difficulty,
            String accepted, String analysis) {
        Question base = fill(key, subject, stem, point, difficulty, accepted);
        return new Question(base.key(), base.subject(), base.type(), base.stem(), base.knowledgePath(),
                base.difficulty(), base.answer(), base.options(), analysis.endsWith("。") ? analysis : analysis + "。");
    }

    static Question fill(String key, String subject, String stem, String point, int difficulty,
            List<String> accepted, String analysis) {
        Question base = fill(key, subject, stem, point, difficulty, accepted);
        return new Question(base.key(), base.subject(), base.type(), base.stem(), base.knowledgePath(),
                base.difficulty(), base.answer(), base.options(), analysis.endsWith("。") ? analysis : analysis + "。");
    }

    private record DemoAttachment(String position, String marker, String relativePath, String hash) {
    }

    private record ChoiceAnalysis(long questionId, String analysis) {
    }

    private record OptionText(String label, String content) {
    }
}
