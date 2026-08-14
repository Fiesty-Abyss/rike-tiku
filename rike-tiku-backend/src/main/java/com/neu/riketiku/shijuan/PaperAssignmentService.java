package com.neu.riketiku.shijuan;

import com.neu.riketiku.ai.AiProviderService;
import com.neu.riketiku.ai.provider.AiModelRequest;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.QuestionDisplayTextNormalizer;
import com.neu.riketiku.xueshenglianxi.ObjectiveAnswerGrader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaperAssignmentService {
    private static final Set<String> OBJECTIVE = Set.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "FILL_BLANK");
    private final JdbcTemplate jdbc;
    private final ObjectiveAnswerGrader grader;
    private final QuestionDisplayTextNormalizer textNormalizer;
    private final AiProviderService aiProvider;
    private final ObjectMapper mapper = new ObjectMapper();

    public PaperAssignmentService(JdbcTemplate jdbc, ObjectiveAnswerGrader grader,
                                  QuestionDisplayTextNormalizer textNormalizer, AiProviderService aiProvider) {
        this.jdbc = jdbc;
        this.grader = grader;
        this.textNormalizer = textNormalizer;
        this.aiProvider = aiProvider;
    }

    @Transactional
    public PaperAssignmentDtos.Release publish(long userId, long paperId, PaperAssignmentDtos.Publish request) {
        Scope scope = jdbc.query("""
                SELECT r.id,r.ban_ji_id,r.ke_mu_id,r.jiao_shi_id,b.ban_ji_ming_cheng,k.ke_mu_ming_cheng
                FROM ren_ke_guan_xi r JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id
                JOIN ban_ji b ON b.id=r.ban_ji_id JOIN ke_mu k ON k.id=r.ke_mu_id
                WHERE r.id=? AND j.yong_hu_id=? AND r.zhuang_tai='ACTIVE'
                  AND b.zhuang_tai='ACTIVE' AND b.yi_shan_chu=0
                """, (rs, row) -> new Scope(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4),
                rs.getString(5), rs.getString(6)), request.teachingScopeId(), userId).stream().findFirst()
                .orElseThrow(() -> error("PAPER_SCOPE_FORBIDDEN", "只能发布到本人有效任课班级", HttpStatus.FORBIDDEN));
        PaperBase paper = ownedPaper(userId, paperId);
        if (!"READY".equals(paper.status()) || paper.subjectId() != scope.subjectId()) {
            throw error("PAPER_NOT_READY", "仅可发布同科目的 READY 试卷", HttpStatus.CONFLICT);
        }
        List<Snapshot> items = snapshots(paperId);
        if (items.isEmpty()) throw error("PAPER_EMPTY", "空试卷不能发布", HttpStatus.CONFLICT);
        LocalDateTime now = LocalDateTime.now();
        if (!request.deadline().isAfter(now)) throw error("PAPER_DEADLINE_INVALID", "截止时间必须晚于当前时间", HttpStatus.BAD_REQUEST);
        int version = jdbc.queryForObject("SELECT COALESCE(MAX(ban_ben_hao),0)+1 FROM shi_juan_fa_bu WHERE shi_juan_id=?", Integer.class, paperId);
        String hash = hash(items);
        jdbc.update("""
                INSERT INTO shi_juan_fa_bu(shi_juan_id,ren_ke_guan_xi_id,ban_ji_id,ke_mu_id,fa_bu_jiao_shi_id,
                  ban_ben_hao,kuai_zhao_ha_xi,fa_bu_shi_jian,jie_zhi_shi_jian,zhuang_tai)
                VALUES (?,?,?,?,?,?,?,?,?,'PUBLISHED')
                """, paperId, scope.id(), scope.classId(), scope.subjectId(), scope.teacherId(), version, hash, now, request.deadline());
        long releaseId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        for (Snapshot item : items) jdbc.update("""
                INSERT INTO shi_juan_fa_bu_ti_mu(shi_juan_fa_bu_id,ti_mu_id,ti_mu_shun_xu,fen_zhi,ti_mu_lei_xing,
                  ti_gan_kuai_zhao,xuan_xiang_kuai_zhao,zheng_que_da_an_kuai_zhao,biao_zhun_jie_xi_kuai_zhao,zhi_shi_dian_kuai_zhao)
                VALUES (?,?,?,?,?,?,CAST(? AS JSON),CAST(? AS JSON),?,CAST(? AS JSON))
                """, releaseId, item.questionId(), item.order(), item.score(), item.type(), item.stem(),
                item.optionsJson(), item.answerJson(), item.analysis(), item.pointsJson());
        return teacherRelease(userId, releaseId);
    }

    @Transactional(readOnly = true)
    public List<PaperAssignmentDtos.Release> studentList(long userId) {
        long studentId = student(userId);
        return jdbc.query("""
                SELECT r.id,r.shi_juan_id,p.shi_juan_ming_cheng,k.ke_mu_ming_cheng,b.ban_ji_ming_cheng,
                  r.fa_bu_shi_jian,r.jie_zhi_shi_jian,
                  CASE WHEN r.zhuang_tai='PUBLISHED' AND r.jie_zhi_shi_jian<CURRENT_TIMESTAMP(3) THEN 'EXPIRED' ELSE r.zhuang_tai END,
                  COALESCE(s.zhuang_tai,'NOT_STARTED'),s.ke_guan_de_fen,s.ke_guan_zong_fen
                FROM shi_juan_fa_bu r JOIN shi_juan p ON p.id=r.shi_juan_id JOIN ke_mu k ON k.id=r.ke_mu_id
                JOIN ban_ji b ON b.id=r.ban_ji_id
                JOIN ban_ji_xue_sheng bx ON bx.ban_ji_id=r.ban_ji_id AND bx.xue_sheng_id=?
                  AND bx.shi_fou_zhu_ban_ji=1 AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL
                LEFT JOIN shi_juan_ti_jiao s ON s.shi_juan_fa_bu_id=r.id AND s.xue_sheng_id=?
                WHERE r.zhuang_tai IN ('PUBLISHED','CLOSED') ORDER BY r.jie_zhi_shi_jian DESC
                """, (rs, row) -> release(rs), studentId, studentId);
    }

    @Transactional(readOnly = true)
    public PaperAssignmentDtos.Detail studentDetail(long userId, long releaseId) {
        long studentId = student(userId);
        PaperAssignmentDtos.Release release = studentRelease(studentId, releaseId);
        boolean submitted = "SUBMITTED".equals(release.submissionStatus());
        List<PaperAssignmentDtos.Question> questions = jdbc.query("""
                SELECT i.id,i.ti_mu_shun_xu,i.fen_zhi,i.ti_mu_lei_xing,i.ti_gan_kuai_zhao,
                  CAST(i.xuan_xiang_kuai_zhao AS CHAR),CAST(a.xue_sheng_da_an AS CHAR),a.shi_fou_zheng_que,a.de_fen,
                  CAST(i.zheng_que_da_an_kuai_zhao AS CHAR),i.biao_zhun_jie_xi_kuai_zhao,
                  CAST(i.zhi_shi_dian_kuai_zhao AS CHAR)
                FROM shi_juan_fa_bu_ti_mu i
                LEFT JOIN shi_juan_ti_jiao s ON s.shi_juan_fa_bu_id=i.shi_juan_fa_bu_id AND s.xue_sheng_id=?
                LEFT JOIN shi_juan_xue_sheng_da_ti a ON a.shi_juan_ti_jiao_id=s.id AND a.shi_juan_fa_bu_ti_mu_id=i.id
                WHERE i.shi_juan_fa_bu_id=? ORDER BY i.ti_mu_shun_xu
                """, (rs, row) -> new PaperAssignmentDtos.Question(rs.getLong(1), rs.getInt(2), rs.getBigDecimal(3),
                rs.getString(4), textNormalizer.normalize(rs.getString(5)), answerSlots(rs.getString(10)), options(rs.getString(6)), json(rs.getString(7)),
                nullableBoolean(rs.getObject(8)), rs.getBigDecimal(9), submitted ? rs.getString(10) : null,
                submitted ? rs.getString(11) : null, strings(rs.getString(12))), studentId, releaseId);
        return new PaperAssignmentDtos.Detail(release, questions, submitted);
    }

    @Transactional
    public void saveDraft(long userId, long releaseId, PaperAssignmentDtos.SaveDraft request) {
        long studentId = student(userId);
        PaperAssignmentDtos.Release release = studentRelease(studentId, releaseId);
        if (!"PUBLISHED".equals(release.status()) || !release.deadline().isAfter(LocalDateTime.now()))
            throw error("PAPER_CLOSED", "试卷已截止，不能保存草稿", HttpStatus.CONFLICT);
        long submissionId = submission(releaseId, studentId);
        ensureInProgress(submissionId);
        saveAnswers(submissionId, releaseId, request.answers(), false);
    }

    @Transactional
    public PaperAssignmentDtos.SubmitResult submit(long userId, long releaseId, PaperAssignmentDtos.Submit request) {
        long studentId = student(userId);
        PaperAssignmentDtos.Release release = studentRelease(studentId, releaseId);
        long submissionId = submission(releaseId, studentId);
        String state = jdbc.queryForObject("SELECT zhuang_tai FROM shi_juan_ti_jiao WHERE id=? FOR UPDATE", String.class, submissionId);
        if ("SUBMITTED".equals(state)) return submitResult(submissionId);
        if (!"PUBLISHED".equals(release.status()) || !release.deadline().isAfter(LocalDateTime.now()))
            throw error("PAPER_CLOSED", "试卷已截止，不能提交", HttpStatus.CONFLICT);
        saveAnswers(submissionId, releaseId, request.answers(), true);
        jdbc.update("""
                INSERT IGNORE INTO shi_juan_xue_sheng_da_ti(shi_juan_ti_jiao_id,shi_juan_fa_bu_ti_mu_id,xue_sheng_da_an,zhuang_tai)
                SELECT ?,id,NULL,'DRAFT' FROM shi_juan_fa_bu_ti_mu WHERE shi_juan_fa_bu_id=?
                """, submissionId, releaseId);
        List<GradeItem> items = gradeItems(submissionId, releaseId);
        BigDecimal score = BigDecimal.ZERO, total = BigDecimal.ZERO;
        int correct = 0, objective = 0, subjective = 0;
        for (GradeItem item : items) {
            if (!OBJECTIVE.contains(item.type())) { subjective++; jdbc.update("UPDATE shi_juan_xue_sheng_da_ti SET zhuang_tai='SUBJECTIVE_PENDING',shi_fou_zheng_que=NULL,de_fen=NULL WHERE id=?", item.answerId()); continue; }
            objective++; total = total.add(item.score());
            if (item.answer() == null) throw error("PAPER_ANSWER_INCOMPLETE", "请完成全部客观题后提交", HttpStatus.BAD_REQUEST);
            boolean ok = grader.grade(item.type(), item.correctAnswer(), item.optionsJson(), item.answer());
            BigDecimal awarded = ok ? item.score() : BigDecimal.ZERO;
            if (ok) { correct++; score = score.add(awarded); }
            jdbc.update("UPDATE shi_juan_xue_sheng_da_ti SET zhuang_tai='GRADED',shi_fou_zheng_que=?,de_fen=? WHERE id=?", ok, awarded, item.answerId());
        }
        jdbc.update("UPDATE shi_juan_ti_jiao SET zhuang_tai='SUBMITTED',ke_guan_de_fen=?,ke_guan_zong_fen=?,ti_jiao_shi_jian=CURRENT_TIMESTAMP(3) WHERE id=?", score, total, submissionId);
        return new PaperAssignmentDtos.SubmitResult(submissionId, score, total, correct, objective, subjective);
    }

    @Transactional(readOnly = true)
    public PaperAssignmentDtos.ClassStats classStats(long userId, long releaseId) {
        teacherRelease(userId, releaseId);
        long assigned = jdbc.queryForObject("SELECT COUNT(*) FROM shi_juan_fa_bu r JOIN ban_ji_xue_sheng b ON b.ban_ji_id=r.ban_ji_id AND b.shi_fou_zhu_ban_ji=1 AND b.zhuang_tai='ACTIVE' WHERE r.id=?", Long.class, releaseId);
        Map<String, Object> total = jdbc.queryForMap("SELECT COUNT(*) submitted,COALESCE(AVG(ke_guan_de_fen),0) average FROM shi_juan_ti_jiao WHERE shi_juan_fa_bu_id=? AND zhuang_tai='SUBMITTED'", releaseId);
        long submitted = ((Number) total.get("submitted")).longValue();
        List<PaperAssignmentDtos.QuestionMetric> questions = jdbc.query("""
                SELECT i.id,i.ti_mu_shun_xu,COUNT(a.id),COALESCE(SUM(a.shi_fou_zheng_que),0)
                FROM shi_juan_fa_bu_ti_mu i LEFT JOIN shi_juan_xue_sheng_da_ti a ON a.shi_juan_fa_bu_ti_mu_id=i.id AND a.zhuang_tai='GRADED'
                WHERE i.shi_juan_fa_bu_id=? GROUP BY i.id ORDER BY i.ti_mu_shun_xu
                """, (rs, row) -> new PaperAssignmentDtos.QuestionMetric(rs.getLong(1), rs.getInt(2), rs.getLong(3), rs.getLong(4), rate(rs.getLong(4), rs.getLong(3))), releaseId);
        Map<String, Counter> points = new LinkedHashMap<>();
        jdbc.query("""
                SELECT CAST(i.zhi_shi_dian_kuai_zhao AS CHAR),a.shi_fou_zheng_que
                FROM shi_juan_fa_bu_ti_mu i JOIN shi_juan_xue_sheng_da_ti a ON a.shi_juan_fa_bu_ti_mu_id=i.id AND a.zhuang_tai='GRADED'
                WHERE i.shi_juan_fa_bu_id=?
                """, rs -> { for (String point : strings(rs.getString(1))) points.computeIfAbsent(point, ignored -> new Counter()).add(rs.getBoolean(2)); }, releaseId);
        List<PaperAssignmentDtos.KnowledgeMetric> kp = points.entrySet().stream().map(e -> new PaperAssignmentDtos.KnowledgeMetric(e.getKey(), e.getValue().all, e.getValue().correct, rate(e.getValue().correct, e.getValue().all))).toList();
        List<String> weak = kp.stream().filter(v -> v.answered() > 0 && v.accuracy().compareTo(new BigDecimal("60")) < 0).map(PaperAssignmentDtos.KnowledgeMetric::knowledgePoint).limit(5).toList();
        return new PaperAssignmentDtos.ClassStats(assigned, submitted, assigned - submitted,
                (BigDecimal) total.get("average"), questions, kp, weak);
    }

    @Transactional(readOnly = true)
    public PaperAssignmentDtos.StudentProfile studentProfile(long userId, long releaseId, long studentId) {
        teacherRelease(userId, releaseId);
        long allowed = jdbc.queryForObject("""
                SELECT COUNT(*) FROM shi_juan_fa_bu r JOIN ban_ji_xue_sheng b ON b.ban_ji_id=r.ban_ji_id
                WHERE r.id=? AND b.xue_sheng_id=?
                """, Long.class, releaseId, studentId);
        if (allowed == 0) throw error("PAPER_STUDENT_FORBIDDEN", "学生不属于该发布班级", HttpStatus.NOT_FOUND);
        List<PaperAssignmentDtos.StudentTrend> trend = jdbc.query("""
                SELECT r.id,p.shi_juan_ming_cheng,s.ti_jiao_shi_jian,s.ke_guan_de_fen,s.ke_guan_zong_fen
                FROM shi_juan_ti_jiao s JOIN shi_juan_fa_bu r ON r.id=s.shi_juan_fa_bu_id JOIN shi_juan p ON p.id=r.shi_juan_id
                WHERE s.xue_sheng_id=? AND s.zhuang_tai='SUBMITTED' ORDER BY s.ti_jiao_shi_jian
                """, (rs, row) -> new PaperAssignmentDtos.StudentTrend(rs.getLong(1), rs.getString(2), rs.getTimestamp(3).toLocalDateTime(),
                rs.getBigDecimal(4), rs.getBigDecimal(5), percent(rs.getBigDecimal(4), rs.getBigDecimal(5))), studentId);
        List<String> weakTypes = jdbc.query("""
                SELECT i.ti_mu_lei_xing FROM shi_juan_ti_jiao s JOIN shi_juan_xue_sheng_da_ti a ON a.shi_juan_ti_jiao_id=s.id
                JOIN shi_juan_fa_bu_ti_mu i ON i.id=a.shi_juan_fa_bu_ti_mu_id WHERE s.xue_sheng_id=? AND a.zhuang_tai='GRADED'
                GROUP BY i.ti_mu_lei_xing HAVING AVG(a.shi_fou_zheng_que)<0.6 ORDER BY AVG(a.shi_fou_zheng_que)
                """, (rs, row) -> rs.getString(1), studentId);
        List<String> weakPoints = classStats(userId, releaseId).weakPoints();
        return new PaperAssignmentDtos.StudentProfile(studentId, trend, weakTypes, weakPoints, weakPoints);
    }

    @Transactional(readOnly = true)
    public PaperAssignmentDtos.QualityAssessment quality(long userId, long paperId) {
        PaperBase paper = ownedPaper(userId, paperId);
        List<Snapshot> items = snapshots(paperId);
        Map<String, Long> types = items.stream().collect(java.util.stream.Collectors.groupingBy(Snapshot::type, LinkedHashMap::new, java.util.stream.Collectors.counting()));
        Set<String> points = items.stream().flatMap(i -> strings(i.pointsJson()).stream()).collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        List<String> risks = new ArrayList<>();
        if (items.size() < 5) risks.add("题量较少，需由教师核对考查范围");
        if (types.size() == 1) risks.add("题型单一，建议人工检查题型梯度");
        if (points.size() < 2) risks.add("知识点覆盖较窄");
        return new PaperAssignmentDtos.QualityAssessment("FACTS_READY",
                "辅助建议，不代替教师审核",
                List.of("题型分布: " + types, "知识点数: " + points.size(), "总分: " + paper.totalScore()),
                risks, List.of("请结合班级薄弱点人工核对", "本评估不会换题、改分或发布试卷"));
    }

    @Transactional(readOnly = true)
    public PaperAssignmentDtos.AiQualityAssessment aiQuality(long userId, long paperId) {
        PaperAssignmentDtos.QualityAssessment facts = quality(userId, paperId);
        String prompt = """
                你是试卷质量辅助分析器。只依据下列确定性统计给出简洁中文建议，不推断学生成绩，不修改题目、分值或发布状态，禁止输出思维链。
                输出四个短段落：覆盖评价、难度与题量风险、班级适配提醒、教师复核建议。
                确定性统计：%s
                已识别风险：%s
                """.formatted(facts.coverage(), facts.risks());
        var result = aiProvider.generate(AiModelRequest.text("PAPER_QUALITY_ASSESSMENT", prompt));
        String content = result.content() == null ? "" : result.content().trim();
        if (content.isBlank()) throw error("AI_PAPER_QUALITY_EMPTY", "AI 试卷质量评估未返回可用内容", HttpStatus.SERVICE_UNAVAILABLE);
        return new PaperAssignmentDtos.AiQualityAssessment("AI_ADVICE_READY", "辅助建议，不代替教师审核",
                result.providerCode(), result.modelCode(), content, facts);
    }

    private void saveAnswers(long submissionId, long releaseId, List<PaperAssignmentDtos.DraftAnswer> answers, boolean submitting) {
        if (answers.stream().map(PaperAssignmentDtos.DraftAnswer::itemId).distinct().count() != answers.size())
            throw error("PAPER_ANSWER_DUPLICATE", "同一题不能重复提交答案", HttpStatus.BAD_REQUEST);
        for (PaperAssignmentDtos.DraftAnswer answer : answers) {
            long allowed = jdbc.queryForObject("SELECT COUNT(*) FROM shi_juan_fa_bu_ti_mu WHERE id=? AND shi_juan_fa_bu_id=?", Long.class, answer.itemId(), releaseId);
            if (allowed == 0) throw error("PAPER_ITEM_FORBIDDEN", "答案包含其他试卷题目", HttpStatus.BAD_REQUEST);
            jdbc.update("""
                    INSERT INTO shi_juan_xue_sheng_da_ti(shi_juan_ti_jiao_id,shi_juan_fa_bu_ti_mu_id,xue_sheng_da_an,zhuang_tai)
                    VALUES (?,?,CAST(? AS JSON),'DRAFT')
                    ON DUPLICATE KEY UPDATE xue_sheng_da_an=VALUES(xue_sheng_da_an),bao_cun_shi_jian=CURRENT_TIMESTAMP(3)
                    """, submissionId, answer.itemId(), mapper.writeValueAsString(answer.answer()));
        }
        if (!submitting) jdbc.update("UPDATE shi_juan_ti_jiao SET geng_xin_shi_jian=CURRENT_TIMESTAMP(3) WHERE id=?", submissionId);
    }

    private long submission(long releaseId, long studentId) {
        jdbc.update("INSERT IGNORE INTO shi_juan_ti_jiao(shi_juan_fa_bu_id,xue_sheng_id) VALUES (?,?)", releaseId, studentId);
        return jdbc.queryForObject("SELECT id FROM shi_juan_ti_jiao WHERE shi_juan_fa_bu_id=? AND xue_sheng_id=?", Long.class, releaseId, studentId);
    }
    private void ensureInProgress(long id) { if (!"IN_PROGRESS".equals(jdbc.queryForObject("SELECT zhuang_tai FROM shi_juan_ti_jiao WHERE id=?", String.class, id))) throw error("PAPER_ALREADY_SUBMITTED", "试卷已经提交", HttpStatus.CONFLICT); }
    private PaperAssignmentDtos.SubmitResult submitResult(long id) { Map<String,Object> m=jdbc.queryForMap("SELECT ke_guan_de_fen,ke_guan_zong_fen FROM shi_juan_ti_jiao WHERE id=?",id);Map<String,Object> c=jdbc.queryForMap("SELECT SUM(shi_fou_zheng_que=1) ok,SUM(zhuang_tai='GRADED') objective,SUM(zhuang_tai='SUBJECTIVE_PENDING') subjective FROM shi_juan_xue_sheng_da_ti WHERE shi_juan_ti_jiao_id=?",id);return new PaperAssignmentDtos.SubmitResult(id,(BigDecimal)m.get("ke_guan_de_fen"),(BigDecimal)m.get("ke_guan_zong_fen"),((Number)c.get("ok")).intValue(),((Number)c.get("objective")).intValue(),((Number)c.get("subjective")).intValue()); }
    private List<GradeItem> gradeItems(long submissionId,long releaseId){return jdbc.query("""
            SELECT a.id,i.ti_mu_lei_xing,i.fen_zhi,CAST(i.zheng_que_da_an_kuai_zhao AS CHAR),CAST(i.xuan_xiang_kuai_zhao AS CHAR),CAST(a.xue_sheng_da_an AS CHAR)
            FROM shi_juan_fa_bu_ti_mu i LEFT JOIN shi_juan_xue_sheng_da_ti a ON a.shi_juan_fa_bu_ti_mu_id=i.id AND a.shi_juan_ti_jiao_id=?
            WHERE i.shi_juan_fa_bu_id=? ORDER BY i.ti_mu_shun_xu
            """,(rs,row)->new GradeItem(rs.getLong(1),rs.getString(2),rs.getBigDecimal(3),rs.getString(4),rs.getString(5),json(rs.getString(6))),submissionId,releaseId);}
    private PaperAssignmentDtos.Release teacherRelease(long user,long release){return jdbc.query("""
            SELECT r.id,r.shi_juan_id,p.shi_juan_ming_cheng,k.ke_mu_ming_cheng,b.ban_ji_ming_cheng,r.fa_bu_shi_jian,r.jie_zhi_shi_jian,r.zhuang_tai,NULL,NULL,NULL
            FROM shi_juan_fa_bu r JOIN shi_juan p ON p.id=r.shi_juan_id JOIN ke_mu k ON k.id=r.ke_mu_id JOIN ban_ji b ON b.id=r.ban_ji_id
            JOIN jiao_shi_dang_an j ON j.id=r.fa_bu_jiao_shi_id WHERE r.id=? AND j.yong_hu_id=?
            """,(rs,row)->release(rs),release,user).stream().findFirst().orElseThrow(()->error("PAPER_RELEASE_NOT_FOUND","发布不存在或无权访问",HttpStatus.NOT_FOUND));}
    private PaperAssignmentDtos.Release studentRelease(long student,long release){return jdbc.query("""
            SELECT r.id,r.shi_juan_id,p.shi_juan_ming_cheng,k.ke_mu_ming_cheng,b.ban_ji_ming_cheng,r.fa_bu_shi_jian,r.jie_zhi_shi_jian,r.zhuang_tai,COALESCE(s.zhuang_tai,'NOT_STARTED'),s.ke_guan_de_fen,s.ke_guan_zong_fen
            FROM shi_juan_fa_bu r JOIN shi_juan p ON p.id=r.shi_juan_id JOIN ke_mu k ON k.id=r.ke_mu_id JOIN ban_ji b ON b.id=r.ban_ji_id
            JOIN ban_ji_xue_sheng bx ON bx.ban_ji_id=r.ban_ji_id AND bx.xue_sheng_id=? AND bx.shi_fou_zhu_ban_ji=1 AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL
            LEFT JOIN shi_juan_ti_jiao s ON s.shi_juan_fa_bu_id=r.id AND s.xue_sheng_id=? WHERE r.id=? AND r.zhuang_tai IN ('PUBLISHED','CLOSED')
            """,(rs,row)->release(rs),student,student,release).stream().findFirst().orElseThrow(()->error("PAPER_RELEASE_NOT_FOUND","试卷不存在或不属于当前班级",HttpStatus.NOT_FOUND));}
    private PaperAssignmentDtos.Release release(java.sql.ResultSet rs)throws java.sql.SQLException{return new PaperAssignmentDtos.Release(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getTimestamp(6).toLocalDateTime(),rs.getTimestamp(7).toLocalDateTime(),rs.getString(8),rs.getString(9),rs.getBigDecimal(10),rs.getBigDecimal(11));}
    private PaperBase ownedPaper(long user,long paper){return jdbc.query("SELECT p.id,p.ke_mu_id,p.zhuang_tai,p.zong_fen FROM shi_juan p JOIN jiao_shi_dang_an j ON j.id=p.chuang_jian_jiao_shi_id WHERE p.id=? AND j.yong_hu_id=? AND p.yi_shan_chu=0",(rs,row)->new PaperBase(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getBigDecimal(4)),paper,user).stream().findFirst().orElseThrow(()->error("PAPER_NOT_FOUND","试卷不存在或无权访问",HttpStatus.NOT_FOUND));}
    private List<Snapshot> snapshots(long paper){return jdbc.query("""
            SELECT q.id,i.ti_mu_shun_xu,i.fen_zhi,q.ti_mu_lei_xing,q.ti_gan,
            COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('label',o.xuan_xiang_biao_shi,'content',o.xuan_xiang_nei_rong)) FROM ti_mu_xuan_xiang o WHERE o.ti_mu_id=q.id AND o.yi_shan_chu=0),'[]'),
            CAST(q.zheng_que_da_an AS CHAR),a.jie_xi_nei_rong,
            COALESCE((SELECT JSON_ARRAYAGG(p.wan_zheng_lu_jing) FROM ti_mu_zhi_shi_dian qp JOIN zhi_shi_dian p ON p.id=qp.zhi_shi_dian_id WHERE qp.ti_mu_id=q.id AND qp.yi_shan_chu=0),'[]')
            FROM shi_juan_ti_mu i JOIN ti_mu q ON q.id=i.ti_mu_id JOIN ti_mu_jie_xi a ON a.ti_mu_id=q.id AND a.jie_xi_lei_xing='STANDARD' WHERE i.shi_juan_id=? ORDER BY i.ti_mu_shun_xu
            """,(rs,row)->new Snapshot(rs.getLong(1),rs.getInt(2),rs.getBigDecimal(3),rs.getString(4),textNormalizer.normalize(rs.getString(5)),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9)),paper);}
    private long student(long user){return jdbc.query("SELECT id FROM xue_sheng_dang_an WHERE yong_hu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0",(rs,row)->rs.getLong(1),user).stream().findFirst().orElseThrow(()->error("STUDENT_PROFILE_REQUIRED","学生档案不可用",HttpStatus.FORBIDDEN));}
    private String hash(List<Snapshot> items){try{MessageDigest digest=MessageDigest.getInstance("SHA-256");return HexFormat.of().formatHex(digest.digest(mapper.writeValueAsBytes(items)));}catch(Exception e){throw new IllegalStateException("试卷快照哈希失败",e);}}
    private List<PaperAssignmentDtos.Option> options(String json){if(json==null)return List.of();return mapper.readValue(json,new TypeReference<List<PaperAssignmentDtos.Option>>(){});}
    private List<String> strings(String json){if(json==null)return List.of();return mapper.readValue(json,new TypeReference<List<String>>(){});}
    private JsonNode json(String value){return value==null?null:mapper.readTree(value);}
    private int answerSlots(String answerJson){JsonNode answer=json(answerJson);return answer!=null&&answer.path("blanks").isArray()?answer.path("blanks").size():1;}
    private Boolean nullableBoolean(Object value){
        if(value==null)return null;
        if(value instanceof Boolean bool)return bool;
        return ((Number)value).intValue()!=0;
    }
    private BigDecimal rate(long correct,long all){return all==0?BigDecimal.ZERO:BigDecimal.valueOf(correct*100).divide(BigDecimal.valueOf(all),2,RoundingMode.HALF_UP);}
    private BigDecimal percent(BigDecimal score,BigDecimal total){return total==null||total.signum()==0?BigDecimal.ZERO:score.multiply(BigDecimal.valueOf(100)).divide(total,2,RoundingMode.HALF_UP);}
    private RenZhengYeWuYiChang error(String code,String message,HttpStatus status){return new RenZhengYeWuYiChang(code,message,status);}
    private record Scope(long id,long classId,long subjectId,long teacherId,String className,String subjectName){}
    private record PaperBase(long id,long subjectId,String status,BigDecimal totalScore){}
    private record Snapshot(long questionId,int order,BigDecimal score,String type,String stem,String optionsJson,String answerJson,String analysis,String pointsJson){}
    private record GradeItem(long answerId,String type,BigDecimal score,String correctAnswer,String optionsJson,JsonNode answer){}
    private static final class Counter{long all;long correct;void add(boolean ok){all++;if(ok)correct++;}}
}
