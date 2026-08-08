package com.neu.riketiku.demo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Year;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoDataService {
    public static final String DEMO_PASSWORD = "a1234567";
    public static final String RIGHTS_BASIS = "本科毕业设计本地演示数据，由项目开发者自行编写，仅用于功能测试。";
    private static final Set<String> FORBIDDEN_DATABASES = Set.of(
            "rike_tiku", "mysql", "information_schema", "performance_schema", "sys");
    private static final String DEMO_STEM_PREFIX = "【演示】";
    private static final List<String> DEMO_USERS = List.of(
            "demo_admin", "demo_teacher", "demo_student",
            "demo_physics_admin", "demo_biology_teacher", "demo_chemistry_teacher",
            "demo_199_01", "demo_199_02", "demo_199_03", "demo_199_04", "demo_199_05",
            "demo_200_01", "demo_200_02", "demo_200_03");

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public DemoDataService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    public void validateSchema() {
        String database = currentDatabase();
        guardDatabaseName(database);
        int version = jdbc.queryForObject("SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success=1", Integer.class);
        int tableCount = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name<>'flyway_schema_history'", Integer.class);
        if (version != 10 || tableCount != 26) {
            throw new IllegalStateException("演示库必须完整执行V1-V10且包含26张业务表，当前V" + version + "，" + tableCount + "张");
        }
        System.out.println("演示数据库结构校验通过: " + database + "，V1-V10，26张业务表");
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
        expect("三个演示账号", 3, count("SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming IN ('demo_admin','demo_teacher','demo_student') AND yi_shan_chu=0"));
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
        expect("演示知识点", 9, count("SELECT COUNT(*) FROM zhi_shi_dian WHERE wan_zheng_lu_jing IN ('力学>运动和力>牛顿运动定律','电磁学>电场>电场强度','热学>分子动理论>温度和内能','化学基本概念>物质的量>摩尔计算','无机化学>元素化合物>氧化还原反应','化学反应原理>化学平衡>平衡移动','分子与细胞>细胞结构>细胞膜','遗传与进化>遗传规律>分离定律','稳态与调节>生命活动调节>激素调节')") );
        expect("演示题总数", 90, demoQuestionCount());
        for (String subject : List.of("PHYSICS", "CHEMISTRY", "BIOLOGY")) {
            expect(subject + "题目", 30, count("SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id WHERE q.ti_gan LIKE '【演示】%' AND s.ke_mu_dai_ma=?", subject));
            expect(subject + "难度覆盖", 3, count("SELECT COUNT(DISTINCT q.nan_du) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id WHERE q.ti_gan LIKE '【演示】%' AND s.ke_mu_dai_ma=?", subject));
            for (String type : List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "FILL_BLANK")) {
                expect(subject + type, 10, count("SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id WHERE q.ti_gan LIKE '【演示】%' AND s.ke_mu_dai_ma=? AND q.ti_mu_lei_xing=?", subject, type));
            }
            for (int difficulty : List.of(1, 2, 3)) {
                expect(subject + "难度" + difficulty, 10, count("SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id WHERE q.ti_gan LIKE '【演示】%' AND s.ke_mu_dai_ma=? AND q.nan_du=?", subject, difficulty));
            }
        }
        expect("每知识点10题", 9, count("""
                SELECT COUNT(*) FROM (
                    SELECT k.id FROM zhi_shi_dian k
                    JOIN ti_mu_zhi_shi_dian qk ON qk.zhi_shi_dian_id=k.id AND qk.yi_shan_chu=0
                    JOIN ti_mu q ON q.id=qk.ti_mu_id AND q.yi_shan_chu=0
                    WHERE q.ti_gan LIKE '【演示】%' AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                    GROUP BY k.id HAVING COUNT(DISTINCT q.id)=10
                ) covered_points
                """));
        expect("可练习题", 90, count("SELECT COUNT(*) FROM ti_mu q WHERE q.ti_gan LIKE '【演示】%' AND q.zhuang_tai='PUBLISHED' AND q.shi_yong_mo_shi='ONLINE_PRACTICE' AND q.shi_fou_ke_zi_dong_pan_fen=1 AND q.yi_shan_chu=0"));
        expect("完整可冻结题", 90, count("""
                SELECT COUNT(*) FROM ti_mu q
                WHERE q.ti_gan LIKE '【演示】%' AND q.zhuang_tai='PUBLISHED'
                  AND q.shi_yong_mo_shi='ONLINE_PRACTICE' AND q.shi_fou_ke_zi_dong_pan_fen=1 AND q.yi_shan_chu=0
                  AND EXISTS (SELECT 1 FROM ti_mu_jie_xi a WHERE a.ti_mu_id=q.id AND a.jie_xi_lei_xing='STANDARD' AND a.ban_ben_hao=1 AND a.zhuang_tai='PUBLISHED' AND a.yi_shan_chu=0)
                  AND EXISTS (SELECT 1 FROM ti_mu_zhi_shi_dian qk JOIN zhi_shi_dian k ON k.id=qk.zhi_shi_dian_id WHERE qk.ti_mu_id=q.id AND qk.yi_shan_chu=0 AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0)
                  AND NOT EXISTS (SELECT 1 FROM ti_mu_fu_jian f WHERE f.ti_mu_id=q.id AND f.zhuang_tai='ACTIVE' AND f.yi_shan_chu=0)
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
        expect("PUBLISHED标准解析", 90, count("SELECT COUNT(*) FROM ti_mu_jie_xi a JOIN ti_mu q ON q.id=a.ti_mu_id WHERE q.ti_gan LIKE '【演示】%' AND a.jie_xi_lei_xing='STANDARD' AND a.ban_ben_hao=1 AND a.zhuang_tai='PUBLISHED' AND a.yi_shan_chu=0"));
        expect("STANDARD解析无演示说明", 0, count("""
                SELECT COUNT(*) FROM ti_mu_jie_xi a JOIN ti_mu q ON q.id=a.ti_mu_id
                WHERE q.ti_gan LIKE '【演示】%' AND a.jie_xi_lei_xing='STANDARD'
                  AND (a.jie_xi_nei_rong LIKE '%演示时可用其他选项构造错题%'
                       OR a.jie_xi_nei_rong LIKE '%正确答案为由%')
                """));
        expect("活动附件", 0, count("SELECT COUNT(*) FROM ti_mu_fu_jian f JOIN ti_mu q ON q.id=f.ti_mu_id WHERE q.ti_gan LIKE '【演示】%' AND f.zhuang_tai='ACTIVE' AND f.yi_shan_chu=0"));
        expect("对象标记", 0, count("""
                SELECT COUNT(*) FROM ti_mu q
                WHERE q.ti_gan LIKE '【演示】%'
                  AND (q.ti_gan LIKE '%[[I%' OR q.ti_gan LIKE '%[[F%'
                    OR CAST(q.zheng_que_da_an AS CHAR) LIKE '%[[I%' OR CAST(q.zheng_que_da_an AS CHAR) LIKE '%[[F%'
                    OR EXISTS (SELECT 1 FROM ti_mu_xuan_xiang o WHERE o.ti_mu_id=q.id AND (o.xuan_xiang_nei_rong LIKE '%[[I%' OR o.xuan_xiang_nei_rong LIKE '%[[F%'))
                    OR EXISTS (SELECT 1 FROM ti_mu_jie_xi a WHERE a.ti_mu_id=q.id AND (a.jie_xi_nei_rong LIKE '%[[I%' OR a.jie_xi_nei_rong LIKE '%[[F%')))
                """));
        expect("重复内容哈希", 0, count("SELECT COUNT(*)-COUNT(DISTINCT q.nei_rong_ha_xi) FROM ti_mu q WHERE q.ti_gan LIKE '【演示】%' AND q.yi_shan_chu=0"));
        expect("三项来源", 270, count("""
                SELECT COUNT(*) FROM ti_mu_lai_yuan s JOIN ti_mu q ON q.id=s.ti_mu_id
                WHERE q.ti_gan LIKE '【演示】%' AND s.lai_yuan_lei_xing='TEACHER_CREATED'
                  AND s.lai_yuan_ming_cheng='本科毕业设计自编演示题'
                  AND s.quan_li_zhuang_tai='USER_PROVIDED' AND s.quan_li_yi_ju IS NOT NULL
                  AND TRIM(s.quan_li_yi_ju)<>'' AND s.yi_shan_chu=0
                """));
        expect("审核轨迹", 180, count("SELECT COUNT(*) FROM ti_mu_shen_he_ji_lu r JOIN ti_mu q ON q.id=r.ti_mu_id WHERE q.ti_gan LIKE '【演示】%' AND r.shen_he_dong_zuo IN ('SUBMITTED','APPROVED')"));
        for (String table : List.of("lian_xi_hui_hua", "lian_xi_ti_mu", "xue_sheng_da_ti", "xue_xi_jie_guo", "cuo_ti_ji_lu", "si_xin_hui_hua", "si_xin_xiao_xi")) expect(table + "初始记录", 0, count("SELECT COUNT(*) FROM " + table));
        System.out.println("演示数据校验通过: 14账号、3班级、4教师、9学生、9任课关系、12高频考点、9知识点、90题、学习与私信记录为0");
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
        Map<String, List<String>> paths = Map.of(
                "PHYSICS", List.of("力学>运动和力>牛顿运动定律", "电磁学>电场>电场强度", "热学>分子动理论>温度和内能"),
                "CHEMISTRY", List.of("化学基本概念>物质的量>摩尔计算", "无机化学>元素化合物>氧化还原反应", "化学反应原理>化学平衡>平衡移动"),
                "BIOLOGY", List.of("分子与细胞>细胞结构>细胞膜", "遗传与进化>遗传规律>分离定律", "稳态与调节>生命活动调节>激素调节"));
        var result = new java.util.HashMap<String, Long>();
        for (var entry : paths.entrySet()) {
            int order = 1;
            for (String path : entry.getValue()) {
                Long existing = jdbc.query("SELECT id FROM zhi_shi_dian WHERE ke_mu_id=? AND wan_zheng_lu_jing=? AND yi_shan_chu=0",
                        rs -> rs.next() ? rs.getLong(1) : null, required(subjects, entry.getKey()), path);
                long id = existing == null
                        ? insert("INSERT INTO zhi_shi_dian (ke_mu_id,zhi_shi_dian_ming_cheng,wan_zheng_lu_jing,ceng_ji,pai_xu,zhuang_tai) VALUES (?,?,?,?,?,'ACTIVE')",
                                required(subjects, entry.getKey()), path.substring(path.lastIndexOf('>') + 1), path, 3, order++)
                        : existing;
                result.put(path, id);
            }
        }
        return result;
    }

    private void seedQuestion(Question q, long subjectId, long pointId, long adminId) {
        long questionId = insert("""
                INSERT INTO ti_mu (ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,nan_du_shuo_ming,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi)
                VALUES (?,?,'ONLINE_PRACTICE',?,?,?,'本地演示数据难度分级',1,'PUBLISHED',?)
                """, subjectId, q.type(), DEMO_STEM_PREFIX + q.stem(), q.answer(), q.difficulty(), sha256(q.subject() + "|" + q.key() + "|" + q.stem()));
        int optionOrder = 1;
        for (Option option : q.options()) jdbc.update("INSERT INTO ti_mu_xuan_xiang (ti_mu_id,xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que,pai_xu) VALUES (?,?,?,?,?)",
                questionId, option.label(), option.content(), option.correct() ? 1 : 0, optionOrder++);
        jdbc.update("INSERT INTO ti_mu_jie_xi (ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (?,'STANDARD',?,1,'PUBLISHED')", questionId, q.analysis());
        jdbc.update("INSERT INTO ti_mu_zhi_shi_dian (ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu) VALUES (?,?,1,1)", questionId, pointId);
        for (String contentType : List.of("QUESTION", "ANSWER", "STANDARD_ANALYSIS")) jdbc.update("""
                INSERT INTO ti_mu_lai_yuan (ti_mu_id,nei_rong_lei_xing,lai_yuan_lei_xing,lai_yuan_ming_cheng,lai_yuan_di_zhi,quan_li_zhuang_tai,quan_li_yi_ju)
                VALUES (?,?,'TEACHER_CREATED','本科毕业设计自编演示题',NULL,'USER_PROVIDED',?)
                """, questionId, contentType, RIGHTS_BASIS);
        jdbc.update("INSERT INTO ti_mu_shen_he_ji_lu (ti_mu_id,shen_he_dong_zuo,yuan_zhuang_tai,mu_biao_zhuang_tai,shen_he_ren_id,shen_he_yi_jian) VALUES (?,'SUBMITTED','DRAFT','PENDING',?,'演示题提交审核')", questionId, adminId);
        jdbc.update("INSERT INTO ti_mu_shen_he_ji_lu (ti_mu_id,shen_he_dong_zuo,yuan_zhuang_tai,mu_biao_zhuang_tai,shen_he_ren_id,shen_he_yi_jian) VALUES (?,'APPROVED','PENDING','PUBLISHED',?,'演示题审核通过')", questionId, adminId);
    }

    private void cleanInternal() {
        guardDatabaseName(currentDatabase());
        jdbc.update("DELETE m FROM si_xin_xiao_xi m JOIN si_xin_hui_hua h ON h.id=m.hui_hua_id JOIN xue_sheng_dang_an s ON s.id=h.xue_sheng_id WHERE s.xue_hao LIKE 'DEMO_%'");
        jdbc.update("DELETE h FROM si_xin_hui_hua h JOIN xue_sheng_dang_an s ON s.id=h.xue_sheng_id WHERE s.xue_hao LIKE 'DEMO_%'");
        jdbc.update("DELETE w FROM cuo_ti_ji_lu w LEFT JOIN xue_sheng_dang_an s ON s.id=w.xue_sheng_id LEFT JOIN ti_mu q ON q.id=w.ti_mu_id WHERE s.xue_hao LIKE 'DEMO_%' OR q.ti_gan LIKE '【演示】%'");
        jdbc.update("DELETE r FROM xue_xi_jie_guo r JOIN lian_xi_hui_hua h ON h.id=r.lian_xi_hui_hua_id JOIN xue_sheng_dang_an s ON s.id=h.xue_sheng_id WHERE s.xue_hao LIKE 'DEMO_%'");
        jdbc.update("DELETE a FROM xue_sheng_da_ti a JOIN lian_xi_ti_mu pq ON pq.id=a.lian_xi_ti_mu_id JOIN lian_xi_hui_hua h ON h.id=pq.lian_xi_hui_hua_id JOIN xue_sheng_dang_an s ON s.id=h.xue_sheng_id WHERE s.xue_hao LIKE 'DEMO_%'");
        jdbc.update("DELETE pq FROM lian_xi_ti_mu pq JOIN lian_xi_hui_hua h ON h.id=pq.lian_xi_hui_hua_id JOIN xue_sheng_dang_an s ON s.id=h.xue_sheng_id WHERE s.xue_hao LIKE 'DEMO_%'");
        jdbc.update("DELETE h FROM lian_xi_hui_hua h JOIN xue_sheng_dang_an s ON s.id=h.xue_sheng_id WHERE s.xue_hao LIKE 'DEMO_%'");
        for (String table : List.of("ti_mu_fu_jian", "ti_mu_shen_he_ji_lu", "ti_mu_lai_yuan", "ti_mu_zhi_shi_dian", "ti_mu_jie_xi", "ti_mu_xuan_xiang")) jdbc.update("DELETE child FROM " + table + " child JOIN ti_mu q ON q.id=child.ti_mu_id WHERE q.ti_gan LIKE '【演示】%'");
        jdbc.update("DELETE FROM ti_mu WHERE ti_gan LIKE '【演示】%'");
        jdbc.update("DELETE h FROM gao_pin_kao_dian h JOIN ren_ke_guan_xi r ON r.id=h.ren_ke_guan_xi_id JOIN ban_ji b ON b.id=r.ban_ji_id WHERE b.ban_ji_bian_ma LIKE 'DEMO_CLASS_%'");
        jdbc.update("DELETE r FROM ren_ke_guan_xi r JOIN ban_ji b ON b.id=r.ban_ji_id WHERE b.ban_ji_bian_ma LIKE 'DEMO_CLASS_%'");
        jdbc.update("DELETE bx FROM ban_ji_xue_sheng bx JOIN ban_ji b ON b.id=bx.ban_ji_id WHERE b.ban_ji_bian_ma LIKE 'DEMO_CLASS_%'");
        jdbc.update("DELETE FROM ban_ji WHERE ban_ji_bian_ma LIKE 'DEMO_CLASS_%'");
        jdbc.update("DELETE FROM jiao_shi_dang_an WHERE gong_hao LIKE 'DEMO_T%'");
        jdbc.update("DELETE FROM xue_sheng_dang_an WHERE xue_hao LIKE 'DEMO_%'");
        jdbc.update("DELETE ur FROM yong_hu_jiao_se ur JOIN yong_hu u ON u.id=ur.yong_hu_id WHERE u.yong_hu_ming LIKE 'demo_%'");
        jdbc.update("DELETE FROM yong_hu WHERE yong_hu_ming LIKE 'demo_%'");
        jdbc.update("DELETE FROM zhi_shi_dian WHERE wan_zheng_lu_jing IN ('力学>运动和力>牛顿运动定律','电磁学>电场>电场强度','热学>分子动理论>温度和内能','化学基本概念>物质的量>摩尔计算','无机化学>元素化合物>氧化还原反应','化学反应原理>化学平衡>平衡移动','分子与细胞>细胞结构>细胞膜','遗传与进化>遗传规律>分离定律','稳态与调节>生命活动调节>激素调节') AND fu_zhi_shi_dian_id IS NULL");
    }

    private String currentDatabase() {
        return jdbc.queryForObject("SELECT DATABASE()", String.class);
    }

    private int demoQuestionCount() {
        return count("SELECT COUNT(*) FROM ti_mu WHERE ti_gan LIKE '【演示】%' AND yi_shan_chu=0");
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

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算演示题哈希", exception);
        }
    }

    private static List<Question> questions() {
        List<Question> items = new ArrayList<>();
        items.add(choice("PHYSICS-S1", "PHYSICS", "SINGLE_CHOICE", "物体保持静止时，所受合力应为多少？", "力学>运动和力>牛顿运动定律", 1,
                List.of("0", "1 N", "2 N", "无法确定"), Set.of("A"), "静止物体加速度为零，因此合力为零"));
        items.add(choice("PHYSICS-S2", "PHYSICS", "SINGLE_CHOICE", "在同一电场点，检验电荷量加倍时，该点电场强度如何变化？", "电磁学>电场>电场强度", 2,
                List.of("不变", "变为2倍", "变为一半", "变为4倍"), Set.of("A"), "电场强度由场源决定，与检验电荷量无关"));
        items.add(choice("PHYSICS-M1", "PHYSICS", "MULTIPLE_CHOICE", "关于惯性，下列说法正确的是哪些？", "力学>运动和力>牛顿运动定律", 3,
                List.of("质量越大惯性越大", "惯性是物体的固有属性", "速度越大惯性越大", "只有静止物体有惯性"), Set.of("A", "B"), "惯性只与质量有关，运动和静止的物体都具有惯性"));
        items.add(choice("PHYSICS-M2", "PHYSICS", "MULTIPLE_CHOICE", "下列哪些过程通常会使物体内能增加？", "热学>分子动理论>温度和内能", 1,
                List.of("外界对物体做功", "物体吸收热量", "物体对外做功且不吸热", "物体向外放热"), Set.of("A", "B"), "做功和热传递都可以改变内能"));
        items.add(fill("PHYSICS-F1", "PHYSICS", "质量为2 kg的物体获得3 m/s²加速度，合力为____N。", "力学>运动和力>牛顿运动定律", 2, "6"));
        items.add(fill("PHYSICS-F2", "PHYSICS", "理想情况下，物体不受外力时将保持静止或做____运动。", "力学>运动和力>牛顿运动定律", 3, "匀速直线"));

        items.add(choice("CHEMISTRY-S1", "CHEMISTRY", "SINGLE_CHOICE", "1 mol任意微粒所含微粒数约为多少？", "化学基本概念>物质的量>摩尔计算", 1,
                List.of("6.02×10²³", "3.01×10²³", "1.00×10²³", "6.02×10²²"), Set.of("A"), "1 mol微粒所含粒子数约为阿伏加德罗常数"));
        items.add(choice("CHEMISTRY-S2", "CHEMISTRY", "SINGLE_CHOICE", "氧化还原反应中，还原剂发生什么变化？", "无机化学>元素化合物>氧化还原反应", 2,
                List.of("失去电子并被氧化", "得到电子并被还原", "只发生物理变化", "化合价一定降低"), Set.of("A"), "还原剂提供电子，自身发生氧化反应"));
        items.add(choice("CHEMISTRY-M1", "CHEMISTRY", "MULTIPLE_CHOICE", "关于物质的量和摩尔质量，下列说法正确的是哪些？", "化学基本概念>物质的量>摩尔计算", 3,
                List.of("物质的量单位是mol", "摩尔质量常用单位是g/mol", "1 mol任何物质质量都相同", "物质的量就是物质质量"), Set.of("A", "B"), "物质的量与质量是不同物理量，不同物质摩尔质量不同"));
        items.add(choice("CHEMISTRY-M2", "CHEMISTRY", "MULTIPLE_CHOICE", "改变可逆反应条件后，下列哪些量可能立即发生变化？", "化学反应原理>化学平衡>平衡移动", 1,
                List.of("反应速率", "平衡移动方向", "元素种类", "原子总数"), Set.of("A", "B"), "浓度、温度或压强变化可改变速率并引起平衡移动，但不改变元素和原子守恒"));
        items.add(fill("CHEMISTRY-F1", "CHEMISTRY", "2 mol水分子含有____mol氢原子。", "化学基本概念>物质的量>摩尔计算", 2, "4"));
        items.add(fill("CHEMISTRY-F2", "CHEMISTRY", "可逆反应达到平衡时，正反应速率与逆反应速率____。", "化学反应原理>化学平衡>平衡移动", 3, "相等"));

        items.add(choice("BIOLOGY-S1", "BIOLOGY", "SINGLE_CHOICE", "细胞膜的基本支架主要由什么构成？", "分子与细胞>细胞结构>细胞膜", 1,
                List.of("磷脂双分子层", "纤维素单分子层", "核酸双链", "糖原颗粒"), Set.of("A"), "磷脂双分子层构成细胞膜的基本支架"));
        items.add(choice("BIOLOGY-S2", "BIOLOGY", "SINGLE_CHOICE", "孟德尔分离定律描述的是哪类遗传因子的行为？", "遗传与进化>遗传规律>分离定律", 2,
                List.of("成对遗传因子", "细胞膜蛋白", "环境因子", "所有染色体整体"), Set.of("A"), "成对遗传因子在形成配子时彼此分离"));
        items.add(choice("BIOLOGY-M1", "BIOLOGY", "MULTIPLE_CHOICE", "细胞膜具有的功能包括哪些？", "分子与细胞>细胞结构>细胞膜", 3,
                List.of("控制物质进出", "参与细胞间信息交流", "储存全部遗传信息", "合成全部蛋白质"), Set.of("A", "B"), "细胞膜参与物质运输和信息交流，遗传信息主要储存在DNA中"));
        items.add(choice("BIOLOGY-M2", "BIOLOGY", "MULTIPLE_CHOICE", "下列属于激素调节特点的有哪些？", "稳态与调节>生命活动调节>激素调节", 1,
                List.of("微量高效", "通过体液运输", "只在分泌部位起作用", "作用时间都极短"), Set.of("A", "B"), "激素通常微量高效，并随体液运输到靶细胞"));
        items.add(fill("BIOLOGY-F1", "BIOLOGY", "细胞膜控制物质进出体现了膜的____功能。", "分子与细胞>细胞结构>细胞膜", 2, "选择透过"));
        items.add(fill("BIOLOGY-F2", "BIOLOGY", "成对遗传因子在形成配子时彼此____。", "遗传与进化>遗传规律>分离定律", 3, "分离"));
        items.addAll(DemoQuestionBank.additionalQuestions());
        return items;
    }

    static Question choice(String key, String subject, String type, String stem, String point, int difficulty,
            List<String> contents, Set<String> correct, String explanation) {
        List<Option> options = new ArrayList<>();
        List<String> labels = List.of("A", "B", "C", "D");
        for (int index = 0; index < labels.size(); index++) options.add(new Option(labels.get(index), contents.get(index), correct.contains(labels.get(index))));
        String answer = "{\"schemaVersion\":1,\"type\":\"" + type + "\",\"optionLabels\":[" + correct.stream().sorted().map(label -> "\"" + label + "\"").reduce((a, b) -> a + "," + b).orElse("") + "]}";
        return new Question(key, subject, type, stem, point, difficulty, answer, options, explanation + "。");
    }

    static Question fill(String key, String subject, String stem, String point, int difficulty, String accepted) {
        String answer = "{\"schemaVersion\":1,\"type\":\"FILL_BLANK\",\"blanks\":[{\"index\":1,\"acceptedAnswers\":[\"" + accepted + "\"],\"caseSensitive\":false}]}";
        return new Question(key, subject, "FILL_BLANK", stem, point, difficulty, answer, List.of(), "根据相关基本概念或计算，空格应填写“" + accepted + "”。");
    }

    record Question(String key, String subject, String type, String stem, String knowledgePath,
                    int difficulty, String answer, List<Option> options, String analysis) {
    }

    private record Option(String label, String content, boolean correct) {
    }
}
