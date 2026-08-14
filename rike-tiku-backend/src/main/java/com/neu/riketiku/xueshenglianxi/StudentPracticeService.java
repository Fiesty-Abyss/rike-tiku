package com.neu.riketiku.xueshenglianxi;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.fujian.QuestionAttachmentContentService;
import com.neu.riketiku.tiku.QuestionDisplayTextNormalizer;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentPracticeService {
    private final ObjectiveAnswerGrader objectiveAnswerGrader;
    private static final List<String> AUTO_GRADABLE_TYPES = List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "FILL_BLANK");
    private static final Pattern OBJECT_MARKER = Pattern.compile("〔(?:图片|公式)对象\\s+([IF]\\d{3})〕");
    private final JdbcTemplate jdbc;
    private final QuestionAttachmentContentService attachmentContentService;
    private final QuestionDisplayTextNormalizer textNormalizer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StudentPracticeService(JdbcTemplate jdbc, QuestionAttachmentContentService attachmentContentService,
                                  ObjectiveAnswerGrader objectiveAnswerGrader,QuestionDisplayTextNormalizer textNormalizer) {
        this.jdbc = jdbc;
        this.attachmentContentService = attachmentContentService;
        this.objectiveAnswerGrader = objectiveAnswerGrader;
        this.textNormalizer=textNormalizer;
    }

    @Transactional(readOnly = true)
    public Set<Long> recommendationEligibleKnowledgePointIds(long subjectId, int questionCount) {
        Map<Long, Integer> availableCounts = new HashMap<>();
        StudentPracticeDtos.CreateRequest request = new StudentPracticeDtos.CreateRequest(
                subjectId, null, null, null, questionCount);
        for (QuestionPoolItem question : findEligibleQuestions(null,request)) {
            for (StudentPracticeDtos.KnowledgePoint point : question.knowledgePoints()) {
                availableCounts.merge(point.id(), 1, Integer::sum);
            }
        }
        return availableCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= questionCount)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public StudentPracticeDtos.Options options(Long userId, Long subjectId) {
        requireStudent(userId);
        List<StudentPracticeDtos.Subject> subjects = jdbc.query("""
                SELECT id,ke_mu_dai_ma,ke_mu_ming_cheng
                FROM ke_mu
                WHERE zhuang_tai='ACTIVE' AND yi_shan_chu=0
                ORDER BY pai_xu,id
                """, (rs, row) -> new StudentPracticeDtos.Subject(rs.getLong(1), rs.getString(2), rs.getString(3)));
        List<StudentPracticeDtos.KnowledgePoint> points = subjectId == null ? List.of() : activeKnowledgePoints(subjectId);
        return new StudentPracticeDtos.Options(subjects, points);
    }

    @Transactional(readOnly = true)
    public StudentPracticeDtos.Availability availability(Long userId, StudentPracticeDtos.CreateRequest request) {
        requireStudent(userId);
        validateRequest(request);
        validateSubject(request.subjectId());
        validateKnowledgePoints(request.subjectId(), request.knowledgePointIds());
        int available = findEligibleQuestions(userId,request).size();
        return new StudentPracticeDtos.Availability(available, Math.min(5, available));
    }

    @Transactional
    public StudentPracticeDtos.Session create(Long userId, StudentPracticeDtos.CreateRequest request) {
        long studentId = requireStudent(userId);
        validateRequest(request);
        validateSubject(request.subjectId());
        validateKnowledgePoints(request.subjectId(), request.knowledgePointIds());

        List<QuestionPoolItem> pool = findEligibleQuestions(userId,request);
        if (pool.size() < request.count()) {
            fail("PRACTICE_QUESTION_INSUFFICIENT", "符合条件的已发布题目不足，还差" + (request.count() - pool.size()) + "题", HttpStatus.BAD_REQUEST);
        }

        List<QuestionPoolItem> selectedPool = new ArrayList<>(pool);
        if (request.referenceQuestionId() == null) {
            Collections.shuffle(selectedPool);
        }
        List<QuestionPoolItem> selected = selectedPool.subList(0, request.count());
        jdbc.update("INSERT INTO lian_xi_hui_hua(xue_sheng_id,ke_mu_id,zhuang_tai,ti_mu_shu) VALUES (?,?,'CREATED',?)",
                studentId, request.subjectId(), selected.size());
        Long sessionId = requiredLastInsertId();
        int order = 1;
        for (QuestionPoolItem question : selected) {
            writeFrozenQuestion(sessionId, question, order++);
        }
        return session(userId, sessionId);
    }

    @Transactional(readOnly = true)
    public StudentPracticeDtos.Session session(Long userId, Long sessionId) {
        long studentId = requireStudent(userId);
        SessionHeader header = findSession(studentId, sessionId, false);
        return toSession(header, frozenQuestions(sessionId));
    }

    @Transactional
    public StudentPracticeDtos.Result submit(Long userId, Long sessionId, StudentPracticeDtos.SubmitRequest request) {
        long studentId = requireStudent(userId);
        SessionHeader header = findSession(studentId, sessionId, true);
        if ("SUBMITTED".equals(header.status())) {
            fail("PRACTICE_ALREADY_SUBMITTED", "该练习已经提交，不能重复提交", HttpStatus.CONFLICT);
        }
        List<FrozenQuestion> questions = frozenQuestions(sessionId);
        Map<Long, StudentPracticeDtos.Answer> answers = validateAnswerSet(request.answers(), questions);
        List<GradedAnswer> gradedAnswers = new ArrayList<>();
        for (FrozenQuestion question : questions) {
            StudentPracticeDtos.Answer answer = answers.get(question.id());
            gradedAnswers.add(new GradedAnswer(question, answer, isCorrect(question, answer.answer())));
        }

        LocalDateTime submittedAt = LocalDateTime.now();
        int correctCount = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        for (GradedAnswer graded : gradedAnswers) {
            BigDecimal score = graded.correct() ? graded.question().score() : BigDecimal.ZERO;
            jdbc.update("""
                    INSERT INTO xue_sheng_da_ti(lian_xi_ti_mu_id,xue_sheng_id,xue_sheng_da_an,shi_fou_zheng_que,de_fen,yong_shi_miao_shu,ti_jiao_shi_jian)
                    VALUES (?,?,?, ?,?,?,?)
                    """, graded.question().id(), studentId, writeJson(graded.answer().answer()), graded.correct(), score,
                    graded.answer().elapsedSeconds(), submittedAt);
            long answerId = requiredLastInsertId();
            updateWrongQuestion(studentId, graded.question().questionId(), answerId, graded.correct(), submittedAt);
            if (graded.correct()) {
                correctCount++;
                totalScore = totalScore.add(score);
            }
        }
        jdbc.update("""
                INSERT INTO xue_xi_jie_guo(lian_xi_hui_hua_id,zong_ti_shu,zheng_que_shu,zong_de_fen,ti_jiao_shi_jian)
                VALUES (?,?,?,?,?)
                """, sessionId, questions.size(), correctCount, totalScore, submittedAt);
        int changed = jdbc.update("""
                UPDATE lian_xi_hui_hua
                SET zhuang_tai='SUBMITTED',ti_jiao_shi_jian=?
                WHERE id=? AND xue_sheng_id=? AND zhuang_tai='CREATED'
                """, submittedAt, sessionId, studentId);
        if (changed != 1) {
            fail("PRACTICE_ALREADY_SUBMITTED", "该练习已经提交，不能重复提交", HttpStatus.CONFLICT);
        }
        return result(userId, sessionId);
    }

    @Transactional(readOnly = true)
    public StudentPracticeDtos.Result result(Long userId, Long sessionId) {
        long studentId = requireStudent(userId);
        SessionHeader header = findSession(studentId, sessionId, false);
        if (!"SUBMITTED".equals(header.status())) {
            fail("PRACTICE_NOT_SUBMITTED", "练习尚未提交，暂不能查看答案和解析", HttpStatus.CONFLICT);
        }
        ResultHeader result = jdbc.query("""
                SELECT zong_ti_shu,zheng_que_shu,zong_de_fen,ti_jiao_shi_jian
                FROM xue_xi_jie_guo WHERE lian_xi_hui_hua_id=?
                """, (rs, row) -> new ResultHeader(rs.getInt(1), rs.getInt(2), rs.getBigDecimal(3), rs.getObject(4, LocalDateTime.class)), sessionId)
                .stream().findFirst().orElseThrow(() -> business("PRACTICE_RESULT_NOT_FOUND", "练习结果不存在", HttpStatus.NOT_FOUND));

        List<StudentPracticeDtos.ResultQuestion> records = new ArrayList<>();
        for (FrozenQuestion question : frozenQuestions(sessionId)) {
            AnswerFact fact = jdbc.query("""
                    SELECT id,xue_sheng_da_an,shi_fou_zheng_que,de_fen
                    FROM xue_sheng_da_ti WHERE lian_xi_ti_mu_id=? AND xue_sheng_id=?
                    """, (rs, row) -> new AnswerFact(rs.getLong(1), readJson(rs.getString(2)), rs.getBoolean(3), rs.getBigDecimal(4)), question.id(), studentId)
                    .stream().findFirst().orElseThrow(() -> business("PRACTICE_ANSWER_NOT_FOUND", "练习答题事实不完整", HttpStatus.INTERNAL_SERVER_ERROR));
            records.add(new StudentPracticeDtos.ResultQuestion(fact.id(), toSafeQuestion(question, sessionId, "SUBMITTED"), fact.answer(), readJson(question.correctAnswer()),
                    question.standardAnalysis(), fact.correct(), fact.score()));
        }
        return new StudentPracticeDtos.Result(sessionId, header.subjectId(), header.subjectCode(), header.subjectName(),
                result.totalCount(), result.correctCount(), result.totalScore(), result.submittedAt(), records);
    }

    @Transactional(readOnly = true)
    public List<StudentPracticeDtos.WrongQuestionItem> wrongQuestions(Long userId) {
        return wrongQuestions(userId, null);
    }

    @Transactional(readOnly = true)
    public StudentPracticeDtos.WrongQuestionPage wrongQuestions(Long userId,String subjectCode,Long knowledgePointId,String status,String keyword,int page,int size) {
        long studentId = requireStudent(userId);
        String normalizedSubjectCode = subjectCode == null || subjectCode.isBlank()
                ? null : subjectCode.trim().toUpperCase(Locale.ROOT);
        if (normalizedSubjectCode != null && count("SELECT COUNT(*) FROM ke_mu WHERE ke_mu_dai_ma=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0",
                normalizedSubjectCode) != 1) {
            fail("PRACTICE_SUBJECT_NOT_FOUND", "科目不存在或已停用", HttpStatus.BAD_REQUEST);
        }
        StringBuilder sql=new StringBuilder("""
                SELECT c.ti_mu_id,s.ke_mu_dai_ma,s.ke_mu_ming_cheng,lt.ti_mu_lei_xing,LEFT(lt.ti_gan_kuai_zhao,120),
                       c.cuo_wu_ci_shu,c.lian_xu_zheng_que_ci_shu,c.zhuang_tai,c.zui_jin_cuo_wu_shi_jian,
                       COALESCE((SELECT JSON_ARRAYAGG(JSON_OBJECT('id',z.id,'name',z.zhi_shi_dian_ming_cheng,'path',z.wan_zheng_lu_jing)) FROM ti_mu_zhi_shi_dian tz JOIN zhi_shi_dian z ON z.id=tz.zhi_shi_dian_id WHERE tz.ti_mu_id=c.ti_mu_id AND tz.yi_shan_chu=0),'[]')
                FROM cuo_ti_ji_lu c
                JOIN xue_sheng_da_ti da ON da.id=c.zui_jin_da_ti_id
                JOIN lian_xi_ti_mu lt ON lt.id=da.lian_xi_ti_mu_id
                JOIN lian_xi_hui_hua h ON h.id=lt.lian_xi_hui_hua_id
                JOIN ke_mu s ON s.id=h.ke_mu_id
                WHERE c.xue_sheng_id=?
                """);List<Object>args=new ArrayList<>();args.add(studentId);if(normalizedSubjectCode!=null){sql.append(" AND s.ke_mu_dai_ma=?");args.add(normalizedSubjectCode);}if(knowledgePointId!=null){sql.append(" AND EXISTS(SELECT 1 FROM ti_mu_zhi_shi_dian tz WHERE tz.ti_mu_id=c.ti_mu_id AND tz.zhi_shi_dian_id=? AND tz.yi_shan_chu=0)");args.add(knowledgePointId);}if(status!=null&&!status.isBlank()){sql.append(" AND c.zhuang_tai=?");args.add(status.trim().toUpperCase(Locale.ROOT));}else sql.append(" AND c.zhuang_tai<>'MASTERED'");if(keyword!=null&&!keyword.isBlank()){sql.append(" AND lt.ti_gan_kuai_zhao LIKE ?");args.add("%"+keyword.trim()+"%");}long total=count("SELECT COUNT(*) FROM ("+sql+") w",args.toArray());sql.append(" ORDER BY c.zui_jin_cuo_wu_shi_jian DESC,c.id DESC LIMIT ? OFFSET ?");args.add(size);args.add(page*size);List<StudentPracticeDtos.WrongQuestionItem>items=jdbc.query(sql.toString(),(rs,row)->wrongItem(rs),args.toArray());return new StudentPracticeDtos.WrongQuestionPage(items,total,page,size);
    }
    public List<StudentPracticeDtos.WrongQuestionItem> wrongQuestions(Long userId,String subjectCode){return wrongQuestions(userId,subjectCode,null,null,null,0,100).items();}

    @Transactional public void archiveWrongQuestion(Long userId,Long questionId){long studentId=requireStudent(userId);if(jdbc.update("UPDATE cuo_ti_ji_lu SET zhuang_tai='MASTERED' WHERE xue_sheng_id=? AND ti_mu_id=?",studentId,questionId)!=1)fail("WRONG_QUESTION_NOT_FOUND","错题不存在",HttpStatus.NOT_FOUND);}

    @Transactional public StudentPracticeDtos.Session retryWrongQuestion(Long userId,Long questionId){long studentId=requireStudent(userId);Long subject=jdbc.query("SELECT q.ke_mu_id FROM cuo_ti_ji_lu c JOIN ti_mu q ON q.id=c.ti_mu_id WHERE c.xue_sheng_id=? AND c.ti_mu_id=?",rs->rs.next()?rs.getLong(1):null,studentId,questionId);if(subject==null)fail("WRONG_QUESTION_NOT_FOUND","错题不存在",HttpStatus.NOT_FOUND);return create(userId,new StudentPracticeDtos.CreateRequest(subject,List.of(),List.of(),null,1,questionId));}

    @Transactional(readOnly = true)
    public StudentPracticeDtos.WrongQuestionDetail wrongQuestion(Long userId, Long questionId) {
        long studentId = requireStudent(userId);
        WrongDetailRow row = jdbc.query("""
                SELECT c.ti_mu_id,s.ke_mu_dai_ma,s.ke_mu_ming_cheng,lt.ti_mu_lei_xing,LEFT(lt.ti_gan_kuai_zhao,120),
                       c.cuo_wu_ci_shu,c.lian_xu_zheng_que_ci_shu,c.zhuang_tai,c.zui_jin_cuo_wu_shi_jian,
                       lt.ti_gan_kuai_zhao,lt.xuan_xiang_kuai_zhao,lt.zheng_que_da_an_kuai_zhao,lt.biao_zhun_jie_xi_kuai_zhao,
                       lt.zhi_shi_dian_kuai_zhao,da.xue_sheng_da_an,
                       (SELECT wrong_da.id
                        FROM xue_sheng_da_ti wrong_da
                        JOIN lian_xi_ti_mu wrong_lt ON wrong_lt.id=wrong_da.lian_xi_ti_mu_id
                        JOIN lian_xi_hui_hua wrong_h ON wrong_h.id=wrong_lt.lian_xi_hui_hua_id
                        WHERE wrong_da.xue_sheng_id=c.xue_sheng_id
                          AND wrong_lt.ti_mu_id=c.ti_mu_id
                          AND wrong_da.shi_fou_zheng_que=0
                          AND wrong_h.zhuang_tai='SUBMITTED'
                        ORDER BY wrong_da.ti_jiao_shi_jian DESC,wrong_da.id DESC
                        LIMIT 1)
                FROM cuo_ti_ji_lu c
                JOIN xue_sheng_da_ti da ON da.id=c.zui_jin_da_ti_id
                JOIN lian_xi_ti_mu lt ON lt.id=da.lian_xi_ti_mu_id
                JOIN lian_xi_hui_hua h ON h.id=lt.lian_xi_hui_hua_id
                JOIN ke_mu s ON s.id=h.ke_mu_id
                WHERE c.xue_sheng_id=? AND c.ti_mu_id=?
                """, (rs, index) -> new WrongDetailRow(wrongItem(rs), rs.getString(10), rs.getString(11), rs.getString(12),
                        rs.getString(13), rs.getString(14), rs.getString(15), rs.getObject(16, Long.class)), studentId, questionId)
                .stream().findFirst().orElseThrow(() -> business("WRONG_QUESTION_NOT_FOUND", "错题不存在或不属于当前学生", HttpStatus.NOT_FOUND));
        if (row.aiAnalysisAnswerFactId() == null) {
            throw business("WRONG_ANSWER_FACT_NOT_FOUND", "错题缺少可用于分析的正式错误答题事实", HttpStatus.CONFLICT);
        }
        List<StudentPracticeDtos.Attachment> attachments = jdbc.query("""
                SELECT id,guan_lian_wei_zhi,fu_jian_lei_xing,yuan_shi_wen_jian_ming,dui_xiang_biao_shi,zhuang_tai,xiang_dui_lu_jing,nei_rong_ha_xi
                FROM ti_mu_fu_jian WHERE ti_mu_id=? AND yi_shan_chu=0 ORDER BY guan_lian_wei_zhi,pai_xu,id
                """, (rs, index) -> attachment(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                        "/api/v1/student/wrong-questions/" + questionId + "/attachments/" + rs.getLong(1) + "/content"), questionId);
        return new StudentPracticeDtos.WrongQuestionDetail(row.aiAnalysisAnswerFactId(), row.item(), row.stem(), readOptions(row.options()), readJson(row.studentAnswer()),
                readJson(row.correctAnswer()), row.analysis(), readKnowledgePoints(row.knowledgePoints()), attachments);
    }

    private void validateRequest(StudentPracticeDtos.CreateRequest request) {
        if (request.questionTypes() != null) {
            for (String type : request.questionTypes()) {
                if (!AUTO_GRADABLE_TYPES.contains(type)) {
                    fail("PRACTICE_QUESTION_TYPE_INVALID", "练习只支持单选、多选和填空自动判分题", HttpStatus.BAD_REQUEST);
                }
            }
        }
        if (request.referenceQuestionId() != null) {
            ReferenceQuestion reference = referenceQuestion(request.referenceQuestionId());
            if (reference.subjectId() != request.subjectId()) {
                fail("PRACTICE_REFERENCE_INVALID", "类似练习参考题与所选科目不一致", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private long requireStudent(Long userId) {
        Long studentId = jdbc.query("""
                SELECT id FROM xue_sheng_dang_an
                WHERE yong_hu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                """, (rs, row) -> rs.getLong(1), userId).stream().findFirst()
                .orElseThrow(() -> business("STUDENT_PROFILE_UNAVAILABLE", "当前账号没有有效学生档案", HttpStatus.FORBIDDEN));
        return studentId;
    }

    private void validateSubject(Long subjectId) {
        if (count("SELECT COUNT(*) FROM ke_mu WHERE id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0", subjectId) != 1) {
            fail("PRACTICE_SUBJECT_NOT_FOUND", "科目不存在或已停用", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateKnowledgePoints(Long subjectId, List<Long> pointIds) {
        if (pointIds == null || pointIds.isEmpty()) {
            return;
        }
        if (new HashSet<>(pointIds).size() != pointIds.size()) {
            fail("PRACTICE_KNOWLEDGE_POINT_INVALID", "知识点不能重复", HttpStatus.BAD_REQUEST);
        }
        String marks = placeholders(pointIds.size());
        int actual = count("SELECT COUNT(*) FROM zhi_shi_dian WHERE ke_mu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0 AND id IN (" + marks + ")",
                join(subjectId, pointIds));
        if (actual != pointIds.size()) {
            fail("PRACTICE_KNOWLEDGE_POINT_INVALID", "知识点不存在、已停用或不属于所选科目", HttpStatus.BAD_REQUEST);
        }
    }

    private List<QuestionPoolItem> findEligibleQuestions(Long userId,StudentPracticeDtos.CreateRequest request) {
        List<Object> arguments = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT q.id,q.ke_mu_id,q.ti_mu_lei_xing,q.ti_gan,CAST(q.zheng_que_da_an AS CHAR),q.nan_du
                FROM ti_mu q
                WHERE q.ke_mu_id=? AND q.zhuang_tai='PUBLISHED' AND q.shi_yong_mo_shi='ONLINE_PRACTICE'
                  AND q.shi_fou_ke_zi_dong_pan_fen=1 AND q.yi_shan_chu=0
                  AND q.ti_mu_lei_xing IN ('SINGLE_CHOICE','MULTIPLE_CHOICE','FILL_BLANK')
                  AND (q.ke_jian_fan_wei='GLOBAL' OR (q.ke_jian_fan_wei='TEACHING_SCOPE_PRIVATE' AND EXISTS (
                    SELECT 1 FROM xue_sheng_dang_an xs JOIN ban_ji_xue_sheng bx ON bx.xue_sheng_id=xs.id
                    JOIN ren_ke_guan_xi r ON r.id=q.ren_ke_guan_xi_id AND r.ban_ji_id=bx.ban_ji_id AND r.ke_mu_id=q.ke_mu_id
                    WHERE xs.yong_hu_id=? AND xs.zhuang_tai='ACTIVE' AND xs.yi_shan_chu=0
                      AND bx.shi_fou_zhu_ban_ji=1 AND bx.zhuang_tai='ACTIVE' AND bx.tui_chu_shi_jian IS NULL AND r.zhuang_tai='ACTIVE')))
                """);
        arguments.add(request.subjectId());
        arguments.add(userId);
        if (request.difficulty() != null) {
            sql.append(" AND q.nan_du=?");
            arguments.add(request.difficulty());
        }
        if (request.questionTypes() != null && !request.questionTypes().isEmpty()) {
            sql.append(" AND q.ti_mu_lei_xing IN (").append(placeholders(request.questionTypes().size())).append(')');
            arguments.addAll(request.questionTypes());
        }
        if (request.knowledgePointIds() != null && !request.knowledgePointIds().isEmpty()) {
            sql.append(" AND EXISTS (SELECT 1 FROM ti_mu_zhi_shi_dian qkp WHERE qkp.ti_mu_id=q.id AND qkp.yi_shan_chu=0 AND qkp.zhi_shi_dian_id IN (")
                    .append(placeholders(request.knowledgePointIds().size())).append("))");
            arguments.addAll(request.knowledgePointIds());
        }
        if (request.referenceQuestionId() != null) {
            ReferenceQuestion reference = referenceQuestion(request.referenceQuestionId());
            sql.append(" AND q.id<>? AND EXISTS (SELECT 1 FROM ti_mu_zhi_shi_dian candidate_kp JOIN ti_mu_zhi_shi_dian reference_kp ON reference_kp.zhi_shi_dian_id=candidate_kp.zhi_shi_dian_id AND reference_kp.ti_mu_id=? AND reference_kp.yi_shan_chu=0 WHERE candidate_kp.ti_mu_id=q.id AND candidate_kp.yi_shan_chu=0)");
            arguments.add(reference.id());
            arguments.add(reference.id());
            sql.append(" ORDER BY CASE WHEN q.ti_mu_lei_xing=? THEN 0 ELSE 1 END,ABS(q.nan_du-?),q.id DESC");
            arguments.add(reference.type());
            arguments.add(reference.difficulty());
        } else {
            sql.append(" ORDER BY q.id DESC");
        }
        List<QuestionPoolSeed> seeds = jdbc.query(sql.toString(), (rs, row) -> new QuestionPoolSeed(rs.getLong(1), rs.getLong(2),
                rs.getString(3), rs.getString(4), rs.getString(5), rs.getInt(6)), arguments.toArray());
        return seeds.stream().map(this::toEligibleQuestion).flatMap(Optional::stream).toList();
    }

    private Optional<QuestionPoolItem> toEligibleQuestion(QuestionPoolSeed question) {
        List<StudentPracticeDtos.Option> options = question.type().equals("FILL_BLANK") ? List.of() : jdbc.query("""
                SELECT xuan_xiang_biao_shi,xuan_xiang_nei_rong
                FROM ti_mu_xuan_xiang WHERE ti_mu_id=? AND yi_shan_chu=0 ORDER BY pai_xu,id
                """, (rs, row) -> new StudentPracticeDtos.Option(rs.getString(1), rs.getString(2)), question.id());
        Optional<String> analysis = jdbc.query("""
                SELECT jie_xi_nei_rong FROM ti_mu_jie_xi
                WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD' AND ban_ben_hao=1 AND zhuang_tai='PUBLISHED' AND yi_shan_chu=0
                """, (rs, row) -> rs.getString(1), question.id()).stream().findFirst();
        List<StudentPracticeDtos.KnowledgePoint> points = jdbc.query("""
                SELECT k.id,k.zhi_shi_dian_ming_cheng,k.wan_zheng_lu_jing
                FROM ti_mu_zhi_shi_dian qkp JOIN zhi_shi_dian k ON k.id=qkp.zhi_shi_dian_id
                WHERE qkp.ti_mu_id=? AND qkp.yi_shan_chu=0 AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                ORDER BY qkp.pai_xu,qkp.id
                """, (rs, row) -> new StudentPracticeDtos.KnowledgePoint(rs.getLong(1), rs.getString(2), rs.getString(3)), question.id());
        if (analysis.isEmpty() || points.isEmpty() || !safePracticeAttachments(question.id(), question.stem(), question.correctAnswer(), analysis.get(), options)
                || !hasValidAnswerStructure(question.type(), question.correctAnswer(), options)) {
            return Optional.empty();
        }
        return Optional.of(new QuestionPoolItem(question.id(), question.subjectId(), question.type(), question.stem(),
                question.correctAnswer(), question.difficulty(), options, analysis.get(), points));
    }

    private void writeFrozenQuestion(long sessionId, QuestionPoolItem question, int order) {
        jdbc.update("""
                INSERT INTO lian_xi_ti_mu(lian_xi_hui_hua_id,ti_mu_id,ti_mu_shun_xu,fen_zhi,ti_mu_lei_xing,nan_du_kuai_zhao,ti_gan_kuai_zhao,
                    xuan_xiang_kuai_zhao,zheng_que_da_an_kuai_zhao,biao_zhun_jie_xi_kuai_zhao,zhi_shi_dian_kuai_zhao)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """, sessionId, question.id(), order, BigDecimal.ONE, question.type(), question.difficulty(),
                textNormalizer.normalize(question.stem()), question.options().isEmpty() ? null : writeJson(question.options()), question.correctAnswer(), question.standardAnalysis(),
                writeJson(question.knowledgePoints()));
    }

    private boolean safePracticeAttachments(long questionId, String stem, String answer, String analysis, List<StudentPracticeDtos.Option> options) {
        if (containsObjectMarker(answer)) return false;
        List<AttachmentRow> rows = jdbc.query("SELECT id,guan_lian_wei_zhi,fu_jian_lei_xing,zhuang_tai,dui_xiang_biao_shi,xiang_dui_lu_jing,nei_rong_ha_xi FROM ti_mu_fu_jian WHERE ti_mu_id=? AND yi_shan_chu=0", (rs, i) -> new AttachmentRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)), questionId);
        for (AttachmentRow row : rows) {
            if (!"ACTIVE".equals(row.status()) || !"IMAGE".equals(row.type()) || "ANSWER".equals(row.position()) || !"AVAILABLE".equals(attachmentContentService.renderStatus(row.path(), row.hash(), row.type(), row.status()))) return false;
        }
        List<String> markers = new ArrayList<>();
        markers.addAll(markers(stem)); markers.addAll(markers(analysis));
        for (StudentPracticeDtos.Option option : options) markers.addAll(markers(option.content()));
        if (markers.size() != rows.size()) return markers.isEmpty() && rows.isEmpty();
        return new HashSet<>(markers).size() == markers.size() && new HashSet<>(markers).equals(rows.stream().map(AttachmentRow::marker).collect(java.util.stream.Collectors.toSet()));
    }

    private List<String> markers(String value) {
        if (value == null) return List.of();
        List<String> result = new ArrayList<>(); Matcher matcher = OBJECT_MARKER.matcher(value); while (matcher.find()) result.add(matcher.group(1)); return result;
    }
    private boolean containsObjectMarker(String value) { return value != null && OBJECT_MARKER.matcher(value).find(); }

    private boolean hasValidAnswerStructure(String type, String answerJson, List<StudentPracticeDtos.Option> options) {
        try {
            JsonNode answer = objectMapper.readTree(answerJson);
            if (!answer.isObject() || !type.equals(answer.path("type").asText())) {
                return false;
            }
            if ("FILL_BLANK".equals(type)) {
                JsonNode blanks = answer.path("blanks");
                if (!blanks.isArray() || blanks.isEmpty()) return false;
                for (int index = 0; index < blanks.size(); index++) {
                    JsonNode blank = blanks.get(index);
                    JsonNode accepted = blank.path("acceptedAnswers");
                    if (blank.path("index").asInt() != index + 1 || !accepted.isArray() || accepted.isEmpty()
                            || java.util.stream.StreamSupport.stream(accepted.spliterator(), false)
                            .anyMatch(value -> !value.isTextual() || value.asText().isBlank())) return false;
                }
                return true;
            }
            if (options.size() < 2 || options.stream().anyMatch(option -> option.label() == null || option.label().isBlank()
                    || option.content() == null || option.content().isBlank()) || optionLabels(writeJson(options)).size() != options.size()) {
                return false;
            }
            JsonNode labels = answer.path("optionLabels");
            int minimum = "SINGLE_CHOICE".equals(type) ? 1 : 2;
            if (!labels.isArray() || labels.size() < minimum || ("SINGLE_CHOICE".equals(type) && labels.size() != 1)) return false;
            Set<String> optionLabels = optionLabels(writeJson(options));
            Set<String> answerLabels = new HashSet<>();
            for (JsonNode label : labels) {
                if (!label.isTextual() || !optionLabels.contains(label(label.asText())) || !answerLabels.add(label(label.asText()))) return false;
            }
            return answerLabels.size() == labels.size();
        } catch (Exception exception) {
            return false;
        }
    }

    private SessionHeader findSession(long studentId, long sessionId, boolean forUpdate) {
        String sql = """
                SELECT h.id,h.ke_mu_id,k.ke_mu_dai_ma,k.ke_mu_ming_cheng,h.zhuang_tai,h.ti_mu_shu,h.chuang_jian_shi_jian,h.ti_jiao_shi_jian
                FROM lian_xi_hui_hua h JOIN ke_mu k ON k.id=h.ke_mu_id
                WHERE h.id=? AND h.xue_sheng_id=?
                """ + (forUpdate ? " FOR UPDATE" : "");
        return jdbc.query(sql, (rs, row) -> sessionHeader(rs), sessionId, studentId).stream().findFirst()
                .orElseThrow(() -> business("PRACTICE_SESSION_NOT_FOUND", "练习会话不存在或不属于当前学生", HttpStatus.NOT_FOUND));
    }

    private List<FrozenQuestion> frozenQuestions(long sessionId) {
        return jdbc.query("""
                SELECT id,ti_mu_id,ti_mu_shun_xu,fen_zhi,ti_mu_lei_xing,nan_du_kuai_zhao,ti_gan_kuai_zhao,xuan_xiang_kuai_zhao,
                       CAST(zheng_que_da_an_kuai_zhao AS CHAR),biao_zhun_jie_xi_kuai_zhao,CAST(zhi_shi_dian_kuai_zhao AS CHAR)
                FROM lian_xi_ti_mu WHERE lian_xi_hui_hua_id=? ORDER BY ti_mu_shun_xu,id
                """, (rs, row) -> new FrozenQuestion(rs.getLong(1), rs.getLong(2), rs.getInt(3), rs.getBigDecimal(4), rs.getString(5), rs.getInt(6),
                        rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11)), sessionId);
    }

    private StudentPracticeDtos.Session toSession(SessionHeader header, List<FrozenQuestion> questions) {
        return new StudentPracticeDtos.Session(header.id(), header.subjectId(), header.subjectCode(), header.subjectName(), header.status(),
                header.questionCount(), header.createdAt(), header.submittedAt(), questions.stream().map(question -> toSafeQuestion(question, header.id(), header.status())).toList());
    }

    private StudentPracticeDtos.SessionQuestion toSafeQuestion(FrozenQuestion question, long sessionId, String sessionStatus) {
        int blankCount = "FILL_BLANK".equals(question.type()) ? readJson(question.correctAnswer()).path("blanks").size() : 0;
        return new StudentPracticeDtos.SessionQuestion(question.id(), question.questionId(), question.order(), question.type(), textNormalizer.normalize(question.stem()), question.difficulty(),
                question.score(), blankCount, readOptions(question.options()), readKnowledgePoints(question.knowledgePoints()), sessionAttachments(question.questionId(), sessionId, sessionStatus));
    }

    private ReferenceQuestion referenceQuestion(long questionId) {
        return jdbc.query("""
                SELECT id,ke_mu_id,ti_mu_lei_xing,nan_du FROM ti_mu
                WHERE id=? AND zhuang_tai='PUBLISHED' AND shi_yong_mo_shi='ONLINE_PRACTICE'
                  AND shi_fou_ke_zi_dong_pan_fen=1 AND yi_shan_chu=0
                """, (rs, row) -> new ReferenceQuestion(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4)), questionId)
                .stream().findFirst().orElseThrow(() -> business("PRACTICE_REFERENCE_INVALID", "类似练习参考题不存在或不可用于普通练习", HttpStatus.BAD_REQUEST));
    }

    private List<StudentPracticeDtos.Attachment> sessionAttachments(long questionId, long sessionId, String sessionStatus) {
        return jdbc.query("SELECT id,guan_lian_wei_zhi,fu_jian_lei_xing,yuan_shi_wen_jian_ming,dui_xiang_biao_shi,zhuang_tai,xiang_dui_lu_jing,nei_rong_ha_xi FROM ti_mu_fu_jian WHERE ti_mu_id=? AND yi_shan_chu=0 ORDER BY guan_lian_wei_zhi,pai_xu,id", (rs, i) -> {
            String position = rs.getString(2); long id = rs.getLong(1);
            if ("ANSWER".equals(position) || ("CREATED".equals(sessionStatus) && "STANDARD_ANALYSIS".equals(position))) return null;
            return attachment(id, position, rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), "/api/v1/student/practice-sessions/" + sessionId + "/attachments/" + id + "/content");
        }, questionId).stream().filter(java.util.Objects::nonNull).toList();
    }

    private StudentPracticeDtos.Attachment attachment(long id, String position, String type, String fileName, String marker, String status, String path, String hash, String url) {
        String renderStatus = attachmentContentService.renderStatus(path, hash, type, status);
        return new StudentPracticeDtos.Attachment(id, position, type, fileName, marker, status, renderStatus, "AVAILABLE".equals(renderStatus) ? url : null);
    }

    private Map<Long, StudentPracticeDtos.Answer> validateAnswerSet(List<StudentPracticeDtos.Answer> answers, List<FrozenQuestion> questions) {
        if (answers.size() != questions.size()) {
            fail("PRACTICE_ANSWER_COUNT_INVALID", "提交答案数量与练习题目数量不一致", HttpStatus.BAD_REQUEST);
        }
        Map<Long, StudentPracticeDtos.Answer> result = new HashMap<>();
        Set<Long> expected = new HashSet<>(questions.stream().map(FrozenQuestion::id).toList());
        for (StudentPracticeDtos.Answer answer : answers) {
            if (!expected.contains(answer.practiceQuestionId()) || result.put(answer.practiceQuestionId(), answer) != null) {
                fail("PRACTICE_ANSWER_SET_INVALID", "提交答案包含未知或重复练习题", HttpStatus.BAD_REQUEST);
            }
            if (answer.elapsedSeconds() != null && answer.elapsedSeconds() > 86400) {
                fail("PRACTICE_ELAPSED_SECONDS_INVALID", "单题用时不能超过86400秒", HttpStatus.BAD_REQUEST);
            }
        }
        return result;
    }

    private boolean isCorrect(FrozenQuestion question, JsonNode submitted) {
        return objectiveAnswerGrader.grade(question.type(), question.correctAnswer(), question.options(), submitted);
    }

    private boolean correctSingle(JsonNode answer, JsonNode submitted, String options) {
        if (!submitted.isTextual()) {
            fail("PRACTICE_ANSWER_FORMAT_INVALID", "单选题答案必须是选项标识", HttpStatus.BAD_REQUEST);
        }
        String value = label(submitted.asText());
        Set<String> available = optionLabels(options);
        if (!available.contains(value)) {
            fail("PRACTICE_OPTION_INVALID", "单选题答案不是当前题目的有效选项", HttpStatus.BAD_REQUEST);
        }
        JsonNode labels = answer.path("optionLabels");
        if (!labels.isArray() || labels.size() != 1) {
            throw business("PRACTICE_QUESTION_INVALID", "冻结的单选题答案不合法", HttpStatus.CONFLICT);
        }
        return value.equals(label(labels.get(0).asText()));
    }

    private boolean correctMultiple(JsonNode answer, JsonNode submitted, String options) {
        if (!submitted.isArray()) {
            fail("PRACTICE_ANSWER_FORMAT_INVALID", "多选题答案必须是选项标识数组", HttpStatus.BAD_REQUEST);
        }
        Set<String> available = optionLabels(options);
        Set<String> actual = new HashSet<>();
        for (JsonNode item : submitted) {
            if (!item.isTextual()) {
                fail("PRACTICE_ANSWER_FORMAT_INVALID", "多选题答案必须全部为选项标识", HttpStatus.BAD_REQUEST);
            }
            String value = label(item.asText());
            if (!available.contains(value)) {
                fail("PRACTICE_OPTION_INVALID", "多选题答案包含无效选项", HttpStatus.BAD_REQUEST);
            }
            actual.add(value);
        }
        if (actual.isEmpty()) {
            fail("PRACTICE_ANSWER_FORMAT_INVALID", "多选题至少选择一个选项", HttpStatus.BAD_REQUEST);
        }
        Set<String> expected = new HashSet<>();
        JsonNode labels = answer.path("optionLabels");
        if (!labels.isArray()) {
            throw business("PRACTICE_QUESTION_INVALID", "冻结的多选题答案不合法", HttpStatus.CONFLICT);
        }
        labels.forEach(item -> expected.add(label(item.asText())));
        return actual.equals(expected);
    }

    private boolean correctFillBlank(JsonNode answer, JsonNode submitted) {
        if (!submitted.isArray()) {
            fail("PRACTICE_ANSWER_FORMAT_INVALID", "填空题答案必须按空位顺序提交数组", HttpStatus.BAD_REQUEST);
        }
        JsonNode blanks = answer.path("blanks");
        if (!blanks.isArray() || blanks.isEmpty() || blanks.size() != submitted.size()) {
            fail("PRACTICE_BLANK_COUNT_INVALID", "填空题答案空位数量不一致", HttpStatus.BAD_REQUEST);
        }
        for (int index = 0; index < blanks.size(); index++) {
            JsonNode blank = blanks.get(index);
            if (blank.path("index").asInt(index + 1) != index + 1 || !submitted.get(index).isTextual()) {
                fail("PRACTICE_BLANK_FORMAT_INVALID", "填空题答案结构不合法", HttpStatus.BAD_REQUEST);
            }
            boolean caseSensitive = blank.path("caseSensitive").asBoolean(false);
            String actual = normalizeBlank(submitted.get(index).asText(), caseSensitive);
            JsonNode accepted = blank.path("acceptedAnswers");
            boolean matched = accepted.isArray() && java.util.stream.StreamSupport.stream(accepted.spliterator(), false)
                    .map(JsonNode::asText).map(value -> normalizeBlank(value, caseSensitive)).anyMatch(actual::equals);
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private void updateWrongQuestion(long studentId, long questionId, long answerId, boolean correct, LocalDateTime submittedAt) {
        if (!correct) {
            jdbc.update("""
                    INSERT INTO cuo_ti_ji_lu(xue_sheng_id,ti_mu_id,cuo_wu_ci_shu,lian_xu_zheng_que_ci_shu,zhuang_tai,zui_jin_da_ti_id,zui_jin_cuo_wu_shi_jian)
                    VALUES (?,?,1,0,'NEW',?,?)
                    ON DUPLICATE KEY UPDATE cuo_wu_ci_shu=cuo_wu_ci_shu+1,lian_xu_zheng_que_ci_shu=0,zhuang_tai='NEW',
                        zui_jin_da_ti_id=VALUES(zui_jin_da_ti_id),zui_jin_cuo_wu_shi_jian=VALUES(zui_jin_cuo_wu_shi_jian)
                    """, studentId, questionId, answerId, submittedAt);
            return;
        }
        jdbc.update("""
                UPDATE cuo_ti_ji_lu
                SET zhuang_tai=CASE WHEN lian_xu_zheng_que_ci_shu+1>=2 THEN 'MASTERED' ELSE 'REVIEWING' END,
                    lian_xu_zheng_que_ci_shu=lian_xu_zheng_que_ci_shu+1,
                    zui_jin_da_ti_id=?
                WHERE xue_sheng_id=? AND ti_mu_id=?
                """, answerId, studentId, questionId);
    }

    private StudentPracticeDtos.WrongQuestionItem wrongItem(ResultSet rs) throws SQLException {
        return new StudentPracticeDtos.WrongQuestionItem(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                textNormalizer.normalize(rs.getString(5)), rs.getInt(6), rs.getInt(7), rs.getString(8), rs.getObject(9, LocalDateTime.class),readKnowledgePoints(rs.getString(10)));
    }

    private List<StudentPracticeDtos.KnowledgePoint> activeKnowledgePoints(long subjectId) {
        return jdbc.query("""
                SELECT id,zhi_shi_dian_ming_cheng,wan_zheng_lu_jing
                FROM zhi_shi_dian WHERE ke_mu_id=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                ORDER BY wan_zheng_lu_jing,id
                """, (rs, row) -> new StudentPracticeDtos.KnowledgePoint(rs.getLong(1), rs.getString(2), rs.getString(3)), subjectId);
    }

    private Set<String> optionLabels(String options) {
        return readOptions(options).stream().map(StudentPracticeDtos.Option::label).map(this::label).collect(java.util.stream.Collectors.toSet());
    }

    private String label(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeBlank(String value, boolean caseSensitive) {
        StringBuilder normalized = new StringBuilder();
        for (char character : value.trim().toCharArray()) {
            if (character == '\u3000') normalized.append(' ');
            else if (character >= '\uFF01' && character <= '\uFF5E') normalized.append((char) (character - 0xFEE0));
            else normalized.append(character);
        }
        String result = normalized.toString().replace('，', ',').replace('。', '.').replace('；', ';').replace('：', ':').trim();
        return caseSensitive ? result : result.toLowerCase(Locale.ROOT);
    }

    private List<StudentPracticeDtos.Option> readOptions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<StudentPracticeDtos.Option>>() { });
        } catch (Exception exception) {
            throw business("PRACTICE_SNAPSHOT_INVALID", "练习题目快照损坏", HttpStatus.CONFLICT);
        }
    }

    private List<StudentPracticeDtos.KnowledgePoint> readKnowledgePoints(String json) {
        try {
            if(json==null||json.isBlank())return List.of();JsonNode nodes=objectMapper.readTree(json);if(!nodes.isArray())return List.of();
            List<StudentPracticeDtos.KnowledgePoint> values=new ArrayList<>();
            for(JsonNode node:nodes){long id=node.path("id").asLong(node.path("knowledgePointId").asLong());String name=node.path("name").asText(node.path("knowledgePointName").asText());String path=node.path("path").asText(node.path("fullPath").asText(name));if(id>0&&!name.isBlank())values.add(new StudentPracticeDtos.KnowledgePoint(id,name,path));}
            return List.copyOf(values);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw business("PRACTICE_SNAPSHOT_INVALID", "练习答案快照损坏", HttpStatus.CONFLICT);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("无法保存练习答题事实", exception);
        }
    }

    private long requiredLastInsertId() {
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null) throw new IllegalStateException("数据库未返回新建记录ID");
        return id;
    }

    private int count(String sql, Object... arguments) {
        Integer result = jdbc.queryForObject(sql, Integer.class, arguments);
        return result == null ? 0 : result;
    }

    private Object[] join(Long first, List<Long> values) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(first);
        arguments.addAll(values);
        return arguments.toArray();
    }

    private String placeholders(int count) {
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }

    private SessionHeader sessionHeader(ResultSet rs) throws SQLException {
        return new SessionHeader(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getInt(6),
                rs.getObject(7, LocalDateTime.class), rs.getObject(8, LocalDateTime.class));
    }

    private RenZhengYeWuYiChang business(String code, String message, HttpStatus status) {
        return new RenZhengYeWuYiChang(code, message, status);
    }

    private void fail(String code, String message, HttpStatus status) {
        throw business(code, message, status);
    }

    private record QuestionPoolSeed(long id, long subjectId, String type, String stem, String correctAnswer, int difficulty) {
    }

    private record ReferenceQuestion(long id, long subjectId, String type, int difficulty) {
    }

    private record QuestionPoolItem(long id, long subjectId, String type, String stem, String correctAnswer, int difficulty,
                                    List<StudentPracticeDtos.Option> options, String standardAnalysis,
                                    List<StudentPracticeDtos.KnowledgePoint> knowledgePoints) {
    }
    private record AttachmentRow(long id, String position, String type, String status, String marker, String path, String hash) { }

    private record SessionHeader(long id, long subjectId, String subjectCode, String subjectName, String status, int questionCount,
                                 LocalDateTime createdAt, LocalDateTime submittedAt) {
    }

    private record FrozenQuestion(long id, long questionId, int order, BigDecimal score, String type, int difficulty, String stem, String options,
                                  String correctAnswer, String standardAnalysis, String knowledgePoints) {
    }

    private record GradedAnswer(FrozenQuestion question, StudentPracticeDtos.Answer answer, boolean correct) {
    }

    private record AnswerFact(long id, JsonNode answer, boolean correct, BigDecimal score) {
    }

    private record ResultHeader(int totalCount, int correctCount, BigDecimal totalScore, LocalDateTime submittedAt) {
    }

    private record WrongDetailRow(StudentPracticeDtos.WrongQuestionItem item, String stem, String options, String correctAnswer,
                                  String analysis, String knowledgePoints, String studentAnswer, Long aiAnalysisAnswerFactId) {
    }
}
